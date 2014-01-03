package com.ansmed.sb.report;

import java.util.Iterator;
import java.util.Map;

import com.ansmed.sb.physician.BusinessPlanVO;
import com.ansmed.sb.physician.ClinicVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.PhoneNumberFormat;
import com.smt.sitebuilder.action.AbstractSBReportVO;

public class PhysicianReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
	private Map<String, PhysicianContainerVO> data = null;
	private int maxClinics = 0;

	public PhysicianReportVO() {
		super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
		setFileName("physician.xls");
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (Map<String, PhysicianContainerVO>) o;

	}
	
	public byte[] generateReport() {
		// Get the Map of fields
		boolean includeBusPlan = false;
		Map<String, ?> fields = this.getAttributes();
		if (fields != null) includeBusPlan = true;
		StringBuffer sb = new StringBuffer();
		
		// Build the headers
		sb.append("<table border=\"1\">");
		sb.append("<tr><th nowrap>Physician ID</th>");
		sb.append("<th nowrap>First Name</th>");
		sb.append("<th nowrap>Last Name</th>");
		sb.append("<th nowrap>Title</th>");

		// Add columns based on max number of clinics.
		log.debug("Max number of clinics: " + getMaxClinics());
		for (int i = 0; i < getMaxClinics(); i++) {
			sb.append("<th nowrap>Location Type</th>");
			sb.append("<th nowrap>Practice Name</th>");
			sb.append("<th nowrap>Address</th>");
			sb.append("<th nowrap>Address 2</th>");
			sb.append("<th nowrap>City</th>");
			sb.append("<th nowrap>State</th>");
			sb.append("<th nowrap>Zip Code</th>");
			sb.append("<th nowrap>Office Phone</th>");
		}

		sb.append("<th nowrap>Cell Phone</th>");
		sb.append("<th nowrap>Specialty</th>");
		sb.append("<th nowrap>Physician Type</th>");
		sb.append("<th nowrap>Status</th>");
		sb.append("<th nowrap>TM Name</th>");
		sb.append("<th nowrap>Region</th>");
		sb.append("<th nowrap>Speaks Spanish</th>");
		sb.append("<th nowrap>Fellowship Trained</th>");
		sb.append("<th nowrap>Allow Mailings</th>");
		sb.append("<th nowrap>Website URL</th>");
		sb.append("<th nowrap>Email Address</th>");
		sb.append("<th nowrap>Spouse</th>");
		sb.append("<th nowrap>Children</th>");
		
		// Include business plan data
		if (includeBusPlan) {
			for(Iterator<String> iter = fields.keySet().iterator(); iter.hasNext(); ) {
				sb.append("<th nowrap>").append(iter.next()).append("</th>");
			}
		}
		
		sb.append("</tr>\r");
		
		// Retrieve the physician data
		for(Iterator<String> iter = data.keySet().iterator(); iter.hasNext(); ) {
			PhysicianContainerVO vo = data.get(iter.next());
			
			sb.append("<tr style=\"text-align:left;\">");
			sb.append("<td nowrap>").append(vo.getSurgeonId()).append("</td>");
			sb.append("<td nowrap>").append(vo.getFirstName()).append("</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getLastName())).append("</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getTitle())).append("</td>");

			// Add the clinic information for the physician
			sb = addClinics(sb,vo);

			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getCellPhone())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getSpecialtyName())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getSurgeonTypeName())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getStatusString())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getSalesRepName())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getSalesRegionName())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getSpanishString())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getFellowshipString())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getAllowMailString())).append("&nbsp;</td>");
			
			sb.append("<td nowrap>");
			if (StringUtil.checkVal(vo.getWebsite()).length() > 0) {
				sb.append("http://").append(vo.getWebsite()).append("&nbsp;</td>");
			} else {
				sb.append("&nbsp;</td>");
			}
			
			sb.append("<td nowrap>");
			if ((StringUtil.checkVal(vo.getEmailAddress()).length() > 0)) {
				sb.append(StringUtil.checkVal(vo.getEmailAddress())).append("&nbsp;</td>");
			} else {
				sb.append("&nbsp;</td>");
			}
			
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getSpouseName())).append("&nbsp;</td>");
			sb.append("<td nowrap>").append(StringUtil.checkVal(vo.getChildrenName())).append("&nbsp;</td>");
			
			// Append the business plan data.
			if (includeBusPlan) {
				for(Iterator<String> bpIter = fields.keySet().iterator(); bpIter.hasNext(); ) {
					Map<String, BusinessPlanVO> businessPlan = vo.getBusinessPlan();
					BusinessPlanVO val = new BusinessPlanVO();
					String key = bpIter.next();
					if (businessPlan.containsKey(key)) {
						val = businessPlan.get(StringUtil.checkVal(key));
					}
					
					sb.append("<td nowrap>").append(StringUtil.checkVal(val.getValueText())).append("&nbsp;</td>");
				}
			}
			
			sb.append("</tr>\r");
		}
		

		sb.append("</table>");
		log.debug("report length: " + sb.length());
		String temp = sb.toString();
		return temp.getBytes();
		//return sb.toString().getBytes();
	}
	
	//private StringBuffer addClinics(StringBuffer cbuf, PhysicianContainerVO pcvo) {
	private StringBuffer addClinics(StringBuffer cbuf, PhysicianContainerVO pcvo) {
		if (!pcvo.getClinics().isEmpty()) {
			log.debug("Size of clinics for this surgeon: " + pcvo.getClinics().size());
			
			if (pcvo.getClinics().size() == 1) {
				log.debug("Clinics == 1");
				cbuf = appendClinic(pcvo.getClinics().get(0),cbuf);
				cbuf = appendSpace(cbuf,((getMaxClinics()-1) * 8));
			} else {
				log.debug("Clinics > 1");
				Iterator<ClinicVO> cIter = pcvo.getClinics().iterator();					
			
				while(cIter.hasNext()) {
					ClinicVO cvo = cIter.next();
					cbuf = appendClinic(cvo,cbuf);
				}
				//Pad with blank columns if number of clinics < max clinics
				appendSpace(cbuf,((getMaxClinics() - pcvo.getClinics().size())*8));
			}
		}
		return cbuf;
	}
	
	private StringBuffer appendClinic(ClinicVO clinic, StringBuffer sbuf) {
		
		sbuf.append("<td nowrap>");
		switch(clinic.getLocationTypeId()) {
			case 1:
				sbuf.append("Primary&nbsp;</td>");
				break;
			case 2:
				sbuf.append("Home&nbsp;</td>");
				break;
			case 3:
				sbuf.append("Other&nbsp;</td>");
				break;
			default:
				sbuf.append("&nbsp;</td>");
		}
		sbuf.append("<td nowrap>").append(StringUtil.checkVal(clinic.getClinicName())).append("&nbsp;</td>");
		sbuf.append("<td nowrap>").append(StringUtil.checkVal(clinic.getAddress())).append("&nbsp;</td>");
		sbuf.append("<td nowrap>").append(StringUtil.checkVal(clinic.getAddress2())).append("&nbsp;</td>");
		sbuf.append("<td nowrap>").append(StringUtil.checkVal(clinic.getCity())).append("&nbsp;</td>");
		sbuf.append("<td nowrap>").append(StringUtil.checkVal(clinic.getState())).append("&nbsp;</td>");
		sbuf.append("<td nowrap>").append(StringUtil.checkVal(clinic.getZipCode())).append("&nbsp;</td>");
		
		// Format the phone number with dashes (type = 3)
		String pn = StringUtil.checkVal(clinic.getPhoneNumber("2"));
		PhoneNumberFormat pnf = new PhoneNumberFormat(pn,3);
		sbuf.append("<td nowrap>");
		if (pnf.getFormattedNumber() != null) {
			sbuf.append(pnf.getFormattedNumber());			
		} else {
			sbuf.append("");
		}
		sbuf.append("&nbsp;</td>");
		
		return sbuf;
	}
	
	private StringBuffer appendSpace(StringBuffer sbuf, int count) {
		for(int i = 0; i < count; i++) {
			sbuf.append("<td nowrap>&nbsp;</td>");
		}
		return sbuf;
	}

	public int getMaxClinics() {
		return maxClinics;
	}

	public void setMaxClinics(int maxClinics) {
		this.maxClinics = maxClinics;
	}
	
	
}
