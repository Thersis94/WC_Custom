package com.biomed.smarttrak.action.rss.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.solr.SolrClientBuilder;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.CustomIndexFactory;
import com.smt.sitebuilder.search.SMTIndexIntfc;

/****************************************************************************
 * <b>Title:</b> SmarttrakRSSImporter.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Smarttrak Util for processing all the RSS Data Sources.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.0
 * @since May 19, 2017
 ****************************************************************************/
public class SmarttrakRSSImporter extends CommandLineUtil {

	private static final String SCRIPT_PROPERTIES_PATH = "scripts/bmg_smarttrak/rss_config.properties";
	private static final int MAX_KEEP_DAYS = 60;
	private static final String FEEDS_OOB_INDEXER_CLASSPATH = "feedsSolrClasspath";
	private List<String> feedList;
	private long startTime;
	protected SolrClient server;
	private SMTIndexIntfc index;
	/**
	 * @param args
	 * @throws ApplicationException 
	 */
	public SmarttrakRSSImporter(String[] args) throws ApplicationException {
		super(args);
		loadProperties(SCRIPT_PROPERTIES_PATH);
		loadDBConnection(props);
		feedList = new ArrayList<>();

		// initialize the connection to the solr server
		String baseUrl = props.getProperty(Constants.SOLR_BASE_URL);
		String collection = props.getProperty(Constants.SOLR_COLLECTION_NAME);
		server = SolrClientBuilder.build(baseUrl, collection);

		index = buildIndexer();
	}

	public static void main(String [] args) {
		SmarttrakRSSImporter rsi;
		try {
			rsi = new SmarttrakRSSImporter(args);
			rsi.run();
		} catch (ApplicationException e) {
			log.error("Error Processing Code", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		startTime = Calendar.getInstance().getTimeInMillis();

		// Flush Record from solr before DB.
		flushSolrRecords(index);

		//Flush Old Filtered Articles.
		flushOldFilteredArticles();

		//Process Feeds.
		if(args == null || args.length == 0) {
			//loop all types in the enum
			for(ArticleSourceType s : ArticleSourceType.values()) {
				log.info("processing ArticleType: " + s.name());
				processFeed(s.name());
			}
		} else {
			//loop all types passed
			for(String s : args) {
				processFeed(s);
			}
		}

		//Clean up Solr Connection
		closeSolrServer();

		// clean up db conn.
		closeDBConnection();

		// send email
		sendEmail();
	}

	/**
	 * Determine the Feed we want based on the passed articleSourceType.
	 * @param s
	 */
	private void processFeed(String articleSourceType) {
		ArticleSourceType ast  = EnumUtil.safeValueOf(ArticleSourceType.class, articleSourceType);
		AbstractSmarttrakRSSFeed asf = null;
		switch (ast) {
			case PUBMED:
				asf = new PubmedDataFeed(dbConn,props,index);
				break;
			case QUERTLE:
				asf = new QuertleDataFeed(dbConn,props,index);
				break;
			case RSS:
				asf = new RSSDataFeed(dbConn,props,index);
				break;
		}
		if (asf != null) {
			log.info("****************   Beginning " + ast + " Feed ****************");
			asf.run();
			feedList.add(asf.getFeedName());
		}
	}

	/**
	 * Email administrators a simple summary
	 */
	protected void sendEmail() {
		final String br = "<br/>";
		StringBuilder msg = new StringBuilder(500);
		msg.append("<h3>SmartTRAK RSS Feeds Importer &mdash; ");
		msg.append(Convert.formatDate(new Date(startTime), Convert.DATETIME_SLASH_PATTERN));
		msg.append("</h3>");
		msg.append("<h4>Feeds processed: ").append("</h4>");
		for (String feedName : feedList) {
			msg.append(feedName).append(br);
		}
		// exec time
		msg.append("<h4>Total elapsed time: ");
		long secs = System.currentTimeMillis()-startTime;
		if (secs > 60000) {
			msg.append(fmtNo(secs/60000)).append(" minutes");
		} else {
			msg.append(fmtNo(secs/1000)).append(" seconds");
		}
		msg.append("</h4>");

		try {
			super.sendEmail(msg,null);
		} catch (Exception e) {
			log.error("Error sending RSS importer admin email", e);
		}
	}

	/**
	 * Helper method
	 * @param no
	 * @return
	 */
	protected String fmtNo(long no) {
		return NumberFormat.getNumberInstance(Locale.US).format(no);
	}

	/**
	 * Helper method that builds the Indexer to be used by the RSS OOB Scripts.
	 * @return
	 * @throws ApplicationException
	 */
	private SMTIndexIntfc buildIndexer() throws ApplicationException {
		String classPath = props.getProperty(FEEDS_OOB_INDEXER_CLASSPATH);
		log.info("Processing ... " + classPath);
		// Initialize the Indexer
		index = CustomIndexFactory.getSolrInstance(classPath, props);
		if (index.isDBConnection())
			index.setDBConnection(dbConn);
		return index;
	}

	/**
	 * Flush the RSS Feed Solr Records using the provided Index.
	 */
	private void flushSolrRecords(SMTIndexIntfc index) {

		try {
			index.purgeItems(loadFlushIds(), true);

			//commit the changes using a softCommit, and wait for Solr to finish before sending it more stuff
			server.commit(true, true, true);

		} catch (Exception ae) {
			log.error("Unable to Flush Feeds", ae);
		}
	}

	/**
	 * Called after importers have run, this ensures all transactions are
	 * committed and the server connection is closed.
	 */
	private void closeSolrServer() {
		//do a hardCommit to finalize the index
		try {
			server.commit();
			server.close();
			log.info("solr conn closed");
		} catch (Exception e) {
			log.error("Unable to commit index: ", e);
		}
	}

	/**
	 * Load the Article Ids that should be flushed from Solr.
	 * @return
	 */
	private List<String> loadFlushIds() {
		List<String> flushIds = new ArrayList<>();
		int maxKeepDays = Integer.parseInt(props.getProperty("maxKeepDays", Integer.toString(MAX_KEEP_DAYS)));
		StringBuilder sql = new StringBuilder(150);
		sql.append("select rss_article_filter_id from ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_rss_filtered_article ");
		sql.append(DBUtil.WHERE_CLAUSE).append("create_dt < current_date - ? and article_status_cd != 'K'");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, maxKeepDays);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				flushIds.add(rs.getString("rss_article_filter_id"));
			}
			log.info(String.format("retrieved %d unused filtered articles older than %d days for solr deletion", flushIds.size(), maxKeepDays));
		} catch (SQLException e) {
			log.error("Error Flushing Old Records", e);
		}

		return flushIds;
	}

	/**
	 * Flush out old Filtered Articles that are older than the flushDtCutoff
	 * value and not flagged with the Kept(K) Status.
	 * Defaulted to 60 Days for now but configurable via maxKeepDays value in
	 * properties.
	 */
	private void flushOldFilteredArticles() {

		int maxKeepDays = Integer.parseInt(props.getProperty("maxKeepDays", Integer.toString(MAX_KEEP_DAYS)));
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_rss_filtered_article ");
		sql.append(DBUtil.WHERE_CLAUSE).append("create_dt < current_date - ? and article_status_cd != 'K'");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, maxKeepDays);
			log.info(String.format("Flushing Articles older than %d days", maxKeepDays));
			int numDeleted = ps.executeUpdate();
			log.info(String.format("Flushed %d unused filtered articles older than %d days", numDeleted, maxKeepDays));
		} catch (SQLException e) {
			log.error("Error Flushing Old Records", e);
		}
	}
}