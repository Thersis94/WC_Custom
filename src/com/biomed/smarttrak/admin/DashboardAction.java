package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.QueryStringParser;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: DashboardAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Passes along information for a solr search specified by 
 * the admin controller.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Billy Larsen
 * @version 1.0
 * @since Jun 13, 2017
 ****************************************************************************/
public class DashboardAction extends SBActionAdapter {

	public DashboardAction() {
		super();
	}

	public DashboardAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Pass along the proper information for a search to be done.
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		req.setParameter("pmid", mod.getPageModuleId());
		String search = StringUtil.checkVal(req.getParameter("searchData"));

		req.setParameter("searchData", search.toLowerCase());

		// Build the solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);

		req.setParameter("searchData", search);
	}
	 */
	
	
	
	enum loadAction { 
		PRODUCT("productAdmin", "product_id", "product_nm", "biomedgps_product"),
		COMPANY("companyAdmin", "company_id", "company_nm", "biomedgps_company"),
		MARKET("marketAdmin", "market_id", "market_nm", "biomedgps_market"),
		ANALYSES("insights", "insight_id", "title_txt", "biomedgps_insight");

		private String actionType;
		private String idField;
		private String nameField;
		private String table;
		
		loadAction(String actionType, String idField, String nameField, String table) {
			this.actionType = actionType;
			this.idField = idField;
			this.nameField = nameField;
			this.table = table;
		}

		public String getActionType() { return actionType;}
		public String getIdField() { return idField;}
		public String getNameField() { return nameField;}
		public String getTable() { return table;}
		
		public static loadAction getFromAction(String action) {
			switch (action) {
				case "productAdmin": return PRODUCT;
				case "companyAdmin": return COMPANY;
				case "marketAdmin": return MARKET;
				case "insights": return ANALYSES;
			}
			return null;
		}
	}
	
	private static final int MAX_RESULTS = 25;
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug(req.getParameter("loadAction"));
		if (!req.hasParameter("loadAction")) return;
		super.putModuleData(loadRecentlyViewed(req));
	}
	
	
	private List<GenericVO> loadRecentlyViewed(ActionRequest req) {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		
		try (PreparedStatement ps = dbConn.prepareStatement(getPageRetrieveSql())) {
			ps.setString(1, "BMG_SMARTTRAK_1");
			ps.setString(2, user.getProfileId());
			ps.setString(3, "%" + req.getParameter("loadAction") + "%");
			
			ResultSet rs = ps.executeQuery();
			
			return parsePageViews(rs, loadAction.getFromAction(req.getParameter("loadAction")));
		} catch (SQLException e) {
			log.error(e);
		}
		return Collections.emptyList();
	}
	
	
	private String getPageRetrieveSql() {
		StringBuilder sql = new StringBuilder(325);
		
		sql.append("select query_str_txt from core.pageview_user pu ");
		sql.append("where site_id = ? and request_uri_txt = '/manage' and profile_id = ? ");
		// Limit the returned pageviews to the main details page of the various manage tools that have actual items associated with them
		sql.append("and query_str_txt like ? and query_str_txt like '%Id=%' and length(query_str_txt) < 100 and query_str_txt not like '%Id=ADD%'");
		sql.append("order by visit_dt desc");
		
		return sql.toString();
	}
	
	
	private List<GenericVO> parsePageViews(ResultSet rs, loadAction load) throws SQLException {
		if (load == null) return Collections.emptyList();
		List<String> ids = new ArrayList<>(MAX_RESULTS);
		QueryStringParser p = new QueryStringParser();
		while(rs.next()) {
			p.processData(rs.getString("query_str_txt"));
			
			for (String s : p.getParameterMap().keySet()) {
				if (s.endsWith("Id")) {
					addItem(ids, p.getParameter(s));
					break;
				}
			}
		}
		
		return getItemDetails(ids, load);
		
	}

	
	private void addItem(List<String> ids, String id) {
		// Only display the first five unique items
		if (StringUtil.isEmpty(id) || ids.size() == MAX_RESULTS || ids.contains(id)) return;
		// Add empty entry for now to retain proper ordering
		ids.add(id);
	}
	
	
	private List<GenericVO> getItemDetails(List<String> ids, loadAction load) {

		Map<String, String> data = new HashMap<>(MAX_RESULTS);
		try (PreparedStatement ps = dbConn.prepareStatement(getDetailSQL(ids.size(), load))) {
			int i = 1;

			for (String s : ids) ps.setString(i++, s);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.put(rs.getString("id"), rs.getString("name"));
			}
			
		} catch (SQLException e) {
			log.error(e);
		}
		
		
		List<GenericVO> items = new ArrayList<>(MAX_RESULTS);
		
		for (String id : ids) {
			items.add(new GenericVO(id, data.get(id)));
		}
		log.debug(items.size());
		return items;	
	}

	private String getDetailSQL(int size, loadAction load) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(750);
		
		sql.append("select ").append(load.getIdField()).append(" as id, ").append(load.getNameField()).append(" as name ");
		sql.append("from ").append(customDb).append(load.getTable()).append(" ");
		sql.append("where ").append(load.getIdField()).append(" in (").append(DBUtil.preparedStatmentQuestion(size)).append(") ");
		
		return sql.toString();
	}
	
	
	
	
	
}