package com.fastsigns.action.saf;

import java.util.Map;

import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;

/****************************************************************************
 * <b>Title</b>: SASAFConfig.java<p/>
 * <b>Description: SAF config for Fastsigns SA</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Sept 20, 2012
 ****************************************************************************/
public class SASAFConfig extends SAFConfig {

	public SASAFConfig() {
		this.countryCode = "SA";
		this.postbackDomain = "www.fastsignssa.com";
		this.sendingFilesNowFieldId = "0a001413d979c50738f6a61159574403";
		this.transactionStageFieldId = "0a001413d979c50338f6a611e159153a";
		this.filesFieldId = "0a001413d979c50238f6a611c8093aea";
		this.statusFieldId = "0a001413d979c50238f6a6114e3abd1c";
		this.contactUsActionId = "0a001413d97974ef38f6a6119e1ecc68";
		this.signTypeId = "0a001413d979c50938f6a6119f5e0356";
		this.companyId = "0a001413d979c50838f6a611985c8a77";
		this.faxId = "0a001413d979c50138f6a611eaaf4ffd";
		this.requestedCompletionDateId = "0a001413d979c50838f6a6115e18f25";
		this.signQuantityId = "0a001413d979c50938f6a611c92d1cc4";
		this.desiredHeightId = "0a001413d979c50738f6a6111b519391";
		this.desiredWidthId = "0a001413d979c50838f6a6118ef132be";
		this.projectDescriptionId = "0a001413d979c50838f6a611354bd503";
		this.salesContactId = "0a001413d979c50138f6a6116a03ea57";
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
			msg.append("<p>This message is to advise you that a customer has filled out a ");
			msg.append("Request a Quote form or sent you a file.  ");
			msg.append("Please use the contact information provided by the customer in the form.  ");
			msg.append("Do not click reply to this message, it is sent to you from a  ");
			msg.append("do_not_reply@fastsigns email box and will not be received by the customer.</p>");
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
