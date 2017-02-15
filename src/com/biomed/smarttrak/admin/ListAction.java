package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
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
public class ListAction extends SBActionAdapter {

	public enum ListType { COMPANY, PRODUCT, MARKET, ACCOUNT }

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String listType = req.getParameter("ajaxListType");
		Map<String, String> vals = getList(listType);
		putModuleData(vals, vals.size(), false);
	}


	/**
	 * Get List of Data.
	 * @param listType
	 * @return
	 * @throws ActionException 
	 */
	protected Map<String, String> getList(String listType) throws ActionException {
		String sql;
		switch(ListType.valueOf(listType)) {
			case COMPANY:
				sql = getCompanySql();
				break;
			case MARKET:
				sql = getMarketSql();
				break;
			case PRODUCT:
				sql = getProductSql();
				break;
			case ACCOUNT:
				sql = getAccountSql();
				break;
			default:
				throw new ActionException("Invalid List Type.");
		}

		Map<String, String> vals = new LinkedHashMap<>(2000);
		try(PreparedStatement ps = dbConn.prepareCall(sql)) {
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				vals.put(rs.getString("id"), rs.getString("val"));

		} catch (SQLException sqle) {
			log.error("could not load select list options", sqle);
		}
		return vals;
	}


	/**
	 * Build company list sql
	 * @return
	 */
	protected String getCompanySql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select company_id as id, company_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_company ");
		sql.append("order by company_nm");

		return sql.toString();
	}

	/**
	 * Build market list sql
	 * @return
	 */
	protected String getMarketSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select market_id as id, market_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET order by market_nm");

		return sql.toString();
	}

	/**
	 * Build product list sql
	 * @return
	 */
	protected String getProductSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select product_id as id, product_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_PRODUCT order by product_nm");

		return sql.toString();
	}

	/**
	 * Build account list sql
	 * @return
	 */
	protected String getAccountSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select account_id as id, account_nm as val from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_account ");
		sql.append("order by account_nm");

		return sql.toString();
	}
}