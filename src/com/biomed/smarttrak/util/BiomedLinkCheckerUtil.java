package com.biomed.smarttrak.util;

import java.util.List;

import org.apache.log4j.Logger;

/****************************************************************************
 * Title: ManageLinkCheckerUtil.java <p/>
 * Project: WC_Custom <p/>
 * Description: Converts public facing links destination to their corresponding manage side link destination <p/>
 * Copyright: Copyright (c) 2018<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Mar 2, 2018
 ****************************************************************************/

public class BiomedLinkCheckerUtil {
	private static final Logger log = Logger.getLogger(BiomedLinkCheckerUtil.class.getName());	
	
	public BiomedLinkCheckerUtil() {
		//default constructor
	}

	public String checkSiteLinks(String text, List<String> siteDomains) {
		log.debug("Checking site links...");
		StringBuilder updatedText = new StringBuilder(text.length() +  250);
		
		//loop site domains and determine if any matches found
		
		//account for relative links
		
		//parse out the start location
		
		//find the end location
		
		//build the appropriate manage link
		
		return updatedText.toString();
	}
	
	private String buildManageLink(String link, String baseDomain) {
		String manageLink = "";
		//prepend /manage
		
		//if present, convert section page to appropriate admin section(markets, companies, updates, etc.)
		
		//if present, convert the qs param to appropriate admin section id
		return manageLink;
	}
}
