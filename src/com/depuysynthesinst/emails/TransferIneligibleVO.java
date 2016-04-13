package com.depuysynthesinst.emails;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: TransferIneligibleVO.java<p/>
 * <b>Description: Registration confirmation email for residents & fellows in ineligible programs TRANSFERING.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class TransferIneligibleVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 1944672222006544L;
	
	public TransferIneligibleVO() {
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
		sb.append("Thank you again for registering for the DePuy Synthes Future Leaders program.  ");
		sb.append("At this time we are unable to grant you access to the Future Leaders Redemption ");
		sb.append("Center. Please contact us at <a href=\"mailto:futureleaders@its.jnj.com\">futureleaders@its.jnj.com</a> ");
		sb.append("for more information regarding your account.</p>");
		
		addThankYou(sb);

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108g 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Validation process complete: DePuy Synthes Future Leaders Program");
	}
}
