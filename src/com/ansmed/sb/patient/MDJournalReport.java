package com.ansmed.sb.patient;

// JDK 1.6.0
import java.util.Collection;
import java.util.Iterator;

// SMT Base Libs
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>:MDJournalReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 3, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class MDJournalReport extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Collection<MDJournalVO> data = null;
	
	/**
	 * 
	 */
	public MDJournalReport() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table border=\"1\"><tr><td>Last Updated</td>");
		sb.append("<td>Physician Name</td>");
		sb.append("<td>SJM Staff at Visit</td>");
		sb.append("<td>Resaon For Visit</td>");
		sb.append("<td>Resources</td>");
		sb.append("<td>Topics of Discussion</td>");
		sb.append("<td>SJM Staff to Handle</td>");
		sb.append("<td>Date of In-Service</td>");
		sb.append("<td>Entered By</td></tr>");
		
		for (Iterator<MDJournalVO> iter = data.iterator(); iter.hasNext(); ) {
			MDJournalVO vo = iter.next();
			sb.append("<tr><td align=\"center\">").append(Convert.formatDate(vo.getLastUpdate())).append("</td>");
			sb.append("<td align=\"center\">").append(vo.getSurgeonName()).append("</td>");
			sb.append("<td align=\"center\">").append(vo.getWho()).append("</td>");
			sb.append("<td align=\"center\">").append(vo.getReasonForVisit()).append("</td>");
			sb.append("<td>").append(StringUtil.checkVal(vo.getCurrencies())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(vo.getSpokeAbout()).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(vo.getWhom()).append("&nbsp;</td>");
			sb.append("<td align=\"center\">");
			sb.append(Convert.formatDate(vo.getInServiceDate(),Convert.DATE_SLASH_PATTERN));
			sb.append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(vo.getRepName()).append("&nbsp;</td></tr>");
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object info) {
		if (info instanceof Collection<?>)
			data = (Collection<MDJournalVO>)info;

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
