package com.perfectstorm.util;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.utils.DBConnectionManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perfectstorm.action.admin.VenueTourAttributeWidget;
import com.perfectstorm.action.venue.VenueAction;
import com.perfectstorm.action.weather.ForecastAlertAction;
import com.perfectstorm.data.VenueTourAttributeVO;
import com.perfectstorm.data.VenueTourVO;
import com.perfectstorm.data.weather.forecast.ForecastAlertVO;
import com.perfectstorm.data.weather.forecast.VenueTourForecastVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.websocket.WebSocketSessionManager;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.scheduler.AbstractSMTJob;

/****************************************************************************
 * <b>Title:</b> WeatherAlertJob.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Scheduled job that will send notifications & alerts
 * when specific weather thresholds have been reached.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 05 2019
 * @updates:
 ****************************************************************************/

public class WeatherAlertJob extends AbstractSMTJob {
	
	private Map<String, Object> attributes;
	private SMTDBConnection dbConnection;

	public WeatherAlertJob() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);
		
		JobDataMap jobAttributes = ctx.getMergedJobDataMap();
		String message = null;
		boolean success = true;
		
		// Transpose the attributes, since this is out-of-band
		attributes = new HashMap<>();
		for (String key : jobAttributes.getKeys()) {
			attributes.put(key, jobAttributes.get(key));
		}
		
		// Get the db connection
		try (Connection dbConn = DBConnectionManager.getInstance().getConnection("quartzDS")) {
			dbConnection = new SMTDBConnection(dbConn);
			
			// Get all tour venues requiring weather alerts
			List<VenueTourVO> venueTours = getVenueTours();
			for (VenueTourVO venueTour : venueTours) {
				// Get latest forecast data
				VenueAction va = new VenueAction(attributes, dbConnection);
				venueTour = va.getVenueTour(venueTour.getVenueTourId());
				venueTour.setCurrentConditions(va.getForecast(venueTour, null));
				
				// Save the forecast data to the db, since we are in the "recording" period
				String forecastId = saveCurrentConditions(venueTour);
				
				// Get thresholds for the tour venue
				VenueTourAttributeWidget vta = new VenueTourAttributeWidget(attributes, dbConnection);
				List<VenueTourAttributeVO> thresholds = vta.getVenueTourAttributes(venueTour.getVenueTourId());
				
				// Scan forecast data for thresholds that are exceeded
				checkThresholds(venueTour, thresholds, forecastId);
			}
			
		} catch (Exception e) {
			message = e.getMessage();
			success = false;
		}
		
		// Finalize the job
		try {
			this.finalizeJob(success, message);
		} catch (InvalidDataException e) {
			log.error("Unable to finalize job");
		}
	}
	
	/**
	 * Checks if weather thresholds have been exceeded in the forecast data
	 * and sends notifications if so.
	 * 
	 * @param venueTour
	 * @param thresholds
	 * @param forecastId
	 */
	private void checkThresholds(VenueTourVO venueTour, List<VenueTourAttributeVO> thresholds, String forecastId) {
		boolean hasAlerts = false;
		Map<String, Integer> forecastData = venueTour.getCurrentConditions().getDataMap();

		// Scan the forecast data for thresholds that are exceeded
		for (VenueTourAttributeVO threshold : thresholds) {
			if (forecastData.get(threshold.getAttributeCode()) >= threshold.getValue()) {
				hasAlerts = true;
				
				// Save the alert
				saveAlert(forecastData, threshold, forecastId);
				
				// TODO: Send SMS message for this alert
			}
		}
		
		// Send push notification to the browser via websockets
		if (hasAlerts) {
			WebSocketSessionManager wsm = WebSocketSessionManager.getInstance();
			wsm.broadcast(venueTour.getVenueTourId(), "updateAlerts");
		}
	}
	
	/**
	 * Creates and saves a forecast alert
	 * 
	 * @param venueTour
	 * @param threshold
	 * @param forecastId
	 */
	private void saveAlert(Map<String, Integer> forecastData, VenueTourAttributeVO threshold, String forecastId) {
		ForecastAlertVO alert = new ForecastAlertVO();
		alert.setVenueTourForecastId(forecastId);
		alert.setAttributeCode(threshold.getAttributeCode());
		alert.setValue(forecastData.get(threshold.getAttributeCode()));
		alert.setNewFlag(1);
		
		ForecastAlertAction fa = new ForecastAlertAction(attributes, dbConnection);
		fa.saveAlert(alert);
	}
	
	/**
	 * Saves the current weather conditions
	 * 
	 * @param venueTour
	 * @return
	 */
	private String saveCurrentConditions(VenueTourVO venueTour) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		DBProcessor dbp = new DBProcessor(dbConnection, schema);
		Gson gson = new GsonBuilder().create();
		
		VenueTourForecastVO forecast = new VenueTourForecastVO();
		forecast.setVenueTourForecastId(new UUIDGenerator().getUUID());
		forecast.setVenueTourId(venueTour.getVenueTourId());
		forecast.setForecastText(gson.toJson(venueTour.getCurrentConditions()));
		try {
			dbp.insert(forecast);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error saving forecast conditions", e);
		}
		
		return forecast.getVenueTourForecastId();
	}
	
	/**
	 * Get the list of tour venues that need weather alerts
	 * 
	 * @return
	 */
	private List<VenueTourVO> getVenueTours() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("ps_venue_tour_xr vt");
		sql.append(DBUtil.WHERE_CLAUSE).append("start_retrieve_dt <= ? and end_retrieve_dt >= ? ");
		log.debug(sql);
		
		DBProcessor dbp = new DBProcessor(dbConnection, schema);
		return dbp.executeSelect(sql.toString(), Arrays.asList(new Date(), new Date()), new VenueTourVO());
	}
}
