package com.biomed.smarttrak.action.rss.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.biomed.smarttrak.action.rss.util.AbstractSmarttrakRSSFeed.MSG_KEY;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

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
	private static final String SUBJECT = "subject";
	private Map<String, Map<MSG_KEY, List<String>>> messages;
	private long startTime;

	/**
	 * @param args
	 */
	public SmarttrakRSSImporter(String[] args) {
		super(args);
		messages = new HashMap<>();
	}

	public static void main(String [] args) {
		SmarttrakRSSImporter rsi = new SmarttrakRSSImporter(args);
		rsi.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		loadProperties(SCRIPT_PROPERTIES_PATH);
		loadDBConnection(props);
		startTime = Calendar.getInstance().getTimeInMillis();

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

		// clean up db conn.
		closeDBConnection();

		// send email
		sendEmail();

		//Only send Error Email if we have errors.
		for(Entry<String, Map<MSG_KEY, List<String>>> e : messages.entrySet()) {
			//Check Errors List
			if(!e.getValue().get(MSG_KEY.Error).isEmpty()) {

				//Send Error Email.
				sendErrorEmail();

				//Send one email for the script.
				break;
			}
		}
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
				asf = new PubmedDataFeed(dbConn,props);
				break;
			case QUERTLE:
				asf = new QuertleDataFeed(dbConn,props);
				break;
			case RSS:
				asf = new RSSDataFeed(dbConn,props);
				break;
		}
		if (asf != null) {
			log.info("****************   Beginning " + ast + " Feed ****************");
			asf.run();

			messages.put(asf.getFeedName(), asf.getMessages());
		}
	}

	/**
	 * Send secondary email to recipients containing any error messages that were captured.
	 */
	protected void sendErrorEmail() {
		final String br = "<br/>";
		StringBuilder msg = new StringBuilder(500);
		msg.append("<h3>SmartTRAK RSS Feeds Importer Errors &mdash; ");
		msg.append(Convert.formatDate(new Date(startTime), Convert.DATETIME_SLASH_PATTERN));
		msg.append("</h3>");
		for (Entry<String, Map<MSG_KEY, List<String>>> feedData : messages.entrySet()) {

			//If this is a single Run, update the Subject Line to reflect the run.
			if(messages.size() == 1) {
				String subject = props.getProperty(SUBJECT);
				props.replace(SUBJECT, StringUtil.join(subject, " - ", feedData.getKey()));
			}
			msg.append(feedData.getKey()).append(br);
			for(String m : feedData.getValue().get(MSG_KEY.Error)) {
				msg.append(m).append(br);
			}
		}

		try {
			super.sendEmail(msg,null);
		} catch (Exception e) {
			log.error("Error sending RSS importer admin email", e);
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
		for (Entry<String, Map<MSG_KEY, List<String>>> feedData : messages.entrySet()) {

			//If this is a single Run, update the Subject Line to reflect the run.
			if(messages.size() == 1) {
				String subject = props.getProperty(SUBJECT);
				props.replace(SUBJECT, StringUtil.join(subject, " - ", feedData.getKey()));
			}
			msg.append(feedData.getKey()).append(br);
			for(String m : feedData.getValue().get(MSG_KEY.Standard)) {
				msg.append(m).append(br);
			}
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