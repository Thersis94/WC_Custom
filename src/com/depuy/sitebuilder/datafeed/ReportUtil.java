package com.depuy.sitebuilder.datafeed;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/****************************************************************************
 * <b>Title</b>:ReportUtil.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Aug 6, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class ReportUtil {
	public static final int DAILY_GROUP = 1;
	public static final int WEEKLY_GROUP = 2;
	public static final int MONTHLY_GROUP = 3;
	
	/**
	 * 
	 */
	public ReportUtil() {
		
	}
	
	public static void main(String[] args) {
		System.out.println("Week 27: " + calculateDate(2, 27, 0,2008));
		System.out.println("Week 28: " + calculateDate(2, 28, 0,2008));
		System.out.println("Week 29: " + calculateDate(2, 29, 0,2008));
		System.out.println("Week 30: " + calculateDate(2, 30, 0,2008));
		System.out.println("Week 31: " + calculateDate(2, 31, 0,2008));
	}
	
	/**
	 * 
	 * @param groupType Daily, Weekly or Monthly grouping
	 * @param val Value for the associated group type. Example: if the group
	 * type is weekly, the val is the week of the year
	 * @param month Only sent if the group type is daily
	 * @param year Year of the date
	 * @return A new date object based upon the data parameters.  Time stamp is
	 * set to 00:00:00 00
	 */
	public static Date calculateDate(int groupType, int val, int month, int year) {
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		switch (groupType) {
			case DAILY_GROUP:
				cal.set(Calendar.DAY_OF_MONTH, val);
				cal.set(Calendar.MONTH, month-1);
				break;
			case WEEKLY_GROUP:
				cal.set(Calendar.WEEK_OF_YEAR, val);
				cal.set(Calendar.DAY_OF_WEEK, 1);
				break;
			case MONTHLY_GROUP:
				cal.set(Calendar.MONTH, val-1);
		}
		
		return cal.getTime();
	}
}
