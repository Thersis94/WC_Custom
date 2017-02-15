package com.biomed.smarttrak.admin;

//Java 7
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationAction;
import com.smt.sitebuilder.action.registration.ResponseLoader;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

//WC_Custom
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.biomed.smarttrak.admin.user.NameComparator;

/*****************************************************************************
 <p><b>Title</b>: AccountUserAction.java</p>
 <p><b>Description: Ties Users to Accounts for Smartrak. Supports creating new users and managing 
 			core user profile data (incl core registration data)</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountUserAction extends SBActionAdapter {

	protected static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param
	protected static final String USER_ID = "userId"; //req param

	public AccountUserAction() {
		super();
	}

	public AccountUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter(USER_ID)) return;

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		List<Object> users = loadAccountUsers(req, schema);

		//get more information about this one user, so we can display the edit screen.
		//If this is an ADD, we don't need the additional lookups
		if (req.hasParameter(USER_ID))
			loadRegistration(req, schema, users);

		//do this last, because loading the registration actions will collide with ModuleVO.actionData
		putModuleData(users);
	}


	/**
	 * loads the list of users tied to this account.  Called via ajax.  This data populates the bootstrap table on the list page
	 * @param req
	 * @param schema
	 * @throws ActionException
	 */
	protected List<Object> loadAccountUsers(ActionRequest req, String schema) throws ActionException {
		String accountId = req.hasParameter(ACCOUNT_ID) ? req.getParameter(ACCOUNT_ID) : null;
		String userId = req.hasParameter(USER_ID) ? req.getParameter(USER_ID) : null;
		String sql = formatRetrieveQuery(schema, userId);

		List<Object> params = new ArrayList<>();
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(accountId);
		if (userId != null) params.add(userId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  data = db.executeSelect(sql, params, new UserVO());
		log.debug("loaded " + data.size() + " users");

		//decrypt the owner profiles
		decryptNames(data);
		Collections.sort(data, new NameComparator());

		return data;
	}


	/**
	 * loads everything we need to know about a single user so we can edit their registration, profile, or user database records.
	 * Also loads ACLs so we can manage their permissions.
	 * @param userId
	 * @param schema
	 * @param userObj
	 * @throws ActionException 
	 */
	protected void loadRegistration(ActionRequest req, String schema, List<Object> users) throws ActionException {
		//load the registration form
		ActionInitVO actionInit = new ActionInitVO();
		actionInit.setActionGroupId(AdminControllerAction.REGISTRATION_GRP_ID);
		RegistrationAction sa = new RegistrationAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(getAttributes());
		sa.retrieve(req);
		req.setAttribute("registrationForm", ((ModuleVO)sa.getAttribute(Constants.MODULE_DATA)).getActionData());

		//fail-fast if there's no user to load responses for
		if (users == null || users.isEmpty())
			return;

		//load the user's registration responses - these will go into the UserDataVO->attributes map
		UserVO user = (UserVO) users.get(0);
		ResponseLoader rl = new ResponseLoader();
		rl.setDbConn(dbConn);
		rl.loadRegistrationResponses(user, AdminControllerAction.PUBLIC_SITE_ID);
		users.set(0, user); //make sure the record gets back on the list, though it probably does by reference anyways
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
	protected String formatRetrieveQuery(String schema, String userId) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select u.account_id, u.profile_id, u.user_id, u.register_submittal_id, u.status_cd, ");
		sql.append("u.expiration_dt, p.first_nm, p.last_nm, p.email_address_txt, cast(max(al.login_dt) as date) as login_dt ");
		sql.append("from ").append(schema).append("biomedgps_user u ");
		sql.append("left outer join profile p on u.profile_id=p.profile_id ");
		sql.append("left outer join authentication_log al on p.authentication_id=al.authentication_id and al.site_id=? and al.status_cd=1 ");
		sql.append("where u.account_id=? ");
		if (userId != null) sql.append("and u.user_id=? ");
		sql.append("group by u.account_id, u.profile_id, u.user_id, u.register_submittal_id, u.status_cd, ");
		sql.append("u.expiration_dt, p.first_nm, p.last_nm, p.email_address_txt ");

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
				db.delete(new UserVO(req));
			} else {
				db.save(new UserVO(req));
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}