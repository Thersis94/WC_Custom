package com.perfectstorm.action.weather;

import java.io.BufferedReader;
import java.io.BufferedWriter;
// JDK 1.8.x
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// Gson 2.3
import com.google.gson.Gson;

// PS Libs
import com.perfectstorm.data.weather.StationExtVO;
import com.perfectstorm.data.weather.WeatherStationVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
// SMT Base Libs
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: StationLoader.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Creates the initial load of the List of weather stations
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 6, 2019
 * @updates:
 ****************************************************************************/

public class StationLoader {
	public static final String FILE_LOC = "/Users/james/Downloads/stations.xml";
	public static final String JSON_LOC = "https://api.weather.gov/stations/";
	private Connection conn;
	private static Logger log = Logger.getLogger(StationLoader.class);
	
	/**
	 * 
	 */
	public StationLoader()  {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		StationLoader sl = new StationLoader();
		BasicConfigurator.configure();
		log.info("Starting");
		List<WeatherStationVO> stations = sl.loadFile();
		
		// Get the DB Connection
		DatabaseConnection dc = new DatabaseConnection();
		dc.setPassword("sqll0gin");
		dc.setUserName("ryan_user_sb");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setDriverClass("org.postgresql.Driver");
		sl.conn = dc.getConnection();

		File f = new File("/Users/james/Desktop/tx-map.txt");
		FileOutputStream fos = new FileOutputStream(f);
		 
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos))) {
			for (WeatherStationVO station : stations) {
				if (! station.getWeatherStationCode().startsWith("C"))
					sl.getExtendedData(station);
				
				bw.write(station.getWeatherStationCode() + "\t" + StringUtil.checkVal(station.getTimezoneCode()));
				bw.newLine();
			}
		}
		
		sl.saveStations(stations);
		sl.updateTimezone(stations);
		sl.conn.setAutoCommit(true);
		sl.updateTimezoneFromFile();
		log.info("Complete");
	}
	
	
	public void updateTimezoneFromFile() throws IOException {
		log.info("Loading file");
		File f = new File("/Users/james/Desktop/tx-map.txt");
		List<GenericVO> data = new ArrayList<>(2048);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String temp = "";
            while((temp = br.readLine()) != null) {
            	String [] ele = temp.split("\\t");
            	data.add(new GenericVO(ele[0], ele[1]));
            }
        }
        log.info("Finished file: " + data.size());
		StringBuilder sql = new StringBuilder(96);
		sql.append("update custom.ps_weather_station set timezone_cd = ? ");
		sql.append("where weather_station_cd = ?");
		
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			for (GenericVO item : data) {
				log.info("processing: " + item);
				ps.setString(1, item.getValue() + "");
				ps.setString(2, item.getKey() + "");
				
				try {
					ps.executeUpdate();
				} catch (Exception e) {
					System.out.println("Error: " + item.getKey() + "|" + item.getValue());
					log.error("Failed: ", e);
				}
			}
			
			ps.executeBatch();
		} catch (Exception e) {
			log.error("unabel to store timezones", e);
		}
	}
	
	/**
	 * updates the time zone
	 * @param stations
	 * @throws SQLException
	 */
	public void updateTimezone(List<WeatherStationVO> stations) throws SQLException {
		
		StringBuilder sql = new StringBuilder(96);
		sql.append("update custom.ps_weather_station set timezone_cd = ? ");
		sql.append("where weather_station_cd = ?");
		
		try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			for (WeatherStationVO station : stations) {
				ps.setString(1, station.getTimezoneCode());
				ps.setString(2, station.getWeatherStationCode());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
	}
	
	/**
	 * 
	 * @param station
	 * @throws IOException
	 */
	public void getExtendedData(WeatherStationVO station) throws IOException {
		SMTHttpConnectionManager cMgr = new SMTHttpConnectionManager();
		String url = JSON_LOC + station.getWeatherStationCode();
		byte[] data = cMgr.retrieveData(url);
		if (data == null) return;
		
		Gson g = new Gson();
		StationExtVO extStat = g.fromJson(new String(data), StationExtVO.class);
		if (extStat.getProperties() == null) return;
		station.setTimezoneCode(extStat.getProperties().getTimeZone());
		station.setElevation(extStat.getProperties().getElevation().getElevationInFeet());

	}
	
	/**
	 * Loads all of the weather stations to the DB
	 * @param stations
	 * @throws DatabaseException
	 */
	public void saveStations(List<WeatherStationVO> stations) throws DatabaseException {
		if (conn == null) return;
		DBProcessor db = new DBProcessor(conn, "custom.");
		db.executeBatch(stations, true);
	}
	
	/**
	 * 
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public List<WeatherStationVO> loadFile() 
	throws SAXException, IOException, ParserConfigurationException {
		// Get the XML File
		File fXmlFile = new File(FILE_LOC);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("station");
		
		// Loop the XML document and create VO
		List<WeatherStationVO> stations = new ArrayList<>();
		for (int i=0; i < nList.getLength(); i++) {
			Node n = nList.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				WeatherStationVO ws = new WeatherStationVO();
				Element eElement = (Element) n;
				ws.setWeatherStationCode(eElement.getElementsByTagName("station_id").item(0).getTextContent());
				ws.setState(eElement.getElementsByTagName("state").item(0).getTextContent());
				ws.setLatitude(Convert.formatDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()));
				ws.setLongitude(Convert.formatDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
				ws.setStationName(eElement.getElementsByTagName("station_name").item(0).getTextContent());
				ws.setCountry("US");
				ws.setActiveFlag(1);
				
				stations.add(ws);
			}
		}
		
		return stations;
	}

}

