package com.depuysynthesinst.emails;

import com.depuysynthesinst.assg.AssignmentVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: AssgDeletedVO.java<p/>
 * <b>Description: Email message for when a Director deletes an assignment.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 23, 2015
 ****************************************************************************/
public class AssgDeletedVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 1143635654354L;

	public AssgDeletedVO() {
		super();
	}


	/**
	 * add a method to build the message as its passed into the VO, not as it's sent (e.g.: getHtmlBody()).
	 * This is important, because when this message gets to JMS it won't have access
	 * to the Assg or SiteVO to do what it needs to do.
	 */
	/* (non-Javadoc)
	 * @see com.depuysynthesinst.emails.AbstractDSIEmailVO#buildMessage(com.siliconmtn.security.UserDataVO, com.depuysynthesinst.assg.AssignmentVO, com.smt.sitebuilder.common.SiteVO)
	 */
	@Override
	public void buildMessage(UserDataVO rcpt, AssignmentVO assg, SiteVO site) {
		String siteUrl = site.getFullSiteAlias();
		String assgUrl = siteUrl + "/assignments";

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<p>Dear ").append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName()).append(",<br>");
		sb.append("Your residency director/coordinator (");
		sb.append(assg.getDirectorProfile().getFirstName()).append(" ").append(assg.getDirectorProfile().getLastName());
		sb.append(") has canceled an assignment in your DePuy Synthes Future Leaders account. ");
		sb.append("To access your assignments please click on the URL below:<br>");
		sb.append("<a href=\"").append(assgUrl).append("\">").append(assgUrl).append("</a></p>");

		addIfYouBelieve(sb);

		addThankYou(sb);

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0715/1154 07/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Your assignment has been changed: " + assg.getAssgName());
	}
}