package com.depuy.events_v2.vo.report;

import com.smt.sitebuilder.action.AbstractSBReportVO;


/***************************************************************************
 * <b>Title</b>: CPSEMReportVO.java<p/>
 * <b>Description: Simple data holder for the HTML version of the CPSEM compliance form,
 * which gets turned into a PDF dynamically and emailed out as an AbstractSBReportVO.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 31, 2014
 ***************************************************************************/
public class CPSEMReportVO extends AbstractSBReportVO {

	public static void main(String [] Args) {
		System.out.println(new String(new CPSEMReportVO().generateReport()));
	}

	private static final long serialVersionUID = -1902614724466857758L;

	@Override
	public byte[] generateReport() {
		StringBuilder sb = new StringBuilder();
		//Build Header
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html><head><title>Compliance Form</title></head><body>");
		sb.append("rev. 4/2/15");
		sb.append("<center>");
		sb.append("<h4><i>DePuy Synthes Joint Reconstruction</i><br/>");
		sb.append("Community Physician Education Seminar<br/>");
		sb.append("Compliance Acknowledgment<br/></h4>");
		sb.append("</center>");
		
		//Requirements Preamble
		sb.append("<p>To the best of my knowledge, the <i>DePuy Synthes Joint Reconstruction</i> Community Physician Education Seminar scheduled for ");
		sb.append("${eventDate} at ${eventLocation} will comply with the following requirements.</p>");
		
		//Paragraph 1
		sb.append("<br/><br/><u>Seminar Marketing:</u><br/>");
		sb.append("<ul><li>No modifications will be made to the seminar marketing invitation templates with the exception of adding the location, date, ");
		sb.append("time, reservation code and speaker information. Only company created and approved marketing collateral may be used.</li>");
		sb.append("<li>The speaker is not permitted to market or issue press releases about this seminar. (i.e. place a newspaper ad, or ");
		sb.append("send a press release to the local newspaper that markets the seminar).</li></ul>");
		
		//Paragraph 2
		sb.append("<br/><br/><u>The Speaker:</u><br/>");
		sb.append("<ul><li>To the best of my knowledge this person has not been asked to speak as a reward, or encouragement to use <i>DePuy Synthes ");
		sb.append("Joint Reconstruction</i> products.</li>");
		sb.append("<li>The speaker meets the requirements needed to speak at the seminar.</li>");
		sb.append("<li>The speaker will be required to sign a legal approved agreement sent to them by the Medical Affairs team at <i>DePuy ");
		sb.append("Synthes Joint Reconstruction</i>.</li>");
		sb.append("<li>The speaker will not make any modifications to the pre-approved presentation slide deck. The additional slides ");
		sb.append("within the deck (i.e. female knee, computer assisted surgery, Anterior Approach) may be used or not used, based on ");
		sb.append("relevance to the speaker's knowledge.<br/>");
			//subIndent
			sb.append("NOTE: The presentation slide deck will be provided to the speaker to prepare for the seminar, but the ");
			sb.append("presentation that will be used during the seminar will be supplied by the Seminar Coordinator the day of the seminar.</li>");
		sb.append("<li>The speaker will not market him/herself, his/her practice or his/her hospital during the seminar. For Example:<br/>");
			//subIndent
			sb.append("<ul><li>The speaker will not have practice/hospital brochures at the seminar.</li>");
			sb.append("<li>The speaker will not hand out business cards to attendees<br/>");
			sb.append("Note: The speaker may not hand out a business card, even if asked by an attendee. However, the ");
			sb.append("attendee may be directed to the <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list which will include ");
			sb.append("surgeons in the area who use <i>DePuy Synthes Joint Reconstruction</i> products.</li></ul></li>");
		
		sb.append("<li>The speaker can talk about the procedures and technologies that he/she uses, provided those uses are <b>not off-");
		sb.append("label</b> for our products.</li></ul>");
		
		//Paragraph 3
		sb.append("<br/><br/><u>The Seminar:</u><br/>");
		sb.append("<ul><li>If using a hospital location for the seminar venue the hospital may not be reimbursed for use of a conference room, nor for ");
		sb.append("food or catering services.</li>");
		sb.append("<li>During the opening remarks, the <i>DePuy Synthes Joint Reconstruction</i> representative may introduce the speaker by ");
		sb.append("his/her name and practice name. The introductory slides will include the speaker's name, practice name and bio. ");
		sb.append("The speaker's contact information may be added to the \"Questions\" slide at the end of the presentation in case there ");
		sb.append("are any questions about the presentation. These are the only designated areas on the presentation to identify the ");
		sb.append("speaker's practice.</li>");
		sb.append("<li>A <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list will be distributed to all attendees.</li>");
		sb.append("<li>No payment or reimbursement of expenses will be paid for the speaker (i.e. parking, mileage). The speaker ");
		sb.append("may partake in the refreshments provided at the seminar.</li>");
		sb.append("<li>A meal service will not be provided, but refreshments are acceptable. The total value of the refreshments will not exceed $50 ");
		sb.append("per person unless the event is being held at a Restaurant or Hotel, then the total value of refreshments is approved up to $125 per person.</li></ul>");
		
		//Footer
		sb.append("<br/><br/>");
		sb.append("Community Education Program Director's Signature: ${admSignature} Date: ${approvalDt}<br/>");
		sb.append("I have read and understand the requirements and have reviewed the Speaker Guideline document with the speaker.<br/>");
		sb.append("Seminar Coordinator's Name: ${ownerName}<br/>Territory #: ${territoryNo} <br/>");
		sb.append("Sales representative's Name: ${repName}<br/>");
		sb.append("</body></html>");
		return sb.toString().getBytes();
	}

	@Override
	public void setData(Object o) {
		// TODO Auto-generated method stub
		
	}

}
