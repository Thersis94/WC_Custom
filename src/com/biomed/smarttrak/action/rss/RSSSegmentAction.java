package com.biomed.smarttrak.action.rss;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.rss.vo.RSSFeedSegment;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> RSSSegmentAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom action manages Smarttrak Feeds Segment Data
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class RSSSegmentAction extends SBActionAdapter {

	public static final String SEGMENT_ID = "segmentId";
	public RSSSegmentAction() {
		super();
	}

	public RSSSegmentAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		try {
			RSSFeedSegment s = new RSSFeedSegment();
			s.setSegmentId(req.getParameter("pkId"));
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).delete(s);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Deleting Feed Segment", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		this.putModuleData(loadSegmentGroups());
	}

	/**
	 * Helper method Loads Group Segments XRs.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<RSSFeedSegment> loadSegmentGroups() {
		return (List<RSSFeedSegment>)(List<?>)new DBProcessor(dbConn).executeSelect(rssSegmentGroupRetrieveSql(), null, new RSSFeedSegment());
	}

	/**
	 * Helper method loadsList of Segments.
	 * @param segmentId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<RSSFeedSegment> loadSegments(String segmentId) {
		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(segmentId)) {
			vals.add(segmentId);
		}

		return (List<RSSFeedSegment>)(List<?>)new DBProcessor(dbConn).executeSelect(getRssSegmentSql(!StringUtil.isEmpty(segmentId)), vals, new RSSFeedSegment());
	}

	@Override
	public void list(ActionRequest req) throws ActionException {
		if(req.hasParameter(SEGMENT_ID) && "ADD".equals(req.getParameter(SEGMENT_ID)))
			return;
		this.putModuleData(loadSegments(req.getParameter(SEGMENT_ID)));
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		update(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		RSSFeedSegment s = new RSSFeedSegment(req);
		saveFeedSegment(s);
	}

	/**
	 * Helper method responsible for saving a Feed Segment
	 * @param s
	 */
	protected void saveFeedSegment(RSSFeedSegment s) {
		try {
			DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)); 
			dbp.save(s);
			if(StringUtil.isEmpty(s.getSegmentId())) {
				s.setSegmentId(dbp.getGeneratedPKId());
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Helper method builds the Segment Retrieval Sql.
	 * @return
	 */
	private String getRssSegmentSql(boolean hasSegmentId) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(schema).append("BIOMEDGPS_FEED_SEGMENT ");
		if(hasSegmentId) {
			sql.append("where feed_segment_id = ? ");
		}
		sql.append("order by FEED_SEGMENT_ID, FEED_SEGMENT_NM ");
		return sql.toString();
	}

	/**
	 * Helper method builds query for retrieving RSS Segments with underlying
	 * groups in proper order.
	 * @return
	 */
	private String rssSegmentGroupRetrieveSql() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(350);
		sql.append("select a.feed_segment_id, a.feed_group_id, a.feed_group_nm, ");
		sql.append("b.FEED_SEGMENT_NM from ").append(schema).append("BIOMEDGPS_FEED_GROUP a ");
		sql.append("inner join ").append(schema).append("BIOMEDGPS_FEED_SEGMENT b ");
		sql.append("on a.FEED_SEGMENT_ID = b.FEED_SEGMENT_ID ");
		sql.append("order by cast(b.FEED_SEGMENT_ID as int), FEED_GROUP_NM ");
		return sql.toString();
	}
}