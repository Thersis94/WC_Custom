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
	
	/**
	 * Information on the various types of items that can be loaded for in this action, 
	 * including thier actionType, database table, and database columns.
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
	private static final String ATTRIBUTE_START = "manage_recent_";
	
	@SuppressWarnings("unchecked")
	@Override
	public void build(ActionRequest req) throws ActionException {
		GenericVO item = new GenericVO(req.getParameter("itemId"), req.getParameter("itemName"));
		String attrKey = ATTRIBUTE_START +req.getParameter("loadAction");
		
		List<GenericVO> itemList = (List<GenericVO>) req.getSession().getAttribute(attrKey);
		if (itemList == null || itemList.isEmpty()) itemList = loadRecentlyViewed(req);

		//Verify that we don't have an empty list
		if(!itemList.isEmpty()) {
			int i=0;
			// Check if the new item is present.
			for (; i < MAX_RESULTS && i < itemList.size(); i++) {
				String id = (String) itemList.get(i).getKey();
				if (id.equals(item.getKey())) break;
			}

			// If this is a new item for the lists ensure that last current item is removed.
			if (i == MAX_RESULTS) {
				i = itemList.size() - 1;
			}

			itemList.remove(i);
			itemList.add(0, item);
		}
	}
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("loadAction")) return;
		super.putModuleData(loadRecentlyViewed(req));
	}
	
	
	/**
	 * Load recently viewed items based on load type.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private List<GenericVO> loadRecentlyViewed(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String action = req.getParameter("loadAction");
		
		// If this has already been loaded just 
		if (req.getSession().getAttributes().containsKey(ATTRIBUTE_START +action)) {
			return (List<GenericVO>) req.getSession().getAttribute(ATTRIBUTE_START +action);
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(getPageRetrieveSql())) {
			ps.setString(1, "BMG_SMARTTRAK_1");
			ps.setString(2, user.getProfileId());
			ps.setString(3, "%" + action + "%");

			ResultSet rs = ps.executeQuery();
			
			List<GenericVO> results = parsePageViews(rs, loadAction.getFromAction(action));
			req.getSession().setAttribute(ATTRIBUTE_START +action, results);
			return results;
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Create sql query for retrieving page view information
	 * @return
	 */
	private String getPageRetrieveSql() {
		StringBuilder sql = new StringBuilder(325);
		
		sql.append("select query_str_txt from core.pageview_user pu ");
		sql.append("where site_id = ? and request_uri_txt = '/manage' and profile_id = ? ");
		// Limit the returned pageviews to the main details page of the various manage tools that have actual items associated with them
		sql.append("and query_str_txt like ? and query_str_txt like '%Id=%' and length(query_str_txt) < 100 and query_str_txt not like '%Id=ADD%'");
		sql.append("order by visit_dt desc");

		return sql.toString();
	}
	
	
	/**
	 * Turn page views into a list of item ids that can be used to get more detailed information
	 * @param rs
	 * @param load
	 * @return
	 * @throws SQLException
	 */
	private List<GenericVO> parsePageViews(ResultSet rs, loadAction load) throws ActionException {
		if (load == null) return Collections.emptyList();
		List<String> ids = new ArrayList<>(MAX_RESULTS);
		QueryStringParser p = new QueryStringParser();
		try {
			while(rs.next()) {
				p.processData(rs.getString("query_str_txt"));
				
				for (String s : p.getParameterMap().keySet()) {
					if (s.endsWith("Id")) {
						addItem(ids, p.getParameter(s));
						break;
					}
				}
				
				// If the maximum desired results have been retrieve stop looping.
				if (ids.size() == MAX_RESULTS) break;
			}
			return getItemDetails(ids, load);
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Ensure that an actual id has been passed along and that it is not already in the list.
	 * @param ids
	 * @param id
	 */
	private void addItem(List<String> ids, String id) {
		// Only display the first five unique items
		if (StringUtil.isEmpty(id) || ids.contains(id)) return;
		// Add empty entry for now to retain proper ordering
		ids.add(id);
	}
	
	
	/**
	 * Get the name of each item in the list of ids.
	 * @param ids
	 * @param load
	 * @return
	 */
	private List<GenericVO> getItemDetails(List<String> ids, loadAction load) throws ActionException {
		if(ids.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, String> data = new HashMap<>(MAX_RESULTS);
		try (PreparedStatement ps = dbConn.prepareStatement(getDetailSQL(ids.size(), load))) {
			int i = 1;

			for (String s : ids) ps.setString(i++, s);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.put(rs.getString("id"), rs.getString("name"));
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		
		List<GenericVO> items = new ArrayList<>(MAX_RESULTS);
		
		for (String id : ids) {
			items.add(new GenericVO(id, data.get(id)));
		}
		
		return items;	
	}

	
	/**
	 * Create the sql query for retrieving the name of all recently viewed items.
	 * @param size
	 * @param load
	 * @return
	 */
	private String getDetailSQL(int size, loadAction load) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(750);
		
		sql.append("select ").append(load.getIdField()).append(" as id, ").append(load.getNameField()).append(" as name ");
		sql.append("from ").append(customDb).append(load.getTable()).append(" ");
		sql.append("where ").append(load.getIdField()).append(" in (").append(DBUtil.preparedStatmentQuestion(size)).append(") ");

		return sql.toString();
	}
	
}