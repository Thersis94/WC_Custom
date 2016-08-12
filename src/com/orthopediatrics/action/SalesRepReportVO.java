package com.orthopediatrics.action;

// JDK 1.6
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

// SMTBaseLibs 2.0



import java.util.Map;

import com.siliconmtn.data.report.ExcelReport;
// SitebuilderII 2.0
import com.siliconmtn.util.PhoneNumberFormat;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: SalesRepReportVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Data bean containing sales rep report data.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 30, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SalesRepReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = -6162859913461769470L;
	private List<SalesRepVO> data = null;
	
	public SalesRepReportVO() {
		
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		if (o instanceof List<?>) {
			data = (List<SalesRepVO>)o;
		}
		
	}
	
	@Override
	public byte[] generateReport() {
		if (fileName != null && fileName.endsWith(".xls") ){
		return this.generateNonHtmlReport();
		}else{
		return this.generateStandardReport().toString().getBytes();
		}
	}
	
	/**
	 * generates a non html report
	 * @return
	 */
	private byte[] generateNonHtmlReport() {
		ExcelReport rpt = new ExcelReport(this.getHeader());
		List<Map<String, Object>> rows = null;
		if (data != null && data.size() > 0) {
			rows = new ArrayList<>(data.size());
			rows = generateDataRows(rows);
		} else {
			rows = new ArrayList<>(1);
			rows = emptyRow(rows);		
		}
		//Close the table.
		
		log.debug("report size: " + rows.size());
		return rpt.generateReport();
	}
	
	
	/**
	 * first non header row, first cell contains the message when there is no information to process
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> emptyRow(List<Map<String, Object>> rows) {
		Map<String, Object> row = new HashMap<String, Object>();
		row.put("REGION_ID","No sales rep information found.");
		rows.add(row);
		return rows;
	}

	/**
	 * generates a standard row of data
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		//loop/add the data
		for(SalesRepVO sv : data) {
			Map<String, Object> row = new HashMap<String, Object>();
			
			row.put("REGION_ID",sv.getRegionId());
			row.put("REGION_NAME",sv.getRegions().get(sv.getRegionId()));
			row.put("TERRITORY_ID",sv.getTerritoryId());
			row.put("TERRITORY_NAME",sv.getTerritories().get(sv.getTerritoryId()));
			row.put("ROLE",sv.getRoleName());
			row.put("LAST_NAME",sv.getLastName());
			row.put("FIRST_NAME",sv.getFirstName());
			row.put("EMAIL_ADDRESS",sv.getEmailAddress());
			pnf.setPhoneNumber(sv.getPhoneNumber());
			row.put("PHONE",pnf.getFormattedNumber());
			row.put("CLASS_ID",sv.getClassId());
			rows.add(row);
		}
		return rows;
	}

	/**
	 * generates the header map for the report
	 * @return
	 */
	private Map<String, String> getHeader() {
		
		HashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		headerMap.put("REGION_ID",     "Region ID");
		headerMap.put("REGION_NAME",   "Region Name");
		headerMap.put("TERRITORY_ID",  "Territory ID");
		headerMap.put("TERRITORY_NAME","Territory Name");
		headerMap.put("ROLE",          "Role");
		headerMap.put("LAST_NAME",     "Last Name");
		headerMap.put("FIRST_NAME",    "First Name");
		headerMap.put("EMAIL_ADDRESS", "Email Address");
		headerMap.put("PHONE",         "Phone");
		headerMap.put("CLASS_ID",      "Class ID");
		
		return headerMap;
	}

	@Override
	public void setFileName(String f) {
		super.setFileName(f);
		if (f.endsWith(".xls") || f.endsWith(".cvs")) {
	        setContentType("application/vnd.ms-excel");
	        isHeaderAttachment(Boolean.TRUE);
		} else {
			setContentType("text/html");
		}
	}
	
	/**
	 * Creates the report in standard format
	 * @return
	 */
	private StringBuffer generateStandardReport() {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		StringBuffer sb = new StringBuffer();
		sb.append("<table border=\"1\">");
		this.addStandardHeader(sb);
		if (data != null && data.size() > 0) {
			//loop/add the data
			for(SalesRepVO sv : data) {
				this.addStandardRow(sv, sb, pnf);
			}
		} else {
			sb.append("<tr><td colspan=\"6\">No sales rep information found.</td></tr>");
		}
		//Close the table.
		sb.append("</table>");	
		log.debug("report size: " + sb.length());
		return sb;
	}
	
	
	/**
	 * Adds the standard header text to the report.
	 * @param sb
	 */
	private void addStandardHeader(StringBuffer sb) {
		//adds a standard HTML table header
		sb.append("<tr>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Region ID</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Region Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Territory ID</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Territory Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Role</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Last Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">First Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Email Address</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Phone</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Class ID</td>");
		sb.append("</tr>");
	}
	
	/**
	 * Adds a data row to the standard version of the report.
	 * @param sv
	 * @param sb
	 */
	private void addStandardRow(SalesRepVO sv, StringBuffer sb, PhoneNumberFormat pnf) {
		sb.append("<tr>");
		sb.append("<td>").append(sv.getRegionId()).append("</td>");
		sb.append("<td>").append(sv.getRegions().get(sv.getRegionId())).append("</td>");
		sb.append("<td>").append(sv.getTerritoryId()).append("</td>");
		sb.append("<td>").append(sv.getTerritories().get(sv.getTerritoryId())).append("</td>");
		sb.append("<td>").append(sv.getRoleName()).append("</td>");
		sb.append("<td>").append(sv.getLastName()).append("</td>");
		sb.append("<td>").append(sv.getFirstName()).append("</td>");
		sb.append("<td>").append(sv.getEmailAddress()).append("</td>");
		pnf.setPhoneNumber(sv.getPhoneNumber());
		sb.append("<td>").append(pnf.getFormattedNumber()).append("</td>");
		sb.append("<td>").append(sv.getClassId()).append("</td>");
		sb.append("</tr>");
	}
	
}
