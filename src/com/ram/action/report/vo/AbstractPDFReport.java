package com.ram.action.report.vo;

import java.io.IOException;
import java.util.Date;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.html.WebColors;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.barcode.BarcodeImageWriter;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: AbstractPDFReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Abstract parent of all ram PDF reports.  
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Aug 22, 2017
 * @updates:
 ****************************************************************************/

public abstract class AbstractPDFReport  extends AbstractSBReportVO {

	private static final long serialVersionUID = 269024848534206007L;

	public static final String IMG_SRC ="/themes/CUSTOM/RAMGRP/MAIN/images/ramgrouplogo.png";
	public static final String CHECK_MARK_SRC = "/org/RAM/images/checkMark.png";

	public static final String HEADER_GREY = "#e7e7e7";
	public static final String TEXT_GREY = "#4e4e4e";
	public static final String BLACK = "black";
	public static final String WHITE = "white";


	/**
	 * formats cells for inner table boolean responses
	 * @param imageCell
	 * @return
	 */
	public PdfPCell flagCellFormatter(PdfPCell imageCell) {
		imageCell.setBorder(0);
		imageCell.setColspan(1);
		imageCell.setBorderWidthBottom(0.25f);
		imageCell.setPaddingTop(5);
		imageCell.setBackgroundColor(WebColors.getRGBColor(WHITE));
		imageCell.setPaddingBottom(10);
		imageCell.setPaddingLeft(10);
		return imageCell;
	}

	/**
	 * formats the barcode cell
	 * @param imageCell
	 * @return
	 */
	public PdfPCell barcodeCellFormater(PdfPCell imageCell) {
		imageCell.setBorder(0);
		imageCell.setColspan(1);
		imageCell.setBorderWidthBottom(0.25f);
		imageCell.setPaddingTop(5);
		imageCell.setBackgroundColor(WebColors.getRGBColor(WHITE));
		imageCell.setPaddingBottom(10);
		imageCell.setPaddingLeft(10);
		return imageCell;
	}

	/**
	 * formats a data cell with dynamic column span
	 * @param dataCell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell dataCellFormater(PdfPCell dataCell) {
		return dataCellFormater(dataCell,1);
	}

	/**
	 * formats a data cell with dynamic column span
	 * @param dataCell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell dataCellFormater(PdfPCell dataCell, int colSpan) {
		dataCell.setBorder(0);
		dataCell.setColspan(colSpan);
		dataCell.setBorderWidthBottom(0.25f);
		dataCell.setBackgroundColor(WebColors.getRGBColor(WHITE));
		dataCell.setPaddingBottom(10);
		dataCell.setPaddingLeft(10);
		return dataCell;
	}

	/**
	 * formats cells for empty space
	 * @param cell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell spacerCellFormater(PdfPCell cell){
		return spacerCellFormater(cell,1);
	}

	/**
	 * sets up empty space
	 * @param cell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell spacerCellFormater(PdfPCell cell, int colSpan){
		cell.setBorder(0);
		cell.setColspan(colSpan);
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);
		return cell;
	}

	/**
	 * formats a data cell with dynamic column span
	 * @param dataCell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell dataHeaderCellFormater(PdfPCell dataCell) {
		return dataHeaderCellFormater(dataCell, 1);
	}

	/**
	 * formats a data cell with dynamic column span
	 * @param dataCell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell dataHeaderCellFormater(PdfPCell dataCell, int colSpan) {
		dataCell.setBorder(0);
		dataCell.setColspan(colSpan);
		dataCell.setBorderWidthBottom(0.25f);
		dataCell.setBackgroundColor(WebColors.getRGBColor(HEADER_GREY));
		dataCell.setPaddingBottom(10);
		dataCell.setPaddingLeft(10);
		return dataCell;
	}

	/**
	 * formats a cell to have no borders
	 * @param cell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell noBorderCellFormater(PdfPCell cell) {
		return noBorderCellFormater(cell, 1);
	}

	/**
	 * formats a cell to have no borders
	 * @param cell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell noBorderCellFormater(PdfPCell cell, int colSpan) {
		cell.setBorder(0);
		cell.setColspan(colSpan);
		cell.setBackgroundColor(WebColors.getRGBColor(WHITE));
		cell.setPaddingLeft(10);
		return cell;
	}

	/**
	 * formats a label cell with dynamic column span
	 * @param labelCell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell lableCellFormater(PdfPCell labelCell) {
		return lableCellFormater(labelCell, 1);
	}

	/**
	 * formats a label cell with dynamic column span
	 * @param labelCell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell lableCellFormater(PdfPCell labelCell, int colSpan) {
		labelCell.setBorder(0);
		labelCell.setColspan(colSpan);
		labelCell.setBorderWidthBottom(0.25f);
		labelCell.setBackgroundColor(WebColors.getRGBColor(WHITE));
		labelCell.setPaddingBottom(10);
		labelCell.setPaddingLeft(10);
		return labelCell;
	}

	/**
	 * formats the cell for the logo area of the pdf
	 * @param pdfPCell
	 * @return
	 */
	public PdfPCell logoCellFormater(PdfPCell pdfPCell) {
		pdfPCell.setBorder(0);
		pdfPCell.setColspan(1);
		pdfPCell.setPaddingBottom(10);
		pdfPCell.setPaddingLeft(10);
		return pdfPCell;
	}


	/**
	 * formats the title bar for the pdf
	 * @param cell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell titleBarFormater(PdfPCell cell){
		return titleBarFormater(cell, 1);
	}

	/**
	 * formats the title bar for the pdf
	 * @param cell
	 * @param colSpan
	 * @return
	 */
	public PdfPCell titleBarFormater(PdfPCell cell, int colSpan){
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setColspan(colSpan);
		cell.setBorderWidthBottom(0.25f);
		cell.setBackgroundColor(WebColors.getRGBColor(HEADER_GREY));
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);
		return cell;

	}

	/**
	 * a row of empty cells between table areas
	 * @return
	 */
	public PdfPCell getSectionSpacer(int colSpan) {
		PdfPCell cell = new PdfPCell(new Paragraph("", dataFont()));
		cell.setBorder(0);
		cell.setColspan(colSpan);
		cell.setPaddingBottom(10);
		cell.setBackgroundColor(WebColors.getRGBColor(WHITE));
		cell.setPaddingLeft(10);
		return cell;
	}

	/**
	 * returns a times Roman font bold for titles and important headers 
	 * @return
	 */
	public Font titleFont() {
		return FontFactory.getFont(FontFactory.TIMES_BOLD, 11, WebColors.getRGBColor(BLACK));
	}

	/**
	 * returns a times Roman font bold for titles and important headers 
	 * @return
	 */
	public Font titleFont(int fontSize) {
		return FontFactory.getFont(FontFactory.TIMES_BOLD, fontSize, WebColors.getRGBColor(BLACK));
	}

	/**
	 * returns a font for very small text
	 * @return
	 */
	public Font smallFont() {
		return FontFactory.getFont(FontFactory.TIMES_ROMAN, 5, WebColors.getRGBColor(BLACK));
	}

	/**
	 * returns a times roman font for data
	 * @return
	 */
	public Font dataFont(int size) {
		return FontFactory.getFont(FontFactory.TIMES_ROMAN, size, WebColors.getRGBColor(TEXT_GREY));
	}
	/**
	 * returns a times roman font for data
	 * @return
	 */
	public Font dataFont() {
		return FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, WebColors.getRGBColor(TEXT_GREY));
	}

	/**
	 * returns a formated header cell
	 * @param cellContent
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getHeadingStyleCell( String cellContent, int colSpan){
		return dataHeaderCellFormater(new PdfPCell(new Paragraph(cellContent , dataFont())), colSpan);
	}

	/**
	 * returns a formatted header cell with a custom font size
	 * @param cellContent
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getHeadingStyleCell( String cellContent, int colSpan, int fontSize){
		return dataHeaderCellFormater(new PdfPCell(new Paragraph(cellContent , dataFont(fontSize))), colSpan);
	}

	/**
	 * returns a formatted data cell 
	 * @param cellContent
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getDataStyleCell(String cellContent, int colSpan){
		return dataCellFormater(new PdfPCell(new Paragraph(cellContent , dataFont())), colSpan);
	}

	/**
	 * returns a formatted data cell with a custom font size
	 * @param cellContent
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getDataStyleCell(String cellContent, int colSpan, int fontSize){
		return dataCellFormater(new PdfPCell(new Paragraph(cellContent , dataFont(fontSize))), colSpan);
	}
	/**
	 * returns a styled label cell
	 * @param cellContent
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getLabelCell(String cellContent, int colSpan){
		return getLabelCell(cellContent, colSpan, 11);
	}

	/**
	 * returns a styled label cell with custom font size
	 * @param cellContent
	 * @param colSpan
	 * @param fontSize
	 * @return
	 */
	public PdfPCell getLabelCell(String cellContent, int colSpan, int fontSize) {
		return lableCellFormater(new PdfPCell(new Paragraph(cellContent, titleFont(fontSize))), colSpan);
	}

	/**
	 * returns a styled title cell
	 * @param cellContent
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getTitleCell(String cellContent, int colSpan) {
		return getTitleCell(cellContent, colSpan, 11);
	}

	/**
	 * returns a styled title cell with custom font size
	 * @param cellContent
	 * @param colSpan
	 * @param fontSize
	 * @return
	 */
	public PdfPCell getTitleCell(String cellContent, int colSpan, int fontSize) {
		return titleBarFormater(new PdfPCell(new Paragraph(cellContent, titleFont(fontSize))),colSpan);
	}

	/**
	 * adds the standard logo and white space to the pdf
	 * @param table
	 */
	public void getLogoRow(PdfPTable table){
		//logo and empty space
		table.addCell(createLogoCell());
		table.addCell(getSectionSpacer(9));
	}

	/**
	 * cell containing the logo of the company
	 * @return
	 */
	private PdfPCell createLogoCell() {
		try {			
			String imageUrl = attributes.get(Constants.PATH_TO_BINARY)+ IMG_SRC;
			Image image = Image.getInstance( imageUrl );

			return logoCellFormater(new PdfPCell(image, true));

		} catch (IOException | BadElementException e) {
			log.error("error while adding image to pdf document ", e);
		}
		return logoCellFormater(new PdfPCell());
	}

	public PdfPCell getBarcodeCell(RAMProductVO product) {
		if (product.getGtinProductId() == null || product.getGtinProductId().isEmpty()){return barcodeCellFormater(new PdfPCell());}

		StringBuilder barcode = new StringBuilder(18);
		barcode.append("011").append(StringUtil.checkVal(product.getGtinProductId()));
		if (product.getExpiree() != null ){
			Date expiree = product.getExpiree();
			String exDateCode = Convert.formatDate(expiree, "yyMMdd");
			barcode.append("17").append(exDateCode);
		}
		if (!product.getLotNumber().isEmpty() ){
			barcode.append("10").append(product.getLotNumber());
		}

		return getBarcodeCell(barcode.toString());
	}

	public PdfPCell getBarcodeCell(RAMCaseItemVO item) {
		if (item.getGtinProductId() == null || item.getGtinProductId().isEmpty()){return barcodeCellFormater(new PdfPCell());}

		StringBuilder barcode = new StringBuilder(18);
		barcode.append("011").append(StringUtil.checkVal(item.getGtinProductId()));
		if (item.getExpiree() != null ){
			Date expiree = item.getExpiree();
			String exDateCode = Convert.formatDate(expiree, "yyMMdd");
			barcode.append("17").append(exDateCode);
		}
		if (!item.getLotNumberTxt().isEmpty() ){
			barcode.append("10").append(item.getLotNumberTxt());
		}

		return getBarcodeCell(barcode.toString());
	}


	/**
	 * takes the information and puts it in barcode form.
	 * @param item
	 * @return
	 */
	public PdfPCell getBarcodeCell(String barCodeText ) {
		BarcodeImageWriter biw = new BarcodeImageWriter();
		try {

			byte[] b = biw.getDataMatrix(barCodeText, 25);

			Image image = Image.getInstance(b);
			return barcodeCellFormater(new PdfPCell(image, false)); 

		} catch (IOException | BadElementException e) {
			log.error("error while adding image to pdf document ", e);
		}
		return  barcodeCellFormater(new PdfPCell());
	}

	/**
	 * this will produce a cell with a check or empty space depending on the int flag sent in
	 * @param qtyNo
	 * @return
	 */
	public PdfPCell getFlagCell(int flag) {
		PdfPCell imageCell = null; 
		try {
			String imageUrl = attributes.get(Constants.PATH_TO_BINARY)+ CHECK_MARK_SRC;
			Image image = Image.getInstance(imageUrl);

			if (Convert.formatBoolean(flag)){
				image.setWidthPercentage(25);
				imageCell = new PdfPCell();
				imageCell.addElement(image);
			}else{
				imageCell = new PdfPCell(new Paragraph(""));
			}

			return flagCellFormatter(imageCell); 

		} catch (IOException | BadElementException e) {
			log.error("error while adding check image to pdf document ", e);
		}
		//if there is an error getting the image use yes or blank to show the information.
		if (Convert.formatBoolean(flag)){
			return  flagCellFormatter(new PdfPCell(new Paragraph("Yes",dataFont())));
		}else{
			return  flagCellFormatter(new PdfPCell());
		}
	}

	/**
	 * takes a base64 encoded image of a signature and name information and produces a sig cell
	 * @param sigTypeName
	 * @param name
	 * @param imageSrc
	 * @param colSpan
	 * @return
	 */
	public PdfPCell getSignatureCell(String sigTypeName, String name, String imageSrc, int colSpan){
		PdfPCell cell = new PdfPCell();
		String encoded = imageSrc;
		if (imageSrc.startsWith("data:image/png;base64,")){
			encoded = imageSrc.replace("data:image/png;base64,", "");
		}
		byte[] decoded = org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes());
		try {
			cell.addElement(new Paragraph(sigTypeName + ": " + name, dataFont()));
			cell.addElement(new Paragraph(" ", smallFont()));
			Image image = Image.getInstance( decoded );
			image.setWidthPercentage(80);
			cell.addElement(image);
		} catch (BadElementException | IOException e) {
			log.error("error producing signature image", e);
		}
		cell.setColspan(colSpan);
		cell.setBorder(0);
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);
		return cell;
	}
}
