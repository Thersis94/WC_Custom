package com.rezdox.data;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserReport.java</p>
 <p><b>Description: </b>The formatted Excel Report for RezDox user-data exports.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jul 02, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class UserReport extends AbstractSBReportVO {

	private static final long serialVersionUID = 7854535932838671578L;
	private transient List<UserReportVO> data;

	public UserReport() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("RezDox Users.xls");
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		Map<String, String> hdr = getHeader();
		List<Map<String, Object>> rows = new ArrayList<>(data.size());
		generateDataRows(hdr, rows);

		ExcelReport rpt = new ExcelReport(hdr);
		rpt.setData(rows);
		return rpt.generateReport();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.data =  (List<UserReportVO>) o;
	}

	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private void generateDataRows(Map<String, String> hdr, List<Map<String, Object>> rows) {
		Map<String,Object> row;
		for (UserReportVO vo : data) {
			row = new HashMap<>();

			//mirror what's on the header row
			for (String methodNm : hdr.keySet())
				row.put(methodNm, callMethod(methodNm, vo));

			rows.add(row);
		}
	}


	/**
	 * @param methodNm
	 * @param vo
	 * @return
	 */
	private String callMethod(String methodNm, UserReportVO vo) {
		try {
			return StringUtil.checkVal(PropertyUtils.getSimpleProperty(vo, methodNm));
		} catch (Exception e) {
			log.error("could not invoke "  + methodNm, e);
		}
		return "";
	}


	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> hdr = new LinkedHashMap<>();
		hdr.put("businessName", "Business Name");
		hdr.put("firstName", "First Name");
		hdr.put("lastName", "Last Name");
		hdr.put("role", "User Role");
		hdr.put("enrollDateStr", "Enrolled");
		hdr.put("address", "Address 1");
		hdr.put("address2", "Address 2");
		hdr.put("city", "City");
		hdr.put("state", "State");
		hdr.put("zip", "Zip");
		hdr.put("fmtPhone", "Phone");
		hdr.put("email", "Email");
		hdr.put("status", "User Status");
		hdr.put("category", "Category (Biz)");
		hdr.put("subCategory", "Sub-Category (Biz)");
		hdr.put("website", "Website");
		return hdr;
	}
}