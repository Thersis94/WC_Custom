package com.biomed.smarttrak.admin;

//Java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// WC_Custom
import com.biomed.smarttrak.vo.PermissionVO;
import com.biomed.smarttrak.util.SmarttrakTree;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.PageVO;

// WebCrescendo
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountPermissionAction.java</p>
 <p><b>Description: Manages the Account's permissions for Smartrak...the sections they'll be able to see.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 20, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountPermissionAction extends AbstractTreeAction {

	private static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param

	public AccountPermissionAction() {
		super();
	}

	public AccountPermissionAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//accountId is required for this action
		String accountId = req.hasParameter(ACCOUNT_ID) ? req.getParameter(ACCOUNT_ID) : null;
		if (accountId == null) return;

		SmarttrakTree t = loadTree(MASTER_ROOT, new PermissionVO().getClass(), accountId);
		putModuleData(t);
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	@Override
	protected String getFullHierarchySql() {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.section_id, a.parent_id, a.section_nm, a.order_no, a.solr_token_txt, ");
		sql.append("b.account_id, b.browse_no, b.updates_no, b.fd_no, b.ga_no from ");
		sql.append(schema).append("BIOMEDGPS_SECTION a ");
		sql.append("left outer join ").append(schema).append("BIOMEDGPS_ACCOUNT_ACL b ");
		sql.append("on a.section_id=b.section_id and b.account_id=? ");
		sql.append("order by a.PARENT_ID, a.ORDER_NO, a.SECTION_NM");
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//delete all first, then reinsert
		delete(req);

		//build a Map<sectionId, PermissionVO> of the records to insert.
		String accountId = req.getParameter(ACCOUNT_ID);
		String[] checkboxes = req.getParameterValues("permission");
		if (checkboxes == null || checkboxes.length == 0) return;

		Map<String, PermissionVO> data = new HashMap<>(checkboxes.length / 4); //4 columns, should get us closer than the default(12).
		for (String value : checkboxes) {
			String[] tokens = value.split("~"); //authArea~sectionId
			String sectionId = tokens[1];
			PermissionVO vo = data.get(sectionId);
			if (vo == null) vo = new PermissionVO(accountId, sectionId);
			vo.addSelection(value);
			data.put(sectionId, vo);
		}

		log.debug("loaded " + data.size() + " PermissionVOs to save");
		savePermissions(data.values());

		//don't alter the global-scope permissions if any of the above failed
		saveAccountSettings(req);

		setupRedirect(req);
	}


	/**
	 * builds the redirect URL that takes us back to the list of teams page.
	 * @param req
	 */
	protected void setupRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?actionType=").append(req.getParameter("actionType"));
		url.append("&accountId=").append(req.getParameter("accountId"));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 * deletes all the ACL records for the given account.  typically called prior to saving them. (flush & reinsert)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql  = new StringBuilder(200);
		sql.append("delete from ").append(schema).append("BIOMEDGPS_ACCOUNT_ACL ");
		sql.append("where account_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1,  req.getParameter(ACCOUNT_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			throw new ActionException("could not delete account ACLs", sqle);
		}
	}


	/**
	 * inserts the provided list of auth records as a batch set
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void savePermissions(Collection<PermissionVO> data) throws ActionException {
		if (data == null || data.isEmpty()) return;
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(schema).append("BIOMEDGPS_ACCOUNT_ACL ");
		sql.append("(account_acl_id, section_id, account_id, browse_no, updates_no, fd_no, ga_no, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?,?)");
		log.debug(sql);

		UUIDGenerator uuid = new UUIDGenerator();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (PermissionVO vo : data) {
				ps.setString(1, uuid.getUUID());
				ps.setString(2, vo.getSectionId());
				ps.setString(3, vo.getAccountId());
				ps.setInt(4,  vo.isBrowseAuth() ? 1 : 0);
				ps.setInt(5,  vo.isUpdatesAuth() ? 1 : 0);
				ps.setInt(6,  vo.isFdAuth() ? 1 : 0);
				ps.setInt(7,  vo.isGaAuth() ? 1 : 0);
				ps.setTimestamp(8, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			throw new ActionException("could not save account ACLs", sqle);
		}
	}


	/**
	 * saves the 3-4 fields we store on the account record for global-scope overrides
	 * @param req
	 * @throws ActionException
	 */
	protected void saveAccountSettings(ActionRequest req) throws ActionException {
		AccountAction aa = new AccountAction();
		aa.setAttributes(getAttributes());
		aa.setDBConnection(dbConn);
		aa.saveGlobalPermissions(req);
	}


	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.AbstractTreeAction#getCacheKey()
	 */
	@Override
	public String getCacheKey() {
		return null; //not cacheable
	}
}