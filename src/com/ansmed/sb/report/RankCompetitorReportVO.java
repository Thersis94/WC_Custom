package com.ansmed.sb.report;

//JDK 1.6.0
import java.util.Iterator;
import java.util.List;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: RankCompetitorReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 07, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class RankCompetitorReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<ImplantVolumeVO> data = null;
	
	/**
	 * 
	 */
	public RankCompetitorReportVO() {
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
			sb.append("<tr>");
			sb.append("<td colspan=\"4\">* - based on forecast data</td>");
			sb.append("<td colspan=\"5\" style=\"text-align: center; font-weight: bold; background-color: #ffcc99;\">Trials</td>");
			sb.append("<td colspan=\"5\" style=\"text-align: center; font-weight: bold; background-color: #99ccff;\">Perms</td>");
			sb.append("</tr>");
			sb.append("<tr>");
			sb.append("<td style=\"text-align: center; font-weight: bold;\">TM</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician First Name</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician Last Name</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold;\">Title</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #ffcc99;\">*BSC</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #ffcc99;\">*MDT</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #ffcc99;\">*SJM</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #ffcc99;\">*Unknown</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #ffcc99;\">*Volume</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #99ccff;\">*BSC</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #99ccff;\">*MDT</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #99ccff;\">*SJM</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #99ccff;\">*Unknown</td>");
			sb.append("<td style=\"text-align: center; font-weight: bold; background-color: #99ccff;\">*Volume</td>");
			sb.append("</tr>");
			
			//Append table data
			for(Iterator<ImplantVolumeVO> iter = data.iterator(); iter.hasNext();) {
				ImplantVolumeVO ivo = iter.next();
				sb.append("<tr>");
				sb.append("<td style=\"text-align: center;\">").append(ivo.getRepFirstNm()).append("&nbsp;");
				sb.append(ivo.getRepLastNm()).append("</td>");
				sb.append("<td style=\"text-align: center;\">").append(ivo.getSurgeonFirstNm()).append("</td>");
				sb.append("<td style=\"text-align: center;\">").append(ivo.getSurgeonLastNm()).append("</td>");
				sb.append("<td style=\"text-align: center;\">").append(ivo.getTitleNm()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #ffcc99;\">").append(ivo.getBscTrialsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #ffcc99;\">").append(ivo.getMdtTrialsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #ffcc99;\">").append(ivo.getSjmTrialsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #ffcc99;\">").append(ivo.getUnknownTrialsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #ffcc99;\">").append(ivo.getTotalTrialsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #99ccff;\">").append(ivo.getBscPermsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #99ccff;\">").append(ivo.getMdtPermsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #99ccff;\">").append(ivo.getSjmPermsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #99ccff;\">").append(ivo.getUnknownPermsVolume()).append("</td>");
				sb.append("<td style=\"text-align: center; background-color: #99ccff;\">").append(ivo.getTotalPermsVolume()).append("</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
			
		} else {
			//Default - no data exists for surgeons.
			sb.append("<table border=\"1\">");
			sb.append("<tr><td>TM</td><td>Physician First Name</td>");
			sb.append("</td><td>Physician Last Name</td><td>Title</td>");
			sb.append("<td>BSC</td><td>MDT</td><td>SJM</td>");
			sb.append("<td>Unknown</td><td>Total Trials Volume</td>");
			sb.append("<td>Total Perms Volume</td></tr>");
			sb.append("<tr><td colspan=\"9\">No physician implant volume information found.</td></tr>");
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
			data = (List<ImplantVolumeVO>)info;
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