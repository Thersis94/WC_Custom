package com.venture.cs.message;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 *<b>Title</b>: CaseShareMessage<p/>
 *Builds the message body for a 'share case' activity message.
 *Copyright: Copyright (c) 2014<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 12, 2014
 * Changes:
 * Mar 12, 2014: DBargerhuff: created class
 ****************************************************************************/

public class CaseShareMessage extends AbstractCaseMessage {

	/**
	 * Builds message body
	 */
	public String getMessageBodyHTML() {
		StringBuilder body = new StringBuilder();
		body.append("<p>");
		UserDataVO user = (UserDataVO) req.getAttribute(Constants.USER_DATA);
		body.append(user.getFirstName()).append(" ").append(user.getLastName()).append(" has shared the case for vehicle ");
		body.append(vehicles.get(0).getVin()).append(" with you.");
		body.append("<br/><br/>");
		body.append("<a href='http:/").append(caseUrl.toString()).append("'>http://").append(caseUrl.toString()).append("</a>");
		body.append("<br/><br/>").append(req.getParameter("comment")).append("<br/><br/>");
		body.append("This is an automated message.  Please do not respond.");
		body.append("</p>");
		return body.toString();
	}
	
	/**
	 * Builds message body as text, defaults to HTML message
	 */
	public String getMessageBodyText() {
		StringBuilder body = new StringBuilder();
		UserDataVO user = (UserDataVO) req.getAttribute(Constants.USER_DATA);
		body.append(user.getFirstName()).append(" ").append(user.getLastName()).append(" has shared the case for vehicle ");
		body.append(vehicles.get(0).getVin()).append(" with you.");
		body.append("\n\n").append(req.getParameter("comment"));
		body.append("\n\n");
		body.append("This is an automated message.  Please do not respond.");
		return body.toString();
	}

	/**
	 * Builds the message subject.
	 */
	public String getMessageSubject() {
		return "A Venture Vehicle case has been shared with you";
	};

	
}
