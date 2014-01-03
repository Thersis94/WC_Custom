package com.venture.cs.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 *<b>Title</b>: EmailNotification<p/>
 * Notifies all the users that are following vehicles <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class EmailNotification {
	private final int millPerDay = 86400000;
	
	private MessageSender sndr = null;
	protected static Logger log = null;
	private String message = "";
	private Connection dbConn = null;
	private String customDb = "";
	private String encKey;
	private int daysBetweenNotifications = 5;
	
	
	
	public EmailNotification(Map<String, Object> attributes, Connection dbConn) {
		sndr = new MessageSender(attributes, dbConn);
		log = Logger.getLogger(this.getClass());
		customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		encKey = (String) attributes.get(Constants.ENCRYPT_KEY);
		this.dbConn = dbConn;
	}
	
	public EmailNotification(Map<String, Object> attributes, Connection dbConn, String message) {
		this(attributes, dbConn);
		this.message = message;
	}
	
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
}
