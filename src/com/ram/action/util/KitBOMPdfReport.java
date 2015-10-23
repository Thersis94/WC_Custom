/**
 *
 */
package com.ram.action.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.ram.datafeed.data.KitLayerProductVO;
import com.ram.datafeed.data.KitLayerVO;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: KitBOMPdfReport.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Class is responsible for generating the Bill of Materials
 * report for Kits.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Oct 9, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class KitBOMPdfReport extends AbstractSBReportVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public KitBOMPdfReport() {
	}

	private RAMProductVO data = null;
	private String baseDomain = null;

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true);
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(getPageHtml());
			Document doc = tidy.parseDOM(bais, new ByteArrayOutputStream());
	
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(doc, "http://"+ baseDomain +"/");
			renderer.layout();
			renderer.createPDF(os);
		} catch (Exception e) {
			log.error("Error creating PDF File", e);
		}
	
		return os.toByteArray();
	}

	/**
	 * Helper method that generates the page data for the PDF Formatter.
	 * @return
	 */
	private byte[] getPageHtml() {
		StringBuilder html = new StringBuilder(3000);
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		html.append("<html><head><title>").append(data.getProductName()).append(" BOM</title></head><body>");
		html.append("<style>@page {@top-center { content: element(header); width:100%;}}</style>");
		html.append("<div style='position:running(header);text-align:center;'><h2>").append(data.getProductName()).append("</h2></div>");
		html.append("<div class='content'>");
		int cnt = 1;
			for(KitLayerVO k : data.getKitLayers()) {
				html.append("<h3>Layer ").append(k.getDepthNumber()).append("</h3>");
				html.append("<table border='1' style='color:#333;border-collapse:collapse;font-size:16px; width:100%;'><thead><tr>");
				html.append("<th>Product Desc</th>");
				html.append("<th>Product Id</th>");
				html.append("<th>Vendor/OEM</th>");
				html.append("<th>Inventory Date</th>");
				html.append("<th>Qty.</th>");
				html.append("</tr></thead><tbody>");
				for(KitLayerProductVO p: k.getProducts().values()) {
					html.append("<tr>");
					html.append("<td>").append(p.getProductName()).append("</td>");
					html.append("<td>").append(p.getProduct().getCustomerProductId()).append("</td>");
					html.append("<td>").append(p.getProduct().getCustomerName()).append("</td>");
					html.append("<td>").append(Convert.formatDate(new Date())).append("</td>");
					html.append("<td>").append(p.getQuantity()).append("</td>");
					html.append("</tr>");
				}
				html.append("</tbody></table>");
				if(cnt < data.getKitLayers().size()) {
					html.append("<p style='page-break-before:always; height:0px; margin:0px; padding:0px;'>&nbsp;</p>");
					cnt++;
				}
			}
		html.append("</div></body>");
		html.append("</html>");
		
		log.debug(html.toString());
		return html.toString().getBytes();
	}

	@Override
	public void setData(Object o) {
		if (o instanceof RAMProductVO) {
			data = (RAMProductVO) o;
		}
	}

	public void setBaseDomain(String baseDomain) {
		this.baseDomain = baseDomain;
	}
}