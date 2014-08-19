package com.fastsigns.scripts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.fastsigns.action.tvspot.ContactField;
import com.fastsigns.action.tvspot.Status;
import com.fastsigns.action.tvspot.TVSpotReportVO;
import com.fastsigns.action.tvspot.TVSpotConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.SMTMailHandler;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.contact.ContactDataAction;
import com.smt.sitebuilder.action.contact.ContactDataActionVO;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: TVSpotEmailer.java<p/>
 * <b>Description: Handles the nightly email notifications sent out-of-band in 
 * relation to the Q2 2014 TV spot commercial/campaign.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 3, 2014
 * @updates
 * 	JM 07.02.14	refactored to use TVSpotConfig API, for country support.
 ****************************************************************************/
public class TVSpotEmailer extends CommandLineUtil {
	
	private boolean isSurveyRun = false;
	private boolean isCenter = false;
	private Map<String, Object> attributes;
	private TVSpotConfig config = null;

	public TVSpotEmailer(String[] args) {
		super(args);
		loadProperties("scripts/fts_TVSpot.properties");
		loadDBConnection(props);
		isSurveyRun =  (args != null && args.length > 0 && "survey".equals(args[0]));
		isCenter =  (args != null && args.length > 0 && "center".equals(args[0]));
		
		//load the country config.  This comes off the command line so we can schedule different countries to run at different times (using the same config)
		String country = (args != null && args.length > 1) ? args[1] : "US";
		config = TVSpotConfig.getInstance(country);
		
		makeAttribMap();
	}

	/**
	 * Set up the arguments for creating the Message Sender
	 * @throws ApplicationException
	 */
	private void makeAttribMap() {
		attributes = new HashMap<String, Object>();
		attributes.put("defaultMailHandler", new SMTMailHandler(props));
		attributes.put("instanceName", props.get("instanceName"));
		attributes.put("appName", props.get("appName"));
		attributes.put("adminEmail", props.get("adminEmail"));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TVSpotEmailer emailer = new TVSpotEmailer(args);
		emailer.run();
	}
	
		
	/**
	 * handles script invocation, to ensure all steps are executed in the proper sequence.
	 */
	public void run() {
		Date start = new Date();
		log.info("starting TVSpotEmailer at" + start);
		
		//load all the inquiries that have a status of pending
		ContactDataContainer cdc;
		try {
			//Attempt to create the message sender
			cdc = loadContactData();
			
			//only send the 7-day surveys if that's what we were invoked to do. 
			if (isSurveyRun) {
				sendSurveys(cdc);
				return;
			}
			
			//for the ones that are 1 day old, send the 1-day notification
			sendFirstNotice(cdc);
			
			//send reports to corporate or center owners based on the command line arguments
			if (isCenter) {
				sendCenterReport(cdc);
			} else {
				sendCorpReport(cdc);
			}
			
		} catch (ActionException e) {
			log.error("Could not create ContactDataContainer. ", e);
		}
		
		log.info("finished TVSpotEmailer in " + ((System.currentTimeMillis()-start.getTime()) /1000) + " seconds");
	}
	
	/**
	 * Creates the ContactDataContainer via the ContactDataAction
	 * @return
	 * @throws ActionException
	 */
	private ContactDataContainer loadContactData() throws ActionException {
		log.info("loading Contact data");
		ContactDataContainer cdc = null;
		
		// Create the VO that contains the information we need to send
		// to the ContactDataAction
		ContactDataActionVO vo = new ContactDataActionVO();
		vo.setContactId(props.getProperty("contactFormId" + config.getCountryCode()));
		vo.setEnd(Convert.getCurrentTimestamp().toString());
		
		// Create and set up the ContactDataAction
		ContactDataAction cda = new ContactDataAction();
		cda.setDBConnection(new SMTDBConnection(dbConn));
		cda.setAttribute("encryptKey", props.get("encryptKey"));
		cda.setAttribute("binaryDirectory", props.get("binaryDirectory"));
		
		cdc = cda.createCDC(vo, props.getProperty("hostName" + config.getCountryCode()));
		log.debug(cdc.getData().size());
		return cdc;
	}
	
	/**
	 * Send out any surveys for requests that are seven days old.  
	 * Eight and nine days as well on Mondays since the script
	 * will not be run on the weekends.
	 * @param cdc
	 */
	private void sendSurveys(ContactDataContainer cdc) {
		log.info("sending survey emails");
		Calendar now = Calendar.getInstance();
		EmailMessageVO msg;
		boolean isMonday = now.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
		int daysBetween;

		MessageSender ms = new MessageSender(attributes, dbConn);
		for (ContactDataModuleVO vo : cdc.getData()) {
			//calculate the days between today and the date the submission was created.
			daysBetween = (int) ((vo.getSubmittalDate().getTime() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24));
			log.debug("daysBetween=" + daysBetween);
			switch (daysBetween) {
				case -8:
				case -9: 
					if (!isMonday) continue;
				case -7:
					try {
						msg = config.buildSurveyEmail(vo.getContactSubmittalId());
						msg.addRecipient(vo.getEmailAddress());

						ms.sendMessage(msg);
					} catch (InvalidDataException e) {
						log.error("Could not create email for submittal: " + vo.getContactSubmittalId(), e);
					}
			}
		}
	}
	

	/**
	 * Run through the data we received and send first notice emails
	 * to all franchisees that have not responded to a day old submission
	 * @param cdc
	 */
	private void sendFirstNotice(ContactDataContainer cdc) {
		log.info("sending first notice email");
		Calendar now = Calendar.getInstance();
		EmailMessageVO msg;
		Status status = null;
		int daysBetween;
		boolean isMonday = now.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
		MessageSender ms = new MessageSender(attributes, dbConn);
		
		for (ContactDataModuleVO vo : cdc.getData()) {
			// Make sure that this is a submittal that requires a first notice to be sent out.
			daysBetween = (int) ((vo.getSubmittalDate().getTime() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24));
			log.debug("notice daysBetween: " + daysBetween);
			
			switch (daysBetween) {
				case -2:
				case -3: 
					if (!isMonday) continue;
				case -1:
					String sts = vo.getExtData().get(config.getContactId(ContactField.status));
					try {
						status = Status.valueOf(sts);
					} catch (Exception e) {
						log.error("could not determine status for contactSubmttalId=" + vo.getContactSubmittalId() + " reported: " + sts);
						continue;
					}
					
					//this email only goes out the day after the record was created, and only if the status is unchanged.
					if (status != Status.initiated) continue;
	
					try {
						msg = config.buildFirstNoticeEmail(vo);
						msg.addRecipient(vo.getDealerLocation().getOwnerEmail());
						ms.sendMessage(msg);
					} catch (InvalidDataException e) {
						log.error("Could not create email for submittal: " + vo.getContactSubmittalId(), e);
					}
			}
		}
	}

	/**
	 * Send an excel document containing a summary of all the information 
	 * gathered by this contact us form to date to corporate
	 * @param cdc
	 */
	private void sendCorpReport(ContactDataContainer cdc) {
		log.info("sending corp report email");
		TVSpotReportVO report = new TVSpotReportVO();
		report.setData(new GenericVO(cdc, config));
		
		MessageSender ms = new MessageSender(attributes, dbConn);
		try {
			EmailMessageVO msg = config.buildCorpReportEmail();
			msg.addAttachment(report.getFileName(), report.generateReport());
			ms.sendMessage(msg);
		} catch (InvalidDataException e) {
			log.error("Could not create email for submittal: ", e);
		}
	}
	
	private void sendCenterReport(ContactDataContainer cdc) {
		log.info("sending Center report emails");
		TVSpotReportVO report = new TVSpotReportVO();
		report.setData(new GenericVO(cdc, config));
		Map<String, HSSFWorkbook> byCenter = report.generateCenterReport();
		
		EmailMessageVO msg;
		MessageSender ms = new MessageSender(attributes, dbConn);
		for (String email : byCenter.keySet()) {
			try {
				if (email == null) continue;
				msg = config.buildCenterReportEmail();
				msg.addRecipients(email);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byCenter.get(email).write(baos);
				msg.addAttachment(report.getFileName(), baos.toByteArray());
				
				ms.sendMessage(msg);
				
			} catch (InvalidDataException e) {
				log.error("Could not create email for submittal: ", e);
			} catch (IOException e) {
				log.error("Could not serialize report. ", e);
			}
		}
	}
}
