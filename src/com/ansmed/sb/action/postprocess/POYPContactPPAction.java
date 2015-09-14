package com.ansmed.sb.action.postprocess;

//SB Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
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

	/**
	 * Default constructor
	 */
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
			emailVo.setFrom("contact@sjm.com", "Laura Sterling");
			emailVo.setSubject("Request Info Kit");
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
	
	/**
	 * Creates the post generated email
	 * @param req
	 * @return
	 */
	protected String getMessage(SMTServletRequest req) {
		StringBuilder msg = new StringBuilder(1000);

		//Get the current site info
		String siteAlias = ((SiteVO) req.getAttribute("siteData")).getSiteAlias();	
		Integer sslLevel = ((SiteVO) req.getAttribute("siteData")).getSsl();
		
		//build the paths
		//String binaryLoc = "/binary/org/ANS-MEDICAL/Power_Over_Your_Pain/post/";
		String basePath = "http://" + siteAlias;
		if(sslLevel == 1){
			basePath = "https://" + siteAlias;
		}
		
		//build the email
		msg.append("<table width='100%' cellspacing='0' cellpadding='0' border='0' ");
		msg.append("bgcolor='#fff' align='center' style='font-size: 15px; ");
		msg.append("font-family: Arial,sans-serif; border-collapse: collapse;'><tr><td> ");
		msg.append("<table width='650' bgcolor='#f3f4f4' align='center' cellspacing='0' cellpadding='0' border='0' ");
		msg.append("style='border: solid 1px #00a98f;'><tr><td> ");
		msg.append("<table width='650' align='center' cellspacing='0' cellpadding='0' border='0' bgcolor='#f3f4f4' ");
		msg.append("style='line-height: 140%; font-family: Arial,sans-serif;'> ");
		msg.append("<tr height='25' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("<tr><td width='8%'></td> ");
		msg.append("<td align='left' width='84%' style='padding: 0;'> ");
		msg.append("<span style='padding: 0; display: inline-block; font-size: 27px; font-weight: 600; ");
		msg.append("line-height: 115%; font-family: arial;'> ");
		msg.append("Thank You For Your Interest In A Neurostimulation System From <br/>St. Jude Medical. ");
		msg.append("</span></td><td width='8%'></td></tr>");
		msg.append("<tr height='20' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("<tr><td width='8%'></td><td align='left' width='84%'> ");
		msg.append("<span style='padding: 0 0 5px; display: inline-block; font-size: 19px;'> ");
		msg.append("Pain Interrupted.</span><br/> ");
		msg.append("We've attached an electronic version of our patient education brochure for ");
		msg.append("your reference. The next step is to ask your doctor about neurostimulation and ");
		msg.append("the particular advantages of a system from St. Jude Medical in your case. ");
		msg.append("We sincerely hope it's a first step toward life after pain for you. ");
		msg.append("</td><td width='8%'></td></tr> ");
		msg.append("<tr height='20' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("<tr><td width='8%'></td> ");
		msg.append("<td align='left' width='84%' style='padding: 0 0 10px; line-height: 130%;'> ");
		msg.append("<table width='100%' align='center' cellspacing='0' cellpadding='0' border='0' ");
		msg.append("style='font-family: Arial,sans-serif;'><tr><td align='left' colspan='2' style='padding: 0 0 7px;'>");
		msg.append("<span style='display: inline-block; font-size: 19px;'> ");
		msg.append("Learn More.</span><br/></td></tr><tr><td valign='top' colspan='2' style='padding: 0 0 5px 0;'> ");
		msg.append("Hear from people who have chosen neurostimulation to manage their<br/> chronic pain: ");
		msg.append("</td></tr><tr><td valign='top' width='8%'>&bull;</td> ");
		msg.append("<td valign='top' style='padding: 0 0 5px 0;'>Read their  ");
		msg.append("<a style='color: #00a98f; display: inline-block; text-decoration: none;' ");
		msg.append("href='").append(basePath).append("/next/stories'> ");
		msg.append("stories</a></td></tr> ");
		msg.append("<tr><td valign='top' width='8%'>&bull;</td> ");
		msg.append("<td valign='top' style='padding: 0 0 5px 0;'> Watch ");
		msg.append("<a style='color: #00a98f; display: inline-block; text-decoration: none;' ");
		msg.append("href='").append(basePath).append("/next/video'> ");
		msg.append("videos</a></td></tr><tr><td valign='top' colspan='2' style='padding: 0 0 5px 0;'> ");
		msg.append("Find a pain specialist<a style='color: #00a98f; display: inline-block; text-decoration: none;' ");
		msg.append("href='").append(basePath).append("/next/specialist'> ");
		msg.append("in your area</a></td></tr></table></td><td width='8%'></td> ");
		msg.append("</tr><tr height='25' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("</table></td></tr></table></td></tr></table> ");
		
		return msg.toString();
	}
}
