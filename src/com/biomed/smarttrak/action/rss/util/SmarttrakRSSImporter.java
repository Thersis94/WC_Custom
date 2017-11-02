package com.biomed.smarttrak.action.rss.util;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.siliconmtn.util.CommandLineUtil;

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
		if(args.length == 0) {
			//Load Normal Feeds.
			new RSSDataFeed(args).run();
			//Load PubMed Feeds.
			new PubmedDataFeed(args).run();
			//Load Quertle Feeds.
			new QuertleDataFeed(args).run();
		} else { 
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
		try {
			ArticleSourceType ast = ArticleSourceType.valueOf(articleSourceType);
			AbstractSmarttrakRSSFeed asf = null;
			switch(ast) {
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
			if(asf != null) {
				asf.run();
			}
		} catch(Exception e) {
			log.error(articleSourceType + " is not a valid Article Source Type.  Must be PUBMED, QUERTLE or RSS.");
		}
	}
}