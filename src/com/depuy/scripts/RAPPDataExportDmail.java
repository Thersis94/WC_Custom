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
import com.siliconmtn.util.StringUtil;


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
public class RAPPDataExportDmail {
   
	private static String DB_URL = "jdbc:sqlserver://10.0.20.63:1433";
    private static String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
    private static String[] DB_AUTH = new String[] {"sb_user", "sqll0gin"};
    private static String exportFile = "/data/dmail.csv";
    public static final Logger log = Logger.getLogger(RAPPDataExportDmail.class);

	RAPPDataExportDmail() {
    	PropertyConfigurator.configure("/data/log4j.properties");
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length > 0) exportFile = args[0];
        
        RAPPDataExportDmail db = new RAPPDataExportDmail();
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
					if (vo.mails.size() < x+1) {
						b.append(",,,");
					} else {
						MailVO eml = vo.mails.get(x);
						b.append("\"" + eml.mailName + "\",");
						b.append("\"" + eml.sent + "\",");
						b.append("\"" + eml.failReason + "\",");
					}
				}
				b.append("\r\n");
				
				bos.write(b.toString().getBytes());
				b = null;
				vo = null;
			}
			bos.close();
		} catch (Exception e) {
			System.err.println("Error Processing ... " + e.getMessage());
			e.printStackTrace();
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
		sql.append("select SKU_DESC, a.PROFILE_ID, isnull(b.KIT_SENT_DT, b.process_start_dt) as 'kit_sent_dt', ");
		sql.append("b.PROCESS_FAIL_TXT, a.ATTEMPT_DT, b.CREATE_DT ");
		sql.append("from DATA_FEED.dbo.CUSTOMER a ");
		sql.append("inner join DATA_FEED.dbo.fulfillment b on a.customer_id=b.customer_id and b.FULFILLMENT_TYPE_CD='MAIL' ");
		sql.append("inner join DATA_FEED.dbo.fulfillment_sku c on b.SKU_CD=c.SKU_CD ");
		sql.append("where a.ATTEMPT_DT > '2009-01-01 00:00:00' and isnull(b.KIT_SENT_DT, b.process_start_dt) is not null ");
		sql.append("group by a.PROFILE_ID, SKU_DESC, isnull(b.KIT_SENT_DT, b.process_start_dt), b.PROCESS_FAIL_TXT, a.ATTEMPT_DT, b.CREATE_DT ");
		sql.append("order by a.ATTEMPT_DT desc, b.CREATE_DT desc");
		
		String lastProfileId = "";
		RecordVO vo = null;
		Map<String,RecordVO> userList = new LinkedHashMap<String,RecordVO>();
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (userList.containsKey(rs.getString("profile_id"))) {
					vo = userList.get(rs.getString("profile_id"));
				} else {
					if (lastProfileId.length() > 0) userList.put(vo.profileId, vo);
					vo = new RecordVO();
					vo.profileId = rs.getString("profile_id");
				}
				
				MailVO eml = new MailVO();
				eml.mailName = rs.getString("sku_desc");
				eml.sent = rs.getDate("kit_sent_dt");
				eml.failReason = StringUtil.checkVal(rs.getString("PROCESS_FAIL_TXT"));
				vo.mails.add(eml);
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
		sb.append("\"ProfileId\",");
		sb.append("\"Mail 1 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Fail Reason\",");
		sb.append("\"Mail 2 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Fail Reason\",");
		sb.append("\"Mail 3 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Fail Reason\",");
		sb.append("\"Mail 4 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Fail Reason\",");
		sb.append("\"Mail 5 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Fail Reason\",");
		sb.append("\"Mail 6 Name\",");
		sb.append("\"Sent Date\",");
		sb.append("\"Fail Reason\",");
		sb.append("\r\n");
		return sb.toString();
	}
	
	
	private class MailVO {
		String mailName = null;
		Date sent = null;
		String failReason = null;
	}
	
	private class RecordVO {
		List<MailVO> mails = new ArrayList<MailVO>();
		String profileId = null;
	}
}
