package com.perfectstorm.action.weather.manager;

//JDK 1.8.x
import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: NWSRadarManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Retrieves the radar meta data from the NWS
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 21, 2019
 * @updates:
 ****************************************************************************/

public class NWSRadarManager {
	private static Logger log = Logger.getLogger(NWSRadarManager.class);
	private static final String RADAR_METADATA_URL = "https://radar.weather.gov/ridge/kml/animation/%s/%s_%s_loop.kml";
	
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
		List<Date> times = radar.retrieveData("FTG", "N1P");
		log.info(times);
	}
	
	/**
	 * Retrieves the times in UTC for each radar image.
	 * 
	 * @param radarCode
	 * @param radarTypeCode
	 * @return
	 * @throws ActionException
	 */
	public List<Date> retrieveData(String radarCode, String radarTypeCode) throws ActionException {
		String url = String.format(RADAR_METADATA_URL, radarTypeCode, radarCode, radarTypeCode);
		
		// Ensures we are getting the latest data
		String path = url + "?rand=" + System.currentTimeMillis();
		log.debug("retrieving radar meta data from: " + path);

		// Retrieve the data
		List<Date> times = new ArrayList<>();
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
				}
			}
		} catch(Exception e) {
			throw new ActionException(e);
		}
		
		return times;
	}
}

