package com.biomed.smarttrak.action.rss;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.rss.vo.RSSFilterTerm;
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
 * <b>Title:</b> RSSTermsAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom Action manages Smarttrak Feeds Term Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class RSSTermsAction extends SBActionAdapter {

	public static final String FILTER_TERM_ID = "filterTermId";

	public RSSTermsAction() {
		super();
	}

	public RSSTermsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		try {
			RSSFilterTerm ft = new RSSFilterTerm();
			ft.setFilterTermId(req.getParameter("pkId"));
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).delete(ft);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Deleting Feed Term", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		RSSFilterTerm ft = new RSSFilterTerm(req);
		saveFilterTerm(ft);
	}

	/**
	 * Helper method manages Saving a given Filter Term.
	 * @param ft
	 */
	protected void saveFilterTerm(RSSFilterTerm ft) {
		try {
			DBProcessor dbp = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)); 
			dbp.save(ft);
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

		//Always Load Types and Groups.
		RSSFilterAction rfa = new RSSFilterAction(this.actionInit);
		rfa.setAttributes(attributes);
		rfa.setDBConnection(dbConn);
		rfa.loadTypes(req);
		rfa.loadGroups(req);

		if(req.hasParameter(FILTER_TERM_ID) && "ADD".equals(req.getParameter(FILTER_TERM_ID))) {
			return;
		}

		this.putModuleData(loadTerms(req.getParameter(FILTER_TERM_ID), req.getParameter("filterTypeCode"), req.getParameter("feedGroupCode")));
	}

	/**
	 * Helper method manages loading Filter Terms.
	 * @return
	 */
	public List<RSSFilterTerm> loadTerms(String filterTermId, String filterTypeCode, String feedGroupCd) {
		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(filterTermId)) {
			vals.add(filterTermId);
		}
		if(!StringUtil.isEmpty(filterTypeCode)) {
			vals.add(filterTypeCode);
		}
		if (!StringUtil.isEmpty(feedGroupCd)) {
			vals.add(feedGroupCd);
		}
		DBProcessor dbp = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		String sql = loadFilterTermTypeSql(!StringUtil.isEmpty(filterTermId), !StringUtil.isEmpty(filterTypeCode), !StringUtil.isEmpty(feedGroupCd));
		return dbp.executeSelect(sql, vals, new RSSFilterTerm());
	}

	/**
	 * Helper method builds Query for Retrievaing Filter Terms with associated
	 * type and Group.
	 * @return
	 */
	private String loadFilterTermTypeSql(boolean hasGroupTypeCd, boolean hasFilterTypeCd, boolean hasFeedGroupCd) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(450);
		sql.append("select * from ").append(schema);
		sql.append("BIOMEDGPS_FILTER_TERM a ");
		sql.append("inner join ").append(schema).append("BIOMEDGPS_RSS_FILTER_TYPE t ");
		sql.append("on a.filter_type_cd = t.filter_type_cd ");
		sql.append("inner join ").append(schema).append("BIOMEDGPS_FEED_GROUP g ");
		sql.append("on a.feed_group_id = g.feed_group_id ");
		sql.append("where 1=1 ");
		if(hasGroupTypeCd) {
			sql.append("and filter_term_id = ? ");
		}
		if(hasFilterTypeCd) {
			sql.append("and a.filter_type_cd = ? ");
		}
		if(hasFeedGroupCd) {
			sql.append("and g.feed_group_id = ? ");
		}

		sql.append("order by filter_term_id");
		log.debug(sql.toString());
		return sql.toString();
	}
}