package com.fastsigns.scripts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.vo.CareersVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.SMTMailHandler;
import com.siliconmtn.util.CommandLineUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: CareerEmailer.java<p/>
 * <b>Description: Handles checks as to whether or not a job posting has been up
 * for six weeks and if so lets the franchise owner know that they need to take
 * action if they desire the job to be removed.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 11, 2014
 * @updates
 ****************************************************************************/
public class CareerEmailer extends CommandLineUtil {
	
	private Map<String, Object> attributes;
	private String orgId;
	
	public CareerEmailer(String[] args) {
		super(args);
		loadProperties("scripts/fts_career_emailer.properties");

		if (args != null && args.length > 0) {
			orgId = args[0];
		} else {
			orgId = "FTS";
		}
		
		loadDBConnection(props);
		
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
		CareerEmailer emailer = new CareerEmailer(args);
		emailer.run();
	}
	
		
	/**
	 * handles script invocation, to ensure all steps are executed in the proper sequence.
	 */
	public void run() {
		Date start = new Date();
		log.info("starting CareerEmailer at" + start);
		try {
			sendNotices();
		} catch (ActionException e) {
			log.error("Could not create ContactDataContainer. ", e);
		}
		
		log.info("finished CareerEmailer in " + ((System.currentTimeMillis()-start.getTime()) /1000) + " seconds");
	}
	
	/**
	 * Get the careers that users need to be reminded of and send out the 
	 * reminder emails
	 * @throws ActionException
	 */
	private void sendNotices() throws ActionException {
		log.info("sending job reminder emails");
		EmailMessageVO msg;
		MessageSender ms = new MessageSender(attributes, dbConn);
		CareersVO career;
		
		for (GenericVO vo : getCareerOptions()) {
			career = (CareersVO) vo.getValue();
			try {
				msg = buildEmail((String)vo.getKey(), career);
				ms.sendMessage(msg);
			} catch (InvalidDataException e) {
				log.error("Invald email address for center " + career.getFranchiseId() + " and job " + career.getJobTitleNm());
			}
		}
	}
	
	/**
	 * Gets all center careers that are a multiple of 42 days old and the center
	 * owner's email address
	 * @return
	 */
	private List<GenericVO> getCareerOptions() {
		List<GenericVO> careers = new ArrayList<GenericVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select fjp.*, dl.LOCATION_NM, dl.STATE_CD, dl.CITY_NM, dl.ATTRIB2_TXT, dl.LOCATION_OWNER_EMAIL_TXT from ");
		sb.append(props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING fjp left outer join DEALER_LOCATION dl on fjp.FRANCHISE_ID = dl.DEALER_LOCATION_ID ");
		sb.append("where fjp.ORGANIZATION_ID = ? and DATEDIFF(DAY, JOB_POST_DT, GETDATE()) != 0 ");
		sb.append("and DATEDIFF(DAY, JOB_POST_DT, GETDATE()) % 42 = 0 and FRANCHISE_ID is not null ");
		sb.append("and JOB_APPROVAL_FLG = 1 order by FRANCHISE_ID, JOB_TITLE_NM");
		log.debug(sb.toString()+"|"+orgId);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			
			ps.setString(1, orgId);
			
			rs = ps.executeQuery();
			
			while (rs.next()) {
				if (rs.getString("LOCATION_OWNER_EMAIL_TXT") != null)
					careers.add(new GenericVO(rs.getString("LOCATION_OWNER_EMAIL_TXT"), new CareersVO(rs)));
			}
			
		} catch (SQLException e) {
			log.error("Could not get list of careers from the database", e);
		} finally {
			try {
				ps.close();
				rs.close();
			} catch (Exception e) {}
		}
		
		return careers;
	}

	/**
	 * Returns a complete email addressed to the proper recipient
	 * @param rcpt
	 * @param career
	 * @return
	 * @throws InvalidDataException
	 */
	private EmailMessageVO buildEmail(String rcpt, CareersVO career) throws InvalidDataException {
		EmailMessageVO email = new EmailMessageVO();
		
		email.addRecipient(rcpt);
		
		StringBuilder body = new StringBuilder();
		
		body.append("Dear Franchise Partner,<br/>");
		body.append("This email alert is to remind you that there is an open job");
		body.append(" posted on your center website that has been up for six weeks or longer.<br/>");
		body.append("If you want this job posting removed, please email eteam@fastsigns.com<br/>");
		body.append("If you want to keep this job posting, no action needed. When ");
		body.append("you are ready to remove the job posting, please let us know.<br/>");
		body.append("Thank you,<br/>");
		body.append("eteam@fastsigns.com");
		
		email.setHtmlBody(body.toString());
		email.setSubject("Job Posting Reminder");
		email.setFrom("eteam@fastsigns.com");
		
		return email;
	}
}
