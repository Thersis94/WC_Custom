package com.ansmed.sb.util;

// JDK 1.6
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SB ANS Medical
import com.ansmed.sb.physician.SurgeonVO;
import com.ansmed.sb.sales.SalesRepVO;

// SMT 2.0 Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;

// SBII Libs
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: GroupAssignmentNotification.java<p/>
 * <b>Description: </b> Sends physician list to each SJM sales rep who has physicians that have been
 * approved for group training. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 10, 2011
 ****************************************************************************/
public class EpiducerNewsletterManager {
	
	private static Logger log = Logger.getLogger(EpiducerNewsletterManager.class);
	private static final String FROM_SENDER = "EpiducerTraining@sjmneuro.com";
	private static final String BCC_TO = "john.astorga@sjmneuro.com";
	//private static final String FROM_SENDER = "dave@siliconmtn.com";
	//private static final String BCC_TO = "dbargerhuff@gmail.com";
	
	private Properties props = new Properties();
	private Connection conn = null;
	private String configPath = "scripts/ans_config.properties";
	//private String dbSchema = "WebCrescendo_custom.dbo.";
	private Map<String,Object> config = new HashMap<String,Object>();
	private Map<String, Map<String, String>> walkIns = new HashMap<String, Map<String, String>>();
	//private int count = 0;
	
	/**
	 * default constructor
	 */
	public EpiducerNewsletterManager() { 
		loadWalkIns();
	}
	
	public static void main(String[] args) {
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		EpiducerNewsletterManager gan = new EpiducerNewsletterManager();
		// initialize connection
		gan.init();
		
		// 1. retrieve today's date
		Calendar cal = GregorianCalendar.getInstance();
		String today = Convert.formatDate(cal.getTime(),Convert.DATE_SLASH_PATTERN);
		log.debug("today as String is: " + today);
				
		// 2. obtain schedule of today's sends
		EpiducerNewsletterScheduler ens = new EpiducerNewsletterScheduler();
		Map<String, Integer> schedule = ens.retrieveTodaysSchedule(today);
		log.debug("Schedule (size) is: " + (schedule != null ? schedule.size() : "null"));
		if (schedule != null && ! schedule.isEmpty()) {
			Iterator<String> iter = schedule.keySet().iterator();
			while (iter.hasNext()) {
				String s = iter.next();
				log.debug("key ---> val: " + s + " ---> " + schedule.get(s));
			}
		}
		
		// process today's sends
		gan.processSends(schedule);

		// 4. send email status to admin
		// 5. clean up db connections
		gan.close();
		
	}
	
	/**
	 * 
	 * @param sendMap
	 */
	private void processSends(Map<String, Integer> sendMap) {
		if (sendMap != null && sendMap.size() > 0) {
			EpiducerNewsletterBuilder letter = new EpiducerNewsletterBuilder();
			//log.debug("letter: " + letter.getNewsLetter(1,"optOutemail"));
			for (String courseName : sendMap.keySet()) {
				log.debug("course/newsletter #: " + courseName + " / " + sendMap.get(courseName));
				List<GenericVO> data = retrieveAttendeeData(courseName);
				log.debug("send size for this course: " + (data != null ? data.size() : 0));
				int mailCount = 0;
				for (GenericVO g : data) {
					//loop the VOs, email the letter
					sendEmail(letter.getNewsLetter(sendMap.get(courseName), g.getSurgeons().get(0).getEmailAddress()), letter.getNewsLetterSubject(sendMap.get(courseName)), g);
					mailCount++;
					log.debug("sent newsletter #: " + sendMap.get(courseName) + " to: " + g.getSurgeons().get(0).getEmailAddress() + ", CC to: " + g.getRep().getEmailAddress());
				}
				log.debug("total mail sent for this course date: " + mailCount);
			}
		}
	}
	
	/**
	 * Retrieves contact rep, surgeon, and submission data for the attendees of the given course
	 * @param courseName
	 * @return
	 */
	public List<GenericVO> retrieveAttendeeData(String courseName) {
		List<GenericVO> repSurgeons = new ArrayList<GenericVO>();
		StringBuffer sql = new StringBuffer();
		/*
		sql.append("select c.email_address_txt as 'PHYS_EMAIL', a.EMAIL_ADDRESS_TXT as 'REP_EMAIL' ");
		sql.append("from WebCrescendo_custom.dbo.ans_sales_rep a ");
		sql.append("inner join WebCrescendo_custom.dbo.ans_surgeon b on a.sales_rep_id = b.sales_rep_id ");
		sql.append("inner join CONTACT_SUBMITTAL c on b.profile_id = c.PROFILE_ID ");
		sql.append("inner join CONTACT_DATA d on c.CONTACT_SUBMITTAL_ID = d.CONTACT_SUBMITTAL_ID ");
		sql.append("where 1 = 1 ");
		sql.append("and b.prod_approval_flg > 0 ");
		sql.append("and c.ACTION_ID = 'c0a802412d2d281ea375003c7a8bb443' ");
		sql.append("and c.email_address_txt is not null ");
		sql.append("and d.CONTACT_FIELD_ID = 'c0a8023751111e8c8200f52c7920a8ab' ");
		sql.append("and CAST(d.value_txt as nvarchar(max)) = ? ");
		*/
		// queries against email_permission 
		sql.append("select d.email_address_txt as 'PHYS_EMAIL', a.EMAIL_ADDRESS_TXT as 'REP_EMAIL' "); 
		sql.append("from WebCrescendo_custom.dbo.ans_sales_rep a  ");
		sql.append("inner join WebCrescendo_custom.dbo.ans_surgeon b on a.sales_rep_id = b.sales_rep_id "); 
		sql.append("inner join EMAIL_PERMISSION c on b.PROFILE_ID=c.PROFILE_ID and c.allow_comm_flg = 1 ");
		sql.append("inner join CONTACT_SUBMITTAL d on c.PROFILE_ID=d.PROFILE_ID  ");
		sql.append("inner join CONTACT_DATA e on d.CONTACT_SUBMITTAL_ID = e.CONTACT_SUBMITTAL_ID ");
		sql.append("where 1 = 1  ");
		sql.append("and b.prod_approval_flg > 0 "); 
		sql.append("and c.email_campaign_id = 'c0a8023771850933dce429202c15b30f' ");
		sql.append("and c.allow_comm_flg = 1 ");
		sql.append("and d.ACTION_ID = 'c0a802412d2d281ea375003c7a8bb443'  ");
		sql.append("and (d.email_address_txt is not null and d.email_address_txt != '') ");
		sql.append("and e.CONTACT_FIELD_ID = 'c0a8023751111e8c8200f52c7920a8ab' "); 
		sql.append("and CAST(e.value_txt as nvarchar(max)) = ?  ");
		log.debug("retrieve registrants emails SQL: " + sql.toString());
		
		/*
		int x = 1;
		if (x == 1) return new ArrayList<GenericVO>();
		*/
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, courseName);
			ResultSet rs = ps.executeQuery();
			GenericVO gvo = null;
			
			while(rs.next()) {
				gvo = new GenericVO();
				SurgeonVO surgeon = new SurgeonVO();
				surgeon.setEmailAddress(rs.getString("phys_email"));
				gvo.addSurgeon(surgeon);
				SalesRepVO rep = new SalesRepVO();
				rep.setEmailAddress(rs.getString("rep_email"));
				gvo.setRep(rep);
				repSurgeons.add(gvo);
			}
			
		} catch (SQLException sqle) {
			log.error("Error retrieving course registrants for course " + courseName + ", ", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		
		log.debug("repSurgeons size before walk-ins: " + repSurgeons.size());
		
		// add the 'walk-in' docs who have no submittal
		this.retrieveWalkIns(courseName, repSurgeons);
		
		log.debug("repSurgeons size after adding walk-ins: " + repSurgeons.size());
		
		return repSurgeons;
	}
	
	/**
	 * Look up the walk-ins
	 * @param repSurgeons
	 */
	private void retrieveWalkIns(String courseName, List<GenericVO> repSurgeons) {
		// if there are no walk-ins for this course return
		if (walkIns.get(courseName) == null) return;
		log.debug("adding walk-ins...");
		StringBuffer sql = new StringBuffer();
		sql.append("select a.email_address_txt,b.profile_id ");
		sql.append("from WebCrescendo_custom.dbo.ans_sales_rep a ");
		sql.append("inner join WebCrescendo_custom.dbo.ans_surgeon b on a.sales_rep_id = b.sales_rep_id ");
		sql.append("where b.profile_id in (");
		
		for (String profileId : walkIns.get(courseName).keySet()) {
			sql.append("'").append(profileId).append("',");
		}
		int last = sql.lastIndexOf(",");
		if (last > 0) { 
			sql.replace(last, last + 1, "");
		}
		sql.append(")");
		log.debug("walk in SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				GenericVO g = new GenericVO();
				SurgeonVO surgeon = new SurgeonVO();
				surgeon.setEmailAddress(walkIns.get(courseName).get(rs.getString("profile_id")));
				log.debug("found walk-In: " + walkIns.get(courseName).get(rs.getString("profile_id")));
				g.addSurgeon(surgeon);
				SalesRepVO rep = new SalesRepVO();
				rep.setEmailAddress(rs.getString("email_address_txt"));
				g.setRep(rep);
				repSurgeons.add(g);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving walk-in data, ", sqle);
		}
	}
	
	/**
	 * Emails reps with an email per surgeon, each with a PDF attachment
	 * @param body
	 * @param subject
	 * @param from
	 * @param mailTo
	 */
	public void sendEmail(StringBuffer body, String subject, GenericVO repSurgeon) {
		if (body == null || body.length() == 0) {
			log.debug("email body is empty....skipping this send");
			return;
		}
		
		String[] mailTo = { repSurgeon.getSurgeons().get(0).getEmailAddress() };
		//String[] mailTo = { FROM_SENDER };
		
		String[] ccTo = new String[2];
		ccTo[0] = repSurgeon.getRep().getEmailAddress();
		ccTo[1] = FROM_SENDER;
		
		String[] bccTo = { BCC_TO };
		
		// mail it.
		SMTMail mail = new SMTMail(props.getProperty(Constants.CFG_SMTP_SERVER));
		mail.setUser(props.getProperty(Constants.CFG_SMTP_USER));
		mail.setPassword(props.getProperty(Constants.CFG_SMTP_PASSWORD));
		mail.setFrom(FROM_SENDER);
		mail.setRecpt(mailTo);
		// CC the sender
		mail.setCC(ccTo);
		mail.setBCC(bccTo);
		mail.setSubject(subject);
		mail.setHtmlBody(body.toString());
		log.debug("Mail Info: " + mail.toString());
		try {
			mail.postMail();
		} catch (MailException me) {
			log.error("Error sending email, ", me);
		}
	}
	
	
	/**
	 * Initializes object; loads properties and obtains db connection
	 */
	public void init() {
		// Load the config file
		FileInputStream inStream = null;
		try {
			inStream = new FileInputStream(configPath);
			props.load(inStream);
			config.put(Constants.ENCRYPT_KEY, props.getProperty("encryptKey"));
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
				}
			}
		}
		try {
			getDbConnection(props);
		} catch (Exception e) {
			log.error("Could not obtain database connection, ", e);
			System.exit(-1);
		}
	}
	
	/**
	 * Establishes db connection
	 * @param props
	 * @throws Exception
	 */
	public void getDbConnection(Properties props) throws Exception {
		//obtain db connection or exit
		DatabaseConnection dbc = new DatabaseConnection(props.getProperty("dbDriver"),
				props.getProperty("dbUrl"),
				props.getProperty("dbUser"),
				props.getProperty("dbPassword"));
		try {
			conn = dbc.getConnection();
			log.debug("Got a database connection.");
		} catch (Exception de) {
			throw new Exception(de.getMessage());
		}
	}
	
	/**
	 * Closes db connection.
	 */
	public void close() {
		if (conn != null) {
			try {
				conn.close();
				log.debug("Closed db connection.");
			} catch (Exception e) {
				log.error("Error closing db connection, ", e);
			}
		}
	}
	
	/**
	 * Loads map of physicians who did not register for a course but who were
	 * allowed to attend as 'walk-ins'
	 * @return
	 */
	private void loadWalkIns() {
		walkIns = new HashMap<String, Map<String, String>>();
		Map<String, String> phys = new HashMap<String, String>();
		// course: TIME - Plano, TX, 09/11/11
		phys.put("0a0015a4cf76f717dd9eee88b562615d", "jerrywayne7@gmail.com"); // Dr. Jerry Lewis
		walkIns.put("TIME - Plano, TX, 09/11/11", phys);
	}
	
	/**
	 * Generic value object to contain sales rep VO and list of surgeon VOs
	 * @author David Bargerhuff 05-12-2011
	 *
	 */
	class GenericVO {
		private SalesRepVO rep = new SalesRepVO();
		private List<SurgeonVO> surgeons = new ArrayList<SurgeonVO>();
		public SalesRepVO getRep() {
			return rep;
		}
		public void setRep(SalesRepVO rep) {
			this.rep = rep;
		}
		public List<SurgeonVO> getSurgeons() {
			return surgeons;
		}
		public void setSurgeons(List<SurgeonVO> surgeons) {
			this.surgeons = surgeons;
		}
		public void addSurgeon(SurgeonVO surgeon) {
			surgeons.add(surgeon);
		}
	}
	
	
}
