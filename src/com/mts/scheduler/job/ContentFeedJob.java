package com.mts.scheduler.job;

// JDK 1.8.x
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Quartz libs
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// GSON 2.3
import com.google.gson.Gson;
// SMT Base libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.html.tool.HTMLFeedParser;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;

// Ganymed v3
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3FileHandle;

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

	// Members
	private Map<String, Object> attributes = new HashMap<>();
	private boolean isManualJob = false;

	/**
	 * 
	 */
	public ContentFeedJob() {
		super();
	}


	public static void main(String[] args) throws Exception {
		// Set job Information
		ContentFeedJob cfj = new ContentFeedJob();
		cfj.attributes.put("FEED_RECPT", "MTS_DOCUMENT_");
		cfj.attributes.put("FEED_TITLE", "MTS Publications News Feed");
		cfj.attributes.put("FEED_DESC", "We help you better understand your world as you make key decisions impacting your day-to-day business and long-term strategic goals.");
		cfj.attributes.put("BASE_URL", "https://www.mystrategist.com");
		cfj.attributes.put("FEED_FILE_PATH", "/home/etewa/Desktop/");
		cfj.attributes.put("START_DT", "2019-07-15");
		cfj.attributes.put("END_DT", "2019-08-31");
		cfj.attributes.put(Constants.CUSTOM_DB_SCHEMA, "custom.");
		cfj.isManualJob = true;
		cfj.setDBConnection();

		// Run the job
		cfj.processDocuments();
	}

	/**
	 * get job database connection
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private void setDBConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescnedo_dev08282019_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("sqll0gin");
		conn = dc.getConnection();
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
		finalizeJob(success, message);
	}

	/**
	 * Runs the workflow for sending the documents
	 * @throws IOException 
	 */
	public void processDocuments() throws IOException {
		// Get the data fields
		String fileLoc = (String)attributes.get("FEED_RECPT");
		String feedTitle = (String)attributes.get("FEED_TITLE");
		String feedDesc = (String)attributes.get("FEED_DESC");
		String host = (String)attributes.get("SFTP_HOST");
		String user = (String)attributes.get("SFTP_USER");
		String pwd = (String)attributes.get("SFTP_PASSWORD");
		String baseUrl = (String)attributes.get("BASE_URL");

		// Append the dates to this
		Date d = new Date();
		String pattern = "yyyyMMdd";
		SimpleDateFormat sdf =new SimpleDateFormat(pattern);
		fileLoc += sdf.format(d) + ".json";

		// Get the docs published in the past day.  Exit of no articles found
		ContentFeedVO docs = getArticles(feedTitle, feedDesc, baseUrl);
		if (docs.getItems().isEmpty()) return;

		String json = convertArticlesJson(docs);

		// Save document
		if (isManualJob) saveFile(json, fileLoc);
		else saveFile(json, fileLoc, host, user, pwd);
	}

	/** 
	 * saves the file to the file system 
	 * @param json
	 * @param fileLoc
	 * @throws IOException
	 */
	protected void saveFile(String json, String fileLoc) throws IOException {
		Path p = Paths.get(attributes.get("FEED_FILE_PATH") + fileLoc);
		try(BufferedWriter bw = Files.newBufferedWriter(p)) {
			bw.write(json);
			bw.flush();
		}
	}

	/**
	 * Write the file to the file system
	 * @param json
	 * @param fileLoc
	 * @throws IOException
	 */
	protected void saveFile(String json, String fileLoc, String host, String user, String pwd) 
			throws IOException {
		// Connect to the server and authenticate
		Connection sftpConn = new Connection(host);

		try {
			sftpConn.connect(null, 3000, 0);
			boolean isAuth = sftpConn.authenticateWithPassword(user, pwd);
			if (! isAuth) throw new IOException("Authentication Failed");

		} catch (Exception e) {
			throw new IOException("Connection / Authentication Failed");
		}

		// Write the file to the server
		ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
		SFTPv3Client sftp = new SFTPv3Client(sftpConn);
		SFTPv3FileHandle handle = sftp.createFile(fileLoc);
		byte[] buffer = new byte[1024];
		long offset=0;
		int i=0;
		while ((i = bais.read(buffer)) != -1) {
			sftp.write(handle, offset, buffer, 0, i);
			offset += i;
		}

		sftp.closeFile(handle);
		sftp.close();
	}

	/**
	 * 
	 * @return
	 */
	public ContentFeedVO getArticles(String title, String desc, String baseUrl) {
		// Build the SQL
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		sql.append("select first_nm || ' ' || last_nm as author_nm, a.unique_cd, action_desc, ");
		sql.append("action_nm, publish_dt, document_txt from ").append(schema).append("mts_document a ");
		sql.append("inner join ").append(schema).append("mts_issue i on a.issue_id = i.issue_id ");
		sql.append("inner join sb_action b ");
		sql.append("on a.document_id = b.action_group_id and b.pending_sync_flg = 0 ");
		sql.append("inner join document c on b.action_id = c.action_id ");
		sql.append("left outer join ").append(schema);
		sql.append("mts_user u on a.author_id = u.user_id ");
		sql.append("where publish_dt between ? and ? and publication_id = 'MEDTECH-STRATEGIST' ");
		sql.append("order by publish_dt ");

		List<Object> vals = new ArrayList<>();
		if (isManualJob) {
			vals.add(Convert.formatStartDate((String)attributes.get("START_DT")));
			vals.add(Convert.formatEndDate((String)attributes.get("END_DT")));
		} else {
			Date startDt = Convert.formatStartDate((String)attributes.get("START_DT")); //this can be set in the admintool for a manual re-run, to catch up missed days.
			Date yesterday = Convert.formatDate(new Date(), Calendar.DAY_OF_YEAR, -1);
			vals.add(Convert.formatStartDate(startDt != null ? startDt : yesterday));
			vals.add(Convert.formatEndDate(yesterday));
		}

		log.debug(sql.length() + "|" + sql + "|" + vals);

		// Create the wrapper bean
		ContentFeedVO feed = new ContentFeedVO();
		feed.setTitle(title);
		feed.setDescription(desc);
		feed.setLink(baseUrl);
		feed.setLastBuildDate(new Date());
		feed.setLocale("en-US");

		// Get the articles and update the links
		DBProcessor db = new DBProcessor(conn);		
		feed.setItems(db.executeSelect(sql.toString(), vals, new ContentFeedItemVO()));
		updateRelativeLinks(feed, baseUrl);
		log.debug("Number Articles: " + feed.getItems().size());

		return feed;
	}

	/**
	 * Updates the relative URLs/links to be fully qualified
	 * @param feed
	 */
	protected void updateRelativeLinks(ContentFeedVO feed, String baseUrl) {
		// Update the relative links and image sources to be fully qualified
		for (ContentFeedItemVO item : feed.getItems()) {
			HTMLFeedParser parser = new HTMLFeedParser();
			String html = parser.updateDocumentPaths(baseUrl, item.getContent());
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

