package com.biomed.smarttrak.security;

import com.smt.sitebuilder.security.DBLoginLockoutModule;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKLoginModule</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2016 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Dec 20, 2016
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKLoginModule extends DBLoginLockoutModule {

	// TODO 2016-12-20: Stub
	/*
	 * Pending requirements discovery, override methods as needed to implement custom
	 * login processing / user data retrieval (e.g. initial recently visited,  
	 */
	
	/**
	 * TODO Requirements indicate tracking user session activity from login to logoff.  This
	 * method would process user logoff and:
	 * 	- remove the appropriate session objects
	 * - log the timestamp of the logoff to complete the record of 'this' visitor's session activity.
	 * - log any additional metrics (recently visited, etc.)
	 * 
	 * One option: Add this method to the base class (AbstractLoginModule) and override here. Would
	 * give the ability to post-process explicit logoff.
	 * 
	 * Would still need way to handle implicit logoff (i.e. session timeout)
	 */
	public void processLogOff() {
		
	}
	
}
