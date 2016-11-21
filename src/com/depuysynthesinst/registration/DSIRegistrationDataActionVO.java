package com.depuysynthesinst.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.DSIUserDataVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.StandardExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.registration.RegistrationDataContainer;
import com.smt.sitebuilder.action.registration.RegistrationDataModuleVO;

/****************************************************************************
 * <b>Title</b>: RegistrationDataActionVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> processes a request for a non html registration data action report vo
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Aug 24, 2016<p/>
 * @updates:
 ****************************************************************************/
public class DSIRegistrationDataActionVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1L;
	RegistrationDataContainer cdc = null;

	public DSIRegistrationDataActionVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("RegistrationDataAction.xls");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		ExcelReport rpt = new StandardExcelReport(this.getHeader());

		List<Map<String, Object>> rows = new ArrayList<>();

		rows = generateDataRows(rows);

		rpt.setData(rows);

		return rpt.generateReport();
	}

	/**
	 * generate data rows of report
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {

		//profession and specialty are in the form as guids placed into strings for readablity
		String profession = DSIUserDataVO.RegField.c0a80241b71c9d40a59dbd6f4b621260.name();
		String Specialty = DSIUserDataVO.RegField.c0a80241b71d27b038342fcb3ab567a0.name();
		
		
		for (RegistrationDataModuleVO rdm : cdc.getData()) {
			Map<String, Object> row = new HashMap<>();

			row.put("DATE", Convert.formatDate(rdm.getSubmittalDate(), "MMMM dd, yyyy"));
			row.put("TIME", Convert.formatDate(rdm.getSubmittalDate(), Convert.TIME_SHORT_PATTERN));
			row.put("PROFILE", rdm.getProfileId());
			row.put(DSIUserDataVO.RegField.DSI_TTLMS_ID.name(), StringUtil.checkVal(rdm.getExtData().get(DSIUserDataVO.RegField.DSI_TTLMS_ID.name())));
			row.put("SUBMITTAL_ID", rdm.getRegisterSubmittalId());
			row.put("ROLE_LEVEL", rdm.getRoleName());
			row.put("ROLE_STATUS", rdm.getRoleStatus());
			row.put("FIRST_NAME", rdm.getFirstName());
			row.put("LAST_NAME", rdm.getLastName());
			row.put("DSI_DEGREE", StringUtil.checkVal(rdm.getExtData("DSI_DEGREE")));
			row.put("EMAIL_ADDRESS", rdm.getEmailAddress());
			row.put("COUNTRY", rdm.getCountryCode());
			row.put(profession, StringUtil.checkVal(rdm.getExtData(profession)));
			row.put(Specialty, StringUtil.checkVal(rdm.getExtData(Specialty)));
			row.put(DSIUserDataVO.RegField.DSI_PGY.name(), StringUtil.checkVal(rdm.getExtData(DSIUserDataVO.RegField.DSI_PGY.name())));
			row.put(DSIUserDataVO.RegField.DSI_ACAD_NM.name(), StringUtil.checkVal(rdm.getExtData(DSIUserDataVO.RegField.DSI_ACAD_NM.name())));
			row.put("DSI_ACAD_CITY", StringUtil.checkVal(rdm.getExtData("DSI_ACAD_CITY")));
			row.put("DSI_ACAD_STATE", StringUtil.checkVal(rdm.getExtData("DSI_ACAD_STATE")));
			row.put(DSIUserDataVO.RegField.DSI_PROG_ELIGIBLE.name(), StringUtil.checkVal(rdm.getExtData(DSIUserDataVO.RegField.DSI_PROG_ELIGIBLE.name())));
			row.put(DSIUserDataVO.RegField.DSI_MIL_HOSP.name(), StringUtil.checkVal(rdm.getExtData(DSIUserDataVO.RegField.DSI_MIL_HOSP.name())));
			row.put(DSIUserDataVO.RegField.DSI_GRAD_DT.name(), StringUtil.checkVal(rdm.getExtData(DSIUserDataVO.RegField.DSI_GRAD_DT.name())));
			row.put("ALLOW_COMMUNICATION", Convert.formatInteger(rdm.getAllowCommunication()) == 1 ? "Yes":"No");
			row.put(DSIUserDataVO.RegField.DSI_VERIFIED.name(), StringUtil.checkVal(rdm.getExtData(DSIUserDataVO.RegField.DSI_VERIFIED.name())));
	
			rows.add(row);
		}

		return rows;
	}

	/**
	 * generates the header row
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();

		//profession and specialty are in the form as guids placed into strings for readablity
		String profession = DSIUserDataVO.RegField.c0a80241b71c9d40a59dbd6f4b621260.name();
		String Specialty = DSIUserDataVO.RegField.c0a80241b71d27b038342fcb3ab567a0.name();
		
		headerMap.put("DATE", "Date");
		headerMap.put("TIME", "Time");
		headerMap.put("PROFILE", "Profile ID");
		headerMap.put(DSIUserDataVO.RegField.DSI_TTLMS_ID.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_TTLMS_ID.name()));
		headerMap.put("SUBMITTAL_ID", "Submittal ID");
		headerMap.put("ROLE_LEVEL", "Role Level");
		headerMap.put("ROLE_STATUS", "Role Status");
		headerMap.put("FIRST_NAME", "First Name");
		headerMap.put("LAST_NAME", "Last Name");
		headerMap.put("DSI_DEGREE", cdc.getFields().get("DSI_DEGREE"));
		headerMap.put("EMAIL_ADDRESS", "Email Address");
		headerMap.put("COUNTRY", "Country");
		//profession and specialty are in the form as guids
		headerMap.put(profession, cdc.getFields().get(profession));
		headerMap.put(Specialty, cdc.getFields().get(Specialty));
		headerMap.put(DSIUserDataVO.RegField.DSI_PGY.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_PGY.name()));
		headerMap.put(DSIUserDataVO.RegField.DSI_ACAD_NM.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_ACAD_NM.name()));
		headerMap.put("DSI_ACAD_CITY", cdc.getFields().get("DSI_ACAD_CITY"));
		headerMap.put("DSI_ACAD_STATE", cdc.getFields().get("DSI_ACAD_STATE"));
		headerMap.put(DSIUserDataVO.RegField.DSI_PROG_ELIGIBLE.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_PROG_ELIGIBLE.name()));
		headerMap.put(DSIUserDataVO.RegField.DSI_MIL_HOSP.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_MIL_HOSP.name()));
		headerMap.put(DSIUserDataVO.RegField.DSI_GRAD_DT.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_GRAD_DT.name()));
		headerMap.put("ALLOW_COMMUNICATION", "Allow Communication");
		headerMap.put(DSIUserDataVO.RegField.DSI_VERIFIED.name(), cdc.getFields().get(DSIUserDataVO.RegField.DSI_VERIFIED.name()));

		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		RegistrationDataContainer methodCdc = (RegistrationDataContainer ) o;
		this.cdc = (RegistrationDataContainer ) methodCdc;

	}

}
