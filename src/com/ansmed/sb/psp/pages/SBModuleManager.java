package com.ansmed.sb.psp.pages;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.ansmed.sb.psp.PSPContent;
import com.ansmed.sb.psp.PspSiteVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>SBModuleManager.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Dec 3, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class SBModuleManager {
	/**
	 * URL of the SB admintool instance
	 */
	protected static final String SB_ADMIN_URL = "http://www.poweroveryourpain.com/sb/admintool";
	//protected static final String SB_ADMIN_URL = "http://sn.sb.whiterabbit.com/sb/admintool";
	
	/**
	 * User name and login for the sb admintool
	 */
	protected static final String SB_LOGIN_NAME = "loginEmailAddress";
	protected static final String SB_PASSWORD = "password";
	
	private static Logger log = Logger.getLogger(SBModuleManager.class);
	private SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
	private Connection dbConn = null;
	private Map<String, String> pageMap = new HashMap<String, String>();
	
	/**
	 * 
	 */
	public SBModuleManager(String userName, String password) throws IOException {
		// Connect to the database
		String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		String dbUrl = "jdbc:sqlserver://192.168.3.120:2007;selectMethod=cursor;responseBuffering=adaptive";
		//String dbUrl = "jdbc:sqlserver://10.0.70.3:2007";
		String dbUser = "sitebuilder_sb_user";
		String dbPassword = "sqll0gin";
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		try {
			dbConn = dbc.getConnection();
			log.debug("DB Connection Set");
		} catch (Exception de) {
			log.error("Unable to get db connection ", de);
			System.exit(-1);
		}

		// Login to the database
		conn.setFollowRedirects(false);
		this.login(userName, password);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
		log.info("Starting PS Site Migration Module Creation");
		SBModuleManager sbmm = new SBModuleManager("dave@siliconmtn.com", "d0ntf0rget");
		SBWizardVO vo = new SBWizardVO();
		vo.setOrganizationId("SJM_PSP_144_1");
		vo.setOrganizationName("Center for Interventional Spine");
		vo.setSiteId("SJM_PSP_144");
		vo.setSiteName("Center for Interventional Spine");
		vo.setThemeId("EMPTY");
		vo.setSiteAliasUrl("psp144.sb.whiterabbit.com");
		//vo.setCopyright("This is the copyright");
		vo.setNumberColumns("2");
		vo.setDefaultColumn("1");
		sbmm.callWizard(vo);
		
		Map<String, List<PSPContent>> mapData = new HashMap<String, List<PSPContent>>();
		log.debug("number of pages: " + sbmm.pageMap.size());
		
		List<PSPContent> data = new ArrayList<PSPContent>();
		PSPContent c = new PSPContent();
		c.setColumn(2);
		c.setContentName("home");
		c.setContentText("Home content");
		data.add(c);
		mapData.put("home", data);
		
		List<PSPContent> data1 = new ArrayList<PSPContent>();
		PSPContent c1 = new PSPContent();
		c1.setColumn(2);
		c1.setContentName("overview");
		c1.setContentText("Overview content");
		data1.add(c1);
		mapData.put("overview", data1);
		
		List<PSPContent> data2 = new ArrayList<PSPContent>();
		PSPContent c2 = new PSPContent();
		c2.setColumn(2);
		c2.setContentName("location");
		c2.setContentText("Location content");
		data2.add(c2);
		mapData.put("location", data2);
		
		List<PSPContent> data3 = new ArrayList<PSPContent>();
		PSPContent c3 = new PSPContent();
		c3.setColumn(2);
		c3.setContentName("contact");
		c3.setContentText("Contact content");
		data3.add(c3);
		mapData.put("contact", data3);
		
		List<PSPContent> data4 = new ArrayList<PSPContent>();
		PSPContent c4 = new PSPContent();
		c4.setColumn(2);
		c4.setContentName("service");
		c4.setContentText("Services content");
		data4.add(c4);
		mapData.put("service", data4);
		
		List<PSPContent> data5 = new ArrayList<PSPContent>();
		PSPContent c5 = new PSPContent();
		c5.setColumn(2);
		c5.setContentName("general");
		c5.setContentText("General content");
		data5.add(c5);
		mapData.put("general", data5);
		
		List<PSPContent> data6 = new ArrayList<PSPContent>();
		PSPContent c6 = new PSPContent();
		c6.setColumn(2);
		c6.setContentName("profile");
		c6.setContentText("Profile content");
		data6.add(c6);
		mapData.put("profile", data6);
		
		List<PSPContent> data7 = new ArrayList<PSPContent>();
		PSPContent c7 = new PSPContent();
		c7.setColumn(2);
		c7.setContentName("link");
		c7.setContentText("Links content");
		data7.add(c7);
		mapData.put("link", data7);
		
		List<PSPContent> data8 = new ArrayList<PSPContent>();
		PSPContent c8 = new PSPContent();
		c8.setColumn(2);
		c8.setContentName("disclaimer");
		c8.setContentText("Disclaimer content");
		data8.add(c8);
		mapData.put("disclaimer", data8);
		
		sbmm.associateContent(vo.getOrganizationId(), mapData);
		
		try {
			sbmm.close();
		} catch(Exception e) {
			log.error("Error closing dbConn connection.",e);
		}
	}

	/**
	 * Logs into the admintool
	 * @param user
	 * @param password
	 * @throws IOException
	 */
	public void login(String user, String password) throws IOException {
		String postData = SB_LOGIN_NAME + "=" + user + "&" + SB_PASSWORD + "=" + password;
		conn.retrieveDataViaPost(SB_ADMIN_URL, postData);

	}
	
	/** 
	 * Sets up the base Organization, site and important info
	 * Adter completing it loads a collection of page types to page IDs
	 * @param wizVo
	 * @throws IOException
	 */
	public void callWizard(SBWizardVO wizVo) throws IOException {
		log.debug("Calling Wizard: " + wizVo.getPostData());
		conn.retrieveDataViaPost(SB_ADMIN_URL, wizVo.getPostData());
		
		// After building the site, get a collection of the page IDs
		this.getPageInfo(wizVo.getSiteId());
	}
	
	/**
	 * Adds the content as a portlet and associates the portlet to the
	 * appropriate page
	 * @param pageType
	 * @param data
	 */
	public void associateContent(String orgId, Map<String, List<PSPContent>> orgData) 
	throws IOException {
		Set<String> s = orgData.keySet();
		Map<String, String> contentXRef = new HashMap<String, String>();
		for(Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
			String pageType = iter.next();
			List<PSPContent> data = orgData.get(pageType);
			for (int i=0; i < data.size(); i++) {
				PSPContent content = data.get(i);
				String moduleId = null;
				if (! contentXRef.containsKey(content.getContentName())) {
					// Add Content as a portlet
					StringBuilder conUrl = new StringBuilder();
					conUrl.append("requestType=reqUpdate&actionId=CONTENT&moduleTypeId=CONTENT");
					conUrl.append("&organizationId=").append(orgId).append("&insertAction=true");
					conUrl.append("&actionName=").append(content.getEncodedContentName());
					conUrl.append("&actionDesc=").append(content.getEncodedContentName());
					conUrl.append("&articleText=").append(content.getEncodedContentText());
					conn.retrieveDataViaPost(SB_ADMIN_URL, conUrl.toString());
					
					// Get the actionId for the module
					moduleId = this.getActionId(content.getContentName(), orgId);
				} else {
					moduleId = contentXRef.get(content.getContentName());
				}
				
				//Associate to a page
				StringBuilder modUrl = new StringBuilder();
				modUrl.append("requestType=reqUpdate&actionId=PAGE_MODULE");
				modUrl.append("&moduleTypeId=CONTENT&organizationId=").append(orgId);
				modUrl.append("&pageId=").append(pageMap.get(pageType));
				modUrl.append("&pmTemplateId=").append("");
				modUrl.append("&moduleId=").append(moduleId);
				modUrl.append("&moduleDisplayId=").append("c0a80a07614b3c24224dd3d77221237a");
				modUrl.append("&displayOrder=3&displayColumn=").append(content.getColumn());
				modUrl.append("&roleId=0&roleId=10&roleId=100");
				conn.retrieveDataViaPost(SB_ADMIN_URL, modUrl.toString());
			}
		}
	}
	
	/**
	 * Calls css creation/update portion of admin tool and creates simple PSP
	 * CSS styles in site CSS.
	 * @param cssTxt
	 * @throws IOException
	 */
	public void callCSSWizard(String orgId, String siteId, String cssTxt) throws IOException {
		log.debug("Calling CSS wizard...");
		StringBuffer cssUrl = new StringBuffer();
		cssUrl.append("requestType=reqUpdate");
		cssUrl.append("&actionId=SITE_CSS");
		cssUrl.append("&organizationId=").append(orgId);
		cssUrl.append("&siteId=").append(siteId);
		cssUrl.append("&exists=false");
		cssUrl.append("&fileId=");
		cssUrl.append("&cssCode=").append(cssTxt);
		conn.retrieveDataViaPost(SB_ADMIN_URL, cssUrl.toString());
	}
	
	/**
	 * Creates additional site aliases for each site if additional aliases
	 * exist.  For the purposes of migration, all additional aliases are set as
	 * 'primary = no'.
	 * @param siteId
	 * @param urls
	 * @throws IOException
	 */
	public void callSiteAliasWizard(String siteId, List<String> urls) 
			throws IOException {
		log.debug("Calling Site Alias wizard...");
		if (urls.size() > 0) {
			StringBuffer aliasUrl = null;
			for(String alias : urls) {
				if(alias.length() == 0) continue;
				aliasUrl = new StringBuffer();
				aliasUrl.append("requestType=reqUpdate");
				aliasUrl.append("&actionId=ALIAS");
				aliasUrl.append("&siteId=").append(siteId);
				aliasUrl.append("&siteAliasId=");
				aliasUrl.append("&siteAliasUrl=").append(alias);
				aliasUrl.append("&googleKey=");
				aliasUrl.append("&googleSitemapKey=");
				aliasUrl.append("&msnSitemapKey=");
				aliasUrl.append("&yahooSitemapKey=");
				aliasUrl.append("&primaryFlg=0");
				conn.retrieveDataViaPost(SB_ADMIN_URL, aliasUrl.toString());
			}
		} else {
			return;
		}
	}
	
	/**
	 * Adds certain SJM admins as org admin users for every PSP site migrated
	 * @param orgId
	 * @throws IOException
	 */
	public void callOrgAdminUserWizard(String orgId) throws IOException {
		log.debug("Calling org admin user wizard...");
		String[] sjmAdmins = {
				/* prod profile ID's for John, Liza, Michelle, Georgia, not in order */
				"c0a80228d4eebe126e232d2519f01b0",
				"c0a8021ebeacc522ddeae269a4ef7252",
				"c0a80241254302a17c205471a505a48a",
				"c0a802283a0d09b1e0204a632ed32d28",
				"c0a80237940b32b4f4be681ec324b67d"};
		
		for (String profileId : sjmAdmins) {
			StringBuffer adminUrl = new StringBuffer();
			adminUrl.append("requestType=reqUpdate");
			adminUrl.append("&actionId=ADMIN_USERS");
			adminUrl.append("&organizationId=").append(orgId);
			adminUrl.append("&profileId=").append(profileId);
			conn.retrieveDataViaPost(SB_ADMIN_URL, adminUrl.toString());
		}
	}
	
	/**
	 * 
	 * @param orgId
	 * @param siteId
	 */
	public void callSiteImageWizard(PspSiteVO pv)  throws IOException {
		log.debug("Calling site image wizard...");
		StringBuffer imageUrl = new StringBuffer();
		imageUrl.append("requestType=reqUpdate");
		imageUrl.append("&actionId=SITE_IMAGE");
		imageUrl.append("&adminFlat=0");
		imageUrl.append("&organizationId=");
		imageUrl.append("&siteId=");
		imageUrl.append("&theme2Image=");
		conn.retrieveDataViaPost(SB_ADMIN_URL, imageUrl.toString());
	}
	
	/**
	 * Retrieves the list of pages by URL (ALIAS) and pageId
	 * @param orgId
	 * @return
	 */
	protected void getPageInfo(String siteId) {
		String s = "select * from page where site_id = ?";
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, siteId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				pageMap.put(rs.getString("page_alias_nm"), rs.getString("page_id"));
			}
		} catch (Exception e) {
			log.error("Unable to retrieve page info", e);
		}
		
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public String getActionId(String name, String orgId) {
		String s = "select action_id from sb_action ";
		s += "where MODULE_TYPE_ID = 'CONTENT' and ORGANIZATION_ID = ? ";
		s += "and ACTION_NM = ? ";
		
		PreparedStatement ps = null;
		String actionId = "";
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, orgId);
			ps.setString(2, name);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				actionId = rs.getString(1);
			}
		} catch (Exception e) {
			log.error("Unable to retrieve page info", e);
		}
		
		return actionId;
	}
	
	/**
	 * Closes the SQL db connection.
	 */
	public void close() throws Exception {
		try {
			if(dbConn != null) {
				dbConn.close();
			}
		} catch (Exception e) {
			throw new Exception("Error closing SQL db connection...",e);
		}
	}
}

