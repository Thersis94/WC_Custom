package com.rezdox.api;

// JDK 1.8.x
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 1.8 XML Parsers
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
// DOM
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// WC Custom
import com.rezdox.vo.ZillowPropertyVO;

// SMT Base Libs 3.5
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.Location;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: APIManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the various api calls for the rezdox system
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/
public class ZillowAPIManager {

	private static final Logger log = Logger.getLogger(ZillowAPIManager.class);

	// Member Variables
	private String zwsId;
	private String baseURL;

	// API Restful calls
	private static final String RETRIEVE_ID_URI = "/GetSearchResults.htm";
	private static final String RETRIEVE_ZESTIMATE_URI = "/GetZestimate.htm";
	private static final String RETRIEVE_DETAILS_URI = "/GetDeepSearchResults.htm";
	private static final String RETRIEVE_UPDATRED_DETAILS_URI = "/GetUpdatedPropertyDetails.htm";

	// API Information
	public static final String API_KEY = "X1-ZWz1ezb2wimxhn_5upah";
	public static final String BASE_URL = "http://www.zillow.com/webservice";

	/**
	 * Defines the API key and base URL for the connection
	 * @param zwsId API Key
	 * @param baseURL Base URL
	 */
	public ZillowAPIManager(String zwsId, String baseURL) {
		super();
		this.zwsId = zwsId;
		this.baseURL = baseURL;
	}

	/**
	 * Uses default API Key and Base Url
	 */
	public ZillowAPIManager() {
		super();
		this.zwsId = API_KEY;
		this.baseURL = BASE_URL;
	}

	/**
	 * 
	 * @param loc
	 * @return
	 * @throws InvalidDataException
	 */
	public ZillowPropertyVO retrieveZillowId(Location loc) throws InvalidDataException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		StringBuilder csz = new StringBuilder(64);
		csz.append(loc.getCity()).append(",").append(loc.getState()).append(" ").append(loc.getZipCode());
		byte[] data = null;

		try {
			StringBuilder path = getBaseURL(RETRIEVE_ID_URI);
			path.append("&address=").append(StringEncoder.urlEncode(loc.getAddress()));
			path.append("&citystatezip=").append(StringEncoder.urlEncode(csz.toString()));
			data = conn.retrieveData(path.toString());
		} catch(Exception e) {
			throw new InvalidDataException("Unable to retrieve zillow id", e);
		}

		// Parse and return the data
		return parseZillowResponse(data, false);
	}

	/**
	 * Retrieves the zestimate by zillow id
	 * @param zpid
	 * @return
	 * @throws InvalidDataException
	 */
	public ZillowPropertyVO retrieveZestimate(String zpid) throws InvalidDataException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = null;

		try {
			StringBuilder path = getBaseURL(RETRIEVE_ZESTIMATE_URI);
			path.append("&zpid=").append(zpid);
			data = conn.retrieveData(path.toString());
		} catch(Exception e) {
			throw new InvalidDataException("Unable to retrieve zestimate", e);
		}

		// Parse and return the data
		return parseZillowResponse(data, false);
	}

	/**
	 * 
	 * @param zpid
	 * @return
	 * @throws InvalidDataException
	 */
	public ZillowPropertyVO retrievePropertyDetails(Location loc) throws InvalidDataException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		String csz = StringUtil.join(loc.getCity(), ",", loc.getState(), " ", loc.getZipCode());
		byte[] data = null;

		try {
			StringBuilder path = getBaseURL(RETRIEVE_DETAILS_URI);
			path.append("&address=").append(StringEncoder.urlEncode(loc.getAddress()));
			if (!StringUtil.isEmpty(loc.getAddress2()))
				path.append(",+").append(StringEncoder.urlEncode(loc.getAddress2()));
			path.append("&citystatezip=").append(StringEncoder.urlEncode(csz));
			data = conn.retrieveData(path.toString());
		} catch(Exception e) {
			throw new InvalidDataException("Unable to retrieve property info", e);
		}

		ZillowPropertyVO vo = parseZillowResponse(data, true);
		if (!StringUtil.isEmpty(vo.getZillowId()))
			retrieveUpdatedDetails(vo);

		return vo;
	}


	/**
	 * They call it updated, but really it's enhanced.  This response gets us school data.
	 * Note: school data is optional and not always returned (varies by address).
	 * @param zpid
	 * @return
	 * @throws InvalidDataException
	 */
	private void retrieveUpdatedDetails(ZillowPropertyVO vo) throws InvalidDataException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = null;

		try {
			StringBuilder path = getBaseURL(RETRIEVE_UPDATRED_DETAILS_URI);
			path.append("&zpid=").append(StringEncoder.urlEncode(vo.getZillowId()));
			data = conn.retrieveData(path.toString());
		} catch(Exception e) {
			throw new InvalidDataException("Unable to retrieve detail info", e);
		}

		parseUpdatedZillowResponse(data, vo);
	}


	/**
	 * Perform a details call to Zillow to load school data and manual listing
	 * corrections performed at Zillow by homeowners.
	 * @param data
	 * @param vo
	 * @return
	 * @throws InvalidDataException 
	 */
	private void parseUpdatedZillowResponse(byte[] data, ZillowPropertyVO vo) 
			throws InvalidDataException {
		Element docEle = createXMLDocument(data);
		if (docEle.getElementsByTagName("zpid").getLength() == 0)
			throw new InvalidDataException("No Zillow data retrieved for the given address.");

		//define an iterable set of fields we want to take from Zillow and infuse into the user's form submission
		Map<String, String> keyPairs = new HashMap<>(); //key=ZillowXML, value=SMT Slug
		keyPairs.put("neighborhood", "RESIDENCE_NEIGHBORHOOD");
		keyPairs.put("schoolDistrict", "RESIDENCE_SCHOOL_DISTRICT");
		keyPairs.put("elementarySchool", "RESIDENCE_SCHOOL_ELEM");
		keyPairs.put("middleSchool", "RESIDENCE_SCHOOL_MID");
		keyPairs.put("highSchool", "RESIDENCE_SCHOOL_HIGH");
		keyPairs.put("useCode", "RESIDENCE_PROP_TYP");

		for (Map.Entry<String, String> entry : keyPairs.entrySet()) {
			String val = getStringFromNode(docEle, entry.getKey());
			if (!StringUtil.isEmpty(val)) {
				vo.addExtendedData(entry.getValue(), val);
				log.debug(String.format("saved school data %s=%s", entry.getValue(), val));
			}
		}
	}


	/**
	 * Get data from the XML document using nodeName.
	 * Bury any exceptions and presumes there's one node (or returns data from the first)
	 * @param docEle
	 * @param nodeName
	 * @return
	 */
	private String getStringFromNode(Element docEle, String nodeName) {
		try {
			return StringUtil.checkVal(docEle.getElementsByTagName(nodeName).item(0).getTextContent());
		} catch(Exception e) {
			return "";
		}
	}


	/**
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	protected ZillowPropertyVO parseZillowResponse(byte[] data, boolean extended) throws InvalidDataException {
		Element docEle = createXMLDocument(data);
		if (docEle.getElementsByTagName("zpid").getLength() == 0)
			throw new InvalidDataException("No Zillow data retrieved for the given address.");

		String zpid = docEle.getElementsByTagName("zpid").item(0).getTextContent();
		double latitude = Convert.formatDouble(docEle.getElementsByTagName("latitude").item(0).getTextContent());
		double longitude = Convert.formatDouble(docEle.getElementsByTagName("longitude").item(0).getTextContent());

		// Loop the zestimate node and  
		NodeList zestimate = docEle.getElementsByTagName("zestimate").item(0).getChildNodes();
		Map<String, String> retData = new HashMap<>();
		for (int i=0; i < zestimate.getLength(); i++) {
			Node n = zestimate.item(i);
			if (n.getChildNodes().getLength() > 0) {
				if (! n.getFirstChild().getTextContent().equals(n.getLastChild().getTextContent())) {
					retData.put(n.getLastChild().getNodeName(), n.getLastChild().getTextContent());
					retData.put(n.getFirstChild().getNodeName(), n.getFirstChild().getTextContent());
				} else {
					retData.put(n.getNodeName(), n.getTextContent());
				}
			}
		}

		// Process the extended data
		mapExtendedData(retData, docEle, extended);

		//set home dimensions to sqft if not set by the user.  This is how Zillow returns the Lot Size, we may as well pre-fill it.
		if (StringUtil.isEmpty(retData.get("RESIDENCE_LOT_TYP")))
			retData.put("RESIDENCE_LOT_TYP", "Feet");

		// Create the estimate object
		ZillowPropertyVO vo = new ZillowPropertyVO(retData);
		vo.setZillowId(zpid);
		vo.setLatitude(latitude);
		vo.setLongitude(longitude);
		return vo;
	}


	/**
	 * Adds the extended data fields to the map
	 * @param retData
	 * @param docEle
	 * @param extended
	 */
	private void mapExtendedData(Map<String, String> retData, Element docEle, boolean extended) {
		if (! extended) return;

		List<String> keys = getExtendedDataKeys();
		for (String key : keys) {
			try {
				retData.put(key, docEle.getElementsByTagName(key).item(0).getTextContent());
			} catch(Exception e) { /* Do Nothing */ }
		}

		addLinksExtendedData(retData, docEle);
	}


	/**
	 * Adds the links to the extended ddata
	 * @param retData
	 * @param docEle
	 */
	private void addLinksExtendedData(Map<String, String> retData, Element docEle) {
		NodeList links = docEle.getElementsByTagName("links").item(0).getChildNodes();

		for (int i=0; i < links.getLength(); i++) {
			Node n = links.item(i);
			retData.put(n.getNodeName(), n.getTextContent());
		}
	}


	/**
	 * List of extended data fields
	 * @return
	 */
	private List<String> getExtendedDataKeys() {
		List<String> extendedDataMap = new ArrayList<>(15);
		extendedDataMap.add("FIPScounty");
		extendedDataMap.add("useCode");
		extendedDataMap.add("taxAssessmentYear");
		extendedDataMap.add("taxAssessment");
		extendedDataMap.add("yearBuilt");
		extendedDataMap.add("lotSizeSqFt");
		extendedDataMap.add("finishedSqFt");
		extendedDataMap.add("bathrooms");
		extendedDataMap.add("bedrooms");
		extendedDataMap.add("totalRooms");
		extendedDataMap.add("lastSoldDate");
		extendedDataMap.add("lastSoldPrice");
		return extendedDataMap;
	}


	/**
	 * Gets the base url with zws id
	 * @return
	 */
	private StringBuilder getBaseURL(String type) {
		StringBuilder path = new StringBuilder(128);
		path.append(baseURL).append(type).append("?");
		path.append("zws-id=").append(zwsId);
		return path;
	}


	/**
	 * Creates the XML DOM for the given document
	 * @param data XML Document
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private Element createXMLDocument(byte[] data) throws InvalidDataException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(data));
			return doc.getDocumentElement();
		} catch (Exception e) {
			throw new InvalidDataException("Unable to parse dom object", e);
		}
	}
}