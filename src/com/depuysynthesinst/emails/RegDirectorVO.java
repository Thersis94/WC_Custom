package com.depuysynthesinst.emails;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: RegDirectorVO.java<p/>
 * <b>Description: Registration confirmation email for directors.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class RegDirectorVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 19866798134476544L;
	
	public RegDirectorVO() {
		super();
	}

	
	/**
	 * add a method to build the message as its passed into the VO, not as it's sent (e.g.: getHtmlBody()).
	 * This is important, because when this message gets to JMS it won't have access
	 * to the Assg or SiteVO to do what it needs to do.
	 */
	public void buildMessage(UserDataVO rcpt, SiteVO site) {
		String siteUrl = site.getFullSiteAlias();

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<p>Dear ").append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName()).append(",<br>");
		sb.append("Thank you for registering for the DePuy Synthes Future Leaders program.</p>");
		
		sb.append("<p>You now have access to view differentiated educational content and resources available online.</p>");
		
		sb.append("<p>As a residency program director/coordinator you have the ability to <u>assign relevant ");
		sb.append("content</u> to the registered residents in your program who have accepted an invitation to ");
		sb.append("participate in your group.  To get started click on the URL below.<br/>");
		sb.append("<a href=\"").append(siteUrl).append("\">").append(siteUrl).append("</a></p>");
		
		sb.append("<p>If you have any questions please feel free to contact us at any time at ");
		sb.append("<a href=\"mailto:futureleaders@its.jnj.com\">futureleaders@its.jnj.com</a></p>");

		addIfYouBelieve(sb);
		
		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108h 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Registration confirmed! DePuy Synthes Future Leaders program");
	}
}
