package com.fastsigns.action.saf;

import java.util.Map;

import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;

/****************************************************************************
 * <b>Title</b>: GBSAFConfig.java<p/>
 * <b>Description: SAF config for Fastsigns UK/GB</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 12, 2012
 ****************************************************************************/
public class GBSAFConfig extends SAFConfig {

	public GBSAFConfig() {
		this.countryCode = "GB";
		this.postbackDomain = "www.fastsigns.co.uk";
		this.sendingFilesNowFieldId = "0a00141332b00d2be2485f2aa50bf899";
		this.transactionStageFieldId = "0a00141332b00d27e2485f2affa9bf19";
		this.filesFieldId = "0a00141332b00d26e2485f2a1e2eeb83";
		this.statusFieldId = "0a00141332b00d26e2485f2a5b99dabd";
		this.contactUsActionId = "0a00141332afb0b46020f5cb2ff87b5e";
		this.signTypeId = "0a00141332b00d2de2485f2a51956ce4";
		this.companyId = "0a00141332b00d2be2485f2abfbe3ec";
		this.faxId = "0a00141332b00d25e2485f2a33ca8afb";
		this.requestedCompletionDateId = "0a00141332b00d2be2485f2a77acf613";
		this.signQuantityId = "0a00141332b00d2de2485f2a74542af6";
		this.desiredHeightId = "0a00141332b00d2be2485f2a8a5aad7";
		this.desiredWidthId = "0a00141332b00d2be2485f2a4b0443c6";
		this.projectDescriptionId = "0a00141332b00d2be2485f2a74ea4356";
		this.salesContactId = "0a00141332b00d25e2485f2accbab9be";
	}

	/* (non-Javadoc)
	 * @see com.fastsigns.action.saf.SAFConfig#buildEmail(boolean, com.smt.sitebuilder.action.contact.ContactDataContainer)
	 */
	@Override
	public String buildEmail(boolean isDealer, ContactDataContainer cdc, Map<String, String> vals) {
		StringBuilder msg = new StringBuilder(1000);
		ContactDataModuleVO record = cdc.getData().get(0);
		String name = StringUtil.checkVal(record.getFirstName()) + " " + StringUtil.checkVal(record.getLastName());
		String dealerLink = "http://" + getPostbackDomain() + "/" + vals.get("aliasPath");
		if (isDealer) {
			msg.append("<p><b>Reminder: Do Not click reply on this email, it is sent to you from a do not reply email box.</b></p>");
			msg.append("<p>This message is to advise you that you have received a Request a Quote and/or a file.<br/>");
			msg.append("Please use the contact information provided in the form to response to the customer or prospect.</p>");
			msg.append("<p>Thank you.</p>");
		} else {
			msg.append("<p>Dear ").append(name).append(",<br/>Thank you for contacting ");
			msg.append(vals.get("fastsignsTxt")).append("&reg; of ").append(vals.get("locationName")).append(". This email serves to confirm ");
			msg.append("that we have received the following information from you.<br/>If you have any ");
			msg.append("questions please contact us at ").append(vals.get("phoneNumber")).append(" or ");
			msg.append(vals.get("dealerEmail")).append("<br/>Thank you,<br/><a href='").append(dealerLink).append("'>");
			msg.append(vals.get("fastsignsTxt")).append("&reg; of ");
			msg.append(vals.get("locationName")).append("</a><br/>More than fast. More than signs.&trade;");
			
		}
		msg.append("<table style=\"width:750px;border:solid 1px black;\">");

		
		msg.append("<tr style=\"background:#c0d2ec;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">Name</td><td valign=\"top\">").append(name).append("</td></tr>");
		msg.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">Email</td><td valign=\"top\">").append(StringUtil.checkVal(record.getEmailAddress())).append("</td></tr>");
		msg.append("<tr style=\"background:#c0d2ec;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">Phone Number</td><td valign=\"top\">").append(StringUtil.checkVal(new PhoneNumberFormat(record.getMainPhone(), this.countryCode, PhoneNumberFormat.NATIONAL_FORMAT).getFormattedNumber())).append("</td></tr>");
		msg.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">Address</td><td valign=\"top\">").append(StringUtil.checkVal(record.getAddress())).append("</td></tr>");
		msg.append("<tr style=\"background:#c0d2ec;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">Address2</td><td valign=\"top\">").append(StringUtil.checkVal(record.getAddress2())).append("</td></tr>");
		msg.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">City</td><td valign=\"top\">").append(StringUtil.checkVal(record.getCity())).append("</td></tr>");
		msg.append("<tr style=\"background:#c0d2ec;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">State</td><td valign=\"top\">").append(StringUtil.checkVal(record.getState())).append("</td></tr>");
		msg.append("<tr style=\"background:#E1EAFE;\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">Zip/Postal Code</td><td valign=\"top\">").append(StringUtil.checkVal(record.getZipCode())).append("</td></tr>");
		
		int x=0;
		Boolean sendingFiles = Boolean.FALSE;
		for (String key : cdc.getFields().keySet()) {
			//prohibit certain info from the emails
			if (this.getTransactionStageFieldId().equalsIgnoreCase(key))
				continue;
			
			String val = StringUtil.checkVal(record.getExtData().get(key));
			String color =  ((x++ % 2) == 0) ? "#c0d2ec" : "#E1EAFE";
			String questionNm = cdc.getFields().get(key);

			//HTML-format the files for clean display in the email
			if (this.getFilesFieldId().equalsIgnoreCase(key)) {
				try {
					StringBuilder o = new StringBuilder();
					String[] files = val.split("\r\n");
					for (String f : files) {
						log.debug("fileName=" + f);
						if (o.length() > 0) o.append("<br/><br/>");
						String[] tokens = f.split(",");
						if (tokens.length < 2) {
							//undecipherable array, append and continue
							o.append(StringUtil.getToString(tokens, false, false, ","));
							continue;
						}
						tokens[0] = StringUtil.replace(tokens[0].trim(), "%2C", ",");
						log.debug("fileName2=" + tokens[0]);
						tokens[1] = tokens[1].trim();
						o.append(tokens[0]).append("<br/>");
						o.append("<a href=\"").append(tokens[1]).append("\">").append(tokens[1]).append("</a>");
					}
					
					val = o.toString();
				} catch (Exception e) {
					log.error("could not HTML-format files for " + record.getContactSubmittalId());
				} finally {
					//draw attention to this section of the email if no files were received 
					// and the user indicated they were sending them.
					if (sendingFiles && val.length() == 0) {
						val = "No Files Received";
						color = "yellow";
					}
				}
				
			} else if (this.getSendingFilesNowFieldId().equalsIgnoreCase(key)) {
				sendingFiles = Boolean.TRUE;
			}
			
			msg.append("<tr style=\"background:").append(color);
			msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">").append(questionNm);
			msg.append("</td><td valign=\"top\">").append(val).append("</td></tr>");
		}
		
		msg.append("</table><br/>");
		return msg.toString();
	}

	/* (non-Javadoc)
	 * @see com.fastsigns.action.saf.SAFConfig#emailSubjectCenter()
	 */
	@Override
	public String getEmailSubjectCenter(String emailAddress) {
		return "SAF Completed: " + emailAddress;
	}

	/* (non-Javadoc)
	 * @see com.fastsigns.action.saf.SAFConfig#emailSubjectUser()
	 */
	@Override
	public String getEmailSubjectUser() {
		return "Your request has been delivered to FASTSIGNS";
	}

}
