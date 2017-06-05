package com.biomed.smarttrak.admin.report;

import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.vo.GapCellVO;
import com.biomed.smarttrak.vo.GapColumnVO;
import com.biomed.smarttrak.vo.GapCompanyVO;
import com.biomed.smarttrak.vo.GapTableVO;
import com.biomed.smarttrak.vo.GapTableVO.ColumnKey;
import com.siliconmtn.data.report.PDFReport;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: GapAnalysisReportVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Builds HTML Report 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 15, 2017
 ****************************************************************************/
public class GapAnalysisReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = -4817102241031112699L;

	private static final String REPORT_TITLE = "Gap Analysis Report.pdf";
	private GapTableVO table;
	private SiteVO site;
	private StringEncoder se;
	private String qs;

	public GapAnalysisReportVO(String qs) {
		this.qs = qs;
		se = new StringEncoder();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		PDFReport rpt = new PDFReport(site.getFullSiteAlias());
		rpt.setFileName(REPORT_TITLE);
		rpt.setData(StringUtil.replace(buildDocument(), " & ", " &amp; "));
		return rpt.generateReport();
	}

	/**
	 * Build the HTML for the Document.
	 * @return
	 */
	private String buildDocument() {
		StringBuilder doc = new StringBuilder(10000);
		getDocType(doc);
		doc.append("<html lang='en'>");
		getHeader(doc);
		generateBody(doc);
		doc.append("</html>");
		log.debug(doc.toString());

		return doc.toString();
	}

	/**
	 * Build the Body of the HTML Doc.
	 * @param doc
	 */
	private void generateBody(StringBuilder doc) {
		doc.append("<body><div id=\"gap_analysis\"><div id=\"tableBlock\">");
		doc.append("<h1>SmartTRAK Gap Analysis</h1>");
		doc.append("<div class=\"gapoffset\">");
		doc.append("<table class='table gap_table' id='headTable'>");
		generateHeadersTable(doc);
		generateBodyTable(doc);
		doc.append("</table>");
		generateFooterTable(doc);
		doc.append("</div></div></div></body>");
	}

	/**
	 * Build the Footer for the Doc.
	 * @param doc
	 */
	private void generateFooterTable(StringBuilder doc) {
		doc.append("<p class='footer'>&#169; ").append(Calendar.getInstance().get(Calendar.YEAR));
		doc.append(" SmartTRAK ");
		doc.append("</p>");
	}

	/**
	 * Build the Body of the Table.
	 * @return
	 */
	private void generateBodyTable(StringBuilder doc) {
		doc.append("<tbody>");
			for(Entry<String, GapCompanyVO> c : table.getCompanies().entrySet()) {
				GapCompanyVO comp = c.getValue();
				if(comp.getPortfolioNo() > 0) {
					StringBuilder url = new StringBuilder();
					url.append(site.getFullSiteAlias()).append(Section.COMPANY.getPageURL());
					url.append(qs).append(comp.getCompanyId());

					doc.append("<tr>");
					doc.append("<th class=\"fix\">");
					doc.append("<div class=\"wrap\">");
					doc.append("<a href=\"").append(url.toString()).append("\" title=\"").append(comp.getCompanyName());
					doc.append("\">").append(comp.getShortCompanyName()).append("</a>");
					doc.append("</div></th>");
					for(GapCellVO cell : comp.getCells()) {
						doc.append("<td><div class=\"pill\">");
						doc.append("<div class=\"left_round ").append(cell.getUsReg().getClassName());
						doc.append("\">").append("&nbsp;");
						doc.append("</div><div class=\"right_round ").append(cell.getOusReg().getClassName());
						doc.append("\">").append("&nbsp;").append("</div></div></td>");
					}
					doc.append("</tr>");
				}
			}
		doc.append("</tbody>");
	}

	/**
	 * Build the Headers for the Table.
	 * @return
	 */
	private void generateHeadersTable(StringBuilder doc) {
		doc.append("<thead>");
		buildScaffolding(doc);
		buildParentRow(doc, true);
		buildParentRow(doc, false);
		buildChildRow(doc);

		doc.append("</thead>");
	}

	/**
	 * Build the Child Row of Headers.  The "True" Headers.
	 * @param doc
	 */
	private void buildChildRow(StringBuilder doc) {
		doc.append("<tr><th class='fix'><div>Limit results by subsegment<i class='fa ");
		doc.append("fa-long-arrow-right'>&nbsp;</i></div></th>");
		List<GapColumnVO> children = table.getHeaderCols().get(ColumnKey.CHILD.name());

		for(GapColumnVO c : children) {
			doc.append("<th class='colSort ");
			doc.append("col-group-").append(c.getColGroupNo());
			doc.append("'>");
			doc.append(se.encodeValue(StringUtil.checkVal(c.getName()))).append("</th>");
		}
		doc.append("</tr>");
	}

	/**
	 * Build the Parent Rows for the Gap Table.
	 * @param doc
	 */
	private void buildParentRow(StringBuilder doc, boolean isGParent) {
		doc.append("<tr><th class='fix'></th>");

		ColumnKey key = isGParent ? ColumnKey.GPARENT : ColumnKey.PARENT;
		List<GapColumnVO> parents = table.getHeaderCols().get(key.name());

		for(GapColumnVO g : parents) {
			doc.append("<th ");
			if(isGParent) {
				doc.append("rowspan='").append(g.getRowSpan()).append("' ");
			}
			doc.append("colspan='").append(g.getColSpan()).append("' class='parents ");
			doc.append("'><span class='wrap'>").append(se.encodeValue(StringUtil.checkVal(g.getName()))).append("</span></th>");
		}
		doc.append("</tr>");
	}

	/**
	 * Build the Scaffolding Row of Columns for Column Sizing.
	 * @param doc
	 */
	private void buildScaffolding(StringBuilder doc) {
		doc.append("<tr class=\"scaffolding\"><th class='fix'></th>");
		int s = table.getScaffolding();
		for(int i = 0; i < s; i++) {
			doc.append("<th></th>");
		}
		doc.append("</tr>");
		
	}

	/**
	 * Builds the header map for the report
	 * @param sb
	 */
	protected void getHeader(StringBuilder sb) {
		sb.append("<head>");
		sb.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\">");
		sb.append("<title>").append(REPORT_TITLE).append("</title>");
		buildCssLinks(sb);
		sb.append("</head>");
	}

	/**
	 * Helper method that builds CSS Links for the Report.
	 * @return
	 */
	private void buildCssLinks(StringBuilder sb) {
		StringBuilder modCom = new StringBuilder(150);
		modCom.append(site.getFullSiteAlias()).append("/binary/themes/");
		modCom.append(site.getTheme().getPageLocationName()).append("/scripts/gap_report.css");
		buildCSSLink(sb, "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css");
		buildCSSLink(sb, "//maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css");
		buildCSSLink(sb, modCom.toString());
	}

	/**
	 * Build a CSS Link for the Document.
	 * @param bootstrapCss
	 * @return
	 */
	private void buildCSSLink(StringBuilder sb, String bootstrapCss) {
		sb.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"").append(bootstrapCss).append("\">");
	}

	/**
	 * Build the DocType Element.
	 * @return
	 */
	private void getDocType(StringBuilder doc) {
		doc.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r\n");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		if(o instanceof GapTableVO) {
			table = (GapTableVO)o;
		}
	}

	/**
	 * @param site the site to set
	 */
	public void setSite(SiteVO site) {
		this.site = site;
	}
}