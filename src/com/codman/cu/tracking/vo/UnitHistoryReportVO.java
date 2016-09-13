package com.codman.cu.tracking.vo;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.codman.cu.tracking.UnitAction;
import com.codman.cu.tracking.vo.UnitVO.ProdType;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: UnitDetailReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2011
 * @updates
 * 		09.07.2016, refactored from HTML to true Excel using the POI libraries - JM
 ****************************************************************************/
public class UnitHistoryReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1407073622234040274L;
	protected List<UnitVO> data;
	protected SiteVO siteVo;
	protected boolean isRepReport = false;

	public UnitHistoryReportVO(SiteVO site) {
		super();
		this.siteVo = site;
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Control Unit History Report.xls");
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting Unit History Report");
		boolean isMedstream = (data != null && data.size() > 0 && data.get(0).getProductType() == ProdType.MEDSTREAM);

		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();

		// make title row, its the first row in the sheet (0)
		int rowNo = 0;
		Row r = s.createRow(rowNo++);
		addTitleRow(wb, s, r);

		//make the column headings row
		r = s.createRow(rowNo++);
		addHeaderRow(wb, s, r, isMedstream);



		//loop the accounts, physians, units, and requests
		for (UnitVO v : data) {
			r = s.createRow(rowNo++); //create a new row
			formatUnit(v, r); //populate the row
		}
		
	    // Auto-size the columns.
		int colCnt = isRepReport ? 24: 31;
		for (int x=0; x < colCnt; x++)
			s.autoSizeColumn(x);

		//lastly, stream the WorkBook back to the browser
		return ExcelReport.getBytes(wb);
	}


	/**
	 * creates the initial title (header) row #1.
	 * @param wb
	 * @param s
	 * @param r
	 */
	protected void addTitleRow(Workbook wb, Sheet s, Row r) {
		int colCnt = isRepReport ? 24: 31;
		
		r.setHeight((short)(r.getHeight()*2));

		//make a heading font for the title to be large and bold
		CellStyle headingStyle = wb.createCellStyle();
		Font font = wb.createFont();
		font.setFontHeightInPoints((short)16);
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headingStyle.setFont(font);

		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue("Codman CU Tracking System - Unit History");
		c.setCellStyle(headingStyle);
		//merge it the length of the report (all columns).
		s.addMergedRegion(new CellRangeAddress(0,0,0,colCnt));
	}


	/**
	 * adds row #2, the column headings
	 * @param wb
	 * @param s
	 * @param r
	 * @param isMedStream
	 */
	protected void addHeaderRow(Workbook wb, Sheet s, Row r, boolean isMedStream) {
		int cellCnt = 0;
		createStringCell(r, "Date", cellCnt++);
		createStringCell(r, "Status", cellCnt++);
		createStringCell(r, "Unit Type", cellCnt++);
		createStringCell(r, "Transaction Type", cellCnt++);
		createStringCell(r, "Serial No.", cellCnt++);
		createStringCell(r, "User", cellCnt++);
		createStringCell(r, "Software Rev No.", cellCnt++);
		if (!isRepReport && isMedStream) { 
			createStringCell(r, "Hardware Rev No.", cellCnt++);
		}
		if (isMedStream) {
			createStringCell(r, "IFU Article No.", cellCnt++);
			createStringCell(r, "IFU Rev No.", cellCnt++);
			createStringCell(r, "Prog Article No.", cellCnt++);
			createStringCell(r, "Prog Rev No.", cellCnt++);
		}
		if (!isRepReport) { 
			createStringCell(r, "Battery Type", cellCnt++);
			if (isMedStream) {
				createStringCell(r, "Battery Serial No.", cellCnt++);
			} else {
				createStringCell(r, "Battery Recharge Date", cellCnt++);
			}
			createStringCell(r, "Lot No.", cellCnt++);
			createStringCell(r, "Service/Repair No.", cellCnt++);
			if (isMedStream) {
				createStringCell(r, "Service/Repair Date", cellCnt++);
			} else {
				createStringCell(r, "Service/Refurb Date", cellCnt++);
			}
		}
		createStringCell(r, "Comments", cellCnt++);
		if (!isRepReport) {
			createStringCell(r, "Production Comments", cellCnt++);
		}
		createStringCell(r, "Date Deployed", cellCnt++);
		createStringCell(r, "Account", cellCnt++);
		createStringCell(r, "Rep Name", cellCnt++);
		createStringCell(r, "Physician Name", cellCnt++);
		createStringCell(r, "Center", cellCnt++);
		createStringCell(r, "Department", cellCnt++);
		createStringCell(r, "Physician Phone#", cellCnt++);
		createStringCell(r, "Physician Address", cellCnt++);
		createStringCell(r, "Address2", cellCnt++);
		createStringCell(r, "City", cellCnt++);
		createStringCell(r, "State", cellCnt++);
		createStringCell(r, "Zip/Postal", cellCnt++);
		createStringCell(r, "Country", cellCnt++);
	}


	/**
	 * transforms a UnitVO (piece of data) into a row of data on the report
	 * @param u
	 * @param restricted
	 * @param r
	 */
	protected void formatUnit(UnitVO u, Row r) {
		boolean isMedstream =  (u.getProductType() == ProdType.MEDSTREAM);
		int cellCnt = 0;

		createStringCell(r, this.formatDate(u.getCreateDate()), cellCnt++);
		createStringCell(r, UnitAction.getStatusName(u.getStatusId()), cellCnt++);
		String prodNm = (u.getProductType() != null) ? u.getProductType().toString() : "";
		createStringCell(r, prodNm, cellCnt++);
		String transType = "";
		if (u.getTransactionType() == null || u.getTransactionType() == 0) transType = "Unit Update";
		else if (u.getTransactionType() == 2 && !isMedstream) transType = "Return for Refurb";
		else if (u.getTransactionType() == 2) transType = "Transfer";
		else if (u.getTransactionType() == 3) transType = "Refurbish";
		else if (u.getTransactionType() == 1) transType = "New Request";

		createStringCell(r, transType, cellCnt++);
		createStringCell(r, u.getSerialNo(), cellCnt++);
		createStringCell(r, u.getModifyingUserName(), cellCnt++);
		createStringCell(r, u.getSoftwareRevNo(), cellCnt++);
		if (!isRepReport && isMedstream) {
			createStringCell(r, u.getHardwareRevNo(), cellCnt++);
		}
		if (isMedstream) {
			createStringCell(r, u.getIfuArticleNo(), cellCnt++);
			createStringCell(r, u.getIfuRevNo(), cellCnt++);
			createStringCell(r, u.getProgramArticleNo(), cellCnt++);
			createStringCell(r, u.getProgramRevNo(), cellCnt++);
		}
		if (!isRepReport) {
			createStringCell(r, u.getBatteryType(), cellCnt++);
			if (isMedstream) {
				createStringCell(r, u.getBatterySerNo(), cellCnt++);
			} else {
				createStringCell(r, this.formatDate(u.getBatteryRechargeDate()), cellCnt++);
			}
			createStringCell(r, u.getLotNo(), cellCnt++);
			createStringCell(r, u.getServiceRefNo(), cellCnt++);
			createStringCell(r, this.formatDate(u.getServiceDate()), cellCnt++);
		}
		createStringCell(r, u.getCommentsText(), cellCnt++);
		if (!isRepReport) {
			createStringCell(r, u.getProductionCommentsText(), cellCnt++);
		}
		createStringCell(r, this.formatDate(u.getDeployedDate()), cellCnt++);
		createStringCell(r, u.getAccountName(), cellCnt++);
		createStringCell(r, u.getRepName(), cellCnt++);
		createStringCell(r, u.getPhysicianName(), cellCnt++);

		PhysicianVO phys = u.getPhysician();
		if (phys == null) return; //don't need the addtl cells if they're void of data.
		createStringCell(r, phys.getCenterText(), cellCnt++);
		createStringCell(r, phys.getDepartmentText(), cellCnt++);
		createStringCell(r, phys.getMainPhone(), cellCnt++);
		createStringCell(r, phys.getAddress(), cellCnt++);
		createStringCell(r, phys.getAddress2(), cellCnt++);
		createStringCell(r, phys.getCity(), cellCnt++);
		createStringCell(r, phys.getState(), cellCnt++);
		createStringCell(r, phys.getZipCode(), cellCnt++);
		createStringCell(r, phys.getCountryCode(), cellCnt++);
	}


	/**
	 * populates and returns a single String cell on the given row.
	 * @param r
	 * @param value
	 * @param cellNo
	 * @return
	 */
	protected Cell createStringCell(Row r, Object value, int cellNo) {
		Cell c = r.createCell(cellNo);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue(StringUtil.checkVal(value));
		return c;
	}


	/**
	 * date formatting helper - leverages the site's Locale for Intl localization
	 * @param d
	 * @return
	 */
	protected String formatDate(Date d) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, siteVo.getLocale());
		return (d != null) ? df.format(d) : "";
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		data = (List<UnitVO>) o;
	}
}