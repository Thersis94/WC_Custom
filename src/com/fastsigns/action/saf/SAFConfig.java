/**
 * 
 */
package com.fastsigns.action.saf;

import java.util.Map;

import org.apache.log4j.Logger;

import com.fastsigns.action.RequestAQuoteSTF;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.contact.ContactDataContainer;

/****************************************************************************
 * <b>Title</b>: SAFConfig.java<p/>
 * <b>Description: wireframe for SAF country/website configs.  All sites/countries 
 *  using RAQ/SAF will implement this in their own way, for their Org.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 12, 2012
 ****************************************************************************/
public abstract class SAFConfig {
	
	protected static Logger log = Logger.getLogger(SAFConfig.class);
	protected String countryCode = "";
	protected String postbackDomain = "";
	protected String contactUsActionId = "";
	protected String sendingFilesNowFieldId = "";
	protected String transactionStageFieldId = "";
	protected String filesFieldId = "";
	protected String statusFieldId = "";
	protected String signTypeId = "";
	protected String companyId = "";
	protected String faxId = "";
	protected String requestedCompletionDateId = "";
	protected String signQuantityId = "";
	protected String desiredHeightId = "";
	protected String desiredWidthId = "";
	protected String projectDescriptionId = "";
	protected String salesContactId = "";
	
	/** 
	 * The two-digit country code
	 */
	public String getCountryCode(){
		return this.countryCode;
	}
	
	/**
	 * the FQDN <b>primary</b> domain of the website, used when we call the postback servlet.  
	 * "www.fastsigns.com"
	 * @return
	 */
	public String getPostbackDomain(){
		return this.postbackDomain;
	}
	
	/**
	 * the actionId of the ContactUs Portlet form that SAF facades
	 * @return
	 */
	public String getContactUsActionId(){
		return this.contactUsActionId;
	}
	
	/**
	 * The ContactFieldId tied to the question that asks the user 
	 * "are you going to upload a file?"
	 * @return
	 */
	public String getSendingFilesNowFieldId(){
		return this.sendingFilesNowFieldId;
	}
	
	/**
	 * The ContactFieldId tied to the question that holds the 'state' of this transaction
	 * @return
	 */
	public String getTransactionStageFieldId(){
		return this.transactionStageFieldId;
	}
	
	/**
	 * The ContactFieldId tied to the question that holds the uploaded files
	 * @return
	 */
	public String getFilesFieldId(){
		return this.filesFieldId;
	}
	
	/**
	 * The ContactFieldId tied to the transaction status. (in progress, complete, etc.)
	 * Different from transaction stage (uploading, files sent, complete, etc.)
	 * @return
	 */
	public String getStatusFieldId(){
		return this.statusFieldId;
	}
	
	/**
	 * the bi-directional method that assembles the emails sent to the Center and User
	 * @param isDealer
	 * @param cdc
	 * @param vals 
	 * @return
	 */
	public abstract String buildEmail(boolean isDealer, ContactDataContainer cdc, Map<String, String> vals);
	
	
	/**
	 * returns the email address of the center; used when we email the customer, so if they reply
	 * to the email it gets sent to the Center, not the ethers.
	 * @param isSAF
	 * @return
	 */
	public String getSenderEmailAddress(Map<String, String> vals) {
		if (vals != null) {
			Integer webId = Convert.formatInteger(vals.get(RequestAQuoteSTF.DEALER_LOCATION_ID), null);
		
			if (webId != null)
				return webId + "@fastsigns.com";
		}
		
		return getNoReplyEmailAddress();
	}
	

	/**
	 * returns a canned "do not reply" email address; used when emailing the Centers SAF requests
	 * @return
	 */
	public String getNoReplyEmailAddress() {
		return "do_not_reply@fastsigns.com";
	}
	
	
	/**
	 * the subject of the email message sent to the Center
	 * @return
	 */
	public abstract String getEmailSubjectCenter(String emailAddress);
	
	/**
	 * the subject of the email message sent to the User
	 * @param userEmail email address of the user (null or empty will return default subject)
	 * @return
	 */
	public String getEmailSubjectUser(String emailAddr) {
		StringBuilder subj = new StringBuilder();
		String addr = StringUtil.checkVal(emailAddr, null);
		if (addr == null){
			subj.append("Your request has been delivered to FASTSIGNS");
		} else {
			subj.append(emailAddr);
			subj.append(", your request has been delivered to FASTSIGNS");
		}
		return subj.toString();
	}
	
	/**
	 * the type of sign requested by the user
	 * @return
	 */
	public String getSignTypeId(){
		return this.signTypeId;
	}
	
	/**
	 * the users company
	 * @return
	 */
	public String getCompanyId(){
		return this.companyId;
	}
	
	/**
	 * the users fax
	 * @return
	 */
	public String getFaxId(){
		return this.faxId;
	}
	
	/**
	 * the requested completion date for the order
	 * @return
	 */
	public String getRequestedCompletionDateId(){
		return this.requestedCompletionDateId;
	}
	
	/**
	 * the requested quantity
	 * @return
	 */
	public String getSignQuantityId(){
		return this.signQuantityId;
	}
	
	/**
	 * the requested sign height
	 * @return
	 */
	public String getDesiredHeightId(){
		return this.desiredHeightId;
	}
	
	/**
	 * the requested sign width
	 * @return
	 */
	public String getDesiredWidthId(){
		return this.desiredWidthId;
	}
	
	/**
	 * users description of the job
	 * @return
	 */
	public String getProjectDescriptionId(){
		return this.projectDescriptionId;
	}
	
	/**
	 * users sales contact
	 */
	
	public String getSalesContactId(){
		return this.salesContactId;
	}
	
	/** 
	 * a very basic factory-method pattern!
	 * @param countryCd
	 * @return
	 */
	public static SAFConfig getInstance(String countryCd) {
		String className = "com.fastsigns.action.saf." + countryCd + "SAFConfig";
		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(className);
			return (SAFConfig) load.newInstance();
		} catch (ClassNotFoundException cnfe) {
			log.error("Unable to find className", cnfe);
		} catch (Exception e) {
			log.error("Unable to create SAFConfig Bean.", e);
		}
			return new USSAFConfig();
	}
	
	
}
