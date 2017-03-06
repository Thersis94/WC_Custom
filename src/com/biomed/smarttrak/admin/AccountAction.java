package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
//Java 7
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.action.AdminControllerAction;
// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

/*****************************************************************************
 <p><b>Title</b>: AccountAction.java</p>
 <p><b>Description: Manages the Account records for Smartrak.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountAction extends SBActionAdapter {

	public static final String ACCOUNT_ID = "accountId"; //req param

	public AccountAction() {
		super();
	}

	public AccountAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter(ACCOUNT_ID)) return;

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String accountId = req.hasParameter(ACCOUNT_ID) ? req.getParameter(ACCOUNT_ID) : null;
		String sql = formatRetrieveQuery(accountId, schema);

		List<Object> params = new ArrayList<>();
		if (accountId != null) params.add(accountId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  accounts = db.executeSelect(sql, params, new AccountVO());
		log.debug("loaded " + accounts.size() + " accounts");

		//decrypt the owner profiles
		decryptNames(accounts);

		//if this is the edit form, we need a list of BiomedGPS Staff for the "Manager" dropdown
		if (accountId != null)
			loadManagerList(req, schema);

		putModuleData(accounts);
	}


	/**
	 * loads a list of profileId|Names for the BiomedGPS Staff role level - these are their Account Managers
	 * @param req
	 * @throws ActionException
	 */
	protected void loadManagerList(ActionRequest req, String schema) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select newid() as account_id, a.profile_id as owner_profile_id, a.first_nm, a.last_nm from profile a ");
		sql.append("inner join profile_role b on a.profile_id=b.profile_id and b.status_id=?");
		sql.append("and b.site_id=? and b.role_id=?");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(SecurityController.STATUS_ACTIVE);
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(AdminControllerAction.STAFF_ROLE_ID);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  accounts = db.executeSelect(sql.toString(), params, new AccountVO());
		log.debug("loaded " + accounts.size() + " managers");

		//decrypt the owner profiles
		decryptNames(accounts);
		Collections.sort(accounts, new NameComparator());

		req.setAttribute("managers", accounts);
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	protected String formatRetrieveQuery(String accountId, String schema) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.account_id, a.company_id, a.account_nm, a.type_id, ");
		sql.append("a.start_dt, a.expiration_dt, a.owner_profile_id, a.address_txt, ");
		sql.append("a.address2_txt, a.city_nm, a.state_cd, a.zip_cd, a.country_cd, ");
		sql.append("a.status_no, a.create_dt, a.update_dt, a.fd_auth_flg, a.ga_auth_flg, a.mkt_auth_flg, ");
		sql.append("p.first_nm, p.last_nm from ").append(schema).append("biomedgps_account a ");
		sql.append("left outer join profile p on a.owner_profile_id=p.profile_id ");		
		if (accountId != null) sql.append("where a.account_id=? ");
		sql.append("order by a.account_nm");

		log.debug(sql);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		saveRecord(req, false);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		saveRecord(req, true);
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(ActionRequest req, boolean isDelete) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			if (isDelete) {
				db.delete(new AccountVO(req));
			} else {
				db.save(new AccountVO(req));
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * saves the 3-4 fields we store on the account record for global-scope overrides
	 * @param req
	 * @throws ActionException
	 */
	protected void saveGlobalPermissions(ActionRequest req) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(schema).append("BIOMEDGPS_ACCOUNT ");
		sql.append("set ga_auth_flg=?, fd_auth_flg=?, mkt_auth_flg=?, update_dt=? where account_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, req.hasParameter("accountGA") ? 1 : 0);
			ps.setInt(2, req.hasParameter("accountFD") ? 1 : 0);
			ps.setInt(3, req.hasParameter("accountMkt") ? 1 : 0);
			ps.setTimestamp(4,  Convert.getCurrentTimestamp());
			ps.setString(5,  req.getParameter(ACCOUNT_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			throw new ActionException("could not save account ACLs", sqle);
		}
	}
}