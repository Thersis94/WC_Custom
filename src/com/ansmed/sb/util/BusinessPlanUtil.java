package com.ansmed.sb.util;

// JDK 1.6.0
import java.util.Calendar;
import java.util.Date;

// SMT Base Libs 2.0
import com.siliconmtn.common.html.AbstractSelectList;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: BusinessPlanUtil.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 13, 2007
 Last Updated:
 ***************************************************************************/

public class BusinessPlanUtil extends AbstractSelectList {
	public static final int BUSPLAN_START_YEAR = 2009;
	public static final Integer QUARTER_1_MONTH = 1;
	public static final Integer QUARTER_2_MONTH = 4;
	public static final Integer QUARTER_3_MONTH = 7;
	public static final Integer QUARTER_4_MONTH = 10;
	
	private int businessPlanYear = 0;
	private int currYear = 0;
	private int currQuarter = 0;
	private int year = 0;
	private int quarter = 0;
	private int month = 0;
	
	public BusinessPlanUtil() {
		super();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		currYear = cal.get(Calendar.YEAR);
		currQuarter = this.getCurrentQuarter(currYear);
		createList();
	}
	
	/**
	 * Sets the business plan year based upon the provided quarter and year
	 * @param quarter
	 * @param year
	 */
	public BusinessPlanUtil(int quarter, int year) {
		this.setYear(year);
		this.setQuarter(quarter);
		this.setMonth(calculateQuarterMonth(quarter));
	}
	
	public BusinessPlanUtil(Date dateIn) {
		super();
		Calendar cal = Calendar.getInstance();
		// Set the current year
		this.setCurrYear(cal.get(Calendar.YEAR));
		// Set the calendar with the date passed in.
		cal.setTime(dateIn);
		
		// Set the year/month/quarter values for the date passed in.
		this.setYear(cal.get(Calendar.YEAR));
		this.setMonth(cal.get(Calendar.MONTH));
		this.setQuarter(calculateQuarter(this.getMonth()));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BusinessPlanUtil bpu = new BusinessPlanUtil();
		bpu.setListName("bpYear");
		bpu.setSelected("2007");
		bpu.setBusinessPlanYear(2008);

	}
	
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * Creates a list of year elements.
	 */
	public void createList() {
		// Populate the selectList with year values.
		for (int i=BUSPLAN_START_YEAR; i <= this.currYear; i++) {
			selectList.put(i,i);
		}
	}
	
	/**
	 * Determines if this is the current quarter of the year.
	 * @param quarter
	 * @param year
	 * @return
	 */
	public boolean isCurrentQuarter(int year) { 
		
		if (getCurrentQuarter(year) > 0) {
			return true;
		} else {
			return false;
		}	
	}
	
	/**
	 * Returns the current quarter of the year given a quarter and year value.
	 * @param quarter
	 * @param year
	 * @return
	 */
	public int getCurrentQuarter(int year) {
		
		Calendar cal = Calendar.getInstance();
		int currYear = cal.get(Calendar.YEAR);
		
		if (year != currYear) return 0;
		
		int currMonth = cal.get(Calendar.MONTH);
		return calculateQuarter(currMonth);
	}
	
	/**
	 * Returns the current quarter value for the current year.
	 * @return
	 */
	public int getCurrentQuarter() {
		
		Calendar cal = Calendar.getInstance();
		int currMonth = cal.get(Calendar.MONTH);
		
		return calculateQuarter(currMonth);
	}
	
	/**
	 * Calculates the month associated with the provided quarter
	 * @param quarter
	 * @return
	 */
	public static int calculateQuarterMonth(int quarter) {
		int month = 0;
		switch(quarter) {
		case 1:
			month = QUARTER_1_MONTH;
			break;
			case 2:
				month = QUARTER_2_MONTH;
				break;
			case 3:
				month = QUARTER_3_MONTH;
				break;
			case 4:
				month = QUARTER_4_MONTH;
				break;
		}
		
		return month;
	}
	
	/**
	 * Returns the quarter given the month value passed in.
	 * @param month
	 * @return
	 */
	public static int calculateQuarter(int month) {
		int currQuarter = 0;
				
		switch(month) {
		case 0:
		case 1:
		case 2:
			currQuarter = 1;
			break;
		case 3:
		case 4:
		case 5:
			currQuarter = 2;
			break;
		case 6:
		case 7:
		case 8:
			currQuarter = 3;
			break;
		case 9:
		case 10:
		case 11:
			currQuarter = 4;
			break;
		default:
			break;
		}
		return currQuarter;
	}
	
	/**
	 * @return the businessPlanYear
	 */
	public int getBusinessPlanYear() {
		return businessPlanYear;
	}
	/**
	 * @param businessPlanYear the businessPlanYear to set
	 */
	public void setBusinessPlanYear(int businessPlanYear) {
		this.businessPlanYear = businessPlanYear;
	}

	/**
	 * @return the currYear
	 */
	public int getCurrYear() {
		return currYear;
	}

	/**
	 * @param currYear the currYear to set
	 */
	public void setCurrYear(int currYear) {
		this.currYear = currYear;
	}

	/**
	 * @return the currYear
	 */
	public int getYear() {
		return year;
	}

	/**
	 * @param currYear the currYear to set
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * @return the currQuarter
	 */
	public int getQuarter() {
		return quarter;
	}

	/**
	 * @param currQuarter the currQuarter to set
	 */
	public void setQuarter(int quarter) {
		this.quarter = quarter;
	}

	/**
	 * @return the currMonth
	 */
	public int getMonth() {
		return month;
	}

	/**
	 * @param currMonth the currMonth to set
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/**
	 * @return the previousQuarter
	 */
	public int getPreviousQuarter() {
		if (quarter == 1) return 4;
		else return quarter - 1;
	}

	/**
	 * @return the nextQuarter
	 */
	public int getNextQuarter() {
		if (quarter == 4) return 1;
		else return quarter + 1;
	}
	
	/**
	 * Returns the year of the next quarter
	 * @return
	 */
	public int getNextQuarterYear() {
		if (quarter == 4) return year + 1;
		else return year;
	}

	/**
	 * Returns the year of the previous quarter
	 * @return
	 */
	public int getPreviousQuarterYear() {
		if (quarter == 1) return year - 1;
		else return year;
	}

	/**
	 * @return the currQuarter
	 */
	public int getCurrQuarter() {
		return currQuarter;
	}

	/**
	 * @param currQuarter the currQuarter to set
	 */
	public void setCurrQuarter(int currQuarter) {
		this.currQuarter = currQuarter;
	}
	
}
