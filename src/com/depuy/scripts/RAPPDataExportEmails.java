package com.depuy.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.exception.DatabaseException;


/****************************************************************************
 * <b>Title</b>: RAPPDataExportEmails.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 07, 2011
 ****************************************************************************/
public class RAPPDataExportEmails {
   
	private static String DB_URL = "jdbc:sqlserver://10.0.20.63:1433";
    private static String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
    private static String[] DB_AUTH = new String[] {"sb_user", "sqll0gin"};
    private static String exportFile = "/data/emails.csv";
    public static final Logger log = Logger.getLogger(RAPPDataExportEmails.class);

	RAPPDataExportEmails() {
    	PropertyConfigurator.configure("/data/log4j.properties");
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length > 0) exportFile = args[0];
        
        RAPPDataExportEmails db = new RAPPDataExportEmails();
		try {
			System.out.println("exportFile=" + exportFile);
			Map<String,RecordVO> data = db.getData();
			
			//write the profiles to the export file in the desired format
			File f = new File(exportFile);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(getHeaderRow().getBytes());
			
			for (RecordVO vo : data.values()) {
				StringBuffer b = new StringBuffer();
				b.append("\"" + vo.profileId + "\",");
				for (int x=0; x < 6; x++) {
					if (vo.emails.size() < x+1) {
						b.append(",,,,");
					} else {
						EmailVO eml = vo.emails.get(x);
						b.append("\"" + eml.campaignName + "\",");
						b.append("\"" + eml.sent + "\",");
						b.append("\"" + eml.opened + "\",");
						b.append("\"" + eml.clicked + "\",");
					}
				}
				b.append("\r\n");
				
				bos.write(b.toString().getBytes());
				b = null;
				vo = null;
			}
			//bos.write(new String("</table>").getBytes());
			bos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		}
		db = null;
	}
	
	/**
	 * 
	 * @param records
	 * @throws Exception
	 * 
	 */
	private Map<String,RecordVO> getData() throws Exception {
		//Open DB Connection
		Connection dbConn = getDBConnection(DB_AUTH[0], DB_AUTH[1], DB_DRIVER, DB_URL);
		
		StringBuffer sql = new StringBuffer();
		sql.append("select e.PROFILE_ID, e.attempt_dt, a.campaign_log_id, a.create_dt, b.instance_nm, ");
		sql.append("count(c.EMAIL_RESPONSE_ID) as 'open', count(d.EMAIL_RESPONSE_ID) as 'redir' ");
		sql.append("from email_campaign_log a ");
		sql.append("inner join email_campaign_instance b on a.campaign_instance_id=b.campaign_instance_id ");
		sql.append("left outer join EMAIL_RESPONSE c on a.CAMPAIGN_LOG_ID=c.CAMPAIGN_LOG_ID and c.RESPONSE_TYPE_ID='EMAIL_OPEN' ");
		sql.append("left outer join EMAIL_RESPONSE d on a.CAMPAIGN_LOG_ID=d.CAMPAIGN_LOG_ID and d.RESPONSE_TYPE_ID='REDIRECT' ");
		sql.append("inner join DATA_FEED.dbo.CUSTOMER e on a.PROFILE_ID=e.PROFILE_ID ");
		sql.append("where e.ATTEMPT_DT > '2009-01-01 00:00:00' ");
		sql.append("group by e.profile_id, e.attempt_dt, a.campaign_log_id, a.create_dt, b.instance_nm ");
		sql.append("order by e.PROFILE_ID, e.attempt_dt desc, a.CREATE_DT desc");
		
		String lastProfileId = "";
		RecordVO vo = null;
		Map<String,RecordVO> userList = new LinkedHashMap<String,RecordVO>();
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (userList.containsKey(rs.getString("profile_id"))) {
					vo = userList.get(rs.getString("profile_id"));
					
					//ignore anything older that this attempt
					if (vo.attemptDt.after(rs.getDate("attempt_dt"))) {
						log.debug(rs.getDate("attempt_dt") + " is before " + vo.attemptDt);
						continue;
					}

				} else {
					if (lastProfileId.length() > 0) userList.put(vo.profileId, vo);
					vo = new RecordVO();
					vo.profileId = rs.getString("profile_id");
					vo.attemptDt = rs.getDate("attempt_dt");
				}
								
				EmailVO eml = new EmailVO();
				eml.campaignName = rs.getString("instance_nm");
				eml.sent = rs.getDate("create_dt");
				if (rs.getInt("open") > 0) eml.opened = 1;
				if (rs.getInt("redir") > 0) eml.clicked = 1;
				vo.emails.add(eml);
				
				lastProfileId = vo.profileId;
			}
			
			if (lastProfileId.length() > 0)
				userList.put(vo.profileId, vo);
			
		} catch (SQLException de) {
			de.printStackTrace();
		}
				
		//close DB Connection
		closeConnection(dbConn);
		
    	log.debug("dataSize=" + userList.size());
		return userList;
	}
	
	/**
	 * 
	 * @param userName Login Account
	 * @param pwd Login password info
	 * @param driver Class to load
	 * @param url JDBC URL to call
	 * @return Database Conneciton object
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String userName, String pwd, String driver, String url) 
	throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver", cnfe);
		}
		
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, userName, pwd);
		} catch (SQLException sqle) {
			sqle.printStackTrace(System.out);
			throw new DatabaseException("Error Connecting to Database", sqle);
		}
		
		return conn;
	}

	private void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch(Exception e) {}
	}
	
	
	
	private static String getHeaderRow() {
		StringBuffer sb = new StringBuffer();
		//sb.append("<table border='1'><tr style='background-color:#ccc; border-bottom:2px solid #000'>");
		sb.append("\"ProfileId\",");
		sb.append("\"Email 1 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Opened\",");
		sb.append("\"Click-Through\",");
		sb.append("\"Email 2 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Opened\",");
		sb.append("\"Click-Through\",");
		sb.append("\"Email 3 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Opened\",");
		sb.append("\"Click-Through\",");
		sb.append("\"Email 4 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Opened\",");
		sb.append("\"Click-Through\",");
		sb.append("\"Email 5 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Opened\",");
		sb.append("\"Click-Through\",");
		sb.append("\"Email 6 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Opened\",");
		sb.append("\"Click-Through\"");
		sb.append("\r\n");
		
		return sb.toString();
	}
	
	
	private class EmailVO {
		String campaignName = null;
		Date sent = null;
		int opened = 0;
		int clicked = 0;
	}
	
	private class RecordVO {
		List<EmailVO> emails = new ArrayList<EmailVO>();
		String profileId = null;
		Date attemptDt = null;
	}
}
