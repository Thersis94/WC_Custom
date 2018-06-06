package com.rezdox.data;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.ProjectMaterialVO;
import com.rezdox.vo.ProjectVO;
import com.siliconmtn.data.report.PDFReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <p><b>Title</b>: InvoiceReportPDF.java</p>
 * <p><b>Description:</b> The PDF report tied to project invoices.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Mar 30, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class InvoiceReportPDF extends AbstractSBReportVO {

	private static final long serialVersionUID = 6305052726273927305L;

	private static final String TD = "<td>";
	private static final String CL_TD = "</td>";
	private static final String CL_TD_TR = "</td></tr>";
	private static final String COST_CELL = "<td class='underline costValue' align='right'><label class='subtotal'>";
	private static final String CL_LABEL_TD_TR = "</label>" + CL_TD_TR;

	private transient ProjectVO project;
	private String fqdn;
	private String imageBase;
	private NumberFormat currency = NumberFormat.getCurrencyInstance();
	private NumberFormat percent = NumberFormat.getPercentInstance();

	public InvoiceReportPDF(String fqdn, String relaImgBase) {
		super();
		this.fqdn = fqdn;
		this.imageBase = fqdn + relaImgBase;
		setFileName("Invoice.pdf");
		currency.setMaximumFractionDigits(2);
		percent.setMaximumFractionDigits(2);
	}


	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		PDFReport rpt = new PDFReport(fqdn);
		rpt.setFileName(getFileName());
		rpt.setData(buildDocument().replaceAll("&[^#|amp;]", "&amp;")); //escape &'s not already escaped, or part of html char sequences (&#39;)
		return rpt.generateReport();
	}


	/**
	 * assemble the document pieces - doctype, header, body, etc.
	 * @return
	 */
	private String buildDocument() {
		StringBuilder doc = new StringBuilder(20000);
		doc.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\r\n");
		doc.append("<html lang='en'>");
		doc.append("<head>");
		doc.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\">");
		doc.append("<title>").append(getFileName()).append("</title>");
		String modulesCss = StringUtil.join(fqdn, "/binary/themes/CUSTOM/REZDOX/REZDOX_MEMBER/scripts/modules.css");
		doc.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"").append(modulesCss).append("\">");
		doc.append("</head><body>");
		appendBody(doc);
		doc.append("</body></html>");
		
		return doc.toString();
	}


	/**
	 * builds the main body of the report (an html table) and appends it onto the StringBuilder
	 * @param sb
	 */
	private void appendBody(StringBuilder sb) {
		BusinessVO biz = project.getBusiness();

		sb.append("<table class=\"invoice\" width='100%'>");
		sb.append("<tr class='heading'>");
		if (!StringUtil.isEmpty(biz.getPhotoUrl())) {
			sb.append("<td width='50'><img src=\"").append(imageBase).append(biz.getPhotoUrl()).append("\" width=\"50\" alt=\"logo\" /></td>");
			sb.append("<td><h1 class=\"black\">").append(biz.getBusinessName()).append("</h1></td>");
		} else {
			sb.append("<td colspan='2'><h1 class=\"black\">").append(biz.getBusinessName()).append("</h1></td>");
		}
		sb.append("<td width='200' align='right'><h2>INVOICE</h2></td></tr>\r\n");

		sb.append("<tr>");
		sb.append("<td colspan='2' class='black'>");
		sb.append(biz.getAddress()).append(", ").append(biz.getCity()).append(", ");
		sb.append(biz.getState()).append(" ").append(biz.getZipCode()).append(CL_TD);
		sb.append("<td align='right' class='black'>");
		sb.append(Convert.formatDate(new Date(), Convert.DATE_SHORT_MONTH)).append("</td></tr>\r\n");

		sb.append("<tr class='customer'>");
		sb.append("<td colspan='3' align='center'>");
		sb.append("<span class=\"name black\">").append(project.getOwner().getFirstName()).append(" ");
		sb.append(project.getOwner().getLastName()).append("</span><br/>");
		sb.append(project.getOwner().getAddress()).append("<br>");
		sb.append(project.getOwner().getCity()).append(", ").append(project.getOwner().getState());
		sb.append(" ").append(project.getOwner().getZipCode()).append(CL_TD_TR);

		//PROJECT section + costs
		sb.append("<tr><td colspan='3'><h2>PROJECT</h2></td></tr>\r\n");

		sb.append("<tr><td colspan='3'>");
		sb.append("<table class='prods' width='100%'><tr class='th'>");

		sb.append("<td class='type'>Type</td>");
		sb.append("<td class='title'>Title</td>");
		sb.append("<td class='summary'>Project Summary</td>");
		sb.append("<td class='cost'>Cost</td></tr>\r\n");

		sb.append("<tr class='project row_1'>");
		sb.append(TD).append(project.getProjectTypeName()).append(CL_TD);
		sb.append(TD).append(project.getProjectName()).append(CL_TD);
		sb.append(TD).append(project.getDescriptionText().replaceAll("\\r", "<br>")).append(CL_TD);
		sb.append(TD).append(currency.format(project.getTotalNo())).append(CL_TD_TR);

		//this works to put summary on it's own line, if they decide to move it
		//sb.append("<tr><td colspan='3' class='summary'>").append(project.getDescriptionText().replaceAll("\\r", "<br>")).append(CL_TD_TR)

		sb.append("</table></td></tr>\r\n");

		addCostTable(sb, project.getTotalNo(), project.getProjectDiscountNo(), project.getAppliedDiscount(), 
				project.getProjectTaxNo(), project.getAppliedTax(), project.getAppliedProjectTotal());

		// MATERIALS section + costs
		List<ProjectMaterialVO> materials = project.getMaterials();
		if (materials != null && !materials.isEmpty()) {
			sb.append("<tr><td colspan='3'><h2>PRODUCT</h2></td></tr>\r\n");
			sb.append("<tr><td colspan='3'>");
			sb.append("<table class='mats' width='100%'><tr class='th'>");
	
			sb.append("<td class='product'>Product</td>");
			sb.append("<td class='qty'>Qty</td>");
			sb.append("<td class='amount'>Amount</td>");
			sb.append("<td class='cost'>Cost</td></tr>\r\n");
	
			int ctr = 0;
			for (ProjectMaterialVO mat : materials) {
				double itemTotal = mat.getCostNo() * mat.getQuantityNo();
				sb.append("<tr class='row_").append(ctr % 2).append("'>");
				sb.append(TD).append(mat.getMaterialName()).append(CL_TD);
				sb.append(TD).append(mat.getQuantityNo()).append(CL_TD);
				sb.append(TD).append(currency.format(mat.getCostNo())).append(CL_TD);
				sb.append(TD).append(currency.format(itemTotal)).append(CL_TD_TR);
				++ctr;
			}
	
			sb.append("</table></td></tr>\r\n");
	
			addCostTable(sb, project.getMaterialSubtotal(), project.getMaterialDiscountNo(), project.getAppliedMaterialDiscount(), 
					project.getMaterialTaxNo(), project.getAppliedMaterialTax(), project.getAppliedMaterialTotal());
		}
		
		// invoice grand total
		sb.append("<tr><td colspan='3'><br><table class='cost grandtotal' align='right'>");
		sb.append("<tr><td align='right' class='costLabel'><label>Invoice Grand Total:</label></td>");
		sb.append("<td class='box' align='right'><label class='subtotal'>").append(currency.format(project.getInvoiceTotal())).append(CL_LABEL_TD_TR);
		sb.append("</table></td></tr></table>\r\n");
	}


	/**
	 * reused method to print the cost table, over on the right of the invoice.
	 * Works for Project as well as Product breakdowns
	 * @param sb
	 * @param totalNo
	 * @param projectDiscountNo
	 * @param appliedDiscount
	 * @param projectTaxNo
	 * @param appliedTax
	 * @param appliedProjectTotal
	 */
	private void addCostTable(StringBuilder sb, double totalNo, double discountNo, 
			double appliedDiscount, double taxNo, double appliedTax, double appliedTotal) {

		sb.append("<tr><td colspan='3'><br/><table class='cost' align='right'>");
		sb.append("<tr><td align='right' class='costLabel'><label class='subtotal'>Subtotal:</label></td>");
		sb.append(COST_CELL).append(currency.format(totalNo)).append(CL_LABEL_TD_TR);
		if (discountNo > 0) {
			sb.append("<tr><td align='right' class='costLabel'><label class='subtotal'>Discount (").append(percent.format(discountNo)).append("):</label></td>");
			sb.append(COST_CELL).append("- ").append(currency.format(appliedDiscount)).append(CL_LABEL_TD_TR);
		}
		if (taxNo > 0) {
			sb.append("<tr><td align='right' class='costLabel'><label class='subtotal'>Tax (").append(percent.format(taxNo)).append("):</label></td>");
			sb.append(COST_CELL).append(currency.format(appliedTax)).append(CL_LABEL_TD_TR);
		}
		sb.append("<tr><td align='right' class='costLabel' nowrap><label class='subtotal emphColor'>Project Total:</label></td>");
		sb.append(COST_CELL).append(currency.format(appliedTotal)).append(CL_LABEL_TD_TR);
		sb.append("</table><br/></td></tr>\r\n");
	}


	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if (o instanceof Collection) {
			Collection<ProjectVO> c = (Collection<ProjectVO>) o;
			if (!c.isEmpty())
				project = c.iterator().next();
		}
	}
}
