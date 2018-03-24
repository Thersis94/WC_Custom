package com.rezdox.action;

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
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTemplateVO;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.security.SBUserRole;

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
	public static final String REZDOX_RESIDENCE_ROLE_ID = "REZDOX_RESIDENCE";
	public static final String REZDOX_RESIDENCE_ROLE_NAME = "RezDox Residence Role";
	public static final int REZDOX_RESIDENCE_ROLE_LEVEL = 25;
	public static final String PRIMARY_RESIDENCE = " Primary Residence";
	public static final String UPGRADE_MSG = "You have reached your maximum residences. Please purchase a residence upgrade to continue.";
	public static final String SLUG_RESIDENCE_ZESTIMATE = "RESIDENCE_ZESTIMATE";
	public static final String SLUG_RESIDENCE_WALK_SCORE = "RESIDENCE_WALK_SCORE";
	public static final String SLUG_RESIDENCE_SUN_NUMBER = "RESIDENCE_SUN_NUMBER";
	
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
		List<ResidenceVO> residenceList = retrieveResidences(req);
		String residenceId = req.getParameter(RESIDENCE_ID, "");

		if ("new".equalsIgnoreCase(residenceId) && !canAddNewResidence(residenceList, req)) {
			// When adding a new residence, check to make sure the member has not reached their limit
			sendRedirect(RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH, UPGRADE_MSG, req);
		} else if (req.hasParameter("homeInfo") || req.hasParameter("settings")) {
			// Set the data to be returned
			req.setAttribute(RESIDENCE_DATA, residenceList);
			putModuleData(retrieveHomeInfoForm(req));
		} else {
			putModuleData(residenceList, residenceList.size(), false);
		}
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
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String residenceId = req.getParameter(RESIDENCE_ID);
		
		// Using pivot table on the attributes to get additional data for display
		StringBuilder sql = new StringBuilder(900);
		sql.append("select r.residence_id, residence_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt) as update_dt, ");
		sql.append("privacy_flg, for_sale_dt, last_sold_dt, beds_no, baths_no, coalesce(f_sqft_no, 0) as sqft_no, zestimate_no ");
		sql.append("from ").append(schema).append("rezdox_residence r inner join ");
		sql.append(schema).append("rezdox_residence_member_xr m on r.residence_id = m.residence_id ");
		sql.append("left join (SELECT * FROM crosstab('SELECT residence_id, slug_txt, value_txt FROM ").append(schema).append("rezdox_residence_attribute ORDER BY 1', ");
		sql.append("'SELECT DISTINCT slug_txt FROM ").append(schema).append("rezdox_residence_attribute WHERE slug_txt in (''bedrooms'',''bathrooms'',''finishedSqFt'', ''RESIDENCE_ZESTIMATE'') ORDER BY 1') ");
		sql.append("AS (residence_id text, baths_no float, beds_no int, f_sqft_no int, zestimate_no float) ");
		sql.append(") ra on r.residence_id = ra.residence_id ");
		sql.append("where member_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));
		
		// Return only a specific residence if selected
		if (!StringUtil.isEmpty(residenceId)) {
			sql.append("and r.residence_id = ? ");
			params.add(residenceId);
		}
		
		DBProcessor dbp = new DBProcessor(dbConn);
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
		} else {
			try {
				//get the residence 
				ResidenceVO residence = new ResidenceVO(req);
				boolean newResidence = StringUtil.isEmpty(residence.getResidenceId());
				
				putModuleData(saveResidence(req), 1, false);
				SMTSession session = req.getSession();
				MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

				SubscriptionAction sa = new SubscriptionAction();
				sa.setDBConnection(dbConn);
				sa.setAttributes(attributes);				
				int count = sa.getResidenceUsage(member.getMemberId());
				
				if (newResidence && count == 1) {
					SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
					req.setSession(changeMemebersRole(session, site, member));
				}

			} catch (Exception e) {
				throw new ActionException("Could not save residence", e);
			}
		}
	}
	
	/**
	 * updates the members record and the session with the new role.
	 * @param member 
	 * @param site 
	 * @param session 
	 * @return 
	 * @throws DatabaseException 
	 * 
	 */
	private SMTSession changeMemebersRole(SMTSession session, SiteVO site, MemberVO member) throws DatabaseException {
		//if new and final count is one change roll
	
		ProfileRoleManager prm = new ProfileRoleManager();
		log.debug("change role for site and member " + site.getSiteId()+"|"+ member.getProfileId());
		SBUserRole role = ((SBUserRole)session.getAttribute(Constants.ROLE_DATA));
		prm.removeRole(role.getProfileRoleId(), dbConn);
		prm.addRole( member.getProfileId(), site.getSiteId(), REZDOX_RESIDENCE_ROLE_ID, 20, dbConn);
		
		role.setRoleId(REZDOX_RESIDENCE_ROLE_ID);
		role.setRoleLevel(REZDOX_RESIDENCE_ROLE_LEVEL); 
		role.setRoleName(REZDOX_RESIDENCE_ROLE_NAME);
		
		role.setProfileRoleId(prm.checkRole(member.getProfileId(),  site.getSiteId(), dbConn));
		session.setAttribute(Constants.ROLE_DATA, role);				
		return session;
		
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
	 * @throws InvalidDataException 
	 */
	public ResidenceVO saveResidence(ActionRequest req) throws DatabaseException, InvalidDataException {
		// Get the residence data
		ResidenceVO residence = new ResidenceVO(req);
		
		boolean newResidence = StringUtil.isEmpty(residence.getResidenceId());
		
		// If this is a new residence, lookup residential API data + extended data for attributes
		ZillowPropertyVO property = null;
		SunNumberVO sunNumber = null;
		WalkScoreVO walkScore = null;
		if (newResidence) {
			// Zillow
			String zAddress = residence.getAddress();
			
			String[] addressTokens = zAddress.split(",");
			residence.setAddress(addressTokens[0].trim());
			residence.setCity(addressTokens[1].trim());
			residence.setState(addressTokens[2].trim());
			residence.setCountry(addressTokens[3].trim());
			
			ZillowAPIManager zillow = new ZillowAPIManager();
			property = zillow.retrievePropertyDetails(residence);
			residence.setLatitude(property.getLatitude());
			residence.setLongitude(property.getLongitude());
			
			// Sun Number
			SunNumberAPIManager sunNumberApi = new SunNumberAPIManager();
			sunNumber = sunNumberApi.retrieveSunNumber(residence);
			
			// Walk Score
			WalkScoreAPIManager walkScoreApi = new WalkScoreAPIManager();
			walkScore = walkScoreApi.retrieveWalkScore(residence);
		}
		
		// Save the residence & attributes records
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(residence);
			req.setParameter(ResidenceAction.RESIDENCE_ID, residence.getResidenceId());
			
			// This must happen here to ensure we have a residence_id first, to pass to the attributes
			if (property != null) {
				List<ResidenceAttributeVO> attributes = mapZillowDataToAttributes(property, residence, req);
				attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_SUN_NUMBER, sunNumber.getSunNumber()));
				attributes.add(new ResidenceAttributeVO(residence.getResidenceId(), SLUG_RESIDENCE_WALK_SCORE, Convert.formatInteger(walkScore.getWalkscore()).toString()));
				dbp.executeBatch(attributes);	
			}
			
		} catch(Exception e) {
			throw new DatabaseException(e);
		}
		
		// Save the Residence/Member XR
		saveResidenceMemberXR(req, newResidence);
		
		// Return the data
		return residence;
	}
	
	/**
	 * Save the XR record between the residence and member
	 * 
	 * @param newResidence
	 * @throws DatabaseException
	 */
	protected void saveResidenceMemberXR(ActionRequest req, boolean newResidence) throws DatabaseException {
		// Record already exists if this isn't a new residence, don't need another here
		if (!newResidence) return;
		
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
		StringBuilder sql = new StringBuilder(150);
		sql.append("select attribute_id, residence_id, slug_txt, value_txt, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("rezdox_residence_attribute ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" residence_id = ? and slug_txt = ? ");
		
		List<Object> params = new ArrayList<>();
		params.addAll(Arrays.asList(residence.getResidenceId(), SLUG_RESIDENCE_ZESTIMATE));
		
		DBProcessor dbp = new DBProcessor(dbConn);
		List<ResidenceAttributeVO> attr = dbp.executeSelect(sql.toString(), params, new ResidenceAttributeVO());
		
		// Check to make sure we are updating at most once per day
		ResidenceAttributeVO zestimate = attr.get(0);
		Date lastUpdate = zestimate.getUpdateDate() == null ? zestimate.getCreateDate() : zestimate.getUpdateDate();
		LocalDate now = LocalDate.now();
		LocalDate prev = lastUpdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int daysSinceLastUpdate = Period.between(prev, now).getDays();
		
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
			ZillowAPIManager zillowApi = new ZillowAPIManager();
			ZillowPropertyVO property = zillowApi.retrieveZillowId(residence);
			zestimate.setValueText(property.getValueEstimate().toString());
			
			DBProcessor dbp = new DBProcessor(dbConn);
			dbp.save(zestimate);
		} catch (Exception e) {
			log.error("Could not update zestimate value for residence", e);
		}
	}
}