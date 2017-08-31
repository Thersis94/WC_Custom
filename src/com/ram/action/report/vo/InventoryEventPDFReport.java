package com.ram.action.report.vo;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ram.datafeed.data.InventoryEventVO;
import com.ram.datafeed.data.InventoryItemVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: InventoryEventPDFReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> manages the 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Aug 30, 2017
 * @updates:
 ****************************************************************************/
public class InventoryEventPDFReport  extends AbstractPDFReport {

	private static final long serialVersionUID = -4806446733921199847L;
	InventoryEventVO data = null;

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generating report");
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		final Document document = new Document();
		try {
			PdfWriter.getInstance(document, byteStream);
			document.setPageSize(new Rectangle(792, 612));
			document.addTitle(StringUtil.checkVal(data.getInventoryEventId()));
			document.setHtmlStyleClass("@page land {size: landscape;}");
			document.open();

			PdfPTable table = new PdfPTable(5);
			table.setWidthPercentage(100f);
			table.setWidths(new float[] { 2,2,2 ,1,1});
			table.setHeaderRows(1);
			table.setFooterRows(0);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
			
			generateTopTable(table);
			generateProductSection(table);

			document.add(table);
			document.newPage();
			document.close();

			return byteStream.toByteArray();

		} catch (DocumentException e) {
			log.error("error while building case table  " , e);
		}

		return new byte[0];
	}

	/**
	 * controls product section of the pdf
	 * @param table
	 */
	private void generateProductSection(PdfPTable table) {
		generateSectionHeader(table);
		generateSectionBody(table);
	}

	/**
	 * returns the body of the product section
	 * @param table
	 */
	private void generateSectionBody(PdfPTable table) {
		for(InventoryItemVO item : data.getInventoryItems()){
			table.addCell(getTableCell(StringUtil.checkVal(item.getProductNm())));
			table.addCell(getTableCell(StringUtil.checkVal(item.getManufacturer())));
			table.addCell(getTableCell(StringUtil.checkVal(item.getCustomerProductId())));
			table.addCell(getTableCell(StringUtil.checkVal(item.getQuantity())));
			table.addCell(getFlagCell(item.getKitFlag()));
		}
	}

	/**
	 * returns the header of the product section
	 * @param table
	 */
	private void generateSectionHeader(PdfPTable table) {
		//the product section has the most columns so it controls the number of cols in the table
		table.addCell(getTableCell("Product Name", true, false));
		table.addCell(getTableCell("Manufacturer", true, false));
		table.addCell(getTableCell("SKU", true, false));
		table.addCell(getTableCell("Qty", true, false));
		table.addCell(getTableCell("Kit", true, false));
	}

	/**
	 * @param table
	 */
	private void generateTopTable(PdfPTable table) {
		getLogoRow(table);
		//top row of the 
		table.addCell(getCaseInfoTop());
		//case info cells bordered on right or left depending on location
		eventInfoLeft(table, "Event Location: " , StringUtil.checkVal(data.getLocationName()));
		eventInfoRight(table, "Location Address: " , StringUtil.checkVal(data.getLocation()));

		eventInfoLeft(table, "Scheduled Date: " , Convert.formatDate(data.getScheduleDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		eventInfoRight(table, "Completed Date: " , Convert.formatDate(data.getInventoryCompleteDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		
		eventInfoLeft(table, "Data Loaded Date: " , Convert.formatDate(data.getDataLoadCompleteDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		eventInfoRight(table, "" ,"");

		
		eventInfoLeft(table, "Total Item Count: " , StringUtil.checkVal(data.getNumberTotalProducts()));
		eventInfoRight(table, "Unique SKU Count: " , StringUtil.checkVal(data.getInventoryItems().size()));

		eventInfoLeft(table, "Received Item Count: " , StringUtil.checkVal(data.getNumberReceivedProducts()));
		eventInfoRight(table, "Returned Item Count: " , StringUtil.checkVal(data.getNumberReturnedProducts()));

		table.addCell(getSectionSpacer(10));
	}
	
	/**
	 * left half of the middle case section
	 * @param string 
	 * @param table 
	 * @return
	 */
	private void eventInfoLeft(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 1));

		table.addCell(getDataStyleCell(cellData, 1));
	}

	/**
	 * right half of the middle case section
	 * @return
	 */
	private void eventInfoRight(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 1));

		table.addCell(getDataStyleCell(cellData, 2));
	}
	
	/**
	 * top of the case section
	 * @return
	 */
	private PdfPCell getCaseInfoTop() {
		StringBuilder sb = new StringBuilder(100);
		sb.append("Inventory Event ID: ").append(StringUtil.checkVal(data.getInventoryEventId()));
		sb.append(" (Report Created Date: ").append(StringUtil.checkVal(Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN )));
		sb.append(")");
		return(getTitleCell(sb.toString(), 12));
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		if (o instanceof InventoryEventVO) {
			data = (InventoryEventVO) o;
		}
	}

}
