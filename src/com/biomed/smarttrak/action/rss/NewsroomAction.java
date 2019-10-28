package com.biomed.smarttrak.action.rss;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.vo.RSSArticleFilterVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSBucketVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedSegment;
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.util.RSSArticleIndexer;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO.AssigneeSection;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

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
	public static final String STATUS_CD = "statusCd";
	private static final String FEED_GROUP_ID = "feedGroupId";
	private static final String MY_BUCKET_COUNT = "myBucketCount";
	private static final String BULK_ACTION = "bulkAction";
	private static final String SEGMENTS_ATTR = "segments";

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

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/**
	 * 
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("reloadCounts")) {
			List<RSSFeedSegment> segments = loadSegmentGroupArticles(req);
			this.putModuleData(segments, segments.size(), false);
		} else if(req.hasParameter("isBucket") && req.hasParameter(BUCKET_ID)) {
			loadBucketArticles(req);
			loadManagers(req);
			req.setAttribute(SEGMENTS_ATTR, loadSegmentGroupArticles(req));
		} else if(req.hasParameter("isBucket")) {
			loadBuckets(req);
			loadMyCounts(req);
			loadManagers(req);
			req.setAttribute(SEGMENTS_ATTR, loadSegmentGroupArticles(req));
		} else if(req.hasParameter(FEED_GROUP_ID) && !req.hasParameter("isConsole")) {
			//Get the Filtered Updates according to Request.
			getFilteredArticles(req);

			ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			SolrResponseVO resp = (SolrResponseVO)mod.getActionData();
			List<Object> params = getIdsFromDocs(resp);

			if(!params.isEmpty()) {
				List<RSSArticleVO> articles = loadDetails(params, req);
				log.debug("DB Count " + articles.size());
				if(!articles.isEmpty()) {
					this.putModuleData(articles, articles.size(), false);
				} else {
					this.putModuleData(Collections.emptyList(), 0, false);
				}
			} else {
				this.putModuleData(Collections.emptyList(), 0, false);
			}

			//Load Managers for assigning rss articles.
			req.setAttribute(SEGMENTS_ATTR, loadSegmentGroupArticles(req));
		} else if(!req.hasParameter("amid")) {
			loadMyCounts(req);
			req.setAttribute(SEGMENTS_ATTR, loadSegmentGroupArticles(req));

		}
	}

	/**
	 * Manage Loading Current Users Bucket Count.
	 * @param req
	 */
	private void loadMyCounts(ActionRequest req) {
		String profileId = ((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).getProfileId();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select count(bucket_id) from ").append(getCustomSchema());
		sql.append("biomedgps_rss_filtered_article ").append(DBUtil.WHERE_CLAUSE);
		sql.append("bucket_id = ?");
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				req.setParameter(MY_BUCKET_COUNT, rs.getString("count"));
			} else {
				req.setParameter(MY_BUCKET_COUNT, "0");
			}
		} catch(Exception e) {
			log.error("Unable to retrtieve Users Saved Article Count.", e);
			req.setParameter(MY_BUCKET_COUNT, "0");
		}
	}

	/**
	 * @param params
	 * @return
	 */
	private List<RSSArticleVO> loadDetails(List<Object> vals, ActionRequest req) {
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(loadFilteredArticleSql(vals.size(), req), vals, new RSSArticleVO());
	}

	/**
	 * @param size
	 * @return
	 */
	private String loadFilteredArticleSql(int size, ActionRequest req) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(800);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.rss_article_id, a.rss_entity_id, ");
		sql.append("a.publication_nm, a.article_guid, a.article_url, a.article_source_type, ");
		sql.append("a.attribute1_txt, a.publish_dt, a.create_dt, af.rss_article_filter_id, ");
		sql.append("af.feed_group_id, af.article_status_cd, af.bucket_id, af.match_no, ");
		sql.append("coalesce(af.filter_title_txt, a.title_txt, 'Untitled') as filter_title_txt, ");
		sql.append("coalesce(af.filter_article_txt, a.article_txt, 'No Article Available') as filter_article_txt, ");
		sql.append("af.complete_flg, affiliation_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("biomedgps_rss_article a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_rss_filtered_article af ");
		sql.append("on a.rss_article_id = af.rss_article_id ");
		sql.append("where af.rss_article_filter_id in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by ");
		if(req.hasParameter("fieldSort")) {
			switch(req.getParameter("fieldSort")) {
				case "title":
					sql.append("filter_title_txt ");
					break;
				case "publishDate":
				default:
					sql.append("a.create_dt ");
					break;
			}
		} else {
			sql.append("a.create_dt ");
		}
		sql.append(StringUtil.checkVal(req.getParameter("sortDirection"), "desc"));
		return sql.toString();
	}

	/**
	 * Get all the document ids from the solr documents and remove the
	 * custom identifier if it is present.
	 * @param resp
	 * @return
	 */
	private List<Object> getIdsFromDocs(SolrResponseVO resp) {
		List<Object> params = new ArrayList<>();

		for (SolrDocument doc : resp.getResultDocuments()) {
			params.add((String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
		}
		return params;
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
	 * Helper method that returns list of Updates filtered by ActionRequest
	 * parameters.
	 * @param req
	 * @param dir 
	 * @param order
	 * @return
	 * @throws ActionException 
	 */
	private void getFilteredArticles(ActionRequest req) throws ActionException {
		//parse the requet object
		setSolrParams(req);
	
		// Pass along the proper information for a search to be done.
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());

		// Build the solr action
		ActionInterface sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
	}

	/**
	 * Set all paramters neccesary for solr to be able to properly search for the desired documents.
	 * @param req
	 * @param dir 
	 * @param order
	 */
	private void setSolrParams(ActionRequest req) {
		int rpp = Convert.formatInteger(req.getParameter("limit"), 10);
		req.setParameter("rpp", StringUtil.checkVal(rpp));

		//build a list of filter queries
		List<String> fq = new ArrayList<>();
		String feedGroupId = req.getParameter(FEED_GROUP_ID);
		String bucketId = req.getParameter(BUCKET_ID);

		String statusCd = req.getParameter(STATUS_CD, ArticleStatus.N.name());

		/*
		 * If filtering by complete articles, ignore status, only look at the flag.
		 * Otherwise if we're not looking for all, apply status.
		 */
		if("C".equals(statusCd)) {
			fq.add("completeFlag_i:1");
		} else if(!"ALL".equals(statusCd)) {
			fq.add("articleStatus_s:" + statusCd);
			fq.add("completeFlag_i:0");
		}
		if (!StringUtil.isEmpty(feedGroupId))
			fq.add("feedGroupId_s:" + feedGroupId);

		if (!StringUtil.isEmpty(bucketId)) {
			fq.add("bucketId_s:" + bucketId);
		}

		//Check for ids we want to ignore. Is managed on front end with a timer.
		if(req.hasParameter("skipIds")) {
			for(String skipId : req.getParameter("skipIds").split(",")) {
				fq.add("!documentId:" + skipId);
			}
		}

		req.setParameter("fq", fq.toArray(new String[fq.size()]), true);
		req.setParameter("allowCustom", "true");
	}

	/**
	 * Method for loading all filtered articles in a feedGroupId.  Used to load Articles for
	 * Solr.
	 * @param feedGroupId
	 * @return
	 */
	public List<SolrDocumentVO> loadAllArticles(String feedGroupId, List<String> rssFilteredArticleIds) {
		List<Object> vals = new ArrayList<>();
		boolean hasFilteredArticleId = rssFilteredArticleIds != null && !rssFilteredArticleIds.isEmpty();
		boolean hasGroupId = !StringUtil.isEmpty(feedGroupId);

		if(hasGroupId) {
			vals.add(feedGroupId);
		}

		if(hasFilteredArticleId) {
			vals.addAll(rssFilteredArticleIds);
		}

		//Check to prevent overloading the system with all Articles.
		if(!hasGroupId && !hasFilteredArticleId) {
			return Collections.emptyList();
		}

		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		List<SolrDocumentVO> docs = dbp.executeSelect(loadFilterArticleSql(hasGroupId, hasFilteredArticleId ? rssFilteredArticleIds.size() : 0), vals, new RSSArticleFilterVO());
		log.debug(dbp.getExecutedSql());
		return docs;
	}

	/**
	 * Build Filtered Article Query
	 * @return
	 */
	private String loadFilterArticleSql(boolean hasGroupId, int articleCount) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(800);
		sql.append(DBUtil.SELECT_CLAUSE).append("a.rss_article_id, ");
		sql.append("a.article_source_type, ");
		sql.append("a.publish_dt, a.create_dt, af.rss_article_filter_id, ");
		sql.append("af.feed_group_id, af.article_status_cd, ");
		sql.append("coalesce(af.filter_title_txt, a.title_txt, 'Untitled') as filter_title_txt, ");
		sql.append("coalesce(af.filter_article_txt, a.article_txt, 'No Article Available') as filter_article_txt, ");
		sql.append("af.complete_flg, a.full_article_txt, a.affiliation_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("biomedgps_rss_article a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_rss_filtered_article af ");
		sql.append("on a.rss_article_id = af.rss_article_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		if(hasGroupId)
			sql.append("and af.feed_group_id = ? ");

		if(articleCount > 0) {
			sql.append("and af.rss_article_filter_id in (");
			DBUtil.preparedStatmentQuestion(articleCount, sql);
			sql.append(") ");
		}
		sql.append(DBUtil.ORDER_BY).append("a.create_dt desc ");

		return sql.toString();
	}

	/**
	 * Load Buckets for the menu bar.  BucketId ~= profileId. Load distinct list
	 * of bucketIds and then retrieve profile Data for them so we can print
	 * names on the view.
	 * @param req
	 */
	@SuppressWarnings("unchecked")
	private void loadBuckets(ActionRequest req) {
		//Load list of Profile Data from list of bucket(profile) ids.
		loadManagers(req);
		List<AccountVO> bucketUsers = (List<AccountVO>)req.getAttribute(AccountAction.MANAGERS);
		Map<String, Integer> bucketCounts = loadBucketCounts();
		List<RSSBucketVO> buckets = new ArrayList<>();
		for(AccountVO a: bucketUsers) {
			int numArticles = 0;
			if(bucketCounts.containsKey(a.getOwnerProfileId())) {
				numArticles = bucketCounts.get(a.getOwnerProfileId());
			}
			buckets.add(new RSSBucketVO(a, numArticles));
		}

		req.setAttribute("buckets", buckets);
	}

	/**
	 * Load the number of articles tied to a bucket Id (User)
	 */
	private Map<String, Integer> loadBucketCounts() {
		Map<String, Integer> counts = new HashMap<>();

		try(PreparedStatement ps = dbConn.prepareStatement(buildBucketCountSql())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				counts.put(rs.getString("bucket_id"), rs.getInt("article_count"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return counts;
	}

	/**
	 * Sql returns distinct bucketIds.
	 * @return
	 */
	private String buildBucketCountSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select count(bucket_id) as article_count, bucket_id from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_filtered_article where bucket_id is not null ");
		sql.append("group by bucket_id");
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
	 * @return 
	 */
	protected List<RSSFeedSegment> loadSegmentGroupArticles(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		String statusCd = req.getParameter(STATUS_CD);
		if("C".equals(statusCd)) {
			vals.add(1);
		} else if(!"ALL".equals(statusCd)) {
			vals.add(EnumUtil.safeValueOf(ArticleStatus.class, req.getParameter(STATUS_CD), ArticleStatus.N).name());
			vals.add(0);
		}
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(loadSegmentGroupArticlesSql(statusCd), vals, new RSSFeedSegment());
	}

	/**
	 * Helper method builds the Segment Group Article Count Sql Query.
	 * @return
	 */
	private String loadSegmentGroupArticlesSql(String statusCd) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.feed_segment_id, a.feed_group_id, a.feed_group_nm, ");
		sql.append("b.FEED_SEGMENT_NM, coalesce(d.article_count, 0) as article_count, s.section_nm, b.section_id, b.profile_id from ");
		sql.append(schema).append("BIOMEDGPS_FEED_GROUP a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("BIOMEDGPS_FEED_SEGMENT b ");
		sql.append("on a.FEED_SEGMENT_ID = b.FEED_SEGMENT_ID ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("BIOMEDGPS_SECTION s ");
		sql.append("on b.section_id = s.section_id and s.parent_id = 'MASTER_ROOT' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(" (select cast(count(feed_group_id) as int) as article_count, feed_group_id from ");
		sql.append(getCustomSchema()).append("biomedgps_rss_filtered_article ");

		if("C".equals(statusCd)) {
			sql.append("where complete_flg = ? ");
		} else if(!"ALL".equals(statusCd)) {
			sql.append("where article_status_cd = ? and complete_flg = ? ");
		}

		sql.append("group by feed_group_id) as d ");
		sql.append("on a.feed_group_id = d.feed_group_id ");
		sql.append("group by a.feed_segment_id, a.feed_group_id, a.feed_group_nm, b.feed_segment_id, s.section_nm, s.order_no, d.article_count ");
		sql.append("order by s.order_no, b.order_no, FEED_GROUP_NM ");
		log.debug(sql.toString());
		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		if(req.hasParameter(BULK_ACTION) && req.getBooleanParameter(BULK_ACTION) && "C".equals(req.getParameter("articleStatus"))) {
			processBulkComplete(req);
		}
		if(req.hasParameter(BULK_ACTION) && req.getBooleanParameter(BULK_ACTION)) {
			processBulkRequest(req);
		} else if("C".equals(req.getParameter("articleStatus"))) {
			markComplete(req);
		} else {
			updateArticle(req);
		}
	}

	/**
	 * Process a Bulk Complet Request.  Iterate the ids param on the request Map
	 * and for each record, set the articleFilterId on the request then call
	 * standard mark complete.
	 * @param req
	 * @throws ActionException
	 */
	private void processBulkComplete(ActionRequest req) throws ActionException {
		for(String id : req.getParameterValues("ids")) {
			req.setParameter("articleFilterId", id);
			markComplete(req);
		}
	}

	/**
	 * Method for handling bulk Feed Article Requests.  Likely
	 * @param req
	 */
	private void processBulkRequest(ActionRequest req) throws ActionException {
		if(req.hasParameter(BUCKET_ID) || req.hasParameter(STATUS_CD)) {
			String [] ids = req.getParameterValues("ids");
			boolean hasBucketId = req.hasParameter(BUCKET_ID);
			String bucketId = null;
			//BucketId may be present on the request for updating.
			if(hasBucketId) {
				bucketId = StringUtil.checkVal(req.getParameter(BUCKET_ID), null);

				//Articles can be removed from a bucket.  Allow for removal here.
				if("null".equals(bucketId)) {
					bucketId = null;
				}
			}

			int i = 1;
			try(PreparedStatement ps = dbConn.prepareStatement(buildBulkActionSql(req, ids))) {
				if(hasBucketId) {
					ps.setString(i++, bucketId);
				}
				if(req.hasParameter("articleStatus"))
					ps.setString(i++, req.getParameter("articleStatus"));

				for(String id : ids) {
					ps.setString(i++, id);
				}
				ps.executeUpdate();
			} catch (SQLException e) {
				log.error("Error Processing Code", e);
			}

			writeToSolr(ids);
		} else {
			throw new ActionException("Unable to process request.");
		}
	}

	/**
	 * Manage Bulk Actions from the front end.
	 * @param req
	 * @param ids
	 * @return
	 */
	private String buildBulkActionSql(ActionRequest req, String[] ids) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("biomedgps_rss_filtered_article set ");
		if(req.hasParameter(BUCKET_ID)) {
			sql.append("bucket_id = ? ");
		}

		if(req.hasParameter("articleStatus")) {
			if(req.hasParameter(BUCKET_ID)) {
				sql.append(", ");
			}
			sql.append("article_status_cd = ? ");
		}

		sql.append(DBUtil.WHERE_CLAUSE).append(" rss_article_filter_id in (");
		DBUtil.preparedStatmentQuestion(ids.length, sql);
		sql.append(")");
		return sql.toString();
	}
	/**
	 * Marks articles as completed.  Complete Articles get the completeFlag set
	 * on their record and do not appear in standard filters.  When an article is
	 * flagged as complete, all filtered articles related to the same article
	 * are flagged at the same time.
	 * @param req
	 * @throws ActionException
	 */
	private void markComplete(ActionRequest req) throws ActionException {
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		RSSArticleFilterVO rss = new RSSArticleFilterVO(req);
		List<String> articleIds = new ArrayList<>();
		String sql = StringUtil.join("select rss_article_filter_id from ", getCustomSchema(), "biomedgps_rss_filtered_article where rss_article_id = ?");
		try {

			//Populate VO so we can get the articleId
			dbp.getByPrimaryKey(rss);

			//Load the List of articles that will be affected.
			try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setString(1, rss.getRssArticleId());
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					articleIds.add(rs.getString("rss_article_filter_id"));
				}
			}

			//Perform update and set complete_flg = 1
			sql = StringUtil.join(DBUtil.UPDATE_CLAUSE, getCustomSchema(), "biomedgps_rss_filtered_article set complete_flg = 1 where rss_article_id = ?");
			try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setString(1, rss.getRssArticleId());
				ps.executeUpdate();
			}

			//Update Records in solr.
			writeToSolr(articleIds.toArray(new String [articleIds.size()]));

		} catch (Exception e) {
			log.error("Error updating article status Code", e);
			throw new ActionException(e);
		}
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
			writeToSolr(rss.getArticleFilterId());

		} catch (Exception e) {
			log.error("Error updating article status Code", e);
			throw new ActionException(e);
		}
	}

	/**
	 * Save an UpdatesVO to solr.
	 * @param u
	 */
	protected void writeToSolr(String... ids) {
		RSSArticleIndexer idx = RSSArticleIndexer.makeInstance(getAttributes());
		idx.setDBConnection(dbConn);
		idx.indexItems(ids);
	}

	/**
	 * Builds sql for updating an rssArticle.  Optionally will include bucketId.
	 * @return
	 */
	private String getUpdateArticleSql(boolean hasBucketId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_filtered_article set article_status_cd = ? ");
		if(hasBucketId)
			sql.append(", bucket_id = ? ");
		sql.append("where rss_article_filter_id = ? ");
		return sql.toString();
	}
}