package com.biomed.smarttrak.action.rss;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSFeedSegment;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.rss.RssVO;
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
		if(req.hasParameter("isBuckets")) {
			loadBuckets(req);
		} else if(req.hasParameter("feedGroupId")){
			loadArticles(req);
		} else {
			loadSegmentGroupArticles(req);
		}
	}

	/**
	 * @param req
	 */
	private void loadArticles(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		vals.add(req.getParameter("feedGroupId"));
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		this.putModuleData(dbp.executeSelect(loadArticleSql(), vals, new RSSArticleVO()));
	}

	/**
	 * @return
	 */
	private String loadArticleSql() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(schema).append("biomedgps_rss_article a ");
		sql.append("where a.feed_group_id = ?");

		return sql.toString();
	}

	/**
	 * @param req
	 */
	private void loadBuckets(ActionRequest req) {
		//TODO Load Buckets
	}

	/**
	 * Load Segment Group Articles for the Feed Landing Page.
	 * @param req
	 * @return 
	 */
	private void loadSegmentGroupArticles(ActionRequest req) {
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
		StringBuilder sql = new StringBuilder(700);
		sql.append("select a.feed_segment_id, a.feed_group_id, a.feed_group_nm, ");
		sql.append("b.FEED_SEGMENT_NM, Count(d.feed_group_id) as article_count from ");
		sql.append(schema).append("BIOMEDGPS_FEED_GROUP a ");
		sql.append("inner join ").append(schema).append("BIOMEDGPS_FEED_SEGMENT b ");
		sql.append("on a.FEED_SEGMENT_ID = b.FEED_SEGMENT_ID ");
		sql.append("left outer join ").append(schema).append("biomedgps_feed_source_group_xr c ");
		sql.append("on a.feed_group_id = c.feed_group_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_rss_article d ");
		sql.append("on c.feed_group_id = d.feed_group_id and d.article_status_cd = ? ");
		sql.append("group by a.feed_segment_id, a.feed_group_id, a.feed_group_nm, b.feed_segment_id ");
		sql.append("order by cast(b.FEED_SEGMENT_ID as int), FEED_GROUP_NM");
		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if(req.hasParameter("status")) {
				updateRSSData(req);
			} else {
				updateBucket(req);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void updateBucket(ActionRequest req) throws InvalidDataException, DatabaseException {
		SmarttrakRssVO rss = new SmarttrakRssVO(req);
		//Delete Old Bucket Record.
		deletefromBucket(rss);
		//Insert New Bucket Record.
		insertToBucket(rss);
	}

	/**
	 * @param rss
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void deletefromBucket(SmarttrakRssVO rss) throws InvalidDataException, DatabaseException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> fields = new ArrayList<>();
		fields.add("rssDataId");

		new DBProcessor(dbConn, schema).executeSqlUpdate(getRssBucketDeleteSql(schema), rss, fields);
	}

	/**
	 * @param schema
	 * @return
	 */
	private String getRssBucketDeleteSql(String schema) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(schema).append("biomedgps_bucket ");
		sql.append("where rss_data_id = ?");

		return sql.toString();
	}

	/**
	 * @param rss
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void insertToBucket(SmarttrakRssVO rss) throws InvalidDataException, DatabaseException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<String> fields = new ArrayList<>();
		fields.add("bucket_id");
		fields.add("rss_data_id");
		new DBProcessor(dbConn, schema).executeSqlUpdate(getRssBucketUpdateSql(schema), rss, fields);
	}

	/**
	 * @return
	 */
	private String getRssBucketUpdateSql(String schema) {
		StringBuilder sql = new StringBuilder(125);
		sql.append("insert into ").append(schema).append("biomedgps_bucket ");
		sql.append("(bucket_id, rss_data_id, create_dt) values(?, ?, ?)");
		return sql.toString();
	}

	/**
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void updateRSSData(ActionRequest req) throws InvalidDataException, DatabaseException {
		List<String> fields = new ArrayList<>();
		fields.add("status_cd");
		fields.add("rss_data_id");
		RssVO rss = new RssVO(req);
		new DBProcessor(dbConn).executeSqlUpdate(getRssStatusSql(), rss, fields);
	}

	/**
	 * @return
	 */
	private String getRssStatusSql() {
		return null;
	}
}