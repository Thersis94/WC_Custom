package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SBUserRoleContainer;
import com.smt.sitebuilder.security.SecurityController;

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
		INACTIVE(0), ACTIVE(1), SHARED(2), PENDING(100);

		private int status;
		private BusinessStatus(int status) { this.status = status; }
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

		//preconfigure requests to create new businesses
		if (!req.hasParameter("storeFront") && (RezDoxUtils.REZDOX_RESIDENCE_ROLE.equals(role.getRoleId()) || SBUserRoleContainer.REGISTERED_USER_ROLE_LEVEL == role.getRoleLevel())) {
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

		if (req.hasParameter("storeFront"))
			configureStorefront(req, businessList);
	}


	/**
	 * Prepare for storefront display by determining if we the viewer is already connected to this business.
	 * We use isConnected to conditionally display the green 'Connect' button.
	 * @param req
	 * @param businessList
	 */
	private void configureStorefront(ActionRequest req, List<BusinessVO> businessList) {
		String memberId = RezDoxUtils.getMemberId(req);
		BusinessVO biz = !businessList.isEmpty() ? businessList.get(0) : new BusinessVO();
		boolean isConnected = memberId.equals(biz.getOwner() != null ? biz.getOwner().getMemberId() : ""); //presume the owner is connected
		if (!isConnected) {
			ConnectionAction ca = new ConnectionAction(dbConn, attributes);
			List<ConnectionReportVO> connections = ca.generateConnections(memberId, ConnectionAction.MEMBER, req);
			isConnected = !connections.isEmpty();
		}
		req.setAttribute("isConnected", isConnected);
	}


	/**
	 * Validates whether a new business can be added by the member.
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
	 * @return
	 */
	private StringBuilder getBaseBusinessSql(String schema) {
		StringBuilder sql = new StringBuilder(1400);
		sql.append("select b.business_id, business_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, ");
		sql.append("latitude_no, longitude_no, main_phone_txt, alt_phone_txt, b.email_address_txt, website_url, photo_url, ad_file_url, b.create_dt, b.privacy_flg, ");
		sql.append("bsc.business_category_cd as sub_category_cd, bsc.category_nm as sub_category_nm, bc.business_category_cd as category_cd, bc.category_nm, ");
		sql.append("coalesce(b.update_dt, b.create_dt) as update_dt, m.member_id, m.profile_id, m.first_nm, m.last_nm, bm.status_flg, ");
		sql.append("attribute_id, slug_txt, value_txt, total_reviews_no, avg_rating_no, p.photo_id, p.desc_txt, p.image_url ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business b ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_member_xr bm on b.business_id = bm.business_id and bm.status_flg >= ? ");
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
		String schema = getCustomSchema();
		StringBuilder sql = getBaseBusinessSql(schema);
		sql.append("where 1=1 ");

		// Get everything that is active or pending
		List<Object> params = new ArrayList<>();
		params.add(BusinessStatus.ACTIVE.getStatus());

		// Restrict to the member owner when editing business details
		if (req.hasParameter(REQ_BUSINESS_INFO) || req.hasParameter(REQ_SETTINGS) || StringUtil.isEmpty(businessId) || req.getAttribute(ATTR_GET_FOR_MEMBER) != null) {
			sql.append("and bm.member_id=? ");
			params.add(RezDoxUtils.getMemberId(req));

		}
		// Return only a specific business if selected - this can be combined with the above conditions
		if (!StringUtil.isEmpty(businessId)) {
			sql.append("and b.business_id=? ");
			params.add(businessId);
		}

		sql.append("order by coalesce(bm.status_flg, 1), b.create_dt"); //show primary business first, which is the 1st one created. REZDOX-275

		DBProcessor dbp = new DBProcessor(dbConn, schema);
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

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		return dbp.executeSelect(sql.toString(), Arrays.asList((Object[]) businessIds), new BusinessVO());
	}


	/**
	 * Returns a list of businesses that have not yet received administrative approval
	 * 
	 * @return
	 */
	protected List<BusinessVO> retrievePendingBusinesses() {
		// Use the base query, no additional filtering required
		String schema = getCustomSchema();
		StringBuilder sql = getBaseBusinessSql(schema);

		// Get everything in the system that is pending approval
		List<Object> params = Arrays.asList(BusinessStatus.PENDING.getStatus());

		// Get/return the data
		DBProcessor dbp = new DBProcessor(dbConn, schema);
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
		String formId = RezDoxUtils.getFormId(getAttributes());
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
		boolean notifyAdmin = false;

		// Validate this member can edit the business data, prevent malicious editing
		if (!newBusiness) {
			req.setAttribute(ATTR_GET_FOR_MEMBER, true);
			List<BusinessVO> memberBusiness = retrieveBusinesses(req);
			req.removeAttribute(ATTR_GET_FOR_MEMBER);

			if (memberBusiness.isEmpty())
				return;
		}

		String memberId = RezDoxUtils.getMemberId(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SubscriptionAction sa = new SubscriptionAction(dbConn, attributes);

		// Edit the business data
		if (req.hasParameter(REQ_BUSINESS_INFO)) {
			saveForm(req);

			int count = sa.getUsageQty(memberId, Group.BU); //counts active or pending businesses
			if (newBusiness && count == 1) {
				try {
					req.setSession(changeMemebersRole(req, false));

					// This is the user's first business, give a reward to anyone that might have invited them
					InvitationAction ia = new InvitationAction(dbConn, attributes);
					ia.applyInviterRewards(req, RezDoxUtils.REWARD_BUSINESS_INVITE);

					//Send First Business Notifications.
					sendFirstBusinessNotifications(site, memberId);

				} catch (DatabaseException e) {
					log.error("could not update member vo", e);
				}
			}
			notifyAdmin = newBusiness;

		} else if (req.hasParameter("deleteBusiness")) {
			String msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			String url = page.getFullPath();

			try {
				new DBProcessor(dbConn, getCustomSchema()).delete(business);
			} catch (Exception e) {
				log.error("could not delete business", e);
				msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
			//remove the connections cookie so it rebuilds after the redirect
			CookieUtil.remove(req, ConnectionAction.CONNECTION_COOKIE);

			//downgrade this user's account if they just deleted their only business (and are currently Hybrid role)
			//first get a count of any businesses this user can see.  Only if it's zero do we want to downgrade.
			int count = sa.getBusinessUsage(memberId, BusinessStatus.ACTIVE.getStatus(), BusinessStatus.PENDING.getStatus(), BusinessStatus.SHARED.getStatus());
			SBUserRole role = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
			if (count == 0 && RezDoxUtils.REZDOX_RES_BUS_ROLE.equals(role.getRoleId())) {
				try {
					req.setSession(changeMemebersRole(req, true));
					url = RezDoxUtils.MEMBER_ROOT_PATH; //redir to dashboard, they can no longer see the businesses page.
				} catch (DatabaseException de) {
					log.error("could not downgrade user account", de);
				}
			}

			sendRedirect(url, msg, req);

		} else if (req.hasParameter("savePhoto")) {
			savePhoto(req);
			
		} else {
			try {				
				putModuleData(saveBusiness(req), 1, false);
				notifyAdmin = newBusiness;
			} catch (Exception e) {
				throw new ActionException("Could not save business", e);
			}
		}

		processNotifications(req, site, notifyAdmin);
	}


	/**
	 * process notifications and/or emails after saving a business (build method above)
	 * @param req
	 * @param notifyAdmin
	 * @param site
	 */
	private void processNotifications(ActionRequest req, SiteVO site, boolean notifyAdmin) {
		BusinessVO business = new BusinessVO(req);
		//notify the admin if a new business got created - it requires approval
		if (notifyAdmin) {
			//repopulate the VO when what the form handler repositioned for us
			EmailCampaignBuilderUtil emailer = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
			List<EmailRecipientVO> rcpts = new ArrayList<>();
			rcpts.add(new EmailRecipientVO(null, site.getAdminEmail(), EmailRecipientVO.TO));
			Map<String, Object> data = new HashMap<>();
			data.put("businessName", business.getBusinessName());
			data.put("phoneNumber", business.getMainPhoneText());
			data.put("websiteUrl", business.getWebsiteUrl());
			data.put("address", business.getAddress());
			data.put("address2", business.getAddress2());
			data.put("city", business.getCity());
			data.put("state", business.getState());
			data.put("zip", business.getZipCode());
			data.put("category", StringUtil.checkVal(business.getCategoryName(), business.getCategoryCd()));
			data.put("subcategory", business.getSubCategoryCd());
			data.put("email", business.getEmailAddressText());

			emailer.sendMessage(data, rcpts, RezDoxUtils.EmailSlug.BUSINESS_PENDING.name());

		} else {
			//if not a new business, and files were uploaded (ads or images), notify connected members of the change
			boolean hasNewAdFile = Convert.formatBoolean(req.getAttribute("adFileUploaded"));
			boolean hasNewVideoUrl = !StringUtil.checkVal(req.getAttribute("newYoutubeAdUrl"), "~|~").equals(req.getParameter("oldYoutubeAdUrl"));
			log.debug(String.format("adFileUploaded=%s - hasNewYoutubeUrl=%s", hasNewAdFile, hasNewVideoUrl));

			if (hasNewAdFile || hasNewVideoUrl)
				notifyNewPromo(site, business);
		}
	}


	/**
	 * Ask the notifier to post a message to connected members that a new ad/promotion is available
	 * @param site
	 * @param business
	 */
	private void notifyNewPromo(SiteVO site, BusinessVO business) {
		String bizUrl = StringUtil.join(RezDoxUtils.BUSINESS_STOREFRONT_PATH, "?storeFront=1&businessId=", business.getBusinessId());

		Map<String, Object> params = new HashMap<>();
		params.put("companyName", business.getBusinessName());
		params.put("storefrontUrl", bizUrl);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		notifyUtil.notifyConnectedMembers(business, Message.NEW_BUS_PROMOTION, params, null);
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
	private SMTSession changeMemebersRole(ActionRequest req, boolean isHybridDowngrade) throws DatabaseException {
		ProfileRoleManager prm = new ProfileRoleManager();
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		log.debug("change role for site and member " + RezDoxUtils.MAIN_SITE_ID +"|"+ member.getProfileId());

		SBUserRole role = ((SBUserRole)session.getAttribute(Constants.ROLE_DATA));

		// Upgrade from Registered to Business
		String newRoleId = RezDoxUtils.REZDOX_BUSINESS_ROLE;
		String newRoleName = RezDoxUtils.REZDOX_BUSINESS_ROLE_NAME;
		int newRoleLevel = RezDoxUtils.REZDOX_BUSINESS_ROLE_LEVEL;

		//downgrade from hybrid to residence - they have no more businesses
		if (isHybridDowngrade) {
			newRoleId = RezDoxUtils.REZDOX_RESIDENCE_ROLE;
			newRoleName = RezDoxUtils.REZDOX_RESIDENCE_ROLE_NAME;
			newRoleLevel = RezDoxUtils.REZDOX_RESIDENCE_ROLE_LEVEL;

			// Upgrade from Residence to Residence/Business Combo
		} else if (RezDoxUtils.REZDOX_RESIDENCE_ROLE.equals(role.getRoleId())) {
			newRoleId = RezDoxUtils.REZDOX_RES_BUS_ROLE;
			newRoleName = RezDoxUtils.REZDOX_RES_BUS_ROLE_NAME;
			newRoleLevel = RezDoxUtils.REZDOX_RES_BUS_ROLE_LEVEL;
		}

		prm.removeRole(role.getProfileRoleId(), dbConn);
		prm.addRole(member.getProfileId(), RezDoxUtils.MAIN_SITE_ID, newRoleId, SecurityController.STATUS_ACTIVE, dbConn);
		role.setRoleId(newRoleId);
		role.setRoleLevel(newRoleLevel); 
		role.setRoleName(newRoleName);

		role.setProfileRoleId(prm.checkRole(member.getProfileId(),  RezDoxUtils.MAIN_SITE_ID, dbConn));
		session.setAttribute(Constants.ROLE_DATA, role);				
		return session;
	}

	/**
	 * Saves a business form builder form
	 * 
	 * @param req
	 */
	protected void saveForm(ActionRequest req) {
		String formId =  RezDoxUtils.getFormId(getAttributes());

		// Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, BUSINESS_FORM_PROCESSOR);
	}

	/**
	 * Saves business data
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
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
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

		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		try {
			dbp.executeSqlUpdate(sql.toString(), new BusinessVO(req), fields);
		} catch (Exception e) {
			log.error("Couldn't save business settings", e);
		}
	}
}