package com.depuy.events_v2.vo.report;

import com.smt.sitebuilder.action.AbstractSBReportVO;

public class CPSEMReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1902614724466857758L;

	@Override
	public byte[] generateReport() {
		StringBuilder sb = new StringBuilder();
		sb.append("<div id='wrapper'>");
		sb.append("<p id='date'>4/22/13</p>");
		
		sb.append("<div id='title'>");
		sb.append("<h4><i>DePuy Synthes Joint Reconstruction</i></br>");
		sb.append("Patient Education Seminar</br>");
		sb.append("Compliance Acknowledgment</br>");
		sb.append("(DePuySynthes Joint Reconstruction/Surgeon Co-funded)</h4>");
		sb.append("</div>");
		
		sb.append("<div id='requirements'>To the best of my knowledge, the <i>DePuy Synthes Joint Reconstruction Patient</i> (Co-funded) Patient Education Seminar scheduled for </br>");
		sb.append("at will comply with the following requirements.</div>");
		
		sb.append("<p class='sectionHeader'><u>Seminar Marketing:</u></p>");
		
		sb.append("<ul><li>No modifications will be made to the seminar marketing templates with the exception of adding the location, date, <br/>");
		sb.append("time, reservation code and speaker information. Only company created and approved marketing collateral may be </br>");
		sb.append("used.</li>");
		
		sb.append("<li>The surgeon speaker is not permitted to market or issue press releases about this seminar. (i.e. place a newspaper ad, or</br>");
		sb.append("send a press release to the local newspaper that markets the seminar).</li>");
		
		sb.append("<p class='sectionHeader'><u>The Speaker:</u></p>");
		
		sb.append("<ul><li>To the best of my knowledge this person has not been asked to speak as a reward, or encouragement to use <i>DePuy Synthes </br>");
		sb.append("Joint Reconstruction</i> products.</li>");
		
		sb.append("<li>The surgeon speaker meets the requirements needed to speak at the seminar.</li>");
		
		sb.append("<li>The surgeon speaker will be required to sign a legal approved agreement sent to them by the Medical Affairs team at <i>DePuy </br>");
		sb.append("Synthes Joint Reconstruction</i>.</li>");
		
		sb.append("<li>The surgeon speaker will not make any modifications to the pre-approved presentation slide deck. The additional slides </br>");
		sb.append("within the deck (i.e. female knee, computer assisted surgery, Anterior Approach) may be used or not used, based on </br>");
		sb.append("relevance to the surgeon speaker's knowledge.</li>");
		
		sb.append("<ul><li>NOTE: The presentation slide deck will be provided to the surgeon speaker to prepare for the seminar, but the </br>");
		sb.append("presentation that will be used during the seminar will be supplied by the Seminar Coordinator the day of the seminar.</li></ul>");
		
		sb.append("<li>The speaker will not market him/herself, his/her practice or his/her hospital during the seminar. For Example:</li>");
		
		sb.append("<li>The speaker will not have practice/hospital brochures at the seminar.</li>");
		
		sb.append("<li>The speaker will not hand out business cards to attendees</li>");
		
		sb.append("<ul><li>Note: The speaker may not hand out a business card, even if asked by an attendee. However, the attendee may be </br>");
		sb.append("directed to the <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list which will include the speaking surgeon as </br>");
		sb.append("well as other surgeons in the area who use <i>DePuy Synthes Joint Reconstruction</i> products.</li></ul></ul>");
		
		sb.append("<li>The surgeon speaker can talk about the procedures and technologies that he/she uses, provided those uses are <b>not off-</br>");
		sb.append("label</b> for our products.</li></ul>");
		
		sb.append("<p class='sectionHeader'><u>The Seminar:</u></p>");
		sb.append("<ul><li>If using a hospital location for the seminar venue the hospital may not be reimbursed for use of a conference room, nor for </br>");
		sb.append("food or catering services.</li>");
		sb.append("<li>During the opening remarks, the <i>DePuy Synthes Joint Reconstruction</i> representative may introduce the surgeon speaker by </br>");
		sb.append("his/her name and practice name. The introductory slides will include the surgeon speaker's name, practice name and bio. </br>");
		sb.append("The surgeon speaker's contact information may be added to the \"Questions\" slide at the end of the presentation in case there </br>");
		sb.append("are any questions about the presentation. These are the only designated areas on the presentation to identify the surgeon </br>");
		sb.append("speaker's practice.</li>");
		sb.append("<li>A <i>DePuy Synthes Joint Reconstruction</i> Surgeon Locator list will be distributed to all attendees.</li>");
		sb.append("<li>No payment or reimbursement of expenses will be paid for the speaker (i.e. parking, mileage). The surgeon speaker may </br>");
		sb.append("partake in the refreshments provided at the seminar.</li>");
		sb.append("<li>A meal service will not be provided, but refreshments are acceptable. The total value of the refreshments will not exceed $35</br>");
		sb.append("per person.</li></ul>");
		sb.append("<div id='acceptance'>");
		sb.append("Area Marketing Director's Signature: _______________________________________ Date: _______________</br>");
		sb.append("I have read and understand the requirements and have reviewed the Surgeon Guideline document with the surgeon speaker.</br>");
		sb.append("Seminar Coordinator's Name: Territory #:</br>");
		sb.append("Sales representative's Name:</br>");
		sb.append("</div>");
		sb.append("</div>");
		return sb.toString().getBytes();
	}

	@Override
	public void setData(Object o) {
		// TODO Auto-generated method stub
		
	}

}
