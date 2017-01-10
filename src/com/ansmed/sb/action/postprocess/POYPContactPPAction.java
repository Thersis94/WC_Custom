package com.ansmed.sb.action.postprocess;

//SB Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
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
	public void build(ActionRequest req) throws ActionException {
		log.info("Starting POYP action build");
		String rcpt = req.getParameter("pfl_EMAIL_ADDRESS_TXT");
		if (!StringUtil.isValidEmail(rcpt)) return;  //can't send an email to an invalid address!
		
		//format the email for sending
		try{
			EmailMessageVO emailVo = new EmailMessageVO();
			emailVo.addRecipient(rcpt);
			emailVo.setFrom("contact@sjmneuro.com", "St. Jude Medical");
			emailVo.setSubject("The Chronic Pain Therapy Materials You Requested");
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
	protected String getMessage(ActionRequest req) {
		StringBuilder msg = new StringBuilder(6500);

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
		msg.append("font-family: Arial,sans-serif; border-collapse: collapse;'><tr><td>  ");
		msg.append("<table width='650' bgcolor='#f3f4f4' align='center' cellspacing='0' cellpadding='0' border='0' ");
		msg.append("style='border: solid 1px #00a98f;'><tr><td> ");
		msg.append("<table width='650' align='center' cellspacing='0' cellpadding='0' border='0' bgcolor='#f3f4f4' ");
		msg.append("style='line-height: 140%; font-family: Arial,sans-serif;'> ");
		msg.append("<tr height='40' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("<tr><td width='8%'></td><td align='left' width='84%'> ");
		msg.append("Thank you for your interest in St. Jude Medical&trade; neurostimulation therapy.  ");
		msg.append("The information you requested can be viewed ");
		msg.append("<a style='color: #00a98f; display: inline-block; text-decoration: none;' download='patient-education-brochure' ");
		msg.append("href='").append(basePath).append("/binary/org/ANS-MEDICAL/new_video/NeurostimulationPatientEducationBrochureSt.JudeMedical.pdf'> ");
		msg.append("here</a>. ");
		msg.append("We hope that you find it helpful. </td><td width='8%'></td></tr>");
		msg.append("<tr height='20' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("<tr><td width='8%'></td> ");
		msg.append("<td align='left' width='84%' style='padding: 0 0 10px; line-height: 140%;'> ");
		msg.append("<table width='100%' align='center' cellspacing='0' cellpadding='0' border='0' style='font-family: Arial,sans-serif;'> ");
		msg.append("<tr><td valign='top' colspan='3' style='padding: 0 0 5px 0; line-height: 140%;'> ");
		msg.append("Once you&#39;ve had an opportunity to learn more about neurostimulation, the next step is ");
		msg.append("to schedule an appointment with a local pain management specialist to determine if ");
		msg.append("an evaluation of neurostimulation with the St. Jude Medical&trade; Invisible Trial System ");
		msg.append("is right for you. During this evaluation period, you can see if the therapy relieves ");
		msg.append("your pain and enables you to go about your daily routine. If it does, then you can ");
		msg.append("talk to your doctor about a low-maintenance implant. ");
		msg.append("In the meantime, we encourage you to use ");
		msg.append("<a style='color: #00a98f; display: inline-block; text-decoration: none;' ");
		msg.append("href='http://www.poweroveryourpain.com/'>PowerOverYourPain.com</a> to:</td></tr> ");
		msg.append("<tr><td valign='top' width='6%'></td><td valign='top' width='6%'>&bull;</td> ");
		msg.append("<td valign='top' style='padding: 0 0 5px 0;'> ");
		msg.append("<a style='color: #00a98f; display: inline-block; text-decoration: none;' ");
		msg.append("href='http://www.poweroveryourpain.com/getting/evaluation'> ");
		msg.append("Learn about the temporary evaluation period</a></td></tr> ");
		msg.append("<tr><td valign='top' width='6%'></td><td valign='top' width='6%'>&bull;</td> ");
		msg.append("<td valign='top' style='padding: 0 0 5px 0;'> ");
		msg.append("<a style='color: #00a98f; display: inline-block; text-decoration: none;' ");
		msg.append("href='http://www.poweroveryourpain.com/next/specialist'> ");
		msg.append("Find a pain specialist in your area</a></td></tr> ");
		msg.append("<tr height='20' width='100%'><td></td> <td></td><td></td></tr> ");
		msg.append("<tr><td valign='top' colspan='3'>We know that making a decision ");
		msg.append("about pain management can be difficult, but you don&#39;t have to do it alone.<br><br> ");
		msg.append("Sincerely,<br><br>The Power Over Your Pain Team</td></tr> ");
		msg.append("<tr height='180' width='100%'><td></td> <td></td><td></td></tr> ");
		msg.append("<tr><td colspan='3' style='font-size: 11px; '> ");
		msg.append("<span style='font-weight: bold'>Rx Only</span><br> ");
		msg.append("<span style='font-weight: bold'>Brief Summary:</span> Prior to using these devices, please ");
		msg.append("review the Instructions for Use for a complete listing of indications, contraindications, warnings, ");
		msg.append("precautions, potential adverse events and directions for use.<br> ");
		msg.append("<span style='font-weight: bold'>Indications for Use:</span> Spinal cord stimulation as an aid in ");
		msg.append("the management of chronic, intractable pain of the trunk and limbs.<br> ");
		msg.append("<span style='font-weight: bold'>Contraindications:</span> Patients who are unable to operate the ");
		msg.append("system or who have failed to receive effective pain relief during trial stimulation.<br/> ");
		msg.append("<span style='font-weight: bold'>Warnings/Precautions:</span> Diathermy therapy, implanted cardiac ");
		msg.append("systems, magnetic resonance imaging (MRI), explosive or flammable gases, theft detectors and ");
		msg.append("metal screening devices, lead movement, operation of machinery and equipment, postural changes, ");
		msg.append("pediatric use, pregnancy, and case damage. Patients who are poor surgical risks, with multiple ");
		msg.append("illnesses, or with active general infections should not be implanted.<br> ");
		msg.append("<span style='font-weight: bold'>Adverse Effects:</span> Painful stimulation, loss of pain relief, ");
		msg.append("surgical risks (e.g. paralysis). User&#39;s Guide must be reviewed for detailed disclosure.</td></tr> ");
		msg.append("<tr height='30' width='100%'><td></td> <td></td><td></td></tr> ");
		msg.append("<tr><td colspan='3' style='font-size: 11px; '> ");
		msg.append("Unless otherwise noted, &trade; indicates that the name is a trademark of, or licensed to, St. Jude Medical or ");
		msg.append("one of its subsidiaries. ST. JUDE MEDICAL and the nine-squares symbol and trademarks and service marks ");
		msg.append("of St. Jude Medical, Inc. and its related companies. &copy; 2015 St. Jude Medical, Inc. All Rights Reserved.</td></tr>  ");
		msg.append("<tr height='50' width='100%'><td></td> <td></td><td></td></tr> ");
		msg.append("<tr><td colspan='3' style='font-size: 11px; '> ");
		msg.append("SJM-CPG-0815-0026a | Item approved for U.S. use only.</td></tr> ");
		msg.append("</table></td><td width='8%'></td></tr> ");
		msg.append("<tr height='25' width='100%'><td ></td><td ></td><td ></td></tr> ");
		msg.append("</table></td></tr></table></td></tr></table> ");
		
		return msg.toString();
	}
}
