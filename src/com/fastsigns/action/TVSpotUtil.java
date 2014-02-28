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
public class TVSpotUtil {
	
	/*
	 * enum mapping the transactionStage levels for the portlet
	 */
	public enum Status {
		initiated("Awaiting Contact"),
		callNoAnswer("Phone call made - no answer"),
		callLeftMessage("Phone call made - left message"),
		callExistingCustomer("Phone call made - existing customer - had conversation"),
		callSuccess("Phone call made - sent information"),
		appointmentMade("Appointment Made"),
		saleMade("Sale Made"),
		prospect("Prospect contacted us directly"),
		invalid("Contact information invalid - bad email and bad phone number");
		
		private String label;
		Status(String label) {
			this.label = label;
		}
		public String getLabel() { return label; }
	}
	
	
	/*
	 * enum mapping the contactFieldIds kept in the database to useable constants for WC
	 */
	public enum ContactField {
		businessChallenge("c0a8023727e92d4c94ee061a529c7d3c"),
		companyNm("c0a80237b0c703fd4020174ce3a74dfd"),
		industry("c0a8022d4aa7a83def1d1f05458cc933"),
		department("c0a802374ae4a1823f8e3f128a806466"),
		title("c0a802374af32fa435952a608c8c3946"),
		zipcode("c0a8022d4af41a7fa75a85ccdfdb1b37"),
		preferredLocation("c0a802374be51c9177a78a7b7677ea5c"),
		status("7f0001019c4932bc3629f3987f43b5ec"),
		transactionNotes("7f000101ed12428e6f503d8d58e4ef90");
		
		private String fieldId;
		ContactField(String fieldId) {
			this.fieldId = fieldId;
		}
		public String id() { return fieldId; }
	}
	
	
}
