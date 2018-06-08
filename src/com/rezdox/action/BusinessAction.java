package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.rezdox.action.RezDoxNotifier.Message;
import com.rezdox.data.BusinessFormProcessor;
import com.rezdox.vo.BusinessAttributeVO;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.ConnectionReportVO;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO.Group;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
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
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SBUserRoleContainer;

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
	public static final String REQ_BUSINESS_INFO = "businessInfo";
	public static final String REQ_SETTINGS = "settings";
	public static final String ATTR_GET_FOR_MEMBER = "getForMember";
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
		// If hitting this action with a residence role or registered role, they are adding a new business
		// Their role will be upgraded appropriately after adding a new business
		SBUserRole role = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA));
		if ((RezDoxUtils.REZDOX_RESIDENCE_ROLE.equals(role.getRoleId()) || SBUserRoleContainer.REGISTERED_USER_ROLE_LEVEL == role.getRoleLevel())
				&& !req.hasParameter("storeFront")) {
			req.setParameter(REQ_BUSINESS_ID, "new");
			req.setParameter(REQ_BUSINESS_INFO, "1");
		}

		List<BusinessVO> businessList = retrieveBusinesses(req);
		String businessId = req.getParameter(REQ_BUSINESS_ID, "");

		if ("new".equalsIgnoreCase(businessId) && !canAddNewBusiness(req)) {
			// When adding a new business, check to make sure the member has not reached their limit
			sendRedirect(RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH, UPGRADE_MSG, req);
		} else if (req.hasParameter(REQ_BUSINESS_INFO) || req.hasParameter(REQ_SETTINGS)) {
			// Set the data to be returned
			req.setAttribute(BUSINESS_DATA, businessList);
			putModuleData(retrieveBusinessInfoForm(req));
		} else {
			putModuleData(businessList, businessList.size(), false);
		}
		
		if (req.hasParameter("storeFront")) {
			ConnectionAction ca = new ConnectionAction(dbConn, attributes);
			List<ConnectionReportVO> connections = ca.generateConnections(RezDoxUtils.getMemberId(req), ConnectionAction.MEMBER, req);
			req.setAttribute("isConnected", !connections.isEmpty());
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
	 * Returns base business sql query. The where clause is left up to the calling method.
	 * Note that the xr status flag parameter is added here and required for all queries.
	 * 
	 * @return
	 */
	private StringBuilder getBaseBusinessSql() {
		String schema = getCustomSchema();

		StringBuilder sql = new StringBuilder(1400);
		sql.append("select b.business_id, business_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, ");
		sql.append("latitude_no, longitude_no, main_phone_txt, alt_phone_txt, b.email_address_txt, website_url, photo_url, ");
		sql.append("b.privacy_flg, bsc.business_category_cd as sub_category_cd, bc.business_category_cd as category_cd, bc.category_nm, b.create_dt, ");
		sql.append("coalesce(b.update_dt, b.create_dt) as update_dt, m.member_id, m.profile_id, bm.status_flg, ");
		sql.append("attribute_id, slug_txt, value_txt, total_reviews_no, avg_rating_no, p.photo_id, p.desc_txt, p.image_url ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business b inner join ");
		sql.append(schema).append("rezdox_business_member_xr bm on b.business_id = bm.business_id and bm.status_flg >= ? ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_member m on bm.member_id = m.member_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category_xr bcx on b.business_id = bcx.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bsc on bcx.business_category_cd = bsc.business_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_category bc on bsc.parent_cd = bc.business_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business_attribute ba on b.business_id = ba.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_photo p on b.business_id = p.business_id ");
		// Review summary data
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("(");
		sql.append("select business_id, cast(count(*) as integer) as total_reviews_no, cast(sum(rating_no) as double precision) / count(*) as avg_rating_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_business_review where parent_id is null group by business_id ");
		sql.append(") as rev on b.business_id = rev.business_id ");

		return sql;
	}

	/**
	 * Retrieves base business data for a member
	 * 
	 * @param req
	 * @return
	 */
	protected List<BusinessVO> retrieveBusinesses(ActionRequest req) {
		String businessId = req.getParameter(REQ_BUSINESS_ID);

		// Use the base query
		StringBuilder sql = getBaseBusinessSql();

		// Get everything that is active or pending
		List<Object> params = new ArrayList<>();
		params.add(BusinessStatus.ACTIVE.getStatus());

		// Restrict to the member owner when editing business details
		if (req.hasParameter(REQ_BUSINESS_INFO) || req.hasParameter(REQ_SETTINGS) || StringUtil.isEmpty(businessId) || req.getAttribute(ATTR_GET_FOR_MEMBER) != null) {
			sql.append("where bm.member_id = ? ");
			params.add(RezDoxUtils.getMemberId(req));
		} else if (!StringUtil.isEmpty(businessId)) {
			sql.append("where 1=1 ");
		}

		// Return only a specific business if selected
		if (!StringUtil.isEmpty(businessId)) {
			sql.append("and b.business_id = ? ");
			params.add(businessId);
		}

		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new BusinessVO());
	}


	/**
	 * Retrieve the core business VO/record for the given IDs.
	 * Referenced from Home History (Projects)
	 * @param req
	 * @return
	 */
	protected List<BusinessVO> retrieveBusinesses(String... businessIds) {
		if (businessIds == null || businessIds.length == 0) 
			return Collections.emptyList();

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("REZDOX_BUSINESS where business_id in (");
		DBUtil.preparedStatmentQuestion(businessIds.length, sql);
		sql.append(")");
		log.debug(sql);

		// Get everything that is active or pending
		List<Object> params = new ArrayList<>(businessIds.length);
		for (String id : businessIds)
			params.add(id);

		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new BusinessVO());
	}


	/**
	 * Returns a list of businesses that have not yet received administrative approval
	 * 
	 * @return
	 */
	protected List<BusinessVO> retrievePendingBusinesses() {
		// Use the base query, no additional filtering required
		StringBuilder sql = getBaseBusinessSql();

		// Get everything in the system that is pending approval
		List<Object> params = new ArrayList<>();
		params.add(BusinessStatus.PENDING.getStatus());

		// Get/return the data
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new BusinessVO());
	}

	/**
	 * Get a list of Businesses this member has access to.
	 * 
	 * @param req
	 * @return
	 */
	protected List<BusinessVO> loadBusinessList(ActionRequest req) {
		String oldBizId = req.getParameter(BusinessAction.REQ_BUSINESS_ID);
		if (!StringUtil.isEmpty(oldBizId)) 
			req.setParameter(BusinessAction.REQ_BUSINESS_ID, "");

		List<BusinessVO> bizList = retrieveBusinesses(req);

		if (!StringUtil.isEmpty(oldBizId)) //put this back the way we found it
			req.setParameter(BusinessAction.REQ_BUSINESS_ID, oldBizId);

		return bizList;
	}

	/**
	 * Retrieves the Business Information form & saved form data
	 * 
	 * @param req
	 */
	protected FormVO retrieveBusinessInfoForm(ActionRequest req) {
		String formId = req.hasParameter(REQ_SETTINGS) ? RezDoxUtils.getFormId(getAttributes(), ModuleVO.ATTRIBUTE_2) : RezDoxUtils.getFormId(getAttributes());
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

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		BusinessVO business = new BusinessVO(req);
		boolean newBusiness = StringUtil.isEmpty(business.getBusinessId());

		// Validate this member can edit the business data, prevent malicious editing
		if (!newBusiness) {
			req.setAttribute(ATTR_GET_FOR_MEMBER, true);
			List<BusinessVO> memberBusiness = retrieveBusinesses(req);
			req.removeAttribute(ATTR_GET_FOR_MEMBER);

			if (memberBusiness.isEmpty()) {
				return;
			}
		}

		// Edit the business data
		if (req.hasParameter(REQ_BUSINESS_INFO) || req.hasParameter(REQ_SETTINGS)) {
			saveForm(req);

			SubscriptionAction sa = new SubscriptionAction(dbConn, attributes);

			int count = sa.getBusinessUsage(RezDoxUtils.getMemberId(req));
			if (newBusiness && count == 1) {
				SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
				try {
					req.setSession(changeMemebersRole(req, site));

					// This is the user's first business, give a reward to anyone that might have invited them
					InvitationAction ia = new InvitationAction(dbConn, attributes);
					ia.applyInviterRewards(req, RezDoxUtils.REWARD_BUSINESS_INVITE);

					//Send First Business Notifications.
					sendFirstBusinessNotifications(site, RezDoxUtils.getMemberId(req));

				} catch (DatabaseException e) {
					log.error("could not update member vo", e);
				}
			}

		} else if (req.hasParameter("deleteBusiness")) {
			String msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			try {
				new DBProcessor(dbConn).delete(business);
			} catch (Exception e) {
				log.error("could not delete buisness", e);
				msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			sendRedirect(page.getFullPath(), msg, req);

		} else if (req.hasParameter("savePhoto")) {
			savePhoto(req);
		} else {
			try {				
				putModuleData(saveBusiness(req), 1, false);
			} catch (Exception e) {
				throw new ActionException("Could not save business", e);
			}
		}
	}

	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void savePhoto(ActionRequest req) throws ActionException {
		new PhotoAction(getDBConnection(), getAttributes()).build(req);
	}

	/**
	 * Sends all the First Business User Notifications.
	 * @param memberId
	 */
	private void sendFirstBusinessNotifications(SiteVO site, String memberId) {
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		notifyUtil.sendToMember(Message.NEW_BUS_REWARDS, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_BUS_ONLINE, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_BUS_CONNECT, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_BUS_INVITE, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_BUS_STOREFRONT, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_BUS_SERVICES, null, null, memberId);
		notifyUtil.sendToMember(Message.NEW_BUS_LOGO, null, null, memberId);
	}

	/**
	 * @param req 
	 * @param session
	 * @param site
	 * @param member
	 * @return
	 * @throws DatabaseException 
	 */
	private SMTSession changeMemebersRole(ActionRequest req, SiteVO site) throws DatabaseException {
		ProfileRoleManager prm = new ProfileRoleManager();
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		log.debug("change role for site and member " + site.getSiteId()+"|"+ member.getProfileId());

		SBUserRole role = ((SBUserRole)session.getAttribute(Constants.ROLE_DATA));

		// Upgrade from Registered to Business
		String newRoleId = RezDoxUtils.REZDOX_BUSINESS_ROLE;
		String newRoleName = RezDoxUtils.REZDOX_BUSINESS_ROLE_NAME;
		int newRoleLevel = RezDoxUtils.REZDOX_BUSINESS_ROLE_LEVEL;

		// Upgrade from Residence to Residence/Business Combo
		if (RezDoxUtils.REZDOX_RESIDENCE_ROLE.equals(role.getRoleId())) {
			newRoleId = RezDoxUtils.REZDOX_RES_BUS_ROLE;
			newRoleName = RezDoxUtils.REZDOX_RES_BUS_ROLE_NAME;
			newRoleLevel = RezDoxUtils.REZDOX_RES_BUS_ROLE_LEVEL;
		}

		prm.removeRole(role.getProfileRoleId(), dbConn);
		prm.addRole(member.getProfileId(), site.getSiteId(), newRoleId, 20, dbConn);
		role.setRoleId(newRoleId);
		role.setRoleLevel(newRoleLevel); 
		role.setRoleName(newRoleName);

		role.setProfileRoleId(prm.checkRole(member.getProfileId(),  site.getSiteId(), dbConn));
		session.setAttribute(Constants.ROLE_DATA, role);				
		return session;
	}

	/**
	 * Saves a business form builder form
	 * 
	 * @param req
	 */
	protected void saveForm(ActionRequest req) {
		String formId =  req.hasParameter(REQ_SETTINGS) ? RezDoxUtils.getFormId(getAttributes(), ModuleVO.ATTRIBUTE_2) : RezDoxUtils.getFormId(getAttributes());

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

	/**
	 * A partial update which updates specific business settings only on the business record
	 * 
	 * @param req
	 */
	public void saveSettings(ActionRequest req) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_business set privacy_flg = ? where business_id = ? ");

		List<String> fields = new ArrayList<>();
		fields.addAll(Arrays.asList("privacy_flg", "business_id"));

		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), new BusinessVO(req), fields);
		} catch (Exception e) {
			log.error("Couldn't save business settings", e);
		}
	}
}