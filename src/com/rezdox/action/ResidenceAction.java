package com.rezdox.action;

//Java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

//WC Custom
import com.rezdox.action.RewardsAction.Reward;
import com.rezdox.action.RezDoxNotifier.Message;
import com.rezdox.action.RezDoxUtils.Product;
import com.rezdox.api.SunNumberAPIManager;
import com.rezdox.api.WalkScoreAPIManager;
import com.rezdox.api.ZillowAPIManager;
import com.rezdox.data.ResidenceFormProcessor;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.ResidenceAttributeVO;
import com.rezdox.vo.ResidenceVO;
import com.rezdox.vo.SunNumberVO;
import com.rezdox.vo.WalkScoreVO;
import com.rezdox.vo.ZillowPropertyVO;
//SMTBaseLIbs
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseNote;
import com.siliconmtn.db.DatabaseNote.DBType;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
//WC Core
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.LocationManager;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTemplateVO;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SBUserRoleContainer;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: ResidenceAction.java<p/>
 * <b>Description: Manages member interactions with a residence.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 7, 2018
 ****************************************************************************/
public class ResidenceAction extends SBActionAdapter {

	public static final String RESIDENCE_DATA = "residenceData";
	public static final String RESIDENCE_ID = "residenceId";
	public static final String PRIMARY_RESIDENCE = " Primary Residence";
	public static final String UPGRADE_MSG = "You have reached your maximum residences. Please purchase a residence upgrade to continue.";
	public static final String SLUG_RESIDENCE_ZESTIMATE = "RESIDENCE_ZESTIMATE";
	public static final String SLUG_RESIDENCE_WALK_SCORE = "RESIDENCE_WALK_SCORE";
	public static final String SLUG_RESIDENCE_TRANSIT_SCORE = "RESIDENCE_TRANSIT_SCORE";
	public static final String SLUG_RESIDENCE_SUN_NUMBER = "RESIDENCE_SUN_NUMBER";

	protected static final int STATUS_INACTIVE = 0;
	protected static final int STATUS_ACTIVE = 1;
	protected static final int STATUS_SHARED = 2;

	public enum ResidenceColumnName {
		RESIDENCE_ID, CREATE_DT, UPDATE_DT
	}

	public ResidenceAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ResidenceAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public ResidenceAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//if arriving to do a residence transfer:
		if (req.hasParameter("approveTransfer")) {
			new ResidenceTransferAction(dbConn, attributes).transferResidence(req);
			return;
		}

		// If hitting this action with a business role or registered role, they are adding a new residence
		// Their role will be upgraded appropriately after adding a new residence
		SBUserRole role = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA));
		if (RezDoxUtils.REZDOX_BUSINESS_ROLE.equals(role.getRoleId()) || SBUserRoleContainer.REGISTERED_USER_ROLE_LEVEL == role.getRoleLevel()) {
			req.setParameter(RESIDENCE_ID, "new");
			if (SBUserRoleContainer.REGISTERED_USER_ROLE_LEVEL == role.getRoleLevel())
				req.setParameter("isRegistration", "1"); //used in new.jsp to prefil the residence-address form
		}

		List<ResidenceVO> residenceList = retrieveResidences(req);
		String residenceId = req.getParameter(RESIDENCE_ID);

		// When adding a new residence, check to make sure the member has not reached their limit
		// if so, redirect them to an upgrade path.
		if ("new".equalsIgnoreCase(residenceId) && !canAddNewResidence(residenceList, req)) {
			sendRedirect(RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH, UPGRADE_MSG, req);

		} else if (req.hasParameter("homeInfo") || req.hasParameter("settings")) {
			req.setAttribute(RESIDENCE_DATA, residenceList);
			putModuleData(retrieveHomeInfoForm(req));

		} else if (req.hasParameter("transfer")) {
			if (req.hasParameter("loadData")) {
				putModuleData(loadMembers(req));
			} else {
				putModuleData(residenceList, residenceList.size(), false);
			}

		} else {
			putModuleData(residenceList, residenceList.size(), false);
		}
	}

	/**
	 * @param req
	 * @return
	 */
	private List<MemberVO> loadMembers(ActionRequest req) {
		MemberAction ma = new MemberAction(getDBConnection(), getAttributes());
		return ma.listMembers(req);
	}

	/**
	 * Validates whether a new residence can be added by the member.
	 * 
	 * @param residenceList
	 * @param member
	 * @return true if residence can be added, false if not
	 * @throws ActionException 
	 */
	private boolean canAddNewResidence(List<ResidenceVO> residenceList, ActionRequest req) throws ActionException {
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		// Validate whether the member needs a residence upgrade
		SubscriptionAction sa = (SubscriptionAction) ActionControllerFactoryImpl.loadAction(SubscriptionAction.class.getName(), this);
		boolean needsUpgrade = sa.checkUpgrade(req, member, Product.RESIDENCE);

		// Set default residence name per requirements
		if (!needsUpgrade && residenceList != null) {
			ResidenceVO defaultResidence = new ResidenceVO();
			defaultResidence.setResidenceName(member.getLastName() + PRIMARY_RESIDENCE);
			residenceList.add(defaultResidence);
		}

		// A residence can be added if they don't need an upgrade
		return !needsUpgrade;
	}

	/**
	 * Retrieves base residence data
	 * 
	 * @param req
	 * @return
	 */
	@DatabaseNote(type = DBType.POSTGRES)
	protected List<ResidenceVO> retrieveResidences(ActionRequest req) {
		String schema = getCustomSchema();
		String residenceId = req.getParameter(RESIDENCE_ID);

		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.IMPROVEMENTS_VALUE_COEF);
		params.add(RezDoxUtils.getMemberId(req));

		// Using pivot table on the attributes to get additional data for display
		StringBuilder sql = new StringBuilder(900);
		sql.append("select r.residence_id, residence_nm, address_txt, address2_txt, city_nm, ");
		sql.append("state_cd, zip_cd, country_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt) as update_dt, ");
		sql.append("privacy_flg, for_sale_dt, last_sold_dt, beds_no, baths_no, coalesce(f_sqft_no, 0) as sqft_no, ");
		sql.append("zestimate_no, sum(pc.project_cost+pc.material_cost)*? as projects_total, m.status_flg ");
		sql.append("from ").append(schema).append("rezdox_residence r inner join ");
		sql.append(schema).append("rezdox_residence_member_xr m on r.residence_id = m.residence_id and m.status_flg > 0 "); //1=mine, 2=shared w/me
		sql.append("left join (SELECT * FROM crosstab('SELECT residence_id, slug_txt, value_txt FROM ").append(schema).append("rezdox_residence_attribute ORDER BY 1', ");
		sql.append("'SELECT DISTINCT slug_txt FROM ").append(schema).append("rezdox_residence_attribute WHERE slug_txt in (''bedrooms'',''bathrooms'',''finishedSqFt'', ''RESIDENCE_ZESTIMATE'') ORDER BY 1') ");
		sql.append("AS (residence_id text, baths_no float, beds_no int, f_sqft_no int, zestimate_no float) ");
		sql.append(") ra on r.residence_id = ra.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_project_cost_view pc on r.residence_id=pc.residence_id and pc.is_improvement=1 and pc.residence_view_flg=1 ");
		sql.append("where m.member_id=? ");

		// Return only a specific residence if selected
		if (!StringUtil.isEmpty(residenceId)) {
			sql.append("and r.residence_id = ? ");
			params.add(residenceId);
		}
		sql.append("group by r.residence_id, residence_nm, address_txt, address2_txt, city_nm, ");
		sql.append("state_cd, zip_cd, country_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt), ");
		sql.append("privacy_flg, for_sale_dt, last_sold_dt, beds_no, baths_no, coalesce(f_sqft_no, 0), ");
		sql.append("zestimate_no, m.status_flg ");
		sql.append("order by coalesce(m.status_flg, 1), r.create_dt"); //show primary residence first, which is the 1st one created. REZDOX-275
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		List<ResidenceVO> residences = dbp.executeSelect(sql.toString(), params, new ResidenceVO());

		// Prevent an unsupported operation exception when trying to add a residence to an empty list
		return residences.isEmpty() ? new ArrayList<>() : residences;
	}


	/**
	 * Return a list of Residences for the given member.
	 * @param req
	 * @return
	 */
	public List<ResidenceVO> listMyResidences(String memberId, String residenceId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(900);
		sql.append("select a.*, b.status_flg from ").append(schema).append("rezdox_residence a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_residence_member_xr b ");
		sql.append("on a.residence_id=b.residence_id and b.member_id=? and b.status_flg > 0 "); //1=active, 2=shared
		if (!StringUtil.isEmpty(residenceId)) sql.append(" where a.residence_id=?");
		sql.append("order by coalesce(b.status_flg, 1), a.create_dt");

		List<Object> params = new ArrayList<>();
		params.add(memberId);
		if (!StringUtil.isEmpty(residenceId)) params.add(residenceId);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		return dbp.executeSelect(sql.toString(), params, new ResidenceVO());
	}


	/**
	 * Return the homeowner memberId for the given residence.  
	 * Homeowner is the oldest person attached to the residence with status=1 in the _XR table.
	 * Used when adding home inventory to ensure the homeowner gets ownership of anything added by a shared user.
	 * @param req
	 * @return
	 */
	public String getHomeownerMemberId(String residenceId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(900);
		sql.append("select member_id as key from ").append(schema).append("rezdox_residence_member_xr ");
		sql.append("where residence_id=? and status_flg=1 "); //1=active, 2=shared
		sql.append("order by create_dt limit 1");
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(residenceId), new GenericVO());
		return (!data.isEmpty()) ? StringUtil.checkVal(data.get(0).getKey()) : "";
	}


	/**
	 * Retrieves the Residence Home Information form & saved form data
	 * 
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	protected FormVO retrieveHomeInfoForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Residence Form: " + formId);

		// Update Zestimate before form load, per requirements
		ResidenceVO residence = ((List<ResidenceVO>) req.getAttribute(RESIDENCE_DATA)).get(0);
		updateZestimate(residence);

		//capture a hash of the address so we know if it changes when saved. (re-Zestimate required if so)
		int addrHash = residence.getLocation().hashCode();
		log.debug("set addr hash=" + addrHash);
		req.getSession().setAttribute("REZ_ADDR_HASH", Integer.valueOf(addrHash));

		// Set the requried params
		GenericQueryVO query = new GenericQueryVO(formId);
		QueryParamVO param = new QueryParamVO(ResidenceColumnName.RESIDENCE_ID.name(), false);
		param.setValues(req.getParameterValues(RESIDENCE_ID));
		query.addConditional(param);

		// Get the form and the saved data for re-display onto the form.
		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadFormWithData(formId, req, query, ResidenceFormProcessor.class);
		req.setAttribute(FormAction.FORM_DATA, dc);

		return dc.getForm();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("residence build called");
		SubscriptionAction sa = new SubscriptionAction(getDBConnection(), getAttributes());	
		String memberId = RezDoxUtils.getMemberId(req);
		
		if (req.hasParameter("testMembership")) {
			putModuleData(canAddNewResidence(null, req),1, false, UPGRADE_MSG);
			return;

		} else if (req.hasParameter("homeInfo") || req.hasParameter("settings")) {
			saveForm(req);

		} else if (req.hasParameter("transferResidence")) {
			new ResidenceTransferAction(dbConn, attributes).initateResidenceTransfer(req);

		} else if (req.hasParameter("deleteResidence")) {
			String msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			String url = page.getFullPath();

			try {
				new DBProcessor(dbConn, getCustomSchema()).delete(new ResidenceVO(req));
			} catch (Exception e) {
				log.error("could not delete residence", e);
				msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}

			//downgrade this user's account if they just deleted their only residence (and are currently Hybrid role)
			//first get a count of any residences this user can see.  Only if it's zero do we want to downgrade.
			int count = sa.getResidenceUsage(memberId, ResidenceAction.STATUS_ACTIVE, ResidenceAction.STATUS_SHARED);
			SBUserRole role = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
			if (count == 0 && RezDoxUtils.REZDOX_RES_BUS_ROLE.equals(role.getRoleId())) {
				confirmMemberRole(req, RezDoxUtils.REZDOX_RES_BUS_ROLE, true);
				url = RezDoxUtils.MEMBER_ROOT_PATH; //redir to dashboard, they can no longer see the residences page.
			}

			sendRedirect(url, msg, req);

		} else {
			try {
				//get the residence 
				ResidenceVO residence = new ResidenceVO(req);
				boolean isNew = StringUtil.isEmpty(residence.getResidenceId());

				//get usage count before writing the table, the increment it.  This avoids read locks and uncomitted data issues	
				int count = sa.getUsageQty(memberId, Product.RESIDENCE);
				++count; //for the one we're adding.

				putModuleData(saveResidence(req), 1, false);

				if (isNew && count == 1) {
					SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
					changeMemebersRole(req, site);

					// This is the user's first residence, give a reward to anyone that might have invited them
					InvitationAction ia = new InvitationAction(dbConn, attributes);
					ia.applyInviterRewards(req, RezDoxUtils.REWARD_HOMEOWNER_INVITE);

					//Add First Residence Notifications
					sendFirstResidenceNotifications(site, memberId);

					//make sure the user has the proper role - this only causes further action when a business user adds their first residence.
					confirmMemberRole(req, RezDoxUtils.REZDOX_BUSINESS_ROLE, false);

				} else if (isNew && count > 1) {
					//award 100pts for subsequent residences
					awardPoints(memberId, req);
				}

			} catch (Exception e) {
				throw new ActionException("Could not save residence", e);
			}
		}
	}


	/**
	 * Change business users to be hybrid (biz+res) - that's all we care about here.
	 * @param req
	 */
	private void confirmMemberRole(ActionRequest req, String reqRoleId, boolean isHybridDowngrade) {
		SBUserRole role = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		if (role == null || !reqRoleId.equals(role.getRoleId())) 
			return;

		//downgrade to business user from hybrid - they no longer have residences to see.
		if (isHybridDowngrade) {
			role.setRoleId(RezDoxUtils.REZDOX_BUSINESS_ROLE);
			role.setRoleLevel(RezDoxUtils.REZDOX_BUSINESS_ROLE_LEVEL);
			role.setRoleName(RezDoxUtils.REZDOX_BUSINESS_ROLE_NAME);

			//change them to a hybrid role
		} else {
			role.setRoleId(RezDoxUtils.REZDOX_RES_BUS_ROLE);
			role.setRoleLevel(RezDoxUtils.REZDOX_RES_BUS_ROLE_LEVEL);
			role.setRoleName(RezDoxUtils.REZDOX_RES_BUS_ROLE_NAME);
		}

		//preserve the change to the DB
		try {
			ProfileRoleManager prm = new ProfileRoleManager();
			prm.removeRole(role.getProfileRoleId(), dbConn);
			prm.addRole(role.getProfileId(), RezDoxUtils.MAIN_SITE_ID, role.getRoleId(), SecurityController.STATUS_ACTIVE, getDBConnection());
			role.setProfileRoleId(prm.checkRole(role.getProfileId(),  RezDoxUtils.MAIN_SITE_ID, dbConn));
		} catch (DatabaseException e) {
			log.error("could not change users role", e);
		}

		req.getSession().setAttribute(Constants.ROLE_DATA, role);
	}


	/**
	 * @param memberId
	 */
	private void awardPoints(String memberId, ActionRequest req) {
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		try {
			ra.applyReward(Reward.CREATE_RES2.name(), memberId, req);
		} catch (ActionException e) {
			log.error("could not award reward points", e);
		}
	}


	/**
	 * Sends all the First Residence User Notifications.
	 * @param memberId
	 */
	private void sendFirstResidenceNotifications(SiteVO site, String memberId) {
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		notifyUtil.sendToMember(Message.NEW_RES_REWARDS, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_RES_CONNECT, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_RES_LOG, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_RES_INVENTORY, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_RES_EQUITY, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_RES_PIC, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_RES_PRIVACY, null, null, memberId);
	}

	/**
	 * updates the members record and the session with the new role.
	 * @param req 
	 * @param member 
	 * @param site 
	 * @param session 
	 * @return 
	 * @throws DatabaseException 
	 * 
	 */
	private void changeMemebersRole(ActionRequest req, SiteVO site) throws DatabaseException {
		ProfileRoleManager prm = new ProfileRoleManager();
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		log.debug("change role for site and member " + site.getSiteId()+"|"+ member.getProfileId());

		SBUserRole role = ((SBUserRole)session.getAttribute(Constants.ROLE_DATA));

		// Upgrade from Registered to Residence
		String newRoleId = RezDoxUtils.REZDOX_RESIDENCE_ROLE;
		String newRoleName = RezDoxUtils.REZDOX_RESIDENCE_ROLE_NAME;
		int newRoleLevel = RezDoxUtils.REZDOX_RESIDENCE_ROLE_LEVEL;

		// Upgrade from Business to Residence/Business Combo
		if (RezDoxUtils.REZDOX_BUSINESS_ROLE.equals(role.getRoleId())) {
			newRoleId = RezDoxUtils.REZDOX_RES_BUS_ROLE;
			newRoleName = RezDoxUtils.REZDOX_RES_BUS_ROLE_NAME;
			newRoleLevel = RezDoxUtils.REZDOX_RES_BUS_ROLE_LEVEL;
		}

		role.setRoleId(newRoleId);
		role.setRoleLevel(newRoleLevel); 
		role.setRoleName(newRoleName);
		role.setStatusId(SecurityController.STATUS_ACTIVE);
		prm.addRole(role, dbConn); //re-save the user's role with the new/elevated level.
		session.setAttribute(Constants.ROLE_DATA, role);
	}


	/**
	 * Saves a residence form builder form
	 */
	protected void saveForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());

		// Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, ResidenceFormProcessor.class);
	}

	/**
	 * Saves residence data
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	public ResidenceVO saveResidence(ActionRequest req) throws DatabaseException {
		// Get the residence data
		ResidenceVO residence = new ResidenceVO(req);
		boolean isNew = StringUtil.isEmpty(residence.getResidenceId());

		// Get geocode data for the residence
		LocationManager lm = new LocationManager(residence);
		GeocodeLocation gl = lm.geocode(attributes);
		residence.setLatitude(gl.getLatitude());
		residence.setLongitude(gl.getLongitude());

		// Save the residence & attributes records
		try {
			new DBProcessor(dbConn, getCustomSchema()).save(residence);
			req.setParameter(ResidenceAction.RESIDENCE_ID, residence.getResidenceId());
		} catch(Exception e) {
			throw new DatabaseException(e);
		}

		// Save the Residence/Member XR
		saveResidenceMemberXR(req, isNew);

		// Retrieve and save data from the various APIs
		retrieveApiData(req, residence, isNew);

		// Return the data
		return residence;
	}

	/**
	 * Save the XR record between the residence and member
	 * 
	 * @param newResidence
	 * @throws DatabaseException
	 */
	protected void saveResidenceMemberXR(ActionRequest req, boolean isNewResidence) throws DatabaseException {
		// Record already exists if this isn't a new residence, don't need another here
		if (!isNewResidence) return;

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_residence_member_xr ");
		sql.append("(residence_member_xr_id, member_id, residence_id, status_flg, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, RezDoxUtils.getMemberId(req));
			ps.setString(3, req.getParameter(ResidenceAction.RESIDENCE_ID));
			ps.setInt(4, 1); // Newly added residences are always active
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException("Could not save RezDox Member/Residence XR", sqle);
		}
	}

	/**
	 * Retrieves API data from Zillow, WalkScore, and SunNumber
	 * Stores the data out to residence attributes
	 * 
	 * NOTE: We try each piece individually so that an exception in one
	 * doesn't prevent retrieval of subsequent calls.
	 * 
	 * @param req
	 * @param residence
	 * @param isNewResidence
	 * @throws DatabaseException 
	 */
	protected void retrieveApiData(ActionRequest req, ResidenceVO residence, boolean isNewRes) 
			throws DatabaseException {
		// This data should only be retrieved when a residence is first created, or when the address changes
		int oldAddrHash = Convert.formatInteger((Integer)req.getSession().getAttribute("REZ_ADDR_HASH"));
		int newAddrHash = residence.getLocation().hashCode();
		log.debug(String.format("old address hash %d, new %d", oldAddrHash, newAddrHash));
		if (!isNewRes && oldAddrHash == newAddrHash) return;

		// Initialize list of attributes to save as a batch
		List<ResidenceAttributeVO> attributes = new ArrayList<>();

		// Get the Zillow Data
		try {
			ZillowPropertyVO property = new ZillowAPIManager().retrievePropertyDetails(residence);
			attributes.addAll(mapZillowDataToAttributes(property, residence, req));
		} catch (Exception e) {
			log.error("Unable to retrieve Zillow data for residence", e);
			attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE, "0"));
		}

		// Get the Sun Number Data
		String sunNumberVal = null;
		try {
			SunNumberVO sunNumber = new SunNumberAPIManager().retrieveSunNumber(residence);
			sunNumberVal = sunNumber.getSunNumber();
		} catch (Exception e) {
			log.error("Unable to retrieve Sun Number data for residence", e);
		}
		attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_SUN_NUMBER, StringUtil.isEmpty(sunNumberVal) ? "0" : sunNumberVal));		

		// Get the Walk Score Data
		String walkScoreVal = null;
		String transitScoreVal = null;
		try {
			WalkScoreVO walkScore = new WalkScoreAPIManager().retrieveWalkScore(residence);
			walkScoreVal = Convert.formatInteger(walkScore.getWalkscore()).toString();
			transitScoreVal = walkScore.getTransit() != null ? Convert.formatInteger(walkScore.getTransit().getScore()).toString() : null;
		} catch (Exception e) {
			log.error("Unable to retrieve Walk Score data for residence", e);
		}
		attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_WALK_SCORE, StringUtil.isEmpty(walkScoreVal) ? "0" : walkScoreVal));
		attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_TRANSIT_SCORE, StringUtil.isEmpty(transitScoreVal) ? "0" : transitScoreVal));

		//delete the records we're about to add, to avoid duplicates.  DBProcessor does not support a hybrid batch add+update.
		deleteExistingAttributes(residence.getResidenceId(), attributes);

		//preserve the list of slugs saved here, so they're not overwritten by the form handler
		req.setAttribute("savedSlugs", attributes);

		// Save the retrieved attributes
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		try {
			dbp.executeBatch(attributes);
		} catch(Exception e) {
			throw new DatabaseException(e);
		}
	}


	/**
	 * deletes the attributes for this residence based on slugText
	 * @param slugs
	 */
	protected void deleteExistingAttributes(String residenceId, List<ResidenceAttributeVO> attributes) {
		if (attributes == null || attributes.isEmpty()) return;
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(getCustomSchema()).append("REZDOX_RESIDENCE_ATTRIBUTE where slug_txt in (");
		DBUtil.preparedStatmentQuestion(attributes.size(), sql);
		sql.append(") and residence_id=?");
		log.debug(sql);

		int x=1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (ResidenceAttributeVO vo : attributes)
				ps.setString(x++, vo.getSlugText());
			ps.setString(x, residenceId);
			int cnt = ps.executeUpdate();
			log.debug("deleted " + cnt + " residence attributes");
		} catch (SQLException sqle) {
			log.error("could not delete residence attributes", sqle);
		}
	}


	/**
	 * Saves data from the Zillow property lookup to the residence attributes table
	 * 
	 * @param property
	 * @param residence
	 */
	private List<ResidenceAttributeVO> mapZillowDataToAttributes(ZillowPropertyVO property, ResidenceVO residence, ActionRequest req) {
		List<ResidenceAttributeVO> attributes = new ArrayList<>();
		Map<String, String> zillowData = property.getExtendedData();

		// Create attributes from Zillow data where there is a matching slug text.
		// These are the only Zillow extended fields we care about.
		for (String slugText : getSlugTxtList(req)) {
			String valueText = zillowData.get(slugText);

			if (!StringUtil.isEmpty(valueText)) {
				attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), slugText, valueText));
				log.debug(String.format("got from Zillow: %s=%s", slugText, valueText));
			}
		}

		// Add additional non-extended attributes
		ResidenceAttributeVO attribute = new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE, StringUtil.checkVal(property.getValueEstimate(), null));
		attributes.add(attribute);

		return attributes;
	}

	/**
	 * Gets a list of slugs for the residence form.
	 * 
	 * @return
	 */
	private List<String> getSlugTxtList(ActionRequest req) {
		List<String> slugs = new ArrayList<>();

		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadForm(RezDoxUtils.getFormId(getAttributes()), req);
		FormVO residenceForm = dc.getForm();

		// Loop over the templates to get form field slug texts
		for (FormTemplateVO template : residenceForm.getTemplateList()) {
			for (FormFieldVO field : template.getFieldList()) {
				if (!StringUtil.isEmpty(field.getSlugTxt())) {
					slugs.add(field.getSlugTxt());	
				}
			}
		}

		return slugs;
	}

	/**
	 * Update the Zestimate data for a residence
	 * @param residence
	 */
	protected void updateZestimate(ResidenceVO residence) {
		StringBuilder sql = new StringBuilder(250);
		String schema = getCustomSchema();
		sql.append("select attribute_id, residence_id, slug_txt, value_txt, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_attribute ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" residence_id = ? and slug_txt = ? ");

		List<Object> params = Arrays.asList(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE);

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<ResidenceAttributeVO> attr = dbp.executeSelect(sql.toString(), params, new ResidenceAttributeVO());

		ResidenceAttributeVO zestimate;
		int daysSinceLastUpdate;
		if (attr.isEmpty()) {
			// If no previous Zestimate recorded, create a new one
			zestimate = new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE, "0");
			daysSinceLastUpdate = 1000;

		} else {
			// Check to make sure we are updating at most once per day
			zestimate = attr.get(0);
			Date lastUpdate = zestimate.getUpdateDate() == null ? zestimate.getCreateDate() : zestimate.getUpdateDate();
			LocalDate now = LocalDate.now();
			LocalDate prev = lastUpdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			daysSinceLastUpdate = Period.between(prev, now).getDays();
		}

		// Update only when required
		if (daysSinceLastUpdate >= 1)
			applyZestimateUpdate(residence, zestimate);
	}


	/**
	 * Applies an update to the residence Zestimate record
	 * @param residence
	 * @param zestimate
	 */
	private void applyZestimateUpdate(ResidenceVO residence, ResidenceAttributeVO zestimate) {
		try {
			ZillowPropertyVO property = new ZillowAPIManager().retrieveZillowId(residence);
			zestimate.setValueText(StringUtil.checkVal(property.getValueEstimate(), null));
		} catch (InvalidDataException e) {
			log.error("Could not retrieve Zestimate data for the residence", e);
		}

		// Save the required record, whether or not the API call was successful. This could be a new record.
		try {
			DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
			dbp.save(zestimate);
		} catch (Exception e) {
			log.error("Could not save the updated Zestimate", e);
		}
	}
}