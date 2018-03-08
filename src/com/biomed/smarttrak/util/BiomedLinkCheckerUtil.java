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
//SMT base libs
import com.siliconmtn.util.StringUtil;
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
	private Connection dbConn;
	private static final String HREF_START_REGEX = "href=['\"]";
	private static final String HREF_END_REGEX = ".*?['\"]>";
	private static final String QS_PARAM_TOKEN = "/qs/";
	
	/**
	 * Constructor to initialize class
	 * @param dbConn
	 */
	public BiomedLinkCheckerUtil(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * Wrapper method, searches given text for any matching URL links, absolute or relative. Then modifies to them
	 * to the appropriate corresponding manage side version.
	 * @param text
	 * @return - The modified text with updated links if applicable, otherwise the original text
	 */
	public String modifySiteLinks(String text, SiteVO site) {		
		//attempt to find any matches for public site links(absolute or relative)
		String modifiedText = modifyRelativeLinks(text);
		modifiedText = modifyAbsoluteLinks(modifiedText, site);
		
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
		
		//build the regex pattern based on search for relative links
		String searchPattern = buildRelativePattern();
		
		Pattern p = Pattern.compile(searchPattern);
		Matcher m = p.matcher(text);
		while(m.find()) {		
			//update to manage link
			text = transposeContentLinks(m.group(), text, false);
		}
		
		return text;
	}
	
	/**
	 * Searches text content for any public links and returns a modified text with links converted to manage links.
	 * @param text
	 * @param site
	 * @return
	 */
	public String modifyAbsoluteLinks(String text, SiteVO site){
		if(StringUtil.isEmpty(text) || site == null) return text;
		log.debug("Checking absolute links...");
		
		//build the regex pattern based on search for absolute links
		String searchPattern = buildAbsolutePattern(site);
		
		Pattern p = Pattern.compile(searchPattern);
		Matcher m = p.matcher(text);
		while(m.find()) {
		
			//update the manage link
			text = transposeContentLinks(m.group(), text, true);
		}
		
		return text;
	}
	
	/**
	 * Updates content by converting the matched public link to it's manage side equivalent
	 * Examples: companies/qs/73139 -> /manage?actionType="companyAdmin&companyId=73139"
	 * www.smarttrak.net/products -> www.smarttrak.net/manage?actionType=productAdmin
	 * @param publicLink
	 * @param text
	 * @param isAbsoluteSearch
	 * @return
	 */
	private String transposeContentLinks(String publicLink, String text, boolean isAbsoluteSearch) {
			try {
				if(publicLink.indexOf("/manage") > -1) {
					return  text; // matched link already points to manage tool, skip			
				}
				log.trace("public link to transpose: " + publicLink);
				StringBuilder manageLink = new StringBuilder(publicLink.length() + 150);	
		
				//add the manage path in correct location
				String baseDomain = "";
				if(isAbsoluteSearch) {
					int endIndex = publicLink.lastIndexOf('.') + 4; //top level domain
					baseDomain = publicLink.substring("href=\"".length(),  endIndex);		
					manageLink.append("href=\"").append(baseDomain).append("/manage"); //append manage		
				}else {
					manageLink.append("href=\"/manage"); //prepend manage
				}
				
				//replace the section page with corresponding manage section
				boolean sectionFound = false;
				String tokenMinusEndSlash = "";
				for(Section section : Section.values()) {
					tokenMinusEndSlash = section.getURLToken().substring(0, section.getURLToken().lastIndexOf('/'));
					if(publicLink.indexOf(tokenMinusEndSlash) > -1) {
						sectionFound = true;
						manageLink.append("?actionType=").append(section.getActionType());
						
						//check for qs param with id and replace with appropriate manage section id
						if(publicLink.indexOf(QS_PARAM_TOKEN) > -1) {
							appendQSParamId(section, publicLink, manageLink);
						}else {
							int startIndex = publicLink.indexOf(tokenMinusEndSlash) + tokenMinusEndSlash.length();
							manageLink.append(publicLink.substring(startIndex, publicLink.length() -2));
						}
					}
				}	
				
				//if no matching manage section found, handle appending any remaining url values(page, query params, etc.) here
				if(isAbsoluteSearch && !sectionFound) {
					int startIndex = publicLink.indexOf(baseDomain) + baseDomain.length();
					manageLink.append(publicLink.substring(startIndex, publicLink.length() -2));
				}
				manageLink.append("\">"); //end the anchor tag
				
				//replace the public link with the admin link
				log.trace("transposed manage link: " + manageLink);
				return text.replace(publicLink, manageLink.toString());	
			}catch(Exception e) {//If an error occurs catch it here, and return original text
				log.error("Error attempting to transpose public link to manage link: " + e);
				return text;
			}
	}
	
	/**
	 * Appends the public URL qs param with ID to it's corresponding manage section ID, if applicable
	 * @param section
	 * @param publicLink
	 * @param manageLink
	 */
	private void appendQSParamId(Section section, String publicLink, StringBuilder manageLink) {
		//parse out the qs param id(go to end of link and capture Id along with any remaining query params)
		int startIndex = publicLink.indexOf(QS_PARAM_TOKEN) + QS_PARAM_TOKEN.length();
		String paramId = publicLink.substring(startIndex , publicLink.length() -2);
		
		switch(section) {
			case MARKET : 
				manageLink.append("&marketId="+paramId);
				break;
			case PRODUCT : 
				manageLink.append("&productId="+paramId);
				break;
			case COMPANY : 
				manageLink.append("&companyId="+paramId);
				break;
			case INSIGHT : 
				manageLink.append("&insightId="+paramId);
				break;
			default : break;
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
		
		//compose regex group of pipe delimited sections : href="/(markets|companies|products)"/>
		relativePattern.append(HREF_START_REGEX).append("/(");
		for(Section section : Section.values()) {
			tokenMinusEndSlash = section.getURLToken().substring(0, section.getURLToken().lastIndexOf('/'));
			if(count > 0) relativePattern.append("|");
			relativePattern.append(tokenMinusEndSlash);
			count++;
		}
		relativePattern.append(")").append(HREF_END_REGEX);
		return relativePattern.toString();
	}
	
	/**
	 * Builds the regular expression for searching for absolute links based on sites' aliases
	 * @param site
	 * @return
	 */
	protected String buildAbsolutePattern(SiteVO site) {
		StringBuilder absolutePattern = new StringBuilder(200);
		String protocolRegex = "((http|https)://)*?";
		
		//fetch list of site domains/aliases
		List<String> aliases = fetchSiteAliases(site);
		
		//compose regex group of pipe delimited absolute links based on the sites' aliases.
		int count = 0;
		absolutePattern.append(HREF_START_REGEX).append(protocolRegex).append("(");
		for (String alias : aliases) {
			if(count > 0) absolutePattern.append("|");
			absolutePattern.append(alias);
			count++;
		}
		absolutePattern.append(")").append(HREF_END_REGEX);
		return absolutePattern.toString();
	}
	
	/**
	 * Returns the list of site aliases based of the parent site
	 * @param site
	 * @return
	 */
	protected List<String> fetchSiteAliases(SiteVO site){
		List<String> aliases = new ArrayList<>(); 
		String siteId = site.getAliasPathParentId() != null ? site.getAliasPathParentId() : site.getSiteId(); 
		
		//execute query and populate list
		try(PreparedStatement ps = dbConn.prepareStatement(buildSiteAliasQuery())){
			ps.setString(1, siteId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				aliases.add(rs.getString("site_alias_url"));
			}
		}catch(SQLException e) {
			log.error("Error attempting to execute alias sql: ", e);
		}
		
		return aliases;
	}
	
	/**
	 * Builds the site alias sql query 
	 * @return
	 */
	protected String buildSiteAliasQuery(){
		StringBuilder sql = new StringBuilder(100);
		sql.append("select site_alias_url from site_alias where site_id = ? ");
		return sql.toString();
	}
}
