package com.biomed.smarttrak.action.rss.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> AbstractSmarttrakRSSFeed.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Abstract Smarttrak Feed Util.  Holds Common Code that
 * should be available to all feed processors.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.0
 * @since May 21, 2017
 ****************************************************************************/
public abstract class AbstractSmarttrakRSSFeed extends CommandLineUtil {

	public static final String REPLACE_SPAN = "replaceSpan";
	public static final String PUBMED_ARTICLE_URL = "pubmedArticleUrl";
	public static final String PUBMED_ENTITY_ID = "pubmedEntityId";
	/**
	 * @param args
	 */
	public AbstractSmarttrakRSSFeed(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/rss_config.properties");
		loadDBConnection(props);
	}

	/**
	 * Update the RSS Feed Status Code.
	 * @param f
	 */
	protected void updateFeed(SmarttrakRssEntityVO f) {
		try(PreparedStatement ps = dbConn.prepareStatement(buildUpdateFeedStatusStatement())) {
			ps.setInt(1, 0);
			ps.setString(2, f.getRssEntityId());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Helper method that builds the 
	 * @return
	 */
	protected String buildUpdateFeedStatusStatement() {
		StringBuilder sql = new StringBuilder();
		sql.append("update RSS_ENTITY set is_active = ? where rss_entity_id = ?");
		return sql.toString();
	}

	/**
	 * Helper method checks Database for existing Articles.
	 * @param article
	 * @return
	 */
	protected Set<String> getExistingArticles(List<String> articleIds, String entityId) {
		Set<String> ids = new HashSet<>();

		try(PreparedStatement ps = dbConn.prepareStatement(getArticleExistsSql(articleIds.size()))) {
			int i = 1;
			ps.setString(i++, entityId);
			for(String aId : articleIds) {
				ps.setString(i++, aId);
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				ids.add(rs.getString("article_guid"));
			}
		} catch (SQLException e) {
			log.error("Error Checking RSS Article Exists", e);
		}
		return ids;
	}

	/**
	 * Helper method builds the SQL for checking existing Articles.
	 * @return
	 */
	protected String getArticleExistsSql(int size) {
		StringBuilder sql = new StringBuilder(150 + size * 3);
		sql.append("select article_guid from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_RSS_ARTICLE where rss_entity_id = ? and article_guid in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") ");
		log.info(sql.toString());
		return sql.toString();
	}

	/**
	 * Method manages Saving a List of RSSArticles.
	 * @param article
	 */
	protected void storeArticles(List<RSSArticleVO> articles) {
		try {
			new DBProcessor(dbConn, props.getProperty(Constants.CUSTOM_DB_SCHEMA)).executeBatch(getArticleInsertSql(), buildBatchVals(articles));
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}


	/**
	 * Method Manages building the Smarttrak RSS Article Batch Save Statement.
	 * @return
	 */
	protected String getArticleInsertSql() {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_article (rss_article_id, article_status_cd, ");
		sql.append("feed_group_id, rss_entity_id, article_guid, article_txt, ");
		sql.append("filter_article_txt, title_txt, filter_title_txt, article_url, ");
		sql.append("publish_dt, create_dt, publication_nm) values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Method manages building Map of RSSArticle values used in Batch Save Statement.
	 * @param articles
	 * @return
	 */
	protected Map<String, List<Object>> buildBatchVals(List<RSSArticleVO> articles) {
		Map<String, List<Object>> insertValues = new HashMap<>();

		UUIDGenerator uuid = new UUIDGenerator();
		for (RSSArticleVO a : articles) {
			String xrId = uuid.getUUID();
			List<Object> insertData = new ArrayList<>();
			insertData.addAll(Arrays.asList(xrId, a.getArticleStatusCd(), a.getFeedGroupId(), a.getRssEntityId()));
			insertData.addAll(Arrays.asList(a.getArticleGuid(), a.getArticleTxt(), a.getFilterArticleTxt(), a.getTitleTxt()));
			insertData.addAll(Arrays.asList(a.getFilterTitleTxt(), a.getArticleUrl(), a.getPublishDt(), Convert.getCurrentTimestamp()));
			insertData.addAll(Arrays.asList(a.getPublicationName()));
			insertValues.put(xrId, insertData);
		}

		return insertValues;
	}

}