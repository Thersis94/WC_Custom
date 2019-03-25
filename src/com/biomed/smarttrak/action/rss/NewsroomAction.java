package com.biomed.smarttrak.action.rss;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.vo.RSSArticleFilterVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedSegment;
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.vo.UserVO.AssigneeSection;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> NewsroomAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action that manages Newsfeed Functionality
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class NewsroomAction extends SBActionAdapter {

	public static final String GROUP_DATA = "groupData";
	public static final String BUCKET_DATA = "bucketData";
	public static final String BUCKET_ID = "bucketId";

	/**
	 * 
	 */
	public NewsroomAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public NewsroomAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("isBucket") && req.hasParameter(BUCKET_ID)) {
			loadBucketArticles(req);
			loadManagers(req);
		} else if(req.hasParameter("isBucket")) {
			loadBuckets(req);
			loadSegmentGroupArticles(req);
		} else if(req.hasParameter("feedGroupId") && !req.hasParameter("isConsole")) {
			loadArticles(req.getParameter("feedGroupId"), req.getParameter("statusCd"), req.getIntegerParameter("page", 0) * 10);

			//Load Managers for assigning rss articles.
			loadManagers(req);
		} else if (!req.hasParameter("statusCd")) {
			loadSegmentGroupArticles(req);
		}
	}

	/**
	 * Load smarttrak managers for use with assigning tickets.
	 * @param req
	 * @throws ActionException
	 */
	protected void loadManagers(ActionRequest req) {
		AccountAction aa = new AccountAction(this.actionInit);
		aa.setAttributes(getAttributes());
		aa.setDBConnection(getDBConnection());
		aa.loadManagerList(req, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA), AssigneeSection.NEWS_ROOM);
	}

	/**
	 * Method loads list of articles tied to a given groupId.
	 * @param req
	 */
	private void loadArticles(String feedGroupId, String statusCd, int offset) {
		List<Object> vals = new ArrayList<>();
		boolean hasStatus = !"ALL".equals(StringUtil.checkVal(statusCd));
		vals.add(feedGroupId);
		if(hasStatus) {
			vals.add(statusCd);
		}

		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		List<RSSArticleVO> articles = dbp.executeSelect(loadArticleSql(hasStatus), vals, new RSSArticleVO(), null, offset, offset+10);
		if(!articles.isEmpty())
			this.putModuleData(articles, articles.size(), false);
	}

	/**
	 * Method returns the sql for loading the article list.
	 * @return
	 */
	private String loadArticleSql(boolean hasStatusCd) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(750);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.rss_article_id, a.rss_entity_id, ");
		sql.append("a.publication_nm, a.article_guid, a.article_url, a.article_source_type, ");
		sql.append("a.attribute1_txt, a.publish_dt, a.create_dt, af.rss_article_filter_id, ");
		sql.append("af.feed_group_id, af.article_status_cd, af.bucket_id, af.match_no, ");
		sql.append("coalesce(af.filter_title_txt, a.title_txt, 'Untitled') as filter_title_txt, ");
		sql.append("coalesce(af.filter_article_txt, a.article_txt, 'No Article Available') as filter_article_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("biomedgps_rss_article a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_rss_filtered_article af ");
		sql.append("on a.rss_article_id = af.rss_article_id ");
		sql.append("where af.feed_group_id = ? ");
		if(hasStatusCd) {
			sql.append("and af.article_status_cd = ? ");
		}
		sql.append("order by a.create_dt desc ");
		return sql.toString();
	}

	/**
	 * Load Buckets for the menu bar.  BucketId ~= profileId. Load distinct list
	 * of bucketIds and then retrieve profile Data for them so we can print
	 * names on the view.
	 * @param req
	 */
	private void loadBuckets(ActionRequest req) {
		List<String> bucketIds = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(loadBucketSql())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				bucketIds.add(rs.getString("bucket_id"));
			}
		} catch (SQLException e) {
			log.error("Error loading Buckets", e);
		}

		//Load list of Profile Data from list of bucket(profile) ids.
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UserDataVO> buckets;
		try {
			buckets = pm.searchProfile(dbConn, bucketIds);
			req.setAttribute("buckets", buckets);
		} catch (DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Sql returns distinct bucketIds.
	 * @return
	 */
	private String loadBucketSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select distinct bucket_id from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_filtered_article where bucket_id is not null");
		return sql.toString();
	}

	/**
	 * Load articles by bucketId and statusCd F
	 * @param req
	 */
	private void loadBucketArticles(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		vals.add(ArticleStatus.F.name());
		if(req.hasParameter(BUCKET_ID)) {
			vals.add(req.getParameter(BUCKET_ID));
		} else {
			vals.add(((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).getProfileId());
		}
		vals.add(req.getIntegerParameter("page", 0) * 10);
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		List<RSSArticleVO> articles = dbp.executeSelect(loadBucketArticlesSql(), vals, new RSSArticleVO());
		if(!articles.isEmpty())
			this.putModuleData(articles, articles.size(), false);
	}

	/**
	 * Builds sql for retrieving list of rssArticles tied to a given Bucket.
	 * @return
	 */
	private String loadBucketArticlesSql() {
		StringBuilder sql = new StringBuilder(250);
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema);
		sql.append("biomedgps_rss_article a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_rss_filtered_article fa ");
		sql.append("on a.rss_article_id = fa.rss_article_id ");
		sql.append("where article_status_cd = ? and fa.bucket_id = ? ");
		sql.append("order by a.create_dt desc ");
		sql.append("limit 10 offset ? ");

		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Load Segment Group Articles for the Feed Landing Page.
	 * @param req
	 * @return 
	 */
	protected void loadSegmentGroupArticles(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		vals.add(ArticleStatus.N.name());
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		req.setAttribute("segments", dbp.executeSelect(loadSegmentGroupArticlesSql(), vals, new RSSFeedSegment()));
	}

	/**
	 * Helper method builds the Segment Group Article Count Sql Query.
	 * @return
	 */
	private String loadSegmentGroupArticlesSql() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(900);
		sql.append("select a.feed_segment_id, a.feed_group_id, a.feed_group_nm, ");
		sql.append("b.FEED_SEGMENT_NM, cast(Count(distinct d.rss_article_id) as int) as article_count, s.section_nm, b.section_id from ");
		sql.append(schema).append("BIOMEDGPS_FEED_GROUP a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("BIOMEDGPS_FEED_SEGMENT b ");
		sql.append("on a.FEED_SEGMENT_ID = b.FEED_SEGMENT_ID ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("BIOMEDGPS_SECTION s ");
		sql.append("on b.section_id = s.section_id and s.parent_id = 'MASTER_ROOT' ");
		sql.append("left outer join ").append(schema).append("biomedgps_rss_filtered_article d ");
		sql.append("on a.feed_group_id = d.feed_group_id and d.article_status_cd = ? ");
		sql.append("and d.create_dt > current_date - 1 ");
		sql.append("group by a.feed_segment_id, a.feed_group_id, a.feed_group_nm, b.feed_segment_id, s.section_nm, s.order_no ");
		sql.append("order by s.order_no, b.order_no, FEED_GROUP_NM");
		log.debug(sql.toString());
		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		updateArticle(req);
	}

	/**
	 * Update and Rss Article.
	 * @param parameter
	 * @param parameter2
	 */
	private void updateArticle(ActionRequest req) throws ActionException {
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		boolean hasBucketId = req.hasParameter(BUCKET_ID);
		RSSArticleFilterVO rss = new RSSArticleFilterVO(req);
		try {
			List<String> fields = new ArrayList<>();
			fields.add("article_status_cd");

			//BucketId may be present on the request for updating.
			if(hasBucketId) {
				fields.add("bucket_id");
				rss.setBucketId(StringUtil.checkVal(req.getParameter(BUCKET_ID), null));

				//Articles can be removed from a bucket.  Allow for removal here.
				if("null".equals(rss.getBucketId())) {
					rss.setBucketId(null);
				}
			}
			fields.add("rss_article_filter_id");
			dbp.executeSqlUpdate(getUpdateArticleSql(hasBucketId), rss, fields);
		} catch (Exception e) {
			log.error("Error updating article status Code", e);
			throw new ActionException(e);
		}
	}

	/**
	 * Builds sql for updating an rssArticle.  Optionally will include bucketId.
	 * @return
	 */
	private String getUpdateArticleSql(boolean hasBucketId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_filtered_article set article_status_cd = ? ");
		if(hasBucketId)
			sql.append(", bucket_id = ? ");
		sql.append("where rss_article_filter_id = ? ");
		return sql.toString();
	}
}
