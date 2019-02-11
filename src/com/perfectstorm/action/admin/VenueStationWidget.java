package com.perfectstorm.action.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
// JD 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Gson 2.3
import com.google.gson.Gson;

// PS Libs
import com.perfectstorm.data.VenueVO;
import com.perfectstorm.data.weather.StationExtVO;
import com.perfectstorm.data.weather.StationVO;
import com.perfectstorm.data.weather.VenueWeatherStationVO;
import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: VenueStationWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the association of stations to venues
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class VenueStationWidget extends SBActionAdapter {

	/**
	 * URL of the API call to get the list of stations
	 */
	public static final String STATION_LIST_URL =  "https://api.weather.gov/points/%f,%f/stations";
	
	/**
	 * 
	 */
	public VenueStationWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public VenueStationWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public VenueStationWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
	}
	
	/**
	 * 
	 * @param venueId
	 * @throws SQLException
	 */
	public void delete(String venueId) throws SQLException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("delete from ").append(getCustomSchema()).append("ps_venue_weather_station ");
		sql.append("where venue_id = ?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, venueId);
			ps.executeUpdate();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Requires venueId, lat and long
		VenueVO venue = new VenueVO(req);
		try {
			updateObservationStation(venue, false);
			putModuleData(venue);
		} catch (InvalidDataException ide) {
			putModuleData(venue, 0, false, ide.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Retrieves the list of stations from the weather service and associates them to the venue
	 * @param venue
	 * @param isNew
	 */
	public void updateObservationStation(VenueVO venue, boolean isNew) 
	throws InvalidDataException {
		String url = String.format(STATION_LIST_URL, venue.getLatitude(),venue.getLongitude());
		List<VenueWeatherStationVO> stations = new ArrayList<>();
		log.debug("URL: " + url);
		
		// Get the data
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		conn.setFollowRedirects(true);
		
		try {
			byte[] data = conn.retrieveData(url);
			Gson g = new Gson();
			StationVO station = g.fromJson(new String(data), StationVO.class);
			
			for (StationExtVO feature : station.getFeatures()) {
				VenueWeatherStationVO vo = new VenueWeatherStationVO();
				vo.setVenueId(venue.getVenueId());
				vo.setWeatherStationCode(feature.getProperties().getStationIdentifier());
				stations.add(vo);
			}
			
			// Delete the existing entries
			if (! isNew && ! stations.isEmpty()) delete(venue.getVenueId());

			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.executeBatch(stations, true);
		} catch (Exception e) {
			log.error("unable to retrieve stations", e);
			throw new InvalidDataException("Unable to update observation stations", e);
		}
	}
}

