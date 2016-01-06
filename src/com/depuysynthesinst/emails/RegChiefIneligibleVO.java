package com.depuysynthesinst.emails;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: RegResidentIneligibleVO.java<p/>
 * <b>Description: Registration confirmation email for chief residents in ineligible programs.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class RegChiefIneligibleVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 194411660022044144L;
	
	public RegChiefIneligibleVO() {
		super();
	}

	
	/**
	 * add a method to build the message as its passed into the VO, not as it's sent (e.g.: getHtmlBody()).
	 * This is important, because when this message gets to JMS it won't have access
	 * to the Assg or SiteVO to do what it needs to do.
	 */
	public void buildMessage(UserDataVO rcpt, SiteVO site) {
		String siteUrl = site.getFullSiteAlias();
		String profileUrl = siteUrl + "/profile";
		String assgUrl = siteUrl + "/assignments";

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<p>Dear ").append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName()).append(",<br>");
		sb.append("Thank you for registering to the DePuy Synthes Future Leaders online educational program.  ");
		sb.append("You now have <u>access to view</u> the educational modules and online resources that are ");
		sb.append("available on our site based upon your selected profession.</p>");
		
		sb.append("<p>As a chief resident you have <u>access to assigning</u> relevant content to the ");
		sb.append("registered residents in your residency program who have accepted an invitation ");
		sb.append("to participate in an online group on the site.  To get started click on the URL below.<br/>");
		sb.append("<a href=\"").append(assgUrl).append("\">").append(assgUrl).append("</a></p>");
		
		sb.append("<p><b><u>PLEASE NOTE:</u></b> You have indicated that you are currently ");
		sb.append("attending a residency program that is not eligible to receive value transfers including ");
		sb.append("items in the <i>DePuy Synthes Institute, LLC</i> redemption center.  If this is not accurate ");
		sb.append("you may change it by accessing ");
		sb.append("<a href=\"").append(profileUrl).append("\">My Profile</a></p>");
		
		addIfYouBelieve(sb);

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108j 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Registration confirmed! DePuy Synthes Future Leaders program");
	}
}
