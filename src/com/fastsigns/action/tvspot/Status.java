package com.fastsigns.action.tvspot;

/****************************************************************************
 * <b>Title</b>: Status.java<p/>
 * <b>Description: enum constants for the Status levels
 * for the TVSpot ContactUs Portlet.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 1, 2014
 ****************************************************************************/
public enum Status {
	initiated,
	callNoAnswer,
	callLeftMessage,
	callExistingCustomer,
	callSuccess,
	appointmentMade,
	saleMade,
	prospect,
	invalid;
}
