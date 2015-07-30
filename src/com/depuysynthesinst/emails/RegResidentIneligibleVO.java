package com.depuysynthesinst.emails;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: RegResidentIneligibleVO.java<p/>
 * <b>Description: Registration confirmation email for residents & fellows in ineligible programs.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class RegResidentIneligibleVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 1944672222006544L;
	
	public RegResidentIneligibleVO() {
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
		sb.append("Thank you for registering for the DePuy Synthes Future Leaders program.  We are ");
		sb.append("currently validating your account.  In the meantime you have full <u>access to view</u> all of the educational content.</p>");
		
		sb.append("<p><b><u>PLEASE NOTE:</u></b> You have indicated that you are in an academic ");
		sb.append("institution that is restricted from accepting value transfers from industry.  Thus, ");
		sb.append("you will not be able to earn credits and/or utilize the DePuy Synthes Institute redemption center.</p>");
		
		sb.append("<p>If you believe that you <u>are eligible</u> to use this feature in the program or if ");
		sb.append("you have any questions please feel free to contact us at any time at <br/>");
		sb.append("<a href=\"mailto:futureleaders@its.jnj.com\">futureleaders@its.jnj.com</a></p>");

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108e 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Registration confirmed! DePuy Synthes Future Leaders program");
	}
}
