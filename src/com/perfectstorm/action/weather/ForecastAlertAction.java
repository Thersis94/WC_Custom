package com.perfectstorm.action.weather;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.perfectstorm.data.VenueTourVO;
// PS Libs
import com.perfectstorm.data.weather.forecast.ForecastAlertVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: ForecastAlertAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages weather forecast alerts
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 6, 2019
 * @updates:
 ****************************************************************************/

public class ForecastAlertAction extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "forecast_alert";
	
	/**
	 * 
	 */
	public ForecastAlertAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ForecastAlertAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param attributes
	 * @param dbConn
	 */
	public ForecastAlertAction(Map<String, Object> attributes, SMTDBConnection dbConn ) {
		super();
		
		this.attributes = attributes;
		this.dbConn = dbConn;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		this.setModuleData(getAlerts(req.getParameter("venueTourId")));
		clearAlerts(req.getParameter("venueTourId"));
	}
	
	/**
	 * Gets the latest alerts for a tour venue
	 * 
	 * @param venueTourId
	 * @return
	 */
	public List<ForecastAlertVO> getAlerts(String venueTourId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct on (attribute_cd) fa.*, a.attribute_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ps_venue_tour_forecast vtf ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_forecast_alert fa on vtf.venue_tour_forecast_id = fa.venue_tour_forecast_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_attribute a on fa.attribute_cd = a.attribute_cd ");
		sql.append(DBUtil.WHERE_CLAUSE).append("venue_tour_id = ? ");
		sql.append(DBUtil.ORDER_BY).append("attribute_cd, create_dt desc");
		log.debug("Venue Tour Alerts: " + sql);
		
		// Return the data
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), Arrays.asList(venueTourId), new ForecastAlertVO());
	}
	
	/**
	 * Clears alerts so we know what has and hasn't been displayed yet.
	 * 
	 * @param venueTourId
	 */
	public void clearAlerts(String venueTourId) {
		VenueTourVO venueTour = new VenueTourVO();
		venueTour.setVenueTourId(venueTourId);

		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("ps_forecast_alert fa ");
		sql.append("set new_flg = 0 ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ps_venue_tour_forecast vtf ");
		sql.append(DBUtil.WHERE_CLAUSE).append("vtf.venue_tour_forecast_id = fa.venue_tour_forecast_id ");
		sql.append("and new_flg = 1 and venue_tour_id = ? ");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			dbp.executeSqlUpdate(sql.toString(), venueTour, Arrays.asList("venue_tour_id"));
		} catch (DatabaseException e) {
			log.error("Could not clear weather alerts", e);
		}
	}
	
	/**
	 * Saves a forecast alert
	 * 
	 * @param alert
	 */
	public void saveAlert(ForecastAlertVO alert) {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(alert);
		} catch (Exception e) {
			log.error("Could not save forecast alert", e);
		}
	}
}

