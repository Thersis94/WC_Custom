package com.depuy.events_v2.vo.report;

import com.smt.sitebuilder.action.AbstractSBReportVO;

/***************************************************************************
 * <b>Title</b>: ESEMReportVO.java<p/>
 * <b>Description: Simple data holder for the HTML version of the ESEM compliance form,
 * which gets turned into a PDF dynamically and emailed out as an AbstractSBReportVO.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 31, 2014
 ***************************************************************************/
public class ESEMReportVO extends AbstractSBReportVO {

	public static void main(String [] args) {
		System.out.println(new String(new ESEMReportVO().generateReport()));
	}
	
	private static final long serialVersionUID = 8396753231530938375L;

	@Override
	public byte[] generateReport() {
		StringBuilder sb = new StringBuilder();
		//Build Header
		sb.append("<!DOCTYPE html><html><head></head><body>");
		sb.append("rev. 4/22/13<br/>");
		sb.append("<center>");
		sb.append("<h4><i>DePuy Synthes Joint Reconstruction</i><br/>");
		sb.append("Patient Education Seminar<br/>");
		sb.append("Compliance Acknowledgment</h4>");
		sb.append("</center>");
		//Requirements Preamble
		sb.append("<p>To the best of my knowledge, the <i>DePuy Synthes Joint Reconstruction</i> Patient ");
		sb.append("Education Seminar scheduled for at ${eventDate} at ${eventTime} will comply with the following requirements.</p>");
		//Paragraph 1
		sb.append("<br/><br/><u>Seminar Marketing:</u><br/>");
		sb.append("<ul><li>No modifications will be made to the seminar marketing templates with the exception of adding the location, date, time, ");
		sb.append("reservation code and speaker information. Only company created and approved marketing collateral may be used.</li>");
		sb.append("<li>The surgeon speaker may market the education seminar provided they use an approved venue and the pre-approved <i>DePuy ");
		sb.append("Synthes Joint Reconstruction</i> newspaper ad. No changes can be made to the pre-approved newspaper ad.</li>");
		sb.append("<li>Surgeon speaker may send the <i>DePuy Synthes Joint Reconstruction</i> pre-approved and pre-printed postcard invitations to ");
		sb.append("their own referral list. The surgeon speaker is responsible for ensuring that any such outreach to their own referral base ");
		sb.append("complies with the HIPAA law and all other applicable legal requirements.</li></ul>");
		//Paragraph 2
		sb.append("<br/><br/><u>The Speaker:</u><br/>");
		sb.append("<ul><li>To the best of my knowledge this person has not been asked to speak as a reward, or encouragement to use <i>DePuy Synthes ");
		sb.append("Joint Reconstruction</i> products.</li>");
		sb.append("<li>The surgeon speaker meets the requirements needed to speak at the seminar.</li>");
		sb.append("<li>The surgeon speaker will be required to sign a legal approved agreement sent to them by the Medical Affairs team at <i>DePuy ");
		sb.append("Synthes Joint Reconstruction</i>.</li>");
		sb.append("<li>The surgeon speaker will not make any modifications to the pre-approved presentation slide deck. The additional slides ");
		sb.append("within the deck (i.e. female knee, computer assisted surgery, Anterior Approach) may be used or not used, based on ");
		sb.append("relevance to the surgeon speaker's knowledge.<br/>");
		sb.append("NOTE: The presentation slide deck will be provided to the surgeon speaker to prepare for the seminar, but the ");
		sb.append("presentation that will be used during the seminar will be supplied by the Seminar Coordinator the day of the seminar.</li>");
		sb.append("<li>The speaker will not market him/herself, his/her practice or his/her hospital during the seminar. For Example:");
			//indented sub-list
			sb.append("<ul><li>The speaker will not book office appointments while at the seminar.</li>");
			sb.append("<li>The speaker will not have practice/hospital brochures at the seminar.</li>");
			sb.append("<li>The speaker will not hand out business cards to attendees<br/>");
			sb.append("Note: The speaker may not hand out a business card, even if asked by an attendee. However, the attendee may be ");
			sb.append("directed to the <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list which will include the speaking surgeon as ");
			sb.append("well as other surgeons in the area who use <i>DePuy Synthes Joint Reconstruction</i> products.</li></ul></li>");
		sb.append("<li>The surgeon speaker can talk about the procedures and technologies that he/she uses, provided those uses are <b>not off-");
		sb.append("label</b> for our products.</li></ul>");
		//Paragraph 3
		sb.append("<br/><br/><u>The Seminar:</u><br/>");
		sb.append("<ul><li>If using a hospital location for the seminar venue the hospital may not be reimbursed for use of a conference room, nor for ");
		sb.append("food or catering services.</li>");
		sb.append("<li>During the opening remarks, the <i>DePuy Synthes Joint Reconstruction</i> representative may introduce the surgeon speaker by ");
		sb.append("his/her name and practice name. The introductory slides will include the surgeon speaker's name, practice name and bio. ");
		sb.append("The surgeon speaker's contact information may be added to the \"Questions\" slide at the end of the presentation in case there ");
		sb.append("are any questions about the presentation. These are the only designated areas on the presentation to identify the surgeon ");
		sb.append("speaker's practice.</li>");
		sb.append("<li>A <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list will be distributed to all attendees.</li>");
		sb.append("<li>No payment or reimbursement of expenses will be paid for the speaker (i.e. parking, mileage). The surgeon speaker may ");
		sb.append("partake in the refreshments provided at the seminar.</li>");
		sb.append("<li>A meal service will not be provided, but refreshments such as cookies and coffee are acceptable. The refreshment spending ");
		sb.append("limit is $10 per attendee, which includes the $3 First Aid kit.</li></ul>");
		//Footer
		sb.append("<br/><br/>");
		sb.append("Area Marketing Director's Signature: ${admSignature} Date: ${approvalDt}<br/>");
		sb.append("I have read and understand the requirements and have reviewed the Surgeon Guideline document with the surgeon speaker.<br/>");
		sb.append("Seminar Coordinator's Name: ${ownerName} Territory #: ${territoryNo}<br/>");
		sb.append("Sales representative's Name: ${repName}<br/>");
		sb.append("</body></html>");
		
		return sb.toString().getBytes();
	}

	@Override
	public void setData(Object o) {
	}

}
