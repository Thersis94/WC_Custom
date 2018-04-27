package com.biomed.smarttrak.action.rss.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;

import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;

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
	private List<String> feedList;
	private long startTime;

	/**
	 * @param args
	 */
	public SmarttrakRSSImporter(String[] args) {
		super(args);
		feedList = new ArrayList<>();
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
				asf = new PubmedDataFeed(args);
				break;
			case QUERTLE:
				asf = new QuertleDataFeed(args);
				break;
			case RSS:
				asf = new RSSDataFeed(args);
				break;
		}
		if (asf != null) {
			// init the asf
			asf.setDbConn(dbConn);
			asf.setProps(props);
			feedList.add(asf.getFeedName());

			log.info("****************   Beginning " + ast + " Feed ****************");
			asf.run();
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
}