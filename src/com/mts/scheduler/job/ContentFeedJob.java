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
import org.quartz.utils.DBConnectionManager;

// GSON 2.3
import com.google.gson.Gson;
import com.mts.hootsuite.HootsuiteClientVO;
import com.mts.hootsuite.HootsuiteManager;
import com.mts.hootsuite.HootsuitePostsVO;
import com.mts.hootsuite.HootsuiteRefreshTokenVO;
import com.mts.hootsuite.PostVO;
import com.mts.publication.data.CategoryVO;
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
import com.smt.sitebuilder.action.scheduler.ScheduleJobInstanceFacadeAction;
import com.smt.sitebuilder.action.tools.SiteRedirVO;
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
		
		log.info("---Constructor---");
	}


	public static void main(String[] args) throws Exception {
		// Set job Information
		ContentFeedJob cfj = new ContentFeedJob();
		cfj.attributes.put("FEED_RECPT", "test-dir");
		cfj.attributes.put("FEED_TITLE", "MTS Publications News Feed");
		cfj.attributes.put("FEED_DESC", "We help you better understand your world as you make key decisions impacting your day-to-day business and long-term strategic goals.");
		cfj.attributes.put("BASE_URL", "https://www.mystrategist.com");
		cfj.attributes.put("FEED_FILE_PATH", "/home/justinjeffrey/Desktop/");
		cfj.attributes.put("scheduleJobInstanceId", "36742e02b91b494e7f000101744bf6c5");
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
		dc.setUrl("jdbc:postgresql://dev-common-sb-db.aws.siliconmtn.com:5432/wc_dev_sb?defaultRowFetchSize=25&amp;prepareThreshold=3&amp;reWriteBatchedInserts=true");
		dc.setUserName("wc_user_dev");
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
		
		log.info("running execute");
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
		
		log.info((String)attributes.get("scheduleJobInstanceId"));

		// Append the dates to this
		Date d = new Date();
		String pattern = "yyyyMMdd";
		SimpleDateFormat sdf =new SimpleDateFormat(pattern);
		fileLoc += sdf.format(d) + ".json";
		
		// Get the docs published in the past day.  Exit of no articles found
		ContentFeedVO docs = getArticles(feedTitle, feedDesc, baseUrl);
		msg.append(String.format("Loaded %d articles\n", docs.getItems().size()));
		
//		log.info("before postToHootsuite");
		postToHootsuite(docs, baseUrl);
		
		// Save the documents for Infodesk publication
		if (!docs.getItems().isEmpty()) {
			
			List<ContentFeedItemVO> medtechDocs = new ArrayList<>();
			
			// Iterate through the docs.items and add the item to the medtech ArrayList if they are published by MedTech
			for(ContentFeedItemVO article : docs.getItems()) {
				if(article.getPublicationId().equalsIgnoreCase("MEDTECH-STRATEGIST")) {
					medtechDocs.add(article);
				}
			}
			
			// Set the docs to the array of medtechDocs
			docs.setItems(medtechDocs);
			
			String json = convertArticlesJson(docs);
			// Save document
//			if (isManualJob) saveFile(json, fileLoc, msg);
//			else saveFile(json, fileLoc, host, user, pwd, msg);
		}
		
		// Update the newly published articles data_feed_processed_flg database entry to 1
//		setSentFlags(docs.getUniqueIds());

		msg.append("Success");
	}

	/**
	 * Post the contents of a ContentFeedVO to social media accounts using the Hootsuite API
	 * @param baseUrl 
	 * @param docs 
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * @throws IOException 
	 * 
	 */
	private void postToHootsuite(ContentFeedVO docs, String baseUrl) throws com.siliconmtn.db.util.DatabaseException, IOException {

		// Create HootsuteClientVO
		HootsuiteClientVO hc = new HootsuiteClientVO();
		// Fill HootsuiteClientVO
		setHootsuiteClientValues(hc);

		HootsuitePostsVO hp = fillPostVOs(docs, baseUrl);
		
		// Create an instance of the HootsuiteManager
		HootsuiteManager hoot = new HootsuiteManager();
		// Request a new set of tokens and then store those tokens in the database
		storeNewRefreshToken(hoot.refreshToken(hc));
		// Set the clients social media ids based upon the social accounts they have connected to hootsuite
		hc.setSocialProfiles(hoot.getSocialProfiles());
		
		
		
		for(PostVO post : hp.getPosts()) {
			for (Map.Entry<String, String> profile : hc.getSocialProfiles().entrySet()) {
				if(profile.getKey().equalsIgnoreCase("TWITTER")) {
					// Post the message to Twitter
//					hoot.post(profile.getValue(), post, hc, post.getTwitterFormattedString(), false);
				} else {
					// Get the list of categories for hashtags
					List<String> categories = getCategoriesList();
					// Update the Post description to use Hashtags
					addHashTags(categories, hp);
					// Post the message to Facebook and Linkedin
					hoot.post(profile.getValue(), post, hc, post.getStandardFormattedString(), false);
				}		
		    }
		}
		
		
//		ScheduleJobInstanceFacadeAction s = new ScheduleJobInstanceFacadeAction((SMTDBConnection) conn, attributes);
//		s.updateScheduler((String)attributes.get("scheduleJobInstanceId"));
		
		
	}

	/**
	 * Add hashtags to the content of each post
	 * @param categories
	 * @param hp
	 */
	private void addHashTags(List<String> categories, HootsuitePostsVO hp) {
		
		for(PostVO post : hp.getPosts()) {
			post.addHashTags(categories);
		}
		
		
	}

	/**
	 * Return a list of the MTS article categories
	 * @return
	 */
	private List<String> getCategoriesList() {
		
		// Build the SQL
				StringBuilder sql = new StringBuilder(400);
				
				sql.append("select widget_meta_data_id, field_nm, field_desc from widget_meta_data ");
				sql.append("where organization_id = 'MTS' and parent_id = 'CHANNELS'");
				
				// Get the articles and update the links
				DBProcessor db = new DBProcessor(conn);	
				
				List<CatVO> categories = db.executeSelect(sql.toString(), null, new CatVO());
				
				List<String> categoryArr = new ArrayList<>();
				
				for(CatVO category : categories) {
					log.info(category.getFieldName());
					categoryArr.add(category.getFieldName());
				}
				
		return categoryArr;
	}


	/**
	 * Populate the variables of the PostVO using the ContentFeedVO class
	 * @param docs
	 * @param baseUrl
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 */
	private HootsuitePostsVO fillPostVOs(ContentFeedVO docs, String baseUrl) throws com.siliconmtn.db.util.DatabaseException {
		
		HootsuitePostsVO hp = new HootsuitePostsVO();
		
		List<SiteRedirVO> redirects = new ArrayList<>();
		
		for(ContentFeedItemVO article : docs.getItems()) {
			
			redirects.add(article.getRedir());
			
			PostVO post = new PostVO();
			
			post.setTitle(article.getTitle());
			post.setAuthor(article.getCreator());
			post.setDescription(article.getDescription());
			post.setShortURL(baseUrl + article.getShortUrl());
//			post.setMediaLocation(baseUrl + article.getImagePath());
			
			hp.addPost(post);
			
		}
		

		// Create an instance of the DBProcessor and then add the shortened redirect urls to the database site_redirects table
		DBProcessor dp = new DBProcessor(conn);
//		dp.executeBatch(redirects, true);
		
		return hp;
	}


	/**
	 * Stores the new response token recieved from the hootsuite API to the database
	 * @param refreshToken
	 */
	private void storeNewRefreshToken(String refreshToken) {
		
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		
		sql.append("update schedule_job_instance_data ");
		sql.append("set value_txt = '").append(refreshToken).append("' ");
		sql.append("where schedule_job_instance_id = '").append((String)attributes.get("scheduleJobInstanceId")).append("' ");
		sql.append("and schedule_job_data_id in (select schedule_job_data_id ");
		sql.append("from core.schedule_job_data sjd ");
		sql.append("where key_txt = 'HOOTSUITE_REFRESH_TOKEN')");
		
		DBProcessor db = new DBProcessor(conn);
		db.executeSQLCommand(sql.toString());
		
	}


	/**
	 * Fill the HootsuiteClientVO Bean with the instance values.
	 * @param hc
	 */
	private void setHootsuiteClientValues(HootsuiteClientVO hc) {
		hc.setRefreshToken(getRefreshToken());
		hc.setAccessToken((String)attributes.get("HOOTSUITE_TOKEN"));
	}

	/**
	 * Get the hootsuite refresh token from the database
	 * @return
	 */
	private String getRefreshToken() {
		
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		
		sql.append("select value_txt from schedule_job_instance_data ");
		sql.append("where schedule_job_instance_id = '").append((String)attributes.get("scheduleJobInstanceId")).append("' ");
		sql.append("and schedule_job_data_id in (select schedule_job_data_id ");
		sql.append("from core.schedule_job_data sjd ");
		sql.append("where key_txt = 'HOOTSUITE_REFRESH_TOKEN')");
		
		log.info(sql.toString());
		
		List<Object> vals = new ArrayList<>();
		
		DBProcessor db = new DBProcessor(conn);
		
		List<HootsuiteRefreshTokenVO> tokenVO = db.executeSelect(sql.toString(), vals, new HootsuiteRefreshTokenVO());
		
		return tokenVO.get(0).getRefreshToken();
	}


	/**
	 * sets all the sent flags for published articles before todays job to sent
	 * @param uniqueIds 
	 * @throws com.siliconmtn.db.util.DatabaseException 
	 * 
	 */
	private void setSentFlags(List<String> uniqueIds) throws com.siliconmtn.db.util.DatabaseException, DatabaseException {
		
		if(uniqueIds == null || uniqueIds.isEmpty())return;
		
		String schema = (String)this.attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(134);
				
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("mts_document ");
		sql.append("set data_feed_processed_flg = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("unique_cd in ( ").append(DBUtil.preparedStatmentQuestion(uniqueIds.size())).append(" ) ");
		
		log.debug(" sql " + sql.toString() +"|1 and than ids : " +uniqueIds);
		
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			//set the flag value of 1
			ps.setInt(1, 1);
			
			//loop the unique ids
			for(int x =0 ; x < uniqueIds.size() ; x++) {
				ps.setString(x+2, uniqueIds.get(x));
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
		sql.append("select first_nm || ' ' || last_nm as author_nm, a.unique_cd, action_desc, document_path, asset_type_cd, issue_pdf_url, publication_id, ");
		sql.append("action_nm, publish_dt, document_txt, direct_access_pth from ").append(schema).append("mts_document a ");
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