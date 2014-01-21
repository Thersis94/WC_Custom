package com.fastsigns.cutover;

//JDK 1.6.0
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

// Log4j 1.2.28
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: FranchiseCreator.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 3, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseCreator {
	// Admintool Login info
	private String admintoolUrl = "http://www.fastsigns.sagefire.com/fs/admintool";
	private String adminUser = "james@siliconmtn.com";
	private String adminPass = "cannondale";
	private SMTHttpConnectionManager adminConn = null;
	//private Map<String, String> cookies = null;
	
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://192.168.56.101:31433;selectMethod=cursor;responseBuffering=adaptive";
	private final String fdbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String fsUser = "fastsigns";
	private final String fsPass = "fastsigns";
	private final String sbUser = "wc_user";
	private final String sbPass = "sqll0gin";
	private Connection fsConn = null;
	private Connection sbConn = null;
	
	// Website/WC params
	public static final String FS_SITE_ID = "FTS_1";
	
	// Misc Parameters
	private static final Logger log = Logger.getLogger("FranchiseCreator");
	private Map<Integer, GenericVO> varLinks = new HashMap<Integer, GenericVO>();
	public static final String CUSTOM_PORTLET_EXT = ""; 
	
	// Hours, 3 Button, center image and text, Modules and Map
	public Map<String, Integer> defDisplay = new LinkedHashMap<String, Integer>();
	public Map<String, Integer> secDisplay = new LinkedHashMap<String, Integer>();
	
	// Hours, 3 Button, center image and text and Map
	public String[] secDisplayTypes = new String[] {};
	
	private ContentParser contentParser = null;
	
	/**
	 * 
	 */
	public FranchiseCreator() throws Exception {
		log.debug("Starting");
		
		// Connect to FastSigns DB
		fsConn = this.getDBConnection(fsUser, fsPass, fdbUrl);
		
		// Connect to SB Database
		sbConn = this.getDBConnection(sbUser, sbPass, dbUrl);
		
		// Load the parser
		contentParser = new ContentParser(fsConn);
		
		// Assign the variable links
		this.assignVaribleLinks();
		
		// Assign the display types for the center pages
		this.assignTypes();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		FranchiseCreator fc = new FranchiseCreator();
		fc.process();
		log.debug("Complete");
	}

	/**
	 * Processes the business logic for the class
	 * @throws Exception
	 */
	public void process() throws Exception {
		// Log into the system
		this.adminLogin();
		
		// Retrieve List of Franchises
		List<FranchiseVO> franchises = this.getFranchiseData();
		
		// Loop franchises
		for (int i=0; i < franchises.size(); i++) {
			FranchiseVO vo = franchises.get(i);
			log.debug("Franchise ID: " + vo.getFranchiseId() + " [" + vo.getVariableLinks() + "]");
			
			// Get the list of Custom Pages for the franchise
			Map<String, FSPageVO> pages = this.getCustomPages(vo.getFranchiseId());
			
			// Get the list of standard pages and add them to the list of custom pages
			this.addVariableLinkData(vo, pages);
			
			// Create org
			this.addOrganization(vo);
			
			// Add the center page portlet
			String centerActionId = addCenterPage(vo.getFranchiseId());
			
			// Create website
			this.addWebsite(vo);			
			
			// Update Layout information and add the secondary layout
			String layoutId = this.updateLayout(vo.getFranchiseId(), centerActionId);
			String secLayoutId = this.addSecondaryLayout(vo.getFranchiseId());
			
			// Associate the main modules and the center image/text to the layouts
			associateCenterPage(layoutId, vo.getFranchiseId(), centerActionId, 1);
			associateCenterPage(secLayoutId, vo.getFranchiseId(), centerActionId, 2);
			
			// Assign the theme
			this.assignTheme(vo);
			
			// Add Pages
			Set<String> s = pages.keySet();
			for(Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
				String key = iter.next();
				FSPageVO page = pages.get(key);
				
				String pageLayoutId = secLayoutId;
				if (page.isDefaultPage()) pageLayoutId = layoutId;
				this.addPage(pageLayoutId, page, vo);
				
				// COntent is only for the secondary pages
				if (page.isDefaultPage()) continue;
				
				// Load custom content
				String contentId = null;
				if (StringUtil.checkVal(page.getPageContent()).length() > 0)
					contentId = this.loadContent(page, vo);
				
				// Associate content/modules
				if (contentId != null) {
					associatePageModule(contentId, vo.getFranchiseId(), page, layoutId);
				}
			}
			
		}
	}
	
	/**
	 * 
	 * @param fId
	 * @return
	 */
	public String addSecondaryLayout(int fId) throws Exception {
		String siteId = "FTS_" + fId + "_1";
		
		String url = "actionId=TEMPLATE&requestType=reqUpdate";
		url += "&paramName=&pageModuleId=&pmTemplateId=&organizationId=FTS_" + fId;
		url += "&columns=3&siteId=" + siteId + "&layoutName=Secondary+Page+Layout";
		url += "&pageTitle=Welcome+to+FASTSIGNS&reg;&defaultColumn=2&numberColumns=3";
		url += "&defaultFlag=0&templateId=";

		// Call the server and create the association
		adminConn.retrieveDataViaPost(admintoolUrl, url);
		
		return this.getSecondaryLayoutId(siteId, "Secondary Page Layout");
	}
	
	
	public String getSecondaryLayoutId(String siteId, String name) throws Exception {
		String sql = "select template_id from template where site_id = ? and layout_nm = ?";
		//log.debug("Get Sec Template ID SQL: " + sql + "|" + siteID + "|" + alias);
		
		PreparedStatement ps = sbConn.prepareStatement(sql);
		String tId = "";
		ps.setString(1, siteId);
		ps.setString(2, name);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) tId = rs.getString(1);
		
		return tId;

	}
	
	
	/**
	 * 
	 * @param page
	 * @param centerActionId
	 * @throws SQLException
	 */
	public void associateCenterPage(String layoutId, int fId, String centerActionId, int type) 
	throws Exception {
			Map<String, Integer> current = secDisplay;
			if (type == 1) current = defDisplay;
			Set<String> keys = current.keySet();
			log.debug("***********************: " + secDisplay.size() + "|" + defDisplay.size() + "|" + current.size());
			int i = 1;
			for (Iterator<String> iter = keys.iterator(); iter.hasNext(); i++) {
				String key = iter.next();
				Integer col = current.get(key);

	            String pageModuleId = new UUIDGenerator().getUUID();
	            StringBuilder sb = new StringBuilder();
	            sb.append("insert into page_module ");
	            sb.append("(module_display_id,template_id,action_id, display_column_no,");
	            sb.append("order_no, module_action_nm, param_nm, create_dt, page_module_id) ");
	            sb.append("values (?,?,?,?,?,?,?,?,?)");
				
				PreparedStatement ps = null;
		        try {
		            ps = sbConn.prepareStatement(sb.toString());
		            ps.setString(1, key);
		            ps.setString(2, layoutId);
		            ps.setString(3, centerActionId);
		            ps.setInt(4, col);
		            ps.setInt(5, col+i);
		            ps.setString(6, null);
		            ps.setString(7, null);
		            ps.setTimestamp(8, Convert.getCurrentTimestamp());
		            ps.setString(9, pageModuleId);
		            ps.executeUpdate();
		            
		    		StringBuffer sql = new StringBuffer();
		    		sql.append("insert into page_module_role(page_module_role_id, page_module_id, role_id, create_dt) ");
		    		sql.append("values (?,?,?,?)");
		    		
		    		String[] roles = new String[] {"0","10", "100", "c0a80167d141d17e20e2d7784364ab3f"};
		            for (int j=0; j < roles.length; j++) {
		            	ps = sbConn.prepareStatement(sql.toString());
		                ps.setString(1, new UUIDGenerator().getUUID());
		                ps.setString(2, pageModuleId);
		                ps.setString(3, roles[j]);
		                ps.setTimestamp(4, Convert.getCurrentTimestamp());
		                ps.executeUpdate();
		            }
		        } finally {
		        	try {
		        		ps.close();
		        	} catch(Exception e) {	}
		        }
			}
	}
	
	/**
	 * 
	 * @param franchiseId
	 * @throws SQLException
	 */
	public String addCenterPage(int franchiseId) throws SQLException {
		String s = "insert into sb_action (action_nm, action_desc, organization_id, ";
		s += "module_type_id, action_id, attrib1_txt, create_dt) values (?,?,?,?,?,?,?)";
		
		PreparedStatement ps = null;
		try {
			ps = sbConn.prepareStatement(s);
			ps.setString(1, "Center Page Portlet");
			ps.setString(2, "Center Page Portlet");
			ps.setString(3, "FTS_"+ franchiseId);
			ps.setString(4, "FTS_CENTER_PAGE");
			ps.setString(5, "FTS_CENTER_PAGE_" + franchiseId);
			ps.setString(6, franchiseId + "");
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Return the action id
		return "FTS_CENTER_PAGE_" + franchiseId;
	}
	
	/**
	 * Associates the custom content for a given portlet to the appropriate page
	 * @param actionId
	 * @param fId
	 * @param pageId
	 */
	public void associatePageModule(String actionId, int fId, FSPageVO page, String layoutId) 
	throws IOException {
		String name = page.getDisplayName() + CUSTOM_PORTLET_EXT;
		
		String url = "actionId=PAGE_MODULE&requestType=reqUpdate";
		url += "&paramName=&pageModuleId=&pmTemplateId=&organizationId=FTS_" + fId;
		url += "&columns=3&pageId=" + page.getPageId() + "&templateId=" + layoutId;
		url += "&moduleDisplayId=CONTENT_MAIN &displayOrder=10";
		url += "&moduleTypeId=CONTENT&moduleId=" + actionId;
		url += "&roleId=0&roleId=10&roles=c0a80167d141d17e20e2d7784364ab3f&roleId=100&displayColumn=2";
		url += "&moduleActionName=" + URLEncoder.encode(name, "UTF-8");

		// Call the server and create the association
		adminConn.retrieveDataViaPost(admintoolUrl, url);
	}
	
	/**
	 * Adds the custom content to the content portlet
	 * @param page
	 * @return Action ID of the added portlet
	 * @throws Exception
	 */
	public String loadContent(FSPageVO page, FranchiseVO vo) throws Exception {
		String content = StringUtil.checkVal(page.getPageContent());
		String extUrl = StringUtil.checkVal(page.getExternalPageUrl());
		
		if (extUrl.length() > 0 || content.length() == 0) return null;
		content = URLEncoder.encode(contentParser.parseAll(content), "UTF-8");
		
		// Build the URL to add the portlet
		String name = page.getDisplayName() + CUSTOM_PORTLET_EXT;
		
		String url = "actionId=CONTENT&requestType=reqUpdate";
		url += "&manMod=true&moduleTypeId=CONTENT&insertAction=true&sbActionId=";
		url += "&organizationId=FTS_" + vo.getFranchiseId();
		url += "&actionName=" + name + "&actionDesc=" + name;
		url += "&articleText=" + content;
		
		adminConn.retrieveDataViaPost(admintoolUrl, url);
		return this.getPortletId("FTS_" + vo.getFranchiseId(), name);
	}
	
	/**
	 * 
	 * @param orgId
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	public String getPortletId(String orgId, String name) throws SQLException {
		String actionId = null;
		String sql = "select action_id from sb_action where organization_id = ? ";
		sql += "and action_nm = ? and module_type_id = 'CONTENT'";
		
		PreparedStatement ps = sbConn.prepareStatement(sql);
		ps.setString(1, orgId);
		ps.setString(2, name);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) actionId = rs.getString(1);
		
		return actionId;
	}
	
	/**
	 * 
	 * @param siteId
	 */
	public void assignTheme(FranchiseVO vo) throws Exception {
		String siteId = "FTS_" + vo.getFranchiseId() + "_1";
		
		String sql = "update site_theme_impl set theme_menu_id = 'c0a80241a3d427b542158983ee4a9464',";
		sql += "theme_stylesheet_id = 'c0a80241a3d2c8faf705a5cfd1d5186f ' ";
		sql += "where site_id = ?";
		log.debug("Theme Update: " + sql + "|" + siteId);
		
		PreparedStatement ps = sbConn.prepareStatement(sql);
		ps.setString(1, siteId);
		ps.executeUpdate();
	}
	
	/**
	 * 
	 * @param layoutId
	 * @param page
	 */
	public void addPage(String layoutId, FSPageVO page, FranchiseVO vo) 
	throws Exception {
		String siteId = "FTS_" + vo.getFranchiseId() + "_1";
		String extUrl = StringUtil.checkVal(page.getExternalPageUrl());
		if (extUrl.length() > 0) extUrl = URLEncoder.encode(extUrl, "UTF-8");
		
		
		// See if the alias matches the franchise number.  Change the alias
		// to "home" and set the default flag to true
		String pan = page.getAliasName();
		if (pan.endsWith("(1)")) pan = pan.substring(0, pan.length() - 4);
		
		int aliasNo = Convert.formatInteger(pan);
		log.debug("************ Looking for Alias Numbers: " + pan + "|" + aliasNo + "|" + vo.getFranchiseId());
		if (aliasNo == vo.getFranchiseId()) {
			log.debug("Changing home page info");
			page.setDisplayName("home");
			page.setAliasName("home");
			page.setDefaultPage(true);
		}
		
		String alias = URLEncoder.encode(page.getAliasName(),"UTF-8");
		int defaultPage = 0;
		if (page.getDefaultPage()) defaultPage = 1;
		
		// Build the URL to add the page
		String url = "actionId=PAGE&requestType=reqUpdate";
		url += "&templateId=" + layoutId + "&startDate=" + URLEncoder.encode("11/1/2010", "UTF-8");
		url += "&parentPath=%2f&organizationId=FTS_" + vo.getFranchiseId(); 
		url += "&parentId=&siteId=" + siteId + "&aliasName=" + alias;
		url += "&displayName=" + URLEncoder.encode(page.getDisplayName(), "UTF-8");
		url += "&titleName=" + URLEncoder.encode(page.getTitleName() + "","UTF-8");
		url += "&metaKeyword=" + URLEncoder.encode(StringUtil.checkVal(page.getMetaKeyword()), "UTF-8");
		url += "&metaDesc=" + URLEncoder.encode(StringUtil.checkVal(page.getMetaDesc()), "UTF-8");
		url += "&visible=" + page.getVisibleFlg();
		url += "&defaultPage=" + defaultPage + "&orderNumber=" + page.getOrder();
		url += "&roles=0&roles=10&roles=c0a80167d141d17e20e2d7784364ab3f&roles=100";
		url += "&externalPageUrl=" + extUrl;
		
		// Call the admintool to add the page
		adminConn.retrieveDataViaPost(admintoolUrl, url);
		
		// Set the page id into the page vo.
		page.setPageId(this.getPageId(siteId, page.getAliasName()));
	}
	
	/**
	 * Retrieves the page ID for the newly added page
	 * @param siteID
	 * @param alias
	 * @return
	 * @throws SQLException
	 */
	public String getPageId(String siteID, String alias) throws SQLException {
		String pageId = null;
		String sql = "select page_id from page where site_id = ? and page_alias_nm = ?";
		//log.debug("get Page ID SQL: " + sql + "|" + siteID + "|" + alias);
		
		PreparedStatement ps = sbConn.prepareStatement(sql);
		ps.setString(1, siteID);
		ps.setString(2, alias);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) pageId = rs.getString(1);
		
		return pageId;
	}
	
	/**
	 * Modifies the number of columns and the default column for the layout
	 * @param fId
	 * @return GUID for the layout
	 */
	public String updateLayout(int fId, String actionId) throws SQLException {
		String siteId = "FTS_" + fId + "_1";
		String tIdSql = "select template_id from template where site_id = '" + siteId + "'";
		String tId = null;
		
		Statement s = sbConn.createStatement();
		ResultSet rs = s.executeQuery(tIdSql);
		if (rs.next()) tId = rs.getString(1);
		
		String sql = "update template set columns_no=3, default_column_no=2";
		sql += "where template_id = '" + tId + "'";
		
		s = sbConn.createStatement();
		s.executeUpdate(sql);
		
		s.close();
		
		// Associate the Center hours, buttons and small map in right column
		
		return tId;
	}
	
	/**
	 * 
	 * @param vo
	 */
	public void addOrganization(FranchiseVO vo) throws Exception {
		String url = admintoolUrl + "?actionId=ORG&requestType=reqUpdate";
		url += "&organizationName=FASTSIGNS+of+" + URLEncoder.encode(vo.getFranchiseName(), "UTF-8");
		url += "&origOrganizationId=&organizationId=FTS_" + vo.getFranchiseId(); 
		url += "&organizationGroupId=FAST_SIGNS&allModuleFlag=1";
		log.debug("Org URL: " + url);
		
		adminConn.retrieveData(url);
	}
	
	/**
	 * Adds the franchise web site for the provided franchise
	 * @param vo
	 * @throws IOException
	 */
	public void addWebsite(FranchiseVO vo) throws IOException {
		String email = "james@siliconmtn.com"; //vo.getFranchiseId() + "@fastsigns.com";
		String dp = URLEncoder.encode("/cms/main.jsp", "UTF-8");
		
		String url = admintoolUrl + "?actionId=SITE&requestType=reqUpdate";
		url += "&noEmailNote=true&organizationId=FTS_" + vo.getFranchiseId(); 
		url += "&siteName=Franchise+Website&adminName=Webmaster&mainEmail=" + email;
		url += "&adminEmail=" + email + "&sslFlg=0&secureSite=0&countryCode=US";
		url += "&languageCode=en&allowAliasPathFlag=0&aliasPathName=" + vo.getFranchiseId();
		url += "&aliasPathParentId=" + FS_SITE_ID + "&documentPath=" + dp;
		log.debug("Add Website URL: " + url);
		
		adminConn.retrieveData(url);
	}
	
	/**
	 * Retrieves a Collection of Franchise Information
	 * @return
	 */
	public void addVariableLinkData(FranchiseVO vo, Map<String, FSPageVO> pages) {
		StringTokenizer st = new StringTokenizer(vo.getVariableLinks(), "|");
		log.debug("Page Size *********: " + pages.size());
		
		for (int i=1; st.hasMoreTokens(); i++) {
			int val = Convert.formatInteger(st.nextToken());
			GenericVO gvo = varLinks.get(val);
			
			if (! pages.containsKey(gvo.getValue())) {
				log.debug("Processing Var Link: " + val);
				String alias = "";
				String extUrl = gvo.getValue() + "";
				String fullPath = gvo.getValue() + "";
				FSPageVO page = new FSPageVO();
				switch (val) {
					case 1:
					case 2:
						alias = gvo.getValue() + "";
						alias = alias.substring(1, alias.length());
						break;
					case 11:
						log.debug("****** Processing Promotional Link ...... " + vo.getPromotionalProductsLink());
						String pVal = StringUtil.checkVal(vo.getPromotionalProductsLink());
						if (pVal.length() > 0) extUrl = pVal;
						break;
					case 14:
						gvo.setKey(vo.getVariableLinkText1());
						String url = StringUtil.checkVal(vo.getVariableLinkVal1());
						if (vo.getVariableLinkTarget1() == 1) {
							extUrl = url;
						} else {
							alias = url.substring(url.lastIndexOf("/") + 1);
							fullPath = url;
							extUrl = null;
							page.setPageContent(this.getVariableContent(vo.getVariableLinkVal1()));
						}
						
						break;
					case 15:
						gvo.setKey(vo.getVariableLinkText2());
						String url2 = StringUtil.checkVal(vo.getVariableLinkVal2());
						if (vo.getVariableLinkTarget2() == 1) {
							extUrl = url2;
						} else {
							alias = url2.substring(url2.lastIndexOf("/") + 1);
							fullPath = url2;
							extUrl = null;
							page.setPageContent(this.getVariableContent(vo.getVariableLinkVal2()));
						}
						
						break;
					default:
						alias = gvo.getKey() + "";
						alias = alias.replace("&reg;", "_");
						alias = StringUtil.removeNonAlpha(alias);
						log.debug("Parsing spaces: " + alias);
						break;
				}
				
				log.debug("adding Page: " + gvo.getValue() + "|" + alias + "|" + fullPath);
				page.setAliasName(alias);
				page.setDefaultColumn(2);
				page.setDefaultPage(false);
				page.setDisplayName(gvo.getKey() + "");
				page.setExternalPageUrl(extUrl);
				page.setFooterFlag(false);
				page.setFullPath(fullPath);
				page.setNumberColumns(3);
				page.setOrder(i * 3);
				page.setParentPath("%2f");
				page.setTitleName(gvo.getKey() + "");
				page.setVisibleFlg(1);
				
				// Add the page to the list of pages
				pages.put(gvo.getValue() + "", page);
			} else {
				FSPageVO page = pages.get(gvo.getValue());
				page.setVisibleFlg(1);
				page.setOrder(i);
			}
		}
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public String getVariableContent(String path) {
		String s = "select DocumentContent from CMS_Document where DocumentUrlPath = ?";
		String content = "";
		log.debug("Variable Content Retrieve: " + s + "|" + path);
		
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s.toString());
			ps.setString(1, path);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ContentParser cp = new ContentParser(fsConn);
				content = cp.parseAll(StringUtil.checkVal(rs.getString(1)));
			}
		} catch(Exception e) {
			log.error("Error getting variable link content", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return content;
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public Map<String, FSPageVO> getCustomPages(int id) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("select  REPLACE(nodealiaspath, '-(1)', '')  as parsepath, * from cms_tree a ");
		s.append("inner join CMS_Document b on a.NodeID = b.DocumentNodeID ");
		s.append("where (nodealiaspath like '/Franchise/" + id + "/%' ");
		s.append("or nodealiaspath = '/Franchise/" + id + "' ");
		s.append("or nodealiaspath like '/Franchise/" + id + "-(1)%' or nodealiaspath = '/Franchise/" + id + "-(1) ') ");
		s.append("and (datalength(DocumentContent) > 0) ");
		s.append("order by NodeLevel, NodeAliasPath  ");
		log.debug("Pages SQL: " + s);
		
		Map<String, FSPageVO> data = new LinkedHashMap<String, FSPageVO>();
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			for (int i = 10; rs.next(); i++) {
				log.debug("***********:  " + rs.getString("NodeAliasPath"));
				FSPageVO page = new FSPageVO(rs, i, id);
				data.put(page.getFullPath(), page);
				log.debug("Full Path: " + page.getFullPath() + "|" + page.getOrder() + "|" + i);
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<FranchiseVO> getFranchiseData() throws SQLException {
		String s = "select * ";
		s += "from custom_franchise a inner join SiteBuilder_custom.dbo.FTS_FRANCHISE b ";
		s += "on a.StoreNumber = b.FRANCHISE_ID and StoreNumber = 999 ";// and storenumber < 100";
		s += "order by StoreNumber ";
		log.debug(s);
		List<FranchiseVO> data = new ArrayList<FranchiseVO>();
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				
				FranchiseVO fr = new FranchiseVO(rs);
				fr.setVariableLinkText1(rs.getString("VAR_LINK"));
				data.add(fr);
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return data;
	}
	
	
	/**
	 * Load the links data that resides in pipe delimited data in the DB
	 */
	public void assignVaribleLinks() {
		varLinks.put(1,new GenericVO("About Us","/About-Us"));
		varLinks.put(2,new GenericVO("Our Staff","/Our-Staff"));
		varLinks.put(3,new GenericVO("Products","/ProductsList"));
		varLinks.put(4,new GenericVO("Services","/Services"));
		varLinks.put(5,new GenericVO("Learning Center","/LearningCenter "));
		varLinks.put(6, new GenericVO("Sign Needs Analysis","/binary/org/FTS/SignNeedsAnalysis-GEN.pdf"));
		varLinks.put(7,new GenericVO("Image Library","http://www.photospin.com/fastsigns.asp"));
		varLinks.put(8,new GenericVO("FASTSIGNS&reg; Blog","/LearningCenter/Blog"));
		varLinks.put(11,new GenericVO("Promotional Products","http://fastsigns191.logomall.com/homepage/default.aspx?DPSV_Id=352637"));
		varLinks.put(12,new GenericVO("Dynamic Digital Signs","/DDS"));
		varLinks.put(13,new GenericVO("Equipment",""));
		varLinks.put(14,new GenericVO("VariableOne","/Variable1"));
		varLinks.put(15,new GenericVO("VariableTwo","/Variable2"));
	}
	
	/**
	 * @throws IOException 
	 * 
	 */
	public void adminLogin() throws IOException {
		adminConn = new SMTHttpConnectionManager();
		adminConn.setFollowRedirects(false);
		String url = "loginEmailAddress=" + adminUser + "&password=" + adminPass;
		adminConn.retrieveDataViaPost(admintoolUrl,url);
		//cookies = adminConn.getCookies();
		adminConn.retrieveData(admintoolUrl + "?cPage=index");
	}
	
	/**
	 * Creates a connection to the database
	 * @param dbUser Database User
	 * @param dbPass Database Password
	 * @return JDBC connection for the supplied user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String dbUser, String dbPass, String url) 
	throws InvalidDataException, DatabaseException  {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, url, dbUser, dbPass);
		return dbc.getConnection();
	}
	
	/**
	 * 
	 */
	public void assignTypes() {
		// Default Display
		defDisplay.put("c0a8016564a7940e9195bd84416afd93", 1);
		defDisplay.put("c0a8016564a839844b856dccb4329653", 2);
		defDisplay.put("c0a8016564a89155842b990af697746c", 2);
		defDisplay.put("c0a8016564a736c8a6dc6531b494309c", 3);
		
		// Secondary pages
		secDisplay.put("c0a8016564a7940e9195bd84416afd93", 1);
		secDisplay.put("c0a80165f0c00ec5ab44b17238772bf9", 2);
		secDisplay.put("c0a80165f0c0ccdaef974a12b5ee3faa", 3);
	}
}
