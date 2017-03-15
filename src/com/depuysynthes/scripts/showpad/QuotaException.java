package com.depuysynthes.scripts.showpad;

/**
 * **************************************************************************
 * <b>Title</b>: ShowpadApiUtil.java<p/>
 * <b>Description: Used to tell invoking classes that we've reached our threshold at Showpad
 * and are unable to process any more requests.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 15, 2016
 ***************************************************************************
 */
public class QuotaException extends Exception {
	private static final long serialVersionUID = -4186031278333010611L;

	public QuotaException(String reason) {
		super(reason);
	}
}