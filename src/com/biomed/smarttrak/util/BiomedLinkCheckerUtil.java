package com.biomed.smarttrak.util;

//jdk 1.8
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//log4j 1.x
import org.apache.log4j.Logger;

//WC Custom
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.siliconmtn.db.pool.SMTDBConnection;
//SMT base libs
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.http.SMTServletRequest;

//WebCrescendo
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * Title: ManageLinkCheckerUtil.java <p/>
 * Project: WC_Custom <p/>
 * Description: Converts public facing links destination to their 
 * corresponding manage side link destination <p/>
 * Copyright: Copyright (c) 2018<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Mar 2, 2018
 ****************************************************************************/

public class BiomedLinkCheckerUtil {
	private static final Logger log = Logger.getLogger(BiomedLinkCheckerUtil.class.getName());	
	private SMTDBConnection dbConn;
	private static final String ANCHOR_END_REGEX = ".*?['\"]>";
	private static final String QS_PARAM_TOKEN = "/"+SMTServletRequest.DIRECTORY_KEY+"/";
	private static final String MANAGE_PATH = "/manage";
	private String baseDomain;
	private List<String> siteAliases;
	
	/**
	 * Basic constructor used when there is no site vo available.
	 * This limits the functionality of the util, preventing 
	 * the following functions producing legitimate results:
	 * modifyAbsoluteLinks
	 * modifyPlainURL
	 */
	public BiomedLinkCheckerUtil() {
		siteAliases = new ArrayList<>();
	}
	
	/**
	 * Overloaded constructor that takes a Connection object
	 * @param conn
	 * @param site
	 */
	public BiomedLinkCheckerUtil(Connection conn, SiteVO site) {
		this(new SMTDBConnection(conn), site);
	}
	
	/**
	 * Constructor to initialize class
	 * @param dbConn
	 * @param site
	 */
	public BiomedLinkCheckerUtil(SMTDBConnection dbConn, SiteVO site) {
		this.dbConn = dbConn;
		siteAliases = new ArrayList<>(); 
		
		//populate list of site domains/aliases
		loadSiteAliases(site);
	}

	/**
	 * Wrapper method, searches given text for any matching URL links, absolute or relative. Then modifies to them
	 * to the appropriate corresponding manage side version.
	 * @param text
	 * @return - The modified text with updated links if applicable, otherwise the original text
	 */
	public String modifySiteLinks(String text) {		
		//attempt to find any matches for public site links(absolute or relative)
		String modifiedText = modifyRelativeLinks(text);
		modifiedText = modifyAbsoluteLinks(modifiedText);
		
		return modifiedText;
	}

	/**
	 * Searches text content for any public links and returns a modified text with links converted to manage links.
	 * @param text
	 * @param site
	 * @return
	 */
	public String modifyRelativeLinks(String text){
		if(StringUtil.isEmpty(text)) return text;
		log.debug("Checking relative links...");
		
		//replace any public site links with manage links and return
		return transposeContentLinks(buildRelativePattern(), text, false);		
	}
	
	/**
	 * Searches text content for any public links and returns a modified text with links converted to manage links.
	 * @param text
	 * @return
	 */
	public String modifyAbsoluteLinks(String text){
		if(StringUtil.isEmpty(text)) return text;
		log.debug("Checking absolute links...");
		
		//replace any public site links with manage links and return
		return transposeContentLinks(buildAbsolutePattern(), text, true);
	}
	
	/**
	 * Modifies a single plain URL string to it's manage side equivalent. Useful when an exact URL is known versus buried in content text.
	 * Example: app.smarttrak.com/analysis/qs/1234 -> app.smarttrak.com/manage?actionType=insights&insightId=1234  
	 * @param url
	 * @return
	 */
	public String modifyPlainURL(String url) {
		//ensure not empty and we are dealing with a URL path
		if(StringUtil.isEmpty(url) || url.indexOf('/') == -1) return url;
		log.debug("Checking plain url...");
		
		//determine the type of URL, if absolute ensure it's an aliases for the site
		boolean isAbsoluteURL = url.indexOf('/') == 0 ? false : true;
		if(isAbsoluteURL) {
			boolean matchFound = false;
			for (String alias : siteAliases) {
				if(url.indexOf(alias) > -1) {
					matchFound = true;
					break;
				}
			}
			if(!matchFound) return url;
		}
				
		//transpose it with appropriate params to pass
		return transposeURL(url, isAbsoluteURL);
	}
	
	/**
	 * Updates content by converting the matched public link to it's manage side equivalent
	 * Examples: companies/qs/73139 -> /manage?actionType="companyAdmin&companyId=73139"
	 * www.smarttrak.net/products -> www.smarttrak.net/manage?actionType=productAdmin
	 * @param publicLink
	 * @param text
	 * @param isAbsoluteLink
	 * @return
	 */
	private String transposeContentLinks(String searchPattern, String text, boolean isAbsoluteLink) {
		Pattern p = Pattern.compile(searchPattern);
		Matcher m = p.matcher(text);
		String publicLink;

		//find all public links and update
		try {
			while(m.find()) {
				publicLink = m.group();
				
				// matched link already points to manage tool, skip		
				if(publicLink.indexOf(MANAGE_PATH) > -1) continue;
			
				if(log.isTraceEnabled()) log.trace("public link to transpose: " + publicLink);
				StringBuilder manageLink = new StringBuilder(publicLink.length() + 150);
				int endIndex =  publicLink.length() -2; //account for anchor end tag plus quote
		
				//add the manage path in correct location
				appendManagePath(publicLink, manageLink, isAbsoluteLink);
				
				//replace the section page with corresponding manage section
				boolean sectionFound = appendSectionToken(publicLink, manageLink, endIndex);
				
				//if no matching manage section found, handle appending any remaining url values(page, query params, etc.) here
				appendEndURLValues(publicLink, manageLink, isAbsoluteLink, sectionFound, endIndex);
				manageLink.append("\">"); //end the anchor tag
				
				//replace the public link with the admin link
				if(log.isTraceEnabled()) log.trace("transposed manage link: " + manageLink);
				text = text.replace(publicLink, manageLink.toString());	
			}
		}catch(Exception e) {//If an error occurs catch it here, and return original text
			log.error("Error attempting to transpose public link to manage link: " + e);
			return text;
		}
		
		return text;
	}
	
	/**
	 * Transposes the direct URL to corresponding manage section
	 * Example: companies/qs/73139 -> /manage?actionType="companyAdmin&companyId=73139"
	 * @param url
	 * @param isAbsoluteURL
	 * @return
	 */
	private String transposeURL(String url, boolean isAbsoluteURL) {		
		// passed url already points to manage tool, return		
		if(url.indexOf(MANAGE_PATH) > -1) return url;
		if(log.isTraceEnabled()) 
			log.trace("given URL to transpose: " + url);	
		
		StringBuilder manageURL = new StringBuilder(url.length() + 150);
		int endIndex =  url.length();
		try {
			//add the manage path in correct location
			appendManagePath(url, manageURL, isAbsoluteURL);
			
			//replace the section page with corresponding manage section
			boolean sectionFound = appendSectionToken(url, manageURL, endIndex);
			
			//if no matching manage section found, handle appending any remaining url values(page, query params, etc.) here
			appendEndURLValues(url, manageURL, isAbsoluteURL, sectionFound, endIndex);			
			if(log.isTraceEnabled()) log.trace("transposed manage URL: " + manageURL);
		
		}catch(Exception e) {//If an error occurs catch it here, and return original url
			log.error("Error attempting to transpose given URL to manage URL: " + e);
			return url;
		}
		
		return manageURL.toString();
	}
	
	/**
	 * Appends the '/manage' path in appropriate location based on relative or absolute domain 
	 * @param publicLink
	 * @param manageLink
	 * @param isAbsoluteSearch
	 */
	private void appendManagePath(String publicLink, StringBuilder manageLink, boolean isAbsoluteSearch) {
		if(isAbsoluteSearch) {
			int endIndex = publicLink.lastIndexOf('.') + 4; //top level domain
			baseDomain = publicLink.substring(0,  endIndex);	
			manageLink.append(baseDomain).append(MANAGE_PATH); //append manage		
		}else {
			manageLink.append(MANAGE_PATH); //prepend manage
		}
	}
	
	/**
	 * Appends the appropriate manage section if a matching public page is found
	 * @param publicLink
	 * @param manageLink
	 * @param endIndex
	 * @return true or false indicating if a matching section was found
	 */
	private boolean appendSectionToken(String publicLink, StringBuilder manageLink, int endIndex) {
		boolean sectionFound = false;
		String tokenMinusEndSlash = "";
		for(Section section : Section.values()) {
			/*skip over sections that don't have 'tools' in path when the link does. Prevent links that point to manage 
			 *sub-pages from being transpose to their manage parent pages incorrectly */
			if(publicLink.contains("tools") && !section.getURLToken().contains("tools")) { 
				continue;
			}
			tokenMinusEndSlash = section.getURLToken().substring(0, section.getURLToken().lastIndexOf('/'));
			if(publicLink.indexOf(tokenMinusEndSlash) > -1) {
				sectionFound = true;
				manageLink.append("?actionType=").append(section.getActionType());
				
				//check for qs param with id and replace with appropriate manage section id
				if(publicLink.indexOf(QS_PARAM_TOKEN) > -1) {
					appendQSParamId(section, publicLink, manageLink, endIndex);
				}else {
					int startIndex = publicLink.indexOf(tokenMinusEndSlash) + tokenMinusEndSlash.length();
					manageLink.append(publicLink.substring(startIndex, endIndex));
				}
				return sectionFound; //return if we find a match
			}
		}
		return sectionFound;
	}
	
	/**
	 * Appends the public URL qs param with ID to it's corresponding manage section ID, if applicable
	 * @param section
	 * @param publicLink
	 * @param manageLink
	 * @param endIndex
	 */
	private void appendQSParamId(Section section, String publicLink, StringBuilder manageLink, int endIndex) {
		//parse out the qs param id(go to end of link and capture Id along with any remaining query params)
		int startIndex = publicLink.indexOf(QS_PARAM_TOKEN) + QS_PARAM_TOKEN.length();
		String paramId = publicLink.substring(startIndex , endIndex);
		
		switch(section) {
			case MARKET : 
				manageLink.append("&marketId=").append(paramId);
				break;
			case PRODUCT : 
				manageLink.append("&productId=").append(paramId);
				break;
			case COMPANY : 
				manageLink.append("&companyId=").append(paramId);
				break;
			case INSIGHT : 
				manageLink.append("&insightId=").append(paramId);
				break;
			default : break;
		}
	}
	
	/**
	 * Handles appending any remaining url values(page, query params, etc.) if no matching manage section found
	 * @param publicLink
	 * @param manageLink
	 * @param isAbsoluteSearch
	 * @param sectionFound
	 * @param endIndex
	 */
	private void appendEndURLValues(String publicLink, StringBuilder manageLink, boolean isAbsoluteSearch, 
			boolean sectionFound, int endIndex) {
		if(isAbsoluteSearch && !sectionFound) {
			int startIndex = publicLink.indexOf(baseDomain) + baseDomain.length();
			manageLink.append(publicLink.substring(startIndex, endIndex));
		}
	}
	
	/**
	 * Builds the regular expression for searching for relative links for section pages
	 * @return
	 */
	protected String buildRelativePattern() {
		StringBuilder relativePattern = new StringBuilder(100);
		String tokenMinusEndSlash;
		int count = 0;
		
		//compose regex group of pipe delimited sections : /(markets|companies|products)"/>
		relativePattern.append("/(");
		for(Section section : Section.values()) {
			tokenMinusEndSlash = section.getURLToken().substring(0, section.getURLToken().lastIndexOf('/'));
			if(count > 0) relativePattern.append("|");
			relativePattern.append(tokenMinusEndSlash);
			count++;
		}
		relativePattern.append(")").append(ANCHOR_END_REGEX);
		return relativePattern.toString();
	}
	
	/**
	 * Builds the regular expression for searching for absolute links based on sites' aliases
	 * @return
	 */
	protected String buildAbsolutePattern() {
		StringBuilder absolutePattern = new StringBuilder(200);
		String protocolRegex = "((http|https)://)*?";
		String binaryRegex = "(?!(/binary|/secBinary))"; //don't worry about binary path links
		
		//compose regex group of pipe delimited absolute links based on the sites' aliases.
		int count = 0;
		absolutePattern.append(protocolRegex).append("(");
		for (String alias : siteAliases) {
			if(count > 0) absolutePattern.append("|");
			absolutePattern.append(alias);
			count++;
		}
		absolutePattern.append(")").append(binaryRegex).append(ANCHOR_END_REGEX);
		return absolutePattern.toString();
	}
	
	/**
	 * Returns the list of site aliases based of the parent site
	 * @param site
	 * @return
	 */
	protected void loadSiteAliases(SiteVO site){
		if(site == null) {
			log.error("Site data is null, cannot load aliases");
			return;
		}
		String siteId = site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId(); 
		
		//execute query and populate list
		try(PreparedStatement ps = dbConn.prepareStatement("select site_alias_url from site_alias where site_id = ?")){
			ps.setString(1, siteId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				siteAliases.add(rs.getString("site_alias_url"));
			}
		}catch(SQLException e) {
			log.error("Error attempting to execute alias sql: ", e);
		}
	}
}
