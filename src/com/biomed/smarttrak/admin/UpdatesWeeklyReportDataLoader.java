package com.biomed.smarttrak.admin;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.biomed.smarttrak.action.UpdatesEditionDataLoader;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title:</b> UpdatesWeeklyReportDataLoader.java<br/>
 * <b>Description:</b> loads the data slightly different for /manage.  This class simply alters the query used to load the data
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 8, 2017
 ****************************************************************************/
public class UpdatesWeeklyReportDataLoader extends UpdatesEditionDataLoader {

	public UpdatesWeeklyReportDataLoader() {
		super();
	}

	/**
	 * @param init
	 */
	public UpdatesWeeklyReportDataLoader(ActionInitVO init) {
		super(init);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		req.setParameter("profileId", null);
		req.setParameter("timeRangeCd", UpdatesWeeklyReportAction.TIME_RANGE_WEEKLY);
		req.setAttribute(IS_MANAGE_TOOL, true); //attribute - can't be spoofed by the browser
		super.retrieve(req);
	}


	/*
	 * overridden to get data for THIS week, not LAST week (as the end-user would see) 
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionDataLoader#makeWeeklyDateRange(java.util.Date, int)
	 */
	@Override
	protected Date[] makeWeeklyDateRange(Date endDt, int days) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-5"), Locale.getDefault());
		//set the first day to monday
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(endDt);
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);

		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
			cal.add(Calendar.DATE, 1);

		//"The Monday after today" is the endDate we want to work against.  Advance the endDt to that date.
		while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
			cal.add(Calendar.DATE, 1);
		}
		cal.add(Calendar.DATE, -days);
		Date startDt = cal.getTime();

		//go seven days out to get the end range
		cal.add(Calendar.DATE, 7);

		//add the start/end dates and daysToGoBack to collection.
		return new Date[]{ startDt, cal.getTime()};
	}
}