package com.ansmed.sb.util.calendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*****************************************************************************
<p><b>Title</b>: SJMCalendarFactory.java</p>
<p><b>Description: <b/>Factory for obtaining the current year's SJM calendar
map or for obtaining the SJM calendar map for a specified year.</p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Dave Bargerhuff
@version 1.0
@since Aug 12, 2009
Last Updated:
***************************************************************************/
public class SJMCalendarFactory {
	
	public static final String CLASS_STUB_NAME = "com.ansmed.sb.util.calendar.SJMCalendar";

	public SJMCalendarFactory() {
		
	}
	
	/**
	 * Returns the SJM calendar map for the current year if it exists.
	 * @return
	 * @throws InvalidCalendarException
	 */
	public static Map<String,List<SJMDateBean>> getSJMCalendar() 
		throws InvalidCalendarException {
		
		Calendar cal = GregorianCalendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		
		Map<String,List<SJMDateBean>> sjm = new HashMap<String,List<SJMDateBean>>();
		
		try {
			sjm = getSJMCalendar(year);
		} catch (InvalidCalendarException ice) {
			throw new InvalidCalendarException();
		}
		
		return sjm;
	}
	
	/**
	 * Returns the SJM calendar map for the specified year if it exists.
	 * @param year
	 * @return
	 * @throws InvalidCalendarException
	 */
	public static Map<String,List<SJMDateBean>> getSJMCalendar(int year) 
		throws InvalidCalendarException {
		
		String className = CLASS_STUB_NAME + year;
		SJMCalendar sc = null;
		
		try {
			Class<?> c = Class.forName(className);
			sc = (SJMCalendar) c.newInstance();
		} catch (Exception cnfe) {
			throw new InvalidCalendarException();
		}

		return sc.getSJMCalendarYear();
				
	}
	
}
