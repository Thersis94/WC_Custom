package com.depuysynthes.nexus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.pdf.BaseFont;
import com.smt.sitebuilder.action.AbstractSBReportVO;


/****************************************************************************
 * <b>Title</b>: NexusCartPDFReport.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Creates the pdf report for all the items in the kit
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Sep 11, 2015<p/>
 * @updates:
 ****************************************************************************/

public class NexusKitPDFReport  extends AbstractSBReportVO {
	
	private static final long serialVersionUID = 1L;

	protected static Logger log = Logger.getLogger(NexusCartExcelReport.class);

	Map<String, Object> data;
	
	// Keep track of how many lines have been written so that page breaks can be properly placed
	private int lines=0;
	// Holds how many lines should appear on each page
	private int threshold;
	private int start;
	private int width;

	@Override
	public byte[] generateReport() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(getPageHtml());
			Document doc = tidy.parseDOM(bais, new ByteArrayOutputStream());
	
			ITextRenderer renderer = new ITextRenderer();
			renderer.getFontResolver().addFont("http://"+data.get("baseDomain")+"/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/scripts/fonts/fontawesome-webfont.ttf", BaseFont.IDENTITY_H, true);
			renderer.setDocument(doc, "http://"+data.get("baseDomain")+"/");
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
		NexusKitVO kit =  (NexusKitVO) data.get("kit");
		boolean isForm = (boolean) data.get("isForm");
		
		
		int colspan;

		if (isForm) {
			threshold = 25;
			colspan=3;
			width = 15;
		} else {
			threshold = 37;
			colspan=1;
			width = 18;
		}
		start += Math.ceil(kit.getKitDesc().length()/((double)width));
		
		StringBuilder html = new StringBuilder(20000);
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		html.append("<html><head><title>Case Summary</title><style>");
		if (isForm) html.append("@page { size: A4 landscape;}");
		html.append(" @page {@top-center { content: element(header); width:100%;}}");
		html.append("@page {@bottom-center { content: counter(page) ' of ' counter(pages);page-break-after: always; }}");
		html.append("@page {");
		if (isForm) {
			int baseHeight = 210;
			html.append("margin-top:").append(baseHeight+start*18).append("px!important;");
		} else {
			int baseHeight = 204;
			html.append("margin-top:").append(baseHeight+start*18).append("px!important;");
		}
		html.append("margin-bottom:40px;}");
		html.append("@page {tr{page-break-inside:avoid-page}}");
		html.append(".border{border:1px solid black;} ");
		html.append("body{font-family: 'MyriadWebPro';word-wrap: break-word!important;}");
		html.append("#body td{text-align:center;} @page{");
		html.append("tr{ page-break-inside:avoid!important; page-break-after:auto!important}");
		html.append("td{ page-break-inside:avoid!important; page-break-after:auto!important}");
		html.append("thead { display:table-header-group }tfoot { display:table-footer-group }}");
		html.append("table{width:100%;} tr{min-height:15px;} #body tr{min-height:20px;}");
		if (isForm) {
			html.append(".col-1{width:3.66%}.col-2{width:13%}.col-3{width:16.66%}");
			html.append(".col-4{width:4.66%}.col-5{width:12%}.col-6{width:16.66%}");
			html.append(".col-7{width:16.66%}.col-8{width:16.66%}");
		} else {
			html.append(".col-1{width:4%}.col-2{width:20%}.col-3{width:30%}");
			html.append(".col-4{width:6%}.col-5{width:20%}");
			html.append(".col-8{width:20%}");
		}
		html.append("</style>");
		html.append("</head><body><link href='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/scripts/css/font-awesome.css' rel='stylesheet'/>");
		
		
		
		
		html.append("<div style='position:running(header);width:98.5%;display:block;'>");
		html.append("<table style='border-collapse:collapse;font-size:16px; width:100%;margin-top:15px;'><tbody>");
		
		html.append("<tr><td colspan='").append(colspan+2).append("'><img style='width:200px' src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/logo.png' /></td><td colspan='3' style='text-align:right;'>");
		html.append("<p style='font-size:20px;'><span style='color:#1A496A;'>U</span>nique&nbsp;");
		html.append("<span style='color:#1A496A;'>D</span>evice&nbsp;");
		html.append("<span style='color:#1A496A;'>I</span>dentification</p></td></tr>");
		
		html.append("<tr><td class=' col-1'></td><td class='border col-2'>Kit Code: </td><td class='border col-3'>").append(kit.getKitSKU()).append("</td><td class=' col-4'></td>");
		if(isForm) {
			html.append("<td class=' col-5'></td><td class=' col-6'></td><td class='border col-7'>Surgery Date: </td>");
		} else {
			html.append("<td class='border col-5'>Surgery Date: </td>");
		}
		html.append("<td class='border col-8'></td></tr>");
		html.append("<tr><td></td><td class='border'>Description: </td><td class='border'>");
		int pos = 0;
		while((pos+width) < kit.getKitDesc().length()) {
			html.append("<span>").append(kit.getKitDesc().substring(pos, pos+width)).append("-</span><br/>");
			pos+=width;
		}
		html.append(kit.getKitDesc().substring(pos));
		html.append("</td><td colspan='").append(colspan).append("'></td><td class='border'>Surgery Time: </td><td class='border'></td></tr>");
		html.append("<tr><td></td><td class='border'>Case Report ID: </td><td style='border:1px solid black;border-bottom:none;'></td><td colspan='").append(colspan).append("'></td><td class='border'>Surgeon: </td><td class='border'></td></tr>");
		html.append("<tr><td colspan='2' rowspan='3'><span style='font-size:24px; color:#636363;'><i class='fa fa-2'>&#xf0b1;</i>&nbsp;Products</span></td><td style='border-top:1px solid black;'></td><td colspan='").append(colspan).append("'></td><td class='border'>Operating Room: </td><td class='border'></td></tr>");
		html.append("<tr><td colspan='").append(colspan+1).append("'></td><td class='border'>Case ID: </td><td class='border'></td></tr>");
		html.append("<tr><td colspan='3'></td><td ");
		if (isForm) html.append("class='border' ");
		html.append(" colspan='2'>");
		if (isForm) html.append("(additional lot #'s can be written on back of sheet)");
		html.append("</td><td></td></tr>");
		html.append("<tr style='text-align:center'><td class='border background col-1'></td><td class='border background col-2'>Product #</td><td class='border background  col-3'>Product Desc.</td><td class='border background col-4'>QTY</td><td class='border background col-5'>GTIN</td>");
		if (isForm) html.append("<td class='border background' colspan='2'>LOT Numbers</td>");
		html.append("<td class='border background col-8'>Barcode</td></tr>");
		

		html.append("</tbody></table>");
		html.append("</div>");
		
		if (isForm) {
			html.append("<table id='body' style='border-collapse:collapse;font-size:16px; width:100%;position:relative;top:-7px;left:-8px;'><tbody>");
		} else {
			html.append("<table id='body' style='border-collapse:collapse;font-size:16px; width:100.4%;position:relative;top:-7px;left:-8px;'><tbody>");
		}

		int i=1;
		for(NexusKitLayerVO layer : kit.getLayers()){
			for (NexusProductVO p : layer.getProducts()) {
				html.append(buildRow(p, i, isForm));
				i++;
			}
			
			for (NexusKitLayerVO sublayer : layer.getSublayers()) {
				for (NexusProductVO p : sublayer.getProducts()) {
					html.append(buildRow(p, i, isForm));
					i++;
				}
			}
		}
		
		html.append("</tbody></table>");
		return html.toString().getBytes();
	}
	
	private String buildRow(NexusProductVO product, int i, boolean isForm) {
		StringBuilder row = new StringBuilder(1000);

		lines += Math.ceil(product.getSummary().length()/15.0) + 1;
		if(lines >= threshold) {
			lines =  (int) (Math.ceil(product.getSummary().length()/15.0) + 1) + start;
			row.append("<tr style='page-break-before:always'><td  rowspan='2' class='border col-1'>").append(i);
		} else {
			row.append("<tr style=''><td  rowspan='2' class='border col-1'>").append(i);
		}
		row.append("</td><td class='border col-2'>").append(product.getProductId()).append("</td>");
		row.append("<td class='border col-3'>");
		int pos = 0;
		while((pos+width) < product.getSummary().length()) {
			row.append("<span >").append(product.getSummary().substring(pos, pos+width)).append("-</span><br/>");
			pos+=width;
		}
		row.append(product.getSummary().substring(pos)).append("</td>");
		row.append("<td class='border col-4'>");
		if (!isForm) row.append(product.getQuantity());
		row.append("</td>");
		row.append("<td class='border col-5'>").append(product.getPrimaryDeviceId()).append("</td>");
		if (isForm) row.append("<td class='border col-6'></td><td class='border col-7'></td>");
		row.append("<td class='border col-8'><img alt='barcode' ");
		row.append("src='/barcodeGenerator?textFormat=NEXUS&amp;barcodeData=01").append(product.getPrimaryDeviceId());
		row.append("&amp;height=15&amp;hideHumanReadable=true' /></td>");
		
		row.append("<tr style=' page-break-inside: avoid;'>");
		row.append("<td class='border'></td><td class='border'></td>");
		if (isForm) row.append("<td class='border'></td><td class='border'></td>");
		row.append("<td class='border'></td><td class='border'></td>");
		row.append("<td class='border'></td>");
		
		
		return row.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Map<?, ?>) {
			data = (Map<String, Object>) o;
		}
	}

}
