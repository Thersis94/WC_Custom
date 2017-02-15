package com.biomed.smarttrak.admin.user;

/****************************************************************************
 * <b>Title</b>: HumanNameIntfc.java<p/>
 * <b>Description: Generics superclass for all VOs that contain and sort based on name (first & last).
 * This is needed because we can't sort these records in the query...they need to be decrypted first.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public interface HumanNameIntfc {

	/**
	 * setter for first name
	 */
	public abstract String getFirstName();

	/**
	 * setter for last name
	 * @return
	 */
	public abstract String getLastName();

	/**
	 * @param firstName
	 */
	public abstract void setFirstName(String decrypt);

	/**
	 * @param lastName
	 */
	public abstract void setLastName(String decrypt);
}