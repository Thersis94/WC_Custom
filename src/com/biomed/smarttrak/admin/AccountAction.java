package com.biomed.smarttrak.admin;

//Java 7
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.action.AdminControllerAction;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
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
	public static final String MANAGERS = "managers";
	public static final String SESS_ACCOUNT = "sesAccount";

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
		if (req.hasParameter("changeAccount"))
			req.getSession().removeAttribute(SESS_ACCOUNT);

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String accountId = req.hasParameter(ACCOUNT_ID) ? req.getParameter(ACCOUNT_ID) : null;
		AccountVO acct = (AccountVO) req.getSession().getAttribute(SESS_ACCOUNT);

		//pull accountId from session if we need it
		if (StringUtil.isEmpty(accountId) && acct != null)
			accountId = acct.getAccountId();

		List<Object> accounts = loadAccounts(schema, accountId);

		//hold the selected account in session for editing
		if (acct == null && !accounts.isEmpty() && accounts.size() == 1)
			setSelectedAccount((AccountVO) accounts.get(0), req);

		//if this is the edit form, we need a list of BiomedGPS Staff for the "Manager" dropdown
		if (accountId != null)
			loadManagerList(req, schema);

		putModuleData(accounts);
	}


	/**
	 * loads a list of accounts from the DB.
	 * @param schema
	 * @param accountId
	 * @return
	 */
	private List<Object> loadAccounts(String schema, String accountId) {
		String sql = formatRetrieveQuery(accountId, schema);
		List<Object> params = new ArrayList<>();
		if (accountId != null) params.add(accountId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  accounts = db.executeSelect(sql, params, new AccountVO());
		log.debug("loaded " + accounts.size() + " accounts");

		//decrypt the owner profiles
		decryptNames(accounts);

		return accounts;
	}


	/**
	 * helper method for other actions - like permissions, teams, and users, to say "load and put this account in session"
	 * @param accountId
	 * @param req
	 */
	protected void loadSelectedAccount(String accountId, ActionRequest req) {
		List<Object> accts = loadAccounts((String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA), accountId);
		if (accts == null || accts.isEmpty())
			return;
		
		AccountVO  acct = (AccountVO) accts.get(0);
		setSelectedAccount(acct, req);
	}


	/**
	 * Puts the loaded account onto session.  Abstracted for reuse
	 * @param acct
	 * @param req
	 */
	protected void setSelectedAccount(AccountVO acct, ActionRequest req) {
		req.setParameter(ACCOUNT_ID, acct.getAccountId());
		req.getSession().setAttribute(SESS_ACCOUNT, acct);
	}


	/**
	 * method overloading so only methods that want the managers titles get them.  
	 * @param req
	 * @param schema
	 * @throws ActionException
	 */
	public void loadManagerList(ActionRequest req, String schema) {
		loadManagerList(req, schema, false);
	}


	/**
	 * loads a list of profileId|Names for the BiomedGPS Staff role level - these are their Account Managers
	 * @param req
	 * @throws ActionException
	 */
	protected void loadManagerList(ActionRequest req, String schema, boolean loadTitles) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select newid() as account_id, a.profile_id as owner_profile_id, a.first_nm, a.last_nm ");
		if (loadTitles) sql.append(", rd.value_txt as title ");
		sql.append("from profile a ");
		sql.append("inner join profile_role b on a.profile_id=b.profile_id and b.status_id=? ");
		if (loadTitles)  {
			sql.append("inner join register_submittal rsub on rsub.profile_id=a.profile_id ");
			sql.append("inner join register_data rd on rd.register_submittal_id=rsub.register_submittal_id and rd.register_field_id=? ");
		}
		sql.append("and b.site_id=? and b.role_id=?");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(SecurityController.STATUS_ACTIVE);
		if (loadTitles) params.add(UserVO.RegistrationMap.TITLE.getFieldId());
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(AdminControllerAction.STAFF_ROLE_ID);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  accounts = db.executeSelect(sql.toString(), params, new AccountVO());
		log.debug("loaded " + accounts.size() + " managers");

		//decrypt the owner profiles
		decryptNames(accounts);
		Collections.sort(accounts, new NameComparator());
		req.setAttribute(MANAGERS, accounts);
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	protected String formatRetrieveQuery(String accountId, String schema) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.account_id, a.company_id, a.account_nm, a.type_id, ");
		sql.append("a.start_dt, a.expiration_dt, a.owner_profile_id, a.address_txt, ");
		sql.append("a.address2_txt, a.city_nm, a.state_cd, a.zip_cd, a.country_cd, a.company_url, a.coowner_profile_id, ");
		sql.append("a.status_no, a.create_dt, a.update_dt, a.fd_auth_flg, a.ga_auth_flg, a.mkt_auth_flg, ");
		sql.append("p.first_nm, p.last_nm, c.company_nm ");
		sql.append("from ").append(schema).append("biomedgps_account a ");
		sql.append("left outer join profile p on a.owner_profile_id=p.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_company c on a.company_id=c.company_id ");
		sql.append("where 1=1 ");

		if (accountId != null) {
			sql.append("and a.account_id=? ");
		}
		sql.append("order by a.type_id, a.account_nm");

		log.debug(sql);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		saveRecord(req, false);
		setupRedirect(req);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		saveRecord(req, true);
		setupRedirect(req);
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
	 * @param req
	 */
	private void setupRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?").append(AdminControllerAction.ACTION_TYPE).append("=").append(req.getParameter(AdminControllerAction.ACTION_TYPE));
		url.append("&").append(ACCOUNT_ID).append("=").append(req.getParameter(ACCOUNT_ID));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
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


	/**
	 * Creates an instance of this class, then uses it to check/load the selected accountVo into session and set reqParam if needed
	 * @param req
	 * @param dbConn
	 * @param attributes
	 */
	public static void loadAccount(ActionRequest req, SMTDBConnection dbConn, Map<String, Object> attributes) {
		String accountId = req.getParameter(ACCOUNT_ID);
		AccountVO acct = (AccountVO) req.getSession().getAttribute(SESS_ACCOUNT);
		//make sure work needs to be done first - most runtime iterations will end here.
		if (acct != null && acct.getAccountId().equals(accountId)) {
			return;
		} else if (acct != null && StringUtil.isEmpty(accountId)) {
			//set the missing reqParam from session
			req.setParameter(ACCOUNT_ID, acct.getAccountId());
			return;
		}

		AccountAction aa = new AccountAction();
		aa.setDBConnection(dbConn);
		aa.setAttributes(attributes);
		aa.loadSelectedAccount(accountId, req);
	}
}