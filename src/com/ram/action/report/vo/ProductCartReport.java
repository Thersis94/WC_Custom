package com.ram.action.report.vo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
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
			renderer.setDocument(doc, "http://"+data.get("baseDomain")+"/");
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
		html.append("<html><head><title>Case Summary</title></head><body>");
		html.append("<link href='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/scripts/css/font-awesome.css' rel='stylesheet'>");
		html.append("<style>@page{margin-bottom:50px;}body{font-family: 'MyriadWebPro';}th{margin-bottom:10px;border-bottom:solid black 2px; font-size:12px;}");
		html.append("@media print{div.sig-footer{position:fixed; bottom:0;}}");
		html.append("</style>");
		
		html.append("<div class='sig-footer'><table><tr><td><p>Sales Rep</p><img style='height:50px;' src='").append(data.get("sales")).append("'/></td>");
		html.append("<td><p>Hospital Administrator</p><img style='height:50px;' src='").append(data.get("admin")).append("'/></td></tr></table></div>");
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px; width:100%;'><tbody>");
		html.append("<tr><td style='width:48%'><img style='width:200px' src='/binary/themes/CUSTOM/RAMGRP/MAIN/images/ramgrouplogo.png' /><span style='float:right'>");
		html.append(data.get(ProductCartAction.COMPLETE_DT)).append("</span></td><td colspan='2' style='text-align:right;'>");
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
		html.append("<span style='font-size:24px; color:#636363;'><i class='fa fa-2'>&#xf0b1;</i>&nbsp;Products</span>");
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px; width:100%'>");
		html.append("<tbody><tr style='margin-bottom:10px;'><th style='width:2%'>&nbsp;</th><th style='width:15%'>Product Name</th>");
		html.append("<th style='width:14%'>Company</th><th style='width:13%;'>GTIN</th><th style='width:10%'>LOT No.</th>");
		html.append("<th style='width:5%; text-align:center;'>QTY</th>");
		html.append("<th style='width:5%; text-align:center;'>Billable</th>");
		html.append("<th style='width:5%; text-align:center;'>Wasted</th>");
		html.append("<th style='text-align:center'>Barcode</th></tr>");


		// Loop over all the items in the cart
		@SuppressWarnings("unchecked")
		Map<String, ShoppingCartItemVO> cart = (Map<String, ShoppingCartItemVO>) data.get("cart");
		int i=1;
		String border="border-bottom:1px solid black;";
		for(String key : cart.keySet()){
			if (i == cart.size()) border="";
			ShoppingCartItemVO item = cart.get(key);
			html.append("<tr><td style='font-size:12px;'>").append(i).append(".</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProductName()).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("customer")).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("gtin")).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("lotNo")).append("</td>");
			html.append("<td style='font-size:12px; text-align:center;'>").append(item.getQuantity()).append("</td>");
			html.append("<td style='font-size:12px; text-align:center;'>");
			if (Convert.formatBoolean(item.getProduct().getProdAttributes().get("billable"))) html.append("<i class='fa'>&#xf00c;</i>");
			html.append("</td>");
			html.append("<td style='font-size:12px; text-align:center;'>");
			if (Convert.formatBoolean(item.getProduct().getProdAttributes().get("wasted"))) html.append("<i class='fa'>&#xf00c;</i>");
			html.append("</td>");
			// This ends off without closing the tag so that the single barcode option can add in a rowspan attribute
			html.append("<td style='font-size:12px; width:400px; text-align:right;");
			String gtin = (String) item.getProduct().getProdAttributes().get("gtin");
			html.append("'><span style='font-weight:bold;position:relative;top:30px;'>GTIN</span><span><img style='float:right' src='/barcodeGenerator?barcodeData=011").append(gtin).append("&height=55' /></span></td></tr>");
			html.append("<tr><td style='").append(border).append("'>&nbsp;</td>");
			html.append("<td colspan='9' style='font-size:12px; margin-bottom:10px; text-align:right;").append(border).append("'>");
			html.append("<span style='font-weight:bold;position:relative;top:30px;'>LOT</span><span><img style='float:right' src='/barcodeGenerator?barcodeData=17").append(item.getProduct().getProdAttributes().get("lotNo")).append("&height=55' /></span>");
			
			html.append("</span></td></tr>");
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
