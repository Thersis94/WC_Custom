package com.fastsigns.action.tvspot;

import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: TVSpotConfig.java<p/>
 * <b>Description: superclass to all country-drive concrete implementations of the 
 * "Consult Fastsigns" contact-us portlets.  (US, UK, & AU)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 1, 2014
 ****************************************************************************/
public abstract class TVSpotConfig {

	protected static Logger log = Logger.getLogger(TVSpotConfig.class);
	public static final String CON_ = "con_";  //comes from ContactUs APIs
	
	// these are designed to be immutable; only set via this object's constructor
	private Map<ContactField, String> contactFields = null; //Map<ContactField, String databasePkId>
	private Map<Status, String> status = null; //Map<Status type, String human-readable label>
	
	
	/**
	 * constructor that sets our two configurable variables
	 * @param s
	 * @param cf
	 */
	public TVSpotConfig(Map<Status, String> s, Map<ContactField, String> cf) {
		this.status = s;
		this.contactFields = cf;
	}
	
	/**
	 * returns the String (database pkId) for this contactField, given the country implementation
	 * @param cf
	 * @return
	 */
	public final String getContactId(ContactField cf) {
		return contactFields.get(cf);
	}
	
	/**
	 * returns the String (human-readable name) for this Status, given the country implementation
	 * @param s
	 * @return
	 */
	public final String getStatusLabel(Status s) {
		return status.get(s);
	}
	
	/**
	 * returns the 2-digit country code of this implementation
	 * @return
	 */
	public abstract String getCountryCode();
	
	/**
	 * sets-up the request parameters needed so ContactUsFacadeAction can deliever a notification to the Center. (realtime)
	 * implementation is country-specific, so Abstract at this level.
	 * @param req
	 * @param dealer
	 */
	public abstract void addCenterEmailParamsToReq(SMTServletRequest req, DealerLocationVO dealer);
	
	
	/**
	 * the email subject line for the dealer-bound email
	 * @param name
	 * @return
	 */
	public abstract String getDealerEmailSubject(String name);
	
	/**
	 * the email header for the dealer-bound email; appears above the contact-us table
	 * @return
	 */
	public abstract String getDealerEmailHeader();
	
	/**
	 * the email footer for the dealer-bound email; appears below the contact-us table
	 * @return
	 */
	public abstract String getDealerEmailFooter();
	
	/**
	 * sends a confirmation email to the visitor who submitted the consultation request
	 * implementation is country-specific, so Abstract at this level.
	 * @param req
	 * @param dealer
	 */
	public abstract EmailMessageVO buildUserConfirmationEmail(SMTServletRequest req, DealerLocationVO dealer)
			throws InvalidDataException ;
	
	/**
	 * The default sender address is used on the email sent to the visitor who submitted the consultation request
	 * @return
	 */
	public abstract String getDefaultSenderEmail();
	
	/**
	 * Returns the 'from' email address for the corp consultation email
	 * @return
	 */
	public abstract String getCorpConsultationEmail();
	
	/**
	 * This field hooks to the View; it's a custom request parameter we defined to hold the dealerLocationId value
	 * @return
	 */
	public abstract String getDealerLocnField();
	
	/**
	 * builds the survey email to send to the user
	 * sent from out-of-band script
	 * @return
	 */
	public abstract EmailMessageVO buildSurveyEmail(String contactSubmittalId);
	
	/**
	 * builds the first notification email to remind the Center of a new lead they need to contact
	 * sent from out-of-band script
	 * @param vo
	 * @return
	 */
	public abstract EmailMessageVO buildFirstNoticeEmail(ContactDataModuleVO vo)
			 throws InvalidDataException;
	
	/**
	 * builds the copy report email, minus the attachment.
	 * sent from out-of-band script
	 * @return
	 * @throws InvalidDataException
	 */
	public abstract EmailMessageVO buildCorpReportEmail() throws InvalidDataException;
	
	/** same as corp report email, except filtered for "this center only" and sent to the Center Owner.
	 * sent from out-of-band script
	 * @return
	 * @throws InvalidDataException
	 */
	public abstract EmailMessageVO buildCenterReportEmail() throws InvalidDataException;
	
	/** 
	 * simple factory pattern to load config based on countryCode, defaults to US config.
	 * @param countryCd
	 * @return
	 */
	public static TVSpotConfig getInstance(String countryCd) {
		String className = "com.fastsigns.action.tvspot.TVSpotConfig" + countryCd;
		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(className);
			return (TVSpotConfig) load.newInstance();
		} catch (ClassNotFoundException cnfe) {
			log.error("Unable to find className", cnfe);
		} catch (Exception e) {
			log.error("Unable to create TVSpotConfig Bean.", e);
		}
		return new TVSpotConfigUS();
	}
}
