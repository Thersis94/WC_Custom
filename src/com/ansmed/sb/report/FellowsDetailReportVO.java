package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Iterator;
import java.util.List;

// SB Libs
import com.ansmed.sb.physician.FellowsGoalVO;
import com.ansmed.sb.physician.FellowsVO;
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
public class FellowsDetailReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<FellowsVO> data = null;
	
	/**
	 * 
	 */
	public FellowsDetailReportVO() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		
		StringBuffer sb = new StringBuffer();
		
		if (data != null && data.size() > 0) {
			//Detail Report
			int recCount = 1;
			for(Iterator<FellowsVO> iter = data.iterator(); iter.hasNext();) {
				if (recCount > 1) sb.append("<br />");
				FellowsVO fv = iter.next();
				sb.append("<table border=\"1\">");
				sb.append("<tr><td align=\"center\"><b>TM Name</b></td>");
				sb.append("<td align=\"center\"><b>Fellow Physician</b></td>");
				sb.append("<td align=\"center\"><b>Physician</b></td></tr>");
				sb.append("<tr>");
				sb.append("<td align=\"center\">").append(fv.getRepFirstNm()).append("&nbsp;");
				sb.append(fv.getRepLastNm());
				sb.append("</td>");
				sb.append("<td align=\"center\">").append(fv.getFellowsSurgeon().get(0).getFellowsNm()).append("</td>");
				sb.append("<td align=\"center\">").append(fv.getSurgeonFirstNm()).append("&nbsp;");
				sb.append(fv.getSurgeonLastNm()).append("</td>");
				sb.append("</tr>");
				sb.append("</table>");
				
				sb.append("<br />");
				
				List<FellowsGoalVO> fg = fv.getFellowsGoal();
				if (fg != null && fg.size() > 0) {
					int count = 1;
					for(Iterator<FellowsGoalVO> iter1 = fg.iterator(); iter1.hasNext();) {
						if (count > 1) sb.append("<br />");
						FellowsGoalVO fgv = iter1.next();
						sb.append("<table border=\"1\">");
						sb.append("<tr>");
						sb.append("<td width=\"25%\" align=\"center\"><b>Objective&nbsp;").append(count).append("</b></td>");
						sb.append("<td colspan=\"2\">").append(fgv.getFellowsGoal()).append("</td>");
						sb.append("</tr>");
						
						sb.append("<tr>");
						sb.append("<td>&nbsp;</td><td><b>Action Steps</b></td>");
						sb.append("<td colspan=\"2\">").append(fgv.getFellowsAction()).append("</td>");
						sb.append("</tr>");
						
						sb.append("<tr>");
						sb.append("<td>&nbsp;</td><td><b>Time Line</b></td>");
						sb.append("<td colspan=\"2\">");
						if (fgv.getFellowsGoalMonth().intValue() > 0) { 
							sb.append(DateUtil.getMonthNameLong(fgv.getFellowsGoalMonth().intValue()));
							sb.append(",&nbsp;");
							sb.append(fgv.getFellowsGoalYear().intValue());
						} else {
							sb.append("Not specified.");
						}
						sb.append("</td>");
						sb.append("</tr>");
						sb.append("</table>");
					}
				}
				recCount++;
			}

		} else {
			//Default - no data exists for surgeons.
			sb.append("<table border=\"1\">");
			sb.append("<tr><td>TM Name</td><td>Fellows Physician</td><td>Physician</td></tr>");
			sb.append("<tr><td colspan=\"3\">No fellows information found.</td></tr>");
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
			data = (List<FellowsVO>)info;
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