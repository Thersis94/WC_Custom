package com.ansmed.sb.physician;

// JDK 1.6
import java.util.GregorianCalendar;
import java.util.Locale;

// SMT Baselibs 2.0
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: EpiducerInvitationVO.java<p/>
 * <b>Description: </b><p>Contains and provides product training invitation text for use in formatting 
 * and sending product training invitations.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since June 09, 2011
 ****************************************************************************/
public class EpiducerInvitationVO {
	
	private SurgeonVO surgeon;
	private StringBuffer invitation;
	
	public EpiducerInvitationVO() {}
	
	public EpiducerInvitationVO(SurgeonVO surgeon) {
		this.surgeon = surgeon;
	}
	
	public StringBuffer getEpiducerInvitation(SurgeonVO surgeon) {
		StringBuffer invite = new StringBuffer();
		invite.append("<div align=\"center\">");
		invite.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\">");
		invite.append("<tbody>");
		invite.append("<tr><td colspan=\"3\"><img height=\"183\" src=\"http://www.poweroveryourpain.com/binary/org/ANS-MEDICAL/images/epiducer-email-header.png\" width=\"600\" /></td></tr>");
		invite.append("<tr style=\"height:3px\"><td style=\"height:3px\" width=\"10\">&nbsp;</td><td style=\"height:3px\" width=\"580\">&nbsp;</td><td style=\"height:3px\" width=\"10\">&nbsp;</td></tr>");
		invite.append("<tr><td width=\"10\">&nbsp;</td>");
		invite.append("<td align=\"left\" width=\"580\">");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Dear Dr. <strong>").append(surgeon.getFirstName()).append(" ").append(surgeon.getLastName()).append("</strong>:</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("St. Jude Medical Neuromodulation Division cordially invites you to attend the <strong>Epiducer lead delivery system</strong> ");
		invite.append("hands-on product training workshop.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("The Epiducer lead delivery system is a revolutionary device that will enable multiple options with one needle stick, including the ");
		invite.append("introduction of up to three percutaneous leads or the percutaneous introduction of a slimline paddle lead in combination with two ");
		invite.append("percutaneous leads. This interactive, hands-on training will provide participants with the opportunity to learn about the device ");
		invite.append("from faculty implanters experienced with its use. The course agenda includes, but is not limited to, the following:</p>");
		invite.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("<li>Product features and benefits</li>");
		invite.append("<li>Clinical implantation technique</li>");
		invite.append("<li>Hands-on cadaver lab</li>");
		invite.append("<li>Clinical research summary</li>");
		invite.append("<li>Epiducer system case studies</li>");
		invite.append("<li>Practical application of the Epiducer system in your neuromodulation practice</li>");
		invite.append("<li>Reimbursement</li>");
		invite.append("</ul>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Please visit <font color=\"#006c56\">www.epiducer.com/</font><strong>(");
		invite.append(getGroupUrl(surgeon)).append(")</strong> &nbsp;to view the available course dates and locations. ");
		invite.append("We request that you respond to this invitation no later than ");
		invite.append(getReplyByDate()).append(" ");
		invite.append("to ensure the course. These courses have a limited number of spaces and are by invitation "); 
		invite.append("only. St. Jude Medical will cover the cost of your hotel stay and a round-trip coach airfare. All travel must be arranged by Plaza Travel in accordance with ");
		invite.append("our travel and expense policy. If you have any questions, please contact the St. Jude Medical Professional Education Team at 877-756-6441. I look forward ");
		invite.append("to your participation at this meeting.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 14px\">");
		invite.append("Sincerely,<br />");
		invite.append("<strong>Chris Chavez or Tom Hickman</strong></p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		invite.append("<strong>Our Commitment to Excellence and Integrity </strong><br />");
		invite.append("St. Jude Medical has adopted the AdvaMed Code and adheres to all laws, rules, and regulations regarding appropriate interactions with healthcare professionals. ");
		invite.append("As part of this commitment to ethical behavior, we provide the following information:</p>");
		invite.append("<ul style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		invite.append("<li>As a registered participant, you must attend the entire course. If you fail to do so, you will be required to pay or reimburse us for your hotel and travel expenditures.</li>");
		invite.append("<li>St. Jude Medical cannot extend business courtesies to persons who do not have a legitimate business reason to attend the event, including spouses and significant others.</li>");
		invite.append("<li>If you change your approved travel itinerary, you may incur the costs associated with rebooking.</li>");
		invite.append("<li>St. Jude Medical may only provide you meals associated with this educational meeting. You are responsible for any meals or snacks that St. Jude Medical has not coordinated.</li>");
		invite.append("<li>For participants, St. Jude Medical will arrange pre-paid ground transportation from the airport to the hotel, from the hotel to the training facility, and from the training facility to the airport. You are responsible for any additional ground transportation costs.</li>");
		invite.append("<li>St. Jude Medical will not reimburse you for baggage fees, extended stay costs, personal expenses, gifts, or entertainment costs.</li>");
		invite.append("</ul>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		invite.append("<strong>Notice for Customers, Potential Customers, and Respective Associates and Agents Licensed to Practice Medicine in the Commonwealth of Massachusetts</strong><br />");
		invite.append("St. Jude Medical agrees to pay for reasonable expenses (including travel, meals, and lodging) associated with the product demonstration and training you will ");
		invite.append("receive at this Epiducer System Training workshop and lab. Please be aware that Massachusetts law requires St. Jude Medical to publicly disclose the amount of such ");
		invite.append("payments and the identity of the recipients annually.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		invite.append("By attending this training event, you acknowledge that you or a facility with which you are associated either has an existing agreement to purchase product with ");
		invite.append("St. Jude Medical or is considering entering into an agreement to purchase product after evaluating the appropriate use and functionality of the product. ");
		invite.append("Massachusetts law requires this in order for our company to pay the training costs of healthcare practitioners licensed in Massachusetts.</p>");
		invite.append("<p style=\"font-family: Arial,Helvetica,sans-serif; font-size: 10px; color:#666666\">");
		invite.append("<strong>Notice for Healthcare Professionals Licensed to Practice Medicine in the State of Vermont</strong><br />");
		invite.append("St. Jude Medical agrees to pay for reasonable expenses (including travel, meals, and lodging) associated with the product training you will receive at this ");
		invite.append("Epiducer System Training workshop and lab. Vermont law requires St. Jude Medical to publicly disclose the amount of such payments and the identity of the ");
		invite.append("recipients annually.</p>");
		invite.append("<br /><td width=\"10\">&nbsp;</td></tr>");
		invite.append("<tr><td style=\"height:10px\" width=\"10\">&nbsp;</td>");
		invite.append("<td style=\"height:10px\" width=\"580\">&nbsp;</td>");
		invite.append("<td style=\"height:10px\" width=\"10\">&nbsp;</td></tr></tbody></table>");
		invite.append("</div><p class=\"footer\">You have received this email communication on behalf of St. Jude Medical, which is solely responsible for its content.  ");
		invite.append("6901 Preston Rd Plano TX 75024</p><p class=\"footer\">If you are no longer interested in receiving these communications, please ");
		invite.append("<a href=\"http://www.poweroveryourpain.com/sb/emailPermissions?emailAddress=john.astorga@sjmneuro.com&organizationId=ANS-MEDICAL\">click here to opt out</a>.</p><img src=\"http://www.poweroveryourpain.com/sb/emailCampaignLink?log=TEST\" width=\"1\" height=\"1\" alt=\"\" />");
		return invite;
	}
	
	private String getGroupUrl(SurgeonVO surgeon) {
		String url = "#";
		Integer type = Convert.formatInteger(surgeon.getSurgeonTypeId());
		switch(type) {
			case 0:
				switch(surgeon.getProductGroupNumber()) {
					case 1:
						url = "http://www.epiducer.com/na";
						break;
					case 2:
						url = "http://www.epiducer.com/nb";
						break;
					case 3:
						url = "http://www.epiducer.com/nc";
						break;
				}
				break; // outer switch, case 0
			case 1:
				switch(surgeon.getProductGroupNumber()) {
					case 1:
						url = "http://www.epiducer.com/fa";
						break;
					case 2:
						url = "http://www.epiducer.com/fb";
						break;
					case 3:
						url = "http://www.epiducer.com/fc";
						break;
				}
				break; // outer switch, case 1
		}
		return url;
	}

	public SurgeonVO getSurgeon() {
		return surgeon;
	}

	public void setSurgeon(SurgeonVO surgeon) {
		this.surgeon = surgeon;
	}

	public StringBuffer getInvitation() {
		return invitation;
	}

	public void setInvitation(StringBuffer invitation) {
		this.invitation = invitation;
	}
	
	public String getReplyByDate() {
		GregorianCalendar gCal = new GregorianCalendar();
		// add 7 days to the current date
		gCal.add(GregorianCalendar.DAY_OF_MONTH, 7);
		// return the date in text format (e.g. June 7, 2011)
		StringBuffer s = new StringBuffer();
		s.append(gCal.getDisplayName(GregorianCalendar.MONTH, 2, new Locale("en")));
		s.append(" ");
		s.append(gCal.get(GregorianCalendar.DAY_OF_MONTH));
		s.append(", ");
		s.append(gCal.get(GregorianCalendar.YEAR));
		return s.toString();
	}

}
