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

import com.perfectstorm.action.venue.VenueAction;
import com.perfectstorm.data.VenueTourVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
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
	
	/**
	 * Field for the database schema name
	 */
	public static final String DB_SCHEMA = "DB_SCHEMA";
	
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
		log.debug("starting weather alert job");
		
		JobDataMap jobAttributes = ctx.getMergedJobDataMap();
		String message = null;
		boolean success = true;
		
		// Get the Job Instance Values
		String schema = StringUtil.checkVal(jobAttributes.getString(DB_SCHEMA));
		
		// Set the required attributes since this is out-of-band
		attributes = new HashMap<>();
		attributes.put(Constants.CUSTOM_DB_SCHEMA, schema + '.');
		
		// Get the db connection
		try (Connection dbConn = DBConnectionManager.getInstance().getConnection("quartzDS")) {
			dbConnection = new SMTDBConnection(dbConn);
			
			// Get all tour venues requiring weather alerts
			List<VenueTourVO> venueTours = getVenueTours();
			for (VenueTourVO venueTour : venueTours) {
				log.debug("handling weather alerts for: " + venueTour.getVenueTourId());
				
				// Get latest forecast data
				VenueAction va = new VenueAction(attributes, dbConnection);
				venueTour = va.getVenueTour(venueTour.getVenueTourId());
				venueTour.setCurrentConditions(va.getForecast(venueTour, null));
				
				// Get thresholds for the tour venue
				
				// Scan forecast data for thresholds that are exceeded
					// Send push notifications
					// Send SMS notifications
				
				// Save the forecast data to the db, along with the alerts they generated
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
