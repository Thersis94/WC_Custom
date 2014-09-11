package com.venture.cs.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.venture.cs.action.vo.ActivityVO;
import com.venture.cs.action.vo.VehicleVO;

/****************************************************************************
 *<b>Title</b>: SummaryFacadeAction<p/>
 * <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 * Changes:
 * July 23, 2013: Eric Damschroder: created class
 * Mar 11, 2014: DBargerhuff: refactored class into additional class.  Renamed class.
 ****************************************************************************/

public class SummaryFacadeAction extends SBActionAdapter {
	
	/**
	 * Constructor
	 */
	public SummaryFacadeAction() {
		super();
	}

	/**
	 * Constructor
	 * @param arg0
	 */
	public SummaryFacadeAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * Enum encapsulating the activity type requested.  Each
	 * ActivityType has a corresponding log message which is used
	 * for activity logging and a response message which is used for informing
	 * the view as to the result of the action.
	 *
	 */
	public enum ActivityType {
		CASE_FOLLOW("Follow case", "been added to the list of followers for this case."),
		CASE_FREEZE("Freeze case", "frozen this case."),
		CASE_SHARE("Share case", "shared this case."),
		CASE_UNFREEZE("Unfreeze case", "unfrozen this case."),
		TICKET_COMMENTS_EDIT("Edit comments", "updated the ticket's comments."),
		TICKET_CLOSE("Close ticket", "closed the ticket."),
		TICKET_ADD("Add ticket", "added a ticket to this case."),
		TICKET_FILE_DELETE("Delete file", "deleted the file."),
		OWNER_MODIFY("Modify owner", "modified the owner information."),
		DEALER_EDIT("Edit dealer", "edited the dealer."),
		DEALER_CHANGE("Change dealer", "changed the dealer.");
		
		ActivityType(String logMsg, String responseMsg) {
			this.logMsg = logMsg;
			this.responseMsg = responseMsg;
		}
		
		private String logMsg;
		private String responseMsg;
		
		public String getLogMessage() {
			return logMsg;
		}
		
		public String getResponseMessage() {
			return responseMsg;
		}
		
	}
	
	/**
     * Retrieves the action data for a specified action id
     */
    public void retrieve(SMTServletRequest req) throws ActionException {
    	log.debug("SummaryFacadeAction retrieve...");
    	
    	SMTActionInterface sai = new ManageTicketAction(actionInit);
    	sai.setDBConnection(dbConn);
    	sai.setAttributes(attributes);
    	sai.retrieve(req);
    	    	
    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA); 
        log.debug("actionData=" + modVo.getActionData());
		// Add the data for viewing
        setAttribute(Constants.MODULE_DATA, modVo);
    }

    /**
     * Looks at the the ReqType parameter in order to figure out what we need to do with the request
     */
    public void build(SMTServletRequest req) throws ActionException {
    	String reqType = StringUtil.checkVal(req.getParameter("reqType"));
    	log.debug("SummaryFacadeAction build..., reqType: " + reqType);
    	StringBuilder activityMsg = new StringBuilder();
    	ActivityType actType = null;
    	
    	try {
    		actType = ActivityType.valueOf(reqType);
    	} catch (Exception e) {
    		log.error("Illegal ActivityType requested, ", e);
    	}
    	
    	if (actType != null) {
    		activityMsg.append("You have successfully ");
	    	switch(actType) {
		    	case CASE_FOLLOW:
		    		followCase(req);
		    		activityMsg.append(actType.getResponseMessage());
		    		break;
		    		
		    	case CASE_FREEZE:
		    	case CASE_UNFREEZE:
		    		freezeCase(req);
		    		if (StringUtil.checkVal(req.getParameter("freezeFlag")).equals("1")) {
		    			activityMsg.append(actType.getResponseMessage());
		    		} else {
		    			activityMsg.append(ActivityType.CASE_UNFREEZE.getResponseMessage());
		    		}
		    		break;
		    		
		    	case OWNER_MODIFY:
		    		modifyOwner(req);
		    		activityMsg.append(actType.getResponseMessage());
		    		break;
		    		
		    	case DEALER_EDIT:
		    		editDealer(req);
		    		activityMsg.append(actType.getResponseMessage());
		    		break;
		    		
		    	case DEALER_CHANGE:
		    		changeDealer(req);
		    		activityMsg.append(actType.getResponseMessage());
		    		break;
		    		
				case TICKET_COMMENTS_EDIT:
				case TICKET_CLOSE:
				case TICKET_ADD:
				case TICKET_FILE_DELETE:
					SMTActionInterface sai = new ManageTicketAction(actionInit);
		    		sai.setDBConnection(dbConn);
		    		sai.setAttributes(attributes);
		    		sai.build(req);
		    		activityMsg.append(actType.getResponseMessage());
		    		break;
		    	default:
		    		break;
	    	}
	    	
	   		ActivityVO activity = new ActivityVO();
    		activity.setComment(actType.getLogMessage());
    		logActivity(req, activity);
    		notifyAdmins(req, activity);
    	} else {
    		activityMsg.append("We were unable to process your update.  Please contact your system administrator.");
    	}
    	
    	// redirect
    	StringBuilder url = new StringBuilder();
    	url.append("result?vehicleId=").append(req.getParameter("vehicleId"));
    	url.append("&msg=").append(activityMsg);
    	
    	log.debug("redirect url: " + url.toString());    	
        req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	    
    }

    /**
     * Freezes the case in order to prevent people from adding tickets to it
     * @param req
     * @throws ActionException
     */
    private void freezeCase(SMTServletRequest req) throws ActionException {
    	log.debug("freezing case...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));
    	StringBuilder sb = new StringBuilder();
    	sb.append("UPDATE ").append(customDb).append("VENTURE_VEHICLE ");
    	sb.append("SET FREEZE_FLG = ? WHERE VENTURE_VEHICLE_ID = ?");
    	
    	try {
			PreparedStatement ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("freezeFlag"));
			ps.setString(2, vehicleId);
			
			if (ps.executeUpdate() < 1)
                throw new SQLException("Error freezing this case.");
			
		} catch (SQLException e) {
			log.error("Error freezing this case, " + vehicleId, e);
            throw new ActionException(e.getMessage());
		}
    	
    }
    
    /**
     * Adds a user's profileId database along with the vehicle they wish to follow.
     * This will allow them to be notified whenever emails related to this vehicle are sent out.
     * @param req
     * @throws ActionException
     */
    private void followCase(SMTServletRequest req) throws ActionException {
    	log.debug("follow case...");
    	// check to see if this user is already following this vehicle.
    	String profileId = checkFollowers(req);
    	if (profileId == null) {
    		// user is not a follower of this vehicle, so add them
	    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
	    	StringBuilder sb = new StringBuilder();
	    	sb.append("INSERT INTO ").append(customDb);
	    	sb.append("VENTURE_NOTIFICATION (VENTURE_NOTIFICATION_ID, VENTURE_VEHICLE_ID, PROFILE_ID, CREATE_DT) ");
	    	sb.append("VALUES (?,?,?,?)");
	    	PreparedStatement ps = null;
	    	try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, req.getParameter("vehicleId"));
				ps.setString(3, req.getParameter("submissionId"));
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				if (ps.executeUpdate() < 1)
	                throw new ActionException("Error adding user to list followers of this case.");
				
			} catch (SQLException e) {
				log.error("Error adding user to list of followers of this case, " + req.getParameter("vehicleId"), e);
			} finally {
				if (ps != null) {
					try {
						ps.close();
					} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
				}
			}
    	}
    }
    
    /**
     * Queries the notification table to see if user is already a follower
     * of the vehicle ID being requested.
     * @param req
     * @return
     * @throws ActionException
     */
    private String checkFollowers(SMTServletRequest req) throws ActionException {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));
    	String profileId = StringUtil.checkVal(req.getParameter("submissionId"));
    	
    	String tmpProfileId = null;
    	StringBuilder sb = new StringBuilder();
    	sb.append("select PROFILE_ID from ").append(customDb).append("VENTURE_NOTIFICATION ");
    	sb.append("where PROFILE_ID = ? and VENTURE_VEHICLE_ID = ?");

    	PreparedStatement ps = null;
    	try {
    		ps = dbConn.prepareStatement(sb.toString());
    		ps.setString(1, profileId);
    		ps.setString(2,  vehicleId);
    		ResultSet rs = ps.executeQuery();
    		
    		if (rs.next()) {
    			tmpProfileId = rs.getString(1);
    		}
    	} catch (SQLException sqle) {
    		log.error("Error retrieving a list of followers for this case, ", sqle);
    		throw new ActionException(sqle.getMessage());
    	} finally {
    		if (ps != null) {
    			try {
    				ps.close();
    			} catch (Exception e) {log.error("Error closing PreparedStatement, e");}
    		}
    	}
    	
    	return tmpProfileId;
    	
    }
    
    /**
     * Edit the existing vehicle owner's profile information.
     * @param req
     * @throws ActionException
     */
    private void modifyOwner(SMTServletRequest req) throws ActionException {
    	log.debug("modify owner...");
    	UserDataVO user = manageOwnerProfile(req);
    	updateOwner(req, user);
    }
    
    /**
     * Manages owner profile.  If this is an 'add' (i.e. no profile ID submitted), we first check to see if there
     * is an existing profile.  If there is an existing profile, we load that profile.  If there is no existing profile
     * we create a new profile.  If this is an 'update' (i.e. a profile ID was submitted), we update the profile.
     * @param req
     * @throws ActionException
     */
    private UserDataVO manageOwnerProfile(SMTServletRequest req) throws ActionException {
    	log.debug("managing owner profile...");
    	String profileId = StringUtil.checkVal(req.getParameter("ownerProfileId"));
    	ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
    	// retrieve user data from request
    	UserDataVO user = new UserDataVO(req);
    	try {
	    	if (profileId.length() > 0) {
	    		// this is an edit
	    		pm.updateProfile(user, dbConn);
	    	} else {
	    		// this is an add (new case or new owner for existing case)
	    		// check for existence
	    		profileId = pm.checkProfile(user, dbConn);
	    		if (StringUtil.checkVal(profileId).length() > 0) {
	    			// found a profile, use it as owner
	    			user = pm.getProfile(profileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP, null);
	    		} else {
	    			// no profile found, create the profile
	    			pm.updateProfile(user, dbConn);
	    		}
	    	}
    	} catch (DatabaseException de) {
    		log.error("Error modifying owner, ", de);
    		throw new ActionException("Error modifying owner information.");
    	}
    	
    	return user;
    }
    
    /**
     * Edit the owner of the vehicle
     * @param req
     * @param owner
     * @throws ActionException
     */
    private void updateOwner(SMTServletRequest req, UserDataVO owner) 
    		throws ActionException {
    	log.debug("update owner...");
    	String purchaseDate = StringUtil.checkVal(req.getParameter("purchaseDate"));
    	
    	StringBuilder ownerSQL = new StringBuilder();
    	ownerSQL.append("UPDATE " + attributes.get(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_VEHICLE ");
    	ownerSQL.append("SET OWNER_PROFILE_ID=? ");
    	if (purchaseDate.length() > 0) {
    		ownerSQL.append(", PURCHASE_DT=? ");
    	}
    	ownerSQL.append("WHERE VENTURE_VEHICLE_ID=?");

    	int index = 1;
    	PreparedStatement ps = null;
    	
        try {
			ps = dbConn.prepareStatement(ownerSQL.toString());
	        ps.setString(index++, owner.getProfileId());
	        if (purchaseDate.length() > 0) {
	        	ps.setString(index++, req.getParameter("purchaseDate"));
	        }
	        ps.setString(index++, req.getParameter("vehicleId"));
	        ps.executeUpdate();
	        
		} catch (SQLException e) {
			log.error("Could not update owner information ", e);
			throw new ActionException("Could not update owner.");
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
    }
        
    /**
     * Edit's the dealer's information
     * @param req
     * @throws ActionException
     */
    private void editDealer(SMTServletRequest req) throws ActionException {
    	log.debug("editing dealer location...");
    	DealerInfoAction sai = new DealerInfoAction(actionInit);
    	sai.setAttributes(attributes);
    	sai.setDBConnection(dbConn);
    	
    	try {
    		sai.updateDealerLocation(req, true);
    	} catch (DatabaseException dbe) {
    		log.error("Error updating dealer location, ", dbe);
    		throw new ActionException(dbe.getMessage());
    	}
    }
    
    /**
     * Reassigns the dealer on the request to the vehicle on the request.
     * @param req
     * @throws ActionException
     */
    private void changeDealer(SMTServletRequest req) throws ActionException {
    	log.debug("change dealer...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	String dealerId = StringUtil.checkVal(req.getParameter("dealerId"));
    	String vehicleId = StringUtil.checkVal(req.getParameter("vehicleId"));

        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(customDb).append(" VENTURE_VEHICLE ");
        sb.append("set DEALER_ID = ? where VENTURE_VEHICLE_ID = ?");
        
        PreparedStatement ps = null;
        try {
        	ps = dbConn.prepareStatement(sb.toString());
        	ps.setString(1, dealerId);
        	ps.setString(2, vehicleId);
        	ps.executeUpdate();
        } catch (SQLException sqle) {
        	log.error("Error updating vehicle dealer association, ", sqle);
        	throw new ActionException("Error changing vehicle's dealer.");
        } finally {
        	try {
        		ps.close();
        	} catch (Exception e) {}
        }
	
    }
    
    /**
     * Called whenever any of the other build actions this class can take are completed
     * in order to record who did what and when.
     * @param req
     * @param comment
     * @throws ActionException
     */
    protected void logActivity(SMTServletRequest req, ActivityVO activity) throws ActionException {
    	log.debug("logging activity...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("INSERT INTO ").append(customDb).append(" VENTURE_ACTIVITY_TRAIL ");
    	sb.append("(VENTURE_ACTIVITY_TRAIL_ID, VENTURE_VEHICLE_ID, COMMENT, PROFILE_ID, CREATE_DT) ");
    	sb.append("VALUES (?,?,?,?,?)");
    	
    	activity.setActivityId(new UUIDGenerator().getUUID());
    	activity.setVehicleId(StringUtil.checkVal(req.getParameter("vehicleId")));
    	activity.setSubmissionId(StringUtil.checkVal(req.getParameter("submissionId")));
    	activity.setCreateDate(new Date(Calendar.getInstance().getTimeInMillis()));
    	PreparedStatement ps = null;
    	try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, activity.getActivityId());
			ps.setString(2, activity.getVehicleId());
			ps.setString(3, activity.getComment());
			ps.setString(4, activity.getSubmissionId());
			ps.setTimestamp(5, Convert.formatTimestamp(activity.getCreateDate()));
			
			if (ps.executeUpdate() < 1)
                throw new ActionException("Error logging activity.");
			
		} catch (SQLException e) {
			log.error("Error logging activity for vehicle " + req.getParameter("vehicleId"), e);
		} finally {
        	try {
        		ps.close();
        	} catch (Exception e) {}
        }
    }
    
    /**
     * Called whenever the build action is called using a valid activity type.
     * @param req
     * @param activityMsg
     * @throws ActionException
     */
    protected void notifyAdmins(SMTServletRequest req, ActivityVO activity) 
    		throws ActionException {
    	// look up the submitter's profile to get first/last name
       	String profileId = StringUtil.checkVal(activity.getSubmissionId());
    	ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
    	try {
    		UserDataVO user = pm.getProfile(profileId, dbConn, ProfileManager.PROFILE_ID_LOOKUP, null);
    		activity.setFirstName(user.getFirstName());
    		activity.setLastName(user.getLastName());
    	} catch (DatabaseException de) {
    		log.error("Error retrieving submitter's profile, ", de);
    	}
    	
    	// retrieve initial data from request
    	VehicleVO vehicle = new VehicleVO(req);
    	lookupVehicle(vehicle);
    	vehicle.addActivity(activity);
    	List<VehicleVO> vehicles = new ArrayList<VehicleVO>();
    	vehicles.add(vehicle);
    	
    	CaseNotificationManager cna = new CaseNotificationManager(attributes, dbConn);
    	cna.notifySiteAdmins(req, vehicles);
    	
    }
    
    /**
     * Performs a lookup of a vehicle based on the vehicle ID.  Used to supply full vehicle information
     * to the email notification process.
     * @param vehicle
     */
    private void lookupVehicle(VehicleVO vehicle) {
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	StringBuilder sql = new StringBuilder("select * from ");
    	sql.append(customDb).append("VENTURE_VEHICLE where VENTURE_VEHICLE_ID = ?");
    	PreparedStatement ps = null;
    	try {
    		ps = dbConn.prepareStatement(sql.toString());
    		ps.setString(1,vehicle.getVehicleId());
    		ResultSet rs = ps.executeQuery();
    		while (rs.next()) {
    			vehicle.setMake(rs.getString("MAKE"));
    			vehicle.setModel(rs.getString("MODEL"));
    			vehicle.setYear(rs.getString("YEAR"));
    		}
    	} catch (SQLException sqle) {
    		log.error("Error retrieving vehicle information, ", sqle);
    	}
    }
}
