package com.fastsigns.action;

/****************************************************************************
 * <b>Title</b>: TVSpotTransactionStep.java<p/>
 * <b>Description: Simple Enum of the steps involved in this process.
 * These determine where (in the process) the user is, 
 * Should be used to block duplicate efforts on previously-completed steps.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 27, 2014
 ****************************************************************************/
public enum TVSpotTransactionStep {
		initiated, // initial form submitted
		emailSent, //emails sent to Center and User
		complete;  //transaction complete
}
