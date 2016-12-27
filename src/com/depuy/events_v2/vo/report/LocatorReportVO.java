package com.depuy.events_v2.vo.report;

// JDK 1.7
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

// DOM4J libs
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// Log4j 1.2.8
import org.apache.log4j.Logger;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.PhoneNumberFormat;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;

/****************************************************************************
 * <b>Title</b>: LocatorReportVO.java<p/>
 * <b>Description: Makes a call to the AAMD surgeon locator web service and
 * downloads the appropriate information in XML format.  The data is formatted and returned</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 2.0
 * @since Feb 3, 2014
 ****************************************************************************/
public class LocatorReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 6l;
	protected static Logger log;
	public static final int DEFAULT_RADIUS = 50;  //miles
	protected int radius = DEFAULT_RADIUS;
	protected DePuyEventSeminarVO sem;
	protected EventEntryVO event;
	private String aamdUrl;

	public LocatorReportVO() {
		super();
		log = Logger.getLogger(getClass());
		setContentType("application/msword");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Locator-Results.doc");
	}

	public void setData(Object o) {
		sem = (DePuyEventSeminarVO) o;
		event = sem.getEvents().get(0);
	}

	public void setRadius(String radius) {
		if (Convert.formatInteger(radius) > 0)
			this.radius = Convert.formatInteger(radius);
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
		StringBuilder doc = getHeader();
		try {
			doc.append(formatDisplay(connect(buildUrl())));

		} catch (Exception ioe) {
			log.error("Unable to retrieve locator results", ioe);
		}

		doc.append("</body></html>");
		log.info(doc);
		return doc.toString().getBytes();
	}


	protected StringBuilder getHeader() {
		StringBuilder sb = new StringBuilder(5000);
		sb.append("<html><head><title>Surgeon Locator Report</title></head><body>");
		sb.append("<u>Today's Seminar:</u><br/>"); 
		sb.append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append("<br/>");
		sb.append(event.getLocationDesc()).append("<br/>");
		sb.append("Speaker: ").append(sem.getSurgeon().getSurgeonName()).append("<br/><br/>");
		sb.append(event.getEventName()).append(", ").append(event.getCityName()).append(", ").append(event.getStateCode());

		sb.append("<h1>LOCAL ORTHOPAEDIC SURGEONS</h1>\n");

		// Load the header into
		sb.append("<p>We are pleased to supply you with a list of orthopaedic surgeons in your area who use DePuy Synthes products.  ");
		sb.append("While our database of orthopaedic surgeons is large, it is not a complete ");
		sb.append("listing of all orthopaedic surgeons in your area.  A surgeon's use of ");
		sb.append("DePuy Synthes products is the sole criterion for being listed below. No orthopaedic ");
		sb.append("surgeon has paid a fee to participate.  DePuy Synthes Inc., ");
		sb.append("does not make any recommendation or referral regarding any of these specific surgeons.</p>");
		sb.append("<p>For general information about DePuy Synthes, visit www.depuysynthes.com</p>");

		return sb;
	}

	@SuppressWarnings("rawtypes")
	protected StringBuilder formatDisplay(StringBuilder xml) 
			throws ParserConfigurationException, UnsupportedEncodingException, SAXException, IOException {
		// Parse out the XML Data and create the root element
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new ByteArrayInputStream(xml.toString().getBytes("UTF-8")));

		// Create an html formatted table structure
		StringBuilder out = new StringBuilder(1000);
		out.append("<table border=\"0\"><tr><td colspan='3'>");
		out.append("(Orthopaedic Surgeons are listed in alphabetical order)</td></tr>");

		NodeList result = doc.getElementsByTagName("result");
		Map<String, String> rowData = new HashMap<>();
		log.info("Number Results: " + result.getLength());
		PhoneNumberFormat pnf = null;
		StringEncoder se = new StringEncoder();

		for (int i=0; i < result.getLength() && i < 100; i++) { //limit by size of results, and a max of 100 surgeons
			Node n = result.item(i);
			if (Node.ELEMENT_NODE != n.getNodeType()) continue; //make sure it's an Element
			Element ele = (Element) n;
			
			//turn the Node into a Map<k,v> to keep separation of XML parsing and view generation
			NodeList children = ele.getChildNodes();
			for (int x=0; x < children.getLength(); x++) { //limit by size of results, and a max of 100 surgeons
				Node child = children.item(x);
				if (Node.ELEMENT_NODE != child.getNodeType()) continue; //make sure it's an Element
				Element childEle = (Element) child;
				if (log.isDebugEnabled())
						log.debug(childEle.getNodeName() + "=" + childEle.getTextContent());
				rowData.put(childEle.getNodeName(), childEle.getTextContent());
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

			if (!StringUtil.isEmpty(rowData.get("address2")))
				out.append(", ").append(rowData.get("address2"));

			out.append("<br/>");

			out.append(rowData.get("city")).append(", ");
			out.append(rowData.get("state")).append(" ").append(rowData.get("zip"));
			out.append("<br/>");

			out.append("<b>Phone: ").append(pnf.getFormattedNumber()).append("</b><br/>");
			if (!StringUtil.isEmpty(rowData.get("siteURL")) && !rowData.get("siteURL").endsWith("null")) 
				out.append(rowData.get("siteURL")).append("<br/>");

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
		out.append("www.reallifetested.com or www.aaos.org.</td></tr>\n");
		out.append("</table>");
		return out;
	}

	/**
	 * Connects to DePuy Locator using the supplied url and gets the Locator XML Data
	 * @param actionUrl
	 * @return
	 * @throws IOException
	 */
	protected StringBuilder connect(String actionUrl) throws IOException {
		StringBuilder sb = new StringBuilder();
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

			// Retrieve the data coming back from MapQuest and Store it in a StringBuilder
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
	protected String buildUrl() {
		StringBuilder s = new StringBuilder(300);
		s.append(aamdUrl).append("/json?amid=locator&");
		s.append("display_template=/xml_display.jsp&company=1");
		s.append("&site_location=PATIENT_ACTIVATION&country=US&language=en");
		s.append("&address=").append(encode(event.getAddressText()));
		s.append("&city=").append(encode(event.getCityName()));
		s.append("&state=").append(encode(event.getStateCode()));
		s.append("&zip=").append(encode(event.getZipCode()));
		s.append("&specialty=").append(sem.getJointCodes());
		s.append("&radius=").append(radius);
		s.append("&order=last");

		log.info("URL: " + s);
		return s.toString();
	}

	protected String encode(String s) {
		return StringEncoder.urlEncode(s);
	}

	public String getAamdUrl() {
		return aamdUrl;
	}

	public void setAamdUrl(String aamdUrl) {
		this.aamdUrl = aamdUrl;
	}
}