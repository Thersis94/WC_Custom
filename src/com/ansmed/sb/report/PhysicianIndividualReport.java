package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Iterator;
import java.util.List;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

// SB ANS Libs
import com.ansmed.sb.physician.BusAssessVO;
import com.ansmed.sb.physician.BusGoalVO;

/****************************************************************************
 * <b>Title</b>:PhysicianIndividualReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr 30, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PhysicianIndividualReport extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private PhysicianIndividualVO data = null;
	
	/**
	 * 
	 */
	public PhysicianIndividualReport() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		
		StringBuffer sb = new StringBuffer();
		if (data != null && data.getSwot().size() > 0) {
			sb.append("<table border=\"1\">");
			sb.append("<tr><td>&nbsp;</td>").append("<td colspan=\"2\" align=\"center\">Physician</td></tr>");
			sb.append("<tr><td>TM Name</td><td>First Name</td><td>Last Name</td></tr>");
			sb.append("<tr><td>").append(data.getSwot().get(0).getRepFirstNm()).append("&nbsp;");
			sb.append(data.getSwot().get(0).getRepLastNm()).append("</td>");
			sb.append("<td>").append(data.getSwot().get(0).getSurgeonFirstNm()).append("</td>");
			sb.append("<td>").append(data.getSwot().get(0).getSurgeonLastNm()).append("</td></tr>");
			sb.append("</table>");
			
			sb.append("<br />");
			
			// SWOT output
			List<BusAssessVO> b = data.getSwot();
			for(Iterator<BusAssessVO> iter = b.iterator(); iter.hasNext();) {
				BusAssessVO bvo = iter.next();
				sb.append("<table border=\"1\">");
				sb.append("<tr>");
				sb.append("<td>");
				switch(bvo.getAssessType().intValue()) {
					case 1:
						sb.append("Strengths");
						break;
					case 2:
						sb.append("Weakness");
						break;
					case 3:
						sb.append("Opportunities");
						break;
					case 4:
						sb.append("Threats");
						break;
					default:
						sb.append("&nbsp;");
						break;
				}
				sb.append("</td>");
				sb.append("<td>").append(bvo.getAssessTxt()).append("</td>");
				sb.append("</tr>");
				sb.append("</table>");
				sb.append("<br />");
			}
			
			//Objectives output
			sb.append("<table border=\"1\">");
			List<BusGoalVO> o = data.getObjectives();
			int count = 0;
			for(Iterator<BusGoalVO> iter = o.iterator(); iter.hasNext();) {
				BusGoalVO bgo = iter.next();
				count++;
				sb.append("<tr>");
				sb.append("<td>Objective&nbsp;").append(count).append("</td>");
				sb.append("<td colspan=\"3\">&nbsp;</td>");
				sb.append("</tr>");
				
				sb.append("<tr>");
				sb.append("<td>&nbsp;</td><td>Action Steps</td>");
				sb.append("<td>").append(bgo.getGoal()).append("</td>");
				sb.append("</tr>");	
			}			
			sb.append("</table>");
			
		} else {
			//Default if no values exist for this surgeon
			sb.append("<table border=\"1\"><tr>");
			sb.append("<td>&nbsp;</td>").append("<td colspan=\"2\" align=\"center\">Physician</td>");
			sb.append("</tr>");
			sb.append("<tr><td>TM Name</td><td>First Name</td><td>Last Name</td></tr>");
			sb.append("<tr><td colspan=\"3\">No SWOT information found for this physician.</td></tr>");
			sb.append("</table>");
			
		}
		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object info) {
		if (info instanceof PhysicianIndividualVO) {
			data = (PhysicianIndividualVO)info;
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