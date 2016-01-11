package com.depuysynthesinst.emails;

import com.depuysynthesinst.assg.AssignmentVO;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: AbstractDSIEmailVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 23, 2015
 ****************************************************************************/
public abstract class AbstractDSIEmailVO extends EmailMessageVO {
	private static final long serialVersionUID = 4167406727059279508L;


	public AbstractDSIEmailVO() {
		super();
	}

	
	public void buildMessage(UserDataVO rcpt, AssignmentVO assg, SiteVO site) {
		//intended to be overwritten
	}
	
	public void buildMessage(UserDataVO rcpt, SiteVO site) {
		//intended to be overwritten
	}
	
	protected void addIfYouBelieve(StringBuilder sb) {
		sb.append("<p>If you believe you received this email in error or if you have any questions ");
		sb.append("please contact us at <a href=\"mailto:futureleaders@its.jnj.com\">futureleaders@its.jnj.com</a>.</p>");
	}
	
	protected void addThankYou(StringBuilder sb) {
		sb.append("<p>Thank you for your interest in DePuy Synthes Future Leaders program.</p>");
	}
	
	protected void addClosingRemark(StringBuilder sb, String siteUrl) {
		sb.append("<p>Sincerely,<br/>DePuy Synthes Institute<br/>donotreply@its.jnj.com</p>");

		sb.append("<p>Please do not reply to this e-mail as we will be unable to respond.  ");
		sb.append("If you would like to opt-out of receiving future e-mails, have any comments, ");
		sb.append("questions or general feedback, please e-mail ");
		sb.append("<a href=\"mailto:depuysynthesinstitute@its.jnj.com\">depuysynthesinstitute@its.jnj.com</a> ");
		sb.append("or write DePuy Synthes Institute, LLC, 325 Paramount Drive, Raynham, MA 02767.</p>");

		sb.append("<p>You may update your profile or communication preferences at any time by visiting ");
		sb.append("<a href=\"").append(siteUrl).append("\">").append(siteUrl).append("</a></p>");
	}
	
	protected void addTrackingNo(StringBuilder sb, String trn) {
		sb.append("<p style=\"font-size:9pt;\">").append(trn).append("</p>");
	}
}
