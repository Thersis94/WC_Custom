package com.depuy.sitebuilder.locator;

// JDK 1.6
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

//DOM4j
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

// SMTBaseLibs
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.XMLUtil;

/*****************************************************************************
 <p><b>Title</b>: LocatorQueryUtil.java</p>
 <p></p>
 <p>Copyright: Copyright (c) 2013 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Apr 18, 2013
 Changes: Apr 18, 2013 - Created initial class file
 ***************************************************************************/

public class LocatorQueryUtil {
    private Logger log = Logger.getLogger("LocatorQueryUtil");
	private int specialty = -1;
	private String productId = null;
	private String siteLocation = null;
	private String address = null;
	private String city = null;
	private String state = null;
	private String zipCode = null;
	private String country = "US";
	private String language = "en";
	private int radius = 50;
	private String order = "last";
	private String displayTemplate = "/xml_display_ids.jsp";
	private int resultCount = -1;
	private String surgeonId = null;
	private String locationId = null;
	private String clinicId = null;
	private String resultId = null;
	// uniqueId is a composite value built using surgeonId, clinicId, and locationId
	// e.g. 12345-6789-0123
	private String uniqueId = null;
		
    public LocatorQueryUtil() { }
    
    public static void main(String[] args) {
    	LocatorQueryUtil lq = new LocatorQueryUtil();
    	lq.setSpecialty(6);
    	lq.setSiteLocation("aamd");
    	lq.setZipCode("46580");
    	//lq.setResultCount(50);
    	//lq.setResultId("5");
    	lq.setUniqueId("12926-12325-17991");
    	Map<String,String> surgeon = null;
    	try {
    		surgeon = lq.locateSurgeonByUniqueId("12926-12325-17991");
    	} catch(Exception e) {
    		System.out.println("error: " + e);
    	}
    	
    	if (surgeon != null) {
    		for (String s : surgeon.keySet()) {
    			System.out.println("key/val: " + s + "/" + surgeon.get(s));
    		}
    	} else {
    		System.out.println("No surgeon found for uniqueId: " + lq.getUniqueId());
    	}
    }
    
    /**
     * Queries the Locator and returns the results.
     * @return
     * @throws IOException
     */
    public StringBuffer locateSurgeons() throws IOException {
    	return this.connect(this.buildLocatorQueryUrl());
    }
    
    /**
     * Queries the Locator and returns the data for a surgeon based on the
     * surgeon's unique(i.e. 'surgeonId-clinicId-locationId' value.
     * @param uniqueId
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    public Map<String, String> locateSurgeonByUniqueId(String uniqueId) 
    		throws IOException, DocumentException {
    	this.uniqueId = uniqueId;
    	StringBuffer results = this.locateSurgeons();
    	return this.findSurgeon(results);
    }
        
    /**
     * Parses locator XML 'result' elements to find a surgeon based on
     * the surgeon's uniqueId value.
     * @param xml
     * @param surgeonId
     * @return
     * @throws DocumentException
     */
	@SuppressWarnings("rawtypes")
	private Map<String, String> findSurgeon(StringBuffer xml) 
			throws DocumentException {
		// Parse out the XML Data and create the root element
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(xml.toString().getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
	
		List results = doc.selectNodes("//*[name()='result']");
		Map<String, String> rowData = null;
		boolean found = false;
		Element result = null;
		// loop the 'result' elements
		for (int i = 0; i < results.size(); i ++) {
			if (! found) rowData = new HashMap<String, String>();
			result = (Element)results.get(i);
			Iterator iter = result.elementIterator();
			// loop children of 'result' element we are currently parsing
			while (iter.hasNext()) {
				Element ne = (Element) iter.next();
				// store values on map in case this is the surgeon we are looking for
				rowData.put(ne.getName(), ne.getStringValue());
				if (ne.getName().equalsIgnoreCase("uniqueId")) {
					if (XMLUtil.checkVal(ne).equalsIgnoreCase(uniqueId)) {
						found = true;
					}
				}
			}
			if (found) break;
		}
		return rowData;
		}

	/**
	 * Builds the url for the locator call
	 * @return URL Formatted for AAMD Locator Request
	 */
	private String buildLocatorQueryUrl() {
		StringBuffer s = new StringBuffer();
		s.append("http://www.allaboutmydoc.com/AAMD/locator?");
		s.append("display_template=/xml_display_ids.jsp&company=1");
		s.append("&site_location=").append(siteLocation);
		s.append("&accept=true");
		s.append("&country=").append(country == null ? "US" : country);
		s.append("&language=").append(language == null ? "en" : language);
		if (address != null) s.append("&address=").append(encode(address));
		if (city != null) s.append("&city=").append(encode(city));
		if (state != null) s.append("&state=").append(encode(state));
		if (zipCode != null) s.append("&zip=").append(encode(zipCode));
		if (productId != null) s.append("&product=").append(productId);
		if (specialty > -1) s.append("&specialty=").append(specialty);
		s.append("&radius=").append(radius);
		s.append("&order=").append(order);
		if (resultCount > 0) {
			s.append("&resultCount=").append(resultCount);;
		}
		log.debug("Locator query url: " + s);
		return s.toString();
	}
	
	/**
	 * Helper method
	 * @param s
	 * @return
	 */
	private String encode(String s) {
		try {
			s = java.net.URLEncoder.encode(StringUtil.checkVal(s), "UTF-8");
		} catch (Exception e) {}
		return s;
	}
    
	/**
	 * Connects to DePuy Locator using the supplied url and gets the Locator XML Data
	 * @param actionUrl
	 * @return
	 * @throws IOException
	 */
	private StringBuffer connect(String actionUrl) throws IOException {
        StringBuffer sb = new StringBuffer();
        HttpURLConnection conn = null;
        URL url = null;
        try {
            url = new URL(actionUrl);
            conn = (HttpURLConnection) url.openConnection();
            
            //Parse header information.  Ensure ode 200 is returned
            for (int c=0; conn.getHeaderField(c) != null; c++) {
            	if (conn.getHeaderFieldKey(c) == null) {
            		String field = conn.getHeaderField(c);
            		if (field == null || field.indexOf("200") == -1) {
            			throw new IOException("Header returned error: " + field + ", not 200");
            		}
            	}
            }
            
            // Retrieve the data coming back from MapQuest and Store it in a StringBuffer
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String temp = "";
            while ((temp = in.readLine()) != null) {
            	sb.append(temp);
            }
            
            // Close the connection
            conn.disconnect();
        } catch(Exception e) {
        	//log.error("Error retreiving surgeons from AAMD", e);
        }
        
		return sb;
	}

	/**
	 * @return the specialty
	 */
	public int getSpecialty() {
		return specialty;
	}

	/**
	 * @param specialty the specialty to set
	 */
	public void setSpecialty(int specialty) {
		this.specialty = specialty;
	}

	/**
	 * @return the productId
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @return the siteLocation
	 */
	public String getSiteLocation() {
		return siteLocation;
	}

	/**
	 * @param siteLocation the siteLocation to set
	 */
	public void setSiteLocation(String siteLocation) {
		this.siteLocation = siteLocation;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the zipCode
	 */
	public String getZipCode() {
		return zipCode;
	}

	/**
	 * @param zipCode the zipCode to set
	 */
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @return the radius
	 */
	public int getRadius() {
		return radius;
	}

	/**
	 * @param radius the radius to set
	 */
	public void setRadius(int radius) {
		this.radius = radius;
	}

	/**
	 * @return the order
	 */
	public String getOrder() {
		return order;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(String order) {
		this.order = order;
	}

	/**
	 * @return the displayTemplate
	 */
	public String getDisplayTemplate() {
		return displayTemplate;
	}

	/**
	 * @param displayTemplate the displayTemplate to set
	 */
	public void setDisplayTemplate(String displayTemplate) {
		this.displayTemplate = displayTemplate;
	}

	/**
	 * @return the resultCount
	 */
	public int getResultCount() {
		return resultCount;
	}

	/**
	 * @param resultCount the resultCount to set
	 */
	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the resultId
	 */
	public String getResultId() {
		return resultId;
	}

	/**
	 * @param resultId the resultId to set
	 */
	public void setResultId(String resultId) {
		this.resultId = resultId;
	}

	/**
	 * @return the locationId
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @return the clinicId
	 */
	public String getClinicId() {
		return clinicId;
	}

	/**
	 * @param clinicId the clinicId to set
	 */
	public void setClinicId(String clinicId) {
		this.clinicId = clinicId;
	}

	/**
	 * @return the uniqueId
	 */
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * @param uniqueId the uniqueId to set
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	
}
