package com.biomed.smarttrak.fd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.biomed.smarttrak.fd.FinancialDashVO.CountryType;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: FinancialDashImportReportVO.java<p/>
 * <b>Description: Report vo for creating the report that is used in the data import.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 20, 2018
 ****************************************************************************/

public class FinancialDashImportReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    
    private List<List<FinancialDashRevenueDataRowVO>> revenueData;
    private HSSFWorkbook wb;
    private Sheet sheet;
    private int rowCount;

    public FinancialDashImportReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Financial-Dashboard-Editable-Export.xls");
    }
    
    @SuppressWarnings("unchecked")
	@Override
	public void setData(Object data) {
    	revenueData = (List<List<FinancialDashRevenueDataRowVO>>) data;
    }

	@Override
	public byte[] generateReport() {
		wb = new HSSFWorkbook();
		sheet = wb.createSheet();

		populateHeader(sheet);
		populateData(sheet);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException ioe) {
			log.error("could not write output stream", ioe);
		} finally {
			try { 
				wb.close(); 
			} catch (Exception e) {
				log.error("could not close ", e );
			}
		}

		return new byte[0];
	}

	/**
	 * Create the header row.
	 * @param sheet2
	 */
	private void populateHeader(Sheet sheet2) {
		Row row = sheet.createRow(rowCount++);
		int cellCount = 0;
		row.createCell(cellCount++).setCellValue("Revenue Id");
		row.createCell(cellCount++).setCellValue("Scenario Id");
		row.createCell(cellCount++).setCellValue("Overlay Id");
		row.createCell(cellCount++).setCellValue("Market Section");
		row.createCell(cellCount++).setCellValue("Company Id");
		row.createCell(cellCount++).setCellValue("Company Name");
		row.createCell(cellCount++).setCellValue("Revenue Year");
		row.createCell(cellCount++).setCellValue("Region");
		row.createCell(cellCount++).setCellValue("Q1 Earnings");
		row.createCell(cellCount++).setCellValue("Q2 Earnings");
		row.createCell(cellCount++).setCellValue("Q3 Earnings");
		row.createCell(cellCount++).setCellValue("Q4 Earnings");
	}

	/**
	 * Populate the rest of the report with the supplied data.
	 * @param sheet
	 */
	private void populateData(Sheet sheet) {
		for (List<FinancialDashRevenueDataRowVO> dataGroup : revenueData) {
			for (FinancialDashRevenueDataRowVO dataRow : dataGroup) {
				Row row = sheet.createRow(rowCount++);
				int cellCount = 0;
				Cell cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getRevenueId());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getScenarioId());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getOverlayId());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getSectionName());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getCompanyId());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getCompanyName());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getYearNo());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getRegionCode().toString());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ1No());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ2No());
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ3No());
				cell = row.createCell(cellCount);
				cell.setCellValue(dataRow.getQ4No());
			}
			int cellNo = 7;
			char column = 'I';
			Row row = sheet.createRow(rowCount++);
			Cell cell = row.createCell(cellNo++);
			cell.setCellValue(CountryType.WW.toString());
			cell = row.createCell(cellNo++);
			cell.setCellType(Cell.CELL_TYPE_FORMULA);
			cell.setCellFormula("SUM("+column+(rowCount-dataGroup.size())+":"+ column++ +(rowCount-1)+")");
			cell = row.createCell(cellNo++);
			cell.setCellType(Cell.CELL_TYPE_FORMULA);
			cell.setCellFormula("SUM("+column+(rowCount-dataGroup.size())+":"+ column++ +(rowCount-1)+")");
			cell = row.createCell(cellNo++);
			cell.setCellType(Cell.CELL_TYPE_FORMULA);
			cell.setCellFormula("SUM("+column+(rowCount-dataGroup.size())+":"+ column++ +(rowCount-1)+")");
			cell = row.createCell(cellNo);
			cell.setCellType(Cell.CELL_TYPE_FORMULA);
			cell.setCellFormula("SUM("+column+(rowCount-dataGroup.size())+":"+ column++ +(rowCount-1)+")");
		}
	}

}
