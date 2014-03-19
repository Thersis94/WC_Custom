package com.venture.cs.message;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;

/****************************************************************************
 *<b>Title</b>: CaseMessageFormatter<p/>
 * Formats a case message based on activity type.  Utilizes a factory method to create the message 
 * body.<p/>
 * 
 *Copyright: Copyright (c) 2014<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 11, 2014
 * Changes:
 * Mar 11, 2014: DBargerhuff: created class
 ****************************************************************************/

public class CaseMessageFormatter {
	
	public enum CaseMessageType {
		ADMIN,
		FOLLOWER,
		SHARED
	}
	
	/**
	 * Static method that returns a case message based on the activity type.
	 * @param activityType
	 * @param vehicles
	 * @return
	 * @throws ActionException 
	 */
	public static AbstractCaseMessage getMessage(CaseMessageType messageType)	throws ActionException {
		AbstractCaseMessage acm = null;
		
		// instantiate the appropriate message builder
		switch(messageType) {
			case ADMIN:
				acm = new CaseAdminMessage();
				break;
			case FOLLOWER:
				acm = new CaseFollowerMessage();
				break;
			case SHARED:
				acm = new CaseShareMessage();
				break;
			default:
				throw new ActionException("Unkown CaseMessageType,  type: " + messageType);
		}
		
		return acm;
	}
	
}
