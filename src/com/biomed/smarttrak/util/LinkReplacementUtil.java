package com.biomed.smarttrak.util;

import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.biomed.smarttrak.vo.CompanyAttributeVO;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.vo.MarketAttributeVO;
import com.biomed.smarttrak.vo.ProductAttributeVO;
import com.biomed.smarttrak.vo.UpdateVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> LinkFixer.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Fix File Links embedded in Updates, Insights, Company,
 * Product and Market content.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2019
 ****************************************************************************/
public class LinkReplacementUtil extends CommandLineUtil {

	private static final String VALUE_TXT = "value_txt";
	private DBProcessor dbp;
	private String schema;

	enum ReplaceType {UPDATES("biomedgps_update", "update_id", "message_txt"),
						INSIGHT("biomedgps_insight", "insight_id", "content_txt"),
						COMPANY("biomedgps_company_attribute_xr", "company_attribute_id", VALUE_TXT),
						PRODUCT("biomedgps_product_attribute_xr", "product_attribute_id", VALUE_TXT),
						MARKET("biomedgps_market_attribute_xr", "market_attribute_id", VALUE_TXT);
		private String tableNm;
		private String column;
		private String idField;
		private ReplaceType(String tableNm, String idField, String column) {
			this.tableNm = tableNm;
			this.idField = idField;
			this.column = column;
		}
		public String getTableNm() {return tableNm;}
		public String getIdField() {return idField;}
		public String getColumn() {return column;}
	}

	/**
	 * default constructor
	 * @param args
	 */
	public LinkReplacementUtil(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/link-checker.properties");
		loadDBConnection(props);
		schema = (String)props.get(Constants.CUSTOM_DB_SCHEMA);
		dbp = new DBProcessor(dbConn, schema);
	}

	/**
	 * main method
	 * @param args
	 */
	public static void main(String[] args) {
		LinkReplacementUtil eui = new LinkReplacementUtil(args);
		eui.run();
	}

	/**
	 * Perform cleanup against each section individually.
	 */
	public void run() {
		cleanUpdates();
		cleanInsights();
		cleanCompanies();
		cleanProducts();
		cleanMarkets();
	}

	/**
	 * Clean up Market Records
	 */
	private void cleanMarkets() {
		List<MarketAttributeVO> markets = dbp.executeSelect(getLoadSql(ReplaceType.MARKET), null, new MarketAttributeVO());

		log.info(String.format("Loaded %d Market Attribute Records for Processing.", markets.size()));
		for(MarketAttributeVO m : markets) {
			m.setValueText(stripFileLinks(m.getValueText()));
			m.setUpdateDate(Convert.getCurrentTimestamp());
		}

		saveRecords(markets, ReplaceType.MARKET);
	}

	/**
	 * Clean up Product Records
	 */
	private void cleanProducts() {
		List<ProductAttributeVO> products = dbp.executeSelect(getLoadSql(ReplaceType.PRODUCT), null, new ProductAttributeVO());

		log.info(String.format("Loaded %d Product Attribute Records for Processing.", products.size()));
		for(ProductAttributeVO p : products) {
			p.setValueText(stripFileLinks(p.getValueText()));
			p.setUpdateDate(Convert.getCurrentTimestamp());
		}

		saveRecords(products, ReplaceType.PRODUCT);
	}

	/**
	 * Clean up Company Records
	 */
	private void cleanCompanies() {
		List<CompanyAttributeVO> companies = dbp.executeSelect(getLoadSql(ReplaceType.COMPANY), null, new CompanyAttributeVO());

		log.info(String.format("Loaded %d Company Attribute Records for Processing.", companies.size()));
		for(CompanyAttributeVO c : companies) {
			c.setValueText(stripFileLinks(c.getValueText()));
			c.setUpdateDate(Convert.getCurrentTimestamp());
		}

		saveRecords(companies, ReplaceType.COMPANY);
	}

	/**
	 * Clean up Insight Records
	 */
	private void cleanInsights() {
		List<InsightVO> insights = dbp.executeSelect(getLoadSql(ReplaceType.INSIGHT), null, new InsightVO());

		log.info(String.format("Loaded %d Insight Records for Processing.", insights.size()));
		for(InsightVO i : insights) {
			i.setContentTxt(stripFileLinks(i.getContentTxt()));
			i.setUpdateDt(Convert.getCurrentTimestamp());
		}

		saveRecords(insights, ReplaceType.INSIGHT);
	}

	/**
	 * Clean up Update Records.
	 */
	private void cleanUpdates() {
		List<UpdateVO> updates = dbp.executeSelect(getLoadSql(ReplaceType.UPDATES), null, new UpdateVO());

		log.info(String.format("Loaded %d Update Records for Processing.", updates.size()));
		for(UpdateVO u : updates) {
			u.setMessageTxt(stripFileLinks(u.getMessageTxt()));
			u.setUpdateDt(Convert.getCurrentTimestamp());
		}

		saveRecords(updates, ReplaceType.UPDATES);
	}

	/**
	 * Remove <a> tags that have an href attribute containing file: links
	 * @param messageTxt
	 * @return
	 */
	private String stripFileLinks(String html) {
		log.debug(html);
		Document doc = Jsoup.parse(html);
		for (Element e : doc.getElementsByAttributeValueContaining("href", "file:")) {

			//Replace Bad Links with a plain Span Tag.
			Element span = new Element(org.jsoup.parser.Tag.valueOf("span"), "");
			span.addClass("fReplace");
			span.append(e.text());
			e.replaceWith(span);
        }

		String docBody = doc.body().html();
		log.debug(docBody);
		return docBody;
	}

	/**
	 * Saves the Records given.
	 * @param updates
	 */
	private void saveRecords(List<? extends Object> vos, ReplaceType r) {
		List<String> fields = Arrays.asList(r.getIdField(), r.getColumn(), "update_dt");
		for(Object o : vos)
			try {
				dbp.update(o, fields);
			} catch (InvalidDataException | DatabaseException e) {
				log.error("Problem Saving Field.", e);
			}
	}

	/**
	 * Return the Loading Sql.
	 * @param type
	 * @return
	 */
	private String getLoadSql(ReplaceType type) {
		String sql = StringUtil.join(DBUtil.SELECT_CLAUSE,
				type.getIdField(), ", ", type.getColumn(),
				DBUtil.FROM_CLAUSE, schema, type.getTableNm(), 
				DBUtil.WHERE_CLAUSE, type.getColumn(),
				" like '%file:%'");

		log.debug(sql);
		return sql;
	}
}
