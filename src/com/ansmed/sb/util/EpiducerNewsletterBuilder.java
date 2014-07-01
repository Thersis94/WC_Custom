package com.ansmed.sb.util;

/****************************************************************************
 * <b>Title</b>: EpiducerNewsletterBuilder.java<p/>
 * <b>Description: </b> Contains the newsletter templates for each version of newsletter that will be
 * sent to a physician who attended an Epiducer training course. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since July 27, 2011
 ****************************************************************************/
public class EpiducerNewsletterBuilder {
	
	/**
	 * default constructor
	 */
	public EpiducerNewsletterBuilder() {}
	
	public StringBuffer getNewsLetter(Integer version, String optOutEmail) {
		StringBuffer nl = null;
		switch(version) {
		case 1:
			nl = getNewsletterOne();
			nl.append(getFooter());
			break;
		case 2:
			nl = getNewsletterTwo();
			break;
		case 3:
			nl = getNewsletterThree();
			break;
		case 4:
			nl = getNewsletterFour();
			break;
		case 5:
			nl = getNewsletterFive();
			break;
		case 6:
			nl = getNewsletterSix();
			break;
		default:
			break;
		}
		
		StringBuffer s = getOptOutText(optOutEmail);
		if (s!= null) nl.append(s);
		return nl;
	}
	
	/**
	 * Returns a String of subject text based on the newsletter version passed in
	 * @param version
	 * @return
	 */
	public String getNewsLetterSubject(Integer version) {
		String subject = null;
		switch(version) {
		case 1:
			subject = "St. Jude Medical | The Entry Point | Issue 1 | Epiducer lead delivery system";
			break;
		case 2:
			subject = "St. Jude Medical | The Entry Point | Issue 2 | Epiducer lead delivery system";
			break;
		case 3:
			subject = "St. Jude Medical | The Entry Point | Issue 3 | Epiducer lead delivery system";
			break;
		case 4:
			subject = "St. Jude Medical | The Entry Point | Issue 4 | Epiducer lead delivery system";
			break;
		case 5:
			subject = "St. Jude Medical | The Entry Point | Issue 5 | Epiducer lead delivery system";
			break;
		case 6:
			subject = "St. Jude Medical | The Entry Point | Issue 6 | Epiducer lead delivery system";
			break;
		default:
			break;
		}
		return subject;
	}
	
	/**
	 * Newsletter edition 1
	 * @return
	 */
	private StringBuffer getNewsletterOne() {
		StringBuffer sb = new StringBuffer();
		sb.append("<link epiducer=\"\" online=\"\" page=\"\" product=\"\" to=\"\" />");
		sb.append("<table bgcolor=\"#DCDDDF\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"700\">");
		sb.append("<tbody>");
		sb.append("<tr><td colspan=\"5\"><a href=\"http://goo.gl/H7eff\"><img alt=\"The Entry Point\" height=\"193\" src=\"http://www.epiducer.com/binary/org/ANS-MEDICAL/Epiducer/images/epiducer-banner.jpg\" width=\"700\" border=\"0\"/></a></td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td>");
		sb.append("<td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px; line-height:50px\"><font color=\"#006c56\"><strong>WELCOME</strong></font></span><br />");
		sb.append("Thank you for attending the lead delivery system training course. You are now a member of the small group qualified to perform spinal cord stimulation procedures using this revolutionary new introduction system.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("As a part of this exclusive group of physicians, you now have access to <a href=\"http://goo.gl/HmrcP\" style=\"text-decoration: none;\"><font color=\"#006c56\">Epiducer.com</font></a>. Your user name is the email address to which this newsletter was delivered and your password is &ldquo;delivers.&rdquo; On this site, you will find case studies, clinical data, product information, procedural tips, and reimbursement information related to the Epiducer lead delivery system.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("In addition to your access to Epiducer.com, you will also be receiving this bi-weekly newsletter for the next three months. The purpose of this newsletter is to keep you informed about news and information regarding the Epiducer lead delivery system and relevant information on Epiducer.com.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("The Epiducer lead delivery system is a game-changing introduction tool, allowing for new procedural options and lead configurations in a percutaneous implant. We look forward to keeping you informed and helping you in your successes with this unique and innovative product.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("To your success,</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<em>-The St. Jude Medical Epiducer lead delivery system team</em></p>");
		sb.append("</td>");
		sb.append("<td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<!--  <tr><td width=\"12\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"16\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"12\">&nbsp;</td></tr>  -->");
		sb.append("<tr><td width=\"12\">&nbsp;</td>");
		sb.append("<td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px;\"><font color=\"#006c56\"><strong>THE IMPORTANCE OF EXPERIENCE</strong></font></span><br />");
		sb.append("The Epiducer lead delivery system may have a slightly different feel than a traditional percutaneous needle. As such, it is important to get experience using the system in order to feel comfortable. Dr. Stefan Schu from Dusseldorf, Germany, says, &quot;Looking back after having completed dozens of cases using the Epiducer system, I have realized that it took at least five implants before I felt completely comfortable with the system. As with any new procedure, there was a learning curve that had to be overcome at the beginning, but now most experienced SCS physicians in Europe have changed completely to the Epiducer system to implant S-Series&trade; leads because of the excellent long-term results experienced by our patients.&rdquo;</p></td>");
		sb.append("<td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td>");
		sb.append("<td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px;\"><font color=\"#006c56\"><strong>PROCEDURE ANIMATION</strong></font></span><br />");
		sb.append("During your training course, you viewed an animation which provided an overview of a procedure with the Epiducer delivery system. If you wish to review the animation, <a href=\"http://goo.gl/VmYsD\" style=\"text-decoration: none;\"><font color=\"#006c56\">you can find it here</font></a><font color=\"#006c56\">.</font></p>");
		sb.append("</td>");
		sb.append("<td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td>");
		sb.append("<td colspan=\"3\"><p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px;\"><font color=\"#006c56\"><strong>PRODUCT HIGHLIGHTS</strong></font></span><br />");
		sb.append("To view a page on Epiducer.com highlighting key features and benefits of the Epiducer lead delivery system, <a href=\"http://goo.gl/QM7qO\" style=\"text-decoration: none;\"><font color=\"#006c56\">click here</font></a><font color=\"#006c56\">.</font></p></td>");
		sb.append("<td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		return sb;
	}
	
	/**
	 * Newsletter edition 2
	 * @return
	 */	
	private StringBuffer getNewsletterTwo() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table bgcolor=\"#DCDDDF\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"700\">");
		sb.append("<tbody>");
		sb.append("<tr><td colspan=\"5\"><a href=\"http://goo.gl/Zrr8g\"><img alt=\"The Entry Point\" src=\"http://www.epiducer.com/binary/org/ANS-MEDICAL/Epiducer/images/epiducer-banner2.jpg\" style=\"width: 700px; height: 193px;\" /></a></td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px; line-height:50px\"><font color=\"#006c56\"><strong>SAFETY-FOCUSED DESIGN</strong></font></span><br />");
		sb.append("We hope your experience with the Epiducer lead delivery system has been exceptional.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("St. Jude Medical is dedicated to reducing risk wherever possible and is focused on seeking simpler solutions to complex medical problems. The Epiducer system was designed to reduce procedural complexity and offer unprecedented options with an innovative, safety-focused design.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("Since receiving TUV approval in September 2009, various studies have been conducted in Europe on the Epiducer system. The following are some highlights of relevant clinical experience:</p>");
		sb.append("<ul>");
		sb.append("<li style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">A prospective, multicenter study consisting of 34 patients showed no adverse events related to the Epiducer lead delivery system.<sup>1</sup></li>");
		sb.append("<li style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">A survey of 28 experienced physicians reported zero neurological deficits in 490 cases.<sup>2</sup></li>");
		sb.append("<li style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">All physicians using the Epiducer system have undergone a didactic and cadaver training session before their first case with the system.</li>");
		sb.append("</ul>");
		sb.append("</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px;\"><font color=\"#006c56\"><strong>FEATURED CASE STUDY, ISSUE 2</strong></font></span><br />");
		sb.append("Visit <a href=\"http://goo.gl/SQ1G3\" style=\"text-decoration: none;\"><font color=\"#006c56\">Epiducer.com</font></a> and click the CASE STUDIES tab to review case studies that have been submitted by the Epiducer system faculty.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("The featured case study this week highlights a patient with left low back and leg pain. This patient was implanted with an S-Series&trade; S-8 lead and an Octrode&trade; lead using the Epiducer system. The resulting paresthesia overlap completely covered the patient&rsquo;s pain.</p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("To review the case in detail, be sure to visit <a href=\"http://goo.gl/LtrbH\" style=\"text-decoration: none;\"><font color=\"#006c56\">Epiducer.com.</font></a></p>");
		sb.append("</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px;\"><font color=\"#006c56\"><strong>NEW TECHNOLOGY ASSESSMENT</strong></font></span><br />");
		sb.append("The Epiducer delivery system has already been investigated in various studies and reports in Europe. David Log&eacute;, MD, and Olivier De Coster, MD, authored the first comprehensive new technology assessment of the Epiducer system procedure in Belgium.3 The assessment included 46 patients at 7 investigational sites. In the report, all physicians indicated that they were satisfied with the device. To learn more about this assessment and to view the results, <a href=\"http://goo.gl/oZ6C8\" style=\"text-decoration: none;\"><font color=\"#006c56\">click here</font></a>.</p>");
		sb.append("</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px;\">");
		sb.append("<span style=\"font-family: Arial,Helvetica,sans-serif; font-size: 18px;\"><font color=\"#006c56\"><strong>CLINICAL STUDY&mdash;SAFETY</strong></font></span><br />");
		sb.append("David Log&eacute;, MD, and Olivier De Coster, MD, published the results of a clinical study of 34 patients implanted using the Epiducer lead delivery system.<sup>1</sup> The study reported no adverse events related to the Epiducer system.</p>");
		sb.append("</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\">");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		sb.append("<sup>1.</sup>Log&eacute; D, De Coster O. A prospective evaluation to assess the safety of using a newly developed delivery system for percutaneous introduction of SCS paddle leads into the epidural space. Poster presented at: Annual meeting of the North American Neuromodulation Society; December 3-6, 2009; Las Vegas, NV.<br />");
		sb.append("<sup>2.</sup>Data on file.<br />");
		sb.append("<sup>3.</sup>Log&eacute; D, De Coster O. Use of a newly developed delivery device for percutaneous introduction of spinal cord stimulation leads. Poster presented at: International Neuromodulation Society (INS) 10th World Congress; May 23, 2011; London, England.</p>");
		sb.append("</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><hr align=\"center\" size=\"2\" width=\"100%\" /></td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td colspan=\"3\"><div style=\"float:right; font-family: Arial,Helvetica,sans-serif; font-size: 12px;\"><a href=\"http://goo.gl/d6GnR\" style=\"text-decoration: none;\"><font color=\"#006c56\">www.epiducer.com</font></a></div><br /><div>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		sb.append("<strong>SCS Systems&mdash;Indications for Use: Spinal cord stimulation as an aid in the management of chronic, intractable pain of the trunk and limbs. Contraindications: Demand-type cardiac pacemakers, patients who are unable to operate the system or who fail to receive effective pain relief during trial stimulation. Warnings/Precautions: Diathermy therapy, cardioverter defibrillators, magnetic resonance imaging (MRI), explosive or flammable gases, theft detectors and metal screening devices, lead movement, operation of machinery and equipment, postural changes, pediatric use, pregnancy, and case damage. Patients who are poor surgical risks, with multiple illnesses, or with active general infections should not be implanted. Adverse Effects: Painful stimulation, loss of pain relief, surgical risks (e.g., paralysis).</strong></p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		sb.append("<strong>Epiducer System&mdash;Intended Use: The Epiducer lead delivery system is intended to introduce Lamitrode S-Series paddle leads and/or St. Jude Medical Neuromodulation Division percutaneous leads into the epidural space at or below the L1 vertebra. Warnings: Patients who have a large amount of epidural scarring and/or adhesions may not be good candidates for the Epiducer system. Prior to surgery, detailed imaging should be used to assess the size of the epidural space. The Epiducer system should not be inserted into the epidural space superior to the L1 vertebra. Patients with a spinal cord that extends past the L1 vertebra may require a lower insertion level. Adverse Effects: Spinal cord compression and/or injury, epidural hemorrhage or hematoma, paralysis.</strong></p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		sb.append("<strong>Clinician&rsquo;s Manual must be reviewed prior to use for detailed disclosure. Rx only.</strong></p>");
		sb.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		sb.append("<strong>Epiducer, Octrode, and S-Series are trademarks of Advanced Neuromodulation Systems, Inc. d/b/a St. Jude Medical Neuromodulation Division. ST. JUDE MEDICAL, the nine-squares symbol, and MORE CONTROL. LESS RISK. are trademarks and service marks of St. Jude Medical, Inc. and its related companies. &copy;2011 St. Jude Medical Neuromodulation Division. All rights reserved.</strong></p>");
		sb.append("</div>");
		sb.append("</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("<tr><td width=\"12\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"16\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"12\">&nbsp;</td></tr>");
		sb.append("</tbody>");
		sb.append("</table>");
		return sb;
	}
	
	/**
	 * Newsletter edition 3
	 * @return
	 */
	private StringBuffer getNewsletterThree() {
		StringBuffer sb = new StringBuffer();
		return sb;
	}
	
	/**
	 * Newsletter edition 4
	 * @return
	 */
	private StringBuffer getNewsletterFour() {
		StringBuffer sb = new StringBuffer();
		return sb;
	}
	
	/**
	 * Newsletter edition 5
	 * @return
	 */
	private StringBuffer getNewsletterFive() {
		StringBuffer sb = new StringBuffer();
		return sb;
	}
	
	/**
	 * Newsletter edition 6
	 * @return
	 */
	private StringBuffer getNewsletterSix() {
		StringBuffer sb = new StringBuffer();
		return sb;
	}
	
	/**
	 * The footer text for each newsletter
	 * @return
	 */
	private StringBuffer getFooter() {
		StringBuffer fp = new StringBuffer();
		fp.append("<tr><td width=\"12\">&nbsp;</td>");
		fp.append("<td colspan=\"3\"><div style=\"float:right; font-family: Arial,Helvetica,sans-serif; font-size: 12px;\">");
		fp.append("<a href=\"http://goo.gl/khyTU\" style=\"text-decoration: none;\"><font color=\"#006c56\">www.epiducer.com</font></a></div><br />");
		fp.append("<div>");
		fp.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		fp.append("<strong>SCS Systems&mdash;Indications for Use: Spinal cord stimulation as an aid in the management of chronic, intractable pain of the trunk and limbs. Contraindications: Demand-type cardiac pacemakers, patients who are unable to operate the system or who fail to receive effective pain relief during trial stimulation. Warnings/Precautions: Diathermy therapy, cardioverter defibrillators, magnetic resonance imaging (MRI), explosive or flammable gases, theft detectors and metal screening devices, lead movement, operation of machinery and equipment, postural changes, pediatric use, pregnancy, and case damage. Patients who are poor surgical risks, with multiple illnesses, or with active general infections should not be implanted. Adverse Effects: Painful stimulation, loss of pain relief, surgical risks (e.g., paralysis).</strong></p>");
		fp.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		fp.append("<strong>Epiducer System&mdash;Intended Use: The Epiducer lead delivery system is intended to introduce Lamitrode S-Series paddle leads and/or St. Jude Medical Neuromodulation Division percutaneous leads into the epidural space at or below the L1 vertebra. Warnings: Patients who have a large amount of epidural scarring and/or adhesions may not be good candidates for the Epiducer system. Prior to surgery, detailed imaging should be used to assess the size of the epidural space. The Epiducer system should not be inserted into the epidural space superior to the L1 vertebra. Patients with a spinal cord that extends past the L1 vertebra may require a lower insertion level. Adverse Effects: Spinal cord compression and/or injury, epidural hemorrhage or hematoma, paralysis.</strong></p>");
		fp.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		fp.append("<strong>Clinician&rsquo;s Manual must be reviewed prior to use for detailed disclosure. Rx only.</strong></p>");
		fp.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		fp.append("<strong>Epiducer and S-Series are trademarks of Advanced Neuromodulation Systems, Inc. d/b/a St. Jude Medical Neuromodulation Division. ST. JUDE MEDICAL, the nine-squares symbol, and MORE CONTROL. LESS RISK. are trademarks and service marks of St. Jude Medical, Inc. and its related companies. &copy;2011 St. Jude Medical Neuromodulation Division. All rights reserved.</strong></p>");
		fp.append("</div></td>");
		fp.append("<td width=\"12\">&nbsp;</td></tr>");
		fp.append("<tr><td width=\"12\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"16\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"12\">&nbsp;</td></tr>");
		return fp;
	}
	
	/**
	 * The opt-out text for each newsletter
	 * @param optOutEmail
	 * @return
	 */
	public StringBuffer getOptOutText(String optOutEmail) {
		StringBuffer wrapper = new StringBuffer();
		wrapper.append("<tr><td width=\"12\">&nbsp;</td>");
		wrapper.append("<td colspan=\"3\">");
		wrapper.append("<div>");
		wrapper.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">");
		wrapper.append("You have received this email communication on behalf of St. Jude Medical, which is solely responsible for its content.  ");
		wrapper.append("6901 Preston Rd Plano TX 75024</p>");
		wrapper.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px;\">If you are no longer interested in receiving these communications, please ");
		wrapper.append("<a href=\"http://www.epiducer.com/sb/emailPermissions?emailAddress=");
		wrapper.append(optOutEmail);
		wrapper.append("&organizationId=ANS-MEDICAL\">click here to opt out</a>.</p>");
		wrapper.append("</div></td>");
		wrapper.append("<td width=\"12\">&nbsp;</td></tr>");
		wrapper.append("<tr><td width=\"12\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"16\">&nbsp;</td><td width=\"330\">&nbsp;</td><td width=\"12\">&nbsp;</td></tr>");
		wrapper.append("</tbody>");
		wrapper.append("</table>");
		return wrapper;
	}
	
}
