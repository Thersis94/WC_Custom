package com.venture.cs.action;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;





// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.venture.cs.action.SummaryFacadeAction.ActivityType;
import com.venture.cs.action.vo.ActivityVO;
import com.venture.cs.action.vo.VehicleVO;

/****************************************************************************
 *<b>Title</b>: ShareCaseAction<p/>
 * Sends this case to a another user on this site <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 * Changes:
 * July 23, 2013: Eric Damschroder: created class
 * Mar 11, 2014: DBargerhuff: added additional comments
 ****************************************************************************/

public class ShareCaseAction extends SBActionAdapter {
	
	public ShareCaseAction() {
		super();
	}

	public ShareCaseAction(ActionInitVO arg0) {
		super(arg0);
	}
	
    public void retrieve(SMTServletRequest req) throws ActionException {
    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
    	StringBuilder sb = new StringBuilder();
    	
    	// Get all the users that are registered with this site
    	sb.append("SELECT p.PROFILE_ID, p.FIRST_NM, p.LAST_NM, p.EMAIL_ADDRESS_TXT FROM PROFILE p ");
    	sb.append("left join PROFILE_ROLE pr on p.PROFILE_ID = pr.PROFILE_ID ");
    	sb.append("WHERE pr.SITE_ID = ?");
    	
    	log.debug(sb.toString() + "|" + req.getParameter("siteId"));
        PreparedStatement ps = null;
        ArrayList<UserDataVO> users = new ArrayList<UserDataVO>();
        
        UserDataVO vo;
    	try {
			String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(encKey);
            ps = dbConn.prepareStatement(sb.toString());
            ps.setString(1, req.getParameter("siteId"));
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
            	
            	//Decrypt the user data
            	vo = new UserDataVO(rs);
            	vo.setFirstName(se.decrypt(vo.getFirstName()));
            	vo.setLastName(se.decrypt(vo.getLastName()));
            	vo.setEmailAddress(se.decrypt(vo.getEmailAddress()));
            	users.add(vo);
            }
        } catch (SQLException sqle) {
        	log.error("Unable to execute query ", sqle);
        } catch (EncryptionException e) {
        	log.error("Unable to decrypt user data ", e);
		}  finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        modVo.setActionData(users);
        log.debug("actionData=" + modVo.getActionData());
		// Add the data for viewing
        setAttribute(Constants.MODULE_DATA, modVo);
    }

    /**
     * 
     */
    public void build(SMTServletRequest req) throws ActionException {
    	// retrieve the vehicle
    	VehicleVO vehicle = new VehicleVO(req);
    	
		// build the activity
		ActivityVO activity = new ActivityVO();
		activity.setVehicleId(vehicle.getVehicleId());
		String submissionId = StringUtil.checkVal(req.getParameter("submissionId"));
		log.debug("submissionId: " + submissionId);
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		if (submissionId.length() > 0) {
			try {
				UserDataVO user = pm.getProfile(submissionId, dbConn, "PROFILE_ID");
				activity.setFirstName(user.getFirstName());
				activity.setLastName(user.getLastName());
			} catch (DatabaseException de) {
				log.error("Error retrieving submitter's profile, ", de);
			}
			
		}
		
		// parse the shared case recipient's name from the request
		String sharedWith = StringUtil.checkVal(req.getParameter("sharedCaseRecipient"));
		if (sharedWith.length() > 0) {
			int index = sharedWith.indexOf("|");
			if (index > -1) {
				sharedWith = sharedWith.substring(index + 1);
			}
		}
		log.debug("sharedWith: " + sharedWith);
		activity.setComment(ActivityType.CASE_SHARE.getLogMessage() + " with: " + sharedWith);
		
		// log and update the activity
    	SummaryFacadeAction ticket = new SummaryFacadeAction();
		ticket.setDBConnection(dbConn);
		ticket.setAttributes(attributes);
		ticket.logActivity(req, activity);
		
		// now add the updated activity to the vehicle
		vehicle.addActivity(activity);
		
		// build the vehicle List
    	List<VehicleVO> vehicles = new ArrayList<VehicleVO>();
    	vehicles.add(vehicle);
		
    	// build the case URL for the link in the message
    	StringBuilder caseUrl = new StringBuilder();
    	caseUrl.append(StringUtil.checkVal(req.getParameter("site"))).append("?vehicleId=");
    	caseUrl.append(vehicle.getVehicleId());
		// send notification to person with whom case was shared
		CaseNotificationManager cnm = new CaseNotificationManager(attributes, dbConn);
		cnm.notifySharedCase(req, vehicles, caseUrl.toString());
		
		// send admin notification
		cnm.notifySiteAdmins(req, vehicles);
    }

}
