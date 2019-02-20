package com.perfectstorm.action.venue;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.smt.sitebuilder.action.SimpleActionAdapter;

//WC Libs 3.x
import com.smt.sitebuilder.common.constants.Constants;
import com.perfectstorm.action.weather.VenueForecastManager;
// Perfect Storm Libs
import com.perfectstorm.data.VenueTourVO;
import com.perfectstorm.data.VenueVO;
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
			} else {
				putModuleData(getVenueTour(venueTourId));
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
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("ps_venue_tour_xr vt");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_venue v on vt.venue_id = v.venue_id");
		sql.append(DBUtil.WHERE_CLAUSE).append("venue_tour_id = ?");
		log.debug(sql);
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		List<VenueTourVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(venueTourId), new VenueTourVO());
		
		return data == null || data.isEmpty() ? new VenueTourVO() : data.get(0);
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

