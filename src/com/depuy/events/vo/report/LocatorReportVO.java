package com.depuy.events.vo.report;


// JDK 1.5.0
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

// DOM4J libs
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
//import org.dom4j.Node;
import org.dom4j.io.SAXReader;

// Log4j 1.2.8
import org.apache.log4j.Logger;

import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;

/****************************************************************************
 * <b>Title</b>: LocatorReportVO.java<p/>
 * <b>Description: Makes a call to the AAMD surgeon locator web service and
 * downloads the appropriate information in XML format.  The data is formatted and returned</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2006<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 3, 2006
 ****************************************************************************/
public class LocatorReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 6l;
	private static Logger log = null;
	public static final int DEFAULT_RADIUS = 50;  //miles
	private EventEntryVO event = null;
	private String productId = "";
	private Integer radius = DEFAULT_RADIUS;
	
	/**
	 * 
	 */
	public LocatorReportVO() {
		super();
        log = Logger.getLogger(getClass());
		setContentType("application/msword");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Locator-Results.doc");
	}
	
	public void setData(Object o) {
		EventEntryVO event = (EventEntryVO) o;
		this.event = event;
		this.productId = (String) event.getAttribute("groupCd");
		this.radius = Convert.formatInteger((String)event.getAttribute("radius"), DEFAULT_RADIUS);
	}

	
	/**
	 * Generates the Locator report
	 * @param address Event Address
	 * @param city Event City
	 * @param state Event State
	 * @param zip Event Zip Code
	 * @param product Product Ccode
	 * @return
	 */
	public byte[] generateReport() {
		
		// Retrieve the XML Data
		StringBuffer doc = getHeader();
		try {
			doc.append(formatDisplay(connect(buildUrl())));
			
		} catch (Exception ioe) {
			log.error("Unable to retrieve locator results", ioe);
		}
		
		doc.append("</body></html>");
		log.info(doc);
		return doc.toString().getBytes();
	}
	
	
	private StringBuffer getHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("<html><head><title>Surgeon Locator Report</title></head><body>");
		sb.append("<u>Today's Seminar:</u><br/>"); 
		sb.append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append("<br/>");
		sb.append(event.getLocationDesc()).append("<br/>");
		sb.append("Speaker: ").append(event.getEventDesc()).append("<br/><br/>");
		sb.append(event.getEventName()).append(", ").append(event.getCityName()).append(", ").append(event.getStateCode());

		sb.append("<h1>LOCAL ORTHOPAEDIC SURGEONS</h1>\n");
		
		// Load the header into
		sb.append("<p>We are pleased to supply you with a list of orthopaedic surgeons in your area who use DePuy products.  ");
		sb.append("While our database of orthopaedic surgeons is large, it is not a complete ");
		sb.append("listing of all orthopaedic surgeons in your area.  A surgeon's use of ");
		sb.append("DePuy products is the sole criterion for being listed below. No orthopaedic ");
		sb.append("surgeon has paid a fee to participate.  DePuy Orthopaedics Inc., ");
		sb.append("does not make any recommendation or referral regarding any of these specific surgeons.</p>");
		sb.append("<p>For general information about DePuy Orthopaedics, visit www.depuy.com</p>");
		
		return sb;
	}
	
	@SuppressWarnings("rawtypes")
	private StringBuffer formatDisplay(StringBuffer xml) throws DocumentException {
		// Parse out the XML Data and create the root element
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(xml.toString().getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		//Element e = doc.getRootElement();
		
		// Create an html formatted table structure
		StringBuffer out = new StringBuffer();
		out.append("<table border=\"0\"><tr><td colspan='3'>");
		out.append("(Orthopaedic Surgeons are listed in alphabetical order)</td></tr>");
		
		//log.info("Message: " + doc.selectSingleNode("//*[name()='message']").getStringValue());
		//log.info("Specialty: " + doc.selectSingleNode("//*[name()='specialty']").getStringValue());
		
		List result = doc.selectNodes("//*[name()='result']");
		Map<String, String> rowData = new HashMap<String, String>();
		//log.info("Number Results: " + result.size());
		PhoneNumberFormat pnf = null;
		StringEncoder se = new StringEncoder();
		
		for(int i=0; i < result.size(); i++) {
			Element ele = (Element) result.get(i);
			Iterator iter = ele.elementIterator();
			
			while (iter.hasNext()) {
				Element ne = (Element) iter.next();
				rowData.put(ne.getName(), ne.getStringValue());
			}
			
			// Setup the columns
			if ((i % 2) == 0) {
				out.append("<tr><td valign='top'>");
			} else { 
				out.append("<td width='10px'>&nbsp;</td><td valign='top'>");
			}
			String nameStr = se.encodeValue(rowData.get("firstName") + " " + rowData.get("lastName"));
			if (event.getEventDesc().contains(nameStr)) {
				out.append("<i>Today's Speaker</i><br/>");
			}
			
			// Format the phone number
			pnf = new PhoneNumberFormat(rowData.get("phone"), PhoneNumberFormat.DASH_FORMATTING);
			out.append("<b>");
			out.append(rowData.get("firstName")).append(" ");
			out.append(rowData.get("lastName")).append(" ");
			out.append(rowData.get("suffix")).append(" ");
			out.append(rowData.get("degreeDesc")).append("</b><br/>");
			out.append("<b>").append(rowData.get("clinicName")).append("</b><br/>");
						out.append(rowData.get("address1"));
			
			if (rowData.get("address2").length() > 0) {
				out.append(", ").append(rowData.get("address2"));
			}
			out.append("<br/>");
			
			out.append(rowData.get("city")).append(", ");
			out.append(rowData.get("state")).append(" ").append(rowData.get("zip"));
			out.append("<br/>");
			
			out.append("<b>Phone: ").append(pnf.getFormattedNumber()).append("</b><br/>");
			if (rowData.get("siteURL").length() > 0 && !rowData.get("siteURL").endsWith("null")) 
				out.append(rowData.get("siteURL")).append("<br/>");
			
			if (rowData.get("site2URL").length() > 0) 
				out.append(rowData.get("site2URL")).append("<br/>");
			
			// Setup the columns
			if ((i % 2) == 0) {
				out.append("</td>");
			} else { 
				out.append("</td></tr>");
				out.append("<tr><td colspan='3'><p>&nbsp;</p></td></tr>\n");
			}
			
		}
		out.append("<tr><td colspan='3'><p>&nbsp;</p></td></tr>\n");
		out.append("<tr><td colspan='3'>To learn more about joint replacement, ");
		out.append("or to find additional surgeons in your area, please visit ");
		if (productId.equals("4")) {
			out.append("www.hipreplacement.com");
		} else if (productId.equals("6")) {
			out.append("www.shoulderpainsolutions.com");
		} else {
			out.append("www.kneereplacement.com");
		}
		out.append(" or www.aaos.org.</td></tr>\n");
		out.append("</table>");
		return out;
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
        	log.error("Error retreiving surgeons from AAMD", e);
        }
        
		return sb;
	}
	
	/**
	 * Builds the url for the locator call
	 * @param address
	 * @param city
	 * @param state
	 * @param zip
	 * @return URL Formatted for AAMD Locator Request
	 */
	private String buildUrl() {

		StringBuffer s = new StringBuffer();
		s.append("http://www.allaboutmydoc.com/AAMD/locator?");
		s.append("display_template=/xml_display.jsp&company=1");
		s.append("&site_location=PATIENT_ACTIVATION&accept=true&country=US&language=en");
		s.append("&address=").append(encode(event.getAddressText()));
		s.append("&city=").append(encode(event.getCityName()));
		s.append("&state=").append(encode(event.getStateCode()));
		s.append("&zip=").append(encode(event.getZipCode()));
		//if (productId.equals("4")) { //hip events query by specialty, knee by product
			s.append("&product=&specialty=").append(productId);
		//} else {
			//s.append("&specialty=&product=").append(productId);
		//}
		s.append("&radius=").append(radius);
		s.append("&order=last");
		s.append("&resultCount=10");
		
		log.info("URL: " + s);
		return s.toString();
	}
	
	private String encode(String s) {
		try {
			s = java.net.URLEncoder.encode(StringUtil.checkVal(s), "UTF-8");
		} catch (Exception e) {}
		
		return s;
	}
			
}
