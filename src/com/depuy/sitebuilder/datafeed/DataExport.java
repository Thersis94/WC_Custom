package com.depuy.sitebuilder.datafeed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.security.StringEncrypter;

/****************************************************************************
 * <b>Title</b>: DataExport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since May 7, 2007
 ****************************************************************************/
public class DataExport {
	private Connection conn = null;
	private FileOutputStream fos = null;
	
	/**
	 * 
	 */
	public DataExport() {
		// Connect to the database
		String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		//String dbUrl = "jdbc:sqlserver://localhost:4959";
		String dbUrl = "jdbc:sqlserver://simon:2007";
		String dbUser = "sb_user";
		String dbPassword = "sqll0gin";

		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		
		try {
			conn = dbc.getConnection();
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	public static void main(String[] args) {
		DataExport de = new DataExport();
		try {
			de.openFile("e:/export.txt");
			de.getData();
			de.closeFile();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			de.conn.close();
		} catch(Exception e) {}
	}
	
	
	private void getData() {
		StringBuffer sb = new StringBuffer();
		sb.append("select email_address_txt, first_nm, last_nm ");
		sb.append("from profile a inner join data_feed.dbo.customer b ");
		sb.append("on a.profile_id = b.profile_id ");
		sb.append("where b.attempt_dt > getDate()-30 ");
		//sb.append("inner join org_profile_comm c on a.profile_id = c.profile_id ");
		//sb.append("where product_cd = 'KNEE' and email_address_txt is not null ");
		//sb.append("and call_target_cd = 'OTHER'  and allow_comm_flg = 1 ");
		sb.append("group by email_address_txt, first_nm, last_nm");
		//int count = 0;
		try {
			// Setup the decrypter
			StringEncrypter se = new StringEncrypter("s1l1c0nmtnT3chm0l0g13$JC");
			
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(sb.toString());
			while (rs.next()) {
				write(se.decrypt(rs.getString(1)) + "\t" +  se.decrypt(rs.getString(2)) + "\t" + se.decrypt(rs.getString(3)) + "\r\n");
				//count++;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void openFile(String path)  throws IOException {
		File f = new File(path);
		fos = new FileOutputStream(f);
	}
	
	private void write(String line) throws IOException {
		fos.write(line.getBytes());
	}
	
	/**
	 * Closes the file handler and stream
	 * @throws IOException
	 */
	private void closeFile() throws IOException {
		fos.flush();
		fos.close();
	}
}
