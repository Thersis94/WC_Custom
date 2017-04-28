package com.ram.action.report.vo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.depuysynthes.nexus.NexusCartExcelReport;
import com.depuysynthes.nexus.NexusSolrCartAction;
import com.lowagie.text.pdf.BaseFont;
import com.ram.action.products.ProductCartAction;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.pdf.Base64ImageReplacer;
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
 ****************************************************************************/

public class ProductCartReport  extends AbstractSBReportVO {

	private static final long serialVersionUID = 1L;

	protected static Logger log = Logger.getLogger(NexusCartExcelReport.class);

	Map<String, Object> data;

	@Override
	public byte[] generateReport() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(getPageHtml());
			Document doc = tidy.parseDOM(bais, new ByteArrayOutputStream());
	
			ITextRenderer renderer = new ITextRenderer();
			log.debug(data.get("baseDomain"));
			renderer.getFontResolver().addFont("http://"+data.get("baseDomain")+"/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/scripts/fonts/fontawesome-webfont.ttf", BaseFont.IDENTITY_H, true);
			renderer.getFontResolver().addFont("http://"+data.get("baseDomain")+"/binary/common/fonts/GreatVibes-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
			renderer.setDocument(doc, "http://"+data.get("baseDomain")+"/");
			// Add the replacer so that base64 images are rendered properly
			renderer.getSharedContext().setReplacedElementFactory(new Base64ImageReplacer(renderer.getSharedContext().getReplacedElementFactory()));
			renderer.layout();
			renderer.createPDF(os);
		} catch (Exception e) {
			log.error("Error creating PDF File", e);
		}
	
		return os.toByteArray();
	}
	
	/**
	 * Create the html page that the pdf will be generated from
	 * @return
	 */
	private byte[] getPageHtml() {
		StringBuilder html = new StringBuilder(3000);
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		html.append("<html><head><title>Case Summary</title>");
		html.append("<link href='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/scripts/css/font-awesome.css' type='text/css' rel='stylesheet'>");
		html.append("<link type='text/css'  href=\"https://fonts.googleapis.com/css?family=Great+Vibes\" rel=\"stylesheet\">");
		html.append("<style type='text/css'>");
		html.append("@page{margin-bottom:50px;}th{margin-bottom:10px;border-bottom:solid black 2px; font-size:12px;}");
		html.append("@media print{div.sig-footer{position:absolute; bottom:-50px;}}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<div class='sig-footer'><table><tr><td><p>Manufacturer Rep:</p></td><td>");
		if (StringUtil.checkVal(data.get("sales")).startsWith("data")) {
			html.append("<img alt='Signature' style='height:50px;' src='").append(data.get("sales")).append("'/>");
		} else {
			html.append("<p style='font-family: \"Great Vibes\", cursive;font-size:20px;'>").append(data.get(ProductCartAction.SALES_SIGNATURE)).append("</p>");
		}
		html.append("</td><td style='font-size:10px;'>").append(data.get(ProductCartAction.SALES_SIGNATURE_DT)).append("</td>");
		html.append("<td><p>Hospital Administrator:</p></td><td>");
		if (StringUtil.checkVal(data.get("sales")).startsWith("data")) {
			html.append("<img alt='Signature' style='height:50px;' src='").append(data.get("admin")).append("'/>");
		} else {
			html.append("<p style='font-family: \"Great Vibes\", cursive;font-size:20px;'>").append(data.get(ProductCartAction.ADMIN_SIGNATURE)).append("</p>");
			
		}
		
		html.append("</td><td style='font-size:10px;'>").append(data.get(ProductCartAction.ADMIN_SIGNATURE_DT)).append("</td>").append("</tr></table></div>");
		
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px; width:100%;'><tbody>");
		html.append("<tr><td style='width:48%'><img alt='RAM Healthcare' style='width:200px' src='/binary/themes/CUSTOM/RAMGRP/MAIN/images/ramgrouplogo.png' /><span style='float:right'>");
		if (data.get(ProductCartAction.COMPLETE_DT) != null) html.append(data.get(ProductCartAction.COMPLETE_DT));
		html.append("</span></td><td colspan='2' style='text-align:right;'>");
		html.append("</td></tr>");
		html.append("<tr><td rowspan='8'>");
		if (StringUtil.checkVal(data.get(NexusSolrCartAction.CASE_ID)).length() > 0)
			html.append("<span style='font-size:20px;'>Case Report (ID: ").append(data.get(ProductCartAction.CASE_ID)).append(")</span>");
		html.append("</td>");
		html.append("<td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Surgery Date and Time:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.TIME)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Surgeon Name:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.SURGEON)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Hospital Name:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.HOSPITAL)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Operating Room:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.ROOM)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Case ID:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.CASE_ID)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Other ID:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.OTHER_ID)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Reseller Name:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.RESELLER)).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Reseller ID:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get(ProductCartAction.REP_ID)).append("</td></tr></tbody></table>");
		html.append("<span style='font-size:24px; color:#636363;'>Products</span>");
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px; width:100%'>");
		html.append("<tbody><tr style='margin-bottom:10px;'><th style='width:2%'>&nbsp;</th><th style='width:15%'>Product Name</th>");
		html.append("<th style='width:14%'>Company</th><th style='width:13%;'>GTIN</th><th style='width:10%'>LOT No.</th>");
		html.append("<th style='width:5%; text-align:center;'>QTY</th>");
		html.append("<th style='width:5%; text-align:center;'>Billable</th>");
		html.append("<th style='width:5%; text-align:center;'>Wasted</th>");
		html.append("<th style='text-align:center'>Barcode</th></tr>");


		// Loop over all the items in the cart
		@SuppressWarnings("unchecked")
		Collection<ShoppingCartItemVO> cart = (Collection<ShoppingCartItemVO>) data.get("cart");
		int i=1;
		String border="border-bottom:1px solid black;";
		for(ShoppingCartItemVO item : cart){
			if (i == cart.size()) border="";
			html.append("<tr style='height:60px;page-break-inside: avoid;'><td style='font-size:12px;'>").append(i).append(".</td>");
			html.append("<td style='font-size:12px;margin-bottom:20px;").append(border).append("'>").append(item.getProduct().getProductName()).append("</td>");
			html.append("<td style='font-size:12px;margin-bottom:20px;").append(border).append("'>").append(item.getProduct().getProdAttributes().get("customer")).append("</td>");
			html.append("<td style='font-size:12px;margin-bottom:20px;").append(border).append("'>").append(item.getProduct().getProdAttributes().get("gtin")).append("</td>");
			html.append("<td style='font-size:12px;margin-bottom:20px;").append(border).append("'>").append(item.getProduct().getProdAttributes().get("lotNo")).append("</td>");
			html.append("<td style='font-size:12px; text-align:center;margin-bottom:20px;").append(border).append("'>").append(item.getQuantity()).append("</td>");
			html.append("<td style='font-size:12px; text-align:center;margin-bottom:20px;").append(border).append("'>");
			if (Convert.formatBoolean(item.getProduct().getProdAttributes().get("billable"))) html.append("<i class='fa'>&#xf00c;</i>");
			html.append("</td>");
			html.append("<td style='font-size:12px; text-align:center;margin-bottom:20px;").append(border).append("'>");
			if (Convert.formatBoolean(item.getProduct().getProdAttributes().get("wasted"))) html.append("<i class='fa'>&#xf00c;</i>");
			html.append("</td>");
			// This ends off without closing the tag so that the single barcode option can add in a rowspan attribute
			html.append("<td style='font-size:12px; width:400px; text-align:right;margin-bottom:20px;").append(border);
			StringBuilder barcodeData = new StringBuilder(50);
			barcodeData.append("011").append(item.getProduct().getProdAttributes().get("gtin"));
			if (StringUtil.checkVal(item.getProduct().getProdAttributes().get("lotNo")).length() > 0) {
				barcodeData.append("17").append(item.getProduct().getProdAttributes().get("lotNo"));
			}
			
			html.append("'><span><img style='float:right' alt='").append(barcodeData).append("' src='/barcodeGenerator?format=DM&amp;barcodeData=011").append(barcodeData);
			
			html.append("&amp;height=40' /></span></td></tr>");
			i++;
		}
		
		
		html.append("</tbody></table></body>");
		return html.toString().getBytes();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Map<?, ?>) {
			data = (Map<String, Object>) o;
		}
	}
}
