package com.depuysynthesinst.emails;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: RegChiefEligibleVO.java<p/>
 * <b>Description: Registration confirmation email for chief residents in eligible programs.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class RegChiefEligibleVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 194411660022044144L;
	
	public RegChiefEligibleVO() {
		super();
	}

	
	/**
	 * add a method to build the message as its passed into the VO, not as it's sent (e.g.: getHtmlBody()).
	 * This is important, because when this message gets to JMS it won't have access
	 * to the Assg or SiteVO to do what it needs to do.
	 */
	public void buildMessage(UserDataVO rcpt, SiteVO site) {
		String siteUrl = site.getFullSiteAlias();
		String assgUrl = siteUrl + "/assignments";

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<p>Dear ").append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName()).append(",<br>");
		sb.append("Thank you for registering to the DePuy Synthes Future Leaders online educational program.</p>");
		
		sb.append("<p>You now have <u>access to view</u> the educational modules and online resources that ");
		sb.append("are available on our site based upon your selected profession.  As a chief resident you ");
		sb.append("have <u>access to assigning</u> relevant content to the registered residents in your residency ");
		sb.append("program who have accepted an invitation to participate in an online group on the site.<br/>");
		sb.append("To get started click on the URL below.<br/>");
		sb.append("<a href=\"").append(assgUrl).append("\">").append(assgUrl).append("</a></p>");
		
		sb.append("<p><b><u>PLEASE NOTE:</u></b> You will not be able to redeem credits earned during ");
		sb.append("this review period. You will receive further communication via email within 1-2 business days.  ");
		sb.append("If you have any questions please feel free to contact us at any time at ");
		sb.append("<a href=\"mailto:futureleaders@its.jnj.com\">futureleaders@its.jnj.com</a>.</p>");
		
		//addIfYouBelieve(sb);

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108i 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Registration confirmed! DePuy Synthes Future Leaders program");
	}
}
