package com.fastsigns.cutover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.PageVO;

/****************************************************************************
 * <b>Title</b>: SubPageHeadline.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 8, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SubPageHeadline {
	// Database Connection info
	private final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	//private final String dbUrl = "jdbc:sqlserver://192.168.56.101:31433;selectMethod=cursor;responseBuffering=adaptive";
	private final String sbUser = "sb_user";
	//private final String sbUser = "wc_user";
	private final String sbPass = "sqll0gin";
	//private String customDb = "sitebuilder_custom.dbo.";
	//private String customDb = "wc_custom.dbo.";
	private Connection sbConn = null;
	private static Logger log = Logger.getLogger("SubPageHeadline");
	
	private Connection upConn = null;
	//private final String upDbUrl = "jdbc:sqlserver://192.168.56.101:31433;selectMethod=cursor;responseBuffering=adaptive";
	//private final String upDbUser = "wc_user";
	//private String customDb = "wc_custom.dbo.";
	private final String upDbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private final String upDbUser = "sb_user";
	private String customDb = "sitebuilder_custom.dbo.";
	
	private final String upDbPass = "sqll0gin";
	
	/**
	 * 
	 */
	public SubPageHeadline() throws Exception {
		BasicConfigurator.configure();
		log.debug("Starting ...");
		
		sbConn = this.getDBConnection(sbUser, sbPass, dbUrl);
		upConn = this.getDBConnection(upDbUser, upDbPass, upDbUrl);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SubPageHeadline sph = new SubPageHeadline();
		sph.processRequest();
		
			
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void processRequest() throws Exception {
		// Get the franchise ids
		List<GenericVO> fids = this.getFranchiseIds();
		Set<String> paths = new HashSet<String>();
		
		// Loop the Franchises
		for (int i=0; i < fids.size(); i++) {
			GenericVO vo = fids.get(i);
			int fid = (Integer) vo.getKey();
			String locationName = (String) vo.getValue();
			//log.debug(locationName + "|" + fid);
			
			// Get the SMT pages that have content portlet
			List<PageVO> pages = this.getSMTPages(fid);
			
			// Loop the pages
			for (int p = 0; p < pages.size(); p++) {
				PageVO page = pages.get(p);
				//log.debug("Pages: " + fid + "|" + page.getFullPath());
				
				// Get the FS sub-headline that matches the page
				SHBean headline = this.getSubHeadline(page, fid);
				
				// If an image exists, get and parse the image path
				if (headline != null) {
					//log.debug("\t Matching: " + headline.headline);
					if(StringUtil.checkVal(headline.guid).length() > 0) {
						String imagePath = this.getImageFromGuid(headline.guid);
						if (imagePath == null) continue;
						
						//log.debug(imagePath);
						paths.add(imagePath);
						headline.imaagePath = imagePath;
					}
				} else {
					continue;
				}
			
				// Update the PAGE table with the image (Update URL if necessary)
				this.updatePage(page, headline, fid, locationName);
				
				// update the content action
				this.updateAction(page, headline);
			}
		}
		
		log.debug("************************\n\n");
		for(Iterator<String> iter = paths.iterator(); iter.hasNext(); ) {
			log.debug(iter.next());
		}
	}
	
	/**
	 * 
	 * @param page
	 * @param hdl
	 * @throws Exception
	 */
	public void updateAction(PageVO page, SHBean hdl) throws Exception {
		String s = "update sb_action set action_nm = ?, action_desc = ?, update_dt = ? ";
		s += "where action_id = ? ";
		//log.debug("\tActionUp: " + s + "|" + hdl.headline + "|" + hdl.headlineText + "|" + page.getJavaScript());
		//log.debug("Headline: " + StringUtil.checkVal(hdl.headline).length());
		//log.debug("HeadlineText: " + StringUtil.checkVal(hdl.headlineText).length());
		
		// Using the page.javascript element for the actionid
		PreparedStatement ps = upConn.prepareStatement(s);
		ps.setString(1, hdl.headline);
		ps.setString(2, hdl.headlineText);
		ps.setTimestamp(3, Convert.getCurrentTimestamp());
		ps.setString(4, page.getJavaScript());
		ps.executeUpdate();
		ps.close();
		
	}
	
	/**
	 * 
	 * @param page
	 * @param hdl
	 * @param url
	 * @param alias
	 * @throws Exception
	 */
	public void updatePage(PageVO page, SHBean hdl, int fid, String locName) 
	throws Exception {
		String s = "update page set page_1_img = ?, page_title_nm = ?, ";
		s += "full_path_txt = ?, meta_keyword_txt = ?, meta_desc_txt = ?, ";
		s += "update_dt = ? where page_id = ? ";
		//log.debug("Page Up: " + s + "|" + hdl.imaagePath + "|" + hdl.url + "|" + page.getPageId() + "|" + "FTS_" + fid + "_1");
		
		// Get the proper Title
		String name = StringUtil.checkVal(hdl.pageTitle);
		if (name.length() == 0) name = locName + " : " + hdl.alias;
		log.debug("Title " + fid + " : " + name);
		
		// Get the proper keyword/desc
		String mKey = StringUtil.checkVal(hdl.metaKeyword, page.getMetaKeyword());
		String mDesc = StringUtil.checkVal(hdl.metaKeyword, page.getMetaDesc());
		
		PreparedStatement ps = upConn.prepareStatement(s);
		ps.setString(1, hdl.imaagePath);
		ps.setString(2, name);
		ps.setString(3, hdl.url);
		ps.setString(4, mKey);
		ps.setString(5, mDesc);
		ps.setTimestamp(6, Convert.getCurrentTimestamp());
		ps.setString(7, page.getPageId());
		ps.executeUpdate();
		ps.close();
		
	}
	
	
	/**
	 * 
	 * @param guid
	 * @return
	 * @throws Exception
	 */
	public String getImageFromGuid(String guid) throws Exception {
		String s = "select * ";
		s += "from fastsigns.dbo.CMS_Tree a  ";
		s += "inner join fastsigns.dbo.CMS_Document b on a.NodeID = b.DocumentNodeID "; 
		s += "where nodeguid != '00000000-0000-0000-0000-000000000000 ' ";
		s += "and  nodeguid = " + StringUtil.checkVal(guid, true);
		PreparedStatement ps = sbConn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		String path = null;
		if (rs.next()) {
			path = rs.getString("nodealiaspath");
			String ext = StringUtil.checkVal(rs.getString("DocumentType")).toLowerCase();
			
			if (path.indexOf("/Uploaded-Files") > -1) {
				path = "/binary/org/FTS" + path;
			} else {
				path = "/binary/org/FTS_" + path.substring(11);
			}
			
			// Add the doc type
			if (path.indexOf(".") == -1) path += ext;
		}
		
		return path;
	}
	
	/**
	 * 
	 * @param page
	 * @param fid
	 * @return
	 * @throws Exception
	 */
	public SHBean getSubHeadline(PageVO page, int fid) throws Exception {
		String s = "select replace(headline, '&nbsp;', '') as headline1, replace(SubHeadlineText, '&nbsp;','') as SubHeadlineText1,"; 
		s += "DocumentUrlPath, NodeName, DocumentName, DocumentPageTitle, DocumentPageDescription, DocumentPageKeyWords ,  c.* ";
		s += "from fastsigns.dbo.CMS_Tree a  ";
		s += "inner join fastsigns.dbo.CMS_Document b on a.NodeID = b.DocumentNodeID "; 
		s += "inner join fastsigns.dbo.custom_franchiseSubPage c on b.DocumentForeignKeyValue = c.FranchiseSubPageID ";
		s += "where NodeAliasPath like '/Franchise/" + fid + "/%' ";
		
		String prefix = "/" + fid;
		
		PreparedStatement ps = sbConn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		SHBean sh = null;
		while (rs.next()) {
			String urlPath = StringUtil.checkVal(rs.getString("DocumentUrlPath"));
			if (prefix.length() >= urlPath.length()) continue;
			urlPath = urlPath.substring(prefix.length());
			if (urlPath.equalsIgnoreCase(page.getFullPath())) {
				sh = new SHBean();
				sh.headline = rs.getString("headline1");
				sh.headlineText = rs.getString("SubHeadlineText1");
				sh.guid = rs.getString("SubHeadlineImage");
				sh.url = urlPath;
				sh.metaDesc = rs.getString("DocumentPageDescription");
				sh.metaKeyword = rs.getString("DocumentPageKeyWords");
				sh.pageTitle = rs.getString("DocumentPageTitle");
				sh.alias = rs.getString("DocumentName");
			}

		}
		
		ps.close();
		
		return sh;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<GenericVO> getFranchiseIds() throws Exception {
		String s = "select franchise_id, location_nm from " + customDb + "fts_franchise a ";
		s += "inner join dealer_location b on a.franchise_id = b.dealer_location_id ";
		//s += "where CAST(franchise_id as int)  < 11 ";
		s+= " order by CAST(franchise_id as int)  ";
		
		PreparedStatement ps = upConn.prepareStatement(s); 
		ResultSet rs = ps.executeQuery();
		List<GenericVO> f = new ArrayList<GenericVO>();
		
		while (rs.next()) {
			f.add(new GenericVO(rs.getInt(1), rs.getString(2)));
		}
		ps.close();
		
		return f;
	}
	
	/**
	 * 
	 * @param f
	 * @return
	 * @throws Exception
	 */
	public List<PageVO> getSMTPages(int f) throws Exception {
		String s = "select * from PAGE a inner join PAGE_MODULE b ";
		s += "on a.PAGE_ID = b.PAGE_ID	where SITE_ID = 'FTS_" + f + "_1'  ";
		
		PreparedStatement ps = upConn.prepareStatement(s); 
		ResultSet rs = ps.executeQuery();
		List<PageVO> pages = new ArrayList<PageVO>();
		
		while (rs.next()) {
			
			// Use the javascript field to hold the action_id
			PageVO p = new PageVO(rs);
			p.setJavaScript(rs.getString("action_id"));
			pages.add(p);
			
		}
		
		ps.close();
		
		return pages;
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

}

class SHBean {
	public String headline = null;
	public String headlineText = null;
	public String guid = null;
	public String imaagePath = null;
	public String alias = null;
	public String url = null;
	public String pageTitle = null;
	public String metaKeyword = null;
	public String metaDesc = null;
	
}
