package com.orthopediatrics.action;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ERPReportParser.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Custom report parser for ERP reports.  Report data is parsed based on the 
 * file format/extension specified for the report.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Aug 17, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ERPReportParser  {
	
	public static final Integer ROLE_AVP = new Integer(60);
	private ActionRequest request = null;
	private byte[] reportData = null;
	private byte[] formattedReport = null;
	private String reportFormat = "html";
	private String delimiter = ",";

	/**
	 * 
	 */
	public ERPReportParser(ActionRequest req) {
		this.request = req;
		reportFormat = "html";
	}

	/**
	 * 
	 * @param data
	 * @param format
	 */
	public ERPReportParser(byte[] data, String format) {
		this.reportData = data;
		this.reportFormat = format;
	}
	
	/**
	 * Parses XML report data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public byte[] parseReportData() throws DocumentException {
		ByteArrayInputStream bais = new ByteArrayInputStream(reportData);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Element root = doc.getRootElement().element("table1").element("Detail_Collection");
		List<Element> sc = new ArrayList<Element>();
		for (Iterator<Element> i = root.elementIterator(); i.hasNext(); ) {
			sc.add(i.next());
		}
		if (reportFormat.equals("html")) {
			parseHTML(sc);
		} else if (reportFormat.equals("xls")) {
			parseXLS(sc);
		} else if (reportFormat.equals("csv")) {
			parseCSV(sc);
		}
		return formattedReport;
	}
	
	/**
	 * Parses XML report data into an HTML table
	 * @param rows
	 */
	private void parseHTML(List<Element> rows) {
		StringBuffer report = new StringBuffer();
		report.append(getHTMLHeader());
		report.append("<table class=\"distributorInventory\">").append("\n");
		report.append("<tr class=\"headerRow\">");
		report.append("<th colspan=\"3\">Distributor Inventory Report&nbsp;&nbsp;&nbsp;");
		report.append("Distributor: ").append(this.checkDistributorId(request)).append("&nbsp;&nbsp;&nbsp;");
		report.append(this.retrieveDateTime()).append("</td></tr>").append("\n");
		report.append("<tr class=\"headerRow\"><th>Part No.</th><th>Description</th><th>Qty.</th>").append("\n");
		for (int i=0; i < rows.size(); i++) {
			Element rowEle = rows.get(i);
			report.append("<tr class=\"row_").append(i % 2).append("\">");
			report.append("<td class=\"partNo\">").append(rowEle.attributeValue("textbox4").trim()).append("</td>");
			report.append("<td class=\"description\">").append(rowEle.attributeValue("textbox5").trim()).append("</td>");
			report.append("<td class=\"quantity\">").append(rowEle.attributeValue("textbox6").trim()).append("</td>");
			report.append("</tr>").append("\n");
		}
		report.append("</table>");
		report.append(getHTMLFooter());
		formattedReport = report.toString().getBytes();
	}
	
	/**
	 * Parses XML report data into an HTML table
	 * @param rows
	 */
	private void parseXLS(List<Element> rows) {
		StringBuffer report = new StringBuffer();
		report.append(getXLSHeader());
		report.append("<table border=\"1\">").append("\n");
		report.append("<tr>");
		report.append("<th colspan=\"3\">Distributor Inventory Report&nbsp;&nbsp;&nbsp;");
		report.append("Distributor: ").append(this.checkDistributorId(request)).append("&nbsp;&nbsp;&nbsp;");
		report.append(this.retrieveDateTime()).append("</td></tr>").append("\n");
		report.append("<tr><th>Part No.</th><th>Description</th><th>Qty.</th>").append("\n");
		for (int i=0; i < rows.size(); i++) {
			Element rowEle = rows.get(i);
			report.append("<tr>");
			report.append("<td style=\"text-align: center;\">").append(rowEle.attributeValue("textbox4").trim()).append("</td>");
			report.append("<td nowrap style=\"padding: 0 10px 0 10px; text-align: left;\">").append(rowEle.attributeValue("textbox5").trim()).append("</td>");
			report.append("<td style=\"text-align: center;\">").append(rowEle.attributeValue("textbox6").trim()).append("</td>");
			report.append("</tr>").append("\n");
		}
		report.append("</table>");
		report.append(getHTMLFooter());
		formattedReport = report.toString().getBytes();
	}
	
	/**
	 * Parses XML report data into comma-delimited format
	 * @param rows
	 */
	private void parseCSV(List<Element> rows) {
		StringBuffer report = new StringBuffer();
		report.append("Distributor Inventory Report,");
		report.append("Distributor: ").append(checkDistributorId(request)).append(",").append(retrieveDateTime()).append("\n");
		report.append("Part No.,Description,Qty\n");
		for (int i=0; i < rows.size(); i++) {
			Element rowEle = rows.get(i);
			report.append(rowEle.attributeValue("textbox4").trim()).append(",");
			// replace any pre-existing commas in the description field with 'space dash space'
			String desc = StringUtil.replace(rowEle.attributeValue("textbox5").trim(),","," - ");
			report.append(desc).append(",");
			report.append(rowEle.attributeValue("textbox6").trim()).append("\n");
		}
		formattedReport = report.toString().getBytes();
	}
	
	private StringBuffer getHTMLHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">").append("\n");
		sb.append("<html>").append("\n");
		sb.append("<head>").append("\n");
		sb.append("<title>Distributor Inventory Report</title>").append("\n");
		sb.append("<link href=\"/binary/org/OP-Extranet/OPSalesPortal/scripts/inventory_rpt.css\" rel=\"stylesheet\" type=\"text/css\" media=\"all\" />").append("\n");
		sb.append("</head>").append("\n");
		sb.append("<body>").append("\n");
		return sb;
	}
	
	private StringBuffer getXLSHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">").append("\n");
		sb.append("<html>").append("\n");
		sb.append("<head>").append("\n");
		sb.append("<title>Distributor Inventory Report</title>").append("\n");
		sb.append("</head>").append("\n");
		sb.append("<body>").append("\n");
		return sb;
	}
	
	private StringBuffer getHTMLFooter() {
		StringBuffer sb = new StringBuffer();
		sb.append("</body></html>");
		return sb;
	}
	
	/**
	 * Returns distributor's territory ID (location ID).
	 * @param req
	 * @return
	 */
	private String checkDistributorId(ActionRequest req) {
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		SalesRepVO rep = (SalesRepVO)user.getUserExtendedInfo();
		if (role.getRoleLevel() < ROLE_AVP) {
			return rep.getTerritoryId(); 
		} else {
			return StringUtil.checkVal(req.getParameter("territoryId"));
		}
	}
	
	/**
	 * Returns date/time as a String
	 * @return
	 */
	private String retrieveDateTime() {
		String today = "";
		Calendar cal = GregorianCalendar.getInstance();
		today = Convert.formatDate(cal.getTime(), Convert.DATE_TIME_SLASH_PATTERN_12HR);
		return today;
	}
	
	public byte[] getReportData() {
		return reportData;
	}

	public void setReportData(byte[] reportData) {
		this.reportData = reportData;
	}

	public String getReportFormat() {
		return reportFormat;
	}

	public void setReportFormat(String reportFormat) {
		if (reportFormat != null) this.reportFormat = reportFormat.toLowerCase();
	}

	/**
	 * @return the formattedReport
	 */
	public byte[] getFormattedReport() {
		return formattedReport;
	}

	/**
	 * @param formattedReport the formattedReport to set
	 */
	public void setFormattedReport(byte[] formattedReport) {
		this.formattedReport = formattedReport;
	}

	/**
	 * @return the delimiter
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * @param delimiter the delimiter to set
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * @return the request
	 */
	public ActionRequest getRequest() {
		return request;
	}

	/**
	 * @param request the request to set
	 */
	public void setRequest(ActionRequest request) {
		this.request = request;
	}
	
}
