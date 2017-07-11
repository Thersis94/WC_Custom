package com.biomed.smarttrak.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.TicketEmailVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.support.SupportTicketAction.ChangeType;
import com.smt.sitebuilder.action.support.TicketActivityVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

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
	public static final String NEW_TICKET_CAMP_INST_ID = "newTicketCampInstId";
	public static final String ASSN_TICKET_CAMP_INST_ID = "assnTicketCampInstId";
	public static final String STAT_TICKET_CAMP_INST_ID = "statTicketCampInstId";
	public static final String ACT_TICKET_CAMP_INST_ID = "actTicketCampInstId";
	private Logger log;

	private EmailCampaignBuilderUtil ecbu;
	private SMTDBConnection dbConn = null;
	private Map<String, Object> attributes = null;

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
		this.ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
	}


	/**
	 * Single point entry for sending emails.
	 * @param ticketId
	 * @param ticketMsg
	 * @param type
	 * @throws Exception
	 */
	public void sendEmail(String ticketId, ChangeType type) throws ActionException {

		TicketEmailVO ticket = loadTicket(ticketId, null);
		try {
			sendEmails(ticket, type);
		} catch (InvalidDataException | EncryptionException e) {
			throw new ActionException("There was a problem building Biomed support email.", e);
		}
	}

	/**
	 * Helper method that builds the Ticket Retrieval Sql complete with creator
	 * info and optional assigned info if available.
	 * @return
	 */
	protected String getTicketSql(boolean getActivity) {
		StringBuilder sql = new StringBuilder(600);
		sql.append("select a.*, b.first_nm as reporter_first_nm, ");
		sql.append("b.last_nm as reporter_last_nm, ");
		sql.append("b.email_address_txt as reporter_email, ");
		sql.append("c.first_nm as assigned_first_nm, ");
		sql.append("c.last_nm as assigned_last_nm, ");
		sql.append("c.email_address_txt as assigned_email ");
		sql.append("from support_ticket a ");
		sql.append("inner join profile b on a.reporter_id = b.profile_id ");
		sql.append("left outer join profile c on a.assigned_id = c.profile_id ");
		if(getActivity) {
			sql.append("left outer join support_activity sa on a.ticket_id = sa.ticket_id ");
		}
		sql.append("where a.ticket_id = ? ");

		if(getActivity) {
			sql.append("and sa.activity_id = ?");
		}

		return sql.toString();
	}

	/**
	 * Load Ticket Info.  This has email Address information in it that we need
	 * to Send the Emails.
	 * @param ticketId
	 * @return
	 */
	public TicketEmailVO loadTicket(String ticketId, String activityId) {
		TicketEmailVO t = null;
		boolean getActivity = !StringUtil.isEmpty(activityId);
		String sql = getTicketSql(getActivity);
		List<Object> params = new ArrayList<>();
		params.add(ticketId);
		if(getActivity) {
			params.add(activityId);
		}

		List<Object> ticket = new DBProcessor(dbConn).executeSelect(sql, params, new TicketEmailVO());

		if(!ticket.isEmpty()) {
			t = (TicketEmailVO)ticket.get(0);

			t.setTicketLink(buildTicketLink(t));
			//Decrypt ProfileData on Ticket.
			decryptTicket(t);
		}

		return t;
	}

	/**
	 * Helper method that builds the Ticket Link for the email.
	 * @param t
	 * @return
	 */
	protected String buildTicketLink(TicketEmailVO t) {
		//Build Url
		StringBuilder url = new StringBuilder(150);
		url.append((String)attributes.get("smarttrakSupportUrl"));
		url.append(attributes.get(Constants.QS_PATH));
		url.append(t.getTicketId());

		return url.toString();
	}


	/**
	 * Helper method that decrypts Profile Data on the TicketEmailVO.
	 * @param t
	 */
	protected void decryptTicket(TicketEmailVO t) {
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
	 * Build EmailCampaign Message Objects to be Enqueued.
	 * @param t
	 * @param type
	 * @param ticketMsg
	 * @throws InvalidDataException 
	 * @throws ActionException 
	 * @throws EncryptionException 
	 * @throws Exception 
	 */
	protected void sendEmails(TicketEmailVO ticket, ChangeType type) throws InvalidDataException, ActionException, EncryptionException {
		switch(type) {
			case ACTIVITY:
				sendActivityEmails(ticket);
				break;
			case ASSIGNMENT:
				sendAssignedEmails(ticket);
				break;
			case STATUS:
				sendStatusEmails(ticket);
				break;
			case TICKET:
				sendNewRequestEmails(ticket);
				break;
			case ATTACHMENT:
			default:
				break;
		}
	}

	/**
	 * Helper method attempts to retrieve all the Account Managers.
	 * @return
	 * @throws EncryptionException 
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	protected List<AccountVO> getAdminEmails() throws EncryptionException {

		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.email_address_txt as owner_email_addr, a.first_nm, a.last_nm, a.profile_id as owner_profile_id from profile a ");
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

		return (List<AccountVO>)(List<?>)accounts;
	}

	/**
	 * Helper method builds an email sent to Requester and all Admins that a new
	 * ticket has been created.
	 * @param t
	 * @param ticketMsg 
	 * @return
	 * @throws InvalidDataException 
	 * @throws EncryptionException 
	 * @throws Exception 
	 */
	protected void sendNewRequestEmails(TicketEmailVO t) throws InvalidDataException, EncryptionException {

		//Get Admins
		List<AccountVO> admins = getAdminEmails();

		//Build Config
		Map<String, Object> config = getBaseConfig(t);

		//Get Recipients
		Map<String, String> recipients = getBaseRecipients(t);

		//New Tickets get sent to All Admins.
		for(AccountVO a : admins) {
			recipients.put(a.getOwnerProfileId(), a.getOwnerEmailAddr());
		}
		ecbu.sendMessage((String)attributes.get(NEW_TICKET_CAMP_INST_ID), recipients, config);

	}


	/**
	 * Helper method builds an email sent to Requester and Assigned Admin that
	 * assignment has changed.
	 * @param t
	 * @param ticketMsg 
	 * @return
	 * @throws InvalidDataException 
	 * @throws ActionException 
	 * @throws Exception 
	 */
	protected void sendAssignedEmails(TicketEmailVO t) throws InvalidDataException, ActionException {

		//Build Config
		Map<String, Object> config = getBaseConfig(t);
		config.put("firstName", t.getFirstName());
		config.put("lastName", t.getLastName());
		config.put("companyId", t.getCompanyId());
		config.put("reporterEmail", t.getReporterEmail());
		config.put("phoneNo", t.getPhoneNo());
		config.put("createDtFmt", t.getCreateDtFmt());

		ecbu.sendMessage((String)attributes.get(ASSN_TICKET_CAMP_INST_ID), getBaseRecipients(t), config);
	}

	/**
	 * Helper method builds an email sent to Requester and Assigned Admin that
	 * status has changed.
	 * @param t
	 * @return
	 * @throws InvalidDataException
	 * @throws ActionException
	 */
	protected void sendStatusEmails(TicketEmailVO t) throws InvalidDataException, ActionException {

		//Build Config
		Map<String, Object> config = getBaseConfig(t);
		config.put("statusNm", t.getStatusNm());
		config.put("assignedFirstNm", t.getAssignedFirstNm());
		config.put("assignedLastNm", t.getAssignedLastNm());

		//Get Emails
		ecbu.sendMessage((String)attributes.get(STAT_TICKET_CAMP_INST_ID), getBaseRecipients(t), config);
	}

	/**
	 * Helper method that puts together basic config.
	 * @param t
	 * @return
	 */
	protected Map<String, Object> getBaseConfig(TicketEmailVO t) {
		Map<String, Object> config = new HashMap<>();
		config.put("ticketLink", t.getTicketLink());
		config.put("ticketDesc", t.getDescText());
		config.put("ticketNo", t.getTicketNo());
		return config;
	}

	/**
	 * Helper method that builds normal emails for Assignee and Reporter.
	 * @param campaignInstanceId
	 * @param t
	 * @param config
	 * @return
	 */
	protected Map<String, String> getBaseRecipients(TicketEmailVO t) {
		Map<String, String> recipients = new HashMap<>();
		//Add Assignee
		if(!StringUtil.isEmpty(t.getAssignedEmail())) {
			recipients.put(t.getAssignedId(), t.getAssignedEmail());
		}

		//Add Reporter
		if(!StringUtil.isEmpty(t.getReporterEmail())) {
			recipients.put(t.getReporterId(), t.getReporterEmail());
		}
		
		String ccAddresses = (String) attributes.get("ccAddresses");
		if (!StringUtil.isEmpty(ccAddresses)) {
			addCCRecipients(recipients, ccAddresses);
		}
		
		return recipients;
	}

	
	/**
	 * Loop through the supplied cc addresses and get their profile id.
	 * If they don't exist create a profile for them.
	 * @param recipients
	 * @param ccAddresses
	 */
	private void addCCRecipients(Map<String, String> recipients, String ccAddresses) {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UserDataVO> users = new ArrayList<>();
		for (String address : ccAddresses.split(",")) {
			try {
				Map<String, String> searchMap = new HashMap<>();
				searchMap.put("EMAIL_ADDRESS_TXT", address.trim());
				List<UserDataVO> search = pm.searchProfile(dbConn, searchMap);
			
				if (search.isEmpty()) {
					UserDataVO user = new UserDataVO();
					user.setEmailAddress(address);
					pm.updateProfile(user, dbConn);
					users.add(user);
				} else {
					users.addAll(search);
				}
			} catch (DatabaseException e) {
				log.error("Failed to add email address: " + address, e);
			}
		}
		
		for (UserDataVO user : users) {
			recipients.put(user.getProfileId(), user.getEmailAddress());
		}
	}


	/**
	 * Helper method builds an email to the Requester containing message from
	 * Assigned Admin.
	 * @param t
	 * @param ticketMsg
	 * @return
	 * @throws InvalidDataException
	 */
	protected void sendActivityEmails(TicketEmailVO t) throws InvalidDataException {

		//Build Config
		Map<String, Object> config = getBaseConfig(t);
		config.put("ticketDesc", t.getActivities().get(0).getDescText());

		//Get Emails
		ecbu.sendMessage((String)attributes.get(ACT_TICKET_CAMP_INST_ID), getBaseRecipients(t), config);
	}


	/**
	 * Build the TicketActivity Email Notification. 
	 * @param act
	 * @throws ActionException 
	 */
	public void sendEmail(TicketActivityVO act) throws ActionException {
		TicketEmailVO t = loadTicket(act.getTicketId(), act.getActivityId());
		try {
			sendEmails(t, ChangeType.ACTIVITY);
		} catch (InvalidDataException | EncryptionException e) {
			throw new ActionException("There was a problem building Biomed support email.", e);
		}
	}
}