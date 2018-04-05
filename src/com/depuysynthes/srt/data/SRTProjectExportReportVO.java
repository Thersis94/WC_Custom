package com.depuysynthes.srt.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title:</b> SRTProjectExportReportVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> ReportVO for converting a map of Project Data into
 * an Excel Report.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 4, 2018
 ****************************************************************************/
public class SRTProjectExportReportVO extends AbstractSBReportVO {

	private Map<String, SRTProjectVO> projects;
	private int rowNo = 0;

	//Holds Column Name and Index for easier refactoring in future.
	private enum HeaderEnum {PROJECT_ID(0, "projectId");

		private String colName;
		private int index;
		private HeaderEnum(int index, String colName) {
			this.index = index;
			this.colName = colName;
		}

		/**
		 * @return
		 */
		public String colName() {
			return colName;
		}

		/**
		 * @return
		 */
		public int getIndex() {
			return index;
		}}
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public SRTProjectExportReportVO() {
		this("Search Results Export.xlsx");
	}

	public SRTProjectExportReportVO(String fileName) {
		this.fileName = fileName;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		//Build Excel File
		try(Workbook wb = new HSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Projects");
	
			//Build Header Rows
			buildHeadersRow(sheet);


			/**
			 * Build Report with Project Data.  Otherwise print line
			 * for No Results Found.
			 */
			if (projects != null) {
				buildReport(sheet);
			} else {
				Row row = sheet.createRow(rowNo);
				row.createCell(0).setCellValue("No Results Found");
			}
	
			//Return serialized byte [] of Workbook
			return serializeWorkbook(wb);
		} catch (IOException wbe) {
			log.error("Error Closing Workbook", wbe);
		}

		return new byte[0];
	}


	/**
	 * Build Header Row of Excel Report.
	 * @param sheet
	 */
	private void buildHeadersRow(Sheet sheet) {
		Row row = sheet.createRow(rowNo++);

		//Loop Headers and set cell values.
		for(HeaderEnum h : HeaderEnum.values()) {
			row.createCell(h.getIndex()).setCellValue(h.colName());
		}
	}

	/**
	 * @param wb
	 * @return
	 */
	private byte[] serializeWorkbook(Workbook wb) {
		//Write xls to ByteStream and return.
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			log.error(e);
			return new byte[0];
		}
	}

	/**
	 * Manages Iterating Project Data and Building Rows on given Sheet.
	 * @param sheet
	 */
	private void buildReport(Sheet sheet) {
		for(SRTProjectVO p : projects.values()) {
			processProject(p, sheet);
		}
	}

	/**
	 * @param i
	 * @param p
	 * @param sheet
	 */
	private void processProject(SRTProjectVO p, Sheet sheet) {
		if(!p.getMasterRecords().isEmpty()) {
			for(SRTMasterRecordVO mr : p.getMasterRecords()) {
				buildProjectDataRow(sheet, p, mr);
			}
		} else {
			buildProjectDataRow(sheet, p, null);
		}
		
	}

	/**
	 * Builds Actual Row in Excel Sheet.
	 * @param sheet
	 * @param p
	 * @param mr
	 */
	private void buildProjectDataRow(Sheet sheet, SRTProjectVO p, SRTMasterRecordVO mr) {
		Row row = sheet.createRow(rowNo++);
		buildProjectCells(row, p);
		buildRequestCells(row, p.getRequest());
		if(mr != null) {
			buildMasterRecordCells(row, mr);
		}

		buildMilestoneCells(row, p.getMilestones());
	}

	/**
	 * Builds Cells related to Project Data
	 * @param row
	 * @param p
	 */
	private void buildProjectCells(Row row, SRTProjectVO p) {
		row.createCell(HeaderEnum.PROJECT_ID.getIndex()).setCellValue(p.getProjectId());
	}

	/**
	 * Builds Cells related to Request Data
	 * @param row
	 * @param request
	 */
	private void buildRequestCells(Row row, SRTRequestVO request) {
		//TODO Build Row Cells for Request Data
	}

	/**
	 * Builds Cells related to Master Record Data
	 * @param row
	 * @param mr
	 */
	private void buildMasterRecordCells(Row row, SRTMasterRecordVO mr) {
		//TODO Build Row Cells for Master Record Data
	}

	/**
	 * Builds Cells related to Project Milestone Data
	 * @param row
	 */
	private void buildMilestoneCells(Row row, Map<String, SRTProjectMilestoneVO> milestones) {
		//TODO Build Row Cells for Milestone Data
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if(o instanceof Map)
			projects = (Map<String, SRTProjectVO>)o;
	}

}
