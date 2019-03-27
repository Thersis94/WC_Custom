package com.rezdox.data;

// Java 8
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;

//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: WarrantyReport.java</p>
 <p><b>Description: </b>The formatted Excel Report for RezDox warranty reports.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2019 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Mar 18, 2019
 <b>Changes:</b> 
 ***************************************************************************/
public class WarrantyReport extends AbstractSBReportVO {

	private static final long serialVersionUID = 3182019L;
	private transient List<WarrantyReportVO> data;
	private Date startDate;
	private Date endDate;

	public WarrantyReport() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Warranty Registration Report.xls");
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
		String title = "Warranty Registration Report – RezDox LLC ";
		if (startDate != null) title += "\rStarting: " + Convert.formatDate(startDate, Convert.DATE_SLASH_PATTERN);
		if (endDate != null) title += "\rEnding: " + Convert.formatDate(endDate, Convert.DATE_SLASH_PATTERN);
		rpt.setTitleCell(title);
		return rpt.generateReport();
	}

	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private void generateDataRows(Map<String, String> hdr, List<Map<String, Object>> rows) {
		Map<String,Object> row;
		Set<String> keys = hdr.keySet();
		for (WarrantyReportVO vo : data) {
			row = new HashMap<>(keys.size(), 100l);

			//mirror what's on the header row
			for (String methodNm : keys)
				row.put(methodNm, callMethod(methodNm, vo));

			rows.add(row);
		}
	}


	/**
	 * @param methodNm
	 * @param vo
	 * @return
	 */
	private String callMethod(String methodNm, Object vo) {
		try {
			return StringUtil.checkVal(PropertyUtils.getSimpleProperty(vo, methodNm));
		} catch (Exception e) {
			log.error("could not invoke "  + methodNm, e);
			return "";
		}
	}


	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> hdr = new LinkedHashMap<>();
		hdr.put("firstName", "First Name");
		hdr.put("lastName", "Last Name");
		hdr.put("address", "Address 1");
		hdr.put("address2", "Address 2");
		hdr.put("city", "City");
		hdr.put("state", "State");
		hdr.put("zip", "Zip");
		hdr.put("fmtPhone", "Phone");
		hdr.put("email", "Email");
		hdr.put("purchaseDateStr", "Date of Purchase");
		hdr.put("purchaseLocation", "Purchase Location");
		hdr.put("manufacturer", "Manufacturer");
		hdr.put("brand", "Brand");
		hdr.put("model", "Model#");
		hdr.put("serial", "Serial#");
		hdr.put("source", "RezDox Source");
		return hdr;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.data =  (List<WarrantyReportVO>) o;
	}


	/**
	 * @param startDt
	 */
	public void setStartDate(Date startDt) {
		this.startDate = startDt;
	}


	/**
	 * @param endDt
	 */
	public void setEndDate(Date endDt) {
		this.endDate = endDt;
	}
}