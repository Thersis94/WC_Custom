package com.mts.scheduler.job;

// JDK 1.8.x
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import com.siliconmtn.html.tool.HTMLFeedParser;
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
	/**
	 * Base URL for links and images
	 */
	private static final String BASE_URL = "https://www.innovationinmedtech.com/";
	
	// Members
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
	 * @throws IOException 
	 */
	public void processDocuments() throws IOException {
		String fileLoc = (String)attributes.get("FEED_RECPT");
		String feedTitle = (String)attributes.get("FEED_TITLE");
		String feedDesc = (String)attributes.get("FEED_DESC");
		
		ContentFeedVO docs = getArticles(feedTitle, feedDesc);
		String json = convertArticlesJson(docs);
		
		// Save document
		saveFile(json, fileLoc);
	}
	
	/**
	 * Write the file to the file system
	 * @param json
	 * @param fileLoc
	 * @throws IOException
	 */
	protected void saveFile(String json, String fileLoc) throws IOException {
		File file = new File (fileLoc);
		try (FileWriter fw = new FileWriter(file)) {
			try (BufferedWriter out = new BufferedWriter(fw)) {
				out.write(json);
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public ContentFeedVO getArticles(String title, String desc) {
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
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		// Create the wrapper bean
		ContentFeedVO feed = new ContentFeedVO();
		feed.setTitle(title);
		feed.setDescription(desc);
		feed.setLink(BASE_URL);
		feed.setLastBuildDate(new Date());
		feed.setLocale("en-US");
		
		// Get the articles and update the links
		DBProcessor db = new DBProcessor(conn);		
		feed.setItems(db.executeSelect(sql.toString(), vals, new ContentFeedItemVO()));
		updateRelativeLinks(feed);

		return feed;
		
	}
	
	/**
	 * Updates the relative URLs/links to be fully qualified
	 * @param feed
	 */
	protected void updateRelativeLinks(ContentFeedVO feed) {
		// Update the relative links and image sources to be fully qualified
		for (ContentFeedItemVO item : feed.getItems()) {
			HTMLFeedParser parser = new HTMLFeedParser();
			String html = parser.updateDocumentPaths(BASE_URL, item.getContent());
			item.setContent(html);
		}
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

