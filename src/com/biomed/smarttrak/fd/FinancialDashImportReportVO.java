package com.biomed.smarttrak.fd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

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
    
    public static final int DATA_START_NO = 7;
    public static final int SCENARIO_ID_COL = 0;
    public static final int COMPANY_ID_COL = 4;
    public static final int REGION_COL = 6;
    public static final String COL_SUFFIX = " Earnings";
    
    private List<Map<Integer, List<FinancialDashRevenueDataRowVO>>> revenueData;
    private int maxYear;
    private int minYear;
    private Sheet sheet;
    private int rowCount;
    private HSSFCellStyle locked;
    private HSSFCellStyle unlocked;

    public FinancialDashImportReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Financial-Dashboard-Editable-Export.xls");
    }
    
    @SuppressWarnings("unchecked")
	@Override
	public void setData(Object data) {
    	Map<String, Object> dataMap = (Map<String, Object>)data;
    	revenueData = (List<Map<Integer, List<FinancialDashRevenueDataRowVO>>>) dataMap.get("data");
    	minYear = (int) dataMap.get("minYear");
    	maxYear = (int) dataMap.get("maxYear");
    }

	@Override
	public byte[] generateReport() {
		HSSFWorkbook wb = new HSSFWorkbook();
		sheet = wb.createSheet();
		sheet.protectSheet(""); 
		locked = wb.createCellStyle();
		locked.setLocked(true);
		unlocked = wb.createCellStyle();
		unlocked.setLocked(false);

		populateHeader();
		populateData();

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
	private void populateHeader() {
		Row row = sheet.createRow(rowCount++);
		int cellCount = 0;
		sheet.setColumnWidth(cellCount, 0);
		row.createCell(cellCount++).setCellValue("Scenario Id");
		sheet.setColumnWidth(cellCount, 7500);
		row.createCell(cellCount++).setCellValue("Scenario Title");
		sheet.setColumnWidth(cellCount, 7500);
		row.createCell(cellCount++).setCellValue("Parent Section");
		sheet.setColumnWidth(cellCount, 7500);
		row.createCell(cellCount++).setCellValue("Market Section");
		sheet.setColumnWidth(cellCount, 0);
		row.createCell(cellCount++).setCellValue("Company Id");
		sheet.setColumnWidth(cellCount, 10000);
		row.createCell(cellCount++).setCellValue("Company Name");
		row.createCell(cellCount++).setCellValue("Region");
		for (int i = minYear; i <= maxYear; i++) {
			sheet.setColumnWidth(cellCount, 0);
			row.createCell(cellCount++).setCellValue(i + "Revenue Id");
			sheet.setColumnWidth(cellCount, 0);
			row.createCell(cellCount++).setCellValue("Year No");
			sheet.setColumnWidth(cellCount, 5000);
			row.createCell(cellCount++).setCellValue("Q1 " + i + COL_SUFFIX);
			sheet.setColumnWidth(cellCount, 5000);
			row.createCell(cellCount++).setCellValue("Q2 " + i + COL_SUFFIX);
			sheet.setColumnWidth(cellCount, 5000);
			row.createCell(cellCount++).setCellValue("Q3 " + i + COL_SUFFIX);
			sheet.setColumnWidth(cellCount, 5000);
			row.createCell(cellCount++).setCellValue("Q4 " + i + COL_SUFFIX);
		}
	}

	/**
	 * Populate the rest of the report with the supplied data.
	 * @param sheet
	 */
	private void populateData() {
		for (Map<Integer, List<FinancialDashRevenueDataRowVO>> dataGroup : revenueData) {
			int cellCount = DATA_START_NO;
			Row row = sheet.createRow(rowCount++);
			boolean writeStarter = true;
			for (int i = minYear; i <= maxYear; i++) {
				cellCount = createRow(row, i, writeStarter, cellCount, dataGroup);
			}
		}
	}
	
	
	/**
	 * Write the next part of the row based on the supplied data
	 * @param row
	 * @param i
	 * @param writeStarter
	 * @param cellCount
	 * @param dataGroup
	 */
	private int createRow(Row row, int i, boolean writeStarter, int cellCount, Map<Integer, List<FinancialDashRevenueDataRowVO>> dataGroup) {
		if (!dataGroup.containsKey(i)) {
			cellCount += 7;
		} else {
			for (FinancialDashRevenueDataRowVO dataRow : dataGroup.get(i)) {
				Cell cell = null;
				if (writeStarter) {
					writeStarter = false;
					int starterCells = 0;
					cell = row.createCell(starterCells++);
					cell.setCellValue(dataRow.getScenarioId());
					cell.setCellStyle(locked);
					cell = row.createCell(starterCells++);
					cell.setCellValue(dataRow.getScenarioName());
					cell.setCellStyle(locked);
					cell = row.createCell(starterCells++);
					cell.setCellValue(dataRow.getParentName());
					cell.setCellStyle(locked);
					cell = row.createCell(starterCells++);
					cell.setCellValue(dataRow.getSectionName());
					cell.setCellStyle(locked);
					cell = row.createCell(starterCells++);
					cell.setCellValue(dataRow.getCompanyId());
					cell.setCellStyle(locked);
					cell = row.createCell(starterCells++);
					cell.setCellValue(dataRow.getCompanyName());
					cell.setCellStyle(locked);
					cell = row.createCell(starterCells);
					cell.setCellValue(dataRow.getRegionCode());
					cell.setCellStyle(locked);
				}
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getRevenueId());
				cell.setCellStyle(locked);
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getYearNo());
				cell.setCellStyle(locked);
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ1No());
				cell.setCellStyle(unlocked);
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ2No());
				cell.setCellStyle(unlocked);
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ3No());
				cell.setCellStyle(unlocked);
				cell = row.createCell(cellCount++);
				cell.setCellValue(dataRow.getQ4No());
				cell.setCellStyle(unlocked);
			}
		}
		return cellCount;
	}

}
