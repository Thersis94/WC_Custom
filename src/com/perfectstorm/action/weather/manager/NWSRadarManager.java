package com.perfectstorm.action.weather.manager;

//JDK 1.8.x
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

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

public class NWSRadarManager {
	private static Logger log = Logger.getLogger(NWSRadarManager.class);
	
	/**
	 * 
	 */
	public NWSRadarManager()  {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		NWSRadarManager radar = new NWSRadarManager();
		List<Date> times = radar.retrieveData("http://radar.weather.gov/ridge/kml/animation/N1P/FTG_N1P_loop.kml");
		log.info(times);
	}
	
	/**
	 * Retrieves the times in UTC for each radar image.
	 * 
	 * @return
	 * @throws ActionException 
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws ParseException 
	 */
	public List<Date> retrieveData(String url) throws ActionException {
		List<Date> times = new ArrayList<>();
		String path = url + "?rand=" + System.currentTimeMillis();

		try {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			byte[] data = conn.retrieveData(path);
			
			// Get the xml data
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(data));
			doc.getDocumentElement().normalize();
			
			// Get the ground overlay data
			NodeList nodeList = doc.getElementsByTagName("GroundOverlay");
			
			// Loop the ground overlays to get each date/time
			for (int idx = 0; idx < nodeList.getLength(); idx++) {
				Node node = nodeList.item(idx);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element overlay = (Element) node;
					String startDate = overlay.getElementsByTagName("begin").item(0).getTextContent();
					
					// The times in the xml are in UTC, make sure the Date object represents it correctly
					DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
					times.add(utcFormat.parse(startDate));
					
					// This will output in the system time zone
					log.info(utcFormat.parse(startDate));
				}
			}
		} catch(Exception e) {
			throw new ActionException(e);
		}
		
		return times;
	}
}

