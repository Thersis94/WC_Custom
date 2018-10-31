package com.biomed.smarttrak.action.rss;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.rss.vo.RSSFilterTypeVO;
import com.biomed.smarttrak.action.rss.vo.RSSFilterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> RSSFilterAction.java
 * <b>Project:</b> WebCrescendo
 * <b>Description:</b> Action manages RSS Parser Filter Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 27, 2017
 ****************************************************************************/
public class RSSFilterAction extends SBActionAdapter {

	public static final String RETRV_FILTER = "retrvFilter";
	public static final String FILTER_ID = "filterId";

	public enum FilterType{R("Required"), O("Omit"), Q("Unused");
		private String typeName;
		FilterType(String typeName) {
			this.typeName = typeName;
		}

		public String getTypeName() {
			return typeName;
		}
	}

	public RSSFilterAction() {
		super();
	}

	public RSSFilterAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		try {
			RSSFilterVO f = new RSSFilterVO();
			f.setFilterId(req.getParameter("pkId"));
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).delete(f);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Deleting Parsing Filter", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String filterId = req.getParameter(FILTER_ID);
		String rssEntityId = req.getParameter("rssEntityId");
		String feedGroupId = req.getParameter("feedGroupId");
		String filterTypeCode = req.getParameter("filterTypeCode");
		List<RSSFilterVO> filters = loadFilters(filterId, rssEntityId, feedGroupId, filterTypeCode);

		this.putModuleData(filters);
	}

	/**
	 * Helper method loads RSS Filters.
	 * @param parameter
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<RSSFilterVO> loadFilters(String filterId, String rssEntityId, String feedGroupId, String filterTypeCode) {
		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(rssEntityId)) {
			vals.add(rssEntityId);
		}
		if(!StringUtil.isEmpty(filterId)) {
			vals.add(filterId);
		}
		if(!StringUtil.isEmpty(feedGroupId)) {
			vals.add(feedGroupId);
		}
		if(!StringUtil.isEmpty(filterTypeCode)) {
			vals.add(filterTypeCode);
		}
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		List<Object> data = dbp.executeSelect(buildRSSFilterRetrieve(filterId, rssEntityId, feedGroupId, filterTypeCode), vals, new RSSFilterVO());
		return (List<RSSFilterVO>)(List<?>) data;
	}

	/**
	 * Helper method that retrieves the list of RSSFilters Query.
	 * @param filterId
	 * @param rssEntityId
	 * @return
	 */
	private String buildRSSFilterRetrieve(String filterId, String rssEntityId, String feedGroupId, String filterTypeCode) {
		StringBuilder sql = new StringBuilder(350);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(schema).append("BIOMEDGPS_RSS_PARSER_FILTER f ");
		if(!StringUtil.isEmpty(rssEntityId)) {
			sql.append("inner join ").append(schema).append("BIOMEDGPS_RSS_FILTER_TYPE x ");
			sql.append("on filter_id = f.filter_id and x.rss_entity_id = ? ");
		}
		if(!StringUtil.isEmpty(feedGroupId)) {
			sql.append("inner join ").append(schema).append("biomedgps_feed_filter_group_xr xr ");
			sql.append("on f.filter_id = xr.filter_id and xr.feed_group_id = ? ");
		}
		sql.append("where 1=1 ");
		if(!StringUtil.isEmpty(filterId)) {
			sql.append("and f.filter_id = ? ");
		}
		if(!StringUtil.isEmpty(filterTypeCode)) {
			sql.append("and f.filter_type_cd = ? ");
		}
		sql.append("order by filter_nm");

		log.debug(sql.toString());
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		update(req);
	}

	/**
	 * Helper method updates a Parser Filter record.  Ensures that filterId
	 * is set correctly.
	 * @param fv
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void updateFilterParser(RSSFilterVO fv) throws InvalidDataException, DatabaseException {
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)); 
		dbp.save(fv);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {

		//Always load Types.
		loadTypes(req);

		//Load Groups if we're editing a record.
		if(req.hasParameter(FILTER_ID)) {
			loadGroups(req);
		}

		if(req.hasParameter(FILTER_ID) && "ADD".equals(req.getParameter(FILTER_ID))) {
			return;
		} else {
			retrieve(req);
		}
	}

	/**
	 * Helper method loads Groups
	 * @param req
	 */
	protected void loadGroups(ActionRequest req) {
		RSSGroupAction rga = new RSSGroupAction(this.actionInit);
		rga.setAttributes(attributes);
		rga.setDBConnection(dbConn);
		req.setAttribute("groups", rga.loadGroupXrs(req.getParameter(FILTER_ID), null, null));
	}

	/**
	 * Helper method loads Filter Types.
	 * @param req
	 */
	protected void loadTypes(ActionRequest req) {
		List<Object> data = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).executeSelect(getTypesSql(), null, new RSSFilterTypeVO());
		req.setAttribute("rssTypes", data);
	}

	/**
	 * Helper method returns SQL for loading Filter Types.
	 * @return
	 */
	private String getTypesSql() {
		StringBuilder sql = new StringBuilder(50);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_RSS_FILTER_TYPE order by filter_type_cd");
		log.debug(sql.toString());
		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		RSSFilterVO fv = new RSSFilterVO(req);
		try {
			updateFilterParser(fv);
			saveFilterGroupXRs(fv, req.getParameterValues("feedFilterGroupXrId"));
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Saving Filters", e);
		}
	}

	/**
	 * Helper method for saving Filter Group XRs.  Flushes old ones out of the
	 * system and re-saves as new.
	 * @param fv
	 * @param parameterValues
	 */
	private void saveFilterGroupXRs(RSSFilterVO fv, String[] groupIds) {
		delFilterGroupXRVals(fv.getFilterId());
		saveFilterGroupXRVals(fv.getFilterId(), groupIds);
	}

	/**
	 * Helper method for flusing Filter Group XRs
	 * @param filterId
	 */
	private void delFilterGroupXRVals(String filterId) {
		StringBuilder s = new StringBuilder(125);
		s.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		s.append("BIOMEDGPS_FEED_FILTER_GROUP_XR where filter_id = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(s.toString())) {
			ps.setString(1, filterId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Deleting Filter Group XRs", e);
		}
	}

	/**
	 * Helper method manages saving all the new Filter Group XRs.
	 * @param filterId
	 * @param groupIds
	 */
	protected void saveFilterGroupXRVals(String filterId, String... groupIds) {
		StringBuilder s = new StringBuilder(150);
		s.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		s.append("biomedgps_feed_filter_group_xr (feed_filter_group_xr_id, ");
		s.append("filter_id, feed_group_id, create_dt) values(?,?,?,?)");
		Map<String, List<Object>> insertValues = new HashMap<>();

		UUIDGenerator uuid = new UUIDGenerator();
		for (String g : groupIds) {
				String xrId = uuid.getUUID();
				List<Object> insertData = new ArrayList<>();
				insertData.addAll(Arrays.asList(xrId, filterId, g, Convert.getCurrentTimestamp()));	
				insertValues.put(xrId, insertData);
		}

		try {
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).executeBatch(s.toString(), insertValues);
		} catch (DatabaseException e) {
			log.error("Couldn't save Filter Group XRs", e);
		}
	}
}
