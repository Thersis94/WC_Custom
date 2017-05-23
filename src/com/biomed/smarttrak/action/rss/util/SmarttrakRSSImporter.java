package com.biomed.smarttrak.action.rss.util;

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
		//Load Normal Feeds.
		new RSSDataFeed(args).run();
		//Load PubMed Feeds.
		new PubmedDataFeed(args).run();
		//Load Quertle Feeds.
		new QuertleDataFeed(args).run();
	}
}