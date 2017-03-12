package com.biomed.smarttrak.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.AccountVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.support.SupportTicketAction.ChangeType;
import com.smt.sitebuilder.action.support.TicketActivityVO;
import com.smt.sitebuilder.action.support.TicketVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: BiomedSupportEmailUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utility class for building Support Emails.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 10, 2017
 ****************************************************************************/
public class BiomedSupportEmailUtil {

	public static final String TICKET_ID = "ticketId";
	public static final String TICKET_MSG = "ticketMsg";
	public static final String EMAIL_TYPE = "emailType";
	public static final String HELP_URL = "helpUrl";
	public static final String SUPPORT_EMAIL = "supportEmail";

	private Logger log;

	private SMTDBConnection dbConn = null;
	private Map<String, Object> attributes = null;
	private String helpUrl;
	private String supportEmail;

	/**
	 * Constructor takes Connection and attributes Map for building emails.
	 * @param mod
	 * @param conn
	 * @param customSchema
	 * @throws Exception
	 */
	public BiomedSupportEmailUtil(SMTDBConnection conn, Map<String, Object> attributes) {
		this.log = Logger.getLogger(this.getClass());
		this.dbConn = conn;
		this.attributes = attributes;
	}


	/**
	 * Single point entry for sending emails.
	 * @param ticketId
	 * @param ticketMsg
	 * @param type
	 * @throws Exception
	 */
	public void sendEmail(String ticketId, ChangeType type) throws Exception {

		TicketVO t = loadTicket(ticketId);

		buildEmail(t, type);
	}

	/**
	 * Helper method that builds the Ticket Retrieval Sql complete with creator
	 * info and optional assigned info if available.
	 * @return
	 */
	protected String getTicketSql() {
		StringBuilder sql = new StringBuilder(500);
		sql.append("select a.*, b.first_nm as reporter_first_nm, ");
		sql.append("b.last_nm as reporter_last_nm ");
		sql.append("b.email_address_txt as reporter_email ");
		sql.append("c.first_nm as assigned_first_nm ");
		sql.append("c.last_nm as assigned_last_nm ");
		sql.append("c.email_address_txt as assigned_email ");
		sql.append("from support_ticket a ");
		sql.append("inner join profile b on a.reporter_id = b.profile_id ");
		sql.append("left outer join profile c on a.assigned_id = c.profile_id ");
		sql.append("where a.ticket_id = ?");

		return sql.toString();
	}


	/**
	 * Helper method that decrypts Profile Data on the TicketVO.
	 * @param t
	 */
	protected void decryptTicket(TicketVO t) {
		try {
			StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			t.setAssignedEmail(se.decrypt(t.getAssignedEmail()));
			t.setAssignedFirstNm(se.decrypt(t.getAssignedFirstNm()));
			t.setAssignedLastNm(se.decrypt(t.getAssignedLastNm()));
			t.setReporterEmail(se.decrypt(t.getReporterEmail()));
			t.setFirstName(se.decrypt(t.getFirstName()));
			t.setLastName(se.decrypt(t.getLastName()));
		} catch (EncryptionException e) {
			log.error(e);
		}
	}

	/**
	 * Helper method that converts EmailType String to EmailType enum.  Throws
	 * Exception if emailType fails to Convert.
	 * @return
	 * @throws Exception - invalid EmailType passed.
	 */
	protected ChangeType getEmailType(String emailType) throws Exception {
		try {
			return ChangeType.valueOf(emailType);
		} catch(Exception e) {
			throw new Exception("Invalid Email Type Passed.", e);
		}
	}

	/**
	 * Helper method that builds the Proper Email for a given EmailType and
	 * Sends it.
	 * @param t
	 * @param type
	 * @param ticketMsg
	 * @throws Exception 
	 */
	protected void buildEmail(TicketVO t, ChangeType type) throws Exception {
		EmailMessageVO email = null;
		switch(type) {
			case ACTIVITY:
				email = buildActivityEmail(t);
				break;
			case ASSIGNMENT:
				email = buildAssignedEmail(t);
				break;
			case STATUS:
				email = buildStatusEmail(t);
				break;
			case TICKET:
				email = buildNewRequestEmail(t);
				break;
			case ATTACHMENT:
			default:
				break;
		}

		//If an email was built, send it.
		if(email != null) {
			sendEmail(email);
		}
	}

	/**
	 * Helper method that sends an Email containing the Order that was just
	 * processed.
	 * @param order
	 * @throws Exception
	 */
	protected void sendEmail(EmailMessageVO email) throws Exception {
		MessageSender ms = new MessageSender(attributes, dbConn);
		ms.sendMessage(email);
		log.debug("Created Message: " + email);
	}

	/**
	 * Helper method that builds Default EmailMessageVO.
	 * @param t
	 * @return
	 * @throws InvalidDataException 
	 */
	protected EmailMessageVO buildDefaultEmail(TicketVO t) throws InvalidDataException {
		EmailMessageVO msg = new EmailMessageVO();
		msg.setFrom(supportEmail);
		msg.addRecipient(t.getReporterEmail());

		if(!StringUtil.isEmpty(t.getAssignedEmail())) {
			msg.addRecipient(t.getAssignedEmail());
		}

		return msg;
	}

	/**
	 * Helper method builds the Help Url for a given Ticket.
	 * @param t
	 * @return
	 */
	protected String buildTicketUrl(TicketVO t) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(helpUrl);
		sql.append(attributes.get(Constants.QS_PATH));
		sql.append(t.getTicketId());
		return sql.toString();
	}

	/**
	 * Helper method attempts to retrieve all the Account Managers.
	 * @return
	 * @throws Exception 
	 */
	protected String [] getAdminEmails() throws Exception {
		List<String> emails = new ArrayList<>();

		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.email_address_txt as owner_email_addr, a.first_nm, a.last_nm from profile a ");
		sql.append("inner join profile_role b on a.profile_id=b.profile_id and b.status_id=?");
		sql.append("and b.site_id=? and b.role_id=?");
		log.debug(sql);

		List<Object> params = new ArrayList<>();
		params.add(SecurityController.STATUS_ACTIVE);
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(AdminControllerAction.STAFF_ROLE_ID);

		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		List<Object>  accounts = db.executeSelect(sql.toString(), params, new AccountVO());
		log.debug("loaded " + accounts.size() + " managers");

		Collections.sort(accounts, new NameComparator());

		StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));

		//Parse Managers List and get emails.
		for(Object o : accounts) {
			AccountVO a = (AccountVO)o;
			a.setOwnerEmailAddr(se.decrypt(a.getOwnerEmailAddr()));
			a.setFirstName(se.decrypt(a.getFirstName()));
			a.setLastName(se.decrypt(a.getLastName()));
		}

		return emails.toArray(new String [emails.size()]);
	}

	/**
	 * Helper method builds an email sent to Requester and all Admins that a new
	 * ticket has been created.
	 * @param t
	 * @param ticketMsg 
	 * @return
	 * @throws Exception 
	 */
	protected EmailMessageVO buildNewRequestEmail(TicketVO t) throws Exception {
		EmailMessageVO msg = buildDefaultEmail(t);

		//New Tickets get sent to All Admins.
		msg.addRecipients(getAdminEmails());

		//Add New Email Html.
		msg.setHtmlBody(buildNewEmailBody(t));

		return msg;

	}


	/**
	 * Helper method that builds a New Email Body Message.
	 * @param t
	 * @param ticketMsg
	 * @return
	 */
	protected String buildNewEmailBody(TicketVO t) {
		StringBuilder text = new StringBuilder(1000);
		text.append("<p>Thank you for creating a Direct Access request, we make ");
		text.append("every effort to reply to Direct Access requests within 1 ");
		text.append("business day.To amend your request or add a screenshot or attachment ");
		text.append("<a href='");
		text.append(buildTicketUrl(t));
		text.append("'>view the request here</a>.</p><p><b>Original Request</b>:</p><hr>");
		text.append(t.getDescText()).append("<br><hr>");

		return text.toString();
	}


	/**
	 * Helper method builds an email sent to Requester and Assigned Admin that
	 * assignment has changed.
	 * @param t
	 * @param ticketMsg 
	 * @return
	 * @throws Exception 
	 */
	protected EmailMessageVO buildAssignedEmail(TicketVO t) throws Exception {
		EmailMessageVO msg = buildDefaultEmail(t);
		msg.setHtmlBody(buildAssignedEmailBody(t));
		return msg;
	}

	/**
	 * Helper method that builds the Assigned Email Body.
	 * @param t
	 * @param ticketMsg
	 * @return
	 * @throws Exception 
	 */
	protected String buildAssignedEmailBody(TicketVO t) throws Exception {
		UserDataVO user = ProfileManagerFactory.getInstance(attributes).getProfile(t.getReporterId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, AdminControllerAction.BIOMED_ORG_ID);
		
		//TODO - Need to get CompanyId for a User.  Probably need to Load similar to AccountUserAction.
		String companyId = (String)user.getAttributes().get("companyId");
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		pnf.setCountryCode(user.getCountryCode());
		pnf.setPhoneNumber(user.getMainPhone());

		StringBuilder text = new StringBuilder(1000);
		text.append("<p>You've been assigned a Direct Access request (#");
		text.append(t.getTicketId()).append(") from <b>");
		text.append(t.getFirstName()).append(" ").append(t.getLastName());
		text.append(" at ");		
		text.append(companyId);
		text.append("</b>.<a href='");
		text.append(buildTicketUrl(t));
		text.append("'>Click here to review or respond to the request</a>.</p><p>");
		text.append("Creator: ");
		text.append(t.getFirstName()).append(" ").append(t.getLastName());
		text.append("<br>Account: ");
		text.append(companyId);
		text.append("<br><a href='");
		text.append(buildTicketUrl(t));
		text.append("'>Link to Ticket</a><br>E-Mail: ");
		text.append(t.getReporterEmail());
		text.append("<br>Phone: ");
		text.append(pnf.getFormattedNumber());
		text.append("<br>Creation Time: ");
		text.append(Convert.formatDate(t.getCreateDt(), Convert.DATE_TIME_DASH_PATTERN_12HR));
		text.append("</p><p><b>Original Request</b>:</p><hr>");
		text.append(t.getDescText());
		text.append("<br><hr>");
		return text.toString();
	}


	/**
	 * Helper method builds an email sent to Requester and Assigned admin that
	 * Ticket status has changed.
	 * @param t
	 * @param ticketMsg 
	 * @return
	 * @throws InvalidDataException 
	 */
	protected EmailMessageVO buildStatusEmail(TicketVO t) throws InvalidDataException {
		EmailMessageVO msg = buildDefaultEmail(t);
		msg.setHtmlBody(buildStatusEmailBody(t));
		return msg;

	}

	/**
	 * Helper method that builds the StatusEmailBody.
	 * @param t
	 * @param ticketMsg
	 * @return
	 */
	protected String buildStatusEmailBody(TicketVO t) {
		StringBuilder text = new StringBuilder(1000);
		text.append("<p><a href='");
		//Replace Url
		text.append(buildTicketUrl(t));
		text.append("'>Direct Access request (#");
		//Replace Ticket Number
		text.append(t.getTicketId());
		text.append(")</a> was moved to ");
		text.append(t.getStatusNm());
		text.append(" status by ");

		//Replace Assigned Name
		text.append(t.getAssignedFirstNm()).append(" ").append(t.getAssignedLastNm());
		text.append(".</p><p><b>Original Request</b>:</p><hr>");

		//Replace ticketMsg
		text.append(t.getDescText());
		text.append("<br><hr>");

		return text.toString();
	}


	/**
	 * Helper method builds an email to the Requester containing message from
	 * Assigned Admin.
	 * @param t
	 * @param ticketMsg 
	 * @return
	 * @throws InvalidDataException 
	 */
	protected EmailMessageVO buildActivityEmail(TicketVO t) throws InvalidDataException {
		return buildDefaultEmail(t);	
	}

	/**
	 * Load Ticket Info.  This has email Address information in it that we need
	 * to Send the Emails.
	 * @param ticketId
	 * @return
	 */
	protected TicketVO loadTicket(String ticketId) {
		TicketVO t = null;
		String sql = getTicketSql();
		List<Object> params = new ArrayList<>(1);
		params.add(ticketId);

		List<Object> ticket = new DBProcessor(dbConn).executeSelect(sql, params, new TicketVO());

		if(!ticket.isEmpty()) {
			t = (TicketVO)ticket.get(0);

			//Decrypt ProfileData on Ticket.
			decryptTicket(t);
		}	
		return t;
	}


	/**
	 * Build the TicketActivity Email Notification. 
	 * @param act
	 */
	public void sendEmail(TicketActivityVO act) {
		//Build Email

		//Send Email
	}
}