package com.depuysynthesinst.emails;

import com.depuysynthesinst.DSIUserDataVO;
import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: RegProfferVO.java<p/>
 * <b>Description: Registration email to the site admin when the user must submit a Proffer Letter.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 24, 2015
 ****************************************************************************/
public class RegProfferVO extends AbstractDSIEmailVO {
	private static final long serialVersionUID = 16544L;
	
	public RegProfferVO() {
		super();
	}

	
	/**
	 * add a method to build the message as its passed into the VO, not as it's sent (e.g.: getHtmlBody()).
	 * This is important, because when this message gets to JMS it won't have access
	 * to the Assg or SiteVO to do what it needs to do.
	 */
	public void buildMessage(DSIUserDataVO rcpt, SiteVO site) {
		String siteUrl = site.getFullSiteAlias();

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<p>Dear Future Leaders Program,<br>");
		sb.append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName());
		sb.append("has indicated during the registration process for the DePuy Synthes Future ");
		sb.append("Leaders program that he/she is working as a healthcare professional in a ");
		sb.append("Federal Military Hospital.  The following are the details of his/her registration:</p>");
		
		sb.append("<p>HCP Name: ").append(rcpt.getFirstName()).append(" ").append(rcpt.getLastName()).append("<br>");
		sb.append("Hospital Name: ").append(StringUtil.checkVal(rcpt.getAttribute(RegField.DSI_ACAD_NM.toString()))).append("<br>");
		sb.append("Email Address: ").append(rcpt.getEmailAddress()).append("<br>");
		sb.append("Profession: ").append(rcpt.getProfession()).append("</p>");
		
		sb.append("<p>Please follow up with the physician to process a proffer letter.</p>");
		
		addClosingRemark(sb, siteUrl);
		
		addTrackingNo(sb, "DSUS/INS/0615/1108k 06/15");

		super.setHtmlBody(sb.toString());
		super.setSubject("Military Hospital Account: Registration Pending / Proffer Letter Required?");
	}
}
