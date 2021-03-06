/**
 *
 */
package com.biomed.smarttrak.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.util.BiomedSupportEmailUtil;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.support.SupportTicketAction.ChangeType;
import com.smt.sitebuilder.action.support.TicketActivityVO;
import com.smt.sitebuilder.action.support.TicketVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: AnalystPostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Post Processor use by Ask An Analyst Contact us.
 * Manages forwarding data onto relevant sections of code.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 21, 2017
 ****************************************************************************/
public class AnalystPostProcessor extends SBActionAdapter {

	public static final String CFG_ZOHO_TICKET_EMAIL = "smarttrakAnalystZOHOEmail";
	public static final String CFG_JIRA_TICKET_EMAIL = "smarttrakAnalystJIRAEmail";
	public static final String CFG_ASK_AN_ANALYST_MESSAGE_ID = "smarttrakAnalystMessageId";
	public static final String CFG_ASK_AN_ANALYST_TYPE_ID = "smarttrakAnalystTypeId";
	public static final String CFG_ASK_AN_ANALYST_REFERRER_URL_ID = "smarttrakAnalystReffererId";
	public static final String CFG_SMARTTRAK_EMAIL = "smarttrakEmail";
	public AnalystPostProcessor() {
		super();
	}
	public AnalystPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		String contactType = req.getParameter((String)getAttribute(CFG_ASK_AN_ANALYST_TYPE_ID));
		UserVO vo = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		attributes.put(BiomedSupportEmailUtil.SOURCE, new EmailRecipientVO(vo.getSourceId(), vo.getSourceEmail(), EmailRecipientVO.BCC));

		if("Analyst".equals(contactType)) {
			//If is Analyst Request
			submitAnalystRequest(req);
		} else if("Tech Team".equals(contactType)) {
			//Else if is Tech Team Request
			submitTechTeamRequest(req);
		}
	}

	/**
	 * Process a Tech Team Request.
	 * @param req
	 */
	private void submitTechTeamRequest(ActionRequest req) {
		log.debug("Tech Team Request");
		//These go into ZOHO.  Submit over email.
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);

		StringBuilder subject = new StringBuilder(75);
		subject.append("Smarttrak Bug Request");

		String msg = StringUtil.checkVal(req.getParameter((String)getAttribute(CFG_ASK_AN_ANALYST_MESSAGE_ID)));
		StringBuilder body = new StringBuilder(150 + msg.length());
		body.append("<p>User: ").append(user.getFullName()).append("</p><p>Email Address: ").append(user.getEmailAddress()).append("</p>");
		body.append("<p>").append(msg).append("</p>");

		try {
			EmailMessageVO email = new EmailMessageVO();
			//TODO - Remove the reference to ZOHO when testing complete.
			email.addRecipient((String)getAttribute(CFG_ZOHO_TICKET_EMAIL));
			email.addRecipient((String)getAttribute(CFG_JIRA_TICKET_EMAIL));
			email.setFrom((String)getAttribute(CFG_SMARTTRAK_EMAIL));
			email.setHtmlBody(body.toString());
			email.setSubject(subject.toString());
			
			List<String> emails = loadTechTeamEmails();
			
			for(String e : emails) {
				email.addCC(e);
			}

			new MessageSender(attributes, dbConn).sendMessage(email);
		} catch (InvalidDataException e) {
			log.error(e);
		}
	}

	
	/**
	 * Load the emails of all active staff users with a direct access user assignment
	 * @return
	 */
	private List<String> loadTechTeamEmails() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct u.email_address_txt from core.register_data rd "); 
		sql.append("left join core.register_submittal rs on rs.register_submittal_id = rs.register_submittal_id ");
		sql.append("left join custom.biomedgps_user u on u.profile_id = rs.profile_id ");
		sql.append("where register_field_id = ? and value_txt = '1' and u.active_flg = ? and status_cd = ?");
		
		List<String> emails = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, RegistrationMap.ASSIGNEESECTIONS.getFieldId());
			ps.setInt(2, 1);
			ps.setString(3, "S");
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				emails.add(rs.getString("email_address_txt"));
			}
		} catch (SQLException e) {
			log.error("Failed to get tech team email addresses", e);
		}
		
		return emails;
	}
	/**
	 * Process an Analyst Request
	 * @param req
	 */
	private void submitAnalystRequest(ActionRequest req) {
		log.debug("Analyst Request");
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);

		//Make the TicketVO
		TicketVO t = new TicketVO();
		t.setDescText(req.getParameter((String)getAttribute(CFG_ASK_AN_ANALYST_MESSAGE_ID)));
		t.setStatusCd(TicketVO.StatusCd.UNASSIGNED.getVal());
		t.setOrganizationId(((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		t.setReporterId(user.getProfileId());

		t.setReferrerUrl(req.getParameter((String)getAttribute(CFG_ASK_AN_ANALYST_REFERRER_URL_ID)));

		//Build an Activity for Ticket Creation.
		TicketActivityVO tav = new TicketActivityVO();
		tav.setDescText("Ticket Submitted via Ask an Analyst.");
		tav.setProfileId(t.getReporterId());

		//Get a DBProcessor.
		DBProcessor db = new DBProcessor(getDBConnection());

		try {

			//Save the TicketVO
			db.save(t);

			//Set it on the Ticket Activity VO
			tav.setTicketId(t.getTicketId());

			//Save the TicketActivityVO
			db.save(tav);

			//Send Email.
			new BiomedSupportEmailUtil(dbConn, attributes).sendEmail(t.getTicketId(), ChangeType.TICKET);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Problem Submitting Data", e);
		} catch (Exception e) {
			log.error("Problem Sending Email", e);
		}
	}
}