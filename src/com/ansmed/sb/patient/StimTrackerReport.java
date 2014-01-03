package com.ansmed.sb.patient;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>:StimTrackerReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 3, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class StimTrackerReport extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<PatientVO> data = new ArrayList<PatientVO>();
	
	/**
	 * 
	 */
	public StimTrackerReport() {
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		StringBuffer sb = new StringBuffer();
		sb.append("<table border=\"1\"><tr><td>Last Updated</td>");
		sb.append("<td>First Name</td>");
		sb.append("<td>Last Name</td>");
		sb.append("<td>Phone Number</td>");
		sb.append("<td>Referring Physician</td>");
		sb.append("<td>Patient Ed Date</td>");
		sb.append("<td>Lead Entry Level</td>");
		sb.append("<td>Final Lead Location</td>");
		sb.append("<td>Trial Date</td>");
		sb.append("<td>Trial Removal Date</td>");
		sb.append("<td>Perm Consult Date</td>");
		sb.append("<td>Perm Date</td>");
		sb.append("<td>Patient Status</td>");
		sb.append("<td>Trial Facility</td>");
		sb.append("<td>Perm Facility</td>");
		sb.append("<td>Insurance</td>");
		sb.append("<td>Comments</td></tr>");
		
		for (int i = 0; i < data.size(); i++) {
			PatientVO vo = data.get(i);
			PhoneNumberFormat pnf = new PhoneNumberFormat();
			pnf.setPhoneNumber(vo.getPhoneNumber());
			sb.append("<tr><td align=\"center\">").append(Convert.formatDate(vo.getLastUpdate())).append("</td>");
			sb.append("<td align=\"center\">").append(vo.getFirstName()).append("</td>");
			sb.append("<td align=\"center\">").append(vo.getLastName()).append("</td>");
			sb.append("<td align=\"center\">").append(pnf.getFormattedNumber()).append("</td>");
			
			sb.append("<td align=\"center\">");
			if (vo.getReferringPhys() != null && vo.getReferringPhys().length() > 0) {
				if (vo.getReferringPhys().equalsIgnoreCase("other")) {
					sb.append(StringUtil.checkVal(vo.getOtherPhys()));
				} else {
					sb.append(vo.getReferringPhys());
				}
			} else {
				if (vo.getOtherPhys() != null && vo.getOtherPhys().length() > 0) {
					sb.append(vo.getOtherPhys());
				}
			}
			sb.append("&nbsp;</td>");
			
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(Convert.formatDate(vo.getPatientEduationDate()))).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getEntryLead())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getFinalLead())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(Convert.formatDate(vo.getTrialDate()))).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(Convert.formatDate(vo.getTrialRemovalDate()))).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(Convert.formatDate(vo.getPermConsultDate()))).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(Convert.formatDate(vo.getPermDate()))).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getPatientStatus())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getTrialFacility())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getPermFacility())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getInsurance())).append("&nbsp;</td>");
			sb.append("<td align=\"center\">").append(StringUtil.checkVal(vo.getComments())).append("&nbsp;</td>");
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
		if (info instanceof List<?>)
			data = (List<PatientVO>)info;

	}

}
