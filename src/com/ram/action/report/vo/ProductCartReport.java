package com.ram.action.report.vo;

//java 8
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


//app libs itext
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.html.WebColors;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

//WC custom
import com.depuysynthes.nexus.NexusSolrCartAction;
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMSignatureVO;
import com.ram.action.products.ProductCartAction;

//WC base libs
import com.siliconmtn.barcode.BarcodeImageWriter;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

//WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;


/****************************************************************************
 * <b>Title</b>ProductCartReport.java<p/>
 * <b>Description: Creates a pdf with information about the current cart</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 * 		changed from using itext rendered parsing xml/html to 
 * 		using itext to make tables and cells.
 ****************************************************************************/

public class ProductCartReport  extends AbstractSBReportVO {

	private static final long serialVersionUID = 1L;

	private static final String IMG_SRC ="/themes/CUSTOM/RAMGRP/MAIN/images/ramgrouplogo.png";
	private static final String CHECK_MARK_SRC = "/org/RAM/images/checkMark.png";

	private static final String HEADER_GREY = "#e7e7e7";
	private static final String TEXT_GREY = "#4e4e4e";


	Map<String, Object> data;

	@Override
	public byte[] generateReport() {
		log.debug("generating report");
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		final Document document = new Document();
		try {
			PdfWriter.getInstance(document, byteStream);
			document.setPageSize(new Rectangle(792, 612));
			document.addTitle(data.get(ProductCartAction.CASE_ID)+"");
			document.setHtmlStyleClass("@page land {size: landscape;}");
			document.open();
			
			PdfPTable table = new PdfPTable(10);
			table.setWidthPercentage(100f);
			table.setWidths(new float[] { 5,4,3 ,2,2, 3,3,2, 2,2 });
			table.setHeaderRows(1);
			table.setFooterRows(0);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

			generateTopTable(table);
			generateProductSection(table);
			generateSignatureSection(table);

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
	 * generates the section of the document that will hold names and signatures
	 * @param table
	 */
	private void generateSignatureSection(PdfPTable table) {
		table.addCell(getSectionSpacer());
		
		@SuppressWarnings("unchecked")
		List<RAMSignatureVO> sigList = (List<RAMSignatureVO>) data.get("signatures");
		if (sigList == null || sigList.isEmpty()) {return;}
		
		int index = 0;
		int max = sigList.size();
		
		for  ( RAMSignatureVO sig : sigList) {
			log.debug("index " +index+ " max " + max);
			String imageSrc = null;
			if (StringUtil.checkVal(sig.getSignatureTxt()).startsWith("data")) {
				boolean isEven = ((index%2)==1);
				imageSrc = sig.getSignatureTxt();
				
				StringBuilder nameDate = new StringBuilder(32);
				nameDate.append(sig.getFirstNm()).append(" ");
				nameDate.append(sig.getLastNm()).append(" (");
				nameDate.append(Convert.formatDate(sig.getCreateDt(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
				nameDate.append(")");
				
				
				table.addCell(getSigCell(sig.getSignatureType().getName(), nameDate.toString() , imageSrc, isEven));
				index++;

				//in order for the cells to be drawn in the pdf they must have a full row.  if the last cell drawn is
				//on the left, and a spacer to the right.
				if (index >= max && !isEven){
					table.addCell(getSigSpacer());
				}
			} 
		}
	}

	/**
	 * used to fill up the black space that is sometimes needed in row on the pdf
	 * @return
	 */
	private PdfPCell getSigSpacer() {
		log.debug("making spacer");
		PdfPCell cell = new PdfPCell(new Paragraph("", dataFont()));
		cell.setBorder(0);
		cell.setColspan(6);
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);
		return cell;
	}

	/**
	 * a cell to hold a name and signature
	 * @param imageSrc 
	 * @param max 
	 * @param isEven 
	 * @param signatureType 
	 * @param string 
	 * @return
	 */
	private PdfPCell getSigCell(String sigTypeName, String name, String imageSrc, boolean isEven) {
		PdfPCell cell = new PdfPCell();
		String encoded = imageSrc.replace("data:image/png;base64,", "");
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

		cell.setBorder(0);
		if (isEven){
			cell.setColspan(6);
		}else {
			cell.setColspan(4);
		}

		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);

		return cell;
	}

	/**
	 * this will produce a cell with a check or empty space depending on the int flag sent in
	 * @param qtyNo
	 * @return
	 */
	private PdfPCell getFlagCell(int flag) {
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
	 * formats cells for inner table boolean responses
	 * @param imageCell
	 * @return
	 */
	private PdfPCell flagCellFormatter(PdfPCell imageCell) {
		imageCell.setBorder(0);
		imageCell.setColspan(1);
		imageCell.setBorderWidthBottom(0.25f);
		imageCell.setPaddingTop(5);
		imageCell.setBackgroundColor(WebColors.getRGBColor("white"));
		imageCell.setPaddingBottom(10);
		imageCell.setPaddingLeft(10);
		return imageCell;
	}
	
	/**
	 * formats the barcode cell
	 * @param imageCell
	 * @return
	 */
	private PdfPCell barcodeCellFormater(PdfPCell imageCell) {
		imageCell.setBorder(0);
		imageCell.setColspan(1);
		imageCell.setBorderWidthBottom(0.25f);
		imageCell.setPaddingTop(5);
		imageCell.setBackgroundColor(WebColors.getRGBColor("white"));
		imageCell.setPaddingBottom(10);
		imageCell.setPaddingLeft(10);
		return imageCell;
	}
	
	/**
	 * formats the cell for the logo area of the pdf
	 * @param pdfPCell
	 * @return
	 */
	private PdfPCell logoCellFormater(PdfPCell pdfPCell) {
		pdfPCell.setBorder(0);
		pdfPCell.setColspan(1);
		pdfPCell.setPaddingBottom(10);
		pdfPCell.setPaddingLeft(10);
		return pdfPCell;
	}

	/**
	 * takes the information and puts it in barcode form.
	 * @param item
	 * @return
	 */
	private PdfPCell getBarcodeCell(RAMCaseItemVO item) {
		BarcodeImageWriter biw = new BarcodeImageWriter();
		 
		try {
			
			StringBuilder barcode = new StringBuilder(18);
			barcode.append("011").append(StringUtil.checkVal(item.getGtinProductId()));
			if (!item.getLotNumberTxt().isEmpty() && item.getExpiree() != null ){
				Date expiree = item.getExpiree();
				String exDateCode = Convert.formatDate(expiree, "yyMMdd");
				barcode.append("17").append(exDateCode);
				barcode.append("10").append(item.getLotNumberTxt());
			}
			
			byte[] b = biw.getDataMatrix(barcode.toString(), 25);
			
			Image image = Image.getInstance(b);
			return barcodeCellFormater(new PdfPCell(image, false)); 
			
		} catch (IOException | BadElementException e) {
			log.error("error while adding image to pdf document ", e);
		}
		return  barcodeCellFormater(new PdfPCell());
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
		@SuppressWarnings("unchecked")
		Collection<RAMCaseItemVO> cart = (Collection<RAMCaseItemVO>) data.get("cart");

		for(RAMCaseItemVO item : cart){
			table.addCell(getTableCell(item.getProductNm()));
			table.addCell(getTableCell(item.getCustomerNm()));
			table.addCell(getTableCell(item.getCustomerProductId()));
			table.addCell(getTableCell(item.getLotNumberTxt()));
			table.addCell(getTableCell(StringUtil.checkVal(item.getQtyNo())));
			table.addCell(getTableCell(item.getProductFromTxt()));
			table.addCell(getTableCell(Convert.formatDate(item.getExpiree(), Convert.DATE_SLASH_PATTERN)));
			table.addCell(getFlagCell(item.getBillableFlg()));
			table.addCell(getFlagCell(item.getWastedFlg()));
			table.addCell(getBarcodeCell(item));
		}
	}

	/**
	 * returns the header of the product section
	 * @param table
	 */
	private void generateSectionHeader(PdfPTable table) {
		//the product section has the most columns so it controls the number of cols in the table
		table.addCell(getTableCell("Product Name", true));
		table.addCell(getTableCell("Manufacturer", true));
		table.addCell(getTableCell("SKU", true));

		table.addCell(getTableCell("LOT No.", true));
		table.addCell(getTableCell("Qty", true));
		
		table.addCell(getTableCell("Product From", true));
		table.addCell(getTableCell("Expiry", true));
		table.addCell(getTableCell("Billable", true));

		table.addCell(getTableCell("Wasted", true));
		table.addCell(getTableCell("Barcode", true));

	}

	/**
	 * controls adding the top case section of the pdf
	 * @param table
	 */
	private void generateTopTable(PdfPTable table) {
		//logo and empty space
		table.addCell(createLogoCell());
		table.addCell(getSectionSpacer());

		//top row of the 
		table.addCell(getCaseInfoTop());

		//case info cells bordered on right or left depending on location
		caseInfoLeft(table, "Hostpital Name: " , StringUtil.checkVal(data.get(ProductCartAction.HOSPITAL)));
		caseInfoRight(table, "OR Room: " , StringUtil.checkVal(data.get(ProductCartAction.ROOM)));

		caseInfoLeft(table, "Surgery Date: " , StringUtil.checkVal(data.get(ProductCartAction.SURG_DATE)));
		caseInfoRight(table, "Surgeon Name: " , StringUtil.checkVal(data.get(ProductCartAction.SURGEON)));

		UserDataVO uvo = (UserDataVO) data.get(ProductCartAction.HOSPITAL_REP);
		String HospitalRepName = "";
		
		if (uvo != null) {
			HospitalRepName = uvo.getFullName();
		}
		
		caseInfoLeft(table, "Hospital Rep Name: " , HospitalRepName );
		caseInfoRight(table, "Sales Rep Name: " , ((UserDataVO) data.get(ProductCartAction.SALES_REP)).getFullName());

		//base of table right left and bottom border and a row of spaced cells
		caseInfoBottom(table);
		table.addCell(getSectionSpacer());

	}

	/**
	 * top of the case section
	 * @return
	 */
	private PdfPCell getCaseInfoTop() {
		String caseId = "";

		if (StringUtil.checkVal(data.get(NexusSolrCartAction.CASE_ID)).length() > 0){
			caseId = StringUtil.checkVal(data.get(NexusSolrCartAction.CASE_ID));
		} 

		PdfPCell cell = new PdfPCell(new Paragraph(("Case Report ID: " +caseId ), titleFont()));
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setColspan(12);
		cell.setBorderWidthBottom(0.25f);
		cell.setBackgroundColor(WebColors.getRGBColor(HEADER_GREY));
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);
		return(cell);
	}

	/**
	 * returns a times Roman font bold for titles and important headers
	 * @return
	 */
	private Font titleFont() {
		return FontFactory.getFont(FontFactory.TIMES_BOLD, 11, WebColors.getRGBColor("black"));
	}
	/**
	 * returns a font for very small text
	 * @return
	 */
	private Font smallFont() {
		return FontFactory.getFont(FontFactory.TIMES_ROMAN, 5, WebColors.getRGBColor("black"));
	}

	/**
	 * returns a times roman font for data
	 * @return
	 */
	private Font dataFont() {
		return FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, WebColors.getRGBColor(TEXT_GREY));
	}

	/**
	 * returns a font for use in the product table section
	 * @return
	 */
	private Font productDataFont() {
		return FontFactory.getFont(FontFactory.TIMES_ROMAN, 9, WebColors.getRGBColor("black"));
	}

	/**
	 * returns the last row of the case section
	 * @param table 
	 * @return
	 */
	private void caseInfoBottom(PdfPTable table) {

		PdfPCell labelCell = new PdfPCell(new Paragraph("Case Note: ", titleFont()));
		labelCell.setBorder(0);
		labelCell.setColspan(10);
		labelCell.setBackgroundColor(WebColors.getRGBColor("white"));
		labelCell.setPaddingLeft(10);
		table.addCell(labelCell);
		
		PdfPCell noteCell = new PdfPCell(new Paragraph( StringUtil.checkVal(data.get("notes")), dataFont()));
		noteCell.setBorder(0);
		noteCell.setColspan(10);
		noteCell.setBorderWidthBottom(0.25f);
		noteCell.setBackgroundColor(WebColors.getRGBColor("white"));
		noteCell.setPaddingBottom(10);
		noteCell.setPaddingLeft(10);
		table.addCell(noteCell);

	}

	/**
	 * left half of the middle case section
	 * @param string 
	 * @param table 
	 * @return
	 */
	private void caseInfoLeft(PdfPTable table, String cellLabel, String cellData) {

		PdfPCell labelCell = new PdfPCell(new Paragraph(cellLabel, titleFont()));
		labelCell.setBorder(0);
		labelCell.setColspan(1);
		labelCell.setBorderWidthBottom(0.25f);
		labelCell.setBackgroundColor(WebColors.getRGBColor("white"));
		labelCell.setPaddingBottom(10);
		labelCell.setPaddingLeft(10);

		table.addCell(labelCell);

		PdfPCell dataCell = new PdfPCell(new Paragraph(cellData, dataFont()));
		dataCell.setBorder(0);
		dataCell.setColspan(3);
		dataCell.setBorderWidthBottom(0.25f);
		dataCell.setBackgroundColor(WebColors.getRGBColor("white"));
		dataCell.setPaddingBottom(10);
		dataCell.setPaddingLeft(10);

		table.addCell(dataCell);
	}
	/**
	 * right half of the middle case section
	 * @return
	 */
	private void caseInfoRight(PdfPTable table, String cellLabel, String cellData) {

		PdfPCell labelCell = new PdfPCell(new Paragraph(cellLabel, titleFont()));
		labelCell.setBorder(0);
		labelCell.setColspan(2);
		labelCell.setBorderWidthBottom(0.25f);
		labelCell.setBackgroundColor(WebColors.getRGBColor("white"));
		labelCell.setPaddingBottom(10);
		labelCell.setPaddingLeft(10);

		table.addCell(labelCell);

		PdfPCell dataCell = new PdfPCell(new Paragraph(cellData, dataFont()));
		dataCell.setBorder(0);
		dataCell.setColspan(4);
		dataCell.setBorderWidthBottom(0.25f);
		dataCell.setBackgroundColor(WebColors.getRGBColor("white"));
		dataCell.setPaddingBottom(10);
		dataCell.setPaddingLeft(10);

		table.addCell(dataCell);
	}
	/**
	 * a row of empty cells between table areas
	 * @return
	 */
	private PdfPCell getSectionSpacer() {
		PdfPCell cell = new PdfPCell(new Paragraph("", dataFont()));
		cell.setBorder(0);
		cell.setColspan(12);
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);
		return cell;
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

	/**
	 * generates one cell of table data 
	 * @param string
	 * @return
	 */
	private PdfPCell getTableCell(String cellContent) {
		return getTableCell(cellContent, false);
	}

	/**
	 * generates one cell of table data 
	 * @param string
	 * @return
	 */
	private PdfPCell getTableCell(String cellContent, boolean isHeader) {
		PdfPCell cell = new PdfPCell(new Paragraph(cellContent , productDataFont()));
		cell.setBorder(0);
		cell.setBorderWidthBottom(0.25f);
		cell.setPaddingBottom(10);
		cell.setPaddingLeft(10);

		if (isHeader) {
			cell.setBackgroundColor(WebColors.getRGBColor(HEADER_GREY));
		}

		return cell;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Map<?, ?>) {
			data = (Map<String, Object>) o;
		}
	}
}
