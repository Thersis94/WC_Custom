package com.ansmed.sb.action.postprocess;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * Title: POYPContactPPAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: <p/>
 * Copyright: Copyright (c) 2015<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Jun 11, 2015
 ****************************************************************************/

public class POYPContactPPAction extends SBActionAdapter {


	public POYPContactPPAction() {
		
	}
	
	/**
	 * Takes action init for initialization
	 * @param actionInit
	 */
	public POYPContactPPAction(ActionInitVO actionInit){
		this.actionInit = actionInit;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.info("Starting POYP action build");
		String rcpt = req.getParameter("pfl_EMAIL_ADDRESS_TXT");
		if (!StringUtil.isValidEmail(rcpt)) return;  //can't send an email to an invalid address!
		
		//format the email for sending
		try{
			EmailMessageVO emailVo = new EmailMessageVO();
			emailVo.addRecipient(rcpt);
			emailVo.setFrom("devon@siliconmtn.com", "Devon Franklin");
			emailVo.setSubject("The Best Group Around");
			emailVo.setHtmlBody(this.getMessage(req));
			log.info("Mail Info: " + emailVo.toString());
			
			//send the email
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(emailVo);
			log.debug("status=" + emailVo.getState() + ", errors=" + emailVo.getErrorString());
			if (emailVo.getErrorString() != null) {
				throw new MailException(emailVo.getErrorString());
			}
		}catch(Exception e){
			log.error("Error sending email. " + e);
		}
	}
	
	protected String getMessage(SMTServletRequest req) {
		StringBuilder msg = new StringBuilder();
		msg.append("<p>Dear ").append(req.getParameter("pfl_FIRST_NM"));
		msg.append(" ").append(req.getParameter("pfl_FIRST_NM")).append(" ");
		msg.append(req.getParameter("pfl_LAST_NM")).append(",</p>");
		
		msg.append("<p>This is a simple test for email setting up email. ");
		msg.append("If you have any questions please, please hesistate to ask. ");
		msg.append("If you still need to ask questions contact the Pope or Tim ");
		msg.append("They will be able to answer any questions you have. </p>");
		
		msg.append("<p>--<br/>Best Regards,<br/>Devon Franklin<br/>");
		msg.append("Office  978 686 1700 x205<br/>Mobile 978 815 3960<br/></p>");
		
		return msg.toString();
	}
}
