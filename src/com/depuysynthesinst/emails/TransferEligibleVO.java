package com.depuysynthesinst.emails;

import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: TransferEligibleVO.java<p/>
 * <b>Description: Registration confirmation email for residents & fellows in eligible programs TRANSFERING.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class TransferEligibleVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 19876544L;
	
	public TransferEligibleVO() {
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
		sb.append("Thank you for registering for the DePuy Synthes Future Leaders program.  You now have full ");
		sb.append("access to the site: inclusive of educational content and access to the Future Leaders Redemption ");
		sb.append("Center in which you may redeem your credits for educational items.</p>");
		
		sb.append("<p><b><u>PLEASE NOTE:</u></b> The credit limit for redemption is $500 per year. ");
		sb.append("If you believe you received this email in error or if you have any questions please feel free ");
		sb.append("to contact us at <a href=\"mailto:futureleaders@its.jnj.com\">futureleaders@its.jnj.com</a>.</p>");
		
		addThankYou(sb);

		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108f 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Validation process complete: DePuy Synthes Future Leaders Program");
	}
}
