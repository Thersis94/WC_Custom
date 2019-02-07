package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;

// PS Libs
import com.perfectstorm.data.VenueTourVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TourVenueWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the venues assigned to a tour
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class TourVenueWidget extends SBActionAdapter {

	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "tour-venue";
	
	/**
	 * Amount of time (in days) before and after the event to store data
	 */
	public static final int DEFAULT_STORAGE_DURATION = 24;
	
	/**
	 * 
	 */
	public TourVenueWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TourVenueWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		this.setModuleData(getTourVenues(req.getParameter("tourId")));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public List<VenueTourVO> getTourVenues(String tourId) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		vals.add(tourId);
		
		// Build the SQL
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(getCustomSchema()).append("ps_venue_tour_xr a ");
		sql.append("inner join ").append(getCustomSchema()).append("ps_venue b ");
		sql.append("on a.venue_id = b.venue_id ");
		sql.append("where tour_id = ? ");
		sql.append(DBUtil.ORDER_BY).append("event_dt asc");
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), vals, new VenueTourVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		VenueTourVO tv = new VenueTourVO(req);
		String[] time = req.getParameter("eventTime").split(":");
		int startDur = req.getIntegerParameter("startDuration", DEFAULT_STORAGE_DURATION);
		int endDur = req.getIntegerParameter("endDuration", DEFAULT_STORAGE_DURATION);
		
		try {
			saveTourVenue(tv, time, startDur, endDur);
			setModuleData(tv);
		} catch(Exception e) {
			log.error("Unable to save tour", e);
			putModuleData(tv, 1, false,e.getLocalizedMessage(), true);
		}
		
	}
	
	/**
	 * Saves the venue for a given tour
	 * @param tv Tour Venue object
	 * @param time array of hour : min of day
	 * @param startDur Timeframe to start tracking data in hours before the event
	 * @param endDur Timeframe to end tracking data in hours before the event
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveTourVenue(VenueTourVO tv, String[] time, int startDur, int endDur) 
	throws InvalidDataException, DatabaseException {
		
		// Merge the date and time
		Calendar cal = Calendar.getInstance();
		cal.setTime(tv.getEventDate());
		cal.set(Calendar.HOUR_OF_DAY, Convert.formatInteger(time[0]));
		cal.set(Calendar.MINUTE, Convert.formatInteger(time[1]));
		tv.setEventDate(cal.getTime());

		// Set the start and end date / time for storing data
		tv.setStartRetrieve(DateUtils.addHours(tv.getEventDate(), -startDur));
		tv.setEndRetrieve(DateUtils.addHours(tv.getEventDate(), endDur));

		// Perform the insert
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.insert(tv);
	}
}

