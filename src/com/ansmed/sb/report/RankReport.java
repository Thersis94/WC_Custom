package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

// SB ANS Libs
import com.ansmed.sb.physician.ActualsVO;

/****************************************************************************
 * <b>Title</b>:RankReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr 28, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class RankReport extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Collection<RankActualVO> data = null;
	
	/**
	 * 
	 */
	public RankReport() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		StringBuffer sb = new StringBuffer();
		if (data != null && data.size() > 0) {
			sb.append("<table border=\"1\"><tr>");
			sb.append("<td>TM Name</td>");
			sb.append("<td>Physician First Name</td>");
			sb.append("<td>Physician Last Name</td>");
			sb.append("<td>Title</td>");
			sb.append("<td>Rank</td>");
			sb.append("<td>Actual Dollars</td>");
			sb.append("<td>Trials</td>");
			sb.append("<td>Perms</td>");
			//sb.append("<td>% Growth</td>");
			sb.append("<td>Revenue Targets</td></tr>");
			
			for (Iterator<RankActualVO> iter = data.iterator(); iter.hasNext(); ) {
				RankActualVO vo = iter.next();
				sb.append("<tr>");
				sb.append("<td align=\"center\">").append(vo.getRepFirstName());
				sb.append("&nbsp;").append(vo.getRepLastName()).append("</td>");
				sb.append("<td align=\"center\">").append(vo.getSurgeonFirstName()).append("</td>");
				sb.append("<td align=\"center\">").append(vo.getSurgeonLastName()).append("</td>");
				sb.append("<td align=\"center\">").append(vo.getTitleNm()).append("</td>");
				sb.append("<td align=\"center\">").append(vo.getRank()).append("</td>");
				sb.append("<td align=\"center\">").append((int)vo.getActualsData().getDollars()).append("</td>");
				sb.append("<td align=\"center\">").append(vo.getActualsData().getTotalTrials()).append("</td>");
				sb.append("<td align=\"center\">").append(vo.getActualsData().getTotalPerms()).append("</td>");
				//sb.append("<td align=\"center\">").append("------").append("</td>");
				sb.append("<td align=\"center\">").append(vo.getRevenueTarget()).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
			
		} else {
			sb.append("<table border=\"1\"><tr><td>TM Name</td>");
			sb.append("<td>Physician First Name</td>");
			sb.append("<td>Physician Last Name</td>");
			sb.append("<td>Title</td>");
			sb.append("<td>Rank</td>");
			sb.append("<td>Actual Dollars</td>");
			sb.append("<td>Trials</td>");
			sb.append("<td>Perms</td>");
			//sb.append("<td>% Growth</td>");
			sb.append("<td>Revenue Targets</td></tr>");
			sb.append("<tr><td colspan=\"10\">").append("No rank information found.").append("</td></tr>");
		}
		
		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object info) {
		if (info instanceof Collection<?>)
			data = (Collection<RankActualVO>)info;

	}
	
	public void setActuals(Map<String,ActualsVO> actuals) {
		
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