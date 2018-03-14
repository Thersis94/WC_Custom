package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.data.BusinessFormProcessor;
import com.rezdox.vo.BusinessAttributeVO;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO.Group;
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
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.LocationManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;

/****************************************************************************
 * <b>Title</b>: BusinessAction.java<p/>
 * <b>Description: Manages member interactions with a business.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 8, 2018
 ****************************************************************************/
public class BusinessAction extends SBActionAdapter {
	
	public static final String BUSINESS_DATA = "businessData";
	public static final String REQ_BUSINESS_ID = "businessId";
	public static final String UPGRADE_MSG = "You have reached your maximum businesses. Please purchase a business upgrade to continue.";
	private static final Class<BusinessFormProcessor> BUSINESS_FORM_PROCESSOR = BusinessFormProcessor.class;
	
	public enum BusinessColumnName {
		BUSINESS_ID, CREATE_DT, UPDATE_DT
	}
	
	/**
	 * Business status
	 */
	public enum BusinessStatus {
		INACTIVE(0), ACTIVE(1), PENDING(2);

		private int status;
		
		private BusinessStatus(int status) {
			this.status = status;
		}
		
		public int getStatus() { return status; }
	}
	
	/**
	 * Required attribute fields in the attributes table
	 */
	public enum BusinessRequiredAttribute {
		BUSINESS_SUMMARY
	}

	public BusinessAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BusinessAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public BusinessAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
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
		List<BusinessVO> businessList = retrieveBusinesses(req);
		String businessId = req.getParameter(REQ_BUSINESS_ID, "");

		if ("new".equalsIgnoreCase(businessId) && !canAddNewBusiness(req)) {
			// When adding a new business, check to make sure the member has not reached their limit
			sendRedirect(RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH, UPGRADE_MSG, req);
		} else if (req.hasParameter("businessInfo")) {
			// Set the data to be returned
			req.setAttribute(BUSINESS_DATA, businessList);
			putModuleData(retrieveBusinessInfoForm(req));
		} else {
			putModuleData(businessList, businessList.size(), false);
		}
	}
	
	/**
	 * Validates whether a new business can be added by the member.
	 * 
	 * @param businessList
	 * @param member
	 * @return true if business can be added, false if not
	 * @throws ActionException 
	 */
	private boolean canAddNewBusiness(ActionRequest req) throws ActionException {
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
			
		// Validate whether the member needs a business upgrade
		SubscriptionAction sa = (SubscriptionAction) ActionControllerFactoryImpl.loadAction(SubscriptionAction.class.getName(), this);
		boolean needsUpgrade = sa.checkUpgrade(member, Group.BU);
		
		// A business can be added if they don't need an upgrade
		return !needsUpgrade;
	}
	
	/**
	 * Retrieves base business data
	 * 
	 * @param req
	 * @return
	 */
	@DatabaseNote(type = DBType.POSTGRES)
	protected List<BusinessVO> retrieveBusinesses(ActionRequest req) {
		String schema = getCustomSchema();
		String businessId = req.getParameter(REQ_BUSINESS_ID);
		
		// Show only businesses that the member has access to
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		
		// Using pivot table on the attributes to get additional data for display,
		// There may be additional attributes at some point beyond summary_txt.
		StringBuilder sql = new StringBuilder(900);
		sql.append("select b.business_id, business_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, ");
		sql.append("latitude_no, longitude_no, main_phone_txt, alt_phone_txt, email_address_txt, website_url, photo_url, ad_file_url, ");
		sql.append("privacy_flg, bsc.business_category_cd as sub_category_cd, bc.business_category_cd as category_cd, b.create_dt, ");
		sql.append("coalesce(b.update_dt, b.create_dt) as update_dt, summary_txt, m.status_flg ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business b inner join ");
		sql.append(schema).append("rezdox_business_member_xr m on b.business_id = m.business_id and m.status_flg > ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(" (SELECT * FROM crosstab('SELECT business_id, slug_txt, value_txt FROM ").append(schema).append("rezdox_business_attribute ORDER BY 1', ");
		sql.append("'SELECT DISTINCT slug_txt FROM ").append(schema).append("rezdox_business_attribute WHERE slug_txt in (''BUSINESS_SUMMARY'') ORDER BY 1') ");
		sql.append("AS (business_id text, summary_txt text) ");
		sql.append(") ba on b.business_id = ba.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category_xr bcx on b.business_id = bcx.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bsc on bcx.business_category_cd = bsc.business_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bc on bsc.parent_cd = bc.business_category_cd ");
		sql.append("where member_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(BusinessStatus.INACTIVE.getStatus());
		params.add(member.getMemberId());
		
		// Return only a specific business if selected
		if (!StringUtil.isEmpty(businessId)) {
			sql.append("and b.business_id = ? ");
			params.add(businessId);
		}
		
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new BusinessVO());
	}
	
	/**
	 * Retrieves the Business Information form & saved form data
	 * 
	 * @param req
	 */
	protected FormVO retrieveBusinessInfoForm(ActionRequest req) {
		String formId = getFormId();
		log.debug("Retrieving Business Form: " + formId);
		
		// Set the requried params
		GenericQueryVO query = new GenericQueryVO(formId);
		QueryParamVO param = new QueryParamVO(BusinessColumnName.BUSINESS_ID.name(), false);
		param.setValues(req.getParameterValues(REQ_BUSINESS_ID));
		query.addConditional(param);
		
		// Get the form and the saved data for re-display onto the form.
		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadFormWithData(formId, req, query, BUSINESS_FORM_PROCESSOR);
		req.setAttribute(FormAction.FORM_DATA, dc);
		
		return dc.getForm();
	}
	
	/**
	 * Get's the form id associated to the action
	 * 
	 * @return
	 */
	private String getFormId() {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		return (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.hasParameter("businessInfo")) {
			saveForm(req);
		} else {
			try {
				putModuleData(saveBusiness(req), 1, false);
			} catch (Exception e) {
				throw new ActionException("Could not save business", e);
			}
		}
	}
	
	/**
	 * Saves a business form builder form
	 * 
	 * @param req
	 */
	protected void saveForm(ActionRequest req) {
		String formId = getFormId();

		// Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, BUSINESS_FORM_PROCESSOR);
	}
	
	/**
	 * Saves business data
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	public BusinessVO saveBusiness(ActionRequest req) throws DatabaseException {
		// Get the business data
		BusinessVO business = new BusinessVO(req);
		boolean newBusiness = StringUtil.isEmpty(business.getBusinessId());
		
		// Get geocode data for the business address
		LocationManager lm = new LocationManager(business);
		GeocodeLocation gl = lm.geocode(attributes);
		business.setLatitude(gl.getLatitude());
		business.setLongitude(gl.getLongitude());
		
		// Save the business records
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(business);
			req.setParameter(BusinessAction.REQ_BUSINESS_ID, business.getBusinessId());
			
			// If this is a new business, add required attribute records
			if (newBusiness) {
				dbp.executeBatch(createRequiredAttributes(business));
			}
		} catch(Exception e) {
			throw new DatabaseException(e);
		}
		
		// Save the Business/Member XR and Category XR
		saveBusinessMemberXR(req, newBusiness);
		saveBusinessCategoryXR(req);
		
		// Return the data
		return business;
	}
	
	/**
	 * Save the XR record between the business and member
	 * 
	 * @param newBusiness
	 * @throws DatabaseException
	 */
	protected void saveBusinessMemberXR(ActionRequest req, boolean newBusiness) throws DatabaseException {
		// Record already exists if this isn't a new business, don't need another here
		if (!newBusiness) return;
		
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_business_member_xr (business_member_xr_id, ");
		sql.append("member_id, business_id, status_flg, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		log.debug(sql);
		
		// Get the member adding this business
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, member.getMemberId());
			ps.setString(3, req.getParameter(BusinessAction.REQ_BUSINESS_ID));
			ps.setInt(4, BusinessStatus.PENDING.getStatus()); // Newly added businesses are always pending status, until admin reviews
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not save RezDox Member/Business XR ", sqle);
			throw new DatabaseException(sqle);
		}
	}
	
	/**
	 * Save the XR record between the business and business categories
	 * 
	 * @throws DatabaseException
	 */
	protected void saveBusinessCategoryXR(ActionRequest req) throws DatabaseException {
		String schema = getCustomSchema();
		deleteBusinessCategoryXR(req);
		
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_business_category_xr (business_category_xr_id, ");
		sql.append("business_id, business_category_cd, create_dt) ");
		sql.append("values (?,?,?,?)");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter(BusinessAction.REQ_BUSINESS_ID));
			ps.setString(3, req.getParameter("subCategoryCd"));
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}
	
	/**
	 * Remove the XR record(s) between the business and business categories
	 * 
	 * @throws DatabaseException
	 */
	private void deleteBusinessCategoryXR(ActionRequest req) throws DatabaseException {
		String schema = getCustomSchema();
		StringBuilder sqlDelete = new StringBuilder(100);
		sqlDelete.append(DBUtil.DELETE_CLAUSE).append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business_category_xr ");
		sqlDelete.append(DBUtil.WHERE_CLAUSE).append("business_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sqlDelete.toString())) {
			ps.setString(1, req.getParameter(BusinessAction.REQ_BUSINESS_ID));
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}
	
	/**
	 * Creates empty required attributes for new businesses, to be added to the business attributes table
	 * 
	 * @param business
	 * @throws DatabaseException 
	 */
	private List<BusinessAttributeVO> createRequiredAttributes(BusinessVO business) {
		List<BusinessAttributeVO> attributes = new ArrayList<>();
		
		// Create attributes for all that are required.
		for (BusinessRequiredAttribute attr : BusinessRequiredAttribute.values()) {
			BusinessAttributeVO attribute = new BusinessAttributeVO(business.getBusinessId(), attr.name(), "");
			attributes.add(attribute);
		}
		
		return attributes;
	}
}