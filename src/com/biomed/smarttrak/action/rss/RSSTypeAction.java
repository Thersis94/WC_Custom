package com.biomed.smarttrak.action.rss;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.rss.vo.RSSFeedTypeVO;
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
 * <b>Title:</b> RSSTypeAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom Action manages smarttrak Feeds Type Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class RSSTypeAction extends SBActionAdapter {

	public static final String FEED_TYPE_CD = "feedTypeId";

	public RSSTypeAction() {
		super();
	}

	public RSSTypeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		try {
			RSSFeedTypeVO gt = new RSSFeedTypeVO();
			gt.setFeedTypeId(req.getParameter("pkId"));
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).delete(gt);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Deleting Feed Group Type", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		RSSFeedTypeVO gt = new RSSFeedTypeVO(req);
		try {
			DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)); 
			dbp.save(gt);
			if(StringUtil.isEmpty(gt.getFeedTypeId())) {
				gt.setFeedTypeId(dbp.getGeneratedPKId());
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		update(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if(req.hasParameter(FEED_TYPE_CD) && "ADD".equals(req.getParameter(FEED_TYPE_CD))) {
			return;
		}

		this.putModuleData(loadTypes(req.getParameter(FEED_TYPE_CD)));
	}

	/**
	 * Halper method manages loading RSS Feed Types.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<RSSFeedTypeVO> loadTypes(String feedTypeCd) {
		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(feedTypeCd)) {
			vals.add(feedTypeCd);
		}
		return(List<RSSFeedTypeVO>)(List<?>) new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA)).executeSelect(loadTypeSql(!StringUtil.isEmpty(feedTypeCd)), vals, new RSSFeedTypeVO());
	}

	/**
	 * Helper method manages build Feed Type SQL.
	 * @return
	 */
	protected String loadTypeSql(boolean hasFeedTypeCd) {
		StringBuilder sql = new StringBuilder(125);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_FEED_TYPE ");
		if(hasFeedTypeCd) {
			sql.append("where feed_type_id = ?");
		}

		return sql.toString();
	}
}