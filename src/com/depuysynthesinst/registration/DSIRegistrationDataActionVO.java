package com.depuysynthesinst.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

		ExcelReport rpt = new StandardExcelReport(this.getHeader());;

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

		for (RegistrationDataModuleVO rdm : cdc.getData()) {
			Map<String, Object> row = new HashMap<>();

			row.put("DATE", Convert.formatDate(rdm.getSubmittalDate(), "MMMM dd, yyyy"));
			row.put("TIME", Convert.formatDate(rdm.getSubmittalDate(), Convert.TIME_SHORT_PATTERN));
			row.put("PROFILE", rdm.getProfileId());
			row.put("DSI_TTLMS_ID", StringUtil.checkVal(rdm.getExtData().get("DSI_TTLMS_ID")));
			row.put("SUBMITTAL_ID", rdm.getRegisterSubmittalId());
			row.put("ROLE_LEVEL", rdm.getRoleName());
			row.put("ROLE_STATUS", rdm.getRoleStatus());
			row.put("FIRST_NAME", rdm.getFirstName());
			row.put("LAST_NAME", rdm.getLastName());
			row.put("DSI_DEGREE", StringUtil.checkVal(rdm.getExtData("DSI_DEGREE")));
			row.put("EMAIL_ADDRESS", rdm.getEmailAddress());
			row.put("COUNTRY", rdm.getCountryCode());
			//profession and specialty are in the form as guids
			row.put("c0a80241b71c9d40a59dbd6f4b621260", StringUtil.checkVal(rdm.getExtData("c0a80241b71c9d40a59dbd6f4b621260")));
			row.put("c0a80241b71d27b038342fcb3ab567a0", StringUtil.checkVal(rdm.getExtData("c0a80241b71d27b038342fcb3ab567a0")));
			row.put("DSI_PGY", StringUtil.checkVal(rdm.getExtData("DSI_PGY")));
			row.put("DSI_ACAD_NM", StringUtil.checkVal(rdm.getExtData("DSI_ACAD_NM")));
			row.put("DSI_ACAD_CITY", StringUtil.checkVal(rdm.getExtData("DSI_ACAD_CITY")));
			row.put("DSI_ACAD_STATE", StringUtil.checkVal(rdm.getExtData("DSI_ACAD_STATE")));
			row.put("DSI_PROG_ELIGIBLE", StringUtil.checkVal(rdm.getExtData("DSI_PROG_ELIGIBLE")));
			row.put("DSI_MIL_HOSP", StringUtil.checkVal(rdm.getExtData("DSI_MIL_HOSP")));
			row.put("DSI_GRAD_DT", StringUtil.checkVal(rdm.getExtData("DSI_GRAD_DT")));
			row.put("ALLOW_COMMUNICATION", rdm.getAllowCommunication());
			row.put("DSI_VERIFIED", StringUtil.checkVal(rdm.getExtData("DSI_VERIFIED")));
	
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

		headerMap.put("DATE", "Date");
		headerMap.put("TIME", "Time");
		headerMap.put("PROFILE", "Profile ID");
		headerMap.put("DSI_TTLMS_ID", cdc.getFields().get("DSI_TTLMS_ID"));
		headerMap.put("SUBMITTAL_ID", "Submittal ID");
		headerMap.put("ROLE_LEVEL", "Role Level");
		headerMap.put("ROLE_STATUS", "Role Status");
		headerMap.put("FIRST_NAME", "First Name");
		headerMap.put("LAST_NAME", "Last Name");
		headerMap.put("DSI_DEGREE", cdc.getFields().get("DSI_DEGREE"));
		headerMap.put("EMAIL_ADDRESS", "Email Address");
		headerMap.put("COUNTRY", "Country");
		//profession and specialty are in the form as guids
		headerMap.put("c0a80241b71c9d40a59dbd6f4b621260", cdc.getFields().get("c0a80241b71c9d40a59dbd6f4b621260"));
		headerMap.put("c0a80241b71d27b038342fcb3ab567a0", cdc.getFields().get("c0a80241b71d27b038342fcb3ab567a0"));
		headerMap.put("DSI_PGY", cdc.getFields().get("DSI_PGY"));
		headerMap.put("DSI_ACAD_NM", cdc.getFields().get("DSI_ACAD_NM"));
		headerMap.put("DSI_ACAD_CITY", cdc.getFields().get("DSI_ACAD_CITY"));
		headerMap.put("DSI_ACAD_STATE", cdc.getFields().get("DSI_ACAD_STATE"));
		headerMap.put("DSI_PROG_ELIGIBLE", cdc.getFields().get("DSI_PROG_ELIGIBLE"));
		headerMap.put("DSI_MIL_HOSP", cdc.getFields().get("DSI_MIL_HOSP"));
		headerMap.put("DSI_GRAD_DT", cdc.getFields().get("DSI_GRAD_DT"));
		headerMap.put("ALLOW_COMMUNICATION", "Allow Communication");
		headerMap.put("DSI_VERIFIED", cdc.getFields().get("DSI_VERIFIED"));

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
