package com.orthopediatrics.action;

// JDK 1.6
import java.util.List;

// SMTBaseLibs 2.0


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
		return this.generateStandardReport().toString().getBytes();
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
