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

//SMTBaseLIbs
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
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
//WC Custom
import com.rezdox.api.SunNumberAPIManager;
import com.rezdox.api.WalkScoreAPIManager;
import com.rezdox.api.ZillowAPIManager;
import com.rezdox.data.ResidenceFormProcessor;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO.Group;
import com.rezdox.vo.ResidenceAttributeVO;
import com.rezdox.vo.ResidenceVO;
import com.rezdox.vo.SunNumberVO;
import com.rezdox.vo.WalkScoreVO;
import com.rezdox.vo.ZillowPropertyVO;

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
		boolean needsUpgrade = sa.checkUpgrade(member, Group.HO);

		// Set default residence name per requirements
		if (!needsUpgrade) {
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

		// Using pivot table on the attributes to get additional data for display
		StringBuilder sql = new StringBuilder(900);
		sql.append("select r.residence_id, residence_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt) as update_dt, ");
		sql.append("privacy_flg, for_sale_dt, last_sold_dt, beds_no, baths_no, coalesce(f_sqft_no, 0) as sqft_no, zestimate_no, projects_total ");
		sql.append("from ").append(schema).append("rezdox_residence r inner join ");
		sql.append(schema).append("rezdox_residence_member_xr m on r.residence_id = m.residence_id and status_flg=1 ");
		sql.append("left join (SELECT * FROM crosstab('SELECT residence_id, slug_txt, value_txt FROM ").append(schema).append("rezdox_residence_attribute ORDER BY 1', ");
		sql.append("'SELECT DISTINCT slug_txt FROM ").append(schema).append("rezdox_residence_attribute WHERE slug_txt in (''bedrooms'',''bathrooms'',''finishedSqFt'', ''RESIDENCE_ZESTIMATE'') ORDER BY 1') ");
		sql.append("AS (residence_id text, baths_no float, beds_no int, f_sqft_no int, zestimate_no float) ");
		sql.append(") ra on r.residence_id = ra.residence_id ");
		sql.append("left join (select residence_id, sum(total_no) as projects_total ");
		sql.append("from ").append(schema).append("rezdox_project where project_category_cd = 'IMPROVEMENT' ");
		sql.append("group by residence_id) impr on r.residence_id = impr.residence_id "); 
		sql.append("where member_id = ? ");

		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));

		// Return only a specific residence if selected
		if (!StringUtil.isEmpty(residenceId)) {
			sql.append("and r.residence_id = ? ");
			params.add(residenceId);
		}

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
		sql.append("select a.* from ").append(schema).append("rezdox_residence a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_residence_member_xr b ");
		sql.append("on a.residence_id=b.residence_id and b.member_id=? and b.status_flg=1");
		if (!StringUtil.isEmpty(residenceId)) sql.append(" where a.residence_id=?");
		sql.append("order by a.residence_nm");

		List<Object> params = new ArrayList<>();
		params.add(memberId);
		if (!StringUtil.isEmpty(residenceId)) params.add(residenceId);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		return dbp.executeSelect(sql.toString(), params, new ResidenceVO());
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
		if (req.hasParameter("homeInfo") || req.hasParameter("settings")) {
			saveForm(req);

		} else if (req.hasParameter("transferResidence")) {
			new ResidenceTransferAction(dbConn, attributes).initateResidenceTransfer(req);

		} else if (req.hasParameter("deleteResidence")) {
			String msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			try {
				new DBProcessor(dbConn, getCustomSchema()).delete(new ResidenceVO(req));
			} catch (Exception e) {
				log.error("could not delete residence", e);
				msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			sendRedirect(page.getFullPath(), msg, req);

		} else {
			try {
				//get the residence 
				ResidenceVO residence = new ResidenceVO(req);
				boolean isNew = StringUtil.isEmpty(residence.getResidenceId());

				putModuleData(saveResidence(req), 1, false);

				SubscriptionAction sa = new SubscriptionAction();
				sa.setDBConnection(dbConn);
				sa.setAttributes(attributes);				
				int count = sa.getResidenceUsage(RezDoxUtils.getMemberId(req));

				if (isNew && count == 1) {
					SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
					changeMemebersRole(req, site);

					// This is the user's first residence, give a reward to anyone that might have invited them
					InvitationAction ia = new InvitationAction(dbConn, attributes);
					ia.applyInviterRewards(req, RezDoxUtils.REWARD_HOMEOWNER_INVITE);
				}

			} catch (Exception e) {
				throw new ActionException("Could not save residence", e);
			}
		}
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
	 * @param residence
	 * @param newResidence
	 * @throws DatabaseException 
	 */
	protected void retrieveApiData(ActionRequest req, ResidenceVO residence, boolean newResidence) throws DatabaseException {
		// This data should only be retrieved when a residence is first created
		if (!newResidence) return;

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

		// Save the retrieved attributes
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		try {
			dbp.executeBatch(attributes);
		} catch(Exception e) {
			throw new DatabaseException(e);
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
				ResidenceAttributeVO attribute = new ResidenceAttributeVO(residence.getResidenceId(), slugText, valueText);
				attributes.add(attribute);
			}
		}

		// Add additional non-extended attributes
		ResidenceAttributeVO attribute = new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE, property.getValueEstimate().toString());
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
	 * 
	 * @param req
	 */
	protected void updateZestimate(ResidenceVO residence) {
		StringBuilder sql = new StringBuilder(250);
		String schema = getCustomSchema();
		sql.append("select attribute_id, residence_id, slug_txt, value_txt, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_attribute ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" residence_id = ? and slug_txt = ? ");

		List<Object> params = new ArrayList<>();
		params.addAll(Arrays.asList(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE));

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<ResidenceAttributeVO> attr = dbp.executeSelect(sql.toString(), params, new ResidenceAttributeVO());

		ResidenceAttributeVO zestimate;
		int daysSinceLastUpdate;
		if (attr.isEmpty()) {
			// If no previous Zestimate recorded, create a new one
			zestimate = new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE, "0");
			daysSinceLastUpdate = 1;

		} else {
			// Check to make sure we are updating at most once per day
			zestimate = attr.get(0);
			Date lastUpdate = zestimate.getUpdateDate() == null ? zestimate.getCreateDate() : zestimate.getUpdateDate();
			LocalDate now = LocalDate.now();
			LocalDate prev = lastUpdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			daysSinceLastUpdate = Period.between(prev, now).getDays();
		}

		// Update only when required
		if (daysSinceLastUpdate >= 1) {
			applyZestimateUpdate(residence, zestimate);
		}
	}

	/**
	 * Applies an update to the residence Zestimate record
	 * 
	 * @param residence
	 * @param zestimate
	 */
	private void applyZestimateUpdate(ResidenceVO residence, ResidenceAttributeVO zestimate) {
		try {
			ZillowPropertyVO property = new ZillowAPIManager().retrieveZillowId(residence);
			zestimate.setValueText(property.getValueEstimate().toString());
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