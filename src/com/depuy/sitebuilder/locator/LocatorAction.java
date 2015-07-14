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

// Google Gson libs
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
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
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

// Xerces
import org.apache.xerces.dom.DeferredDocumentImpl;

// W3C
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    public void build(SMTServletRequest req) throws ActionException {
	    log.debug("LocatorAction build...");
	    // Only process the survey if requested
	    Boolean processSurvey = Convert.formatBoolean(req.getParameter("processSurvey"));
	    Boolean processRegistration = Convert.formatBoolean(req.getParameter("processRegistration"));
	    Boolean processPhysician = Convert.formatBoolean(req.getParameter("processPhysician"));

	    // set the locator data
	    LocatorSubmittalVO loc = (LocatorSubmittalVO)req.getSession().getAttribute(LOCATOR_SUBMITTAL_DATA);
	    if (loc == null) loc = new LocatorSubmittalVO();

	    //the page to send the user to after we process.
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
		    newUrl = newUrl + "&doneReg=1";

	    } else if (processRegistration && !processPhysician) {
		    log.debug("Process Registration: " + processRegistration);

		    try {
			    // Add the data to the registration submittal
			    log.debug("Registration ID: " + req.getParameter("sbActionId"));
			    ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			    mod.setActionId(req.getParameter("sbActionId"));

			    //verify we have a valid email address, if we don't then force printed fulfillment (instead of email)
			    //this works for knee and hip because the field names are the same across all portlets
			    if (req.getParameter("reg_||c0a8022dd74873cf6a6706c42120cb01") != null) {
				    String type = req.getParameter("reg_||c0a8022dd74873cf6a6706c42120cb01");
				    String email = req.getParameter("reg_enc|EMAIL_ADDRESS_TXT|7f000001397b18842a834a598cdeafa");
				    if (type.equalsIgnoreCase("INTERNET") && !StringUtil.isValidEmail(email)) {
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

		    //req.getSession().setAttribute("locatorRegistrationSubmitted", Boolean.TRUE);
		    newUrl = newUrl + "&doneReg=1";

		    // Send the email if user requested Internet kit (Production Codes)
		    //String type = StringUtil.checkVal(this.getFulfillType(req), "MAIL");

		    // Dev codes
		    //log.debug("Request Type for fulfillment: " + type);
		    //log.debug("Call Target for fulfillment: " + this.getCallTarget(req));
		    //if ("INTERNET".equalsIgnoreCase(type)) {
		    //this.sendLocatorEmail(req, this.getCallTarget(req));
		    //}

		    // Process the physician by adding a single record to a DB table for tracking
	    } else if (processPhysician) {
		    log.debug("Update phys info");
		    String sql = "insert into locator_survey_bypass (create_dt) values(?)";
		    PreparedStatement ps = null;
		    try {
			    ps = dbConn.prepareStatement(sql);
			    ps.setTimestamp(1, Convert.getCurrentTimestamp());
			    ps.executeUpdate();
		    } catch(Exception e) {
			    log.error("Error updating locator phyician bypass");
		    } finally {
			    try {
				    ps.close();
			    } catch(Exception e) {}
		    }
		    newUrl = newUrl + "&doneReg=1";

	    } else if (req.hasParameter("emailFriendId")) {
		    this.processMessageSend(req, loc);
		    return;
	    }

		// Build the redirection
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, newUrl);
		log.debug("Redirect URL: " + newUrl);
	}

    /**
	 * New Copy method utilizing the record Duplicator.
	 */
	public void copy(SMTServletRequest req) throws ActionException{
		super.copy(req);	
		
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "LOCATOR", "ACTION_ID", true);
		rdu.addWhereClause(DB_ACTION_ID, req.getParameter(SB_ACTION_ID));
		rdu.copy();
	}
    
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void delete(SMTServletRequest req) throws ActionException {
    	Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
        String sbActionId = req.getParameter(SBModuleAction.SB_ACTION_ID);
        ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
        log.info("Starting Locator Data Action - Delete: " + sbActionId);
        
        StringBuffer sb = new StringBuffer();
        sb.append("delete from locator where action_id = ?");

        PreparedStatement ps = null;
        try {
        	log.debug("Attempting to connect to database");
            ps = dbConn.prepareStatement(sb.toString());
            log.debug("Statement created");
            ps.setString(1, sbActionId);
            log.debug("Statement Set");
            if (ps.executeUpdate() < 1) {
                msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
                log.debug("Nothing was deleted for " + sbActionId);
            } else {

                // Delete the entry in the Module Table
                log.info("Deleting entry in SB_ACTION");
                req.setAttribute(SBModuleAction.SB_ACTION_ID, sbActionId);
                super.delete(req);
            }
            
        } catch (SQLException sqle) {
            log.error("Error deleting content: " + sbActionId, sqle);
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
        } finally {
            mod.setErrorMessage((String)msg);
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // Redirect the user
        util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void update(SMTServletRequest req) throws ActionException {
        ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
        log.info("Starting Locator Data - Update: " + mod.toString());
        
        Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
        StringBuffer sql = new StringBuffer();
        
	    super.update(req);
	    
        String sbActionId = (String) req.getAttribute(SBModuleAction.SB_ACTION_ID);
        req.setAttribute(SBModuleAction.MODULE_TYPE, mod.getModuleType());

        if (Convert.formatBoolean(req.getAttribute(INSERT_TYPE))) {
            sql.append("insert into locator (search_id, search_type_id, ");
            sql.append("organization_id, results_page_no, country_cd, ");
            sql.append("language_cd, header_txt, footer_txt, privacy_url, ");
            sql.append("survey_id, survey_req_flg, registration_id, registration_req_flg, ");
            sql.append("email_friend_id, email_friend_req_flg, create_dt, action_id ) ");
            sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        } else {
            sql.append("update locator set search_id = ?, search_type_id = ?, ");
            sql.append("organization_id = ?, results_page_no = ?,  country_cd = ?, ");
            sql.append("language_cd = ?, header_txt = ?,  footer_txt = ?, ");
            sql.append("privacy_url = ?, survey_id = ?, survey_req_flg = ?, ");
            sql.append("registration_id = ?, registration_req_flg = ?, ");
            sql.append("email_friend_id = ?, email_friend_req_flg = ?, ");
            sql.append("update_dt = ? where action_id = ?");
        }
        
        log.info("Locator Data Update SQL: " + sql.toString());
        
        // perform the execute
        PreparedStatement ps = null;
        try {
            LocatorVO vo = new LocatorVO();
            vo.setData(req);
            
            ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, vo.getSearchId());
            ps.setInt(2, vo.getSearchTypeId());
            ps.setString(3, vo.getOrganizationId());
            ps.setInt(4, vo.getResultsPerPage());
            ps.setString(5, vo.getCountry());
            ps.setString(6, vo.getLanguage());
            ps.setString(7, vo.getHeaderText());
            ps.setString(8, vo.getFooterText());
            ps.setString(9, vo.getPrivacyUrl());
            ps.setString(10, vo.getSurveyId());
            ps.setInt(11, vo.getSurveyReqFlag());
            ps.setString(12, vo.getRegistrationId());
            ps.setInt(13, vo.getRegistrationReqFlag());
            ps.setString(14, vo.getEmailFriendId());
            ps.setInt(15, vo.getEmailFriendReqFlag());
            ps.setTimestamp(16, Convert.getCurrentTimestamp());
            ps.setString(17, sbActionId);
            
            if (ps.executeUpdate() < 1) {
                msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
                log.info("No records updated: ");
            }
            
            // update the list of fields for the action
            SMTActionInterface lfaa = new LocatorFieldAssocAction(actionInit);
            lfaa.setAttributes(this.attributes);
            lfaa.setDBConnection(dbConn);
            lfaa.update(req);
        } catch (Exception sqle) {
            msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
            log.info("Error Update Content", sqle);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // Redirect after the update
        util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
    }

    

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void retrieve(SMTServletRequest req) throws ActionException {
    	log.debug("LocatorAction retrieve...");
    	
    	Boolean locatorSubmit = Convert.formatBoolean(req.getParameter("locatorSubmit"));
    	String surveyId = StringUtil.checkVal(req.getParameter("surveyId"));
    	String registrationId = StringUtil.checkVal(req.getParameter("registrationId"));
        Boolean locatorSurveySubmitted = Convert.formatBoolean(req.getSession().getAttribute("locatorSurveySubmitted"));
        Cookie c = req.getCookie("locatorRegistrationSubmitted");
        Boolean locRegSubmitted = Boolean.FALSE;
        if (c != null) locRegSubmitted = Convert.formatBoolean(c.getValue());
        log.debug("Boolean: " + locatorSubmit + "|" + surveyId.length() + "|" +  registrationId.length() + "|" + locRegSubmitted);
        
        // If the call is to the locator results, bypass the below
    	if (locatorSubmit && surveyId.length() == 0 && (registrationId.length() == 0 || locRegSubmitted)) {
    		log.debug("Going straight to results");
    		req.setAttribute("NoRegistration", Boolean.TRUE);
    		return;
    	}
    	
        // Only get the data if the locator has not been submitted
        ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
        if (locatorSubmit) {
        	req.getSession().setAttribute(LOCATOR_SUBMITTAL_DATA, new LocatorSubmittalVO(req));
        	mod.setActionData(new LocatorVO());
	        attributes.put(Constants.MODULE_DATA, mod);
        } else {
	        StringBuffer sql = new StringBuffer();
	        sql.append("select * from locator where action_id = ?");
	        log.info("Locator Action Retrieve SQL: " + sql.toString());
	        
	        PreparedStatement ps = null;
	        LocatorVO vo = new LocatorVO();
	        try {
	            ps = dbConn.prepareStatement(sql.toString());
	            ps.setString(1, actionInit.getActionId());
	            ResultSet rs = ps.executeQuery();
	            if (rs.next()) vo.setData(rs);
	        } catch (SQLException sqle) {
	            throw new ActionException("Error getting Locator action: " + sqle.getMessage());
	        } finally {
	            if (ps != null) {
	                try {
	                    ps.close();
	                } catch(Exception e) {}
	            }
	        }
	        
	        // retrieve the list of fields for the action
	        SMTActionInterface sbac = new LocatorFieldAssocAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.retrieve(req);
	        vo.setFields((Map<String, Boolean>)req.getAttribute(LocatorFieldAssocAction.LOCATOR_FIELD_RETRV));
	        
	    	// If the user has the locator reg cookie or there is no reg id
	        // set a variable for tracking
	    	if (locRegSubmitted || StringUtil.checkVal(vo.getRegistrationId()).length() == 0) {
	    		req.setAttribute("NoRegistration", Boolean.TRUE);
	    	}
	    	
	        // Store the retrieved data in the ModuleVO.actionData and replace into
	        // the Map
	        mod.setActionData(vo);
	        attributes.put(Constants.MODULE_DATA, mod);
        }
        
        // If the user has submitted a survey or registraton, don't get the data
        if (locRegSubmitted || locatorSurveySubmitted) return;
        
        // Get the survey Data or registration data if required
        if (StringUtil.checkVal(req.getParameter("surveyId")).length() > 0) {
        	ActionInitVO newAi = this.actionInit;
        	newAi.setActionId(req.getParameter("surveyId"));
        	SurveyAction sa = new SurveyAction(newAi);
        	sa.setDBConnection(dbConn);
        	sa.setAttributes(attributes);
        	sa.retrieve(req);
        } else if (StringUtil.checkVal(req.getParameter("registrationId")).length() > 0) {
        	log.debug("Getting registration data: " + req.getParameter("registrationId"));
        	ActionInitVO newAi = this.actionInit;
        	newAi.setActionId(req.getParameter("registrationId"));
        	RegistrationAction sa = new RegistrationAction(newAi);
        	sa.setDBConnection(dbConn);
        	sa.setAttributes(attributes);
        	sa.retrieve(req);
        }
    }
    
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void list(SMTServletRequest req) throws ActionException {
        String actionId = req.getParameter(SBModuleAction.SB_ACTION_ID);
        
        if (actionId == null || actionId.length() == 0) return;
        ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
        
        StringBuffer sql = new StringBuffer();
        sql.append("select search_id, a.action_id, action_nm, b.action_id, ");
        sql.append("a.organization_id, action_desc, search_type_id, results_page_no, ");
        sql.append("a.action_group_id, a.pending_sync_flg, ");
        sql.append("country_cd, language_cd, header_txt, footer_txt, privacy_url, ");
        sql.append("survey_id, survey_req_flg, registration_id, registration_req_flg, ");
        sql.append("email_friend_id, email_friend_req_flg from sb_action a left outer join locator b ");
        sql.append("ON a.action_id = b.action_id ");
        sql.append("where a.action_id = ? ");
        
        log.info("Locator Action List SQL: " + sql.toString());
        LocatorVO vo = new LocatorVO();
        PreparedStatement ps = null;
        try {
            ps = dbConn.prepareStatement(sql.toString());
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
            
            throw new ActionException("Error getting Locator action: " + sqle.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // retrieve the list of fields for the action
        SMTActionInterface sbac = new LocatorFieldAssocAction(actionInit);
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
    private void processMessageSend(SMTServletRequest req, LocatorSubmittalVO loc) {
    	log.debug("processing locator message send...");
    	String type = StringUtil.checkVal(req.getParameter("messageType"));
    	log.debug("type is: " + type);
    	if (type.length() > 0) {
        	String sendValue=StringUtil.checkVal(req.getParameter("sendValue"));
        	boolean isValidSend = this.prepareMessageSendParameters(req, type, sendValue);
        	String errMsg = null;
        	if (isValidSend) {
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
            		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
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
    private boolean prepareMessageSendParameters(SMTServletRequest req, String type, String sendValue) {
    	log.debug("validating and preparing send values...");
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
    		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
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
    private StringBuffer buildSurgeonDetailUrl(SMTServletRequest req) {
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuffer url = new StringBuffer();
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
		url.append("&zip=").append(req.getParameter("zip"));
		url.append("&uniqueId=").append(req.getParameter("uniqueId"));
		String tmp = null;
		if (req.hasParameter("radius")) {
			tmp = StringUtil.checkVal(req.getParameter("radius"), null);
			if (tmp != null) url.append("&radius=").append(tmp);
		}
		if (req.hasParameter("page")) {
			tmp = StringUtil.checkVal(req.getParameter("page"), null);
			if (tmp != null) url.append("&page=").append(tmp);
		}
		return url;
    }
    
    /**
     * Queries the locator for the specified surgeon
     * @param surgeonId
     * @return
     */
    private Map<String,String> findSurgeon(SMTServletRequest req) {
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
    private Map<String,String> findSurgeonFromLookup(SMTServletRequest req) {
    	log.debug("finding surgeon by lookup...");
    	LocatorQueryUtil lq = new LocatorQueryUtil();
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
    private Map<String,String> findSurgeonFromJSTLSessionVar(SMTServletRequest req) {
    	log.debug("finding surgeon from session");
    	String uniqueId = StringUtil.checkVal(req.getParameter("uniqueId"));
    	String idFormat = StringUtil.checkVal(req.getParameter("idFormat"), null);
    	if (idFormat != null) {
    		if (idFormat.equalsIgnoreCase("json")) {
    			return findSurgeonFromJson(req, uniqueId);
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
    private Map<String,String> findSurgeonFromXml(SMTServletRequest req, String uniqueId) {
    	DeferredDocumentImpl ddi = (DeferredDocumentImpl) req.getSession().getAttribute("locData");
    	Map<String,String> surgeon = null;
    	if (ddi != null) {
	    	Element root = ddi.getDocumentElement();
	    	if (root != null) {
	    		surgeon = new HashMap<String,String>();
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
	    						if (childItem.getLocalName() != null) {
	    							if (childItem.getTextContent().equalsIgnoreCase(uniqueId)) {
	    								targetParent = uId.getParentNode();
	    								found = true;
	    							}
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
	    		
	    		if (found) {
    				if (targetParent.hasChildNodes()) {
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
    	}		
    	return surgeon;
    }

    /**
     * Attempts to find a surgeon from the JSON search data on the session.  
     * @param req
     * @param uniqueId
     * @return
     */
    private Map<String,String> findSurgeonFromJson(SMTServletRequest req, String uniqueId) {
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
    
    private String parseJsonStringValue(JsonObject jo, String propertyName) {
    	if (jo.has(propertyName)) {
    		return jo.get(propertyName).getAsString();
    	} else {
    		return "";
    	}
    }
}
