package com.ram.action.util;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Map.Entry;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ram.action.report.vo.AbstractPDFReport;
import com.ram.datafeed.data.KitLayerProductVO;
import com.ram.datafeed.data.KitLayerVO;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: KitBOMPdfReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Class is responsible for generating the Bill of Materials
 * report for Kits. 
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Oct 9, 2015
 * @updates:
 * 		changed from HTML conversion to native PDF and changed formatting to match other
 * 			PDFs.   Rjr 8/22/2017
 ****************************************************************************/
public class KitBOMPdfReport extends AbstractPDFReport {

	private static final long serialVersionUID = 1L;

	private RAMProductVO data = null;

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
			document.addTitle(data.getProductName());
			document.setHtmlStyleClass("@page land {size: landscape;}");
			document.open();

			PdfPTable table = new PdfPTable(6);
			table.setWidthPercentage(100f);
			table.setWidths(new float[] {  3,5 ,2,3,1,2 });
			table.setHeaderRows(1);
			table.setFooterRows(0);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

			bomInfoSection(table);
			generateLayerSections(table);

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
	 * @param table
	 */
	private void generateLayerSections(PdfPTable table) {
		int index = 1;
		for (KitLayerVO layer :data.getKitLayers()){
			table.addCell(this.getHeadingStyleCell("Kit Layer #" + index++, 6));
			generateSectionHeader(table);
			generateSectionBody(table, layer);
			table.addCell(getSectionSpacer(6));
		}
	}

	/**
	 * @param table
	 * @param layer
	 */
	private void generateSectionBody(PdfPTable table, KitLayerVO layer) {
		for(Entry<Integer, KitLayerProductVO> item : layer.getProducts().entrySet()){
			String productGTIN = "";
			if (item.getValue().getProduct().getGtinProductId() != null && !item.getValue().getProduct().getGtinProductId().isEmpty()){
				productGTIN = item.getValue().getProduct().getGtinProductNumber();
			}

			table.addCell(getTableCell(item.getValue().getProduct().getCustomerName()));
			table.addCell(getTableCell(item.getValue().getProduct().getProductName()));
			table.addCell(getTableCell(StringUtil.checkVal(item.getValue().getProduct().getCustomerProductId())));
			table.addCell(getTableCell(productGTIN));
			table.addCell(getTableCell(StringUtil.checkVal(item.getValue().getProduct().getQuantity())));
			table.addCell(getBarcodeCell(item.getValue().getProduct()));
		}
	}

	/**
	 * returns the header of the product section
	 * @param table
	 */
	private void generateSectionHeader(PdfPTable table) {
		//the product section has the most columns so it controls the number of cols in the table
		table.addCell(getTableCell("Manufacturer", true));
		table.addCell(getTableCell("Product Name", true));
		table.addCell(getTableCell("SKU", true));
		table.addCell(getTableCell("GTIN", true));
		table.addCell(getTableCell("Qty.", true));
		table.addCell(getTableCell("Barcode", true));
	}

	/**
	 * generates one cell of table data 
	 * @param string
	 * @return
	 */
	private PdfPCell getTableCell(String cellContent, boolean isHeader) {
		if (isHeader) {
			return getHeadingStyleCell(cellContent, 1, 9);
		} else {
			return getDataStyleCell(cellContent, 1, 9);
		}
	}


	/**
	 * generates one cell of table data 
	 * @param string
	 * @return
	 */
	private PdfPCell getTableCell(String cellContent) {
		return getTableCell(cellContent, false);
	}

	/**
	 * info about the entire case/kit
	 * @param table
	 */
	private void bomInfoSection(PdfPTable table) {
		getLogoRow(table);
		table.addCell(getTitleCell(data.getProductName(), 6));

		//case info cells bordered on right or left depending on location
		bomInfoLeft(table, "Kit SKU: " , StringUtil.checkVal(data.getCustomerProductId()));
		bomInfoRight(table, "Kit GTIN: " , StringUtil.checkVal(data.getGtinProductId()));
		bomInfoLeft(table, "Kit Manufacturer: " , StringUtil.checkVal(data.getCustomerName()));
		bomInfoRight(table, "# Layers: " , StringUtil.checkVal(data.getKitLayers().size()));
		bomInfoLeft(table, "Inventory Date: " , Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN));
		table.addCell(getLabelCell("Kit Barcode: ", 2));
		table.addCell(getBarcodeCell(data, 2));
		table.addCell(getSectionSpacer(6));
	}

	/**
	 * left half of the bom info section
	 * @param string 
	 * @param table 
	 * @return
	 */
	private void bomInfoLeft(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 1));
		table.addCell(getDataStyleCell(cellData, 1));
	}

	/**
	 * right half of the bom info section
	 * @return
	 */
	private void bomInfoRight(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 2));
		table.addCell(getDataStyleCell(cellData, 2));
	}

	@Override
	public void setData(Object o) {
		if (o instanceof RAMProductVO) {
			data = (RAMProductVO) o;
		}
	}

}