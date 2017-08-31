package com.ram.action.report.vo;

//java 8
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

//app libs itext
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

//WC custom
import com.depuysynthes.nexus.NexusSolrCartAction;
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMSignatureVO;
import com.ram.action.products.ProductCartAction;

//WC base libs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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

public class ProductCartReport  extends AbstractPDFReport {

	private static final long serialVersionUID = 1L;

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
		table.addCell(getSectionSpacer(12));

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
					table.addCell(getSectionSpacer(6));
				}
			} 
		}
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

		if (isEven){
			return getSignatureCell(sigTypeName, name, imageSrc, 6);
		}else {
			return getSignatureCell(sigTypeName, name, imageSrc, 4);
		}
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
			table.addCell(getTableCell(item.getProductName()));
			table.addCell(getTableCell(item.getCustomerName()));
			table.addCell(getTableCell(item.getCustomerProductId()));
			table.addCell(getTableCell(item.getLotNumberTxt()));
			table.addCell(getTableCell(StringUtil.checkVal(item.getQuantity())));
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
		table.addCell(getTableCell("Product Name", true, false));
		table.addCell(getTableCell("Manufacturer", true, false));
		table.addCell(getTableCell("SKU", true, false));

		table.addCell(getTableCell("LOT No.", true, false));
		table.addCell(getTableCell("Qty", true, false));

		table.addCell(getTableCell("Product From", true, false));
		table.addCell(getTableCell("Expiry", true, false));
		table.addCell(getTableCell("Billable", true, false));

		table.addCell(getTableCell("Wasted", true, false));
		table.addCell(getTableCell("Barcode", true, false));

	}

	/**
	 * controls adding the top case section of the pdf
	 * @param table
	 */
	private void generateTopTable(PdfPTable table) {

		getLogoRow(table);
		//top row of the 
		table.addCell(getCaseInfoTop());

		//case info cells bordered on right or left depending on location
		caseInfoLeft(table, "Hostpital Name: " , StringUtil.checkVal(data.get(ProductCartAction.HOSPITAL)));
		caseInfoRight(table, "OR Room: " , StringUtil.checkVal(data.get(ProductCartAction.ROOM)));

		caseInfoLeft(table, "Surgery Date: " , StringUtil.checkVal(data.get(ProductCartAction.SURG_DATE)));
		caseInfoRight(table, "Surgeon Name: " , StringUtil.checkVal(data.get(ProductCartAction.SURGEON)));

		UserDataVO uvo = (UserDataVO) data.get(ProductCartAction.HOSPITAL_REP);
		UserDataVO sales = (UserDataVO) data.get(ProductCartAction.SALES_REP);

		caseInfoLeft(table, "Hospital Rep Name: " , (uvo != null)? uvo.getFullName():"" );
		caseInfoRight(table, "Sales Rep Name: " , (sales != null)? sales.getFullName():"");

		//base of table right left and bottom border and a row of spaced cells
		caseInfoBottom(table);
		table.addCell(getSectionSpacer(10));

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

		return(getTitleCell("Case Report ID: " +caseId, 12));
	}

	/**
	 * returns the last row of the case section
	 * @param table 
	 * @return
	 */
	private void caseInfoBottom(PdfPTable table) {

		table.addCell(noBorderCellFormater(new PdfPCell(new Paragraph("Case Note: ", getTitleFont())), 10));

		table.addCell(getDataStyleCell(StringUtil.checkVal(data.get("notes")), 10));
	}

	/**
	 * left half of the middle case section
	 * @param string 
	 * @param table 
	 * @return
	 */
	private void caseInfoLeft(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 1));

		table.addCell(getDataStyleCell(cellData, 3));
	}

	/**
	 * right half of the middle case section
	 * @return
	 */
	private void caseInfoRight(PdfPTable table, String cellLabel, String cellData) {

		table.addCell(getLabelCell(cellLabel, 2));

		table.addCell(getDataStyleCell(cellData, 4));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Map<?, ?>) {
			data = (Map<String, Object>) o;
		}
	}
}
