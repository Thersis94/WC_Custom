package com.fastsigns.cutover;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: FSRedirectMapper.java <p/>
 * <b>Project</b>: Sandbox <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 25, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FSRedirectMapper {
	private static Logger log = Logger.getLogger("FSRedirectMapper");
	static int ctr = 0;
	
	// Database Connection info
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String wcDbUrl = "jdbc:sqlserver://192.168.56.101:31433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String sbUser = "sb_user";
	private static final String wcUser = "wc_user";
	private static final String sbPass = "sqll0gin";
		
	/**
	 * 
	 */
	public FSRedirectMapper() {
		
	}

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		Connection sbConn = getDBConnection(dbUrl, sbUser);
		Connection wcConn = getDBConnection(wcDbUrl, wcUser);
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] b = conn.retrieveData("http://www.fastsigns.com/sitemap.xml");

		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Element e = doc.getRootElement();
		Iterator<Element> iter = e.elementIterator("url");
		
		Map<String, String> links = new TreeMap<String, String>();
		while(iter.hasNext()) {
			Element ele = iter.next();
			String link = ele.elementText("loc");
			if (link.toLowerCase().indexOf("cmshelp") > -1) continue;
			if (link.toLowerCase().indexOf(".png") > -1) continue;
			if (link.toLowerCase().indexOf(".gif") > -1) continue;
			if (link.toLowerCase().indexOf(".jpg") > -1) continue;
			if (link.toLowerCase().indexOf(".ico") > -1) continue;
			if (link.toLowerCase().indexOf("-png") > -1) continue;
			if (link.toLowerCase().indexOf("-gif") > -1) continue;
			if (link.toLowerCase().indexOf("-jpg") > -1) continue;
			if (link.toLowerCase().indexOf("-ico") > -1) continue;
			if (link.toLowerCase().indexOf("-css") > -1) continue;
			if (link.toLowerCase().indexOf(".css") > -1) continue;
			if (link.toLowerCase().indexOf("-js") > -1) continue;
			if (link.toLowerCase().indexOf(".js") > -1) continue;
			if (link.toLowerCase().indexOf("-swf") > -1) continue;
			if (link.toLowerCase().indexOf(".swf") > -1) continue;
			if (link.toLowerCase().indexOf("scriptresource") > -1) continue;
			if (link.toLowerCase().indexOf("webresource") > -1) continue;
			if (link.toLowerCase().indexOf("-f4v") > -1) continue;
			if (link.toLowerCase().indexOf(".f4v") > -1) continue;
			if (link.toLowerCase().indexOf("getcss") > -1) continue;
			if (link.toLowerCase().indexOf("getfile") > -1) continue;
			if (link.toLowerCase().indexOf("/assets/") > -1) continue;
			if (link.toLowerCase().indexOf("/uploaded-files/") > -1) continue;
			if (link.toLowerCase().indexOf("/images/") > -1) continue;
			//if (! link.startsWith("/")) continue;
			
			int franchise = Convert.formatInteger(link.substring(link.lastIndexOf("/") + 1));
			if (franchise > 0) continue;
			
			String path = link.substring(24);
			if (path.length() > 2) {
				String val = getRedirValue(path, sbConn);
				if (val != null) links.put(path, val);
			}
		}
		
		log.debug("Number of Pages ************* : " + links.size());
		Set<String> s = links.keySet();
		for (Iterator<String> loop = s.iterator(); loop.hasNext(); ) {
			String key = loop.next();
			addRedirect(key, links.get(key), wcConn);
			
		}
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public static String getRedirValue(String key, Connection conn) 
	throws Exception {
		String newKey = key;
		newKey = newKey.replace("/Franchise", "");
		newKey = newKey.replace("-(1)", "");
		newKey = newKey.toLowerCase();
		int franchise = 0;
		
		if (newKey.indexOf("/", 1) > -1) {
			String fid = newKey.substring(1, newKey.indexOf("/", 1));
			franchise = Convert.formatInteger(fid);
		}
		
		if (franchise > 0) {
			String alias = getFranchiseUrl(franchise, conn);
			newKey = "/" + alias + newKey.substring(newKey.indexOf("/", 1));
		} else if (newKey.indexOf("/locationsresults") > -1) {
			String metro = newKey.substring(newKey.indexOf("=") + 1).replace("%20", " ");
			if (metro.indexOf("mexico") > -1) return null;
			
			if (metro.indexOf("dallas") > -1) newKey="/metro/qs/dallas_fort_worth ";
			else newKey = "/metro/qs/" + getMetroAlias(metro, conn);
		} else {
			newKey = null;
		}
		
		return newKey;
	}
	
	/**
	 * 
	 * @param key
	 * @param value
	 * @param conn
	 * @throws SQLException
	 */
	public static void addRedirect(String key, String value, Connection conn) throws SQLException {
		String s = "insert into site_redirect(site_redirect_id, site_id, redirect_alias_txt, ";
		s += "destination_url, active_flg, create_dt, global_flg, permanent_redir_flg)   ";
		s+= "values ('" + new UUIDGenerator().getUUID() + "', 'FTS_1','" + key;
		s += "','" + value + "', 1, '1/28/2011', 0, 1)";
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.executeUpdate();
			log.debug("Added: " + value);
		} finally {
			try {
				ps.close();
			}catch(Exception e){}
		}
	}
	
	/**
	 * 
	 * @param metro
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static String getMetroAlias(String metro, Connection conn) throws SQLException {
		String s = "select * from sitebuilder_custom.dbo.fts_metro_area where area_nm = ? ";
		String alias = "";
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setString(1, metro);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				alias = rs.getString("area_alias_nm");
			} else {
				log.debug("******** Couldn't locate: " + metro);
			}
		} finally {
			try {
				ps.close();
			}catch(Exception e){}
		}
		
		return alias;
	}
	
	/**
	 * 
	 * @param fid
	 * @return
	 */
	public static String getFranchiseUrl(int fid, Connection conn) throws SQLException {
		String s = "select * from dealer_location where dealer_location_id = ?";
		String alias = "";
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setString(1, fid + "");
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				alias = rs.getString("location_alias_nm");
			}
		} finally {
			try {
				ps.close();
			}catch(Exception e){}
		}
		
		return alias;
	}
	
	/**
	 * Creates a connection to the database
	 * @param dbUser Database User
	 * @param dbPass Database Password
	 * @return JDBC connection for the supplied user
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private static Connection getDBConnection(String url, String user) 
	throws InvalidDataException, DatabaseException  {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, url, user, sbPass);
		return dbc.getConnection();
	}

}
