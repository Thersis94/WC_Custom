package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MarkdownConverter.java<p/>
 * <b>Description: Reads Markdown syntax from specific tables, Converts the syntax to HTML.  
 * 	Writes the HTML back into the database/record.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 22, 2017
 ****************************************************************************/
public class MarkdownConverter extends CommandLineUtil {

	private final String markdownServer;
	
	/**
	 * regex matchers for the graph injections - we're replacing their html markup with ours, which triggers some jquery onload to populate the DOM.
	 */
	private static final String GRAPH_MATCHER = "(?m)(?i)<!-- —\\s?([^—]+)\\s— -->(<br />)?\\s+?(<p>)?<div class=('|\")([^<>]+)?('|\") data-embed=('|\")([^'\"]+)?('|\")>([^<>]+)?</div>(</p>)?";
	private static final String GRAPH_REPLACE = "$3<div class=\"biomed_grid\">\n<a data-graph=\"$8\" data-target=\"#graph-modal\" data-title=\"$1\" data-toggle=\"modal\" data-type=\"TABLE\" href=\"#\"><span class=\"graph-name fa fa-table\"></span>$1</a>\n</div>$11";
	
	
	/**
	 * This enum is what we iterate when the script runs.
	 */
	enum Table {
		COMPANY_ATTR_XR("select a.company_attribute_id, a.value_txt from custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR a inner join custom.BIOMEDGPS_COMPANY_ATTRIBUTE b on a.ATTRIBUTE_ID=b.ATTRIBUTE_ID and b.TYPE_NM='HTML'",
				"UPDATE custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR set value_txt=? where company_attribute_id=?"),
		PROD_ATTR_XR("select a.product_attribute_id, a.value_txt from custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR a inner join custom.BIOMEDGPS_PRODUCT_ATTRIBUTE b on a.ATTRIBUTE_ID=b.ATTRIBUTE_ID and b.TYPE_CD='HTML'",
				"UPDATE custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR set value_txt=? where product_attribute_id=?"),
		MKRT_ATTR_XR("select a.market_attribute_id, a.value_txt from custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR a inner join custom.BIOMEDGPS_MARKET_ATTRIBUTE b on a.ATTRIBUTE_ID=b.ATTRIBUTE_ID and b.TYPE_CD='HTML'",
				"UPDATE custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR set value_txt=? where market_attribute_id=?"),
		INSIGHT_ABS("select insight_id, abstract_txt from custom.BIOMEDGPS_INSIGHT",
				"UPDATE custom.BIOMEDGPS_INSIGHT set abstract_txt=? where insight_id=?"),
		INSIGHT_MAIN("select insight_id, content_txt from custom.BIOMEDGPS_INSIGHT",
				"UPDATE custom.BIOMEDGPS_INSIGHT set content_txt=? where insight_id=?");

		String updateSql;
		String selectSql;

		Table(String sel, String upd) {
			this.selectSql = sel;
			this.updateSql = upd;
		}
		String getUpdateSql() { return updateSql; }
		String getSelectSql() { return selectSql; }
	}


	/**
	 * default constructor
	 * @param args
	 */
	public MarkdownConverter(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/markdown.properties");
		loadDBConnection(props);
		markdownServer = props.getProperty("markdownServer");
	}


	/**
	 * main method
	 * @param args
	 */
	public static void main(String[] args) {
		MarkdownConverter eui = new MarkdownConverter(args);
		eui.run();
		//eui.runTest()
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 * call this method from main() to iterate the enum and execute all tables
	 */
	@Override
	public void run() {
		//iterate the enum, read, convert, and write for each
		for (Table t : Table.values())
			run(t);
	}


	/**
	 * call this from main() to run a single enum/table.
	 * @param t
	 */
	void run(Table t) {
		List<GenericVO> recs = readRecords(t);
		convertRecords(recs);
		saveRecords(recs, t);
	}

	void runTest() {
		List<GenericVO> recs = new ArrayList<>();
		recs.add(new GenericVO("test","[Archive](( \n\n---2010---\n\nIn a Dec 6th release, Stryker " +
				"announced that it has [entered](https://www.smarttrak.net/archives/177/) into a definitive agreement with Olympus Corporation for the sale of its OP-1 product family, which includes OP-1 Implant, OP-1 Putty, Opgenra and Osigraft, for orthopaedic bone applications for $60 million.  Stryker will continue to develop BMP-7 for potential use in osteoarthritis and non-orthopaedic applications. It had been rumored that the Viscogliosi Brothers were also suitors of this technology." +
				"\n\nIn October 29, Stryker [announced](https://www.smarttrak.net/archives/324/?c=118965&p=irol-newsArticle&ID=1489349&highlight=) the acquisition of privately held Porex Surgical for a undisclosed amount.  Porex' reconstructive implants for the face and head expands Stryker's craniomaxillofacial product offerings. "+
				"\n\nIn July 2010, Stryker Spine and ETEX [entered](http://www.prnewswire.com/news-releases/etex-corporation-announces-distribution-agreement-with-stryker-98421089.html) into a non-exclusive distribution agreement in which Stryker will market and sell ETEX' nanocrystalline calcium phosphate products CarriGen, under the brand name BIO MatrX Structure, and ETEX' EquivBone as BIO MatrX Generate. Both products are approved for posterolateral fusion applications.---" +
				"\n\n---2008---\n\nIn 2008, Stryker made an investment in Histogenics, a private company developing NeoCart and VeriCart cartilage repair technologies.---\n" +
				"[Click to hear the interview.](https://vimeo.com/205713795 ) \n\n" +
				"[published](http://pdfaiw.uspto.gov/.aiw?PageNum=0&docid=20160367811) describing\n\n" +
				"<details class=\"pre\"><summary class=\"pre-title\">\n**Wells Fargo Healthcare Conference 2016**\n</summary>\n" +
				"Neuromodulation/interventional pain <a href=\"https://www.smarttrak.net/companies/11242/\">Advanced Biohealing</a> devices (spinal cord stimulation (SCS), dorsal root ganglion (DRG) stimulation, deep brain stimulation (DBS), and radiofrequency therapy (RF))\nHello!\n))"));
		convertRecords(recs);
	}


	/**
	 * reads the key/value pairings from the database using the getSelectSql() query defined in the enum
	 * @param t
	 * @return
	 */
	protected List<GenericVO> readRecords(Table t) {
		List<GenericVO> records = new ArrayList<>();

		try (PreparedStatement ps = dbConn.prepareStatement(t.getSelectSql())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				records.add(new GenericVO(rs.getString(1), rs.getString(2)));
		} catch (SQLException sqle) {
			log.error("could not read records from table " + t.toString(), sqle);
		}
		return records;
	}


	/**
	 * converts the markup to html for each of the records retrieved
	 * @param records
	 */
	protected void convertRecords(List<GenericVO> records) {
		int cnt = 0;
		for (GenericVO vo : records) {
			String markup = StringUtil.checkVal(vo.getValue());
			if (StringUtil.isEmpty(markup))continue;
			markup = fixInnerExpanders(vo.getKey(), markup);
			markup = fixOuterExpanders(markup);
			markup = fixLinks(markup);
			markup = convertViaHttp(vo.getKey(), markup);
			markup = removeExtraTags(markup);
			markup = fixGraphs(markup);
			vo.setValue(markup);
			//log.debug("*********************** converted " + vo.getKey() + " ***********************")
			//log.debug("parsed=" + vo.getValue())
			if (cnt % 100 == 0) log.debug("converted: " + cnt);
			++cnt;
		}
		log.debug("converted total: " + cnt);
	}

	/**
	 * @param markup
	 * @return
	 */
	private String fixGraphs(String markup) {
		return markup.replaceAll(GRAPH_MATCHER, GRAPH_REPLACE);
	}


	protected String convertViaHttp(Object id, String markdown) {
		Map<String, String> params = new HashMap<>();
		params.put("content", markdown);
		try {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			byte[] resp = conn.retrieveDataViaPost(markdownServer,params);				

			//trap all errors generated by Python
			if (200 == conn.getResponseCode())
				return new String(resp);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		log.error("could not get markdown for record id=" + id);
		return markdown;
	}


	/**
	 * do some specific value replacements after calling the Python tool...it put some extra <p> tags 
	 * around our expandables code
	 * @param markup
	 * @return
	 */
	protected String removeExtraTags(String markup) {
		String x = markup.replaceAll("(?i)<p><details class=\"pre\">", "<details class=\"pre\">");
		x = x.replaceAll("(?i)<div class=\"pre-content\"> ?</p>", "<div class=\"pre-content\">");
		x = x.replaceAll("(?i)</details></p>", "</details>");
		return x;
	}


	/**
	 * writes the converted html back to the database using the getUpdateSql() query in the enum
	 * @param records
	 * @param t
	 */
	protected void saveRecords(List<GenericVO> records, Table t) {
		int cnt = 0;
		for (GenericVO vo : records) {
			if (StringUtil.isEmpty((String)vo.getValue())) continue;
			try (PreparedStatement ps = dbConn.prepareStatement(t.getUpdateSql())) {
				ps.setString(1, (String)vo.getValue());
				ps.setString(2, (String)vo.getKey());
				int x = ps.executeUpdate();
				if (x != 1) {
					log.error("not found: " + vo.getKey());
				} else if (cnt % 100 == 0) {
					log.warn("updated: " + cnt);
				}
				++cnt;
			} catch (Exception e) {
				log.error("could not save updates to " + t.toString(), e);
			}
		}
		log.debug("updated total: " + cnt);
	}


	/**
	 * transposes markdown expandable blocks to clickable HTML.
	 * The HTML syntax here aligns with SMT's WYSIWYG plugin.
	 * @param markup
	 * @return
	 */
	protected String fixInnerExpanders(Object id, String markup) {
		StringBuilder sb = new StringBuilder(markup.length()+100);
		String[] arr = markup.split("-{3}");
		if (arr.length == 2) {
			log.error("not enough tokens, this may not be an expandable block.  Not adding expandables to id=" + id + "\n" + markup);
			return markup;
		}
		for (int x=0; x < arr.length; x++) {
			switch (x % 3) {
				case 1: //clickable title
					sb.append("<details class=\"pre\"><summary class=\"pre-title\">\n").append(arr[x]).append("\n</summary>\n");
					break;
				case 2: //expanded text
					sb.append("<div class=\"pre-content\">\n").append(arr[x]).append("\n</div></details>\n");
					break;
				default: //leave what lives outside the expander alone
					sb.append(arr[x]);
			}
		}
		return sb.toString();
	}

	/**
	 * transposes markdown expandable html blocks that wrap other expandable HTML blocks.  
	 * The outer shell has a different syntax than the inner markdown syntax.  The HTML we're converting to
	 * correlates to SMT's WYSWIYG plugin.
	 * @param markup
	 * @return
	 */
	protected String fixOuterExpanders(String markup) {
		String x = markup.replaceAll("(?m)^\\[(.*)?\\]\\(\\(", "\n<details class=\"pre\"><summary class=\"pre-title\">\n$1\n</summary>\n<div class=\"pre-content\">");
		x = x.replaceAll("(?m)^\\)\\)", "\n</div>\n</details>\n");
		return x;
	}


	protected String fixLinks(String markup) {
		String x = markup.replaceAll("(https?://www\\.smarttrak\\.net)?/(companies|markets|products)?/([0-9]+)?/", "/$2/qs/$3");
		x = x.replaceAll("(https?://www\\.smarttrak\\.net)?/archives/([0-9]+)?/", "/insights/qs/$2");
		//some binary paths
		x = x.replaceAll("/media/", "/secBinary/org/BMG_SMARTTRAK/");
		return x;
	}
}