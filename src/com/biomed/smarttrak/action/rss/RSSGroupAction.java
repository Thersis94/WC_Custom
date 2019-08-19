package com.biomed.smarttrak.action.rss;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.rss.vo.RSSFeedGroupVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title:</b> RSSGroupAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom Action manages Smarttrak Feeds Group Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class RSSGroupAction extends SBActionAdapter {

	public static final String FEED_GROUP_ID = "feedGroupId";
	public static final String DB_FEED_GROUP_ID = "feed_group_id";

	public RSSGroupAction() {
		super();
	}

	public RSSGroupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		try {
			RSSFeedGroupVO g = new RSSFeedGroupVO();
			g.setFeedGroupId(req.getParameter("pkId"));
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).delete(g);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Deleting Feed Group", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String feedGroupId = req.getParameter(FEED_GROUP_ID);
		List<RSSFeedGroupVO> groups = loadGroupXrs(null, feedGroupId, null);

		this.putModuleData(groups);
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
		RSSFeedGroupVO g = new RSSFeedGroupVO(req);
		saveFeedGroup(g);
	}

	/**
	 * Helper method manages Saving the given RSSFeedGroup.
	 * @param g
	 */
	private void saveFeedGroup(RSSFeedGroupVO g) {
		try {
			DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)); 
			dbp.save(g);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Helper method responsible for returning proper Feed Group XR's based on
	 * passed Parameters. 
	 * @param rssEntityId 
	 * @param hasParameter
	 * @return
	 */
	public List<RSSFeedGroupVO> loadGroupXrs(String filterId, String feedGroupId, String rssEntityId) {

		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(filterId)) {
			vals.add(filterId);
		}
		if(!StringUtil.isEmpty(feedGroupId)) {
			vals.add(feedGroupId);
		}
		if(!StringUtil.isEmpty(rssEntityId)) {
			vals.add(rssEntityId);
		}

		String sql = getGroupXRSql(!StringUtil.isEmpty(filterId), !StringUtil.isEmpty(feedGroupId), !StringUtil.isEmpty(rssEntityId));
		return new DBProcessor(dbConn).executeSelect(sql, vals, new RSSFeedGroupVO());
	}

	/**
	 * Helper method builds the proper Sql Retrieval statement for getting
	 * group filter XRs.  Based on boolean passed, joins out to relevant table.
	 * @return
	 */
	private String getGroupXRSql(boolean hasFilterId, boolean hasGroupId, boolean hasEntityId) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(650);
		sql.append("select * from ").append(schema).append("BIOMEDGPS_FEED_GROUP g ");
		sql.append("inner join ").append(schema).append("biomedgps_feed_segment s ");
		sql.append("on g.feed_segment_id = s.feed_Segment_id ");
		if(hasFilterId) {
			sql.append("left outer join ").append(schema).append("BIOMEDGPS_FEED_FILTER_GROUP_XR x ");
			sql.append("on g.FEED_GROUP_ID = x.FEED_GROUP_ID and x.filter_id = ? ");
		}
		if(hasEntityId) {
			sql.append("left outer join ").append(schema).append("biomedgps_feed_source_group_xr fsg ");
			sql.append("on g.feed_group_id = fsg.feed_group_id and fsg.rss_entity_id = ? ");
		}

		if(hasGroupId)
			sql.append("where g.feed_group_id = ? ");
		sql.append("order by g.FEED_GROUP_NM");

		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if(req.hasParameter(FEED_GROUP_ID)) {
			loadGroupTypes(req);

			loadSegments(req);
		}

		if(req.hasParameter(FEED_GROUP_ID) && "ADD".equals(req.getParameter(FEED_GROUP_ID))) {
			return;
		} else {
			retrieve(req);
		}
	}

	/**
	 * Helper method loads Feed Group Types.
	 * @param req
	 */
	private void loadGroupTypes(ActionRequest req) {
		RSSTypeAction rta = new RSSTypeAction();
		rta.setAttributes(attributes);
		rta.setDBConnection(dbConn);
		req.setAttribute("types", rta.loadTypes(null));
	}

	/**
	 * Helper method loads Group Segments.
	 * @param req
	 */
	private void loadSegments(ActionRequest req) {
		RSSSegmentAction rsa = new RSSSegmentAction();
		rsa.setAttributes(attributes);
		rsa.setDBConnection(dbConn);
		req.setAttribute("segments", rsa.loadSegments(null));
	}
}