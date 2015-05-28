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
		html.append("<style>td{padding:0px 12px;}</style>");
		html.append("<table style='color:#636363;border-collapse:collapse;font-size:16px;'><tbody><tr>");
		html.append("<td colspan='4'><img style='width:100%' src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/logo.jpg'/></td>");
		html.append("<td colspan='5'><p style='font-size:20px;'><span style='color:#1A496A;'>U</span>nique&nbsp;");
		html.append("<span style='color:#1A496A;'>D</span>evice&nbsp;");
		html.append("<span style='color:#1A496A;'>I</span>dentification</p>");
		html.append("</td></tr><tr><td colspan='4' rowspan='5' style='font-size:16px;'>Case Report(ID:").append(data.get("caseId")).append(")</td>");
		html.append("<td colspan='2' style='border-left:1px solid black; padding-left:12px;font-size:12px;'>Surgery Date and Time:</td>");
		html.append("<td colspan='3' style='font-size:12px;'>").append(data.get("time")).append("</td></tr>");
		html.append("<tr><td colspan='2' style='border-left:1px solid black; padding-left:12px;font-size:12px;'>Surgeon Name:</td>");
		html.append("<td colspan='3' style='font-size:12px;'>").append(data.get("surgeon")).append("</td></tr><tr>");
		html.append("<td colspan='2' style='border-left:1px solid black; padding-left:12px;font-size:12px;'>Hospital Name:</td>");
		html.append("<td colspan='3' style='font-size:12px;'>").append(data.get("hospital")).append("</td></tr><tr>");
		html.append("<td colspan='2' style='border-left:1px solid black; padding-left:12px;font-size:12px;'>OR Room:</td>");
		html.append("<td colspan='3' style='font-size:12px;'>").append(data.get("room")).append("</td></tr><tr>");
		html.append("<td colspan='2' style='border-left:1px solid black; padding-left:12px;font-size:12px;'>Case ID:</td>");
		html.append("<td colspan='3' style='font-size:12px;'>").append(data.get("caseId")).append("</td></tr><tr>");
		html.append("<td colspan='10'><span><i class='fa fa-2'>&#xf0b1;</i>&nbsp;Products</span></td></tr><tr>");
		html.append("<td colspan='3'>&nbsp;</td><td colspan='2'>");
		html.append("<img style='width:35%' src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/line-before.jpg' />");
		html.append("UDI<img style='width:35%' src='/binary/themes/CUSTOM/DEPUY/DPY_SYN_NEXUS/images/line-after.jpg' /></td>");
		html.append("</tr><tr style='margin-bottom:12px;'>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>&nbsp;</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black; width:80px;'>Product No.</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>Company</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>GTIN</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>LOT No.</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;width:60px;text-align:center;'>Date Lot</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>UOM</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>QTY</td>");
		html.append("<td style='font-size:12px;border-bottom:2px solid black;'>Barcodes</td>");
		html.append("<td>&nbsp;</td></tr>");

		// Loop over all the items in the cart
		@SuppressWarnings("unchecked")
		Map<String, ShoppingCartItemVO> cart = (Map<String, ShoppingCartItemVO>) data.get("cart");
		int i=1;
		for(String key : cart.keySet()){
			ShoppingCartItemVO item = cart.get(key);
			html.append("<tr><td style='font-size:12px; width:10px;'>").append(i).append(".</td>");
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
			html.append("<td colspan='2' style='font-size:12px; width:200px;'><span style='font-weight:bold;'>GTIN</span>");
			
			if ("QR".equals(data.get("format"))) {
				html.append("<span><img style='margin-left:5px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("gtin")).append("&height=25&humanReadable=false&format=").append(data.get("format")).append("' /></span></td></tr>");
				html.append("<tr><td style='border-bottom:1px solid black;'>&nbsp;</td>");
				html.append("<td colspan='7' style='font-size:12px; width:200px;border-bottom:1px solid black;'>").append(item.getProduct().getShortDesc()).append("</td><td colspan='2' style='font-size:12px;border-bottom:1px solid black;'>");
				html.append("<span style='font-weight:bold;'>LOT<span><img style='margin-left:10px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("lotNo")).append("&height=25&humanReadable=false&format=").append(data.get("format")).append("' /></span>");
			} else {
				html.append("<span><img style='height:10px; margin-left:5px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("gtin")).append("&height=25&humanReadable=false' /></span></td></tr>");
				html.append("<tr><td style='border-bottom:1px solid black;'>&nbsp;</td>");
				html.append("<td colspan='7' style='font-size:12px; width:200px;border-bottom:1px solid black;'>").append(item.getProduct().getShortDesc()).append("</td><td colspan='2' style='font-size:12px;border-bottom:1px solid black;'>");
				html.append("<span style='font-weight:bold;'>LOT<span><img style='height:10px; margin-left:10px;' src='/barcodeGenerator?barcodeData=").append(item.getProduct().getProdAttributes().get("lotNo")).append("&height=25&humanReadable=false' /></span>");
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
