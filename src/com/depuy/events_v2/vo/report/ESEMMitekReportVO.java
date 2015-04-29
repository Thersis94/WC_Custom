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
public class ESEMMitekReportVO extends AbstractSBReportVO {

	public static void main(String [] args) {
		System.out.println(new String(new ESEMMitekReportVO().generateReport()));
	}
	
	private static final long serialVersionUID = 8396753231530938375L;

	@Override
	public byte[] generateReport() {
		StringBuilder sb = new StringBuilder();
		//Build Header
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html><head><title>Compliance Form</title></head><body>");
		sb.append("rev. 4/2/15<br/>");
		sb.append("<center>");
		sb.append("<h4><i>DePuy Synthes Mitek Sports Medicine</i><br/>");
		sb.append("Patient Education Seminar<br/>");
		sb.append("Compliance Acknowledgment</h4>");
		sb.append("</center>");
		//Requirements Preamble
		sb.append("<p>To the best of my knowledge, the <i>DePuy Synthes Mitek Sports Medicine</i> Patient ");
		sb.append("Education Seminar scheduled for at ${eventDate} at ${eventLocation} will comply with the following requirements.</p>");
		//Paragraph 1
		sb.append("<br/><br/><u>Seminar Marketing:</u><br/>");
		sb.append("<ul><li>No modifications will be made to the seminar marketing templates with the exception of adding the location, date, time, ");
		sb.append("reservation code and speaker information. Only company created and approved marketing collateral may be used.</li>");
		sb.append("<li>The speaker may market the education seminar provided they use an approved venue and the pre-approved <i>DePuy ");
		sb.append("Synthes Joint Reconstruction</i> newspaper ad. No changes can be made to the pre-approved newspaper ad.</li>");
		sb.append("<li>Speaker may send the <i>DePuy Synthes Mitek Sports Medicine</i> pre-approved and pre-printed postcard invitations to ");
		sb.append("their own referral list. The speaker is responsible for ensuring that any such outreach to their own referral base ");
		sb.append("complies with the HIPAA law and all other applicable legal requirements.</li></ul>");
		//Paragraph 2
		sb.append("<br/><br/><u>The Speaker:</u><br/>");
		sb.append("<ul><li>To the best of my knowledge this person has not been asked to speak as a reward, or encouragement to use <i>DePuy Synthes ");
		sb.append("Joint Reconstruction</i> products.</li>");
		sb.append("<li>The speaker meets the requirements needed to speak at the seminar.</li>");
		sb.append("<li>The speaker will be required to sign a legal approved agreement sent to them by the Medical Affairs team at <i>DePuy ");
		sb.append("Synthes Joint Reconstruction</i>.</li>");
		sb.append("<li>The surgeon speaker will not make any modifications to the pre-approved presentation slide deck. The additional slides ");
		sb.append("within the deck may be used or not used, based on relevance to the speaker's knowledge.<br/>");
		sb.append("NOTE: The presentation slide deck will be provided to the speaker to prepare for the seminar, but the ");
		sb.append("presentation that will be used during the seminar will be supplied by the EISC the day of the seminar.</li>");
		sb.append("<li>The speaker can talk about the procedures and technologies that he/she uses, provided those uses are <b>not off-");
		sb.append("label</b> for our products.</li></ul>");
		//Paragraph 3
		sb.append("<br/><br/><u>The Seminar:</u><br/>");
		sb.append("<ul><li>If using a hospital location for the seminar venue the hospital may not be reimbursed for use of a conference room, nor for ");
		sb.append("food or catering services.</li>");
		sb.append("<li>During the opening remarks, the <i>DePuy Synthes Mitek Sports Medicine</i> representative may introduce the speaker by ");
		sb.append("his/her name and practice name. The introductory slides will include the speaker's name, practice name and bio. ");
		sb.append("The speaker's contact information may be added to the \"Questions\" slide at the end of the presentation in case there ");
		sb.append("are any questions about the presentation. These are the only designated areas on the presentation to identify the ");
		sb.append("speaker's practice.</li>");
		sb.append("<li>A <i>DePuy Synthes Mitek Sports Medicine</i> Surgeon Locator list will be distributed to all attendees.</li>");
		sb.append("<li>No payment or reimbursement of expenses will be paid for the speaker (i.e. parking, mileage). The speaker may ");
		sb.append("partake in the refreshments provided at the seminar.</li>");
		sb.append("<li>A meal service will not be provided, but refreshments such as cookies and coffee are acceptable. The refreshment spending ");
		sb.append("limit is $10 per attendee, which includes the give-away kit.</li></ul>");
		//Footer
		sb.append("<br/><br/>");
		sb.append("Area Marketing Director's Signature: ${admSignature} Date: ${approvalDt}<br/>");
		sb.append("I have read and understand the requirements and have reviewed the Speaker Guideline document with the speaker.<br/>");
		sb.append("Early Intervention Sales Consultant's Name: ${ownerName}<br/>");
		sb.append("Early Intervention Sales Manager's Name: ${repName}<br/>");
		sb.append("</body></html>");
		
		return sb.toString().getBytes();
	}

	@Override
	public void setData(Object o) {
	}

}
