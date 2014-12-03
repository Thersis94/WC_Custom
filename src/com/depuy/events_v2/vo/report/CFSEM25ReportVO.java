package com.depuy.events_v2.vo.report;

import com.smt.sitebuilder.action.AbstractSBReportVO;

/***************************************************************************
 * <b>Title</b>: CFSEMReportVO.java<p/>
 * <b>Description: Simple data holder for the HTML version of the CFSEM compliance form,
 * which gets turned into a PDF dynamically and emailed out as an AbstractSBReportVO.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 31, 2014
 ***************************************************************************/
public class CFSEM25ReportVO extends AbstractSBReportVO {
	public static void main(String [] Args) {
		System.out.println(new String(new CFSEM25ReportVO().generateReport()));
	}
	private static final long serialVersionUID = 2149353708238601389L;

	@Override
	public byte[] generateReport() {
		StringBuilder sb = new StringBuilder();
		//Build header
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html><head><title>Compliance Form</title></head><body>");
		sb.append("rev. 4/22/13");
		sb.append("<center>");
		sb.append("<h4><i>DePuy Synthes Joint Reconstruction</i><br/>");
		sb.append("Patient Education Seminar<br/>");
		sb.append("Compliance Acknowledgment<br/>");
		sb.append("(DePuySynthes Joint Reconstruction/Co-funded 50/25/25 Split)</h4>");
		sb.append("</center>");
		
		//Requirements preamble
		sb.append("<p>To the best of my knowledge, the <i>DePuy Synthes Joint Reconstruction</i> (Co-funded) Patient Education Seminar scheduled for ");
		sb.append("${eventDate} at ${eventLocation} will comply with the following requirements.</p>");
		
		//Paragraph 1
		sb.append("<br/><br/><u>Seminar Marketing:</u><br/>");
		sb.append("<ul><li>Surgeon speaker and hospital will be responsible for 25% of the cost of the newspaper ad and combined are responsible ");
		sb.append("for a $200 upfront fee covering postcards, flyers, posters, refreshments and venue rental fees. The surgeon speaker or hospital ");
		sb.append("will therefore have 25% of the real estate on the newspaper ad, postcards, flyers and posters. Seminar will be cancelled if ");
		sb.append("surgeon speaker does not approve the newspaper ad, agree to the related costs, provide credit card information to third party ");
		sb.append("agency for related costs when contacted by third party agency. </li>");
		sb.append("<li>The surgeon speaker may market the education seminar provided they use an approved venue and the pre-approved <i>DePuy ");
		sb.append("Synthes Joint Reconstruction</i> newspaper ad. No changes can be made to the pre-approved newspaper ad.</li>");
		sb.append("<li>No modifications will be made to the seminar marketing templates with the exception of adding the location, date, time, ");
		sb.append("reservation code and surgeon speaker information.</li>");
		sb.append("<li>Surgeon speakers may send the pre-approved and pre-printed postcard invitations to their own referral list. The surgeon is ");
		sb.append("responsible for ensuring that any such outreach to their own referral base complies with the \"HIPAA Act\" and all other ");
		sb.append("applicable legal requirements.</li></ul>");

		//Paragraph 2		
		sb.append("<br/><br/><u>The Speaker:</u><br/>");
		sb.append("<ul><li>To the best of my knowledge this person has not been asked to speak as a reward, or encouragement to use <i>DePuy Synthes ");
		sb.append("Joint Reconstruction</i> products.</li>");
		sb.append("<li>The surgeon speaker meets the requirements needed to speak at the seminar.</li>");
		sb.append("<li>The surgeon speaker will not make any modifications to the pre-approved presentation slide deck. The additional slides ");
		sb.append("within the deck (i.e. female knee, computer assisted surgery, Anterior Approach) may be used or not used, based on ");
		sb.append("relevance to the surgeon speaker's knowledge.<br/>");
			//indented sub list
			sb.append("NOTE: The presentation slide deck will be provided to the surgeon speaker to prepare for the seminar, but the ");
			sb.append("presentation that will be used during the seminar will be supplied by the Seminar Coordinator the day of the ");
			sb.append("seminar.</li>");
		sb.append("<li>The surgeon speaker can talk about the procedures and technologies that he/she uses, provided those uses are <b>not off-");
		sb.append("label</b> for our products.</li></ul>");

		//Paragraph 3
		sb.append("<br/><br/><u>The Seminar:</u><br/>");
		sb.append("<ul><li>If using a hospital location for the seminar venue the hospital may not be reimbursed for use of a conference room, nor for ");
		sb.append("food or catering services.</li>");
		sb.append("<li>During the opening remarks, the <i>DePuy Synthes Joint Reconstruction</i> representative may introduce the surgeon speaker by ");
		sb.append("his/her name and practice name. The introductory slides will include the surgeon speaker's name, practice name and bio. ");
		sb.append("The surgeon speaker's contact information may be added to the \"Questions\" slide at the end of the presentation in case there ");
		sb.append("are any questions about the presentation. </li>");
		sb.append("<li>A <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list will be distributed to all attendees.</li>");
		sb.append("<li>The surgeon speaker may distribute his business card, display practice brochures, as well as have staff at the seminar.</li>");
		sb.append("<li>No payment or reimbursement of expenses will be paid for the speaker (i.e. parking, mileage). The surgeon speaker ");
		sb.append("may enjoy the refreshments provided at the seminar.</li>");
		sb.append("<li>A meal service will not be provided, but refreshments such as cookies and coffee are acceptable. The refreshment spending ");
		sb.append("limit is $13 per attendee, which includes the $3 First Aid kit.</li></ul>");

		//Footer
		sb.append("<br/><br/>");
		sb.append("Community Education Program Director's Signature: ${admSignature} Date: ${approvalDt} <br/>");
		sb.append("I have read and understand the requirements and have reviewed the Surgeon Guideline document with the surgeon speaker.<br/>");
		sb.append("Seminar Coordinator's Name: ${ownerName}<br/>Territory #: ${territoryNo} <br/>");
		sb.append("Sales representative's Name: ${repName} <br/>");
		sb.append("</body></html>");
	
		return sb.toString().getBytes();
	}

	@Override
	public void setData(Object o) {
	}

}
