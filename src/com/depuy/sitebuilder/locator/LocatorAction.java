package com.depuy.sitebuilder.locator;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

// Xerces
import org.apache.xerces.dom.DeferredDocumentImpl;

// W3C
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// WC_Custom
import com.depuysynthes.locator.LocationBean;
import com.depuysynthes.locator.ResultsContainer;
import com.depuysynthes.locator.SurgeonBean;

// Google Gson libs
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationAction;
import com.smt.sitebuilder.action.registration.SubmittalAction;
import com.smt.sitebuilder.action.survey.SurveyAction;
import com.smt.sitebuilder.action.survey.SurveyResponseAction;
import com.smt.sitebuilder.action.tools.EmailFriendAction;
import com.smt.sitebuilder.action.tools.EmailFriendVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/*****************************************************************************
 <p><b>Title</b>: LocatorAction.java</p>
 <p><<< Place Description Here</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Jun 19, 2006
 Code Updates
 James Camire, Jun 19, 2006 - Creating Initial Class File
 Jun 03, 2015: DBargerhuff: Added method to parse JSON-ified surgeon data from session.
 ***************************************************************************/

public class LocatorAction extends SBActionAdapter {
	private SiteBuilderUtil util = null;
	// Cookie max age
	public static final int COOKIE_MAX_AGE = 60*60*24*90;
	public static final String DS_LOCATOR_CONSENT_FLG = "dsLocatorConsent";

	// Codes for the type of query 
	public static final int ALL_JOINTS_ALL_PRODUCTS = 1;
	public static final int ALL_JOINTS = 2;
	public static final int SPECIFIC_JOINT = 3;
	public static final int ALL_PRODUCTS = 4;
	public static final int ALL_PRODUCTS_FOR_JOINT = 5;
	public static final int SPECIFIC_PRODUCT = 6;
	public static final int SPECIFIC_JOINT_PRODUCTS_FOR_JOINT = 7;
	public static final String LOCATOR_SUBMITTAL_DATA = "locatorSubmittalData";

	/* !!! Locator v1 session data key.  MUST BE MATCHED IN v1 views so 
	 * that email/sms send works correctly.  */
	public static final String LOCATOR_SESSION_DATA_KEY_V1 = "locData";

	/* !!! Locator v2 session data key.  MUST BE MATCHED IN v1 views so 
	 * that email/sms send works correctly.  */
	public static final String LOCATOR_SESSION_DATA_KEY_V2 = "results";

	/**
	 * Constant for locator session variable set when locator form is loaded.  If session
	 * variable does not exist when locator search is requested, the search is prevented.
	 * Used to mitigate locator bot attacks.
	 */
	public static final String LOCATOR_FORM_ORIGIN = "locatorFormOrigin";

	/**
	 * 
	 */
	public LocatorAction() {
		super();
		util = new SiteBuilderUtil();
	}

	/**
	 * @param arg0
	 */
	public LocatorAction(ActionInitVO arg0) {
		super(arg0);
		util = new SiteBuilderUtil();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("LocatorAction build...");
		// Only process the survey if requested
		Boolean processSurvey = Convert.formatBoolean(req.getParameter("processSurvey"));
		Boolean processRegistration = Convert.formatBoolean(req.getParameter("processRegistration"));
		Boolean processPhysician = Convert.formatBoolean(req.getParameter("processPhysician"));

		//the page to send the user to after we process.
		LocatorSubmittalVO loc = (LocatorSubmittalVO)req.getSession().getAttribute(LOCATOR_SUBMITTAL_DATA);
		if (loc == null) loc = new LocatorSubmittalVO(); 
		String newUrl = loc.getRedirectUrl();

		log.debug("processing build: " + processRegistration + "|" + processSurvey);
		// If the user has submitted a survey, add the data
		if (processSurvey && !processPhysician) {
			log.debug("Process Survey" + processSurvey);
			// Add the data to the survey_response
			ActionInitVO newAi = this.actionInit;
			newAi.setActionId(req.getParameter("surveyId"));
			SurveyResponseAction sa = new SurveyResponseAction(newAi);
			sa.setDBConnection(dbConn);
			sa.build(req);

			// Add a session param so user does not receive the survey twice
			req.getSession().setAttribute("locatorSurveySubmitted", Boolean.TRUE);

		} else if (processRegistration && !processPhysician) {
			log.debug("Process Registration: " + processRegistration);

			try {
				// Add the data to the registration submittal
				log.debug("Registration ID: " + req.getParameter("sbActionId"));
				ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
				mod.setActionId(req.getParameter("sbActionId"));

				//verify we have a valid email address, if we don't then force printed fulfillment (instead of email)
				//this works for knee and hip because the field names are the same across all portlets
				if (req.getParameter("reg_||c0a8022dd74873cf6a6706c42120cb01") != null) {
					String type = req.getParameter("reg_||c0a8022dd74873cf6a6706c42120cb01");
					String email = req.getParameter("reg_enc|EMAIL_ADDRESS_TXT|7f000001397b18842a834a598cdeafa");
					if ("INTERNET".equalsIgnoreCase(type) && !StringUtil.isValidEmail(email)) {
						req.setParameter("reg_||c0a8022dd74873cf6a6706c42120cb01", "MAIL", true);
					}
				}
				ActionInitVO newAi = this.actionInit;
				newAi.setActionId(req.getParameter("sbActionId"));
				SubmittalAction sa = new SubmittalAction(newAi);
				sa.setAttributes(this.attributes);
				sa.setDBConnection(dbConn);
				sa.build(req);
			} catch (Exception e) {
				log.error("Error adding registrant", e);
			}

			// Add a Cookie so user does not receive the survey twice
			Cookie cookie = new Cookie("locatorRegistrationSubmitted", "true");
			cookie.setMaxAge(COOKIE_MAX_AGE);
			HttpServletResponse res = (HttpServletResponse) this.getAttribute(GlobalConfig.HTTP_RESPONSE);
			cookie.setPath("/" + (String) this.getAttribute(Constants.CONTEXT_NAME));
			res.addCookie(cookie);

			// Process the physician by adding a single record to a DB table for tracking
		} else if (processPhysician) {
			log.debug("Update phys info");
			String sql = "insert into locator_survey_bypass (create_dt) values(?)";
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setTimestamp(1, Convert.getCurrentTimestamp());
				ps.executeUpdate();
			} catch(Exception e) {
				log.error("Error updating locator phyician bypass, ", e);
			}

		} else if (req.hasParameter("emailFriendId")) {
			this.processMessageSend(req);
			return;
		}

		// Build the redirection
		newUrl = newUrl + "&doneReg=1";
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, newUrl);
		log.debug("Redirect URL: " + newUrl);
	}

	/**
	 * New Copy method utilizing the record Duplicator.
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException{
		super.copy(req);	

		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "LOCATOR", "ACTION_ID", true);
		rdu.addWhereClause(DB_ACTION_ID, req.getParameter(SB_ACTION_ID));
		rdu.copy();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String sbActionId = req.getParameter(SBActionAdapter.SB_ACTION_ID);
		ModuleVO mod = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		log.info("Starting Locator Data Action - Delete: " + sbActionId);

		StringBuilder sb = new StringBuilder(75);
		sb.append("delete from locator where action_id = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, sbActionId);
			if (ps.executeUpdate() < 1) {
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			} else {

				// Delete the entry in the Module Table
				req.setAttribute(SBActionAdapter.SB_ACTION_ID, sbActionId);
				super.delete(req);
			}

		} catch (SQLException sqle) {
			log.error("Error deleting content: " + sbActionId, sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			mod.setErrorMessage((String)msg);
		}

		// Redirect the user
		util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		log.info("Starting Locator Data - Update: " + mod.toString());

		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		StringBuilder sql = new StringBuilder(400);

		super.update(req);

		String sbActionId = (String) req.getAttribute(SBActionAdapter.SB_ACTION_ID);
		req.setAttribute(SBActionAdapter.MODULE_TYPE, mod.getModuleType());

		if (Convert.formatBoolean(req.getAttribute(INSERT_TYPE))) {
			sql.append("insert into locator (search_id, search_type_id, ");
			sql.append("organization_id, results_page_no, country_cd, ");
			sql.append("language_cd, header_txt, footer_txt, privacy_url, ");
			sql.append("survey_id, survey_req_flg, registration_id, registration_req_flg, ");
			sql.append("email_friend_id, email_friend_req_flg, extd_radius_max_no, ");
			sql.append("create_dt, action_id ) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update locator set search_id = ?, search_type_id = ?, ");
			sql.append("organization_id = ?, results_page_no = ?,  country_cd = ?, ");
			sql.append("language_cd = ?, header_txt = ?,  footer_txt = ?, ");
			sql.append("privacy_url = ?, survey_id = ?, survey_req_flg = ?, ");
			sql.append("registration_id = ?, registration_req_flg = ?, ");
			sql.append("email_friend_id = ?, email_friend_req_flg = ?, ");
			sql.append("extd_radius_max_no = ?, update_dt = ? where action_id = ?");
		}

		log.info("Locator Data Update SQL: " + sql.toString());

		// perform the execute
		int idx = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			LocatorVO vo = new LocatorVO();
			vo.setData(req);

			ps.setString(idx++, vo.getSearchId());
			ps.setInt(idx++, vo.getSearchTypeId());
			ps.setString(idx++, vo.getOrganizationId());
			ps.setInt(idx++, vo.getResultsPerPage());
			ps.setString(idx++, vo.getCountry());
			ps.setString(idx++, vo.getLanguage());
			ps.setString(idx++, vo.getHeaderText());
			ps.setString(idx++, vo.getFooterText());
			ps.setString(idx++, vo.getPrivacyUrl());
			ps.setString(idx++, vo.getSurveyId());
			ps.setInt(idx++, vo.getSurveyReqFlag());
			ps.setString(idx++, vo.getRegistrationId());
			ps.setInt(idx++, vo.getRegistrationReqFlag());
			ps.setString(idx++, vo.getEmailFriendId());
			ps.setInt(idx++, vo.getEmailFriendReqFlag());
			ps.setInt(idx++,  vo.getMaxExtendedSearchRadius());
			ps.setTimestamp(idx++, Convert.getCurrentTimestamp());
			ps.setString(idx++, sbActionId);

			if (ps.executeUpdate() < 1) {
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
				log.info("No records updated: ");
			}

			// update the list of fields for the action
			ActionInterface lfaa = new LocatorFieldAssocAction(actionInit);
			lfaa.setAttributes(this.attributes);
			lfaa.setDBConnection(dbConn);
			lfaa.update(req);
		} catch (Exception sqle) {
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.info("Error Update Content", sqle);
		}

		// Redirect after the update
		util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}



	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		Boolean locatorSubmit = Convert.formatBoolean(req.getParameter("locatorSubmit"));
		String surveyId = StringUtil.checkVal(req.getParameter("surveyId"));
		String registrationId = StringUtil.checkVal(req.getParameter("registrationId"));
		Boolean locRegSubmitted = Boolean.FALSE;

		SMTCookie c = req.getCookie("locatorRegistrationSubmitted");
		if (c != null) 
			locRegSubmitted = Convert.formatBoolean(c.getValue());
		log.debug("locatorSubmit | surveyId length | registrationId length | locRegSubmitted: " + 
				locatorSubmit + "|" + surveyId.length() + "|" +  registrationId.length() + "|" + locRegSubmitted);

		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		if (locatorSubmit) {
			// If the call is to the locator results and originates from a locator form, bypass the below
			if (surveyId.isEmpty() && (registrationId.isEmpty() || locRegSubmitted)) {
				if (isSubmitFromLocatorForm(req)) {
					log.debug("Going straight to results");
					req.setAttribute("NoRegistration", Boolean.TRUE);
				}
				return;
			}

			req.getSession().setAttribute(LOCATOR_SUBMITTAL_DATA, new LocatorSubmittalVO(req));
			mod.setActionData(new LocatorVO());
			attributes.put(Constants.MODULE_DATA, mod);

		} else {
			// retrieve locator from cache or db
			retrieveLocator(req,mod,locRegSubmitted);

			// Set a session value to signal a locator form load
			req.getSession().setAttribute(LOCATOR_FORM_ORIGIN, "true");
		}

		// If the user has submitted a survey or registraton, don't get the data
		Boolean locatorSurveySubmitted = Convert.formatBoolean(req.getSession().getAttribute("locatorSurveySubmitted"));
		if (locRegSubmitted || locatorSurveySubmitted) return;

		// Get the survey Data or registration data if required
		if (! surveyId.isEmpty()) 
			retrieveSurvey(req,surveyId);
		else if (! registrationId.isEmpty())
			retrieveRegistration(req,registrationId);
	}

	/**
	 * Checks to see if 
	 * @param req
	 * @return
	 */
	private boolean isSubmitFromLocatorForm(ActionRequest req) {
		if (Convert.formatBoolean(req.getSession().getAttribute(LOCATOR_FORM_ORIGIN)))
			return true;

		/* If locator search was submitted without first going through a form, the submittal
		 * is suspicious.  To prevent bots we negate the request params that 'allow' the view to execute
		 * a search, and set an empty results container on the session as if the search had been completed
		 * with no results. */
		req.setParameter("newSearch", null);
		req.getSession().setAttribute(LOCATOR_SESSION_DATA_KEY_V2, new ResultsContainer());    	
		return false;
	}

	/**
	 * Retrieves the locator from cache or db.
	 * @param req
	 * @param mod
	 * @param locRegSubmitted
	 * @throws ActionException
	 */
	private void retrieveLocator(ActionRequest req, ModuleVO mod, boolean locRegSubmitted) 
			throws ActionException {
		String cacheKey = mod.getPageModuleId();
		ModuleVO cachedMod = super.readFromCache(cacheKey);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		boolean isPreview = page.isPreviewMode();
		log.debug("isPreview | cacheKey: " + isPreview + "|" + cacheKey);

		if (cachedMod == null || isPreview) {
			cachedMod = loadLocatorData(req);
			/* If we are in preview mode we do not want this to have anything to do with the cache
			 * Otherwise we make use of the cache as normal. */
			cachedMod.setCacheable(!isPreview);
			cachedMod.setPageModuleId(cacheKey);
			log.debug("writing to cache, id=" + mod.getPageModuleId());

			// If we are in preview mode, do not cache this. 
			if (!isPreview)
				super.writeToCache(cachedMod);
		}

		// If the user has the locator reg cookie or there is no reg id, set a variable for tracking
		String regId = ((LocatorVO)cachedMod.getActionData()).getRegistrationId();
		if (locRegSubmitted || StringUtil.isEmpty(regId)) {
			req.setAttribute("NoRegistration", Boolean.TRUE);
		}

		// replace into the Map
		mod.setActionData(cachedMod.getActionData());
		setAttribute(Constants.MODULE_DATA, mod);
	}

	/**
	 * Retrieves the locator based on the action's action ID.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private ModuleVO loadLocatorData(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(75);
		sql.append("select * from locator where action_id = ?");
		log.debug("Locator Action Retrieve SQL: " + sql.toString());

		LocatorVO vo = new LocatorVO();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, actionInit.getActionId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) vo.setData(rs);
		} catch (SQLException sqle) {
			throw new ActionException("Error getting Locator action: " + sqle);
		}

		// retrieve the list of fields for the action
		ActionInterface sbac = new LocatorFieldAssocAction(actionInit);
		sbac.setAttributes(this.attributes);
		sbac.setDBConnection(dbConn);
		sbac.retrieve(req);
		vo.setFields((Map<String, Boolean>)req.getAttribute(LocatorFieldAssocAction.LOCATOR_FIELD_RETRV));

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionData(vo);
		return mod;
	}

	/**
	 * Retrieves the survey associated to the given surveyId.
	 * @param req
	 * @param surveyId
	 * @throws ActionException
	 */
	private void retrieveSurvey(ActionRequest req, String surveyId) 
			throws ActionException {
		// Get the survey Data or registration data if required
		ActionInitVO newAi = this.actionInit;
		newAi.setActionId(surveyId);
		SurveyAction sa = new SurveyAction(newAi);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
	}

	/**
	 * Retrieves the registration associated to the given registrationId.
	 * @param req
	 * @param registrationId
	 * @throws ActionException
	 */
	private void retrieveRegistration(ActionRequest req, String registrationId) 
			throws ActionException {
		log.debug("Getting registration data: " + registrationId);
		actionInit.setActionGroupId(registrationId);
		RegistrationAction sa = new RegistrationAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void list(ActionRequest req) throws ActionException {
		String actionId = req.getParameter(SBActionAdapter.SB_ACTION_ID);

		if (actionId == null || actionId.length() == 0) return;
		ModuleVO mod = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);

		StringBuilder sql = new StringBuilder(450);
		sql.append("select search_id, a.action_id, action_nm, b.action_id, ");
		sql.append("a.organization_id, action_desc, search_type_id, results_page_no, ");
		sql.append("a.action_group_id, a.pending_sync_flg, ");
		sql.append("country_cd, language_cd, header_txt, footer_txt, privacy_url, ");
		sql.append("survey_id, survey_req_flg, registration_id, registration_req_flg, ");
		sql.append("email_friend_id, email_friend_req_flg, extd_radius_max_no ");
		sql.append("from sb_action a left outer join locator b ");
		sql.append("ON a.action_id = b.action_id ");
		sql.append("where a.action_id = ? ");

		log.info("Locator Action List SQL: " + sql.toString());
		LocatorVO vo = new LocatorVO();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, actionId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo.setData(rs);
				vo.setActionName(rs.getString(3));
				vo.setActionDesc(rs.getString(6));
				String built = rs.getString(4);
				if (built != null && built.length() > 0) vo.setBuilt(true);

			}
		} catch (SQLException sqle) {
			throw new ActionException("Error getting Locator action: " + sqle);
		}

		// retrieve the list of fields for the action
		ActionInterface sbac = new LocatorFieldAssocAction(actionInit);
		sbac.setAttributes(this.attributes);
		sbac.setDBConnection(dbConn);
		sbac.list(req);
		vo.setFields((Map<String, Boolean>)req.getAttribute(LocatorFieldAssocAction.LOCATOR_FIELD_LIST));

		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		mod.setActionData(vo);
		this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
	}

	/**
	 * Processes message send request.
	 * @param req
	 * @param loc
	 */
	private void processMessageSend(ActionRequest req) {
		log.debug("processing locator message send...");
		String type = StringUtil.checkVal(req.getParameter("messageType"));
		if (type.length() > 0) {
			String sendValue=StringUtil.checkVal(req.getParameter("sendValue"));
			boolean isValidSend = this.prepareMessageSendParameters(req, type, sendValue);
			String errMsg = null;
			if (isValidSend) { 
				log.debug("isValidSend: " + isValidSend);
				SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
				Map<String,String> surgeon = this.findSurgeon(req);
				attributes.put(EmailFriendAction.MESSAGE_DATA_MAP, surgeon);
				String origActionId = actionInit.getActionId();
				actionInit.setActionId(req.getParameter("emailFriendId"));
				EmailFriendAction eaf = new EmailFriendAction(actionInit);
				eaf.setAttributes(attributes);
				eaf.setDBConnection(dbConn);
				try {
					eaf.retrieve(req);
					ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
					EmailFriendVO eVo = (EmailFriendVO) mod.getActionData();
					eaf.processMessageSend(req, site, eVo, "");
				} catch(ActionException ae) {
					errMsg = ae.getMessage();
				} catch (InvalidDataException ide) {
					errMsg = ide.getMessage();
				} catch (MailException me) {
					errMsg = me.getMessage();
				} finally {
					eaf = null;
					actionInit.setActionId(origActionId);        			
				}

				if (errMsg != null) {
					log.error("Error sending locator message: " + errMsg);
					ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
					mod.setErrorCondition(true);
					mod.setErrorMessage(errMsg);
					attributes.put(Constants.MODULE_DATA, mod);
				}
			}	
		}
	}

	/**
	 * Validates message type and value and sets specific parameters based on the message type.
	 * @param req
	 * @param type
	 * @param sendValue
	 */
	private boolean prepareMessageSendParameters(ActionRequest req, String type, String sendValue) {
		log.debug("validating and preparing send values...send value is: " + sendValue);
		boolean isValidSend = false;
		// determine if valid send
		if (type.equalsIgnoreCase(EmailFriendAction.MESSAGE_TYPE_EMAIL)) {
			if (StringUtil.isValidEmail(sendValue)) {
				req.setParameter("rcptEml", sendValue);
				isValidSend = true;
			}
		} else if (type.equalsIgnoreCase(EmailFriendAction.MESSAGE_TYPE_SMS)) {
			StringEncoder se = new StringEncoder();
			String tmpVal = se.decodeValue(sendValue);
			tmpVal = StringUtil.removeNonNumeric(tmpVal);
			if (tmpVal != null) {
				if (tmpVal.length() == 10) {
					tmpVal = "1" + tmpVal;
					isValidSend = true;
				} else if (tmpVal.length() > 10) {
					tmpVal = tmpVal.substring(0,11);
					isValidSend = true;
				}
			}
			if (isValidSend) req.setParameter("rcptNo", tmpVal);
		} else {
			ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			mod.setErrorCondition(true);
			mod.setErrorMessage("Invalid message type specified for message send.  Message type must be one of 'email' or 'sms'.");
			return isValidSend;
		}

		return isValidSend;
	}

	/**
	 * Builds the surgeon detail (a.k.a. 'more information') link URL used to 
	 * link to a specific surgeon's information.
	 * @param req
	 * @return
	 */
	private StringBuilder buildSurgeonDetailUrl(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuilder url = new StringBuilder(300);
		url.append(site.getFullSiteAlias());
		url.append(req.getParameter("locatorPage"));
		url.append("?language=").append(StringUtil.checkVal(req.getParameter("language"), "en"));
		url.append("&country=").append(StringUtil.checkVal(req.getParameter("country"), "US"));
		url.append("&number_of_results_per_page=").append(StringUtil.checkVal(req.getParameter("number_of_results_per_page"), "5"));
		url.append("&locatorType=").append(req.getParameter("locatorType"));
		url.append("&site_location=").append(site.getSiteId());
		url.append("&locatorSubmit=true");
		url.append("&displayResults=true");
		url.append("&specialty=").append(req.getParameter("specialty"));
		url.append("&product=").append(req.getParameter("product"));
		url.append("&zip=").append(req.getParameter("zip"));
		url.append("&uniqueId=").append(req.getParameter("uniqueId"));
		if (req.hasParameter("radius")) {
			url.append("&radius=").append(req.getParameter("radius"));
		}
		if (req.hasParameter("page")) {
			url.append("&page=").append(req.getParameter("page"));
		}
		return url;
	}

	/**
	 * Queries the locator for the specified surgeon
	 * @param surgeonId
	 * @return
	 */
	private Map<String,String> findSurgeon(ActionRequest req) {
		log.debug("finding surgeon...");
		Map<String,String> surgeon = null;
		// try from session first
		surgeon = findSurgeonFromJSTLSessionVar(req);
		if (surgeon == null) {
			// surgeon not found on session, perform lookup
			surgeon = findSurgeonFromLookup(req);
		}

		if (surgeon != null) {
			// format the phone number
			String phone = surgeon.get("phone");
			PhoneNumberFormat pnf = new PhoneNumberFormat();
			pnf.setPhoneNumber(phone);
			log.debug("formatted phone number: " + pnf.getFormattedNumber());
			surgeon.put("phone", pnf.getFormattedNumber());

			// build the 'surgeonUrl'
			surgeon.put("surgeonUrl", buildSurgeonDetailUrl(req).toString());
			log.debug("surgeon detail url: " + surgeon.get("surgeonUrl"));
		}

		return surgeon;
	}

	/**
	 * Performs a Locator query to find the surgeon based on request ID.
	 * @param req
	 * @return
	 */
	private Map<String,String> findSurgeonFromLookup(ActionRequest req) {
		log.debug("finding surgeon by lookup...");
		LocatorQueryUtil lq = new LocatorQueryUtil(StringUtil.checkVal(getAttribute("aamdUrl")));
		lq.setSpecialty(Convert.formatInteger(req.getParameter("specialty")));
		lq.setSiteLocation("aamd");
		lq.setZipCode(req.getParameter("zip"));
		lq.setUniqueId(req.getParameter("uniqueId"));
		lq.setOrder(req.getParameter("order"));
		try {
			return lq.locateSurgeonByUniqueId(lq.getUniqueId());
		} catch(Exception e) {
			log.error("Error retrieving surgeon from Locator: " + e);
			return null;
		}
	}

	/**
	 * Attempts to find a surgeon from the search data on the session.  
	 * @param req
	 * @return
	 */
	private Map<String,String> findSurgeonFromJSTLSessionVar(ActionRequest req) {
		log.debug("finding surgeon from session");
		String uniqueId = StringUtil.checkVal(req.getParameter("uniqueId"));
		String idFormat = StringUtil.checkVal(req.getParameter("idFormat"), null);
		String version = StringUtil.checkVal(req.getParameter("version"),"1");
		if (idFormat != null) {
			if ("json".equalsIgnoreCase(idFormat)) {
				if ("2".equals(version)) {
					//return findSurgeonFromJsonV2(req, uniqueId);
					return this.findSurgeonFromResultsContainer(req, uniqueId);
				} else {
					return findSurgeonFromJson(req, uniqueId);
				}
			} else {
				return null;
			}
		} else {
			return findSurgeonFromXml(req, uniqueId);
		}
	}

	/**
	 * Attempts to find a surgeon from the XML search data on the session.  
	 * This method assumes that the underlying XML implementation(format) of 
	 * the search data on the session is based on Apache's Xerces implementation
	 * because this is the implementation used by the JSTL XML taglib.
	 * @param req
	 * @param uniqueId
	 * @return
	 */
	private Map<String,String> findSurgeonFromXml(ActionRequest req, String uniqueId) {
		DeferredDocumentImpl ddi = (DeferredDocumentImpl) req.getSession().getAttribute("locData");
		Map<String,String> surgeon = null;
		if (ddi != null) {
			Element root = ddi.getDocumentElement();
			if (root != null) {
				surgeon = new HashMap<>();
				log.debug("uniqueId: " + uniqueId);
				// get all of the "uniqueId" nodes
				NodeList results = root.getElementsByTagName("uniqueId");

				// loop the uniqueId nodes to find the one we want
				Node uId =  null;
				Node targetParent = null;
				boolean found = false;
				for (int k = 0; k < results.getLength(); k++) {
					uId = results.item(k);
					if (uId.getLocalName() == null) {
						// iterate the node's children
						if (uId.hasChildNodes()) {
							NodeList children = uId.getChildNodes();
							for (int child = 0; child < children.getLength(); child++) {
								Node childItem = children.item(child);
								if (childItem.getLocalName() != null && 
										childItem.getTextContent().equalsIgnoreCase(uniqueId)) {
									targetParent = uId.getParentNode();
									found = true;
								}
								if (found) break;
							}
						}
					} else if (uId.getTextContent().equalsIgnoreCase(uniqueId)) {
						targetParent = uId.getParentNode();
						found = true;
					}
					if (found) break;
				}

				if (found && targetParent.hasChildNodes()) {
					NodeList children = targetParent.getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						Node child = children.item(j);
						if (child.getLocalName() != null) {
							surgeon.put(child.getLocalName(), child.getTextContent());
						}
					}
				}
			}
		}		
		return surgeon;
	}

	/**
	 * Attempts to find a surgeon from the JSON search data on the session.  
	 * @param req
	 * @param uniqueId
	 * @return
	 */
	private Map<String,String> findSurgeonFromJson(ActionRequest req, String uniqueId) {
		log.debug("findSurgeonFromJson...");
		Map<String,String> surgeon = null;
		String json = (String)req.getSession().getAttribute("locData");
		if (json == null) return surgeon;

		// parse the data from the session.
		JsonParser parser = new JsonParser();
		JsonElement jEle = null;
		try {
			jEle = parser.parse(json);
			surgeon = new HashMap<>();
		} catch (Exception e) {
			log.error("Error parsing surgeon data from JSON on session, ", e);
			return surgeon;
		}

		JsonArray jArr = jEle.getAsJsonArray();
		Iterator<JsonElement> jIter = jArr.iterator();
		// loop JSON array until we find the surgeon we are looking for
		while (jIter.hasNext()) {
			JsonObject jo = jIter.next().getAsJsonObject();
			if (! jo.has("uniqueId")) continue;
			if (uniqueId.equalsIgnoreCase(jo.get("uniqueId").getAsString())) {
				// found surgeon record, add keys/values required by email/sms
				surgeon.put("firstName", parseJsonStringValue(jo,"firstName"));
				surgeon.put("lastName", parseJsonStringValue(jo,"lastName"));
				surgeon.put("address1", parseJsonStringValue(jo,"address"));
				surgeon.put("city", parseJsonStringValue(jo,"city"));
				surgeon.put("state", parseJsonStringValue(jo,"state"));
				surgeon.put("phone", parseJsonStringValue(jo,"phoneNumber"));
				break;
			}
		}

		return surgeon;
	}

	/**
	 * Retrieves the search results container from the session and iterates the 
	 * surgeon's list to find the specific surgeon.
	 * @param req
	 * @param uniqueId: dash-delimited String representing a combination of 
	 * surgeon ID, clinic ID, and location ID that identifies a specific surgeon at a 
	 * specific clinic and at a clinic's specific location. Format is typically 
	 * nnnnn-nnnnn-nnnnn where 'nnnnn' means a number greater than 1.
	 * @return
	 */
	private Map<String,String> findSurgeonFromResultsContainer(ActionRequest req, 
			String uniqueId) {
		log.debug("findSurgeonFromResultsContainer...");
		String type = StringUtil.checkVal(req.getParameter("messageType"));
		Map<String,String> surgeonVals = new HashMap<>();
		ResultsContainer rc = (ResultsContainer)req.getSession().getAttribute(LOCATOR_SESSION_DATA_KEY_V2);
		for (SurgeonBean sb : rc.getResults()) {
			if (sb.getUniqueId().equals(uniqueId)) {
				//log.debug("found surgeon");
				/* NOTE: Map keys are used as FreeMarker tags in JSTL. (e.g. ${firstName})
				 * If you specify a key in JSTL but do not include it in your map, the
				 * JSTL will fail to render.  Having more keys in your map than you use in
				 * your JSTL has no detrimental effect to page rendering. */
				surgeonVals.put("firstName", sb.getFirstName());
				surgeonVals.put("lastName", sb.getLastName());
				surgeonVals.put("degree", sb.getDegreeDesc());

				StringBuilder website = new StringBuilder(75);
				if (StringUtil.checkVal(sb.getCustomUrl(), null) != null) {
					if (type.equalsIgnoreCase(EmailFriendAction.MESSAGE_TYPE_SMS)) {
						website.append(sb.getCustomUrl());
					} else if (type.equalsIgnoreCase(EmailFriendAction.MESSAGE_TYPE_EMAIL)) {
						website.append("<a href=\"");
						website.append(sb.getCustomUrl()).append("\">");
						website.append(sb.getCustomUrl()).append("</a>");
					}
				}
				surgeonVals.put("website", website.toString());

				// get location vals from first location in locations list.
				LocationBean loc = sb.getLocations().get(0);
				surgeonVals.put("address1", loc.getAddress());
				surgeonVals.put("city", loc.getCity());
				surgeonVals.put("state", loc.getState());
				surgeonVals.put("zip", loc.getZip());
				surgeonVals.put("phone", loc.getPhoneNumber());
				/* log.debug("first/last/address/city/state: " + surgeonVals.get("firstName") + 
						"|" + surgeonVals.get("lastName") + "|" + surgeonVals.get("address1") + 
						"|" + surgeonVals.get("city") + "|" + surgeonVals.get("state") + 
						"|" + surgeonVals.get("zip") + "|" + surgeonVals.get("phone")); */
				break;
			}
		}

		return surgeonVals;
	}

	/**
	 * Utility method to ensure that a value is returned for JSON properties.
	 * @param jo
	 * @param propertyName
	 * @return
	 */
	private String parseJsonStringValue(JsonObject jo, String propertyName) {
		if (jo.has(propertyName)) {
			return jo.get(propertyName).getAsString();
		} else {
			return "";
		}
	}
}
