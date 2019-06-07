package com.biomed.smarttrak.admin.report;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map.Entry;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.vo.GapCellVO;
import com.biomed.smarttrak.vo.GapColumnVO;
import com.biomed.smarttrak.vo.GapCompanyVO;
import com.biomed.smarttrak.vo.GapTableVO;
import com.biomed.smarttrak.vo.GapTableVO.ColumnKey;
import com.siliconmtn.data.report.PDFReport;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
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

	public static final String REPORT_TITLE = "Gap Analysis Report.pdf";
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

		return doc.toString();
	}

	/**
	 * Build the Body of the HTML Doc.
	 * @param doc
	 */
	private void generateBody(StringBuilder doc) {
		doc.append("<body>");
		generateBodyTable(doc);
		doc.append("</body>");
	}

	/**
	 * Build the Body of the Table.
	 * @return
	 */
	private void generateBodyTable(StringBuilder doc) {
		int count = 0;
		generateHeader(doc);
			for(Entry<String, GapCompanyVO> c : table.getCompanies().entrySet()) {
				GapCompanyVO comp = c.getValue();
				if(comp.getPortfolioNo() > 0) {
					if (count >= 27) {
						generateFooter(doc, false);
						generateHeader(doc);
						count = 0;
					}
					
					
					StringBuilder url = new StringBuilder();
					url.append(site.getFullSiteAlias()).append(Section.COMPANY.getPageURL());
					url.append(qs).append(comp.getCompanyId());

					String name = StringUtil.isEmpty(comp.getShortCompanyName())? comp.getCompanyName() : comp.getShortCompanyName();
					doc.append("<tr>");
					doc.append("<th class=\"fix\">");
					doc.append("<div class=\"wrap\">");
					doc.append("<a href=\"").append(url.toString()).append("\" title=\"").append(name);
					doc.append("\">").append(name).append("</a>");
					doc.append("</div></th>");
					for(GapCellVO cell : comp.getCells()) {
						doc.append("<td><div class=\"pill\">");
						doc.append("<div class=\"left_round ").append(cell.getUsReg().getClassName());
						doc.append("\">").append("&nbsp;");
						doc.append("</div><div class=\"right_round ").append(cell.getOusReg().getClassName());
						doc.append("\">").append("&nbsp;").append("</div></div></td>");
					}
					doc.append("</tr>");
					count++;
				}
			}
		generateFooter(doc, true);
	}

	private void generateFooter(StringBuilder doc, boolean finalFooter) {
		doc.append("</tbody>");
		doc.append("</table>");
		doc.append("</div></div></div>");
		doc.append("<div class='footer");
		// Last footer should not cause a page break
		if (finalFooter) {
			doc.append(" no-break");
		}
		doc.append("'><span class='left'>SmartTRAK Business Intelligence</span>");
		doc.append("<span class='right'>Copyright &copy; 2008-");
		doc.append(Calendar.getInstance().get(Calendar.YEAR));
		doc.append("BiomedGPS LLC</span></div>");
	}

	private void generateHeader(StringBuilder doc) {
		doc.append("<div id=\"gap_analysis\"><div id=\"tableBlock\">");
		doc.append("<h1 class='underline-full'>SmartTRAK Gap Analysis</h1>");
		doc.append("");
		doc.append("<div class=\"gapoffset\">");
		doc.append("<table class='table gap_table headTable'>");
		doc.append("<p>");
		Collection<GapColumnVO> parents = table.getHeaderCols().get(ColumnKey.GPARENT.name()).values();
		int i = 0;
		for(GapColumnVO g : parents) {
			if (i > 0) doc.append(", ");
			doc.append(g.getName());
			i++;
		}
		doc.append("</p>");
		generateHeadersTable(doc);
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
		doc.append("<tr>");
		Collection<GapColumnVO> children = table.getHeaderCols().get(ColumnKey.CHILD.name()).values();
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
		doc.append("<tr>");
		if (isGParent) {
			doc.append("<th rowspan='3'>");
			doc.append("<table class='legend pull-left'>");
			doc.append("<tbody><tr><td class='pill'><div class='left_round usa'>US</div><div class='right_round ousa'>OUS</div></td><td><span>Available</span></td></tr>");
			doc.append("<tr><td class='pill'><div class='left_round usid'>US</div><div class='right_round ousid'>OUS</div></td><td><span>Development</span></td></tr>");
			doc.append("<tr><td class='pill'><div class='left_round usd'>US</div><div class='right_round ousd'>OUS</div></td><td><span>Discontinued</span></td></tr>");
			doc.append("<tr><td class='pill'><div class='left_round usg'>US</div><div class='right_round ousg'>OUS</div></td><td><span>Product Gaps</span></td></tr>");
			doc.append("</tbody></table>");
			doc.append("</th>");
		}
		ColumnKey key = isGParent ? ColumnKey.GPARENT : ColumnKey.PARENT;
		Collection<GapColumnVO> parents = table.getHeaderCols().get(key.name()).values();
		for(GapColumnVO g : parents) {
			doc.append("<th ");
			if(isGParent) {
				doc.append("rowspan='").append(g.getRowSpan()).append("' ");
			}
			doc.append("colspan='").append(g.getColSpan()).append("' class='parents col-group-").append(g.getColGroupNo());
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
		buildCssBlock(sb);
		int width = 210;
		Collection<GapColumnVO> children = table.getHeaderCols().get(ColumnKey.CHILD.name()).values();
		width += children.size() * 73;
		
		sb.append("<style>@page{size: ").append(width).append("px 1200px; margin:0mm;}</style>");
		sb.append("</head>");
	}

	/**
	 * Build the DocType Element.
	 * @return
	 */
	private void getDocType(StringBuilder doc) {
		doc.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r\n");
	}
	
	/**
	 * Retrieve the gap_report css file and transcribe it to the stringbuilder
	 * in order to circumvent issues itextrenderer has with getting files from
	 * strictly https sites.
	 * @param sb
	 */
	private void buildCssBlock(StringBuilder sb) {
		try {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			StringBuilder cssPage = new StringBuilder(120);
			cssPage.append(site.getFullSiteAlias()).append("/binary/themes/");
			cssPage.append(site.getTheme().getPageLocationName()).append("/scripts/gap_report.css");
			byte[] cssData = conn.retrieveData(cssPage.toString());
			sb.append("<style>").append(new String(cssData)).append("</style>");
		} catch (IOException e) {
			log.error("Unable to retrieve style sheet.", e);
		}
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