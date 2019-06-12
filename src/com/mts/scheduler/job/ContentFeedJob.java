package com.mts.scheduler.job;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Quartz libs
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// GSON 2.3
import com.google.gson.Gson;

// SMT Base libs
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;

// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;

/****************************************************************************
 * <b>Title</b>: ContentFeedJob.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the sending of the new articles in the system to 
 * third parties
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 11, 2019
 * @updates:
 ****************************************************************************/

public class ContentFeedJob extends AbstractSMTJob {
	private Map<String, Object> attributes;
	
	/**
	 * 
	 */
	public ContentFeedJob() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);
		attributes = ctx.getMergedJobDataMap().getWrappedMap();
		String message = "Success";
		boolean success = true;
		log.info(attributes);
		
		// Process the data feed
		try {
			processDocuments();
		} catch (Exception e) {
			message = "Fail: " + e.getLocalizedMessage();
			success = false;
		}
		
		// Close out the database and the transaction log
		try {
			this.finalizeJob(success, message);
		} catch (Exception e) { /** nothing to do here **/ }
	}
	
	/**
	 * Runs the workflow for sending the documents
	 */
	public void processDocuments() {
		ContentFeedVO docs = getArticles();
		String json = convertArticlesJson(docs);
		log.info("Json: " + json);
	}
	
	/**
	 * 
	 * @return
	 */
	public ContentFeedVO getArticles() {
		// Build the SQL
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		sql.append("select first_nm || ' ' || last_nm as author_nm, a.unique_cd, action_desc, ");
		sql.append("action_nm, publish_dt, document_txt from ").append(schema);
		sql.append("mts_document a ").append("inner join sb_action b ");
		sql.append("on a.document_id = b.action_group_id and b.pending_sync_flg = 0 ");
		sql.append("inner join document c on b.action_id = c.action_id ");
		sql.append("left outer join ").append(schema);
		sql.append("mts_user u on a.author_id = u.user_id ");
		sql.append("where publish_dt between ? and ? ");
		
		Date yesterday = Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, -1);
		List<Object> vals = new ArrayList<>();
		vals.add(Convert.formatStartDate(yesterday));
		vals.add(Convert.formatEndDate(yesterday));
		log.info(sql.length() + "|" + sql + "|" + vals);
		
		// Create the wrapper bean
		ContentFeedVO feed = new ContentFeedVO();
		feed.setTitle("MTS Publications News Feed");
		feed.setDescription("We help you better understand your world as you make key decisions impacting your day-to-day business and long-term strategic goals.");
		feed.setLink("https://www.medtechstrategist.com/");
		
		DBProcessor db = new DBProcessor(conn);		
		feed.setItems(db.executeSelect(sql.toString(), vals, new ContentFeedItemVO()));
		return feed;
		
	}
	
	/**
	 * Convert the object to a json data object
	 * @param docs
	 * @return
	 */
	public String convertArticlesJson(ContentFeedVO doc) {
		Gson g = new Gson();
		return g.toJson(doc);
	}
}

