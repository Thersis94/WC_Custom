package com.perfectstorm.util;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.utils.DBConnectionManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perfectstorm.action.admin.VenueTourAttributeWidget;
import com.perfectstorm.action.venue.VenueAction;
import com.perfectstorm.action.weather.ForecastAlertAction;
import com.perfectstorm.data.CustomerVO.CustomerType;
import com.perfectstorm.data.MemberVO;
import com.perfectstorm.data.VenueTourAttributeVO;
import com.perfectstorm.data.VenueTourVO;
import com.perfectstorm.data.weather.forecast.ForecastAlertVO;
import com.perfectstorm.data.weather.forecast.VenueTourForecastVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.websocket.WebSocketSessionManager;
import com.siliconmtn.io.mail.SMSMessageVO;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.scheduler.AbstractSMTJob;
import com.smt.sitebuilder.util.MessageSender;

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
				
				// Notify via websocket sessions there are updated conditions available
				WebSocketSessionManager wsm = WebSocketSessionManager.getInstance();
				wsm.broadcast(venueTour.getVenueTourId(), "updateCurrent");
				
				// No alerts need to be sent if the event is not in progress
				LocalDateTime now = new Date().toInstant().atZone(ZoneId.of(venueTour.getTimezone())).toLocalDateTime();
				if (!venueTour.isEventInProgress(java.sql.Timestamp.valueOf(now)))
					continue;
				
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
		int alertCount = 0;
		Map<String, Integer> forecastData = venueTour.getCurrentConditions().getDataMap();
		
		StringBuilder smsMessage = new StringBuilder(100);
		smsMessage.append(venueTour.getVenueName() + " - ");

		// Scan the forecast data for thresholds that are exceeded
		for (VenueTourAttributeVO threshold : thresholds) {
			if (forecastData.get(threshold.getAttributeCode()) >= threshold.getValue()) {
				alertCount++;
				
				// Save the alert
				saveAlert(forecastData, threshold, forecastId, venueTour.getTimezone());
				
				// Build onto the SMS message for this alert
				buildSmsMessage(smsMessage, alertCount, forecastData, threshold);
			}
		}
		
		// Send push notification to the browser via websockets and SMS messages
		if (alertCount > 0) {
			WebSocketSessionManager wsm = WebSocketSessionManager.getInstance();
			wsm.broadcast(venueTour.getVenueTourId(), "updateAlerts");
			
			sendSmsAlerts(smsMessage.toString(), venueTour);
		}
	}
	
	/**
	 * Creates and saves a forecast alert
	 * 
	 * @param venueTour
	 * @param threshold
	 * @param forecastId
	 * @param timezone
	 */
	private void saveAlert(Map<String, Integer> forecastData, VenueTourAttributeVO threshold, String forecastId, String timezone) {
		ForecastAlertVO alert = new ForecastAlertVO();
		alert.setVenueTourForecastId(forecastId);
		alert.setAttributeCode(threshold.getAttributeCode());
		alert.setValue(forecastData.get(threshold.getAttributeCode()));
		alert.setNewFlag(1);
		
		// Record the current time at the venue this alert was generated
		LocalDateTime venueDateTime = new Date().toInstant().atZone(ZoneId.of(timezone)).toLocalDateTime();
		alert.setVenueDate(java.sql.Timestamp.valueOf(venueDateTime));
		
		ForecastAlertAction fa = new ForecastAlertAction(attributes, dbConnection);
		fa.saveAlert(alert);
	}
	
	/**
	 * Builds onto a weather alert SMS message to be sent with all alerts for a tour venue
	 * 
	 * @param smsMessage
	 * @param alertCount
	 * @param forecastData
	 * @param threshold
	 */
	private void buildSmsMessage(StringBuilder smsMessage, int alertCount, Map<String, Integer> forecastData, VenueTourAttributeVO threshold) {
		smsMessage.append(alertCount > 1 ? ", " : "");
		smsMessage.append(threshold.getName()).append(": ");
		smsMessage.append(forecastData.get(threshold.getAttributeCode()));
	}
	
	/**
	 * Sends SMS alerts to notify all interested parties that
	 * a weather threshold was exceeded.
	 * 
	 * @param message
	 */
	private void sendSmsAlerts(String message, VenueTourVO venueTour) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(700);
		
		// Perfect Storm Members
		sql.append(DBUtil.SELECT_CLAUSE).append("m.*");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ps_customer c");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_customer_member_xr cm on c.customer_id = cm.customer_id");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_member m on cm.member_id = m.member_id");
		sql.append(DBUtil.WHERE_CLAUSE).append("c.customer_type_cd = ? ");
		
		// Venue Members
		sql.append(DBUtil.UNION);
		sql.append(DBUtil.SELECT_CLAUSE).append("m.*");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ps_venue v ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_customer c on v.customer_id = c.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_customer_member_xr cm on c.customer_id = cm.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_member m on cm.member_id = m.member_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("v.venue_id = ? ");
		
		// Tour Members
		sql.append(DBUtil.UNION);
		sql.append(DBUtil.SELECT_CLAUSE).append("m.*");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ps_tour t ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_customer c on t.customer_id = c.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_customer_member_xr cm on c.customer_id = cm.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("ps_member m on cm.member_id = m.member_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("t.tour_id = ? ");
		log.debug(sql);
		
		// Add the parameters
		List<Object> params = new ArrayList<>();
		params.add(CustomerType.PS.name());
		params.add(venueTour.getVenueId());
		params.add(venueTour.getTourId());
		
		// Get the interested members
		DBProcessor dbp = new DBProcessor(dbConnection);
		List<MemberVO> members = dbp.executeSelect(sql.toString(), params, new MemberVO());
		
		// Add the mobile numbers into a set in case of duplicates
		Set<String> mobileNumbers = new HashSet<>();
		for (MemberVO member: members) {
			mobileNumbers.add(member.getPhoneNumber());
		}
		
		// Send the alerts to the members
		for (String mobileNumber: mobileNumbers) {
			sendSmsMessage(message, mobileNumber);
		}
	}
	
	/**
	 * Sends a single SMS alert
	 * 
	 * @param message
	 * @param phoneNumber
	 */
	private void sendSmsMessage(String message, String phoneNumber) {
		log.debug("Sending SMS to: " + phoneNumber);
		
		try {
			SMSMessageVO sms = new SMSMessageVO();
			sms.addRecipient(phoneNumber);
			sms.setBody(message);

			MessageSender ms = new MessageSender(attributes, dbConnection);
			ms.sendMessage(sms);
			if (sms.getErrorString() != null) {
				throw new MailException(sms.getErrorString());
			}
		} catch (InvalidDataException | MailException e) {
			log.error("could not send weather alert sms", e);
		}
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
		
		// Record the current time at the venue for this forecast record
		LocalDateTime venueDateTime = new Date().toInstant().atZone(ZoneId.of(venueTour.getTimezone())).toLocalDateTime();
		forecast.setVenueDate(java.sql.Timestamp.valueOf(venueDateTime));
		
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
