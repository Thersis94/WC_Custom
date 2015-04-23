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
public class AESAFConfig extends SAFConfig {

	public AESAFConfig() {
		this.countryCode = "AE";
		this.postbackDomain = "www.fastsigns.ae";
		this.sendingFilesNowFieldId = "c0a80223e1d32dcc13144565d66a00d8";
		this.transactionStageFieldId = "c0a80223e1d32dca131445651d62f056";
		this.filesFieldId = "c0a80223e1d32dca13144565af61229a";
		this.statusFieldId = "c0a80223e1d32dca13144565c2e92683";
		this.contactUsActionId = "c0a8024ae85538163e399b708c71d78e";
		this.signTypeId = "c0a8024aa2f37c67add458d04d3031f6";
		this.companyId = "c0a80223e1d32dcc131445655531b4fd";
		this.faxId = "c0a8024aa2f37c44add458d0fc6e0844";
		this.requestedCompletionDateId = "c0a80223e1d32dcc13144565ce735342";
		this.signQuantityId = "c0a8024aa2f37c68add458d0e706df00";
		this.desiredHeightId = "c0a8024aa2f37c5eadd458d0d6b01a4d";
		this.desiredWidthId = "c0a8024aa2f37c5fadd458d0ad9bb183";
		this.projectDescriptionId = "c0a80223e1d32dcc1314456596cbbca6";
		this.salesContactId = "c0a80223e1d32dc9131445658820b465";
	}

	/* (non-Javadoc)
	 * @see com.fastsigns.action.saf.SAFConfig#statusFieldId()
	 */
	public String getSenderEmailAddress(boolean isSAF) {
		return (isSAF) ? "sendafile@fastsigns.com" : "requestaquote@fastsigns.com";
	}

	/* (non-Javadoc)
	 * @see com.fastsigns.action.saf.SAFConfig#buildEmail(boolean, com.smt.sitebuilder.action.contact.ContactDataContainer)
	 */
	@Override
	public String buildEmail(boolean isDealer, ContactDataContainer cdc, Map<String, String> vals) {
		ContactDataModuleVO record = cdc.getData().get(0);
		String name = StringUtil.checkVal(record.getFirstName()) + " " + StringUtil.checkVal(record.getLastName());
		StringBuilder msg = new StringBuilder(1000);
		String dealerLink = "http://" + getPostbackDomain() + "/" + vals.get("aliasPath");
		
		/*
		 * Billy Larsen.
		 * Brand Imaging needed a special announcement, cleared approval for a special
		 * rule with James Mckain
		 */
		String dealer = vals.get("fastsignsTxt") + "&reg; of " + vals.get("locationName");
		if(((String)vals.get("dealerLocationId")).equals("210"))
			dealer = "Brand Imaging Group";
		
		if (isDealer) {
			msg.append("<p><b>Reminder: Do Not click reply on this email, it is sent to you from a do not reply email box.</b></p>");
			msg.append("<p>This message is to advise you that you have received a Request a Quote and/or a file.<br/>");
			msg.append("Please use the contact information provided in the form to response to the customer or prospect.</p>");
			msg.append("<p>Thank you.</p>");
		} else {
			msg.append("<p>Dear ").append(name).append(",<br/>Thank you for contacting ");
			msg.append(dealer);
			msg.append(". This email serves to confirm ");
			msg.append("that we have received the following information from you.<br/>If you have any ");
			msg.append("questions please contact us at ").append(vals.get("phoneNumber")).append(" or ");
			msg.append(vals.get("dealerEmail")).append("<br/>Thank you,<br/><a href='").append(dealerLink).append("'>");
			msg.append(dealer).append("</a><br/>More than fast. More than signs.&trade;");
			
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
