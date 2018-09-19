package com.depuysynthes.srt.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.depuysynthes.srt.vo.ProjectExportReportVO;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.siliconmtn.util.Convert;
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
	private Map<Integer, String> headers;
	private int rowNo = 0;
	private int colNo = 0;
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public SRTProjectExportReportVO() {
		this("Search Results Export.xls");
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
		try(Workbook wb = getWorkbook()) {
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
	 * Helper method returns proper Workbook Type.
	 * @return
	 */
	private Workbook getWorkbook() {
		if(this.fileName.endsWith(".xlsx")) {
			log.debug("Generating xlsx Workbook.");
			return new XSSFWorkbook();
		} else {
			log.debug("Generating xls Workbook.");
			return new HSSFWorkbook();
		}
	}

	/**
	 * Build Header XSSFRow of Excel Report.
	 * @param sheet
	 */
	private void buildHeadersRow(Sheet sheet) {
		Row row = sheet.createRow(rowNo++);

		//Loop Headers and set cell values.
		for(Entry<Integer, String> h : headers.entrySet()) {
			row.createCell(h.getKey()).setCellValue(h.getValue());
		}
	}

	/**
	 * Convert given Workbook to Byte [] for Streaming.
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
	 * Writes rows of data into the given sheet based on data on the
	 * SRTProjectVO Record.
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
	 * Builds Actual XSSFRow in Excel Sheet.
	 * @param sheet
	 * @param p
	 * @param mr
	 */
	private void buildProjectDataRow(Sheet sheet, SRTProjectVO p, SRTMasterRecordVO mr) {
		Row row = sheet.createRow(rowNo++);
		colNo = 0;
		buildProjectCells(row, p);
		buildRequestCells(row, p.getRequest());
		buildMasterRecordCells(row, mr);
		buildMilestoneCells(row, p.getMilestones());
	}

	/**
	 * Builds Cells related to Project Data
	 * @param row
	 * @param p
	 */
	private void buildProjectCells(Row row, SRTProjectVO p) {
		//row.createCell(colNo++).setCellValue(p.getProjectId());
		row.createCell(colNo++).setCellValue(p.getCoProjectId());
		row.createCell(colNo++).setCellValue(p.getOpCoId());
		row.createCell(colNo++).setCellValue(p.getProjectName());
		row.createCell(colNo++).setCellValue(p.getProjectType());
		row.createCell(colNo++).setCellValue(p.getPriority());
		row.createCell(colNo++).setCellValue(p.getHospitalPONo());
		row.createCell(colNo++).setCellValue(p.getSpecialInstructions());
		row.createCell(colNo++).setCellValue(p.getProjectStatus());
		row.createCell(colNo++).setCellValue(p.getActualRoiDbl());
		row.createCell(colNo++).setCellValue(p.getSrtContact());
		row.createCell(colNo++).setCellValue(p.getEngineerNm());
		row.createCell(colNo++).setCellValue(p.getSecondaryEngineerNm());
		row.createCell(colNo++).setCellValue(p.getDesignerNm());
		row.createCell(colNo++).setCellValue(p.getSecondaryDesignerNm());
		row.createCell(colNo++).setCellValue(p.getQualityEngineerNm());
		row.createCell(colNo++).setCellValue(p.getSecondaryQualityEngineerNm());
		row.createCell(colNo++).setCellValue(Boolean.toString(p.isMakeFromScratch()));
		row.createCell(colNo++).setCellValue(p.getFuncCheckOrderNo());
		row.createCell(colNo++).setCellValue(p.getMakeFromOrderNo());
		row.createCell(colNo++).setCellValue(p.getBuyerNm());
		row.createCell(colNo++).setCellValue(p.getSecondaryBuyerNm());
		row.createCell(colNo++).setCellValue(p.getMfgPOToVendor());
		row.createCell(colNo++).setCellValue(p.getSupplierId());
		row.createCell(colNo++).setCellValue(Boolean.toString(p.isProjectHold()));
		row.createCell(colNo++).setCellValue(Boolean.toString(p.isProjectCancelled()));
		row.createCell(colNo++).setCellValue(p.getWarehouseTrackingNo());
		row.createCell(colNo++).setCellValue(p.getMfgDtChangeReason());
		row.createCell(colNo++).setCellValue(p.getWarehouseSalesOrderNo());
	}

	/**
	 * Builds Cells related to Request Data
	 * @param row
	 * @param request
	 */
	private void buildRequestCells(Row row, SRTRequestVO r) {
		row.createCell(colNo++).setCellValue(r.getRequestId());
		row.createCell(colNo++).setCellValue(r.getHospitalName());
		row.createCell(colNo++).setCellValue(r.getSurgeonNm());
		row.createCell(colNo++).setCellValue(r.getDescription());
		row.createCell(colNo++).setCellValue(r.getReqTerritoryId());
		row.createCell(colNo++).setCellValue(r.getEstimatedRoiDbl());
		row.createCell(colNo++).setCellValue(r.getQtyNo());
		row.createCell(colNo++).setCellValue(r.getReason());
		row.createCell(colNo++).setCellValue(r.getReasonTxt());
		row.createCell(colNo++).setCellValue(r.getChargeTo());
		row.createCell(colNo++).setCellValue(r.getCreateDt());
		buildAddressCells(row, r.getRequestAddress());
	}

	/**
	 * Build Cells related to Request Address Data
	 * @param requestAddress
	 */
	private void buildAddressCells(Row row, SRTRequestAddressVO addr) {
		if(addr == null) {
			addr = new SRTRequestAddressVO();
		}
		row.createCell(colNo++).setCellValue(addr.getAddress());
		row.createCell(colNo++).setCellValue(addr.getAddress2());
		row.createCell(colNo++).setCellValue(addr.getCity());
		row.createCell(colNo++).setCellValue(addr.getState());
		row.createCell(colNo++).setCellValue(addr.getZipCode());
	}

	/**
	 * Builds Cells related to Master Record Data
	 * @param row
	 * @param mr
	 */
	private void buildMasterRecordCells(Row row, SRTMasterRecordVO mr) {
		row.createCell(colNo++).setCellValue(mr.getMasterRecordId());
		row.createCell(colNo++).setCellValue(mr.getPartNo());
		row.createCell(colNo++).setCellValue(mr.getTitleTxt());
		row.createCell(colNo++).setCellValue(mr.getQualitySystemId());
		row.createCell(colNo++).setCellValue(mr.getProdTypeId());
		row.createCell(colNo++).setCellValue(mr.getComplexityId());
		row.createCell(colNo++).setCellValue(mr.getProdCatId());
		row.createCell(colNo++).setCellValue(mr.getMakeFromPartNos());
		row.createCell(colNo++).setCellValue(mr.getProdFamilyId());
		row.createCell(colNo++).setCellValue(mr.getPartCount());
		row.createCell(colNo++).setCellValue(mr.getTotalBuilt());
		row.createCell(colNo++).setCellValue(Boolean.toString(mr.isObsolete()));
		row.createCell(colNo++).setCellValue(mr.getObsoleteReason());

		buildMasterRecordAttributes(row, mr.getAttributes());
	}

	/**
	 * Build Cells related to MasterRecord Attrbute Data
	 * @param row
	 * @param attributes
	 */
	private void buildMasterRecordAttributes(Row row, Map<String, String> attributes) {
		for(String attr: attributes.values()) {
			row.createCell(colNo++).setCellValue(attr);
		}
	}

	/**
	 * Builds Cells related to Project Milestone Data
	 * @param row
	 */
	private void buildMilestoneCells(Row row, Map<String, SRTProjectMilestoneVO> milestones) {
		for(SRTProjectMilestoneVO m : milestones.values()) {
			row.createCell(colNo++).setCellValue(Convert.formatDate(m.getMilestoneDt()));
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object reportData) {
		if(reportData instanceof ProjectExportReportVO) {
			ProjectExportReportVO reportVO = (ProjectExportReportVO) reportData;
			projects = reportVO.getProjects();
			headers = reportVO.getHeaders();
		}
	}
}