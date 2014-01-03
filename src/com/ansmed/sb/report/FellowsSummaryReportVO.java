package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Iterator;
import java.util.List;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

// SMT BaseLibs
import com.siliconmtn.util.DateUtil;

/****************************************************************************
 * <b>Title</b>: FellowsDetailReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 05, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class FellowsSummaryReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<FellowsSummaryVO> data = null;
	
	/**
	 * 
	 */
	public FellowsSummaryReportVO() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
				
		StringBuffer sb = new StringBuffer();
		int count = 1;
		if (data != null && data.size() > 0) {
			//SUMMARY Report
			
			if (count > 1) sb.append("<br />");
			
			//Table header
			sb.append("<table border=\"1\">");
			sb.append("<tr>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">TM Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Physian First Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Physician Last Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Physician Fellow</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Email</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Phone</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Program Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Specialty</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Fellow End Date</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Post Grad Plans</td>");
			sb.append("</tr>");
			
			//Append table data
			for(Iterator<FellowsSummaryVO> iter = data.iterator(); iter.hasNext();) {
				FellowsSummaryVO fd = iter.next();
				sb.append("<tr>");
				sb.append("<td>").append(fd.getRepFirstNm()).append(" ").append(fd.getRepLastNm());
				sb.append("<td>").append(fd.getSurgeonFirstNm()).append("</td>");
				sb.append("<td>").append(fd.getSurgeonLastNm()).append("</td>");
				sb.append("<td>").append(fd.getFellowsNm()).append("</td>");
				sb.append("<td>").append(fd.getFellowsEmail()).append("</td>");
				sb.append("<td>").append(fd.getFellowsPhone()).append("</td>");
				sb.append("<td>").append(fd.getProgramNm()).append("</td>");
				sb.append("<td>").append(fd.getSpecialtyNm()).append("</td>");
				sb.append("<td>");
				if (fd.getFellowsEndMonth().intValue() > 0) {
					sb.append(DateUtil.getMonthNameLong(fd.getFellowsEndMonth()));
					sb.append(",&nbsp;");
					sb.append(fd.getFellowsEndYear());
				} else {
					sb.append("Not specified.");
				}
				sb.append("</td>");
				sb.append("<td>").append(fd.getFellowsPlan()).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
			
			count++;
				
		} else {
			//Default - no data exists for surgeons.
			sb.append("<table border=\"1\">");
			sb.append("<tr>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">TM Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Physian First Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Physician Last Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Physician Fellow</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Email</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Phone</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Program Name</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Specialty</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Fellow End Date</td>");
			sb.append("<td style=\"text-align:center;font-weight:bold\">Post Grad Plans</td>");
			sb.append("</tr>");
			sb.append("<tr><td colspan=\"10\">No physician fellows information found.</td></tr>");
			sb.append("</table>");
		}
					
		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object info) {
		if (info instanceof List<?>) {
			data = (List<FellowsSummaryVO>)info;
		}
	}
	
	@Override
	public void setFileName(String f) {
		super.setFileName(f);
		if (f.endsWith(".xls")) {
	        setContentType("application/vnd.ms-excel");
	        isHeaderAttachment(Boolean.TRUE);
		} else {
			setContentType("text/html");
		}
	}

}