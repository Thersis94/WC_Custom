package com.depuysynthes.action.nexus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.pdf.BaseFont;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;


/****************************************************************************
 * <b>Title</b>: NexusCartPDFReport.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Creates the pdf report for all the items in the cart
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since May 27, 2015<p/>
 * @updates:
 ****************************************************************************/

public class NexusCartPDFReport  extends AbstractSBReportVO {
	
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
		html.append("<style>body{font-family: 'MyriadWebPro';}th{margin-bottom:10px;border-bottom:solid black 2px; font-size:12px;}</style>");
		
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px; width:100%;'><tbody>");
		html.append("<tr><td style='width:48%'><img style='width:200px' src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/logo.png' /></td><td colspan='2' style='text-align:right;'>");
		html.append("<p style='font-size:20px;'><span style='color:#1A496A;'>U</span>nique&nbsp;");
		html.append("<span style='color:#1A496A;'>D</span>evice&nbsp;");
		html.append("<span style='color:#1A496A;'>I</span>dentification</p></td></tr>");
		html.append("<tr><td rowspan='5'>");
		if (StringUtil.checkVal(data.get("caseId")).length() > 0)
			html.append("<span style='font-size:20px;'>Case Report(ID:").append(data.get("caseId")).append(")</span>");
		html.append("</td>");
		html.append("<td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Surgery Date and Time:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get("time")).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Surgeon Name:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get("surgeon")).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Hospital Name:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get("hospital")).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>OR Room:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get("room")).append("</td></tr>");
		html.append("<tr><td style='border-left: solid 1px black; padding-left:10px;font-size:14px;'>Case ID:</td>");
		html.append("<td style='font-size:14px;'>").append(data.get("caseId")).append("</td></tr></tbody></table>");
		html.append("<span style='font-size:24px; color:#636363;'><i class='fa fa-2'>&#xf0b1;</i>&nbsp;Products</span>");
		html.append("<div style='position:relative; left:26%;'>");
		html.append("<img src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/line-before.jpg' style='width:60px' /> ");
		html.append("UDI<img src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/line-after.jpg' style='width:60px' /></div>");
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px; width:100%'>");
		html.append("<tbody><tr style='margin-bottom:10px;'><th style='width:2%'>&nbsp;</th><th style='width:12%'>Product No.</th>");
		html.append("<th style='width:12%'>Company</th><th style='width:15%;'>GTIN</th><th style='width:10%'>LOT No.</th>");
		html.append("<th style='width:8%'>Date Lot</th><th style='width:8%'>UOM</th><th style='width:7%'>QTY</th>");
		html.append("<th>Barcode</th></tr>");


		// Loop over all the items in the cart
		@SuppressWarnings("unchecked")
		Map<String, ShoppingCartItemVO> cart = (Map<String, ShoppingCartItemVO>) data.get("cart");
		int i=1;
		String border="border-bottom:1px solid black;";
		for(String key : cart.keySet()){
			if (i == cart.size()) border="";
			ShoppingCartItemVO item = cart.get(key);
			html.append("<tr><td style='font-size:12px;'>").append(i).append(".</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProductId()).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("orgName")).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("gtin")).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("lotNo")).append("</td>");
			html.append("<td style='font-size:12px; text-align:center;'>");
			if (Convert.formatBoolean(item.getProduct().getProdAttributes().get("dateLot"))) {
				html.append("<i class='fa'>&#xf00c;</i>");
			}
			html.append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("uom")).append("</td>");
			html.append("<td style='font-size:12px;'>").append(item.getProduct().getProdAttributes().get("qty")).append("</td>");
			html.append("<td style='font-size:12px; width:400px;min-height:70px;'>");
			
			if ("DM".equals(data.get("format"))) {
				html.append("<span><img style='height:35px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("gtin")).append("&height=35&humanReadable=false&format=").append(data.get("format")).append("' /></span></td></tr>");
				html.append("<tr style='margin-bottom:10px;'><td style='").append(border).append("'>&nbsp;</td>");
				html.append("<td colspan='7' style='font-size:12px; width:400px;").append(border).append("'>");
				html.append(item.getProduct().getShortDesc()).append("</td><td style='font-size:12px;min-height:70px;").append(border).append("'>");
				html.append("<img style='margin-bottom:10px;height:35px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("lotNo")).append("&height=35&humanReadable=false&format=").append(data.get("format")).append("' /></span>");
			} else {
				html.append("<span style='font-weight:bold;position:relative;top:-5px;'>GTIN</span><span><img style='height:50px; margin-left:5px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("gtin")).append("&height=50&humanReadable=false' /></span></td></tr>");
				html.append("<tr><td style='").append(border).append("'>&nbsp;</td>");
				html.append("<td colspan='7' style='font-size:12px; width:400px;").append(border).append("'>");
				html.append(item.getProduct().getShortDesc()).append("</td><td style='font-size:12px;min-height:70px; margin-bottom:10px;").append(border).append("'>");
				html.append("<span style='font-weight:bold;position:relative;top:-5px;'>LOT</span><span><img style='height:50px; margin-left:10px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("lotNo")).append("&height=50&humanReadable=false' /></span>");
			}
			html.append("</span></td></tr>");
			i++;
		}
		html.append("</tbody></table></body></html>");
		log.debug(html);
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
