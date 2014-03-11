package com.venture.cs.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

/****************************************************************************
 *<b>Title</b>: VehicleFacadeAction<p/>
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

public class OverviewFacadeAction extends SBActionAdapter {
	
	public OverviewFacadeAction() {
		super();
	}

	public OverviewFacadeAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
     * Retrieves the action data for a specified action id
     */
    public void retrieve(SMTServletRequest req) throws ActionException {
    	log.debug("VehicleFacadeAction retrieve...");
    	
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
    	log.debug("VehicleFacadeAction build..., reqType: " + reqType);
    	String activityMsg = null;
    	
    	if (reqType.equals("freeze")) {
    		freezeCase(req);
    		if ("1".equals(req.getParameter("freezeFlag"))) {
    			activityMsg = "Froze case";
    		} else {
    			activityMsg = "Unfroze case";
    		}
    		
    	} else if (reqType.equals("follow")) {
    		followCase(req);
    		activityMsg = "Followed case";
    		
    	} else if (reqType.equals("modifyOwner")) {
    		modifyOwner(req);
    		activityMsg = "Modified owner";
    		
    	} else if (reqType.equals("editDealer")) {
    		editDealer(req);
    		activityMsg = "Updated dealer";
    		
    	} else if (reqType.equals("changeDealer")) {
    		changeDealer(req);
    		activityMsg = "Changed dealer";
    		
    	} else {
    		
    		SMTActionInterface sai = new ManageTicketAction(actionInit);
    		sai.setDBConnection(dbConn);
    		sai.setAttributes(attributes);
    		sai.build(req);
    		
    	   	if (reqType.equals("manageTicket")) {
        		activityMsg = "Added ticket";
        		
        	}  else if (reqType.equals("updateComments")) {
        		activityMsg = "Updated ticket comments";
        		
        	} else if (reqType.equals("deleteFile")) {
        		activityMsg = "Deleted file";
        		
        	} else if (reqType.equals("closeTicket")) {
        		activityMsg = "Closed ticket";
        			
        	}
    		
    	}
    	
    	// log the msg
    	StringBuilder url = new StringBuilder();
    	url.append("result?vehicleId=").append(req.getParameter("vehicleId"));
    	
    	if (activityMsg != null) {
    		logActivity(req, activityMsg);
    	} else {
    		url.append("&msg=We were unable to process your update.  Please contact your system administrator.");
    		
    	}
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
     */
    private String checkFollowers(SMTServletRequest req) {
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
    		log.error("Error retrieving");
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
	    		// this is an add or change to different owner
	    		// find out if there is an existing profile for this person
	    		profileId = pm.checkProfile(user, dbConn);
	    		if (StringUtil.checkVal(profileId).length() > 0) {
	    			// found a profile, use it as owner
	    			user = pm.getProfile(profileId, dbConn, "PROFILE_ID");
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
     * @throws ActionException
     */
    private void updateOwner(SMTServletRequest req, UserDataVO owner) throws ActionException {
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
    protected void logActivity(SMTServletRequest req, String comment) throws ActionException {
    	log.debug("logging activity...");
    	String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("INSERT INTO ").append(customDb).append(" VENTURE_ACTIVITY_TRAIL ");
    	sb.append("(VENTURE_ACTIVITY_TRAIL_ID, VENTURE_VEHICLE_ID, COMMENT, PROFILE_ID, CREATE_DT) ");
    	sb.append("VALUES (?,?,?,?,GETDATE())");
    	
    	PreparedStatement ps = null;
    	try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter("vehicleId"));
			ps.setString(3, comment);
			ps.setString(4, req.getParameter("submissionId"));
			
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
     
}
