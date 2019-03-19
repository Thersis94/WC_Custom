package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;

// PS Libs
import com.perfectstorm.data.TourVO.TourType;
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
import com.siliconmtn.util.StringUtil;
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
		Object data;
		if (req.hasParameter("isPwa")) {
			//display all events exclusive of type on the PWA
			data = getAllEvents(req.getDoubleParameter("latitude"), req.getDoubleParameter("longitude"));
		} else {
			boolean showPast = req.getBooleanParameter("showPast");
			data = getTourVenues(req.getParameter("tourId"), showPast);
		}
		setModuleData(data);
	}


	/**
	 * List upcoming events predicated by tourId or tourTypeCd
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	private List<VenueTourVO> getTourVenues(String tourId, boolean showPast) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 

		// Build the SQL
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(250);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("ps_venue_tour_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_venue b ");
		sql.append("on a.venue_id=b.venue_id ");

		// If no tourId was passed, we are searching for events
		if (StringUtil.isEmpty(tourId)) {
			sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_tour c ");
			sql.append("on a.tour_id = c.tour_id ");
			sql.append(DBUtil.WHERE_CLAUSE).append("c.tour_type_cd = ? ");
			vals.add(TourType.EVENT.name());
		} else {
			sql.append("where a.tour_id = ? ");
			vals.add(tourId);
		}

		// Add the filter designation whether to show past events
		if (!showPast) {
			sql.append("and a.event_dt >= ? ");
			vals.add(Convert.formatStartDate(Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, -1)));
		}

		sql.append(DBUtil.ORDER_BY).append("a.event_dt asc");

		// execute the sql
		DBProcessor db = new DBProcessor(dbConn, schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		return db.executeSelect(sql.toString(), vals, new VenueTourVO());
	}



	/**
	 * List the top 10 events - upcoming & closest to me first.  Used by the PWA.
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	private List<VenueTourVO> getAllEvents(Double latitude, Double longitude) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select *, core.geoCalcDistance(cast(? as numeric), cast(? as numeric), b.latitude_no, ");
		sql.append("b.longitude_no, 'mi') as distance from ");
		sql.append(schema).append("ps_venue_tour_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_venue b on a.venue_id=b.venue_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_tour c on a.tour_id = c.tour_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("a.event_dt >= ? ");
		sql.append(DBUtil.ORDER_BY).append("a.event_dt asc, distance limit 10");

		// Add the params
		List<Object> vals = new ArrayList<>();
		vals.add(latitude);
		vals.add(longitude);
		vals.add(Convert.formatStartDate(Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, -1)));

		// execute the sql
		DBProcessor db = new DBProcessor(dbConn, schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		return db.executeSelect(sql.toString(), vals, new VenueTourVO());
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		VenueTourVO tv = new VenueTourVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.delete(tv);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("unable to remove venue from tour", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (req.getBooleanParameter("delAction")) {
			this.delete(req);
			return;
		} else {

			VenueTourVO tv = new VenueTourVO(req);
			String[] time = req.getParameter("eventTime").split(":");
			int startDur = req.getIntegerParameter("startDuration", DEFAULT_STORAGE_DURATION);
			int endDur = req.getIntegerParameter("endDuration", DEFAULT_STORAGE_DURATION);

			try {
				saveTourVenue(tv, time, startDur, endDur);
				setModuleData(tv);
			} catch(Exception e) {
				log.error("Unable to save tour venue", e);
				putModuleData(tv, 1, false,e.getLocalizedMessage(), true);
			}
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
		db.save(tv);
	}
}

