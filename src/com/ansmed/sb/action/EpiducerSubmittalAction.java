package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

//SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB II Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.contact.SubmittalDataAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EpiducerSubmittalAction.java<p/>
 * <b>Description: This is essentially a copy of com.smt.sitebuilder.action.contact.SubmittalAction 
 * except that this action will safely handle an attempt to update a profile with an email address that
 * is already in use.  In a 'duplicate email' scenario, the error thrown by ProfileManager is logged, the 
 * email address is discarded, and the submittal is allowed to continue so that the submittal data is 
 * not lost.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since June 09, 2011
 ****************************************************************************/
public class EpiducerSubmittalAction extends SBActionAdapter {
	public static final String CONTACT_SUBMITTAL_ID = "contactSubmittalId";
	
	/**
	 * 
	 */
	public EpiducerSubmittalAction() {
		super();
		
	}

	/**
	 * @param actionInit
	 */
	public EpiducerSubmittalAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Starting EpiducerSubmittalAction build...");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		log.debug("Action ID's: module/actionInit: " + mod.getActionId() + "/" + actionInit.getActionId());
		String id = new UUIDGenerator().getUUID();
        String profileId = StringUtil.checkVal(req.getParameter("pfl_PROFILE_ID"));
        String emailAddress = StringUtil.checkVal(req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
        
        try {
        	this.managePhysicianProfile(req);
        } catch (Exception e) {
        	log.error("Error managing physician's profile; profile ID is: " + profileId + ", ", e);
        }
		log.debug("inserting contact submittal");
		// Build the insert statement
		StringBuffer sql = new StringBuffer();
		sql.append("insert into contact_submittal (contact_submittal_id, ");
		sql.append("profile_id, site_id, action_id, email_address_txt, ");
		sql.append("create_dt, accepted_privacy_flg, dealer_location_id) ");
		sql.append("values(?,?,?,?,?,?,?,?)");
		
		String siteId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteId();
		log.debug("Contact Submital sql: " + sql.toString());
		log.debug(id + "-" + profileId + "-" + siteId + "-" + actionInit.getActionId() + "-" + req.getParameter("pfl_EMAIL_ADDRESS_TXT"));
		req.setParameter("profileId", profileId);
        PreparedStatement ps = null;
        try {
            ps = dbConn.prepareStatement(sql.toString());
            ps.setString(1, id);
            ps.setString(2, profileId);
            ps.setString(3, siteId);
            ps.setString(4, actionInit.getActionId());
            ps.setString(5, emailAddress);
            ps.setTimestamp(6, Convert.getCurrentTimestamp());
            ps.setInt(7, Convert.formatInteger(req.getParameter("collectionStatement")));
            ps.setString(8, req.getParameter(Constants.DEALER_LOCATION_ID_KEY));
            ps.execute();
        } catch (SQLException sqle) {
            log.debug("Error inserting contact submittal", sqle);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        
        // put the Contact Submittal Id on the request object
        req.setAttribute(CONTACT_SUBMITTAL_ID, id);
        
        // Call the action to set the specific field data
        SMTActionInterface aac = new SubmittalDataAction(this.actionInit);
        aac.setAttributes(this.attributes);
        aac.setDBConnection(dbConn);
        aac.build(req);
	}
	
	/**
	 * Manages the user profile information for the submitted contact form, also
	 * adds the org profile comm record
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private void managePhysicianProfile(ActionRequest req) throws Exception {
		log.debug("starting managePhysicianProfile");
		UserDataVO user = new UserDataVO();
		Map<String,Object> profileData = new TreeMap<String,Object>();
		Enumeration<?> iter = req.getParameterNames();
		String paramName;
		// parse the profile fields from the request.
		while (iter.hasMoreElements()) {
			paramName = StringUtil.checkVal(iter.nextElement());
			//log.debug("param=" + paramName);
			if (paramName.startsWith("pfl_")) {
				//log.debug(paramName.substring(4) + " = " + req.getParameter(paramName));
				profileData.put(paramName.substring(4), req.getParameter(paramName));
			}
		}
		
		//check for "none" or "no" email addresses...
		String email = StringUtil.checkVal(profileData.get("EMAIL_ADDRESS_TXT"));
		if (! StringUtil.isValidEmail(email)) 
			profileData.remove("EMAIL_ADDRESS_TXT");
		
		user.setData(profileData);
		log.debug("User: " + user.toString());
		
		// at this point, we should have a profileId on the user object
		log.debug("profileId on user VO: " + user.getProfileId());
		
	    ProfileManager pm = ProfileManagerFactory.getInstance(attributes);

		//Update the user's profile based on the form data or add a new profile
		if (user.getProfileId() != null) {
			pm.updateProfilePartially(user.getDataMap(), user, dbConn);
			
			// **** NOTE: we are forcing the addition of the org_profile_comm record
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			try {
				// orgConsent value is set to 1 to force the allow_comm_flg
				pm.assignCommunicationFlg(site.getOrganizationId(), user.getProfileId(), 1, dbConn,	"CONTACT_US_SUBMISSION");
			} catch (Exception e) {
				log.error("could not set org_profile_comm", e);
				throw new Exception(e.getMessage());
			}
		} 
		return;		
	}
	
}
