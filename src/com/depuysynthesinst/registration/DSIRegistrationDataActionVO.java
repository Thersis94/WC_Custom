package com.depuysynthesinst.registration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.StandardExcelReport;
import com.siliconmtn.util.Convert;
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
			row.put("SUBMITTAL_ID", rdm.getRegisterSubmittalId());
			row.put("SITE_NAME", rdm.getSiteName());
			row.put("ROLE_LEVEL", rdm.getRoleName());
			row.put("ROLE_STATUS", rdm.getRoleStatus());
			row.put("ROLE_EXPIRATION", Convert.formatDate(rdm.getRoleExpireDt(),Convert.DATE_TIME_SLASH_PATTERN_12HR));
			row.put("LAST_LOGIN", Convert.formatDate(rdm.getLastLoginDt(),Convert.DATE_TIME_SLASH_PATTERN_12HR));
			String d;
			if (rdm.getDataMap().get("UPDATE_DT") instanceof Date){
				d = Convert.formatDate((Date)rdm.getDataMap().get("UPDATE_DT"), Convert.DATE_TIME_SLASH_PATTERN_12HR);
			}else {
				d = "";
			}
			row.put("PROFILE_LAST_UPDATED", d);
			row.put("PREFIX", rdm.getPrefixName());
			row.put("FIRST_NAME", rdm.getFirstName());
			row.put("LAST_NAME", rdm.getLastName());
			row.put("EMAIL_ADDRESS", rdm.getEmailAddress());
			row.put("ADDRESS", rdm.getAddress());
			row.put("ADDRESS2", rdm.getAddress2());
			row.put("CITY", rdm.getCity());
			row.put("STATE", rdm.getState());
			row.put("ZIP", rdm.getZipCode());
			row.put("COUNTRY", rdm.getCountryCode());
			row.put("PHONE", rdm.getMainPhone());
			row.put("ALLOW_COMMUNICATION", rdm.getAllowCommunication());


			for(Entry<String, String> entry: cdc.getFields().entrySet()){
				row.put(entry.getKey()+"_field", rdm.getExtData().get(entry.getKey()));
			}


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
		headerMap.put("SUBMITTAL_ID", "Submittal ID");
		headerMap.put("SITE_NAME", "Site Name");
		headerMap.put("ROLE_LEVEL", "Role Level");
		headerMap.put("ROLE_STATUS", "Role Status");
		headerMap.put("ROLE_EXPIRATION", "Role Expiration");
		headerMap.put("LAST_LOGIN", "Last Login");
		headerMap.put("PROFILE_LAST_UPDATED", "Profile Last Updated");
		headerMap.put("PREFIX", "Prefix");
		headerMap.put("FIRST_NAME", "First Name");
		headerMap.put("LAST_NAME", "Last Name");
		headerMap.put("EMAIL_ADDRESS", "Email Address");
		headerMap.put("ADDRESS", "Address");
		headerMap.put("ADDRESS2", "Address2");
		headerMap.put("CITY", "City");
		headerMap.put("STATE", "State");
		headerMap.put("ZIP", "Zip");
		headerMap.put("COUNTRY", "Country");
		headerMap.put("PHONE", "Phone");
		headerMap.put("ALLOW_COMMUNICATION", "Allow Communication");

		//lower case field concat to the end of the key to stop any possible duplicates
		for(Entry<String, String> entry: cdc.getFields().entrySet()){
			headerMap.put(entry.getKey()+"_field", entry.getValue());
		}

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
