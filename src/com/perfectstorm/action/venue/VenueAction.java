package com.perfectstorm.action.venue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
// JDK 1.8.x
import java.util.Map;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

//WC Libs 3.x
import com.perfectstorm.action.weather.SunTimeCalculatorAction;
import com.perfectstorm.action.weather.VenueForecastManager;
import com.perfectstorm.action.weather.manager.NWSRadarManager;
// Perfect Storm Libs
import com.perfectstorm.data.VenueTourVO;
import com.perfectstorm.data.VenueVO;
import com.perfectstorm.data.weather.SunTimeVO;
import com.perfectstorm.data.weather.forecast.ForecastVO;

/****************************************************************************
 * <b>Title</b>: VenueAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the venues widget
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/

public class VenueAction extends SimpleActionAdapter {
	
	/**
	 * Key for the Facade / Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "venue_public";
	
	/**
	 * 
	 */
	public VenueAction() {
		super();
	}
	
	/**
	 * 
	 * @param attributes
	 * @param dbConn
	 */
	public VenueAction(Map<String, Object> attributes, SMTDBConnection dbConn ) {
		super();
		
		this.attributes = attributes;
		this.dbConn = dbConn;
	}

	/**
	 * @param actionInit
	 */
	public VenueAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String venueTourId = req.getParameter("venueTourId");
		boolean isJson = req.getBooleanParameter("json");
		
		try {
			if (isJson && req.hasParameter("getForecast")) {
				VenueTourVO venueTour = getVenueTour(req.getParameter("venueTourId"));
				putModuleData(getForecast(venueTour, req.getDateParameter("eventDate")));

			} else if (isJson && req.hasParameter("refreshRadar")) {
				putModuleData(getRadarMetaData(req.getParameter("radarCode"), req.getParameter("radarTypeCode")));

			} else if (isJson) {
				putModuleData(getFullVenueTour(venueTourId, req.getParameter("radarTypeCode")));
			}
		} catch (Exception e) {
			log.error("Unable to retrieve venue tour event: " + venueTourId, e);
			this.putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Gets the specified event with its forecast data and radar metadata.
	 * 
	 * @param venueTourId
	 * @param radarTypeCode
	 * @return
	 */
	public VenueTourVO getFullVenueTour(String venueTourId, String radarTypeCode) {
		VenueTourVO venueTour = getVenueTour(venueTourId);
		
		// Get the radar metadata
		venueTour.setRadarTypeCode(radarTypeCode);
		addRadarMetaData(venueTour);
		
		// Add the forecast data
		venueTour.setEventForecast(getForecast(venueTour, venueTour.getEventDate()));
		venueTour.setCurrentConditions(getForecast(venueTour, null));
		
		// Add the sun time data
		venueTour.setEventSunTime(getSunTime(venueTour, venueTour.getEventDate()));
		venueTour.setCurrentSunTime(getSunTime(venueTour, null));
		
		return venueTour;
	}
	
	/**
	 * Gets the specified event.
	 * 
	 * @param venueTourId
	 * @return
	 */
	public VenueTourVO getVenueTour(String venueTourId) {
		StringBuilder sql = new StringBuilder(200); 
		sql.append("select vt.*, v.*, ws.station_nm, ws.weather_station_cd, ws.radar_flg, ws.radar_cd ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ps_venue_tour_xr vt");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_venue v on vt.venue_id = v.venue_id");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ps_weather_station ws on v.radar_station_cd = ws.weather_station_cd");
		sql.append(DBUtil.WHERE_CLAUSE).append("venue_tour_id = ?");
		log.debug(venueTourId + " | " + sql);
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		List<VenueTourVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(venueTourId), new VenueTourVO());
		
		return data == null || data.isEmpty() ? new VenueTourVO() : data.get(0);
	}
	
	/**
	 * Adds the radar meta data required by the UI.
	 * 
	 * @param venueTour
	 */
	public void addRadarMetaData(VenueTourVO venueTour) {
		String radarCode = venueTour.getRadarCode();
		String radarTypeCode = venueTour.getRadarTypeCode();

		// We can't get the data without these values
		if (StringUtil.isEmpty(radarCode) || StringUtil.isEmpty(radarTypeCode)) {
			venueTour.setRadarTime(new ArrayList<>());
			return;
		}
		
		// Retrieve the radar metadata
		try {
			NWSRadarManager nrm = new NWSRadarManager();
			venueTour.setRadarTime(nrm.retrieveData(radarCode, radarTypeCode));
		} catch (ActionException e) {
			log.error("unable to get radar meta data", e);
			venueTour.setRadarTime(new ArrayList<>());
		}
	}
	
	/**
	 * Retrieves updated radar meta data only.
	 * 
	 * @param radarCode
	 * @param radarTypeCode
	 * @return
	 */
	public VenueTourVO getRadarMetaData(String radarCode, String radarTypeCode) {
		VenueTourVO venueTour = new VenueTourVO();
		venueTour.setRadarCode(radarCode);
		venueTour.setRadarTypeCode(radarTypeCode);
		addRadarMetaData(venueTour);
		
		return venueTour;
	}
	
	/**
	 * Gets the forecast for the venue and the requested date.
	 * 
	 * @param venueTour
	 * @param eventDate
	 * @return
	 */
	public ForecastVO getForecast(VenueTourVO venueTour, Date eventDate) {
		LocalDateTime date = eventDate == null ? LocalDateTime.now() : eventDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		
		try {
			// Return the forecast data
			VenueForecastManager vfm = new VenueForecastManager(venueTour, getAttributes());
			return vfm.getDetailForecast(date);
		} catch (ActionException e) {
			log.error("unable to retrieve forecast", e);
			return new ForecastVO();
		}
		
	}
	
	/**
	 * Gets the sunrise and sunset for the venue.
	 * 
	 * @param venue
	 * @param eventDate - might be the event date or the current date
	 * @return
	 */
	public SunTimeVO getSunTime(VenueVO venue, Date eventDate) {
		SunTimeCalculatorAction stc = new SunTimeCalculatorAction(getAttributes(), getDBConnection());
		SunTimeVO sunTime = new SunTimeVO();
		sunTime.setLatitudeNumber(venue.getLatitude());
		sunTime.setLongitudeNumber(venue.getLongitude());
		sunTime.setSourceDate(eventDate == null ? new Date() : eventDate);
		sunTime.setTimeZoneName(venue.getTimezone());
		
		try {
			return stc.calculateSunTimes(sunTime);
		} catch (InvalidDataException e) {
			log.error("unable to calculate the sun times", e);
			return sunTime;
		}
	}
}

