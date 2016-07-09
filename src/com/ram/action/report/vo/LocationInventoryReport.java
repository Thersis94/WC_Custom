/**
 *
 */
package com.ram.action.report.vo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.ram.workflow.data.vo.LocationItemMasterVO;
import com.siliconmtn.data.report.AbstractReport;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: LocationInventoryReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Class manages generating the Location Inventory Report
 * based on Location Item Master Records.
 * <b>Copyright:</b> Copyright (c) 2016
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jul 8, 2016
 ****************************************************************************/
public class LocationInventoryReport extends AbstractReport {

	private static final long serialVersionUID = 1L;
	private String customerLocationId = null;
	private String locationName = null;
	private List<LocationItemMasterVO> data;
	
	public LocationInventoryReport(String customerLocationId, String locationName) {
		this.customerLocationId = customerLocationId;
		this.locationName = locationName;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();
		Row r = null;
		Cell c= null;
		wb.setSheetName(0, locationName);
		int rowNum = 0;

		//Build the Header Cell Style
		CellStyle headerStyle = wb.createCellStyle();
		headerStyle.setWrapText(true);

		//Add Location Name
		r = s.createRow(rowNum++);
		c = r.createCell(0);
		c.setCellValue("Location");
		c.setCellStyle(headerStyle);
		c = r.createCell(1);
		c.setCellValue(locationName);

		//Add Inventory Date
		r = s.createRow(rowNum++);
		c = r.createCell(0);
		c.setCellValue("Date");
		c.setCellStyle(headerStyle);
		c = r.createCell(1);
		c.setCellValue(Convert.formatDate(new Date()));

		//Add Spacer
		r = s.createRow(rowNum++);
		CellStyle style = wb.createCellStyle();
		style.setFillBackgroundColor(IndexedColors.BLACK.getIndex());
		style.setFillPattern(CellStyle.BIG_SPOTS);
		r.setRowStyle(style);

		//Build Location Item Headers.
		r = s.createRow(rowNum++);
		r.setRowStyle(headerStyle);

		c = r.createCell(0);
		c.setCellValue("OEM");
		c.setCellStyle(headerStyle);

		c = r.createCell(1);
		c.setCellValue("Product Id");
		c.setCellStyle(headerStyle);

		c = r.createCell(2);
		c.setCellValue("Product Name");
		c.setCellStyle(headerStyle);

		c = r.createCell(3);
		c.setCellValue("Last Scan Date");
		c.setCellStyle(headerStyle);

		c = r.createCell(4);
		c.setCellValue("OnHand Qty");
		c.setCellStyle(headerStyle);

		c = r.createCell(5);
		c.setCellValue("Par Level");
		c.setCellStyle(headerStyle);

		c = r.createCell(6);
		c.setCellValue("OnOrder Qty");
		c.setCellStyle(headerStyle);

		//Iterate LocationItemMaster and build inv Rows.
		for(LocationItemMasterVO l : data) {
			int celNum = 0;
			r = s.createRow(rowNum++);
			c = r.createCell(celNum++);
			c.setCellValue(l.getCustomerNm());
			c = r.createCell(celNum++);
			c.setCellValue(l.getCustProductId());
			c = r.createCell(celNum++);
			c.setCellValue(l.getProductNm());
			c = r.createCell(celNum++);
			if(l.getUpdateDt() != null) {
				c.setCellValue(Convert.formatDate(l.getUpdateDt()));
			} else {
				c.setCellValue(Convert.formatDate(l.getCreateDt()));
			}
			c = r.createCell(celNum++);
			c.setCellValue(l.getQtyOnHand());
			c = r.createCell(celNum++);
			c.setCellValue(l.getParValueNo());
			c = r.createCell(celNum++);
			c.setCellValue(l.getQtyOnOrder());
		}

		//Auto-size the columns.
		for(int i = 0; i < 7; i++) {
			s.autoSizeColumn(i);
		}

		//Convert WorkBook to byte []
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
		    wb.write(bos);
			wb.close();
		} catch (IOException e) {
			log.error(e);
		}

		//return Excel file as byte []
		return bos.toByteArray();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		data = (List<LocationItemMasterVO>)o;
	}

	/**
	 * @return the customerLocationId
	 */
	public String getCustomerLocationId() {
		return customerLocationId;
	}

	/**
	 * @return the locationName
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * @param customerLocationId the customerLocationId to set.
	 */
	public void setCustomerLocationId(String customerLocationId) {
		this.customerLocationId = customerLocationId;
	}

	/**
	 * @param locationName the locationName to set.
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	
}