package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Iterator;
import java.util.List;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: TerritorySummaryReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 07, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class TerritorySummaryReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<TerritorySummaryVO> data = null;
	
	/**
	 * 
	 */
	public TerritorySummaryReportVO() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
				
		StringBuffer sb = new StringBuffer();
		if (data != null && data.size() > 0) {
			//Table header
			sb.append("<table border=\"1\">");
			sb.append("<tr><td>TM</td><td>Physician First Name</td>");
			sb.append("<td>Physician Last Name</td><td>Title</td><td>Practice Name</td>");
			sb.append("<td>Address</td><td>Address2</td><td>City</td>");
			sb.append("<td>State</td><td>Zip</td><td>Office Number</td>");
			sb.append("<td>Email Address</td></tr>");
			
			//Append table data
			for(Iterator<TerritorySummaryVO> iter = data.iterator(); iter.hasNext();) {
				TerritorySummaryVO ts = iter.next();
				sb.append("<tr>");
				sb.append("<td>").append(ts.getRepFirstNm()).append("&nbsp;");
				sb.append(ts.getRepLastNm()).append("</td>");
				sb.append("<td>").append(ts.getSurgeonFirstNm()).append("</td>");
				sb.append("<td>").append(ts.getSurgeonLastNm()).append("</td>");
				sb.append("<td>").append(ts.getTitleNm()).append("</td>");
				sb.append("<td>").append(ts.getPracticeNm()).append("</td>");
				sb.append("<td>").append(ts.getAddressTxt()).append("</td>");
				sb.append("<td>").append(ts.getAddress2Txt()).append("</td>");
				sb.append("<td>").append(ts.getCityNm()).append("</td>");
				sb.append("<td>").append(ts.getStateCd()).append("</td>");
				sb.append("<td>").append(ts.getZipCd()).append("</td>");
				sb.append("<td>").append(ts.getPhoneNo()).append("</td>");
				sb.append("<td>").append(ts.getSurgeonEmailAddress()).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
							
		} else {
			//Default - no data exists for surgeons.
			sb.append("<table border=\"1\">");
			sb.append("<tr><td>TM</td><td>Physician First Name</td>");
			sb.append("<td>Physician Last Name</td><td>Title</td><td>Practice Name</td>");
			sb.append("<td>Address</td><td>Address 2</td><td>City</td>");
			sb.append("<td>State</td><td>Zip</td><td>Office Number</td>");
			sb.append("<td>Email Address</td></tr>");
			sb.append("<tr><td colspan=\"12\">No territory summary information found.</td></tr>");
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
			data = (List<TerritorySummaryVO>)info;
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