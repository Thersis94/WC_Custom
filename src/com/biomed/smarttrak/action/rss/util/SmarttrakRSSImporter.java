package com.biomed.smarttrak.action.rss.util;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.siliconmtn.util.CommandLineUtil;
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

	/**
	 * @param args
	 */
	public SmarttrakRSSImporter(String[] args) {
		super(args);
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
		if(args == null || args.length == 0) {
			//loop all types in the enum
			for(ArticleSourceType s : ArticleSourceType.values()) {
				processFeed(s.name());
			}
		} else {
			//loop all types passed
			for(String s : args) {
				processFeed(s);
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
			log.info("****************   Beginning " + ast + " Feed ****************");
			asf.run();
		}
	}
}