package com.depuy.scripts;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.depuy.datafeed.tms.modules.CustomerVO;
import com.depuy.datafeed.tms.modules.ProfileVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.constants.Constants;


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
public class RAPPDataExportSurvey {
   
	private static String DB_URL = "jdbc:sqlserver://10.0.20.63:1433";
    private static String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
    private static String[] DB_AUTH = new String[] {"sb_user", "sqll0gin"};
    private static String exportFile = "/data/survey.csv";
    public static final Logger log = Logger.getLogger(RAPPDataExportSurvey.class);
    private Map<String, String> questionMaster = new LinkedHashMap<String, String>();
    private static final String encKey = "s1l1c0nmtnT3chm0l0g13$JC";

	RAPPDataExportSurvey() {
    	PropertyConfigurator.configure("/data/log4j.properties");
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length > 0) exportFile = args[0];
        
        RAPPDataExportSurvey db = new RAPPDataExportSurvey();
		try {
			System.out.println("exportFile=" + exportFile);
			Map<String,RecordVO> data = db.getData();
			
			//write the profiles to the export file in the desired format
			File f = new File(exportFile);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(db.getHeaderRow().getBytes());
			
			for (RecordVO vo : data.values()) {
				StringBuffer b = new StringBuffer();
				b.append("\"" + vo.profileId + "\",");
				b.append("\"" + vo.getAttemptDate() + "\",");
				b.append("\"" + StringUtil.checkVal(vo.getProfile().getGenderCode()) + "\",");
				b.append("\"" + vo.getProfile().getBirthYear() + "\",");
				b.append("\"" + vo.getProfile().getCity() + "\",");
				b.append("\"" + vo.getProfile().getState() + "\",");
				b.append("\"" + vo.callSource + "\",");
				b.append("\"" + StringUtil.checkVal(vo.getProductCode()) + "\",");
				b.append("\"" + StringUtil.checkVal(vo.getSecondaryProductCode()) + "\",");
				b.append("\"" + vo.getCallTarget() + "\",");
				b.append("\"" + StringUtil.checkVal(vo.callReason) + "\",");
				b.append("\"" + vo.channelNm + "\",");
				b.append("\"" + vo.getLeadLabel() + "\",");
				b.append("\"" + StringUtil.checkVal(vo.getReferringPath()) + "\",");
				b.append("\"" + StringUtil.checkVal(vo.getReferringSite()) + "\",");
				
				//add all questions from the master list; the user's response for each one
				for (String q : db.questionMaster.keySet()) {
					b.append("\"").append(StringUtil.checkVal(vo.responses.get(q))).append("\",");
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
		sql.append("select e.CHANNEL_CD + ' (' + e.CHANNEL_DESC + ')' as 'channel_name', ");
		sql.append("a.CUSTOMER_ID,[PROFILE_ID],[CALL_REASON_CD],[RESULT_CD], ");
		sql.append("[CALL_SOURCE_CD],[PRODUCT_CD],[SELECTION_CD],[CALL_TARGET_CD],[EVENT_FORM_TXT], ");
		sql.append("[CALL_REASON_OTHER_TXT],[TOLL_FREE_NO],[ATTEMPT_DT],[LEAD_TYPE_ID],[REFERRING_PATH_TXT], "); 
		sql.append("[REFERRING_SITE_TXT],[SEC_PRODUCT_CD],b.RESPONSE_TXT,c.QUESTION_CD  ");
		sql.append("from DATA_FEED.dbo.CUSTOMER a  ");
		sql.append("left outer join DATA_FEED.dbo.CUSTOMER_RESPONSE b on a.CUSTOMER_ID=b.CUSTOMER_ID ");
		sql.append("inner join DATA_FEED.dbo.QUESTION_MAP c on b.QUESTION_MAP_ID=c.QUESTION_MAP_ID ");
		sql.append("inner join DATA_FEED.dbo.CUSTOMER_CHANNEL d on a.CUSTOMER_ID=d.CUSTOMER_ID ");
		sql.append("inner join DATA_FEED.dbo.CHANNEL e on d.CHANNEL_ID=e.CHANNEL_ID ");
		sql.append("where a.ATTEMPT_DT > '2009-01-01 00:00:00' ");
		sql.append("group by a.CUSTOMER_ID,[PROFILE_ID],[CALL_REASON_CD],[RESULT_CD],[CALL_SOURCE_CD], "); 
		sql.append("[PRODUCT_CD],[SELECTION_CD],[CALL_TARGET_CD],[EVENT_FORM_TXT],[CALL_REASON_OTHER_TXT], "); 
		sql.append("[TOLL_FREE_NO],[ATTEMPT_DT],[LEAD_TYPE_ID],[REFERRING_PATH_TXT],[REFERRING_SITE_TXT],  ");
		sql.append("[SEC_PRODUCT_CD],b.RESPONSE_TXT,c.QUESTION_CD, e.CHANNEL_CD + ' (' + e.CHANNEL_DESC + ')' ");
		sql.append("order by a.ATTEMPT_DT desc");
		log.debug(sql);
		String lastCustomerId = "";
		RecordVO vo = null;
		List<String> profileIds = new ArrayList<String>();
		Map<String,RecordVO> userList = new LinkedHashMap<String,RecordVO>();
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (userList.containsKey(rs.getString("customer_id"))) {
					vo = userList.get(rs.getString("customer_id"));
					
					//ignore anything but the most-recent attempt from this person (profileId)
					if (vo.getAttemptDate().after(rs.getDate("attempt_dt"))) {
						log.debug(rs.getDate("attempt_dt") + " is before " + vo.getAttemptDate());
						continue;
					}
					
				} else {
					if (lastCustomerId.length() > 0) userList.put(vo.getCustomerId(), vo);
					vo = new RecordVO();
					vo.setCustomerId(rs.getString("customer_id"));
					vo.setAttemptDate(rs.getDate("attempt_dt"));
					vo.callSource = rs.getString("call_source_cd");
					vo.setProductCode(rs.getString("product_cd"));
					vo.setSecondaryProductCode(rs.getString("sec_product_cd"));
					vo.setCallTarget(rs.getString("call_target_cd"));
					vo.callReason = rs.getString("call_reason_cd");
					vo.channelNm = rs.getString("channel_name");
					vo.setLeadTypeId(rs.getInt("lead_type_id"));
					vo.setReferringPath(rs.getString("REFERRING_PATH_TXT"));
					vo.setReferringSite(rs.getString("REFERRING_SITE_TXT"));
					vo.profileId = rs.getString("profile_id");
					
					//profiles w/address & phone# will be retrieved later
					profileIds.add(rs.getString("profile_id"));
				}
								
				//load the ReponseVO
				vo.responses.put(rs.getString("question_cd"), rs.getString("response_txt"));
				questionMaster.put(rs.getString("question_cd"), null);
				
				lastCustomerId = vo.getCustomerId();
				userList.put(vo.getCustomerId(), vo);
			}
			
			if (lastCustomerId.length() > 0)
				userList.put(vo.getCustomerId(), vo);
			
		} catch (SQLException de) {
			de.printStackTrace();
		}
		
		
		//call PM to load profile/core data
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(Constants.ENCRYPT_KEY, encKey);
		ProfileManager pm = new SBProfileManager(config);
		Map<String,UserDataVO> profiles = null;
		try {
			profiles = pm.searchProfileMap(dbConn, profileIds);
			
			for (RecordVO custVo : userList.values()) {
				if (!profiles.containsKey(custVo.profileId)) {
					log.error("no profile available for " + custVo.profileId);
					continue;
				}
				
				ProfileVO pro = new ProfileVO();
				pro.setData(profiles.get(custVo.profileId).getDataMap());
				custVo.setProfile(pro);
			}
		} catch (Exception e) {
			log.error("could not load profiles", e);
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
	
	
	
	private String getHeaderRow() {
		StringBuffer sb = new StringBuffer();
		//sb.append("<table border='1'><tr style='background-color:#ccc; border-bottom:2px solid #000'>");
		sb.append("\"ProfileId\",");
		sb.append("\"Attempt Date\",");
		sb.append("\"Gender\",");
		sb.append("\"Birth Year\",");
		sb.append("\"City\",");
		sb.append("\"State\",");
		sb.append("\"Call Source\",");
		sb.append("\"Pri Product\",");
		sb.append("\"Sec Product\",");
		sb.append("\"Call Target\",");
		sb.append("\"Call Reason\",");
		sb.append("\"Channel\",");
		sb.append("\"Lead Type\",");
		sb.append("\"Referring Path\",");
		sb.append("\"Referring Site\",");
		
		//add all questions from the master list
		for (String q : this.questionMaster.keySet()) {
			sb.append("\"").append(q).append("\",");
		}
		sb.append("\r\n");
		
		return sb.toString();
	}
	
		
	private class RecordVO extends CustomerVO {
		private static final long serialVersionUID = -6134048747680996870L;
		String callSource = null;
		String channelNm = null;
		String callReason = null;
		String profileId = null;
		Map<String, String> responses = new HashMap<String, String>();
		
		public String getLeadLabel() {
			switch (this.getLeadTypeId()) {
				case 10:	return "Qualified Lead";
				case 5:		return "Lead";
				default:	
					if (this.getProductCode().equals("SHUNT")) return "Lead";
					else return "Not a Lead";
			}
		}
	}
}
