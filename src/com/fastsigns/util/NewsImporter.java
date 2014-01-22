package com.fastsigns.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: NewsImporter.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 17, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class NewsImporter {
	// Admintool Login info
	private static String admintoolUrl = "http://localhost/sb/admintool";
	private static String adminUser = "james@siliconmtn.com";
	private static String adminPass = "cannondale";
	private static SMTHttpConnectionManager adminConn = null;
	
	protected static Logger log = Logger.getLogger(NewsImporter.class);
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String dbUrl = "jdbc:sqlserver://sql_server_db:1433;selectMethod=cursor;responseBuffering=adaptive";
	private static final String dbUser = "sb_user";
	private static final String dbPass = "sqll0gin";
	private static Connection conn = null;
	
	public NewsImporter() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		adminLogin();
		openDBConnection();
		
		List<NewsVO> data = getNews();
		
		for (int i = 0; i < data.size(); i++) {
			NewsVO vo = data.get(i);
			loadContent(vo);
		}
	}
	
	/**
	 * Adds the custom content to the CMS portlet
	 * @param page
	 * @return Action ID of the added portlet
	 * @throws Exception
	 */
	public static void loadContent(NewsVO news) throws Exception {
		String fileName = news.id + ".html";
		String summ = URLEncoder.encode(StringUtil.checkVal(news.desc), "UTF-8");
		String title = URLEncoder.encode(StringUtil.checkVal(news.title), "UTF-8");
		String pContent = URLEncoder.encode(StringUtil.checkVal(news.content), "UTF-8");
		
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
		String encDate = URLEncoder.encode(formatter.format(news.create), "UTF-8");
		
		String url = "actionId=CMS&requestType=reqUpdate";
		url += "&organizationId=FTS&83007=FS+Existing&83009=en&83013=000";
		url += "&folderId=84010&83014=" + encDate;
		url += "&83015=" + summ + "&83016=" + title + "&fileName=" + fileName;
		url += "&83010=" + title + "&83011=" + summ + "&89055=Press+Release";
		url += "&articleData=" + pContent;
		
		log.debug("**** Adding CMS Content: " + fileName);
		adminConn.retrieveDataViaPost(admintoolUrl, url);
	}
	
	
	private static List<NewsVO> getNews() throws SQLException {
		String s = "select * from fastsigns.dbo.custom_FastSignsNews ";
		PreparedStatement ps = conn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		
		List<NewsVO> news = new ArrayList<NewsVO>();
		while (rs.next()) {
			NewsVO vo = new NewsImporter().new NewsVO();
			vo.id = rs.getString("FastSignsNewsId");
			vo.title = rs.getString("title");
			vo.tagline = rs.getString("tagline");
			vo.content = rs.getString("content");
			vo.create = rs.getDate("date");
			vo.desc = rs.getString("description");
			
			news.add(vo);
		}
		
		return news;
	}
	
	private static void openDBConnection() {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		try {
			conn = dbc.getConnection();
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * @throws IOException 
	 * 
	 */
	public static void adminLogin() throws IOException {
		adminConn = new SMTHttpConnectionManager();
		adminConn.setFollowRedirects(false);
		String url = "loginEmailAddress=" + adminUser + "&password=" + adminPass;
		adminConn.retrieveDataViaPost(admintoolUrl,url);
		adminConn.retrieveData(admintoolUrl + "?cPage=index");
	}
	
	class NewsVO {
		String id = null;
		String title = null;
		String tagline = null;
		String desc = null;
		String content = null;
		Date create = null;
		
	}

}
