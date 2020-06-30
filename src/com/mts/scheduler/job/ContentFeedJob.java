package com.mts.scheduler.job;

// JDK 1.8.x
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import com.mts.hootsuite.HootsuiteClientVO;
import com.mts.hootsuite.HootsuiteManager;
import com.mts.hootsuite.HootsuitePostsVO;
import com.mts.hootsuite.HootsuiteRefreshTokenVO;
import com.mts.hootsuite.PostVO;
import com.siliconmtn.db.DBUtil;
// SMT Base libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.html.tool.HTMLFeedParser;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.metadata.MetadataVO;
import com.smt.sitebuilder.action.scheduler.ScheduleJobInstanceFacadeAction;
import com.smt.sitebuilder.action.tools.SiteRedirVO;
// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;

// Ganymed v3
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3FileHandle;

/****************************************************************************
 * <b>Title</b>: ContentFeedJob.java <b>Project</b>: WC_Custom <b>Description:
 * </b> Manages the sending of the new articles in the system to third parties
 * <b>Copyright:</b> Copyright (c) 2019 <b>Company:</b> Silicon Mountain
 * Technologies
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
	private boolean success;

	/**
	 * 
	 */
	public ContentFeedJob() {
		super();
	}

	/**
	 * Used for testing a post using preset attributes
	 * 
	 * @param args
	 * @throws Exception
	 */
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
		ContentFeedJob.log.info(msg);
	}

	/**
	 * get job database connection
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private void setDBConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl( "jdbc:postgresql://dev-common-sb-db.aws.siliconmtn.com:5432/wc_dev_sb?defaultRowFetchSize=25&amp;prepareThreshold=3&amp;reWriteBatchedInserts=true");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("sqll0gin");
		conn = dc.getConnection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.
	 * JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);
		attributes = ctx.getMergedJobDataMap().getWrappedMap();

		StringBuilder msg = new StringBuilder(500);
		success = true;

		// Process the data feed
		try {
			processDocuments(msg);
		} catch (Exception e) {
			success = false;
			msg.append("Failure: ").append(e.getLocalizedMessage());
		}
		// Close out the database and the transaction log
		finalizeJob(success, msg.toString());
	}

	/**
	 * Runs the workflow for sending the documents
	 * @param msg error/success message
	 * @throws Exception handled in execute
	 */
	public void processDocuments(StringBuilder msg) throws Exception {

		// Get the data fields
		String fileLoc = (String) attributes.get("FEED_RECPT");
		String feedTitle = (String) attributes.get("FEED_TITLE");
		String feedDesc = (String) attributes.get("FEED_DESC");
		String host = (String) attributes.get("SFTP_HOST");
		String user = (String) attributes.get("SFTP_USER");
		String pwd = (String) attributes.get("SFTP_PASSWORD");
		String baseUrl = (String) attributes.get("BASE_URL");

		// Append the dates to this
		Date d = new Date();
		String pattern = "yyyyMMdd";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		fileLoc += sdf.format(d) + ".json";

		// Get the docs published in the past day. Exit of no articles found
		ContentFeedVO docs = getArticles(feedTitle, feedDesc, baseUrl);
		ContentFeedVO MTSDocs = docs;
		msg.append(String.format("Loaded %d articles\n", docs.getItems().size()));
		
		// Get a list of the docs UIDs
		List<String> UIDs = docs.getUniqueIds();

		// Post all new docs using the hootsuite API
		postToHootsuite(msg, docs, baseUrl);

		// Save the documents for Infodesk publication
		if (!docs.getItems().isEmpty()) {

			List<ContentFeedItemVO> medtechDocs = new ArrayList<>();

			// Iterate through the docs.items and add the item to the medtech ArrayList if
			// they are published by MedTech
			for (ContentFeedItemVO article : docs.getItems()) {
				if ("MEDTECH-STRATEGIST".equalsIgnoreCase(article.getPublicationId())) {
					medtechDocs.add(article);
				}
			}

			// Set the docs to the array of medtechDocs
			MTSDocs.setItems(medtechDocs);

			String json = convertArticlesJson(MTSDocs);
			// Save document
			if (isManualJob) saveFile(json, fileLoc, msg);
			else saveFile(json, fileLoc, host, user, pwd, msg);
		}

		// Update the newly published articles data_feed_processed_flg database entry to
		// 1
		setSentFlags(UIDs);

		msg.append("Success");
	}

	/**
	 * Post the contents of a ContentFeedVO to social media accounts using the
	 * Hootsuite API
	 * 
	 * @param msg error/success message
	 * @param docs VO with information about the issue and a list of articles
	 * @param baseUrl the baseUrl passed with attributes
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void postToHootsuite(StringBuilder msg, ContentFeedVO docs, String baseUrl)
			throws com.siliconmtn.db.util.DatabaseException, IOException, InterruptedException {
		// Fill HootsuteClientVO
		HootsuiteClientVO hc = fillHootsuiteClientValues();

		// Fill HootsuiteClientVO
		HootsuitePostsVO hp = fillHootsuitePostVOs(docs, baseUrl);

		// Create an instance of the HootsuiteManager
		HootsuiteManager hoot = new HootsuiteManager();

		// Request a new set of tokens and then store those tokens in the database
		storeNewRefreshToken(hoot.refreshToken(msg, hc));

		// Set the clients social media ids based upon the social accounts they have
		// connected to hootsuite
		hc.setSocialProfiles(hoot.getSocialProfiles(msg));

		for (PostVO post : hp.getPosts()) {
			sequencePosts(hc, hoot, msg, post, hp);
		}
		
		// Resync the scheduler instance with the new database values
		updateScheduler(msg);
	}

	/**
	 * Sequence the posts to for each of the social media profiles
	 * @param hc
	 * @param hoot
	 * @param msg
	 * @param post
	 * @param hp
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void sequencePosts(HootsuiteClientVO hc, HootsuiteManager hoot, StringBuilder msg, PostVO post, HootsuitePostsVO hp) throws IOException, InterruptedException {
		for (Map.Entry<String, String> profile : hc.getSocialProfiles().entrySet()) {
			if ("TWITTER".equalsIgnoreCase(profile.getKey())) {
				// Post the message to Twitter
				hoot.post(msg, profile.getValue(), post, post.getTwitterFormattedString(), false);
			} else {
				// Get the list of categories for hashtags
				List<String> categories = getCategoriesList();
				// Update the Post description to use Hashtags
				addHashTags(categories, hp);
				// Post the message to Facebook and Linkedin
				hoot.post(msg, profile.getValue(), post, post.getStandardFormattedString(), true);
			}
		}
	}

	/**
	 * Resyncs the scheduler instance with the new database values
	 * @param msg error/success message
	 */
	private void updateScheduler(StringBuilder msg) {
		try {
			// The connection needs to be a quarts connection
			SMTDBConnection smtDB = new SMTDBConnection(conn);
			ScheduleJobInstanceFacadeAction s = new ScheduleJobInstanceFacadeAction(smtDB, attributes);
			s.updateScheduler((String) attributes.get("scheduleJobInstanceId"));
		} catch (Exception e) {
			msg.append("Failure: ").append(e.getLocalizedMessage());
		}
	}

	/**
	 * Add hashtags to the content of each post
	 * @param categories
	 * @param hp
	 */
	private void addHashTags(List<String> categories, HootsuitePostsVO hp) {

		for (PostVO post : hp.getPosts()) {
			post.addHashTags(categories);
		}

	}

	/**
	 * Return a list of the MTS article categories
	 * @return list of MTS article categories
	 */
	private List<String> getCategoriesList() {

		// Build the SQL
		StringBuilder sql = new StringBuilder(400);

		sql.append("select widget_meta_data_id, field_nm, field_desc from widget_meta_data ");
		sql.append("where organization_id = 'MTS' and parent_id = 'CHANNELS'");

		// Get the articles and update the links
		DBProcessor db = new DBProcessor(conn);

		List<MetadataVO> categories = db.executeSelect(sql.toString(), null, new MetadataVO());

		List<String> categoryArr = new ArrayList<>();

		for (MetadataVO category : categories) {
			categoryArr.add(category.getFieldName());
		}
		return categoryArr;
	}

	/**
	 * Populate the variables of the PostVO using the ContentFeedVO class
	 * @param docs  VO with information about the issue and a list of articles
	 * @param baseUrl
	 * @return
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	private HootsuitePostsVO fillHootsuitePostVOs(ContentFeedVO docs, String baseUrl)
			throws com.siliconmtn.db.util.DatabaseException {

		HootsuitePostsVO hp = new HootsuitePostsVO();

		List<SiteRedirVO> redirects = new ArrayList<>();

		for (ContentFeedItemVO article : docs.getItems()) {

			redirects.add(article.getRedir());

			PostVO post = new PostVO();

			post.setTitle(article.getTitle());
			post.setAuthor(article.getCreator());
			post.setDescription(article.getDescription());
			post.setShortURL(baseUrl + article.getShortUrl());
			post.setMediaLocation(baseUrl + article.getImagePath());

			hp.addPost(post);

		}

		// Create an instance of the DBProcessor and then add the shortened redirect
		// urls to the database site_redirects table
		DBProcessor dp = new DBProcessor(conn);
		dp.executeBatch(redirects, true);

		return hp;
	}

	/**
	 * Apache/2.4.41 (Ubuntu) Server at mts.dev.siliconmtn.com Port 443 Stores the
	 * new response token recieved from the hootsuite API to the database
	 * @param refreshToken
	 */
	private void storeNewRefreshToken(String refreshToken) {

		StringBuilder sql = new StringBuilder(400);

		sql.append("update schedule_job_instance_data ");
		sql.append("set value_txt = '").append(refreshToken).append("' ");
		sql.append("where schedule_job_instance_id = '").append((String) attributes.get("scheduleJobInstanceId"))
				.append("' ");
		sql.append("and schedule_job_data_id in (select schedule_job_data_id ");
		sql.append("from core.schedule_job_data sjd ");
		sql.append("where key_txt = 'HOOTSUITE_REFRESH_TOKEN')");

		DBProcessor db = new DBProcessor(conn);
		db.executeSQLCommand(sql.toString());

	}

	/**
	 * Fill the HootsuiteClientVO Bean with the instance values.
	 * @return
	 */
	private HootsuiteClientVO fillHootsuiteClientValues() {
		HootsuiteClientVO hc = new HootsuiteClientVO();
		hc.setRefreshToken(getRefreshToken());
		hc.setAccessToken((String) attributes.get("HOOTSUITE_TOKEN"));
		return hc;
	}

	/**
	 * Get the hootsuite refresh token from the database
	 * @return
	 */
	private String getRefreshToken() {

		StringBuilder sql = new StringBuilder(400);

		sql.append("select value_txt from schedule_job_instance_data ");
		sql.append("where schedule_job_instance_id = '").append((String) attributes.get("scheduleJobInstanceId"))
				.append("' ");
		sql.append("and schedule_job_data_id in (select schedule_job_data_id ");
		sql.append("from core.schedule_job_data sjd ");
		sql.append("where key_txt = 'HOOTSUITE_REFRESH_TOKEN')");

		List<Object> vals = new ArrayList<>();

		DBProcessor db = new DBProcessor(conn);

		List<HootsuiteRefreshTokenVO> tokenVO = db.executeSelect(sql.toString(), vals, new HootsuiteRefreshTokenVO());

		return tokenVO.get(0).getRefreshToken();
	}

	/**
	 * sets all the sent flags for published articles before todays job to sent
	 * @param uniqueIds
	 * @throws com.siliconmtn.db.util.DatabaseException
	 * @throws DatabaseException
	 */
	private void setSentFlags(List<String> uniqueIds)
			throws com.siliconmtn.db.util.DatabaseException, DatabaseException {

		if (uniqueIds == null || uniqueIds.isEmpty())
			return;

		String schema = (String) this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(134);

		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("mts_document ");
		sql.append("set data_feed_processed_flg = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("unique_cd in ( ")
				.append(DBUtil.preparedStatmentQuestion(uniqueIds.size())).append(" ) ");

		log.debug(" sql " + sql.toString() + "|1 and than ids : " + uniqueIds);

		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			// set the flag value of 1
			ps.setInt(1, 1);

			// loop the unique ids
			for (int x = 0; x < uniqueIds.size(); x++) {
				ps.setString(x + 2, uniqueIds.get(x));
			}

			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	/**
	 * saves the file to the file system
	 * @param json
	 * @param fileLoc
	 * @param msg error/success message
	 * @throws IOException
	 */
	protected void saveFile(String json, String fileLoc, StringBuilder msg) throws IOException {
		log.debug("saving to file");
		Path p = Paths.get(attributes.get("FEED_FILE_PATH") + fileLoc);
		try (BufferedWriter bw = Files.newBufferedWriter(p)) {
			bw.write(json);
			bw.flush();
		}
		msg.append(String.format("Wrote File: %s\n", fileLoc));
	}

	/**
	 * Write the file to the file system
	 * @param json
	 * @param fileLoc
	 * @param host
	 * @param user
	 * @param pwd
	 * @param msg error/success message
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
			if (!isAuth)
				throw new IOException("Authentication Failed");

		} catch (Exception e) {
			throw new IOException("Info deskConnection / Authentication Failed");
		}

		// Write the file to the server
		ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
		SFTPv3Client sftp = new SFTPv3Client(sftpConn);
		SFTPv3FileHandle handle = sftp.createFile(fileLoc);
		byte[] buffer = new byte[1024];
		long offset = 0;
		int i = 0;
		while ((i = bais.read(buffer)) != -1) {
			sftp.write(handle, offset, buffer, 0, i);
			offset += i;
		}

		sftp.closeFile(handle);
		sftp.close();
		msg.append(String.format("Uploaded File: %s\n", fileLoc));
	}

	/**
	 * Get the unsent articles from the database
	 * @param title
	 * @param desc
	 * @param baseUrl
	 * @return
	 */
	public ContentFeedVO getArticles(String title, String desc, String baseUrl) {
		// Build the SQL
		String schema = (String) this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		sql.append(
				"select first_nm || ' ' || last_nm as author_nm, a.unique_cd, action_desc, document_path, asset_type_cd, issue_pdf_url, publication_id, ");
		sql.append("action_nm, publish_dt, document_txt, direct_access_pth from ").append(schema)
				.append("mts_document a ");
		sql.append("inner join ").append(schema).append("mts_issue i on a.issue_id = i.issue_id ");
		sql.append("inner join sb_action b ");
		sql.append("on a.document_id = b.action_group_id and b.pending_sync_flg = 0 ");
		sql.append("inner join document c on b.action_id = c.action_id ");
		sql.append("left outer join ").append(schema);
		sql.append("mts_user u on a.author_id = u.user_id ");
		sql.append("left outer join ").append(schema);
		sql.append("mts_document_asset da on a.document_id = da.object_key_id ");
		sql.append("where publish_dt <= ? and a.data_feed_processed_flg = '0' ");
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
	 * @param baseUrl
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
	 * @param doc a ContentFeedVO
	 * @return json of ContentFeedVO
	 */
	public String convertArticlesJson(ContentFeedVO doc) {
		Gson g = new Gson();
		return g.toJson(doc);
	}
}