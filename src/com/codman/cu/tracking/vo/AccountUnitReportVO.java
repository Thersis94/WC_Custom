package com.codman.cu.tracking.vo;

import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: AccountUnitReportVO<p/>
 * <b>Description: expands upon the default UnitReport to do data extraction 
 * 		before iterating the data</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class AccountUnitReportVO extends UnitReportVO {
	private static final long serialVersionUID = -4473379747242916803L;
	private List<AccountVO> data;

	public AccountUnitReportVO(SiteVO site) {
		super(site);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Account Unit Report");

		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();

		// make title row, its the first row in the sheet (0)
		int rowNo = 0;
		Row r = s.createRow(rowNo++);
		addTitleRow(wb, s, r);

		//make column headings row
		r = s.createRow(rowNo++);
		addHeaderRow(wb, s, r);

		//loop the accounts, physians, units, and requests
		for (AccountVO acct : data) {
			for (TransactionVO t : acct.getTransactions()) {
				for (UnitVO v : t.getUnits()) {
					v.setAccountName(acct.getAccountName());
					v.setRepName(StringUtil.checkVal(acct.getRep().getFirstName()) + " " + StringUtil.checkVal(acct.getRep().getLastName()));
					v.setPhysicianName(StringUtil.checkVal(t.getPhysician().getFirstName()) + " " + StringUtil.checkVal(t.getPhysician().getLastName()));
					r = s.createRow(rowNo++); //create a new row
					formatUnit(v, r); //populate the row
				}
			}
		}
		
	    // Auto-size the columns.
		for (int x=0; x < 11; x++)
			s.autoSizeColumn(x);

		//lastly, stream the WorkBook back to the browser
		return ExcelReport.getBytes(wb);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (List<AccountVO>) o;
	}
}