package com.depuysynthesinst.lms;

/****************************************************************************
 * <b>Title</b>: AbstractLMSCall.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 27, 2015
 ****************************************************************************/
public abstract class AbstractLMSCall {

	public AbstractLMSCall() {
	}

	protected abstract LMSResponse invoke();
	
}
