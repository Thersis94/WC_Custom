package com.venture.cs.action;

// JDK 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;










// Apache Log4j
import org.apache.log4j.Logger;










// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

import com.siliconmtn.util.StringUtil;
// WebCrescendo 2.0
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageSender;

// WC_Custom
import com.venture.cs.action.vo.VehicleVO;
import com.venture.cs.message.AbstractCaseMessage;
import com.venture.cs.message.CaseMessageFormatter;
import com.venture.cs.message.CaseMessageFormatter.CaseMessageType;

/****************************************************************************
 *<b>Title</b>: CaseNotificationManager<p/>
 * Notifies all the users that are following vehicles <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 * Changes:
 * July 23, 2013: Eric Damschroder: created class
 * Mar 11, 2014: DBargerhuff: added additional comments
 ****************************************************************************/

public class CaseNotificationManager {
	
	private final int millPerDay = 86400000;
	private MessageSender sndr = null;
	protected static Logger log = null;
	private String message = null;
	private Connection dbConn = null;
	private Map<String, Object> attributes;
	private String customDb = null;
	private String encKey = null;
	private int daysBetweenNotifications = 5;
	
	/**
	 * Constructor
	 * @param attributes
	 * @param dbConn
	 */
	public CaseNotificationManager(Map<String, Object> attributes, Connection dbConn) {
		sndr = new MessageSender(attributes, dbConn);
		log = Logger.getLogger(this.getClass());
		customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		encKey = (String) attributes.get(Constants.ENCRYPT_KEY);
		this.dbConn = dbConn;
		this.attributes = attributes;
	}
	
	/**
	 * Constructor
	 * @param attributes
	 * @param dbConn
	 * @param message
	 */
	public CaseNotificationManager(Map<String, Object> attributes, Connection dbConn, String message) {
		this(attributes, dbConn);
		this.message = message;
	}
	
	/**
	 * 
	 */
	public void findDueTickets () {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT vt.VENTURE_VEHICLE_ID, p.EMAIL_ADDRESS_TXT, vt.CREATE_DT ");
		sb.append("FROM ").append(customDb).append("VENTURE_TICKET vt ");
		sb.append("left join PROFILE p on p.PROFILE_ID = vt.SUBMITTED_BY ");
		sb.append("WHERE ACTION_REQ_FLG = 1 ");

		PreparedStatement ps;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			StringEncrypter se = new StringEncrypter(encKey);
			
			while (rs.next()) {
				int diffInDays = (int) (Convert.getCurrentTimestamp().getTime() - rs.getTimestamp(3).getTime()/millPerDay);
				log.debug(diffInDays);
				if(diffInDays % daysBetweenNotifications == 0)
					sendNotifications(rs.getString(1), se.decrypt(rs.getString(2)));
			}
		} catch (SQLException e) {
			log.debug("Could not get list of tickets still requiring action ", e);
		} catch (EncryptionException e) {
			log.error("Could not decrypt submitter email address ", e);
		}
	}
	
	/**
	 * Send notification emails to all users following this vehicle as well as to the provided email address
	 * @param vehicleId
	 * @param submitterEmail
	 */
	public void sendNotifications(String vehicleId, String submitterEmail) {

    	try {
	    	ArrayList<String> rcpts = getRcpts(vehicleId);
    		if(rcpts.size() < 1 ) return;
    		
    		//Build the message
	    	EmailMessageVO msg = new EmailMessageVO();
	    	msg.setHtmlBody(message);
	    	msg.setTextBody(message);
	    	
	    	//Add the submitter
	    	msg.addRecipient(submitterEmail);
	    	
	    	//Add all the followers
			for(String rcpt : rcpts) {
				log.debug(rcpt);
				msg.addRecipient(rcpt);
			}
	    	msg.setSubject("Venture RV Case " + vehicleId);
	    	msg.setFrom("no-reply@admintool.webcrescendo.com");
	    	sndr.sendMessage(msg);

		} catch (InvalidDataException e) {
			log.error("Invalid email address ", e);
		} catch (SQLException e) {
			log.error("Could not get list of users to send emails to ", e);
		}  catch (EncryptionException e) {
			log.error("Unable to decode email address ", e);
		}
	}
	
	/**
	 * Gets the list of people that need to be notified about this vehicle
	 * @param id
	 * @return
	 * @throws SQLException
	 * @throws EncryptionException 
	 */
	private ArrayList<String> getRcpts(String id) throws SQLException, EncryptionException { 
		StringBuilder sb = new StringBuilder();
		StringEncrypter se = new StringEncrypter(encKey);
		
		//Get the email addresses of all users following this vehicle
		sb.append("SELECT p.EMAIL_ADDRESS_TXT ");
		sb.append("FROM Web_Crescendo_SB_Custom.dbo.VENTURE_NOTIFICATION vn ");
		sb.append("left join PROFILE p on p.PROFILE_ID = vn.PROFILE_ID ");
		sb.append("WHERE vn.VENTURE_VEHICLE_ID = ?");
		log.debug(sb.toString()+"|"+id);
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		ps.setString(1, id);
		
		ResultSet rs = ps.executeQuery();
		
		// Decrypt the email addresses and add them to the list
		ArrayList<String> list = new ArrayList<String>();
		while (rs.next()){
			list.add(se.decrypt(rs.getString(1)));
		}
		
		return list;
		
	}
	
	public void notifySharedCase(SMTServletRequest req, List<VehicleVO> vehicles, String caseUrl) 
			throws ActionException {
		log.debug("Notifying user of shared case...");
		EmailMessageVO emo = null;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		try {
			// instantiate the message formatter
			AbstractCaseMessage acm = CaseMessageFormatter.getMessage(CaseMessageType.SHARED);
			acm.setReq(req);
			acm.setVehicles(vehicles);
			acm.setCaseUrl(caseUrl);
			
			// populate the subject/body of the message
			emo = new EmailMessageVO();
			emo.setSubject(acm.getMessageSubject());
			emo.setHtmlBody(acm.getMessageBodyHTML());
			emo.setTextBody(acm.getMessageBodyText());
			emo.setFrom(site.getMainEmail());
			
			// TODO REMOVE after testing emo.addRecipient("dave@siliconmtn.com");
			
			// retrieve recipients profile ID from the compound value on the param (pipe-delimited, as in profileId|name)
			String profileId = StringUtil.checkVal(req.getParameter("sharedCaseRecipient"));
			int index = profileId.indexOf("\\|");
			if (index > -1) {
				profileId = profileId.substring(0,index);
				List<String> ids = new ArrayList<String>();
				ids.add(profileId);
				// retrieve user profile/email address.
				try {
					ids = this.retrieveProfileEmail(ids);
					if (ids != null && ids.size() > 0) {
						emo.addRecipient(ids.get(0));
						//log.debug("would add recipient: " + ids.get(0));
					}
				} catch (Exception e) {
					// Exception logged upstream.
				}
			}
			
			// if lookup failed, default to main site email.
			if (emo.getRecipient() == null || emo.getRecipient().isEmpty()) {
				emo.addRecipient(site.getMainEmail());
			}
			
		} catch (InvalidDataException ide) {
			log.error("Error setting addresses on case EmailMessageVO, ", ide);
			throw new ActionException(ide.getMessage());
		}
		
		// send message
		sndr.sendMessage(emo);
	}
	
	/**
	 * Notifies site administrators of the activity that took place for the specified case (vehicleId).
	 * @param vo
	 */
	public void notifySiteAdmins(SMTServletRequest req, List<VehicleVO> vehicles) 
			throws ActionException {
		log.debug("Notifying site admins of case activity...");
		// retrieve site admins
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		List<String> adminRecipients = retrieveSiteAdminEmail(site.getSiteId(), SecurityController.ADMIN_ROLE_LEVEL);
		// format msg
		EmailMessageVO emo = null;
		try {
			AbstractCaseMessage acm = CaseMessageFormatter.getMessage(CaseMessageType.ADMIN);
			acm.setReq(req);
			acm.setVehicles(vehicles);
			
			emo = new EmailMessageVO();
			emo.setSubject(acm.getMessageSubject());
			emo.setHtmlBody(acm.getMessageBodyHTML());
			emo.setTextBody(acm.getMessageBodyText());
			emo.setFrom(site.getMainEmail());
			// TODO REMOVE after testing emo.addRecipient("dave@siliconmtn.com");
			
			// add recipients
			for (String rcpt : adminRecipients) {
				emo.addRecipient(rcpt);
				//log.debug("would add site admin recipient: " + rcpt);
			}
		} catch (InvalidDataException ide) {
			log.error("Error setting addresses on case EmailMessageVO, ", ide);
			throw new ActionException(ide.getMessage());
		}
		
		// send message
		sndr.sendMessage(emo);

	}

	/**
	 * Retrieves site admin email addresses
	 * @return List<String> of site admin email addresses.
	 * @throws ActionException
	 */
	private List<String> retrieveSiteAdminEmail(String siteId, int roleId) throws ActionException {
		List<String> profileIds = null;
		StringBuilder sb = new StringBuilder();
		sb.append("select PROFILE_ID from PROFILE_ROLE where SITE_ID = ? and ROLE_ID = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1,siteId);
			ps.setString(2,String.valueOf(roleId));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (profileIds == null) profileIds = new ArrayList<String>();
				profileIds.add(rs.getString("PROFILE_ID"));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving profile IDs for site admins, ", sqle);
			throw new ActionException(sqle.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		
		return retrieveProfileEmail(profileIds);
		
	}
	
	/**
	 * Retrieves email addresses for the List of profile IDs passed to the method.
	 * @param ids
	 * @return List<String> of email addresses.
	 * @throws ActionException
	 */
	private List<String> retrieveProfileEmail(List<String> ids) throws ActionException {
		List<UserDataVO> profiles = new ArrayList<UserDataVO>();
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			profiles = pm.searchProfile(dbConn, ids);
		} catch (DatabaseException de) {
			log.error("Error retrieving profiles via ProfileManager, ", de);
			throw new ActionException(de.getMessage());
		}
		
		List<String> email = new ArrayList<String>();
		for (UserDataVO user : profiles) {
			email.add(user.getEmailAddress());
		}
		return email;
	}
	
}
