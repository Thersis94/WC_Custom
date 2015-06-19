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
		
		msg.append("<table width='100%' cellspacing='0' cellpadding='0' border='0' ");
		msg.append("bgcolor='#fff' style='font-size: 16px; font-family: Arial, sans-serif;' ");
		msg.append("<tr><td><table width='750px' bgcolor='#fff' ");
		msg.append("border='0' align='center' cellspacing='0' cellpadding='0' border='0' style='border: solid 1px #00a98f;'> ");
		msg.append("<tr><td> <table width='750px' align='center' bgcolor='#fff' cellspacing='0' cellpadding='0' border='0' > ");
		msg.append("<tr><td style='width: 55%; padding: 0 0 0 30px;'> ");
		msg.append("<img alt='SJM_logo' style='width: 320px;' ");
		msg.append("src='").append(imagePath).append("SJM_Logo_Standard_2C_NoTag_highres.jpg' /> ");
		msg.append("</td><td style='width: 45%; font-size: 12px; font-weight: bold;'> ");
		msg.append("<a style='color: #00a98f; text-decoration: none; margin: 0 5px;' ");
		msg.append("href='http://poyp.sbdev.siliconmtn.com/about'>ABOUT SJM |</a> ");
		msg.append("<a style='color: #00a98f; text-decoration: none; margin: 0 5px;' ");
		msg.append("href='http://poyp.sbdev.siliconmtn.com/privacy'>PRIVACY POLICY |</a> ");
		msg.append("</td></tr></table> ");
		msg.append("<table width='750px' bgcolor='#f3f4f4' cellspacing='0' cellpadding='0' border='0' align='center' ");
		msg.append("style='padding: 0 60px; font-family: trebuchet ms, helvetica;'> ");
		msg.append("<tr><td style='padding: 15px 0; font-family: Arial, sans-serif;'> ");
		msg.append("<p style='margin: 0;'><span style='float: left; color: #e47f25;  ");
		msg.append("font-weight: bold; margin-top: 27px; font-size: 16px; line-height: 170%;'> ");
		msg.append("The upgradeable Prot&eacute;g&eacute; ");
		msg.append("<span style='font-size: 9px;'>&#0153;</span> <br/> MRI IPG from <br /> St. Jude Medical. ");
		msg.append("<span style='padding: 0 0 0 15px; font-size: 35px; font-weight: ");
		msg.append("normal; margin-top: -10px; position: relative; top: 15px;'>&tilde;</span> ");
		msg.append("</span> ");
		msg.append("<img style=' width: 480px; float: left; margin-left: -100px;' ");
		msg.append("src='").append(imagePath).append("SJM.png' alt='protege_mri'/> ");
		msg.append("</p></td></tr> ");
		msg.append("<tr><td style='background-color: #f3f4f4;'> ");
		msg.append("<h3 style='color: #00a98f; margin: 0; font-size: 35px; line-height: 130%; ");
		msg.append("padding: 0 0 10px; border-bottom: 1px solid #00a98f;'> ");
		msg.append("THANK YOU FOR YOUR INTERESET <br/> IN A NEUROSTIMULATION SYSTEM <br/> ");
		msg.append("FROM ST. JUDE MEDICAL</h3></td></tr> ");
		msg.append("<tr><td style='padding: 40px 0 75px; background-color: #f4f5f5;'> ");
		msg.append("<p style='float: left; width: 48%; line-height: 150%; '> ");
		msg.append("<span style='color: #00a98f; padding: 0; display: inline-block; font-size: 21px;'> ");
		msg.append("MOVE BEYOND PAIN.</span><br/> ");
		msg.append("We've attached an electronic version of our patient education brochure for your reference. ");
		msg.append("If you requested the actual brochure, you'll be receiving one soon in the mail. ");
		msg.append("The next step. is to ask your doctor about neurostimulation, and the particular ");
		msg.append("advantages of a system from St. Jude Medical in your case. We sincerly hope ");
		msg.append("it's a first step toward life after pain for you.</p> ");
		msg.append("<p style='float: right; width: 35%; padding: 75px 5px 30px; margin: 15px 0 0; ");
		msg.append("background-color: #e9e9ea; height: 160px;'> ");
		msg.append("<a href='http://poyp.sbdev.siliconmtn.com/next/specialist' style='background-color: #00a98f; ");
		msg.append("color: #fff; display: inline-block; padding: 10px 35px; margin: 10px 0 10px 25px; text-decoration: none;'> ");
		msg.append("Learn More</a><br/>	 ");
		msg.append("<span style='display: inline-block; margin: 10px 0 0 25px; line-height: 140%;'> ");
		msg.append("Let us help you find<br/> a pain specialist.</span></p></td></tr></table> ");
		msg.append("<table width='750px' bgcolor='#fbfbfc' cellspacing='0' cellpadding='0' border='0'> ");
		msg.append("<tr><td> ");
		msg.append("<img src='").append(imagePath).append("SJM-CHUNG-TRACI-WICKHAM-20508_sm.jpg'  ");
		msg.append("alt='SJM_wickham' style='width: 100%; height: 240px; float: left; width: 25%;' /> ");
		msg.append("<a href='http://poyp.sbdev.siliconmtn.com/next/stories'> ");
		msg.append("<p style='float: left; width: 33%; height: 240px; margin: 0 7px 0 0; ");
		msg.append("background-color: #00a98f; color: #fff; line-height: 175%; font-size: 16px;'> ");
		msg.append("<span style='display: inline-block; margin: 65px 27px 0;'> ");
		msg.append("Read about the lives our patients are living now that neurostimulation is managing their pain.<span></p></a> ");
		msg.append("<a href='http://poyp.sbdev.siliconmtn.com/next/video'><span style='background-image:  ");
		msg.append("url(&#39;").append(imagePath).append("23SAMBENSON_SCS_SELECTS_CPJ_NB.jpg&#39;); ");
		msg.append("background-repeat: no-repeat; background-size: 100% 240px; display: inline-block; width: 100%;");
		msg.append("height: 260px; float: left; width: 41%; margin-bottom: -20px;'> ");
		msg.append("<span style='display: inline-block; color: #fff; font-size: 18px; ");
		msg.append("margin: 100px 2px 0 15px; line-height: 170%;'> ");
		msg.append("WATCH FILMS ABOUT OUR PATIENT'S STORIES OF HOPE</span></span></a> ");
		msg.append("</td></tr></table> ");
		msg.append("</td></tr></table></td></tr></table> ");
		
		return msg.toString();
	}
}
