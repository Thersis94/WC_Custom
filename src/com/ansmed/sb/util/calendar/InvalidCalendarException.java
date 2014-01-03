package com.ansmed.sb.util.calendar;

import com.siliconmtn.exception.BaseException;

 /****************************************************************************
 <b>Title</b>: InvalidCalendarException<p/>
 <p>Exetnds the BaseException class and should be utilized for
 generic Calendar exceptions<p/>
 <p>Copyright: Copyright (c) 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Aug 21, 2009
 ****************************************************************************/

public class InvalidCalendarException extends BaseException {
	 private static final long serialVersionUID = 77L;
	 
    /** Creates new InvalidCalendarException */
    public InvalidCalendarException() {
        super();
    }

    /** Creates new InvalidCalendarException
     *  @param msg msg to display or localize
     *  @param cause original error object */
    public InvalidCalendarException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /** Creates new InvalidCalendarExceptionon
     *  @param key Pass in just the error msg*/
    public InvalidCalendarException(String key) {
        super(key);
    }

    /** Creates new InvalidCalendarException
     *  @param cause Pass in just the error object */
    public InvalidCalendarException(Throwable cause) {
        super(cause);
    }
}
