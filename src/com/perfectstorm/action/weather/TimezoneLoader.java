package com.perfectstorm.action.weather;

// JDK 1.8.x
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

// Log4j 1.2.17
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// PS Libs
import com.perfectstorm.data.weather.TimezoneVO;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: TimezoneLoader.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> loads the timezones file
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class TimezoneLoader {

	public static final String FILE_LOC = "/Users/james/Desktop/timezone.txt";
	private Connection conn;
	private static Logger log = Logger.getLogger(StationLoader.class);
	
	/**
	 * 
	 */
	public TimezoneLoader() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		TimezoneLoader tl = new TimezoneLoader();
		BasicConfigurator.configure();
		log.info("Starting ...");
		
		// Get the DB Connection
		DatabaseConnection dc = new DatabaseConnection();
		dc.setPassword("sqll0gin");
		dc.setUserName("ryan_user_sb");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setDriverClass("org.postgresql.Driver");
		tl.conn = dc.getConnection();

		// Parse the text file
		List<TimezoneVO> zones = tl.parseZones();
		tl.saveTimezone(zones, tl.conn);
		
		tl.conn.close();
		log.info("Completed Timezone loading");
	}

	/**
	 * Saves the timezones
	 * @param zones
	 * @param conn
	 * @throws DatabaseException
	 */
	public void saveTimezone(List<TimezoneVO> zones, Connection conn) throws DatabaseException {
		DBProcessor db = new DBProcessor(conn, "custom.");
		db.executeBatch(zones, true);
	}
	
	/**
	 * Parses the time zone file
	 * @return
	 * @throws IOException
	 */
	public List<TimezoneVO> parseZones() throws IOException {
		List<TimezoneVO> zones = new ArrayList<>(512);

		File f = new File(FILE_LOC);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String temp = "";
            while((temp = br.readLine()) != null) {
            	String[] zd = temp.split("\\t");
            	TimezoneVO tvo = new TimezoneVO();
            	tvo.setCountryCode(zd[0]);
            	tvo.setTimezoneCode(zd[1]);
            	tvo.setName(zd[1]);
            	tvo.setOffset(Convert.formatDouble(zd[2].replace(':', '.')));
            	tvo.setDstOffset(Convert.formatDouble(zd[3].replace(':', '.')));
            	zones.add(tvo);
             }
        }
        
		return zones;
	}
}

