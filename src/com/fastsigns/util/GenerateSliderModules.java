package com.fastsigns.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.db.util.DatabaseException;

public class GenerateSliderModules {
	protected static String DESTINATION_DB_URL = "jdbc:sqlserver://localhost:1433";
	protected static String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	protected static String[] DESTINATION_AUTH = new String[] {
			"webcrescendo_fs_user", "sqll0gin" };
	protected static final Logger log = Logger.getLogger(GenerateSliderModules.class);
	final String customDb = "WebCrescendo_fs_custom.dbo.";
	List<Integer> options = new ArrayList<Integer>();

	//US
//	final int moduleId = 18;
//	final String countryCd = "US";
	//UK
	final int moduleId = 36;
	final String countryCd = "GB";

	protected Connection dbConn = null;

	public GenerateSliderModules() {
		PropertyConfigurator.configure("C:/Software/log4j.properties");

	}

	public static void main(String[] args) {

		GenerateSliderModules cgo = new GenerateSliderModules();
		try {
			cgo.populateOptions();
			cgo.execute();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
	
	public void populateOptions(){
		if(countryCd.equals("US")){
		options.add(new Integer(27914));
		options.add(new Integer(27915));
		options.add(new Integer(27916));
		options.add(new Integer(27917));
		} else {
		options.add(new Integer(27918));
		options.add(new Integer(27919));
		options.add(new Integer(27920));
		options.add(new Integer(27921));
		}
	}
	
	public void execute() throws IOException {
		FileWriter f = new FileWriter(new File("C:\\Users\\smt_user\\Desktop\\SliderInserts.sql"));
		ArrayList<Integer> franchises = new ArrayList<Integer>();
		
		
		try {
			dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1],
					DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
			int modSeed = nextModuleOptionPkId();
			String sql = getFranchises();
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql);
				ps.setString(1, countryCd);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					franchises.add(new Integer(rs.getInt("FRANCHISE_ID")));
				}
			} catch (SQLException e) {
				log.debug(e);
			} finally {
				try {
					dbConn.close();
				} catch (Exception e) {
				}
			}
			System.out.println("ModSeed: " + modSeed);
			System.out.println("retrieved " + franchises.size() + " franchises");
			int count= 0;
			for(int i = 0; i<franchises.size(); i++){
				//System.out.println(generateLocationModuleXRQuery(franchises.get(i)));
				f.write(generateLocationModuleXRQuery(franchises.get(i)));
				f.write("\n");
				count++;
				for(int j = 0; j < options.size(); j++){
					//System.out.println(generateModuleXRQuery((modSeed + i), options.get(j), j));
					f.write(generateModuleXRQuery((modSeed + i), options.get(j), j));
					f.write("\n");
					count++;
				}
			}
			System.out.println("Wrote " + count +" scripts");
			f.close();
		} catch (DatabaseException e) {
			log.debug(e);
		}
	}

	private String getFranchises() {
		StringBuilder sb = new StringBuilder();
		sb.append("select FRANCHISE_ID from ")
				.append(customDb)
				.append("FTS_FRANCHISE where COUNTRY_CD=?");
		return sb.toString();
	}
	
	private String generateLocationModuleXRQuery(int franchiseId){
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR ");
		sb.append("(CP_LOCATION_ID, FRANCHISE_ID, CREATE_DT, CP_MODULE_ID) ");
		sb.append("values (9, ").append(franchiseId).append(", getDate(), ");
		sb.append(moduleId).append(")");
		return sb.toString();
	}
	
	private String generateModuleXRQuery(int locationModXRId, int moduleOptId, int order){
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR ");
		sb.append("(CP_LOCATION_MODULE_XR_ID, CP_MODULE_OPTION_ID, CREATE_DT, ORDER_NO) ");
		sb.append("values (").append(locationModXRId).append(", ").append(moduleOptId);
		sb.append(", getDate(), ").append(order).append(")");
		return sb.toString();
	}

	/**
	 * this is in place because this table does not support an Identity seed
	 * counter.
	 * 
	 * @return
	 */
	private int nextModuleOptionPkId() {
		int pkId = 0;
		StringBuilder sb = new StringBuilder();
		sb.append("select max(CP_LOCATION_MODULE_XR_ID) from ")
				.append(customDb)
				.append("FTS_CP_LOCATION_MODULE_XR");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				pkId = rs.getInt(1) + 1;
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		return pkId;
	}

	/**
	 * 
	 * @param userName
	 *            Login Account
	 * @param pwd
	 *            Login password info
	 * @param driver
	 *            Class to load
	 * @param url
	 *            JDBC URL to call
	 * @return Database Conneciton object
	 * @throws DatabaseException
	 */
	protected Connection getDBConnection(String userName, String pwd,
			String driver, String url) throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver",
					cnfe);
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

	protected void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (Exception e) {
		}
	}
}