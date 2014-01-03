package com.depuy.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;


/****************************************************************************
 * <b>Title</b>: RecontactStudyExport.java<p/>
 * <b>Description: Exports encrypted profile data to a flat file.  Written for use by
 * DDB to perform recontact studies.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 12, 2008
 ****************************************************************************/
public class RecontactStudyExport {
   
	//private static String DB_URL = "jdbc:sqlserver://192.168.3.120:1433";
	private static String DB_URL = "jdbc:sqlserver://10.0.20.63:1433";
    private static String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
    private static String[] DB_AUTH = new String[] {"sb_user", "sqll0gin"};
    private static String exportFile = "/data/profiles.xls";
    private static final String DELIMITER = "</td><td>";
    private static final String encKey = "s1l1c0nmtnT3chm0l0g13$JC"; //from SB's config
    public static final Logger log = Logger.getLogger(RecontactStudyExport.class);
	StringEncrypter se = null;

	RecontactStudyExport() {
    	PropertyConfigurator.configure("/data/log4j.properties");
    	try {
    		se = new StringEncrypter(encKey);
    	} catch (Exception e) {}
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length > 0) exportFile = args[0];
        
        RecontactStudyExport db = new RecontactStudyExport();
		try {
			System.out.println("exportFile=" + exportFile);
			List<UserDataVO> data = db.getData();
			
			//write the profiles to the export file in the desired format
			File f = new File(exportFile);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(getHeaderRow().getBytes());
			Iterator<UserDataVO> iter = data.iterator();
			while (iter.hasNext()) {
				UserDataVO vo = iter.next();
				StringBuffer b = new StringBuffer("<tr><td>");
				b.append(vo.getProfileId()).append(DELIMITER).append(vo.getPrefixName()).append(DELIMITER);
				b.append(vo.getFirstName()).append(DELIMITER).append(vo.getLastName()).append(DELIMITER);
				List<PhoneVO> phones = vo.getPhoneNumbers();
				String hPhone = "";
				String dPhone = "";
				try {
					PhoneVO ph = phones.get(0);
					if (ph.getPhoneType().equals(PhoneVO.HOME_PHONE)) {
						hPhone = ph.getFormattedNumber();
					} else if (ph.getPhoneType().equals(PhoneVO.DAYTIME_PHONE)) {
						dPhone = ph.getFormattedNumber();
					}
					if (dPhone.length() == 0) {
						dPhone = phones.get(1).getFormattedNumber();
					}
				} catch (Exception e) {}
				
				b.append(hPhone).append(DELIMITER).append(dPhone).append(DELIMITER);
				b.append((vo.getBirthYear() > 0) ? 2009-vo.getBirthYear() : "").append(DELIMITER);
				b.append(vo.getBestTime()).append(DELIMITER).append(vo.getAliasName()).append(DELIMITER);
				b.append(vo.getCounty()).append(DELIMITER).append(vo.getBarCodeId()).append(DELIMITER);
				b.append(vo.getBirthDate()).append(DELIMITER).append(vo.getAddress()).append(DELIMITER);
				b.append(vo.getCity()).append(DELIMITER).append(vo.getState()).append(DELIMITER);
				b.append(vo.getZipCode()).append("</td></tr>");
				
				bos.write(b.toString().getBytes());
				b = null;
				vo = null;
			}
			bos.write(new String("</table>").getBytes());
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
	private List<UserDataVO> getData() throws Exception {
		
		//Open DB Connection
		Connection dbConn = getDBConnection(DB_AUTH[0], DB_AUTH[1], DB_DRIVER, DB_URL);
		
		StringBuffer sql = new StringBuffer();
		sql.append("select * from data_feed.dbo.customer a ");
		sql.append("inner join profile b on a.profile_id=b.profile_id ");
		sql.append("inner join profile_address c on b.profile_id=c.profile_id ");
		sql.append("inner join phone_number d on a.profile_id=d.profile_id and d.phone_type_cd in ('HOME','DAYTIME') ");
		sql.append("left outer join org_profile_comm e on b.profile_id=e.profile_id and e.organization_id='DEPUY' ");
		sql.append("where a.attempt_dt between '08/08/2008 00:00:00' and '01/01/2009 00:00:00' ");
		sql.append("and (a.selection_cd='COACHK' or a.selection_cd='BRC') and a.result_cd='QUALIFIED' ");
		sql.append("and e.allow_comm_flg=1 ");
		sql.append("order by b.profile_id, d.phone_type_cd desc");
		
		String lastProfileId = "";
		UserDataVO vo = null;
		List<UserDataVO> userList = new ArrayList<UserDataVO>();
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!lastProfileId.equals(rs.getString("customer_id"))) {
					if (lastProfileId.length() > 0) userList.add(vo);
					
					vo = new UserDataVO();
					vo.setFirstName(decrypt(rs.getString("first_nm")));
					vo.setLastName(decrypt(rs.getString("last_nm")));
					vo.setPrefixName(rs.getString("prefix_nm"));
					vo.setBestTime(rs.getString("product_cd"));
					vo.setAliasName(StringUtil.checkVal(rs.getString("sec_product_cd")));
					vo.setAddress(decrypt(rs.getString("address_txt")));
					vo.setCity(rs.getString("city_nm"));
					vo.setState(rs.getString("state_cd"));
					vo.setZipCode(rs.getString("zip_cd"));
					vo.setCounty(rs.getString("call_source_cd"));
					vo.setBarCodeId((rs.getInt("lead_type_id") == 10) ? "Qualified Lead" : "Lead");
					vo.setBirthYear(rs.getInt("birth_year_no"));
					vo.setProfileId(rs.getString("customer_id"));
					vo.setBirthDate(rs.getDate("attempt_dt"));
					lastProfileId = vo.getProfileId();
				}
				//this is the only variable that may return duplicate records for a single user
				PhoneVO ph = new PhoneVO();
				ph.setPhoneNumber(rs.getString("phone_number_txt"));
				ph.setPhoneType(rs.getString("phone_type_cd"));
				vo.addPhone(ph);
				
			}
			//add the final record that terminated the RS
			if (lastProfileId.length() > 0) 
				userList.add(vo);
			
		} catch (SQLException de) {
			de.printStackTrace();
		}
				
		//close DB Connection
		closeConnection(dbConn);
		

    	//re-order the data using decrypted names
    	Collections.sort(userList, new UserDataComparator());
		
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
	
	private String decrypt(String val) {
		try {
			val = se.decrypt(val);
		} catch (Exception e) {}
		
		return val;
	}
	
	private static String getHeaderRow() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table border='1'><tr style='background-color:#ccc; border-bottom:2px solid #000'>");
		sb.append("<td>CustomerId</td>");
		sb.append("<td>Prefix</td>");
		sb.append("<td>First Name</td>");
		sb.append("<td>Last Name</td>");
		sb.append("<td>Home Phone</td>");
		sb.append("<td>Daytime Phone</td>");
		sb.append("<td>Age</td>");
		sb.append("<td>Pri Product</td>");
		sb.append("<td>Sec Product</td>");
		sb.append("<td>Record Source</td>");
		sb.append("<td>Lead Type</td>");
		sb.append("<td>Registration Date</td>");
		sb.append("<td>Address</td>");
		sb.append("<td>City</td>");
		sb.append("<td>State</td>");
		sb.append("<td>Zip Code</td></tr>");
		
		return sb.toString();
	}
}
