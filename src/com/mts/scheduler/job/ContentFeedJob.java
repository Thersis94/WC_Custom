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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Quartz libs
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// GSON 2.3
import com.google.gson.Gson;
import com.mts.publication.data.MTSDocumentVO;
import com.siliconmtn.db.DBUtil;
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
		cfj.attributes.put("FEED_FILE_PATH", "/home/ryan/Desktop/");
		cfj.attributes.put(Constants.CUSTOM_DB_SCHEMA, "custom.");
		cfj.isManualJob = true;
		cfj.setDBConnection();

		// Run the job
		StringBuilder msg = new StringBuilder(500);
		cfj.processDocuments(msg);
		cfj.log.info(msg);
	}

	/**
	 * get job database connection
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private void setDBConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_dev112019_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
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
		StringBuilder msg = new StringBuilder(500);
		boolean success = true;

		// Process the data feed
		try {
			processDocuments(msg);
		} catch (Exception e) {
			msg.append("Failure: ").append(e.getLocalizedMessage());
			success = false;
		}

		// Close out the database and the transaction log
		finalizeJob(success, msg.toString());
	}

	/**
	 * Runs the workflow for sending the documents
	 * @throws IOException 
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 */
	public void processDocuments(StringBuilder msg) throws Exception {
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
		msg.append(String.format("Loaded %d articles\n", docs.getItems().size()));

		if (!docs.getItems().isEmpty()) {
			String json = convertArticlesJson(docs);
			// Save document
			if (isManualJob) saveFile(json, fileLoc, msg);
			else saveFile(json, fileLoc, host, user, pwd, msg);
		}
		
		setSentFlags();

		msg.append("Success");
	}

	/**
	 * sets all the sent flags for published articles before todays job to sent
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * 
	 */
	private void setSentFlags() throws DatabaseException, com.siliconmtn.db.util.DatabaseException {
		
		MTSDocumentVO vo = new MTSDocumentVO();
		
		vo.setSentFlag(1);
		vo.setPublishDate(Convert.formatEndDate(new Date()));
		
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(134);
				
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("mts_document ");
		sql.append("set sent_flg = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("publish_dt <= ? and sent_flg = '0' ");
		
		DBProcessor db = new DBProcessor(conn);		
		List<String> fields  = new ArrayList<>();
		fields.add("sent_flg");
		fields.add("publish_dt");
		
		db.executeSqlUpdate(sql.toString(), vo, fields);
	}


	/** 
	 * saves the file to the file system 
	 * @param json
	 * @param fileLoc
	 * @throws IOException
	 */
	protected void saveFile(String json, String fileLoc, StringBuilder msg) throws IOException {
		log.debug("saving to file");
		Path p = Paths.get(attributes.get("FEED_FILE_PATH") + fileLoc);
		try(BufferedWriter bw = Files.newBufferedWriter(p)) {
			bw.write(json);
			bw.flush();
		}
		msg.append(String.format("Wrote File: %s\n", fileLoc));
	}

	/**
	 * Write the file to the file system
	 * @param json
	 * @param fileLoc
	 * @throws IOException
	 */
	protected void saveFile(String json, String fileLoc, String host, String user, String pwd, StringBuilder msg) 
			throws IOException {
		log.debug("saving to infodesk");
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
		msg.append(String.format("Uploaded File: %s\n", fileLoc));
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
		sql.append("where publish_dt <= ? and publication_id = 'MEDTECH-STRATEGIST' and a.sent_flg = '0' ");
		sql.append("order by publish_dt ");

		List<Object> vals = new ArrayList<>();
		
		vals.add(Convert.formatEndDate(new Date()));

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

