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
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

//WC Libs 3.x
import com.smt.sitebuilder.common.constants.Constants;
import com.perfectstorm.action.weather.VenueForecastManager;
import com.perfectstorm.action.weather.manager.NWSRadarManager;
// Perfect Storm Libs
import com.perfectstorm.data.VenueTourVO;
import com.perfectstorm.data.VenueVO;
import com.perfectstorm.data.weather.VenueWeatherStationVO;
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
		UserDataVO profile = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String venueTourId = req.getParameter("venueTourId");
		boolean isJson = req.getBooleanParameter("json");
		
		try {
			if (isJson && req.hasParameter("getForecast")) {
				String venueId = req.getStringParameter("venueId");
				Date eventDate = req.getDateParameter("eventDate");
				putModuleData(getForecast(venueId, eventDate));
			} else if (isJson && req.hasParameter("refreshRadar")) {
				VenueTourVO venueTour = new VenueTourVO();
				venueTour.setRadarTypeCode(req.getStringParameter("radarTypeCode"));
				venueTour.setRadarCode(req.getStringParameter("radarCode"));
				addRadarMetaData(venueTour);
				putModuleData(venueTour);
			} else if (isJson) {
				VenueTourVO venueTour = getVenueTour(venueTourId);
				venueTour.setRadarTypeCode(req.getStringParameter("radarTypeCode"));
				addRadarMetaData(venueTour);
				putModuleData(venueTour);
			}
		} catch (DatabaseException | InvalidDataException e) {
			log.error("Unable to retrieve venue tour event: " + venueTourId, e);
			this.putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Gets the specified event.
	 * 
	 * @param venueTourId
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public VenueTourVO getVenueTour(String venueTourId) throws InvalidDataException, DatabaseException {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select vt.*, v.*, vws.*, ws.station_nm, ws.radar_flg, ws.radar_cd, ");
		sql.append("ws.forecast_office_cd, ws.forecast_gridx_no, ws.forecast_gridy_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ps_venue_tour_xr vt");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_venue v on vt.venue_id = v.venue_id");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_venue_weather_station vws on v.venue_id = vws.venue_id");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_weather_station ws on vws.weather_station_cd = ws.weather_station_cd");
		sql.append(DBUtil.WHERE_CLAUSE).append("venue_tour_id = ?");
		log.debug(venueTourId + " | " + sql);
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		List<VenueTourVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(venueTourId), new VenueTourVO());
		VenueTourVO venueTour = data == null || data.isEmpty() ? new VenueTourVO() : data.get(0);
		
		// Use the first radar code as the default for this venue tour
		for (VenueWeatherStationVO station : venueTour.getWeatherStations()) {
			if (!StringUtil.isEmpty(station.getRadarCode())) {
				venueTour.setRadarCode(station.getRadarCode());
				break;
			}
		}
		
		return venueTour;
	}
	
	/**
	 * Adds the radar meta data required by the UI
	 * 
	 * @param venueTour
	 * @throws ActionException 
	 */
	public void addRadarMetaData(VenueTourVO venueTour) throws ActionException {
		String radarCode = venueTour.getRadarCode();
		if (StringUtil.isEmpty(radarCode)) {
			venueTour.setRadarTime(new ArrayList<>());
			return;
		}
		
		String radarTypeCode = venueTour.getRadarTypeCode();
		String url = StringUtil.join("http://radar.weather.gov/ridge/kml/animation/", radarTypeCode, "/", radarCode, "_", radarTypeCode, "_loop.kml");
		log.debug("retrieving radar meta data from: " + url);
		
		NWSRadarManager nrm = new NWSRadarManager();
		venueTour.setRadarTime(nrm.retrieveData(url));
	}
	
	/**
	 * Gets the forecast for the venue and the requested date.
	 * 
	 * @param venueId
	 * @param eventDate
	 * @return
	 * @throws ActionException
	 */
	public ForecastVO getForecast(String venueId, Date eventDate) throws ActionException {
		LocalDateTime date = eventDate == null ? LocalDateTime.now() : eventDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		
		// Get the venue data
		VenueVO venue = new VenueVO();
		venue.setVenueId(venueId);
		try {
			DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
			dbp.getByPrimaryKey(venue);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
		
		// Return the forecast data
		VenueForecastManager vfm = new VenueForecastManager(venue, getAttributes());
		return vfm.getDetailForecast(date);
	}
}

