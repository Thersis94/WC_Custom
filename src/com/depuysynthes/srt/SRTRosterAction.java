package com.depuysynthes.srt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SRTList;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.report.GenericReport;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.WebCrescendoReport;
import com.smt.sitebuilder.action.list.ListAction;
import com.smt.sitebuilder.action.registration.RegistrationAction;
import com.smt.sitebuilder.action.registration.ResponseLoader;
import com.smt.sitebuilder.action.registration.SubmittalAction;
import com.smt.sitebuilder.action.registration.SubmittalDataVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.admin.action.UserAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.security.UserLogin;

/****************************************************************************
 * <b>Title:</b> SRTRosterAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages SRT Roster Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
public class SRTRosterAction extends SimpleActionAdapter {

	public static final String REQ_CHECK_USER_BY_EMAIL = "checkUserByEmail";
	public static final String REQ_ROSTER_ID = "rosterId";
	public static final String USER_TABLE_NAME = "userTableName";
	public SRTRosterAction() {
		super();
	}

	public SRTRosterAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		if(req.getBooleanParameter("exportForm")) {
			processExportReq(req);
			return;
		}
		String opCoId = SRTUtil.getOpCO(req);
		req.setAttribute("workgroups", loadWorkgroups());
		req.setAttribute("territories", loadSRTList(SRTUtil.getListId(opCoId, SRTList.SRT_TERRITORIES), req));
		req.setAttribute("regions", loadSRTList(SRTUtil.getListId(opCoId, SRTList.SRT_REGION), req));
		req.setAttribute("areas", loadSRTList(SRTUtil.getListId(opCoId, SRTList.SRT_AREA), req));

		if (req.hasParameter(REQ_CHECK_USER_BY_EMAIL)) {
			checkForExistingUser(req);
			return;
		} else if (req.hasParameter("amid") || req.hasParameter(REQ_ROSTER_ID)) {
			GridDataVO<SRTRosterVO> users = loadRosterUsers(req);
			putModuleData(users.getRowData(), users.getTotal(), false);
		}


	}

	private void processExportReq(ActionRequest req) throws ActionException {
		AnnotationParser parser;
		String format = req.getParameter("format");
		try {
			parser = new AnnotationParser(Arrays.asList(SRTRosterVO.class), format);
		} catch (InvalidDataException e) {
			throw new ActionException("Could not load export file", e);
		}

		// Updated Code to use Standard Report Redir methods.
		AbstractSBReportVO rpt = new WebCrescendoReport(new GenericReport());
		rpt.setFileName("SRT User Import Form." + format);
		rpt.setData(parser.getTemplate());
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	/**
	 * Loads List Data for the given SRTList.
	 * @param territory
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private List<GenericVO> loadSRTList(String listId, ActionRequest req) throws ActionException {
		log.debug(listId);
		req.setParameter(ListAction.REQ_LIST_ID, listId);
		ActionControllerFactoryImpl.loadAction(ListAction.class.getName(), this).retrieve(req);
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		return (List<GenericVO>) mod.getActionData();
	}

	/**
	 * @param opCO
	 * @return
	 */
	private List<GenericVO> loadWorkgroups() {
		return new DBProcessor(dbConn, getCustomSchema()).executeSelect(loadWorkgroupsSql(), null, new GenericVO());
	}

	/**
	 * @return
	 */
	private String loadWorkgroupsSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select workgroup_id as key, workgroup_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_WORKGROUP order by workgroup_nm");
		return sql.toString();
	}

	/**
	 * loads the list of users tied to this account.
	 * This data populates the bootstrap table on the list page
	 * Can pass an optional profileId in order to retrieve via profileId rather
	 * than USER_ID.
	 * @param req
	 * @param schema
	 * @param profileId
	 * @return
	 * @throws ActionException
	 */
	public GridDataVO<SRTRosterVO> loadRosterUsers(ActionRequest req) throws ActionException {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String rosterId = req.hasParameter(REQ_ROSTER_ID) ? req.getParameter(REQ_ROSTER_ID) : null;
		String opCoId = ((SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA)).getOpCoId();
		String workgroupId = req.getParameter("workgroupId");
		boolean loadProfileData = false;

		//Build Sql
		String sql = formatRetrieveQuery(schema, rosterId, workgroupId);

		//Build Params
		List<Object> params = new ArrayList<>();
		params.add(opCoId);
		if (!StringUtil.isEmpty(rosterId)) {
			params.add(rosterId);
			loadProfileData = true;
		}
		if(!StringUtil.isEmpty(workgroupId)) {
			params.add(workgroupId);
		}

		GridDataVO<SRTRosterVO> rosters = new DBProcessor(dbConn, getCustomSchema()).executeSQLWithCount(sql, params, new SRTRosterVO(), req.getIntegerParameter("limit", SRTUtil.DEFAULT_RPP), req.getIntegerParameter("offset", 0));

		if(!rosters.getRowData().isEmpty()) {
			try {
				ProfileManagerFactory.getInstance(attributes).populateRecords(dbConn, rosters.getRowData());
			} catch (com.siliconmtn.exception.DatabaseException e) {
				log.error("Error Loading Profile Data for Rosters", e);
			}
		}

		if (loadProfileData)
			loadRegistration(req, rosters.getRowData());

		return rosters;
	}

	/**
	 * loads everything we need to know about a single user so we can edit their registration, profile, or user database records.
	 * Also loads ACLs so we can manage their permissions.
	 * @param userId
	 * @param schema
	 * @param userObj
	 * @throws ActionException 
	 */
	protected void loadRegistration(ActionRequest req, List<SRTRosterVO> users) throws ActionException {
		//fail-fast if there's no user to load responses for, or too many users
		if (users == null || users.isEmpty() || users.size() != 1)
			return;

		//load the registration form
		ActionInitVO actionInit = new ActionInitVO();
		actionInit.setActionGroupId(SRTUtil.REGISTRATION_GRP_ID);
		RegistrationAction sa = new RegistrationAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(getAttributes());
		sa.retrieve(req);
		req.setAttribute("registrationForm", ((ModuleVO)sa.getAttribute(Constants.MODULE_DATA)).getActionData());

		//load the user's registration responses - these will go into the UserDataVO->attributes map
		SRTRosterVO user = users.get(0);
		ResponseLoader rl = new ResponseLoader();
		rl.setDbConn(dbConn);
		rl.loadRegistrationResponses(user, SRTUtil.PUBLIC_SITE_ID);

		//load the user's profile data
		callProfileManager(user, req, false);

		users.set(0, user); //make sure the record gets back on the list, though it probably does by reference anyways
	}

	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	protected String formatRetrieveQuery(String schema, String rosterId, String workgroupId) {
		StringBuilder sql = new StringBuilder(300);
		sql.append(DBUtil.SELECT_FROM_STAR);
		sql.append(schema).append("dpy_syn_srt_roster ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		sql.append("and op_co_id=? ");
		if (!StringUtil.isEmpty(rosterId)) {
			sql.append("and roster_id=? ");
		}

		if(!StringUtil.isEmpty(workgroupId)) {
			sql.append("and workgroup_id = ?");
		}
		sql.append(DBUtil.ORDER_BY).append(" workgroup_id");
		log.debug(sql);
		return sql.toString();
	}

	/**
	 * uses email address to see if a user is already in ST, or already in WC, before being added.
	 * @param req
	 */
	private void checkForExistingUser(ActionRequest req) {
		boolean isEmailSearch = req.hasParameter(REQ_CHECK_USER_BY_EMAIL);
		String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		String email = req.getParameter(REQ_CHECK_USER_BY_EMAIL).toUpperCase();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select a.roster_id, b.profile_id, a.op_co_id from profile b ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster a ");
		sql.append("on a.profile_id=b.profile_id ");
		if(isEmailSearch) {
			sql.append("and b.search_email_txt = ? ");
		} else {
			sql.append(DBUtil.WHERE_CLAUSE).append(" a.wwid = ?");
		}

		log.debug(sql + " " + email);

		Map<String, String> data = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			StringEncrypter se = new StringEncrypter(encKey);
			if(isEmailSearch)
				ps.setString(1, se.encrypt(email));
			else 
				ps.setString(1, req.getParameter("wwid"));
			log.debug(ps);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				data.put(REQ_ROSTER_ID, rs.getString(1));
				data.put(UserAction.REQ_PROFILE_ID, rs.getString(2));
				data.put(SRTUtil.OP_CO_ID, rs.getString(3));
			}
		} catch (Exception e) {
			log.error("could not search existing users by email", e);
		}

		putModuleData(data);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		if(req.hasParameter("filePathText")) {
			ActionControllerFactoryImpl.loadAction(SRTRosterImportAction.class.getName(), this).build(req);
			return;
		}
		//ajax hook for quick-saving notes:
		if (req.hasParameter("saveStatus")) {
			saveStatus(req);
			return;
		}

		SRTRosterVO user = new SRTRosterVO(req);

		//save auth
		saveAuthRecord(user);

		//save their WC profile
		callProfileManager(user, req, true);

		//check & create profile_role if needed
		saveProfileRole(user, false);

		//save their registration data
		saveRegistrationData(req, user);

		//save their RosterVO (SRT_ROSTER table)
		saveRecord(user, false);

		//Send User back to Roster Edit Page with updated data.
		setupRedirect(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		SRTRosterVO user = new SRTRosterVO(req);
		saveRecord(user, true); //De-Activates them from SRT.
		saveProfileRole(user, true); //revoke website access
		setupRedirect(req);
	}

	/**
	 * Save SRT RosterVO 
	 * @param r
	 */
	/**
	 * Handles the onClick ajax call to quick-save the user's status (change / toggle)
	 * @param req
	 */
	private void saveStatus(ActionRequest req) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_srt_roster set is_active=?, update_dt=CURRENT_TIMESTAMP ");
		if(req.getBooleanParameter("activeFlg")) {
			sql.append(", deactivated_dt = CURRENT_TIMESTAMP ");
		}
		sql.append("where roster_id=?");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, req.getIntegerParameter("activeFlg"));
			ps.setString(2, req.getParameter(REQ_ROSTER_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not update user status", sqle);
		}
	}

	/**
	 * gives the user a dummy randomly-generated password and creates the authentication record.
	 * puts authenticationId onto the userVo for storage in the profile table.
	 * @param user
	 */
	protected void saveAuthRecord(SRTRosterVO user) throws ActionException {
		//create a random password if this is a new account and a password was not provided - this is for security reasons
		if (StringUtil.isEmpty(user.getPassword()) && StringUtil.isEmpty(user.getAuthenticationId()))
			user.setPassword(RandomAlphaNumeric.generateRandom(8));

		UserLogin ul = new UserLogin(dbConn, getAttributes());
		//save the record.  Flag it for password reset immediately.
		try {
			String authId = ul.checkAuth(user.getEmailAddress());
			//if the user had an auth record already then don't change their password or flag them for reset
			String pswd = !StringUtil.isEmpty(user.getPassword()) ? user.getPassword() : UserLogin.DUMMY_PSWD;
			authId = ul.saveAuthRecord(authId, user.getEmailAddress(), pswd, StringUtil.isEmpty(authId) ? 1 : 0);
			user.setAuthenticationId(authId);
		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Calls ProfileManager for both read and write transaction for core user data
	 * @param user
	 * @param req
	 * @throws ActionException 
	 */
	protected void callProfileManager(SRTRosterVO user, ActionRequest req, boolean isSave) {
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		try {
			if (isSave) {
				pm.updateProfile(user, dbConn);
				pm.assignCommunicationFlg(site.getOrganizationId(), user.getProfileId(), user.getAllowCommunication(), dbConn);
			} else {
				UserDataVO vo = pm.getProfile(user.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, site.getOrganizationId());
				user.setData(vo.getDataMap());
			}
		} catch (com.siliconmtn.exception.DatabaseException de) { //this is a different DatabaseException than DBProcessor
			log.error("could not load user profile", de);
		}
	}

	/**
	 * checks and creates the profile_role record tied to the public site, if necessary
	 * @param user
	 * @throws ActionException
	 */
	protected void saveProfileRole(SRTRosterVO user, boolean isDelete) throws ActionException {
		ProfileRoleManager prm = new ProfileRoleManager();
		SBUserRole role = new SBUserRole(SRTUtil.PUBLIC_SITE_ID);
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		role.setProfileId(user.getProfileId());

		try {
			//find any existing role - will either be deleted or updated
			role.setProfileRoleId(prm.checkRole(user.getProfileId(), role.getSiteId(), null, null, dbConn));

			if (isDelete) {
				prm.removeRole(role.getProfileRoleId(), dbConn);
				return;
			}
			//determine the role id to set
			setRoleId(role, user);

			prm.addRole(role, dbConn);

		} catch (com.siliconmtn.exception.DatabaseException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Helper method that determines the role id to set for the UserRole
	 * @param role
	 * @param user
	 */
	protected void setRoleId(SBUserRole role, SRTRosterVO user) {
		//determine the role id
		if (user.isAdmin()) {
			role.setRoleId(Integer.toString(SecurityController.ADMIN_ROLE_LEVEL));
		} else if(user.isActive()) {
			role.setRoleId(Integer.toString(SecurityController.PUBLIC_REGISTERED_LEVEL));
		} else {
			role.setRoleId(Integer.toString(SecurityController.PUBLIC_ROLE_LEVEL));
		}
	}

	/**
	 * Transposes the registration fields off the request into a list of Fields, and passes them to the 
	 * Registration action to be saved.
	 * @param req
	 * @param user
	 * @throws ActionException
	 */
	protected void saveRegistrationData(ActionRequest req, SRTRosterVO user) throws ActionException {
		SubmittalAction sa = new SubmittalAction();
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		Set<String> formFields = new HashSet<>(30);

		//possibly create a new registration record.  This would be for the 'add user' scenario
		if (StringUtil.isEmpty(user.getRegisterSubmittalId())) {
			req.setParameter(SBActionAdapter.SB_ACTION_ID, SRTUtil.REGISTRATION_GRP_ID);
			user.setRegisterSubmittalId(sa.insertRegisterSubmittal(req, user.getProfileId(), SRTUtil.PUBLIC_SITE_ID));
		}

		//build a list of values to insert based on the ones we're going to delete
		List<SubmittalDataVO> regData = new ArrayList<>();

		//put the fields we're going to be saving onto the request - Registration won't save what we can't prove we're passing
		req.setParameter("formFields", formFields.toArray(new String[formFields.size()]) , Boolean.TRUE);
		sa.updateRegisterData(req, user, user.getRegisterSubmittalId(), regData, false); //false = do not reload responses onto session
	}

	/**
	 * builds the redirect URL that takes us back to the list of teams page.
	 * @param req
	 */
	protected void setupRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?rosterId=").append(req.getParameter(REQ_ROSTER_ID));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(SRTRosterVO user, boolean isDelete) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			db.save(user);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}