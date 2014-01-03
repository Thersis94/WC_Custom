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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SB ANS Medical
import com.ansmed.sb.action.EpiducerMailFormatter;
import com.ansmed.sb.physician.SurgeonVO;
import com.ansmed.sb.sales.SalesRepVO;

// SMT 2.0 Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.SMTMail;

// SBII Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
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
public class EpiducerRepNotifier {
	
	private static Logger log = Logger.getLogger(EpiducerRepNotifier.class);
	private static final String FROM_SENDER = "EpiducerTraining@sjmneuro.com";
	//private static final String FROM_SENDER = "dave@siliconmtn.com";
	
	private Properties props = new Properties();
	private Connection conn = null;
	private String configPath = "scripts/ans_config.properties";
	private String dbSchema = "WebCrescendo_custom.dbo.";
	private Map<String,Object> config = new HashMap<String,Object>();
	private int totalCount = 0;
	
	/**
	 * default constructor
	 */
	public EpiducerRepNotifier() {}
	
	public static void main(String[] args) {
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		
		EpiducerRepNotifier gan = new EpiducerRepNotifier();
		gan.init();
		EpiducerMailFormatter emf = new EpiducerMailFormatter();		
		// retrieve reps/surgeons: operator (= or >) and group number
		// ">", 0 means retrieve all surgeons with group number > 0
		// "=", 3 means retrieve all surgeons whose group number is 3
		/*
		 * 1 = SJM group 1A
		 * 2 = SJM group 1B
		 * 3 = SJM group S
		 * 4 = SJM group 2
		 * 5 = SJM group 3
		 * 6 = SJM group C
		*/
		// Retrieve reps and surgeons for a specific group
		List<GenericVO> repSurgeons = gan.retrieveRepSurgeonData("=", 5);
		log.debug("repSurgeons size: " + repSurgeons.size());
		
		// close the db connection before processing/mailing.
		gan.close();
		
		//for (GenericVO g : repSurgeons) {
			//log.debug("rep: " + g.getRep().getProfileId() + " <---> " + g.getRep().getLastName() + ", " + g.getRep().getFirstName());
		//}
		
		// 1: Group notification (1 email each rep)
		//gan.sendRepGroupNotification(emf, repSurgeons);
		
		// 2: First Rep notification (1 email to rep for each physician)
		//gan.sendRepFirstNotification(emf, repSurgeons);
		
		// 3: Reminder notification (1 email each rep)
		//gan.sendRepSecondNotification(emf, repSurgeons);
		
		// 4: Physician Invites (1 email to rep for each physician)
		gan.sendPhysicianInvitations(emf, repSurgeons);
		
		log.debug("total email count: " + gan.totalCount);
	}
	
	/**
	 * Retrieves list of surgeons 
	 */
	public List<GenericVO> retrieveRepSurgeonData(String operator, int intVal) {
		List<String> repProfileIds = new ArrayList<String>();
		List<GenericVO> repSurgeons = new ArrayList<GenericVO>();
		StringBuffer sql = new StringBuffer();
		sql.append("select a.sales_rep_id, a.profile_id as 'rep_profile_id', a.first_nm as 'rep_first_nm', a.last_nm as 'rep_last_nm', b.* from ");
		sql.append(dbSchema).append("ans_sales_rep a ");
		sql.append("inner join ");
		sql.append(dbSchema).append("ans_surgeon b ");
		sql.append("on a.sales_rep_id = b.sales_rep_id ");
		sql.append("where b.prod_group_no ");
		sql.append(operator).append(" ").append(intVal).append(" ");
		sql.append("and b.prod_approval_flg = 1 ");
		sql.append("order by b.sales_rep_id, b.last_nm, b.first_nm, b.prod_group_no");
		
		log.debug("retrieveSurgeon SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			
			String currRep = "";
			String prevRep = "";
			GenericVO gvo = null;
			
			while(rs.next()) {
				currRep = rs.getString("sales_rep_id");
				if (currRep.equalsIgnoreCase(prevRep)) {
					// add the surgeon to the rep's list
					gvo.addSurgeon(new SurgeonVO(rs));
				} else {
					// capture the rep's profileId
					if (gvo == null) {
						// instantiate new VO
						gvo = new GenericVO();
					} else {
						// add gvo to list
						repSurgeons.add(gvo);
						gvo = new GenericVO();
					}
					repProfileIds.add(rs.getString("rep_profile_id"));
					gvo.getRep().setProfileId(rs.getString("rep_profile_id"));
					gvo.getRep().setFirstName(rs.getString("rep_first_nm"));
					gvo.getRep().setLastName(rs.getString("rep_last_nm"));
					gvo.addSurgeon(new SurgeonVO(rs));
				}
				prevRep = currRep;	
			}
			
			// add the last record
			repSurgeons.add(gvo);
			
		} catch (SQLException sqle) {
			log.error("Error retrieving surgeon records, ", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		
		log.debug("repProfileIds size: " + repProfileIds.size());
		log.debug("repSurgeons size: " + repSurgeons.size());
		
		// retrieve rep profiles
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		Map<String,UserDataVO> ids = new HashMap<String,UserDataVO>();
		try {
			ids = pm.searchProfileMap(conn, repProfileIds);
		} catch (DatabaseException de) {
			log.error("Error retrieving rep profiles, ", de);
		}
		
		// merge rep profiles with rep's surgeon list
		for (GenericVO g : repSurgeons) {
			String id = g.getRep().getProfileId();
			//log.debug("before first/lastname: " + g.getRep().getFirstName() + " " + g.getRep().getLastName());
			SalesRepVO rep = new SalesRepVO();
			rep.setProfileId(ids.get(id).getProfileId());
			rep.setFirstName(ids.get(id).getFirstName());
			rep.setLastName(ids.get(id).getLastName());
			rep.setEmailAddress(ids.get(id).getEmailAddress());
			g.setRep(rep);
			//log.debug("  after first/lastname: " + g.getRep().getFirstName() + " " + g.getRep().getLastName());
		}
		return repSurgeons;
	}
	
	/**
	 * Sends physician/group notification to each rep.  total of 1 email to each rep who has
	 * physicians with a prod_approval_flg value of 1
	 * @param emf
	 * @param gvo
	 */
	protected void sendRepGroupNotification(EpiducerMailFormatter emf, List<GenericVO> gvo) {
		String subject = "Epiducer System Training | Attendee Confirmation";
		String from = FROM_SENDER;
		String mailTo = null;
		for (GenericVO g : gvo) {
			emf.setRep(g.getRep().getUserData());
			emf.setSurgeons(g.getSurgeons());
			// mailTo = g.getRep().getEmailAddress();
			mailTo = FROM_SENDER;
			this.sendEmail(emf.formatRepGroupNotification(), subject, from, mailTo);
		}
	}
	
	/**
	 * Sends 'n' emails to each rep; one for each individual physician that has been selected to attend training. 
	 * @param emf
	 * @param gvo
	 */
	protected void sendRepFirstNotification(EpiducerMailFormatter emf, List<GenericVO> gvo) {
		String subject = "Epiducer System Training | Attendee Confirmation";
		String from = FROM_SENDER;
		String mailTo = null;
		int count = 0;
		int totalMail = 0;
		for (GenericVO g : gvo) {
			count = 0;
			//if (count > 0) break;
			mailTo = g.getRep().getEmailAddress();
			//mailTo = "dbargerhuff@gmail.com";
			emf.setRep(g.getRep().getUserData());
			for (SurgeonVO s : g.getSurgeons()) {
				//if (count > 0) continue;
				emf.setSurgeon(s);
				sendEmail(emf.formatRepFirstNotification(), subject, from, mailTo);
				count++;
				totalMail++;
			}
			log.debug("sent " + count + " email(s) to rep: " + g.getRep().getProfileId() + " <---> " + g.getRep().getLastName() + ", " + g.getRep().getFirstName() + "(" + g.getRep().getEmailAddress() + ")");
		}
		log.debug("sent total of " + totalMail + " emails to reps");
	}
	
	/**
	 * Sends 1 reminder email to each rep
	 * @param emf
	 * @param gvo
	 */
	protected void sendRepSecondNotification(EpiducerMailFormatter emf, List<GenericVO> gvo) {
		String subject = "Reminder | Epiducer System Training | Attendee Confirmation";
		String from = FROM_SENDER;
		String mailTo = null;
		for (GenericVO g : gvo) {
			emf.setRep(g.getRep().getUserData());
			mailTo = g.getRep().getEmailAddress();
			//mailTo = FROM_SENDER;
			sendEmail(emf.formatRepSecondNotification(), subject, from, mailTo);
			totalCount++;
			log.debug("sent 1 email to rep: " + g.getRep().getProfileId() + " <---> " + g.getRep().getLastName() + ", " + g.getRep().getFirstName() + "(" + g.getRep().getEmailAddress() + ")");
		}
		log.debug("sent email to " + totalCount + " reps");
	}
	
	/**
	 * Sends one physician invitation to each rep for each physician selected to attend training.  Invitiation
	 * is personalized for each physician.
	 * @param emf
	 * @param gvo
	 */
	private void sendPhysicianInvitations(EpiducerMailFormatter emf, List<GenericVO> gvo) {
		StringBuffer subject = null;
		String mailTo = null;
		String from = FROM_SENDER;
		int count = 0;
		int repCount = 0;
		for (GenericVO g : gvo) {
			repCount++;
			//if (repCount > 1) break;
			mailTo = g.getRep().getEmailAddress();
			//mailTo = FROM_SENDER;
			emf.setRep(g.getRep().getUserData());
			for (SurgeonVO s : g.getSurgeons()) {
				count++;
				//if (count > 1) continue;
				emf.setSurgeon(s);
				subject = new StringBuffer("Epiducer System Training Invitation | ");
				subject.append(s.getLastName()).append(" | St. Jude Medical Neuromodulation");
				sendEmail(emf.getPhysicianInvitation(s.getProductGroupNumber()), subject.toString(), from, mailTo);
				//log.debug("body for group " + s.getProductGroupNumber() + "(db #): " + emf.getPhysicianInvitation(s.getProductGroupNumber()));
			}
			log.debug("sent " + count + " email(s) to rep: " + g.getRep().getProfileId() + " <---> " + g.getRep().getLastName() + ", " + g.getRep().getFirstName() + "(" + mailTo + ")");
			totalCount += count;
			count = 0;
		}
		log.debug("sent mail to " + repCount + " reps");
		log.debug("total invitations sent to reps: " + totalCount);
	}
	
	/**
	 * Emails reps with an email per surgeon, each with a PDF attachment
	 * @param body
	 * @param subject
	 * @param from
	 * @param mailTo
	 */
	public void sendEmail(StringBuffer body, String subject, String from, String mailTo) {
		if (body == null || body.length() == 0) {
			log.debug("email body is empty....skipping this send");
			return;
		}
		
		// mail it.
		SMTMail mail = new SMTMail(props.getProperty(Constants.CFG_SMTP_SERVER));
		mail.setUser(props.getProperty(Constants.CFG_SMTP_USER));
		mail.setPassword(props.getProperty(Constants.CFG_SMTP_PASSWORD));
		mail.setFrom(from);
		mail.setRecpt(new String[] {mailTo});
		// CC the sender
		mail.setCC(new String[] {from});
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
