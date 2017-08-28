package com.ram.action.report.vo;

import java.io.ByteArrayOutputStream;
import java.util.Map.Entry;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ram.workflow.data.vo.order.OrderLineItemVO;
import com.ram.workflow.data.vo.order.OrderVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProductOrderReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Creates a pdf of the current order
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Aug 24, 2017
 * @updates:
 ****************************************************************************/
public class ProductOrderReport  extends AbstractPDFReport {

	private static final long serialVersionUID = 1276776536317491393L;

	OrderVO data = null;
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug(" generating report");
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		final Document document = new Document();
		try {
			PdfWriter.getInstance(document, byteStream);
			document.setPageSize(new Rectangle(792, 612));
			document.addTitle(data.getOrderId());
			document.setHtmlStyleClass("@page land {size: landscape;}");
			document.open();

			PdfPTable table = new PdfPTable(7);
			table.setWidthPercentage(100f);
			table.setWidths(new float[] { 5,4,2,2, 2,2,3, });
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
	 * @param table
	 */
	private void generateSectionBody(PdfPTable table) {
		for(Entry<String, OrderLineItemVO> entry : data.getLineItems().entrySet()){
			OrderLineItemVO olv = entry.getValue();
			table.addCell(getTableCell(StringUtil.checkVal(olv.getProductNm())));
			table.addCell(getTableCell(StringUtil.checkVal(olv.getCustomerName())));
			table.addCell(getTableCell(StringUtil.checkVal(olv.getCustProductId())));
			table.addCell(getTableCell(StringUtil.checkVal(olv.getGtinNumber())));
			
			table.addCell(getTableCell(StringUtil.checkVal(olv.getQtyNo()), false, true));
			table.addCell(getTableCell(StringUtil.checkVal(olv.getRecQtyNo()), false, true));
			table.addCell(getTableCell(Convert.formatDate(olv.getFulfilledDt(), Convert.DATE_SLASH_PATTERN)));
		}
	}

	/**
	 * @param table
	 */
	private void generateSectionHeader(PdfPTable table) {
		//the product section has the most columns so it controls the number of cols in the table
		table.addCell(getTableCell("Product Name", true, false));
		table.addCell(getTableCell("Manufacturer", true, false));
		table.addCell(getTableCell("SKU", true, false));
		table.addCell(getTableCell("GTIN", true, false));
		
		table.addCell(getTableCell("Qty Ordered", true, false));
		table.addCell(getTableCell("Qty Recived", true, false));
		table.addCell(getTableCell("Fulfillment Date", true, false));
	}

	/**
	 * @param table
	 */
	private void generateTopTable(PdfPTable table) {
		getLogoRow(table);
		table.addCell(getOrderInfoTop());

		//case info cells bordered on right or left depending on location
		orderInfoLeft(table, "Location Name: " , StringUtil.checkVal(data.getLocationName()));
		orderInfoRight(table, "" , "");

		orderInfoLeft(table, "Order Date: " , Convert.formatDate(data.getCreateDt(), Convert.DATE_SLASH_PATTERN));
		orderInfoRight(table, "Ordered By: ", StringUtil.checkVal(data.getFullName()));
		
		orderInfoLeft(table, "Qty Ordered: ", StringUtil.checkVal(data.getNumberItemsOrdered()));
		orderInfoRight(table, "Qty Recived: ", StringUtil.checkVal(data.getNumberItemsReceived()));
		
		orderInfoLeft(table, "Status:  ", StringUtil.checkVal(data.getOrderStatusName()));
		orderInfoRight(table, "Order Type:  ", (data.getUserRoleId()==0)? "System":"Manual" );
		
		table.addCell(getSectionSpacer(10));
	}
	
	/**
	 * top of the case section
	 * @return
	 */
	private PdfPCell getOrderInfoTop() {
		String title;
		
		if (data.getUserRoleId()==0) {
			title = "System Generated Order";
		}else{
			title = "Order Id: " + data.getOrderId() + " by " + data.getFullName();
		}
		
		return(getTitleCell(title, 7));
	}

	/**
	 * left half of a row in the order info section
	 * @param string 
	 * @param table 
	 * @return
	 */
	private void orderInfoLeft(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 1));

		table.addCell(getDataStyleCell(cellData, 1));
	}

	/**
	 * right half of a row in the order info section
	 * @return
	 */
	private void orderInfoRight(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 2));

		table.addCell(getDataStyleCell(cellData, 3));
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object localData) {
		if (localData instanceof OrderVO) {
			data = (OrderVO) localData;
		}
	}

}
