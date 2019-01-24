package com.wsla.scheduler.job;

// JDK 1.8.x
import java.util.HashMap;
import java.util.Map;

// Quartz 2.2.3
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.siliconmtn.db.DatabaseConnection;
// SMT Base Libs
import com.siliconmtn.http.filter.fileupload.Constants;

// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;

/****************************************************************************
 * <b>Title</b>: WSLAStandingJob.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Nightly job that looks at each job and determines whether 
 * the standing of the job is GOOD, CRITICAL or DELAYED
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 24, 2019
 * @updates:
 ****************************************************************************/

public class WSLAStandingJob extends AbstractSMTJob {

	// Members
	private Map<String, Object> attributes;
	
	/**
	 * 
	 */
	public WSLAStandingJob() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		WSLAStandingJob job = new WSLAStandingJob();
		job.log.info("Starting job test");
		
		// Assign the needed attributes
		job.attributes = new HashMap<>();
		job.attributes.put(Constants.CUSTOM_DB_SCHEMA, "custom.");

		// Get a db connection
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass("org.postgresql.Driver");
		dbc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dbc.setUserName("ryan_user_sb");
		dbc.setPassword("sqll0gin");
		job.conn = dbc.getConnection();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);
		attributes = ctx.getMergedJobDataMap().getWrappedMap();
	}

}

