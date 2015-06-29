package com.ansmed.sb.action.postprocess;

//SB Libs
import javax.servlet.http.HttpSession;

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
		
		//retrieve the selected value and store in session
		HttpSession session = req.getSession();
		String optChoice = req.getParameter("con_c0a80228d7bc7345474c59ff8c97b5e1");
		log.debug("User choice " + optChoice);
		
		//set only if they want to download pdf
		if(optChoice.equals("download-pdf")){	
			session.setAttribute("deliveryPref", optChoice);
		}else if(session.getAttribute("deliveryPref") != null ) session.removeAttribute("deliveryPref");
		
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
		StringBuilder msg = new StringBuilder();
		
		String imagePath = "http://www.poweroveryourpain.com/binary/org/ANS-MEDICAL/Power_Over_Your_Pain/post/";	
		
		msg.append("<table width='100%' cellspacing='0' cellpadding='0' border='0'");
		msg.append("bgcolor='#fff' align='center' style='font-size: 15px; font-family: Arial,sans-serif;'>");
		msg.append("<tr><td><table width='650' bgcolor='#fff' align='center' cellspacing='0' cellpadding='0' border='0'  ");
		msg.append("style='border: solid 1px #00a98f;'><tr><td>");
		msg.append("<table width='650' align='center' bgcolor='#fff' cellspacing='0' cellpadding='0' border='0' bgcolor='#fff'> ");
		msg.append("<tr><td align='left' width='55%' style='padding: 0 0 0 30px;'> ");
		msg.append("<img src='").append(imagePath).append("SJM_Logo_Standard_2C_NoTag_highres.jpg' ");
		msg.append(" alt='SJM_logo' width='320' height='110'  /></td> ");
		msg.append("<td align='left' width='45%' style='font-size: 12px; font-weight: bold; '> ");
		msg.append("<a style='color: #00a98f; text-decoration: none; padding: 0 5px;'  ");
		msg.append("href='http://poyp.sbdev.siliconmtn.com/about'>ABOUT SJM |</a> ");
		msg.append("<a style='color: #00a98f; text-decoration: none; padding: 0 5px;'  ");
		msg.append("href='http://poyp.sbdev.siliconmtn.com/privacy'>PRIVACY POLICY </a> ");
		msg.append("</td></tr></table> ");
		msg.append("<table width='650' bgcolor='#f3f4f4' cellspacing='0' cellpadding='0' border='0' align='center' > ");
		msg.append("<tr><td width='8%'></td><td align='left' width='28%' valign='top' style='padding: 40px 0 0; color: #e47f25; ");
		msg.append("font-weight: bold; font-size: 14px; line-height: 170%;'> ");
		msg.append("The upgradeable Prot&eacute;g&eacute;<span style='font-size: 9px; '>&#0153;");
		msg.append("</span> <br/> MRI IPG from <br />  St. Jude Medical.</td> ");
		msg.append("<td align='left' width='60%' style='padding: 15px 0 30px;'> ");
		msg.append("<img src='").append(imagePath).append("SJM_NB.png'  ");
		msg.append("width='275' height='240' alt='upgradeable_protege' /></td></tr></table> ");
		msg.append("<table width='650' bgcolor='#f3f4f4' cellspacing='0' cellpadding='0' border='0'  ");
		msg.append("align='center' style='font-family: Arial;' ><tr><td width='8%'></td> ");
		msg.append("<td align='left' colspan='3' style='background-color: #f3f4f4; padding: 0 0 10px; color: #00a98f; font-size: 27px; ");
		msg.append("font-family: Verdana; border-bottom: 1px solid #00a98f; line-height: 120%;'> ");
		msg.append("THANK YOU FOR YOUR INTEREST <br/> IN A NEUROSTIMULATION SYSTEM <br/> ");
		msg.append("FROM ST. JUDE MEDICAL.</td><td width='8%'></td></tr> ");
		msg.append("<tr><td height='40' colspan='2'></td></tr> ");
		msg.append("<tr><td width='8%'></td> ");
		msg.append("<td align='left' width='41%' style='padding: 0 ; background-color: #f4f5f5; line-height: 175%; font-size: 15px; '> ");
		msg.append("<span style='color: #00a98f; padding: 0; display: inline-block; font-size: 21px; '> ");
		msg.append("MOVE BEYOND PAIN.</span><br/> ");
		msg.append("We've attached an electronic version of our patient education brochure for your reference. If you  ");
		msg.append("requested the actual brochure, you'll be receiving one soon in the mail. The next step. is to ask your doctor about ");
		msg.append("neurostimulation, and the particular advantages of a system from St. Jude Medical in your case. We sincerly hope ");
		msg.append("it's a first step toward life after pain for you.</td><td width='10%'></td> ");
		msg.append("<td align='left' width='35%' bgcolor='#e9e9ea' style='padding: 0;'> ");
		msg.append("<table width='100%' height='100%' bgcolor='#e9e9ea' cellspacing='0' cellpadding='0' border='0' align='center'  ");
		msg.append("style='font-family: Arial; height: 290px;'><tr>");
		msg.append("<td width='10%' style='padding: 50px 0 0;'></td> ");
		msg.append("<td align='left' style=' padding: 10px 0; font-size: 16px;'> ");
		msg.append("<table width='100%' cellspacing='0' cellpadding='0' border='0' align='center'  ");
		msg.append("style='background-color: #00a98f; color: #fff;'><tr><td align='center' style='padding: 12px 0;'> ");
		msg.append("<a href='http://poyp.sbdev.siliconmtn.com/next/specialist' style='color: #fff; text-decoration: none;'> ");
		msg.append("Learn More</a></td></tr></table>	 ");
		msg.append("<br/><span style=' line-height: 140%;'> Let us help you find<br/> a pain specialist.</span></td>  ");
		msg.append("<td width='10%'></td></tr></table></td>	<td width='8%'></td></tr> ");
		msg.append("<tr><td height='40' colspan='2'></td></tr></table> ");
		msg.append("<table width='650' bgcolor='#fbfbfc' cellspacing='0' cellpadding='0' border='0' align='center'> ");
		msg.append("<tr><td width='169' align='left'> ");
		msg.append("<img src='").append(imagePath).append("SJM-CHUNG-TRACI-WICKHAM-20508_sm.jpg' ");
		msg.append("alt='SJM_wickham' height='240' width='169' style='display: block;'/></td> ");
		msg.append("<td width='180' style='padding: 0 15px; background-color: #00a98f; color: #fff; line-height: 175%; font-size: 14px;'>");
		msg.append("<a href='http://poyp.sbdev.siliconmtn.com/next/stories' style='color: #fff; text-decoration: none;'>  ");
		msg.append("Read about the lives our patients are living now that neurostimulation is managing their pain.</a></td> ");
		msg.append("<td width='7'></td> ");
		msg.append("<td width='295' bgcolor='#333333' style='color: #fff; font-size: 17px; padding: 0 20px; line-height: 170%; ");
		msg.append("background: #333 url(&quot;").append(imagePath).append("23SAMBENSON_SCS_SELECTS_CPJ_NB.jpg&quot;) ");
		msg.append("no-repeat scroll 100% center / 102% 240px;'><br/> ");
		msg.append("<a href='http://poyp.sbdev.siliconmtn.com/next/video' style='color: #fff; text-decoration: none;'> ");
		msg.append("WATCH FILMS ABOUT OUR PATIENT'S STORIES OF HOPE </a></td></tr></table> ");
		msg.append("</td></tr></table></td></tr></table> ");
			
		return msg.toString();
	}
}
