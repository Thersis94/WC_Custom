package com.ansmed.sb.util.calendar;

import java.util.Date;

/*****************************************************************************
<p><b>Title</b>: SJMDateBean.java</p>
<p><b>Description: <b/>Utility class containing a start/end date pair
for a given SJM business date (i.e. month, quarter, year).</p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Dave Bargerhuff
@version 1.0
@since Aug 12, 2009
Last Updated:
***************************************************************************/
public class SJMDateBean {

	private Date startDate = null;
	private Date endDate = null;
	
	public SJMDateBean() {
		
	}
	
	public SJMDateBean(Date start, Date end) {
		setStartDate(start);
		setEndDate(end);
	}
	
	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	/**
	 * returns String representation of this object
	 */
	public String toString() {
		return this.getStartDate().toString() + " | " + this.getEndDate().toString();
	}
	
	
	
	
}
