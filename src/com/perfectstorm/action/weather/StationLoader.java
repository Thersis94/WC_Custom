package com.perfectstorm.action.weather;

// JDK 1.8.x
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
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
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
// SMT Base Libs
import com.siliconmtn.util.Convert;

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
		log.info("Starting");
		List<WeatherStationVO> stations = sl.loadFile();
		BasicConfigurator.configure();
		
		// Get the DB Connection
		DatabaseConnection dc = new DatabaseConnection();
		dc.setPassword("sqll0gin");
		dc.setUserName("ryan_user_sb");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_wsla_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setDriverClass("org.postgresql.Driver");
		sl.conn = dc.getConnection();
		int i=0;
		for (WeatherStationVO station : stations) {
			if ((i++ % 500) == 0) log.info("Processed: " + i  + " records");
			
			if (! station.getStationCode().startsWith("C"))
				sl.getExtendedData(station);
		}
		
		sl.saveStations(stations);
		
	}
	
	/**
	 * 
	 * @param station
	 * @throws IOException
	 */
	public void getExtendedData(WeatherStationVO station) throws IOException {
		SMTHttpConnectionManager cMgr = new SMTHttpConnectionManager();
		String url = JSON_LOC + station.getStationCode();
		byte[] data = cMgr.retrieveData(url);
		if (data == null) return;
		
		Gson g = new Gson();
		StationExtVO extStat = g.fromJson(new String(data), StationExtVO.class);
		if (extStat.getProperties() == null) return;
		
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
				ws.setStationCode(eElement.getElementsByTagName("station_id").item(0).getTextContent());
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

