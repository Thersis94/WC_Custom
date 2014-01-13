package com.fastsigns.cutover;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: MainSiteCreator.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 15, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MainSiteCreator {
	// Admintool Login info
	private String admintoolUrl = "http://localhost/sb/admintool";
	private String adminUser = "james@siliconmtn.com";
	private String adminPass = "cannondale";
	private SMTHttpConnectionManager adminConn = null;
		
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String sbUser = "sb_user";
	private final String sbPass = "sqll0gin";
	private final String fsUser = "fastsigns";
	private final String fsPass = "fastsigns";
	private Connection fsConn = null;
	private Connection sbConn = null;
	
	// Misc Params
	private static final Logger log = Logger.getLogger("MainSiteCreator");
	private Map<Integer, Integer> footerPages = new LinkedHashMap<Integer, Integer>();
	private Map<Integer, Integer> mainPages = new LinkedHashMap<Integer, Integer>();
	private Map<String, Integer> folderPaths = new LinkedHashMap<String, Integer>();
	private ContentParser contentParser = null;
	private String templateId = "c0a80241a5df7fa4f9b05b9a2b51569b";
	//private String templateId = "c0a802419aefe7ef7b2cbac41841cb91";
	
	/**
	 * 
	 */
	public MainSiteCreator() throws Exception {
		log.debug("Starting");
		
		// Connect to FastSigns DB
		fsConn = this.getDBConnection(fsUser, fsPass);
		
		// Connect to SB Database
		sbConn = this.getDBConnection(sbUser, sbPass);
		
		// Load up the parser
		contentParser = new ContentParser(fsConn);
		
		// Assign the vals for the footer page
		this.assignVals();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		MainSiteCreator msc = new MainSiteCreator();
		msc.process();
		
		log.debug("Complete ...");
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {
		// Login to the admintool
		this.adminLogin();
		
		// Load the pages
		List<FSPageVO> pages = this.loadPages();
		
		// Create the pages, modules and associations
		for (int i=0; i < pages.size(); i++) {
			FSPageVO page = pages.get(i);
			log.debug("Page: " + page.getFullPath() + "|" + page.getPageId() + "|" + page.getParentId() + "|" + page.isFooterFlag() + "|" + page.getParentPath());
			if (! "44".equals(page.getPageId())) {
				this.addPage(page);
				
				// Load custom content
				String[] content = this.loadContent(page);
				
				// Associate content/modules
				if (content != null && content[0] != null) {
					associatePageModule(content, page);
				}
			}
		}
	}
	
	/**
	 * Associates the custom content for a given portlet to the appropriate page
	 * @param actionId
	 * @param fId
	 * @param pageId
	 */
	public void associatePageModule(String[] actionId, FSPageVO page) 
	throws IOException {
		String url = "actionId=PAGE_MODULE&requestType=reqUpdate";
		url += "&paramName=&pageModuleId=&pmTemplateId=&organizationId=FTS";
		url += "&columns=3&pageId=" + page.getPageId() + "&templateId=" + page.getTemplateId();
		url += "&moduleDisplayId=c0a80228e8972428d49987ea09f4227  &displayOrder=3";
		url += "&moduleTypeId=CMS&moduleId=" + actionId[0];
		url += "&roleId=0&roleId=10&roleId=100&displayColumn=2";
		url += "&moduleActionName=" + URLEncoder.encode(actionId[1], "UTF-8");

		// Call the server and create the association
		adminConn.retrieveDataViaPost(admintoolUrl, url);
	}
	
	/**
	 * Adds the page to the Main Site
	 * @param page
	 * @throws SQLException
	 */
	public void addPage(FSPageVO page) throws SQLException {
		StringBuilder sql = new StringBuilder();
        sql.append("insert into page (PARENT_ID, TEMPLATE_ID, ");
        sql.append("SITE_ID, PAGE_ALIAS_NM, PAGE_DISPLAY_NM, PAGE_TITLE_NM,");
        sql.append("LIVE_START_DT, LIVE_END_DT, ORDER_NO, VISIBLE_FLG, ");
        sql.append("META_KEYWORD_TXT, META_DESC_TXT, META_ROBOT_TXT, DEFAULT_FLG, " );
        sql.append("FOOTER_FLG, CREATE_DT, PARENT_PATH_TXT, FULL_PATH_TXT, ");
        sql.append("PAGE_1_IMG, EXTERNAL_PAGE_URL, JAVA_SCRIPT_TXT, ");
        sql.append("PAGE_ID) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        
        PreparedStatement ps = null;
        try {
            ps = sbConn.prepareStatement(sql.toString());
            ps.setString(1, page.getParentId());
            ps.setString(2, page.getTemplateId());
            ps.setString(3, "FTS_1");
            ps.setString(4, page.getAliasName());
            ps.setString(5, page.getDisplayName());
            ps.setString(6, page.getTitleName());
            ps.setDate(7, Convert.formatSQLDate(page.getStartDate()));
            ps.setDate(8, Convert.formatSQLDate(page.getEndDate()));
            ps.setInt(9, page.getOrder().intValue());
            ps.setInt(10, page.getVisibleFlg());
            ps.setString(11, page.getMetaKeyword());
            ps.setString(12, page.getMetaDesc());
            ps.setString(13, page.getMetaRobot());
            ps.setInt(14, (page.isDefaultPage() ? 1 : 0));
            ps.setInt(15, (page.isFooterFlag() ? 1 : 0));
            ps.setTimestamp(16, Convert.getCurrentTimestamp());
            ps.setString(17, page.getParentPath());
            ps.setString(18, page.getParentPath() + page.getAliasName());
            ps.setString(19, page.getImage1());
            ps.setString(20, page.getExternalPageUrl());
            ps.setString(21, page.getJavaScript());
            ps.setString(22, page.getPageId());
            
            ps.executeUpdate();
            ps.close();
            
            // Add the roles
    		sql = new StringBuilder();
    		sql.append("insert into page_role(page_role_id, page_id, role_id, create_dt) ");
    		sql.append("values (?,?,?,?)");
    		
    		ps = sbConn.prepareStatement(sql.toString());
    		Set<String> s = page.getRoles().keySet();
    		for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
    			String val = iter.next();
                ps.setString(1, new UUIDGenerator().getUUID());
                ps.setString(2, page.getPageId());
                ps.setString(3, val);
                ps.setTimestamp(4, Convert.getCurrentTimestamp());
                ps.addBatch();
    		}
    		
    		ps.executeBatch();
    		log.debug("\t added page: " + page.getPageId() + "|" + page.getParentId());
        } finally {
        	ps.close();
        }
	}
	
	/**
	 * Adds the custom content to the CMS portlet
	 * @param page
	 * @return Action ID of the added portlet
	 * @throws Exception
	 */
	public String[] loadContent(FSPageVO page) throws Exception {
		String content = StringUtil.checkVal(page.getPageContent());
		String extUrl = StringUtil.checkVal(page.getExternalPageUrl());
		
		if (extUrl.length() > 0 || content.length() == 0) return null;
		String pContent = URLEncoder.encode(contentParser.parseAll(content), "UTF-8");
		String metaKey = URLEncoder.encode(StringUtil.checkVal(page.getMetaKeyword()), "UTF-8");
		String metaDesc = URLEncoder.encode(StringUtil.checkVal(page.getMetaDesc()), "UTF-8");
		if (metaKey.length() > 254) metaKey = metaKey.substring(0, 250);
		if (metaDesc.length() > 254) metaDesc = metaDesc.substring(0, 250);
		
		// Build the URL to add the portlet
		String name = URLEncoder.encode(StringUtil.checkVal(page.getDisplayName()), "UTF-8");
		String fileName = StringUtil.removeWhiteSpace(page.getDisplayName()) + ".html";
		
		/* URL to add Simple COntent
		String url = "actionId=CONTENT&requestType=reqUpdate";
		url += "&manMod=true&moduleTypeId=CONTENT&insertAction=true&sbActionId=";
		url += "&organizationId=FTS";
		url += "&actionName=" + name + "&actionDesc=" + name;
		url += "&articleText=" + pContent;
		*/
		
		String url = "actionId=CMS&requestType=reqUpdate";
		url += "&organizationId=FTS&83007=FS+Existing&83009=en&83013=000";
		url += "&folderId=" + getFolderId(page.getFullPath());
		url += "&83015=" + name + "&83016=" + name + "&fileName=" + fileName;
		url += "&83010=" + metaDesc + "&83011=" + metaKey;
		url += "&articleData=" + pContent;
		
		log.debug("**** Adding CMS Content: " + url);
		adminConn.retrieveDataViaPost(admintoolUrl, url);
		return this.getPortletId("FTS", fileName);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFolderId(String fullPath) {
		Integer id = Integer.valueOf(0);
		Set<String> s = folderPaths.keySet();
		for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
			String key = iter.next();
			if (fullPath.toLowerCase().indexOf(key.toLowerCase()) > -1) 
				id = folderPaths.get(key);
		}
		
		// If the path is not in the Map, add the file to the Misc Folder
		if (id == 0) id = 84006;
		
		return id.toString();
	}
	
	/**
	 * Gets the object ID for Simple Content
	 * @param orgId
	 * @param name
	 * @return
	 * @throws SQLException
	 
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
	*/
	
	/**
	 * Gets the CMS Object ID
	 * @param orgId
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	public String[] getPortletId(String orgId, String name) throws SQLException {
		String actionId[] = new String[2];
		
		String sql = "select * from cms.dbo.obj_rev_info where ORG_ID = 83001 and OBJ_NAME =  ?";
		PreparedStatement ps = sbConn.prepareStatement(sql);
		ps.setString(1, name);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) actionId[0] = rs.getString(1);
		
		actionId[1] = name;
		return actionId;
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<FSPageVO> loadPages() throws SQLException {
		String s = "select NodeID, NodeAliasPath, replace(NodeName, '(1)', '') as node_nm, NodeLevel, NodeParentID, ";
		s += "NodeOrder, DocumentPageTitle, DocumentPageKeyWords, DocumentPageDescription, ";
		s += "DocumentShowInSiteMap, DocumentContent, DocumentMenuItemHideInNavigation , ";
		s += "replace(replace(replace(replace(NodeAliasPath,'/National-Accounts-Landing-Page', '/NationalAccounts'),'/National-Accounts-', '/'),'/National-Account-', '/'),'-(1)','') as alias ";
		s += "from cms_tree a ";
		s += "inner join CMS_Document b on a.NodeID = b.DocumentNodeID "; 
		s += "where nodealiaspath not like '/Franchise/%'  ";
		s += "and NodeAliasPath not like '/Redirects/%' ";
		s += "and NodeAliasPath not like '/COCC%'  ";
		s += "and NodeAliasPath not like '/Content-Folder%'  ";
		s += "and NodeAliasPath not like '/Uploaded%'  ";
		s += "and DocumentPageTemplateID is not null ";
		s += "order by NodeAliasPath ";
		
		List<FSPageVO> data = new ArrayList<FSPageVO>();
		PreparedStatement ps = null;
		try {
			ps = fsConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(this.loadPageInfo(rs));
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		
		return data;
	}
	
	/**
	 * Parses the result set into an FSPageVO object
	 * @param rs
	 * @return
	 */
	public FSPageVO loadPageInfo(ResultSet rs) {
		FSPageVO page = new FSPageVO();
		DBUtil db = new DBUtil();
		page.setFullPath(db.getStringVal("alias", rs));
		page.setTitleName(db.getStringVal("DocumentPageTitle", rs));
		page.setPageId(db.getIntVal("NodeId", rs) + "");
		page.setMetaKeyword(db.getStringVal("DocumentPageKeyWords", rs));
		page.setMetaDesc(db.getStringVal("DocumentPageDescription", rs));
		page.setDisplayName(db.getStringVal("node_nm", rs));
		page.setAliasName(parseAlias(db.getStringVal("alias", rs)));
		page.setPageContent(db.getStringVal("DocumentContent", rs));
		page.setOrder(db.getIntegerVal("NodeOrder", rs));
		
		if (page.getDisplayName().equalsIgnoreCase("Home ")) 
			page.setDefaultPage(true);
		
		page.setVisibleFlg(0);
		page.setFooterFlag(false);
		if (footerPages.containsKey(db.getIntVal("NodeId", rs))) {
			page.setFooterFlag(true);
			page.setOrder(footerPages.get(db.getIntVal("NodeId", rs)));
			page.setVisibleFlg(1);
		} else if (mainPages.containsKey(db.getIntVal("NodeId", rs))) {
			page.setVisibleFlg(1);
			page.setOrder(mainPages.get(db.getIntVal("NodeId", rs)));
		}
		
		if (db.getIntegerVal("NodeLevel", rs) > 1)
			page.setVisibleFlg(1);
		
		// Determine if the page is in the site map
		if (page.isVisible() && db.getIntVal("DocumentShowInSiteMap", rs) == 0)
			page.setVisibleFlg(5);
		
		page.setNumberColumns(3);
		page.setDefaultColumn(2);
		page.setParentId(db.getIntVal("NodeParentID", rs) + "");
		page.setParentPath(this.parseParentPath(page.getFullPath()));
		page.setTemplateId(templateId);
		page.setStartDate(new java.util.Date());
		
		Map<String, Integer> roles = new LinkedHashMap<String,Integer>();
		roles.put("0", 0);
		roles.put("10", 10);
		roles.put("c0a80167d141d17e20e2d7784364ab3f", 30);
		roles.put("100", 100);
		page.setRoles(roles);
		
		if (StringUtil.checkVal(page.getTitleName()).length() == 0)
			page.setTitleName(page.getDisplayName());
		
		if (page.getAliasName().length() == 0) {
			page.setAliasName("home");
			page.setDisplayName("home");
			page.setDefaultPage(true);
			page.setParentId("");
		}
				
		// Reset the parentId to null for the root pages
		if ("1".equals(page.getParentId())) page.setParentId("");
		return page;
	}
	
	
	public String parseAlias(String path) {
		return path.substring(path.lastIndexOf("/") + 1);
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	public String parseParentPath(String path) {
		String newPath = path.substring(0, path.lastIndexOf("/") + 1);
		
		return newPath;
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
	private Connection getDBConnection(String dbUser, String dbPass) 
	throws InvalidDataException, DatabaseException  {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		return dbc.getConnection();
	}
	
	/**
	 * 
	 */
	private void assignVals() {
		// Footer Pages
		footerPages.put(900, 3);
		footerPages.put(899, 6);
		footerPages.put(147, 9);
		footerPages.put(996, 12);
		footerPages.put(997, 15);
		footerPages.put(986, 18);
		footerPages.put(146, 21);
		footerPages.put(897, 24);
		
		// Main Pages
		mainPages.put(47, 3);
		mainPages.put(153, 6);
		mainPages.put(152, 9);
		mainPages.put(217, 12);
		mainPages.put(150, 15);
		mainPages.put(389, 18);
		
		// Folder Paths
		folderPaths.put("/Sign_Makeovers", 84001);
		folderPaths.put("/Service", 84002);
		folderPaths.put("/Learning", 84003);
		folderPaths.put("/Sign_Buyers", 84003);
		folderPaths.put("/Franchise", 84004);
		folderPaths.put("/National", 84005);
		folderPaths.put("/Career", 84006);
		folderPaths.put("/Copyright", 84006);
		folderPaths.put("/DocExample", 84006);
		folderPaths.put("/event", 84006);
		folderPaths.put("/Press", 84010);
		folderPaths.put("/Privacy", 83019);
		folderPaths.put("/Terms", 83019);
		folderPaths.put("/About", 84009);
	}
}
