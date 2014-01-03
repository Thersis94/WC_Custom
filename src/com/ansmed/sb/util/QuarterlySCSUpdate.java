package com.ansmed.sb.util;

// JDK 1.6
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

// log4j 1.2
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SB Libs 2.0
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;

// SB ANS Medical libs
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;

/****************************************************************************
 * <p><b>Title</b>:QuarterlySCSUpdate.java<p/>
 * <p><b>Description: Determines if 'today' is the start of a new SJM business 
 * quarter and then archives SJM surgeon business plan records for previous 
 * quarter.</b><p/>
 * 
 * <p><b>Copyright:</b> Copyright (c) 2008<p/>
 * <p><b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Dec 18, 2008
 * <p><b>Changes: </b>
 * 08-27-09 DBargerhuff: Added SJMBusinessCalendar for calculating SJM bus dates.
 * 08-31-09 DBargerhuff: Added Email notification.</p>
 ****************************************************************************/
public class QuarterlySCSUpdate {
	protected String schema = null;
	private static Logger log = null;
	private Connection conn = null;
	private String dbDriver = null;
	private String dbUrl = null;
	private String dbUser = null;
	private String dbPwd = null;
	private String smtpServer;
	private int smtpPort;
	private String smtpUser;
	private String smtpPwd;
	private String smtpRcpt;
	private String smtpFrom;
	
	/**
	 * 
	 */
	public QuarterlySCSUpdate() {
		// Get the Config
		Properties p = loadConfiguration("scripts/ans_config.properties");
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		log = Logger.getLogger(QuarterlySCSUpdate.class);
		schema = p.getProperty("sbANSSchema");
		
		dbDriver = p.getProperty("dbDriver");
		dbUrl = p.getProperty("dbUrl");
		dbUser = p.getProperty("dbUser");
		dbPwd = p.getProperty("dbPassword");
		
		smtpServer = p.getProperty("smtpServer");
		smtpPort = Convert.formatInteger(p.getProperty("smtpPort"));
		smtpUser = p.getProperty("smtpUser");
		smtpPwd = p.getProperty("smtpPassword");
		smtpRcpt = p.getProperty("smtpRecipient");
		smtpFrom = p.getProperty("smtpFrom");
		
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean success = false;
		String msg = null;
		int recordCount = 0;
		int prevQtr = 0;
		int prevYr = 0;
		int currQtr = 0;
		int currYr = 0;
		
		// Instantiate, set properties, obtain logger.
		QuarterlySCSUpdate qu = new QuarterlySCSUpdate();
		
		Calendar cal = GregorianCalendar.getInstance();
		
		// Get SJM business calendar for current year
		SJMBusinessCalendar sbc = null;
		
		try {
			sbc = new SJMBusinessCalendar(cal);
			success = true;
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving SJM business calendar.", ice);
			success = false;
			msg = "SJM QA-ERROR: task failed retrieving business calendar.";
		}
		
		if (success) {
			log.debug("Calendar year is: " + sbc.getCurrentYear());
			
			// Get today as a Date object.
			java.util.Date today = cal.getTime();
			
			// If today is not the start of a new business quarter, exit.
			if (! (sbc.isStartOfBusQtr(today))) {
				log.info(today + " is not the start of a new SJM business quarter.  Exiting.");
				//Set 'success' to false even though this is not an error condition.
				success = false;
				msg = "SJM QA-INFO: task completed normally.  Nothing to process today.";
			}
		}
		
		if (success) {
			/* If 'today' starts a new business quarter, get the previous and 
			 * current quarter/year values.
			 */
			currQtr = sbc.getCurrentQuarter();
			currYr = sbc.getCurrentYear();
			prevQtr = sbc.getPreviousQuarter();
			prevYr = currYr;
			
			//if previous quarter was last year, set previous year to last year.
			if (prevQtr == 4) --prevYr;
			
			log.info("Attempting to archive records for Q" + prevQtr + ", " + prevYr 
					+ " and inserting records for Q" + currQtr + ", " + currYr);

			// open a db connection
			try {
				qu.openDBConnection();
				success = true;
			} catch (Exception e) {
				log.error("Could not open a db connection.  Exiting.", e);
				success = false;
				msg = "SJM QA-ERROR: task failed with no db connection!";
			}
		}
		
		// Run the update
		if (success) {
			try {
				recordCount = qu.performUpdate(prevQtr, prevYr, currQtr, currYr);
				success = true;
			} catch (SQLException sqle) {
				log.error("Error archiving records for Q" + prevQtr + ", " + prevYr 
						+ " error inserting business plan records for Q" + currQtr 
						+ ", " + currYr, sqle);
				success = false;
				msg = "SJM QA-FAILURE: task failed trying to archive/insert records! Check logs!";
			}
		}
		
		//Close the db connection
		if (success) {
			try {
				qu.closeDBConnection();
				success = true;
			} catch (SQLException sqle) {
				log.error("Error closing db connection. Exiting.",sqle);
				success = false;
				msg = "SJM QA-ERROR: task failed closing db connection.  Check logs!";
			}
		}
		
		// if msg is null, means 'success' was successful archive/insert.
		if (success && (msg == null)) msg = "SJM QA-SUCCESS: Archived/inserted " + recordCount + " records.";
		
		// send admin email
		try {
			qu.sendAdminMail(qu.smtpFrom, msg);
		} catch (MailException me) {
			log.error("Error sending admin email notification with message: " + msg 
					+ " : exception is: ", me);
		}
		
		// Complete
		log.info("Quarterly archive task exiting...");
	}
	
	/**
	 * Archives previous quarter's business plan records and inserts copies of 
	 * the records using the new quarter/year values.
	 * @param prevQtr
	 * @param prevYr
	 * @param newQtr
	 * @param newYr
	 */
	protected int performUpdate(int prevQtr, int prevYr, int newQtr, int newYr)
	throws SQLException {
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ANS_XR_SURGEON_BUSPLAN ( ");
		sql.append("SURGEON_BUSPLAN_ID, BUSINESS_PLAN_ID, SURGEON_ID,SELECTED_FLG,  ");
		sql.append("VALUE_TXT, BP_YEAR_NO, BP_QUARTER_NO, CREATE_DT )  ");
		sql.append("select replace(NEWID(), '-', ''), BUSINESS_PLAN_ID, SURGEON_ID, ");
		sql.append("SELECTED_FLG, VALUE_TXT, ?, ?, GETDATE() ");
		sql.append("from ").append(schema).append("ANS_XR_SURGEON_BUSPLAN ");
		sql.append("where BP_YEAR_NO = ? and BP_QUARTER_NO = ? ");
		
		log.debug("Update SQL: " + sql);
		log.debug("SQL params: curr year(for quarter)|curr quarter|prev " +
				"year(for quarter)|prev quarter: " 
				+ newYr + "|" + newQtr + "|" + prevYr + "|" + prevQtr);
		
		PreparedStatement ps = null;
		ps = conn.prepareStatement(sql.toString());
		ps.setInt(1, newYr);
		ps.setInt(2, newQtr);
		ps.setInt(3, prevYr);
		ps.setInt(4, prevQtr);
			
		// Execute the update
		int count = ps.executeUpdate();
		log.info("Number of business plan records archived/inserted: " + count);
		return count;
	}
	
	/**
	 * Loads the config properties to be used in the app
	 * @param path
	 * @return
	 */
	public Properties loadConfiguration(String path) {
		Properties config = new Properties();
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(path);
			config.load(inStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {}
			}
		}
		
		return config;
	}
	
	/**
	 * Opens a DB connection
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void openDBConnection()
	throws InvalidDataException, DatabaseException {
		
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPwd);
		this.conn = dbc.getConnection();
	}
	
	/**
	 * Closes a DB connection
	 * @throws SQLException
	 */
	public void closeDBConnection()
	throws SQLException {
		this.conn.close();
	}
	
	/**
	 * Sends email notification to admin of result of task execution.
	 * @param fromUser
	 * @param subject
	 * @throws MailException
	 */
	public void sendAdminMail(String fromUser, String subject) throws MailException {
		log.info("Starting admin email send...");
		SMTMail mail = new SMTMail(smtpServer,smtpPort,smtpUser,smtpPwd);
		mail.setFrom(fromUser);
		mail.setRecpt(new String[] {smtpRcpt});
		mail.setSubject(subject);
		mail.setReplyTo(smtpRcpt);
		mail.setHtmlBody("No body specified.");
		
		// send the message
		mail.postMail();
		log.info("Email successfully sent to " + smtpRcpt);
	}

}
