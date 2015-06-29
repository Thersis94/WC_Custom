package com.depuysynthesinst.lms;

import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: LMSResponse.java<p/>
 * <b>Description: Wrapper for all responses returned from LMSProxy (calls).  Checks for error messages,
 * captures response values, etc.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 27, 2015
 ****************************************************************************/
public class LMSResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int errorCode = 0;

	public LMSResponse(byte[] response) {
	}
	
	
	public boolean hasError() { 
		return errorCode > 0;
	}
	
	/**
	 * transposes error codes into meaningful string values, for error logging and informing the user
	 * @return
	 */
	public String getError() {
		switch (errorCode) {
			case 1: return "";
			default: return "";
		}
	}
}