package com.depuysynthesinst.emails;

import com.depuysynthesinst.assg.AssignmentVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: AssgPublishVO.java<p/>
 * <b>Description: Email message for when a Director first publishes an assignment to his students.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 22, 2015
 ****************************************************************************/
public class InviteResidentVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 1L;
	
	public InviteResidentVO() {
		super();
	}

	
	/**
	 * add a method to build the message as its passed into the VO, not as it's sent (e.g.: getHtmlBody()).
	 * This is important, because when this message gets to JMS it won't have access
	 * to the Assg or SiteVO to do what it needs to do.
	 */
	public void buildMessage(UserDataVO rcpt, UserDataVO resDir, SiteVO site) {
		String siteUrl = site.getFullSiteAlias();
		String profileUrl = siteUrl + "/profile";

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<p>Dear ").append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName()).append(",<br>");
		sb.append(resDir.getFirstName()).append(" ").append(resDir.getLastName());
		sb.append(" has invited you to participate in his/her residents group in the DePuy Synthes Future Leaders program.</p>");
		sb.append("<p>To accept or decline this invitation please click on the URL below and sign-in to your account:<br/>");
		sb.append("<a href=\"").append(profileUrl).append("\">").append(profileUrl).append("</a></p>");

		sb.append("<p>By accepting this invitation you are giving permission to the program director, coordinator ");
		sb.append("and/or chief resident in your residency program to assign relevant educational content from the site.</p>");

		sb.append("<p>You may change these permissions at any time by visiting ");
		sb.append("<a href=\"").append(profileUrl).append("\">My Profile</a>.</p>");
		
		addIfYouBelieve(sb);

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108c 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("You have been invited! DePuy Synthes Future Leaders Program");
	}


	/* (non-Javadoc)
	 * @see com.depuysynthesinst.emails.AbstractDSIEmailVO#buildMessage(com.siliconmtn.security.UserDataVO, com.depuysynthesinst.assg.AssignmentVO, com.smt.sitebuilder.common.SiteVO)
	 * This method satisfies an interface requirement and is not directly invoked.  -JM 07.23.15
	 */
	@Override
	public void buildMessage(UserDataVO rcpt, AssignmentVO assg, SiteVO site) {
		buildMessage(rcpt, assg.getDirectorProfile(), site);
	}
}
