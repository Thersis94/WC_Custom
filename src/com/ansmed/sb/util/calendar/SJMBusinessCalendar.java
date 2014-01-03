package com.ansmed.sb.util.calendar;

// JDK 1.6
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SMT Base Libs 2.0
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: SJMBusinessCalendar.java</p>
 <p>SJM business calendar utility which provides SJM business month/quarter
 start and end dates.</p>
 <p>Copyright: Copyright (c) 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Aug 12, 2009
***************************************************************************/

public class SJMBusinessCalendar {
	
	private static final int MIN_SJM_BASE_YEAR = 2009;
	private static final int MAX_SJM_BASE_YEAR = 2020;
	
	private Map<String,List<SJMDateBean>> sjmCal = null;
	
	private List<SJMDateBean> months = null;
	private List<SJMDateBean> quarters = null;
	private List<SJMDateBean> year = null;
	
	private int thisBusCalYear = 0;
	
	private int currentSjmYear = 0;
	private SJMDateBean currentSjmYearDate = null;
	
	private int currentSjmMonth = 0;
	private SJMDateBean currentSjmMonthDate = null;
	
	private int currentSjmQuarter = 0;
	private SJMDateBean currentSjmQuarterDate = null;
	
	
	/**
	 * default constructor instantiates object using today's calendar date
	 * @throws InvalidCalendarException
	 */
	public SJMBusinessCalendar() throws InvalidCalendarException {
    	this(Calendar.getInstance());
    }
    
	/**
	 * instantiates business calendar after validating calendar passed in.
	 * @param cal
	 * @throws InvalidCalendarException
	 */
	public SJMBusinessCalendar(Calendar cal) throws InvalidCalendarException {
		
		int inYear = cal.get(Calendar.YEAR);
		
		// If calendar is out of range, throw exception.  We have to check against
		// max + 1 because max year end date may fall in year max + 1. 
		if ((inYear < MIN_SJM_BASE_YEAR) || (inYear > (MAX_SJM_BASE_YEAR + 1))) {
			throw new InvalidCalendarException("Calendar for business year " 
						+ inYear + " is not available.");
		} else {
			if (inYear == (MAX_SJM_BASE_YEAR + 1)) {
				inYear = MAX_SJM_BASE_YEAR;
			}
			
			// retrieve the map.
			try {
				this.sjmCal = SJMCalendarFactory.getSJMCalendar(inYear);
			} catch (InvalidCalendarException ice) {
				throw new InvalidCalendarException("Calendar for business year " 
					+ inYear + " is not available.");
			}
			
			// check to see if the passed in calendar's date falls within 
			// the year we just retrieved.
			Date inDate = cal.getTime();
			Date yearStart = this.sjmCal.get("year").get(0).getStartDate();
			Date yearEnd = this.sjmCal.get("year").get(0).getEndDate();
			
			// if date is within range, initialize the calendar.
			if (inDate.after(yearStart) && inDate.before(yearEnd)) {
				this.setThisBusCalYear(inYear);
				this.initCalendar(cal);
				
			} else {
				int newYear = 0;
				if (inDate.before(yearStart)) {
					// date belongs to previous bus calendar year.
					newYear = inYear - 1;
				} else {
					// date belongs to next bus calendar year.
					newYear = inYear + 1;
				}
				
				if ((newYear < MIN_SJM_BASE_YEAR) || (newYear > (MAX_SJM_BASE_YEAR))) {
					throw new InvalidCalendarException("Date falls within calendar year " 
								+ newYear + " which is not available.");
				} else {
					try {
						this.sjmCal = SJMCalendarFactory.getSJMCalendar(newYear);
					} catch (InvalidCalendarException ice) {
						throw new InvalidCalendarException("Calendar for business year " 
								+ newYear + " is not available.");
					}
					this.setThisBusCalYear(newYear);
					this.initCalendar(cal);
				}				
			}
		}
	}
	
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) throws InvalidCalendarException {
				
		//Should initialize calendar to specified year.
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(GregorianCalendar.MONTH, 0);
		cal.set(GregorianCalendar.DATE, 3);
		cal.set(GregorianCalendar.YEAR, 2021);
		System.out.println("cal time: " + cal.getTime());
		SJMBusinessCalendar s = null;
		try {
			s = new SJMBusinessCalendar(cal);
		} catch(InvalidCalendarException ice) {
			System.out.println(ice);
			System.exit(-1);
		}
		
		System.out.println("This object's base calendar year: " + s.getThisBusCalYear());

		//Test business months:
		System.out.println("Current date is: " + cal.getTime());
		
		System.out.println("Current calculated bus month is: " + s.getMonthByDate(cal.getTime()));
		System.out.println("Current object bus month is: " + s.getCurrentMonth());
		System.out.println("Current bus month date: " + s.getCurrentMonthDate());
		System.out.println("Current bus quarter is: " + s.getCurrentQuarter());
		System.out.println("Current bus quarter date: " + s.getCurrentQuarterDate());
		
		System.out.println("");
		try {
			System.out.println("Prev month no: " + s.getPreviousMonth());
			System.out.println("Prev month: " + s.getPreviousMonthDate().toString());
		} catch (InvalidCalendarException ice) {
			System.out.println(ice);
		}
		
		System.out.println("Curr month no: " + s.getCurrentMonth());
		System.out.println("Curr month: " + s.getCurrentMonthDate().toString());
		
		try {
			System.out.println("Next month no: " + s.getNextMonth());
			System.out.println("Next month: " + s.getNextMonthDate().toString());
		} catch (InvalidCalendarException ice) {
			System.out.println(ice);
		}
		
		System.out.println("");
		
		try {
			System.out.println("Prev qtr no: " + s.getPreviousQuarter());
			System.out.println("Prev qtr: " + s.getPreviousQuarterDate().toString());
		} catch (InvalidCalendarException ice) {
			System.out.println(ice);
		}
		System.out.println("Curr qtr no: " + s.getCurrentQuarter());
		System.out.println("Curr qtr: " + s.getCurrentQuarterDate().toString());
		
		try {
			System.out.println("Next qtr no: " + s.getNextQuarter());
			System.out.println("Next qtr: " + s.getNextQuarterDate().toString());
		} catch (InvalidCalendarException ice) {
			System.out.println(ice);
		}
		
		System.out.println("");
		
		try {
		System.out.println("Prev year no: " + s.getPreviousYear());
		System.out.println("Prev year: " + s.getPreviousYearDate().toString());
		} catch (InvalidCalendarException ice) {
			System.out.println(ice);
		}
		
		System.out.println("Curr year no: " + s.getCurrentYear());
		System.out.println("Curr year: " + s.getCurrentYearDate().toString());
		
		try {
			System.out.println("Next year no: " + s.getNextYear());
			System.out.println("Next year: " + s.getNextYearDate().toString());
		} catch (InvalidCalendarException ice) {
			System.out.println(ice);
		}
		System.out.println("Is today start of new bus month? " + s.isStartOfBusMonth(cal.getTime()));
		System.out.println("Is today start of new bus qtr? " + s.isStartOfBusQtr(cal.getTime()));

	}
		
	
	/**
	 * Initializes various calendar properties based on the business calendar
	 * year retrieved.
	 * @param cal
	 */
	private void initCalendar(Calendar cal) throws InvalidCalendarException {
		
		// initialize the maps and the business calendar
		this.months = new ArrayList<SJMDateBean>();
		this.quarters = new ArrayList<SJMDateBean>();
		this.year = new ArrayList<SJMDateBean>();
		
		loadMonths((List<SJMDateBean>)this.sjmCal.get("months"));
		loadQuarters((List<SJMDateBean>)this.sjmCal.get("quarters"));
		loadYear((List<SJMDateBean>)this.sjmCal.get("year"));
		setCurrentMonth(cal);
		setCurrentMonthDate();
		setCurrentQuarter(cal);
		setCurrentQuarterDate();
		setCurrentYear();
		setCurrentYearDate();
	}
	
	
	/**
	 * @return the month dates list for this calendar
	 */
	public List<SJMDateBean> getMonths() {
		return months;
	}


	/**
	 * @return the quarterly dates list for this calendar
	 */
	public List<SJMDateBean> getQuarters() {
		return quarters;
	}
	
	/**
	 * @return the year dates list for this calendar
	 */
	public List<SJMDateBean> getYear() {
		return year;
	}
	
	
	/**
	 * @return the business year value for this calendar
	 */
	public int getThisBusCalYear() {
		return thisBusCalYear;
	}
	
	
	/**
	 * Sets the base 'business' year value for this calendar
	 * @param year
	 */
	private void setThisBusCalYear(int year) {
		this.thisBusCalYear = year;
	}
	
	
	/**
	 * returns int value of previous business month
	 * @return
	 */
	public int getPreviousMonth() {
		
		int month = this.getCurrentMonth();
		if (month == 1) {
			return 12;
		} else {
			return --month;
		}
	}
	
	/**
	 * returns previous business month date bean
	 * @return
	 */
	public SJMDateBean getPreviousMonthDate() 
		throws InvalidCalendarException {
		
		// get the previous month's number
		int month = this.getPreviousMonth();
		
		SJMDateBean s = new SJMDateBean();
		
		if (month == 12) {
			//Fetch last year's business calendar if available.
			int prevYear = this.getThisBusCalYear() - 1;
			
			Map<String,List<SJMDateBean>> prev = null;
			try {
				prev = SJMCalendarFactory.getSJMCalendar(prevYear);
			} catch (InvalidCalendarException ice) {
				throw new InvalidCalendarException("Calendar for business year " 
						+ prevYear + " is not available.");
			}
			List<SJMDateBean> prevMonths = (List<SJMDateBean>)prev.get("months");
			//get 12th month of previous year
			s = prevMonths.get(--month);
			return s;
		} else {
			return this.months.get(--month);
		}
	}
	
	/**
	 * return the current month as an int value.
	 * @return
	 */
	public int getCurrentMonth() {
		return this.currentSjmMonth;
	}
	
	/**
	 * 
	 * @param cal
	 */
	private void setCurrentMonth(Calendar cal) {
		this.currentSjmMonth = this.getMonthByDate(cal.getTime());
	}
	
	/**
	 * sets the current month date bean.
	 */
	private void setCurrentMonthDate() {
		this.currentSjmMonthDate = this.months.get(this.getCurrentMonth() - 1);
	}
	
	/**
	 * returns the current month date bean.
	 * @return
	 */
	public SJMDateBean getCurrentMonthDate() {
		return this.currentSjmMonthDate;
	}
	
	/**
	 * returns int value of next business month
	 * @return
	 */
	public int getNextMonth() {
		
		int month = this.getCurrentMonth();
		if (month == 12) {
			return 1;
		} else {
			return ++month;
		}
	}
	
	/**
	 * returns next business month date bean
	 * @return
	 */
	public SJMDateBean getNextMonthDate() throws InvalidCalendarException {
		
		// get the next month's number
		int month = this.getNextMonth();
		SJMDateBean s = new SJMDateBean();
		
		if (month == 1) {
			//Fetch next year's business calendar if available.
			int nextYear = this.getThisBusCalYear() + 1;
			
			// get a next year if it exists.
			Map<String,List<SJMDateBean>> nextYr = null;
			try {
				nextYr = SJMCalendarFactory.getSJMCalendar(nextYear);
			} catch (InvalidCalendarException ice) {
				throw new InvalidCalendarException("Calendar for business year " 
						+ nextYear + " is not available.");
			}
			List<SJMDateBean> nextMonths = (List<SJMDateBean>)nextYr.get("months");
			//get 12th month of next year
			s = nextMonths.get(--month);
			return s;
		} else {
			return this.months.get(--month);
		}
	}
	
	/**
	 * returns previous quarter as an int value
	 * @return
	 */
	public int getPreviousQuarter() {
		int qtr = this.getCurrentQuarter();
		if (qtr == 1) {
			return 4;
		} else {
			return --qtr;
		}
	}
	
	/**
	 * returns previous quarter date bean
	 * @return
	 */
	public SJMDateBean getPreviousQuarterDate() throws InvalidCalendarException {
		int qtr = this.getPreviousQuarter();
		SJMDateBean s = new SJMDateBean();
		
		if (qtr == 4) {
			//Fetch last year's business calendar if available.
			int prevYear = this.getThisBusCalYear() - 1;
			
			// get a previous year if it exists.
			Map<String,List<SJMDateBean>> prev = null;
			try {
				prev = SJMCalendarFactory.getSJMCalendar(prevYear);
			} catch (InvalidCalendarException ice) {
				throw new InvalidCalendarException("Calendar for business year " 
						+ prevYear + " is not available.");
			}
			List<SJMDateBean> prevQuarters = (List<SJMDateBean>)prev.get("quarters");
			//get Q4 of previous year
			s = prevQuarters.get(--qtr);
			return s;
		} else {
			return this.quarters.get(--qtr);
		}
	}
	
	/**
	 *  returns the current quarter as an int value.
	 * @return
	 */
	public int getCurrentQuarter() {
		return this.currentSjmQuarter;
	}
	
	/**
	 * sets the current 
	 * @param cal
	 */
	private void setCurrentQuarter(Calendar cal) {
		this.currentSjmQuarter = this.getQuarterByDate(cal.getTime());
	}
	
	/**
	 * sets the current quarter date bean.
	 */
	private void setCurrentQuarterDate() {
		this.currentSjmQuarterDate = this.quarters.get(this.getCurrentQuarter() - 1);
	}
	
	/**
	 * returns the current quarter date bean.
	 * @return
	 */
	public SJMDateBean getCurrentQuarterDate() {
		return this.currentSjmQuarterDate;
	}
	
	public int getNextQuarter() {
		int qtr = this.getCurrentQuarter();
		if (qtr == 4) {
			return 1;
		} else {
			return ++qtr;
		}
	}
	
	/**
	 * returns next quarter date bean
	 * @return
	 */
	public SJMDateBean getNextQuarterDate() throws InvalidCalendarException {
		int qtr = this.getNextQuarter();
		SJMDateBean s = new SJMDateBean();
		
		if (qtr == 1) {
			//Fetch next year's business calendar if available.
			int nextYear = this.getThisBusCalYear() + 1;
			
			// get next year if it exists.
			Map<String,List<SJMDateBean>> next = null;
			try {
				next = SJMCalendarFactory.getSJMCalendar(nextYear);
			} catch (InvalidCalendarException ice) {
				throw new InvalidCalendarException("Calendar for business year " 
						+ nextYear + " is not available.");
			}
			List<SJMDateBean> nextQuarters = (List<SJMDateBean>)next.get("quarters");
			//get Q1 of next year
			s = nextQuarters.get(--qtr);
			return s;
		} else {
			return this.quarters.get(--qtr);
		}
	}
	
	/**
	 * returns previous year as int value
	 * @return
	 */
	public int getPreviousYear() {
		return this.getCurrentYear() - 1;
	}
	
	/**
	 * returns previous year start/end date bean
	 * @return
	 */
	public SJMDateBean getPreviousYearDate() throws InvalidCalendarException {
				
		int prevYear = this.getCurrentYear() - 1;
		SJMDateBean s = new SJMDateBean();
		
		// Retrieve prev year if it exists.
		if (prevYear < MIN_SJM_BASE_YEAR) 
			throw new InvalidCalendarException("Calendar for business year " 
					+ prevYear + " is not available.");
		
		Map<String,List<SJMDateBean>> prev = null;
		try {
			prev = SJMCalendarFactory.getSJMCalendar(prevYear);
		} catch (Exception e) {
			throw new InvalidCalendarException("Calendar for business year " 
					+ prevYear + " is not available.");
		}
		List<SJMDateBean> prevYearDate = (List<SJMDateBean>)prev.get("year");
		//get date bean for prev year
		s = prevYearDate.get(0);
		return s;
	}
	
	/**
	 * returns current year as int value
	 * @return
	 */
	public int getCurrentYear() {
		return this.currentSjmYear;
	}
	
	/**
	 * sets current year int value
	 */
	private void setCurrentYear() {
		this.currentSjmYear = getThisBusCalYear();
	}
	
	/**
	 * sets current year start/end date bean
	 */
	private void setCurrentYearDate() {
		this.currentSjmYearDate = this.year.get(0);
	}
	
	/**
	 * returns current year start/end date bean
	 * @return
	 */
	public SJMDateBean getCurrentYearDate() {
		return this.currentSjmYearDate;
	}
	
	/**
	 * returns next year as int value
	 * @return
	 */
	public int getNextYear() {
		return this.getCurrentYear() + 1;
	}
	
	/**
	 * returns next year start/end date bean
	 * @return
	 */
	public SJMDateBean getNextYearDate() throws InvalidCalendarException {
				
		int nextYear = this.getCurrentYear() + 1;
		SJMDateBean s = new SJMDateBean();
		
		if (nextYear > MAX_SJM_BASE_YEAR) 
			throw new InvalidCalendarException("Calendar for business year " + nextYear 
					+ " is not available.");
		
		// get next year if it exists.
		Map<String,List<SJMDateBean>> next = null;
		try {
			next = SJMCalendarFactory.getSJMCalendar(nextYear);
		} catch (InvalidCalendarException ice) {
			throw new InvalidCalendarException("Calendar for business year " 
					+ nextYear + " is not available.");
		}
		List<SJMDateBean> nextYearDate = (List<SJMDateBean>)next.get("year");
		//get date bean for next year
		s = nextYearDate.get(0);
		return s;
	}

	/**
	 * Returns boolean indicating whether 'today' (i.e today's date starting 
	 * at HH:mm:ss of 00:00:00 is the start date of a new SJM business month.
	 * @param today
	 * @return
	 */
	public boolean isStartOfBusMonth(Date today) {
		
		boolean monthStart = false;
		Date start = Convert.formatStartDate(today);
		SJMDateBean bn = null;
		
		//iterate the business month start dates.
		for(Iterator<SJMDateBean> iter = months.iterator(); iter.hasNext();) {
			bn = iter.next();
			if (start.equals(bn.getStartDate())) {
				monthStart = true;
				break;
			}
		}
		return monthStart;
	}
	
	/**
	 * Returns boolean indicating whether 'today' is the start of a
	 * new SJM business quarter.
	 * @param today
	 * @return
	 */
	public boolean isStartOfBusQtr(Date today) {
		
		boolean qtrStart = false;
		Date start = Convert.formatStartDate(today);
		SJMDateBean bn = null;
		
		//iterate the business quarter start dates.
		for(Iterator<SJMDateBean> iter = quarters.iterator(); iter.hasNext();) {
			bn = iter.next();
			if (start.equals(bn.getStartDate())) {
				qtrStart = true;
				break;
			}
		}
		
		return qtrStart;
	}
	
	/**
	 * Returns the int value of the month based on the date passed.  Returns a
	 * value of -1 if the date passed in is not valid for the business year upon
	 * which the util object is based.
	 * @param inDate
	 * @return
	 */
	public int getMonthByDate(Date inDate) {
		int cnt = 0;
		boolean found = false;
		SJMDateBean bn = null;
		
		for(Iterator<SJMDateBean> iter = months.iterator(); iter.hasNext();) {
			cnt++;
			bn = iter.next();
			if (inDate.after(bn.getStartDate()) && inDate.before(bn.getEndDate())) {
				found = true;
				break;
			}
		}
		if (found) {
			return cnt;
		} else return -1;
	}
	
	/**
	 * Returns the int value of the quarter based on the date passed.  Returns a
	 * value of -1 if the date passed in is not valid for the business year that
	 * the util object is based on.
	 * @param inDate
	 * @return
	 */
	public int getQuarterByDate(Date inDate) {
		
		int cnt = 0;
		boolean found = false;
		SJMDateBean bn = null;
		
		for(Iterator<SJMDateBean> iter = quarters.iterator(); iter.hasNext();) {
			cnt++;
			bn = iter.next();
			if (inDate.after(bn.getStartDate()) && inDate.before(bn.getEndDate())) {
				found = true;
				break;
			}
		}
		if (found) {
			return cnt;
		} else {
			return -1;
		}
	}
	
	/**
	 * Utility method - returns the natural quarter integer value given the 
	 * month integer value passed in.
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
	 * @param months the months to set
	 */
	private void loadMonths(List<SJMDateBean> months) {
		this.months = months;
	}
	
	/**
	 * @param quarters the quarters to set
	 */
	private void loadQuarters(List<SJMDateBean> quarters) {
		this.quarters = quarters;
		
	}

	/**
	 * @param year the year to set
	 */
	private void loadYear(List<SJMDateBean> year) {
		this.year = year;
	}
	
}
