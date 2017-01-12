package com.biomed.smarttrak.security;

import com.smt.sitebuilder.security.DBLoginLockoutModule;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKLoginModule</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Jan 03, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKLoginModule extends DBLoginLockoutModule {

	// TODO 2017-01-03: Stub
	/*
	 * Pending requirements discovery, override methods as needed to implement custom
	 * login processing / user data retrieval (e.g. initial recently visited,  
	 */
	
	/**
	 * TODO Requirements indicate tracking user session activity from login to logoff.
	 * Possibly add a logoff method to the login module to process user logoff and:
	 * 	- remove the appropriate session objects
	 * - log the timestamp of the logoff to complete the record of 'this' visitor's session activity.
	 * - persist any additional metrics (recently visited, etc.)
	 * 
	 * Will still need way to handle implicit logoff (i.e. session timeout)
	 */
	
}
