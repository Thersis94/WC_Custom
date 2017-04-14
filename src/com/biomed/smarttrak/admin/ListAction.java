package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.admin.action.DirectUrlManagerAction;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ListAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> List Action that returns Map of Key value pairs for
 * Ajax calls.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public class ListAction extends DirectUrlManagerAction {

	public enum ListType { COMPANY, PRODUCT, MARKET, ACCOUNT }

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("ajaxListType")) {
			String listType = req.getParameter("ajaxListType");
			String searchTerm = req.getParameter("term");

			List<GenericVO> vals = getList(listType, false, searchTerm);
			putModuleData(vals, vals.size(), false);
		} else {
			list(req);
		}
	}

	@Override
	public void list(ActionRequest req) throws ActionException {

		//If a type is passed, attempt to retrieve URLs related to that type.
		if(req.hasParameter("type")) {
			// Get Map of Urls to Return and place on request.
			Map<String, List<GenericVO>> urls = getUrls(req);

			/*
			 * Put data on request in proper location.
			 * If amid is present then this isn't an admintool call.
			 */
			this.putModuleData(urls, urls.size(), !req.hasParameter("amid"));
		}
	}

	/**
	 * Helper method that loads all the Direct Urls available.
	 * @param orgId
	 * @return 
	 * @return
	 * @throws ActionException 
	 */
	@Override
	protected Map<String, List<GenericVO>> getUrls(ActionRequest req) throws ActionException {

		//Call Super to load any lists from core.
		Map<String, List<GenericVO>> urlMap = new HashMap<>();

		String type = req.getParameter("type");
		String searchTerm = req.getParameter("term");
		if("ALL".equals(type)) {
			urlMap.put("PAGE", getPageUrls(req));
			urlMap.put("COMPANY", getList(ListType.COMPANY.name(), true, searchTerm));
			urlMap.put("MARKET", getList(ListType.MARKET.name(), true, searchTerm));
			urlMap.put("PRODUCT", getList(ListType.PRODUCT.name(), true, searchTerm));
		} else {
			//Call Super to load any lists from core.
			urlMap = super.getUrls(req);

			//Get Companies
			if(ListType.COMPANY.name().equals(type)) {
				urlMap.put(ListType.COMPANY.name(), getList(ListType.COMPANY.name(), true, searchTerm));
			}

			//Get Markets
			if(ListType.MARKET.name().equals(type)) {
				urlMap.put(ListType.MARKET.name(), getList(ListType.MARKET.name(), true, searchTerm));
			}

			//Get Products
			if(ListType.PRODUCT.name().equals(type)) {
				urlMap.put(ListType.PRODUCT.name(), getList(ListType.PRODUCT.name(), true, searchTerm));
			}
		}

		return urlMap;
	}


	/**
	 * Get List of Data.
	 * @param listType
	 * @return
	 * @throws ActionException 
	 */
	protected List<GenericVO> getList(String listType, boolean asUrl, String searchTerm) throws ActionException {
		String sql;
		String url = null;
		boolean hasSearchTerm = !StringUtil.isEmpty(searchTerm);
		switch(ListType.valueOf(listType)) {
			case COMPANY:
				sql = getCompanySql(hasSearchTerm);
				url = Section.COMPANY.getPageURL() + getAttribute(Constants.QS_PATH);
				break;
			case MARKET:
				sql = getMarketSql(hasSearchTerm);
				url = Section.MARKET.getPageURL() + getAttribute(Constants.QS_PATH);
				break;
			case PRODUCT:
				sql = getProductSql(hasSearchTerm);
				url = Section.PRODUCT.getPageURL() + getAttribute(Constants.QS_PATH);
				break;
			case ACCOUNT:
				sql = getAccountSql(hasSearchTerm);
				break;
			default:
				throw new ActionException("Invalid List Type.");
		}

		List<GenericVO> vals = new ArrayList<>(2000);
		try(PreparedStatement ps = dbConn.prepareCall(sql)) {
			int i = 1;
			if(hasSearchTerm) {
				ps.setString(i++, ".*" + searchTerm + ".*");
			}
			ResultSet rs = ps.executeQuery();
			StringBuilder val = null;
			while(rs.next()) {
				val = new StringBuilder(rs.getString("id"));
				if(asUrl && !StringUtil.isEmpty(url)) {
					val.insert(0, url);
				}
				vals.add(new GenericVO(val.toString(), rs.getString("val")));
			}

		} catch (SQLException sqle) {
			log.error("could not load select list options", sqle);
		}
		return vals;
	}


	/**
	 * Build company list sql
	 * @return
	 */
	protected String getCompanySql(boolean hasSearchTerm) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select company_id as id, company_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_company ");
		sql.append("where status_no = 'P' ");
		if(hasSearchTerm) {
			sql.append("and company_nm ~* ? ");
		}
		sql.append("order by company_nm limit 100");

		return sql.toString();
	}

	/**
	 * Build market list sql
	 * @return
	 */
	protected String getMarketSql(boolean hasSearchTerm) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select market_id as id, market_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET where status_no = 'P' ");
		if(hasSearchTerm) {
			sql.append("and market_nm ~* ? ");
		}
		sql.append("order by market_nm limit 100");

		return sql.toString();
	}

	/**
	 * Build product list sql
	 * @return
	 */
	protected String getProductSql(boolean hasSearchTerm) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select product_id as id, product_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT where status_no = 'P' ");
		if(hasSearchTerm) {
			sql.append("and product_nm ~* ? ");
		}
		sql.append("order by product_nm limit 100");

		return sql.toString();
	}

	/**
	 * Build account list sql
	 * @return
	 */
	protected String getAccountSql(boolean hasSearchTerm) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select account_id as id, account_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_account ");
		sql.append("where 1=1 ");
		if(hasSearchTerm) {
			sql.append("and account_nm ~* ? ");
		}
		sql.append("order by account_nm");

		return sql.toString();
	}
}