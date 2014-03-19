package com.venture.cs.message;

// SMTBaseLibs 2.0
import com.siliconmtn.util.Convert;

// WC_Custom
import com.venture.cs.action.vo.ActivityVO;
import com.venture.cs.action.vo.VehicleVO;


/****************************************************************************
 *<b>Title</b>: CaseFollowerMessage<p/>
 *Builds the message body for a case activity message for followers of a case.
 *Copyright: Copyright (c) 2014<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 12, 2014
 * Changes:
 * Mar 12, 2014: DBargerhuff: created class
 ****************************************************************************/

public class CaseFollowerMessage extends AbstractCaseMessage {

	/**
	 * Builds message body as HTML
	 */
	public String getMessageBodyHTML() {
		StringBuilder body = new StringBuilder();
		
		body.append("<p>");
		body.append("Activity has taken place for one or more of the vehicle cases you are following: ");
		body.append("</p>");
		
		// loop vehicles
		body.append("<table style=\"width: 500px; border: solid 1px black; border-collapse: collapse;\">");
		for (VehicleVO v : vehicles) {
			body.append(addVehicleHeaderRow(v));
			body.append("<tr style='background-color: #14548F;'>");
			body.append("<td colspan='3' style='color: #fff; text-align: center;'>").append(v.getVin()).append("</td><tr>");
			body.append("<tr style='background-color: #14548F;'>");
			body.append("<td style='width: 150px; color: #fff; text-align: center;'>Date</td>");
			body.append("<td style='color: #fff; text-align: center;'>Activity</td>");
			body.append("<td style='color: #fff; text-align: center;'>Submitted By</td></tr>");
			// loop activities
			String rowColor = "#fff";
			int row = 0;
			for (ActivityVO a : v.getActivity()) {
				row++;
				rowColor = (row % 2 == 0 ? "#aaa" : "#fff");
				body.append("<tr style='background-color: ").append(rowColor).append(";'>");
				body.append("<td style='width: 150px; text-align: center;'>");
				body.append(Convert.formatDate(a.getCreateDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
				body.append("</td>");
				body.append("<td style='text-align: center;'>").append(a.getComment()).append("</td>");
				body.append("<td style='text-align: center;'>").append(a.getLastName()).append(", ").append(a.getFirstName()).append("</td>");
				body.append("</tr>");
			}
			
		}
		body.append("</table>");
		return body.toString();
	}
	
	/**
	 * Builds message body as text 
	 */
	public String getMessageBodyText() {
		return getMessageBodyHTML();
	}

	/**
	 * Builds the message subject.
	 */
	public String getMessageSubject() {
		return "Venture Vehicle case activity notification";
	};

	
}
