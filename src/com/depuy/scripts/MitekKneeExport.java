package com.depuy.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;


/****************************************************************************
 * <b>Title</b>: MitekKneeExport.java<p/>
 * <b>Description: Exports decrypted dataFeed data to a flat file.  Written as
 * a one-time export for Courtney Franklin.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 26, 2010
 ****************************************************************************/
public class MitekKneeExport {
   
	private static String DESTINATION_DB_URL = "jdbc:sqlserver://10.0.20.43:2007";
    private static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
    private static String[] DESTINATION_AUTH = new String[] {"sb_user", "sqll0gin"};
    private static String exportFile = "/data/knee.csv";
    private static final String DELIMITER = ",";
    private static final String encKey = "s1l1c0nmtnT3chm0l0g13$JC"; //from SB's config
    public static final Logger log = Logger.getLogger(MitekKneeExport.class);

	StringEncrypter se = null;
	Boolean decryptableRow = true;

    MitekKneeExport() {
    	PropertyConfigurator.configure("/data/log4j.properties");

		try {
			se = new StringEncrypter(encKey);
		} catch (Exception e) {
			System.out.println("could not create StringEncryptor");
			System.exit(0);
		}
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length > 0) exportFile = args[0];
        
		MitekKneeExport db = new MitekKneeExport();
		db.run();
		db = null;
	}
	
	private void run() {
		Connection dbConn = null;
		PreparedStatement ps = null;
		try {
			System.out.println("exportFile=" + exportFile);
			dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
			ps = dbConn.prepareStatement(query()); 
			ResultSet rs = ps.executeQuery();
			
			//write the profiles to the export file in the desired format
			File f = new File(exportFile);
			FileOutputStream fos = new FileOutputStream(f);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			//bos.write(new String("<table border='1'>").getBytes());
			ResultSetMetaData rsmd = rs.getMetaData();
			int colCnt = rsmd.getColumnCount();
			
			//print column headings
			for (int x=1; x <= colCnt; x++) {
				//skip secondary phone# fields
				if (x < 10 || x > 11) bos.write(new String(rsmd.getColumnName(x) + DELIMITER).getBytes());
			}
			bos.write("\n".getBytes());
			
			//print the data
			
			for (; rs.next(); ) {
				decryptableRow = true;
				StringBuffer b = new StringBuffer("");
				
				for (int x=1; x <= colCnt; x++) {
					if ((x == 1 || x == 2 || x == 3 || x == 8) && decryptableRow) {
						b.append("\"" + decrypt(rs.getString(x)) + "\"");
					} else if (x == 9) {
						b.append("\"" + StringUtil.checkVal(rs.getString(9))).append(decrypt(rs.getString(10))).append(decrypt(rs.getString(11)) + "\"");
						x = 11;
					} else {
						b.append("\"" + StringUtil.checkVal(rs.getString(x)) + "\"");
					}
					if (x < colCnt) b.append(DELIMITER);
				}
				
				b.append("\n");
				bos.write(b.toString().getBytes());
			}
			//bos.write(new String("</table>").getBytes());
			bos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		} finally {
			try {
				ps.close();
				dbConn.close();
			} catch (Exception e) {}
		}
	}
	
	private String decrypt(String s) {
		String retVal = "";
		try {
			retVal = se.decrypt(s);
		} catch (Exception e) {
			if (s != null) retVal = s;
			decryptableRow = false;
		}
		
		return StringUtil.checkVal(retVal);
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

	
	public static final String query() {
		String sql = "select a.first_nm, a.last_nm, b.address_txt, b.address2_txt, " + 
		"b.city_nm, b.state_cd, b.zip_cd, a.email_address_txt, " + 
		"c.area_cd as 'PHONE_NO', c.exchange_cd, c.line_cd, " + 
		"a.birth_year_no, d.product_cd as 'PRI_PRODUCT',d.sec_product_cd " + 
		"as 'SEC_PRODUCT', d.call_reason_cd, d.call_source_cd, d.call_target_cd, " + 
		"e.response_txt as 'QUAL_KNEE_01', f.response_txt as 'QUAL_KNEE_02', " + 
		"g.response_txt as 'IMPACT_QUAL', h.response_txt as 'Q_HOW_SOON', " + 
		"i.response_txt as 'KNEE018', j.response_txt as 'DxOsteo', '' as 'DOCTOR_TYPE', " + 
		"k.allow_comm_flg as 'OPT_IN', d.attempt_dt, a.gender_cd, a.prefix_nm " +
		"from sitebuilder_depuy.dbo.profile a " + 
		"inner join sitebuilder_depuy.dbo.profile_address b on a.profile_id=b.profile_id " + 
		"inner join sitebuilder_depuy.dbo.phone_number c on a.profile_id=c.profile_id and (c.phone_type_cd='HOME' or c.phone_type_cd='DAYTIME') " + 
		"inner join data_feed.dbo.customer d on a.profile_id=d.profile_id " + 
		"inner join sitebuilder_depuy.dbo.org_profile_comm k on a.profile_id=k.profile_id and k.organization_id='DEPUY' " + 
		"left outer join data_feed.dbo.customer_response e on d.customer_id=e.customer_id and e.question_map_id=535 " + 
		"left outer join data_feed.dbo.customer_response f on d.customer_id=f.customer_id and f.question_map_id=537 " + 
		"left outer join data_feed.dbo.customer_response g on d.customer_id=g.customer_id and g.question_map_id=539 " + 
		"left outer join data_feed.dbo.customer_response h on d.customer_id=h.customer_id and h.question_map_id=542 " + 
		"left outer join data_feed.dbo.customer_response i on d.customer_id=i.customer_id and i.question_map_id=369 " + 
		"left outer join data_feed.dbo.customer_response j on d.customer_id=j.customer_id and j.question_map_id=97 " + 
		"where (d.product_cd='KNEE' or d.sec_product_cd='KNEE') " + 
		"union " + 
		"select first_nm, last_nm, address_txt, address_2_txt, city_nm, state_cd, zip_cd,  " + 
		"email_address_txt, phone_no_txt as 'PHONE_NO','','', birth_year_no, 'OV_KNEE' as 'PRI_PRODUCT', " + 
		"'' as 'SEC_PRODUCT', referrer_q as 'CALL_REASON_CD', call_source_cd, 'SELF' as 'CALL_TARGET_CD', " + 
		"knee_pain_q as 'QUAL_KNEE_01',frequency_q as 'QUAL_KNEE_02',impact_q as 'IMPACT_QUAL',  " + 
		"how_soon_q as 'Q_HOW_SOON', treatment_q as 'KNEE018', osteo_q as 'DxOsteo',  " + 
		"doctor_q as 'DOCTOR_TYPE', case when opt_in_flg = 'Additional-Information' then 0 else opt_in_flg end as 'OPT_IN', attempt_dt, " + 
		"'' as 'gender_cd', '' as 'prefix_nm' from pga_data.dbo.master";
		
		return sql;
	}

}
