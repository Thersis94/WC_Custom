/**
 *
 */
package com.biomed.smarttrak.data;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.contact.SubmittalAction;
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

	//TODO Move these to a config File.
	public static final String ZOHO_TICKET_EMAIL = "siliconmtn_967168000000017005@bugs.zohoprojects.com";
	public static final String ASK_AN_ANALYST_MESSAGE_ID = "con_a3000f703d63b1da0a00142186c0cd47";
	public static final String ASK_AN_ANALYST_TYPE_ID = "con_6ce79b3d3d64f3980a00142169629ed6";
	public static final String ASK_AN_ANALYST_REFERRER_URL_ID = "con_f7315485834cf9b20a00141327be70a1";
	public static final String SMARTTRAK_EMAIL = "info.smarttrak@siliconmtn.com";
	public AnalystPostProcessor() {
		super();
	}
	public AnalystPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		String contactType = req.getParameter(ASK_AN_ANALYST_TYPE_ID);

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

		StringBuilder subject = new StringBuilder(75);
		subject.append("Smarttrak Bug Request - ").append(req.getParameter(SubmittalAction.CONTACT_SUBMITTAL_ID));

		try {
			EmailMessageVO email = new EmailMessageVO();
			email.addRecipient(ZOHO_TICKET_EMAIL);
			email.setFrom(SMARTTRAK_EMAIL);
			email.setTextBody(req.getParameter(ASK_AN_ANALYST_MESSAGE_ID));
			
			email.setSubject(subject.toString());

			new MessageSender(attributes, dbConn).sendMessage(email);
		} catch (InvalidDataException e) {
			log.error(e);
		}
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
		t.setDescText(req.getParameter(ASK_AN_ANALYST_MESSAGE_ID));
		t.setStatusCd(TicketVO.StatusCd.UNASSIGNED.getVal());
		t.setOrganizationId(((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		t.setReporterId(user.getProfileId());

		t.setReferrerUrl(req.getParameter(ASK_AN_ANALYST_REFERRER_URL_ID));

		//Build an Activity for Ticket Creation.
		TicketActivityVO tav = new TicketActivityVO();
		tav.setDescText("Ticket Submitted via Ask an Analyst.");
		tav.setProfileId(t.getReporterId());

		//Get a DBProcessor.
		DBProcessor db = new DBProcessor(getDBConnection());

		try {

			//Save the TicketVO
			db.save(t);

			//Update the TicketId
			t.setPrimaryKey(db.getGeneratedPKId());

			//Set it on the Ticket Activity VO
			tav.setTicketId(t.getTicketId());

			//Save the TicketActivityVO
			db.save(tav);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Problem Submitting Data", e);
		}
	}
}