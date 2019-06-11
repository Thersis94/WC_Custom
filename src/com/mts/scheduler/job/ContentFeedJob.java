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

// WC Libs
import com.mts.publication.data.MTSDocumentVO;

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
		log.info("Starting processing");
		attributes = ctx.getMergedJobDataMap().getWrappedMap();
		log.info("Attributes: " + attributes.size());
		processDocuments();
		log.info("complete");
	}
	
	/**
	 * Runs the workflow for sendingthe documents
	 */
	public void processDocuments() {
		log.info("getting articles");
		List<MTSDocumentVO> docs = getArticles();
		log.info("converting to json");
		String json = convertArticlesJson(docs);
		log.info("Json: " + json);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MTSDocumentVO> getArticles() {
		// Build the SQL
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(schema).append("mts_document a ");
		sql.append("inner join sb_action b ");
		sql.append("on a.document_id = b.action_group_id and b.pending_sync_flg = 0 ");
		sql.append("inner join document c on b.action_id = c.action_id ");
		sql.append("where publish_dt between ? and ? ");
		
		Date yesterday = Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, -1);
		List<Object> vals = new ArrayList<>();
		vals.add(Convert.formatStartDate(yesterday));
		vals.add(Convert.formatEndDate(yesterday));
		log.info(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(conn);		
		return db.executeSelect(sql.toString(), vals, new MTSDocumentVO());
		
	}
	
	/**
	 * Convert the object to a json data object
	 * @param docs
	 * @return
	 */
	public String convertArticlesJson(List<MTSDocumentVO> docs) {
		Gson g = new Gson();
		return g.toJson(docs);
	}
}

