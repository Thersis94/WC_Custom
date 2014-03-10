package com.ansmed.datafeed.ambassador;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.survey.SurveyDataContainer;
import com.smt.sitebuilder.action.survey.SurveyDataModuleVO;

// SB ANS Medical Libs
import com.ansmed.datafeed.ZipStreamWriter;
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;
import com.ansmed.sb.util.calendar.SJMDateBean;

/****************************************************************************
 * <p><b>Title</b>: AmbassadorRollupReport.java<p/>
 * <p><b>Description: Retrieves the data for the Ambassador survey submittal and
 * exports the data in an Excel file.  This class is based heavily on the class 
 * com.smt.sitebuilder.action.survey.SurveyDataAction with modifications made to
 * fit the needs of a datafeed class. The start/end date range for the report 
 * are determined by the SJMBusinessCalendar object.<p/>
 * <p><b>Copyright:</b> Copyright (c) 2009<p/>
 * <p><b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 27, 2009
 * Change log:
 * 08/19/2009: Dave Bargerhuff - integrated SJMBusinessCalendar for start/end
 * date calculation.  Report will only run on the start of each new SJM business
 * month. 
 ****************************************************************************/

public class AmbassadorRollupReport {
	
	private static Logger log = Logger.getLogger(AmbassadorRollupReport.class);
	private Connection conn = null;
	private Properties props = new Properties();
	private InputStream inStream = null;
	private String configPath = "scripts/ambassador_config.properties";
	private String logConfigPath = "scripts/ambassador_log4j.properties";
	private Date dateStart;
	private Date dateEnd;
	private String surveyId;
	private String xmlWrapper;
	private String reportFileName;
	private String ftpFileName;
	private boolean useSecureFTP = false;
	private List<String> order = null;
	private SurveyDataContainer cdc = null;
	private String smtpServer;
	private int smtpPort;
	private String smtpUser;
	private String smtpPwd;
	private String smtpRcpt;
	private String smtpFrom;
	private String smtpFail;
	private String smtpSuccess;
	private String bodyMessage;
	
	public AmbassadorRollupReport() {
		this(new Date(GregorianCalendar.getInstance().getTimeInMillis()));
	}
	
	public AmbassadorRollupReport(Date dateIn) {
		
		// dateIn is the run date for this report
		
		order = new ArrayList<String>();
		cdc = new SurveyDataContainer();
		
		PropertyConfigurator.configure(logConfigPath);
		
		// Load the config file.
		loadConfig();
				
		// Set properties that we need
		surveyId = props.getProperty("surveyId");
		xmlWrapper = props.getProperty("xmlWrapper");
		reportFileName = props.getProperty("fileName") + props.getProperty("fileExt");
		ftpFileName = props.getProperty("ftpFileName") + dateIn.toString() + props.getProperty("ftpExt");
		useSecureFTP = Convert.formatBoolean(props.getProperty("useSecureFTP"));
		smtpServer = props.getProperty("smtpServer");
		smtpPort = Convert.formatInteger(props.getProperty("smtpPort"));
		smtpUser = props.getProperty("smtpUser");
		smtpPwd = props.getProperty("smtpPassword");
		smtpRcpt = props.getProperty("smtpRecipient");
		smtpFrom = props.getProperty("smtpFrom");
		smtpFail = props.getProperty("smtpSubjectFail");
		smtpSuccess = props.getProperty("smtpSubjectSuccess");
	}
	
	
	public static void main(String[] args) {
		
		boolean success = false;
		
		// Instantiate ARR and establish logging.
		AmbassadorRollupReport ar = new AmbassadorRollupReport();
		
		SJMBusinessCalendar sbc = null;
		
		Calendar cal = GregorianCalendar.getInstance();
		
		// Get the current year's SJM business calendar.
		try {
			sbc = new SJMBusinessCalendar(cal);
			success = true;
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving initial business calendar.");
			ar.setBodyMessage("Failed retrieving initial calendar.");
			success = false;
		} 
		
		SJMDateBean bn = null;
		
		// If we successfully retrieved a calendar, proceed.
		if (success) {
			// Get today's date.
			java.util.Date today = cal.getTime();
			
			// if 'today' isn't the start of a new business month, exit
			if (! sbc.isStartOfBusMonth(today)) {
				log.info(today.toString() + " is not start of new SJM business month. Exiting.");
				System.exit(-1);
			}
			
			// retrieve the previous business month's start/end dates for the report
			try {
				bn = sbc.getPreviousMonthDate();
				success = true;
			} catch (InvalidCalendarException ice) {
				log.error("Could not retrieve previous month for report - Exiting.", ice);
				StringBuffer msg = new StringBuffer();
				msg.append("Failed retrieving previous month date range for report. ");
				msg.append("Calendar date - Month: " ).append(sbc.getCurrentMonth());
				msg.append(", Year: ").append(sbc.getCurrentYear());
				ar.setBodyMessage(msg.toString());
				success = false;
			} 
		}
		
		// If we successfully retrieved the previous month's date bean, proceed.
		if (success) {
			// set the start/end dates using the SJM business calendar.
			ar.setDateStart(Convert.formatSQLDate(bn.getStartDate()));
			ar.setDateEnd(Convert.formatSQLDate(Convert.formatEndDate(bn.getEndDate())));
			
			// Open a db connection.
			ar.openConnection();
			
			// Get data
			ar.retrieve(ar.surveyId);
	
			// Create Excel file
			AmbassadorReportBuilder arb = new AmbassadorReportBuilder(ar.xmlWrapper);
			arb.addHeaderRow(ar.cdc.getFields());
			arb.addRows(ar.cdc);
			
			// Close the db connection
			ar.closeConnection();
			
			// FTP the Excel file
			try {
				ar.createZipFile(ar.reportFileName,arb.getFileData(),ar.ftpFileName,ar.useSecureFTP);
			} catch (IOException ioe) {
				log.error("Error FTP'ing file.", ioe);
				StringBuffer msg = new StringBuffer();
				msg.append("Failed FTP'ing the Ambassador Tracking report for ");
				msg.append(ar.getDateStart()).append(" through ").append(ar.getDateEnd());
				ar.setBodyMessage(msg.toString());
				success = false;
			}
		}
		
		try {
			if (success) {
				log.debug("success == true...");
				StringBuffer msg = new StringBuffer();
				msg.append("Successfully processed and FTP'd the Ambassador ");
				msg.append("Tracking report for ").append(bn.getStartDate());
				msg.append(" through ").append(bn.getEndDate());
				msg.append("\nNumber of records: ").append(ar.cdc.getCoreData().size());
				ar.setBodyMessage(msg.toString());
				ar.sendAdminMail(ar.smtpFrom,ar.smtpSuccess);
			} else {
				log.debug("success == false...");
				ar.sendAdminMail(ar.smtpFrom,ar.smtpFail);
			}
		} catch (MailException me) {
			log.error("Error sending admin email.",me);
		}
		
	}
	
	/**
	 * Retrieves survey data
	 * @param req
	 */
	public void retrieve(String surveyId) {
		log.info("Processing Ambassador surveys  for date range between: " + this.dateStart + " and " + this.dateEnd);
				
		// retrieve the fields associated to this survey form and the 
		// core data (profile info, action name, etc ..)
		getFields(surveyId, cdc);
		setCoreData(surveyId, cdc);
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("select a.transaction_id, a.survey_question_id,  a.value_txt ");
		sql.append("from survey_response a inner join survey_question b ");
		sql.append("on a.survey_question_id=b.survey_question_id ");
		sql.append("where b.action_id=? and a.create_dt between ? and ? ");
		sql.append("order by a.transaction_id, b.page_no, b.order_no");
		
		log.debug("Ambassador survey report SQL: " + sql);
		log.debug("dateStart: " + this.dateStart);
		log.debug("dateEnd: " + this.dateEnd);
		
		PreparedStatement ps = null;
		Map<String, String[]> data = new HashMap<String, String[]>();
		int i = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(i++, surveyId);
			ps.setDate(i++, this.dateStart);
			ps.setDate(i++, this.dateEnd);
			
			ResultSet rs = ps.executeQuery();
			String[] fd = null;
			String csId = "";
			int cnt = 0;
			while(rs.next()) {
				String newCsId = rs.getString(1); 
				if (! csId.equals(newCsId)) {
					if (cnt > 0) {
						data.put(csId, fd);
						//log.debug("ID: " + csId + " - ValCnt: " + fd.length);
					}
					fd = new String[order.size()];
				}
				
				// Get the appropriate location in the array for the value
				// Concatenate the value if there are multiple values
				// submitted for that entry (check boxes)
				int loc = getLoc(rs.getString(2));
				//log.debug("loc: " + loc);
				if (fd[loc] != null) fd[loc] = fd[loc] + ", " + rs.getString(3);
				else fd[loc] = rs.getString(3);
				
				// Reset the ids for comparison and increment the counter
				csId = newCsId;
				cnt++;
			}
			
			// add the remaining data
			data.put(csId, fd);
			
			// Add the data to the container
			cdc.setExtData(data);
			
		} catch (Exception e) {
			log.error("Error getting SurveyData for action: ", e);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
		
		log.info("Finished retrieving Ambassador survey data: " + cdc.toString());
		
	}

	
	/**
	 * Sets the data that is the same for all contact us forms
	 * @param contactId
	 * @param cdc
	 */
	private void setCoreData(String surveyId, SurveyDataContainer cdc) {
				
		StringBuffer sql = new StringBuffer();
		sql.append("select action_nm, c.transaction_id, site_id, ");
		sql.append("cast(Convert(varchar,c.create_dt,100) as datetime) as create_dt ");
		sql.append("from sb_action a inner join survey b ");
		sql.append("on a.action_id=b.action_id ");
		sql.append("inner join survey_response c ");
		sql.append("on b.action_id=c.action_id ");
		sql.append("where c.action_id = ? ");
		sql.append("and c.create_dt between ? and ? ");
		
		sql.append("group by action_nm, c.transaction_id, ");
		sql.append("cast(Convert(varchar,c.create_dt,100) as datetime), site_id ");
		sql.append("order by create_dt");
		
		log.debug("CORE Data SQL: " + sql);
		log.debug("actionId: " + surveyId);
		log.debug("dateStart: " + this.dateStart);
		log.debug("dateEnd: " + this.dateEnd);
		
		PreparedStatement ps = null;
		List<SurveyDataModuleVO> data = new ArrayList<SurveyDataModuleVO>();
		int i=1;
		String prevId = "";
		String currId = "";
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(i++, surveyId);
			ps.setDate(i++, this.dateStart);
			ps.setDate(i++, this.dateEnd);

			ResultSet rs = ps.executeQuery();
			
			SurveyDataModuleVO vo = null;
			while (rs.next()) {
				// For surveys with multiple pages we need to make sure 
				// that we process the page submissions appropriately.
				currId = StringUtil.checkVal(rs.getString("transaction_id"));
				if (currId.equalsIgnoreCase(prevId)) {
					vo.setData(rs);
				} else {
					if (vo != null) data.add(vo);
					vo = new SurveyDataModuleVO();
					vo.setData(rs);
				}
				
				prevId = currId;
			}
			
			// Make sure we get the last record/VO.
			if (vo != null) data.add(vo);
			
			log.info("Core data size: " + data.size());
			cdc.setCoreData(data);
		} catch (Exception e) {
			log.error("Error retrieving Ambassador survey data: ", e);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	
	/**
	 * Retrieves a list of fields for a given COntact Us Form
	 * @param contactId
	 */
	private void getFields(String surveyId, SurveyDataContainer cdc) {
		StringBuffer sql = new StringBuffer();
		sql.append("select survey_question_id, question_txt ");
		sql.append("from survey_question ");
		sql.append("where action_id = ? ");
		sql.append("order by page_no, order_no, question_txt");
			
		PreparedStatement ps = null;
		List<String> fields = new ArrayList<String>();
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, surveyId);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				order.add(rs.getString(1));
				fields.add(rs.getString(2));
			}
			
			cdc.setFields(fields);
		} catch (Exception e) {
			log.error("Error retrieving Ambassador survey field data: ", e);
		} finally {
        	if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	
	/**
	 * Determines the position of the array to place the data
	 * @param id
	 * @return
	 */
	private int getLoc(String id) {
		for (int i = 0; i < order.size(); i++ ) {
			String fId = order.get(i);
			if (id != null && id.equalsIgnoreCase(fId))
				return i;
		}
		
		return 0;
	}
	
	/** 
	 * Loads the properties configuration file.
	 */
	public void loadConfig() {
		// Load the config file.
		try {
			inStream = new FileInputStream(configPath);
			props.load(inStream);
			log.debug("Successfully loaded config file");
		} catch (FileNotFoundException e){
			log.error("Unable to find configuration file.");
			System.exit(-1);
		} catch (IOException ioe) {
			log.error("Unable to access configuration file.");
			System.exit(-1);
		}
		finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {
					log.error("Could not close file input stream.");
					System.exit(-1);
				}
			}
		}
	}
	
	/**
	 * Establishes a database connection.
	 */
	public void openConnection() {
		DatabaseConnection dbc = new DatabaseConnection(props.getProperty("dbDriver"),
				props.getProperty("dbUrl"), props.getProperty("dbUser"),
				props.getProperty("dbPassword"));
		try {
			conn = dbc.getConnection();
			log.debug("Got a database connection.");
		} catch (Exception de) {
			log.error("Could not obtain a database connection. ",de);
		}
	}
	
	/**
	 * Closes database connection.
	 */
	public void closeConnection() {
		try {
			conn.close();
			log.debug("Closed the db connection.");
		} catch (Exception e) {
			log.error("Could not close db connection.",e);
		}
	}
	
	/**
	 * Creates a zipped archive of the report and FTP's the report.
	 * @param dataFileName
	 * @param fileData
	 * @param ftpFileName
	 * @param secure
	 */
	protected void createZipFile(String dataFileName, StringBuffer fileData,  
			String ftpFileName, boolean secure) throws IOException {
		
		ZipStreamWriter zStream = new ZipStreamWriter();
		// Create the zip stream in the ZipStreamWriter.
		try {
			zStream.createZipStream();
			log.debug("Created zip stream.");
		} catch (IOException ioe) {
			log.error("Could not create zip stream.", ioe);
			throw new IOException();
		}
		
		// Add the filename and file data to the zip stream.
		try {
			zStream.addEntry(dataFileName, fileData.toString().getBytes());
			log.debug("Adding report file zip entry to zip stream.");
		} catch (IOException ioe) {
			log.error("Could not add file entry to zip stream.", ioe);
			throw new IOException();
		}
		
		try {
			zStream.close();
			if (secure) {
				log.info("Using Secure FTP...");
				zStream.secureFTPFile(props.getProperty("sftpHost"),props.getProperty("sftpUser"),
						props.getProperty("sftpPassword"),ftpFileName);				
			} else {
				log.info("Using insecure FTP...");
				zStream.ftpFile(props.getProperty("ftpHost"),
						Convert.formatInteger(props.getProperty("ftpPort")).intValue(),
						props.getProperty("ftpUser"), props.getProperty("ftpPassword"), ftpFileName);	
			}
			log.debug("Successfully FTP'd zipped file to host.");
		} catch (IOException ioe) {
			log.error("Could not FTP file to host.", ioe);
			throw new IOException();
		}
	}
	
	/**
	 * Formats and sends the email message
	 * @throws MailException
	 */
	public void sendAdminMail(String fromUser, String subject) throws MailException {
		log.info("Starting admin email send...");
		SMTMail mail = new SMTMail(smtpServer,smtpPort,smtpUser,smtpPwd);
		mail.setFrom(fromUser);
		mail.setRecpt(new String[] {smtpRcpt});
		mail.setSubject(subject);
		mail.setReplyTo(smtpRcpt);
		mail.setHtmlBody(this.getBodyMessage());
		
		// send the message
		mail.postMail();
		log.info("Email successfully sent to " + smtpRcpt);
	}

	/**
	 * @return the dateStart
	 */
	public Date getDateStart() {
		return dateStart;
	}

	/**
	 * @param dateStart the dateStart to set
	 */
	public void setDateStart(Date dateStart) {
		this.dateStart = dateStart;
	}

	/**
	 * @return the dateEnd
	 */
	public Date getDateEnd() {
		return dateEnd;
	}

	/**
	 * @param dateEnd the dateEnd to set
	 */
	public void setDateEnd(Date dateEnd) {
		this.dateEnd = dateEnd;
	}
	
	/**
	 * sets the body message
	 * @param bodyMessage
	 */
	public void setBodyMessage(String bodyMessage) {
		this.bodyMessage = bodyMessage;
	}
	
	/**
	 * returns the body message
	 * @return
	 */
	public String getBodyMessage() {
		return this.bodyMessage;
	}
	

}
