package com.fastsigns.scripts;

import com.siliconmtn.util.CommandLineUtil;
import com.smt.sitebuilder.action.contact.ContactDataContainer;

/****************************************************************************
 * <b>Title</b>: TVSpotEmailer.java<p/>
 * <b>Description: Handles the nightly email notifications sent out-of-band in 
 * relation to the Q2 2014 TV spot commercial/campaign.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 3, 2014
 ****************************************************************************/
public class TVSpotEmailer extends CommandLineUtil {
	
	private boolean isSurveyRun = false;

	public TVSpotEmailer(String[] args) {
		super(args);
		loadProperties("scripts/fts_TVSpot.properties");
		loadDBConnection(props);
		
		isSurveyRun =  (args != null && args.length > 0 && "survey".equals(args[0]));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TVSpotEmailer emailer = new TVSpotEmailer(args);
		emailer.run();
	}
	
		
	/**
	 * handles script invocation, to ensure all steps are executed in the proper sequence.
	 */
	public void run() {
		//load all the inquiries that have a status of pending
		ContactDataContainer cdc = loadContactData();
		
		//only send the 7-day surveys if that's what we were invoked to do. 
		if (isSurveyRun) {
			sendSurveys(cdc);
			return;
		}
		
		//for the ones that are 1 day old, send the 1-day notification
		sendFirstNotice(cdc);
		
		//send reports to corporate
		sendCorpReport(cdc);
		
	}
	
	
	private ContactDataContainer loadContactData() {
		ContactDataContainer cdc = null;
		//leverage com.smt.sitebuilder.contact.ContactDataAction.
		//refactor ContactDataAction to be able to load data w/o depending on a SMTServletRequest...
		//we'll re-use this (new method) in WC3.  You can move 80% of the code in the update()
		//method to the new method and invoke it both ways (directly, and from update for legacy code).
		
		//be sure you test the Contact Data Tool in the admintool after you make your changes.
		
		return cdc;
	}
	
	private void sendSurveys(ContactDataContainer cdc) {
		//loop all records and sends the survey email to records 7 days old.
		//if today is monday, also grab records that are 8 & 9 days old (Saturday & Sunday)
		//we don't run this script on Saturdays & Sundays.
	}
	
	private void sendFirstNotice(ContactDataContainer cdc) {
		//if the record status (com.fastsigns.action.TVSpotUtil.ContactField.status) 
		// is pending (com.fastsigns.action.TVSpotUtil.Status.initiated)
		// and the record was created 'yesterday', send an email to the Center
		// warning them they have to take action.
		
		// NOTE: be careful of timestamps.  You'll want to turn the Time into a Date before comparing.
		//if 'today' is 3/24/14, this email goes out to every record stamped 3/23/14, regardless of time (8am or 11pm).
		
	}

	private void sendCorpReport(ContactDataContainer cdc) {
		//turn all records into a single Excel report (com.fastsigns.action.TVSpotReportVO)
		//and email it to corporate.
	}
	
}
