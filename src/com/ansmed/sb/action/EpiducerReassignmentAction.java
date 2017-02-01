package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

//SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SBII Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: EpiducerReassignmentAction.java</p>
 <p>Allows an contact submission</p>
 <p>Copyright: Copyright (c) 2011 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Sept 15, 2011
 Code Updates
 Dave Bargerhuff, Sept 15, 2011 - Creating Initial Class File
 ***************************************************************************/
public class EpiducerReassignmentAction extends SimpleActionAdapter {
	
	//private static final String CONTACT_ACTION_ID = "c0a802412d2d281ea375003c7a8bb443";
	private static final String COURSE_SELECTION_FIELD_ID = "c0a8023751111e8c8200f52c7920a8ab";
	private static final String EPIDUCER_EMAIL_CAMPAIGN_ID = "c0a8023771850933dce429202c15b30f";
	//private static final String CUSTOM_DB_SCHEMA = Constants.CUSTOM_DB_SCHEMA;
	private String msg = null;
	private UserDataVO user;
		
    public EpiducerReassignmentAction() {
        super();
    }

    public EpiducerReassignmentAction(ActionInitVO arg0) {
        super(arg0);
    }
    
    public void list(ActionRequest req) throws ActionException {
    	super.retrieve(req);    	
    }
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(ActionRequest req) throws ActionException {
    	log.debug("Starting EpiducerReassignmentAction build...");
    	StringBuffer url = new StringBuffer(req.getRequestURI());    	
		String id = StringUtil.checkVal(req.getParameter("contactSubmittalId"));
    	
    	if (req.getParameter("processUpdate") != null) {
    		try {
    			this.processSubmittalUpdate(req);
    		} catch (SQLException sqle) {
    			log.error("Error updating user's course record, ", sqle);
    			msg = "Unable to update the physician's course data.";
    		}
    		
    		// process the opt-out if necessary
   			this.processOptOut(req);
	
    	} else {
    		url.append("?contactSubmittalId=").append(id);
    	}
    	    	
		// add the msg to the redirect
		if (msg != null) {
	    	url.append(url.indexOf("?") > -1 ? "&" : "?");
			url.append("msg=").append(msg);
		}
		
		log.debug("redirect url: " + url.toString());
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("EpiducerReassignmentAction retrieve...");
		String id = StringUtil.checkVal(req.getParameter("contactSubmittalId"));
		log.debug("contactSubmittalId: " + id);
		if (id.length() > 0) {
			if (this.checkSubmittalId(id)) {
				this.retrieveUserData(req);
			} else {
				// remove submittalId from request, return 'invalid contact submittal id' message
				req.setParameter("contactSubmittalId", "", true);
				msg = "No physician submission was found for the given contact submittal ID.";
			}
		}
		
		if (msg != null) req.setParameter("msg", msg);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		log.debug("mod is: " + (mod != null ? "not null" : "null"));
		this.putModuleData(user, 1, false, msg);
		attributes.put(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Updates user's course name
	 * @param req
	 */
	private void processSubmittalUpdate(ActionRequest req) throws SQLException {
		if (StringUtil.checkVal(req.getParameter("newCourseValue")).length() == 0) return;
		log.debug("processing submittal...");
		StringBuffer sql = new StringBuffer();
		sql.append("update contact_data set value_txt = ? ");
		sql.append("where contact_submittal_id = ? ");
		sql.append("and contact_field_id = ? ");
		log.debug("update SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		
		ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, req.getParameter("newCourseValue"));
		ps.setString(2, req.getParameter("contactSubmittalId"));
		ps.setString(3, COURSE_SELECTION_FIELD_ID);
		ps.executeUpdate();
		msg = "You have successfully updated the physician's course data.";
		try {
			ps.close();
		} catch (Exception e) {}

	}
	
	/**
	 * Opts user out of Epiducer communication
	 * @param req
	 * @throws SQLException
	 */
	private void processOptOut(ActionRequest req) {
		if (StringUtil.checkVal(req.getParameter("optOut")).length() == 0) return;
		if (StringUtil.checkVal(req.getParameter("profileId")).length() == 0) {
			msg = "No profile ID provided for physician. Unable to update opt-out data.";
			return;
		}
		log.debug("processing opt-out...");
		// update email history first
		StringBuffer upd = new StringBuffer();
		upd.append("insert into email_permission_history ");
		upd.append("(email_permission_history_id, profile_id,email_campaign_id, ");
		upd.append("allow_comm_flg, orig_create_dt, create_dt)  ");
		upd.append("select email_permission_id, profile_id, a.email_campaign_id, ");
		upd.append("allow_comm_flg, a.create_dt, ? ");
		upd.append("from email_permission a inner join email_campaign b  ");
		upd.append("on a.email_campaign_id = b.email_campaign_id  ");
		upd.append("and organization_id = ? where profile_id = ? ");
		log.debug("email permission history SQL: " + upd.toString());
		log.debug("profileId: " + req.getParameter("profileId"));
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(upd.toString());
			ps.setDate(1, new java.sql.Date(new Date().getTime()));
			ps.setString(2, "ANS-MEDICAL");
			ps.setString(3, req.getParameter("profileId"));
			int count = ps.executeUpdate();
			log.debug("history rows inserted: " + count);
		} catch (SQLException sqle) {
			log.error("Error inserting email permission history record, ", sqle);
		}
    	if (ps != null) {
        	try {
        		ps.close();
        	} catch(Exception e) {}
    	}
    	
    	// delete the existing record
		StringBuffer del = new StringBuffer();
		del.append("delete from email_permission ");
		del.append("where email_campaign_id = ? and profile_id = ?");
		log.debug("delete sql: " + del.toString());
		
		ps = null;
		try {
			ps = dbConn.prepareStatement(del.toString());
			ps.setString(1, EPIDUCER_EMAIL_CAMPAIGN_ID);
			ps.setString(2, req.getParameter("profileId"));
			int count = ps.executeUpdate();
			log.debug("rows deleted: " + count);
		} catch (SQLException sqle) {
    		log.error("Error deleting email permissions: ",sqle);
    	} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
        }
    	
    	// insert new record
    	StringBuffer ins = new StringBuffer();
		ins.append("insert into email_permission (email_permission_id, ");
		ins.append("email_campaign_id, profile_id, allow_comm_flg, create_dt) ");
		ins.append("values (?,?,?,?,?)");
		
		log.debug("insert email permission SQL: " + ins.toString());
		log.debug("email_campaign_id | profileId | allow_comm_flg: " + EPIDUCER_EMAIL_CAMPAIGN_ID + " | " + req.getParameter("profileId") + " | " + 0);
		ps = null;
		try {
			ps = dbConn.prepareStatement(ins.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, EPIDUCER_EMAIL_CAMPAIGN_ID);
			ps.setString(3, req.getParameter("profileId"));
			ps.setInt(4, 0);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			int count = ps.executeUpdate();
			log.debug("email perm rows inserted: " + count);
			if (msg != null) {
				msg = msg + " You have successfully updated the physician's opt-out data.";
			} else {
				msg = "You have successfully updated the physician's opt-out data.";
			}
		} catch (SQLException sqle) {
			log.error("Error processing user opt-out, ", sqle);
			msg = "Unable to insert physician's opt-out data.";
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
        }
	}
	
	/**
	 * Makes sure that submitted contact submittal ID is valid.
	 * @param submittalId
	 * @return
	 */
	private boolean checkSubmittalId(String submittalId) {
		boolean isValid = false;
		StringBuffer sql = new StringBuffer();
		sql.append("select a.contact_submittal_id, a.profile_id, a.email_address_txt, ");
		sql.append("b.value_txt from contact_submittal a ");
		sql.append("inner join contact_data b on a.contact_submittal_id = b.contact_submittal_id ");
		sql.append("where a.contact_submittal_id = ? and b.contact_field_id = ? ");
		log.debug("checkSubmittal SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, submittalId);
			ps.setString(2, COURSE_SELECTION_FIELD_ID);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				log.debug("contact submittal id was found...");
				isValid = true;
				UserDataVO user = new UserDataVO();
				user.setProfileId(rs.getString("profile_id"));
				user.setEmailAddress(rs.getString("email_address_txt"));
				user.addAttribute("contactSubmittalId", rs.getString("contact_submittal_id"));
				user.addAttribute("currentCourse", rs.getString("value_txt"));
				this.setUser(user);
			}
		} catch (SQLException sqle) {
			msg = "Error validating contact submittal id.";
			log.error("Error retrieving contact submittal id, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return isValid;
	}
	
	/**
	 * Retrieves physician's name info from the custom db table
	 */
	private void retrieveUserData(ActionRequest req) {
		log.debug("retrieving user data...");
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		// get physician data
		StringBuffer sql = new StringBuffer();
		sql.append("select first_nm, last_nm from ");
		sql.append(schema).append("ans_surgeon ");
		sql.append("where prod_approval_flg > 0 and profile_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, user.getProfileId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				user.setFirstName(rs.getString("first_nm"));
				user.setLastName(rs.getString("last_nm"));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving physician info, ", sqle);
			msg = "Unable to retrieve the physician's name information.";
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * @return the user
	 */
	public UserDataVO getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(UserDataVO user) {
		this.user = user;
	}
	
}
