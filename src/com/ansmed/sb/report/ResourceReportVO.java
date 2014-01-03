package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

// SMT BaseLibs
import com.siliconmtn.util.DateUtil;

// SB ANS Libs
import com.ansmed.sb.physician.ResourceVO;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;

/****************************************************************************
 * <b>Title</b>:ResourceReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 04, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class ResourceReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String,List<ResourceVO>> data = null;
	
	/**
	 * 
	 */
	public ResourceReportVO() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		
		String key = "";
		if (data.containsKey("individual")) {
			key = "individual";
		} else {
			key = "summary";
		}
		
		List<ResourceVO> rvo = data.get(key);
				
		StringBuffer sb = new StringBuffer();
		
		if (rvo != null && rvo.size() > 0) {
			if (key.equalsIgnoreCase("summary")) {
				//SUMMARY Report
				//Table header
				sb.append("<table border=\"1\">");
				sb.append("<tr><td>TM Name</td><td>Physician Last Name</td><td>Program</td>");
				sb.append("<td>Projected Quarter</td><td>Objective for Use</td>");
				sb.append("<td>Actual Date</td><td>Follow-up/Result</td>");
				sb.append("<td>Projected vs Actual</td></tr>");
				
				//Append table data
				for(Iterator<ResourceVO> iter = rvo.iterator(); iter.hasNext();) {
					ResourceVO rv = iter.next();
					sb.append("<tr><td>").append(rv.getRepFirstNm()).append("&nbsp;");
					sb.append(rv.getRepLastNm()).append("</td>");
					sb.append("<td>").append(rv.getSurgeonLastNm()).append("</td>");
					sb.append("<td>").append(rv.getResourceNm()).append("</td>");
					sb.append("<td>");
					if (rv.getUsedQtr() > 0) {
						sb.append("Q");
						sb.append(rv.getUsedQtr());
					} else {
						sb.append("&nbsp;");
					}
					sb.append("</td>");
					sb.append("<td>").append(rv.getResourceObj()).append("</td>");
					sb.append("<td>");
					if (rv.getCompletionMonth() > 0) {
						sb.append(DateUtil.getMonthNameLong(rv.getCompletionMonth()));
						sb.append(",&nbsp;");
						sb.append(rv.getCompletionYear());
					} else {
						sb.append("&nbsp;");
					}
					sb.append("</td>");
					sb.append("<td>").append(rv.getResourceResult()).append("</td>");
					sb.append("<td>");
					// Projected Quarter/Year
					if (rv.getUsedQtr() > 0 && rv.getUsedYear() > 0) {
						sb.append("Q").append(rv.getUsedQtr());
						sb.append(", ");
						sb.append(rv.getUsedYear());
					} else {
						sb.append("None");
					}
					
					sb.append(" | ");
					
					// Actual Quarter/Year
					if (rv.getCompletionMonth() > 0 && rv.getCompletionYear() > 0) {
						sb.append("Q").append(SJMBusinessCalendar.calculateQuarter(rv.getCompletionMonth().intValue()));
						sb.append(", ");
						sb.append(rv.getCompletionYear());
					} else {
						sb.append("None");
					}
					sb.append("</td></tr>");
				}
				
				sb.append("</table>");
				
			} else {
				//INDIVIDUAL Report
				for(Iterator<ResourceVO> iter = rvo.iterator(); iter.hasNext();) {
					ResourceVO rv = iter.next();
					
					//Table header (Names)
					sb.append("<table border=\"1\">");
					sb.append("<tr><td>&nbsp;</td><td>TM Name</td><td>Physician</td></tr>");
					sb.append("<tr><td>&nbsp;</td><td>").append(rv.getRepFirstNm());
					sb.append("&nbsp;").append(rv.getRepLastNm()).append("</td>");
					sb.append("<td>").append(rv.getSurgeonFirstNm()).append("&nbsp;");
					sb.append(rv.getSurgeonLastNm()).append("</td></tr>");
					sb.append("</table>");
					
					sb.append("<br />");
					
					//Table header (objectives)
					sb.append("<table border=\"1\">");
					sb.append("<tr>");
					sb.append("<td colspan=\"2\">").append("Program</td>");
					sb.append("<td>").append(rv.getResourceNm()).append("</td>");
					sb.append("</tr>");
					sb.append("<tr>");
					sb.append("<td colspan=\"2\">Objective for Use</td>");
					sb.append("<td>").append(rv.getResourceObj()).append("</td>");
					sb.append("</tr>");
					
					sb.append("<tr>");
					sb.append("<td colspan=\"2\">Time Line</td>");
					sb.append("<td>").append(rv.getCompletionMonth()).append(", ");
					sb.append(rv.getCompletionYear()).append("</td>");
					sb.append("</tr>");
					
					sb.append("<tr>");
					sb.append("<td colspan=\"2\">Follow up Result</td>");
					sb.append("<td>").append(rv.getResourceResult()).append("</td>");
					sb.append("</tr>");
					sb.append("</table>");
					
					sb.append("<br />");
				}
			}
			
		} else {
			if (key.equalsIgnoreCase("summary")) {
				//Default - no data exists for surgeons.
				sb.append("<table border=\"1\">");
				sb.append("<tr><td>TM Name</td><td>Physician Last Name</td><td>Program</td>");
				sb.append("<td>Projected Quarter</td><td>Actual Date</td>");
				sb.append("<td>Projected vs Actual</td></tr>");
				sb.append("<tr><td colspan=\"6\">No physician resource information found.</td></tr>");
				sb.append("</table>");
			} else {
				sb.append("<table border=\"1\">");
				sb.append("<tr><td>&nbsp;</td><td>TM Name</td><td>Physician</td></tr>");
				sb.append("<tr><td colspan=\"3\">No physician resource information found.</td></tr>");
				sb.append("</table>");
			}

		}
		
		//Close the table.
			
		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object info) {
		if (info instanceof Map<?,?>) {
			data = (Map<String,List<ResourceVO>>)info;
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