package com.mts.util;

import java.sql.Connection;

// Log4j Imports
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

/****************************************************************************
 * <b>Title</b>: DataMigrationUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Importer of MTS Core data 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 1, 2019
 * @updates:
 ****************************************************************************/

public class DataMigrationUtil {

	private static final Logger log = Logger.getLogger(DataMigrationUtil.class);
	
	/**
	 * 
	 */
	public DataMigrationUtil() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		log.info("Starting Migration");
		
		DataMigrationUtil dmu = new DataMigrationUtil();
		Connection srcConn = dmu.getSourceConnection();
		Connection destConn = dmu.getDestConnection();
		
		log.info("Source Conn: " + ! srcConn.isClosed());
		log.info("Dest Conn: " + ! destConn.isClosed());
		
		srcConn.close();
		destConn.close();
		log.info("Migration Completed");
	}
	
	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getSourceConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("com.mysql.cj.jdbc.Driver");
		dc.setUrl("jdbc:mysql://sonic:3306/medtechinno");
		dc.setUserName("smtdev");
		dc.setPassword("smtrul3s");

		return dc.getConnection();
	}

	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getDestConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/SMT_GEOCODER?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("smt_geo");
		dc.setPassword("sqll0gin");

		return dc.getConnection();
	}
}

