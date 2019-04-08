package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.admin.vo.UpdateTitleVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UpdateListTitleAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> used to bring back a list of requested matching data, along with
 * 		extra data needed for default title creation
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Jun 22, 2017
 * @updates:
 ****************************************************************************/
public class UpdateListTitleAction extends ListAction {

	public enum ListType { COMPANY, PRODUCT, MARKET, ACCOUNT }

	//TODO post code freeze this class should be merged with listAction
	//see ticket Q3-409 for more details

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.ListAction#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("ajaxListType")) {
			String listType = req.getParameter("ajaxListType");
			String searchTerm = req.getParameter("term", "");
			boolean isAutoComplete = Convert.formatBoolean(req.getParameter("autoComplete"));

			List<UpdateTitleVO> vals = getUpdateTitleList(listType, searchTerm, isAutoComplete);
			log.debug(" size of vals: " + vals.size());
			putModuleData(vals, vals.size(), false);
		} else {
			list(req);
		}
	}

	/**
	 * Get List of Data.
	 * @param listType
	 * @return
	 * @throws ActionException 
	 */

	protected List<UpdateTitleVO> getUpdateTitleList(String listType, String searchTerm, boolean isAutoComplete) throws ActionException {
		String sql;
		String mainUrl = null;
		boolean hasSearchTerm = !StringUtil.isEmpty(searchTerm);
		switch(ListType.valueOf(listType)) {
		case COMPANY:
			sql = getCompanySql(hasSearchTerm, isAutoComplete);
			mainUrl = Section.COMPANY.getPageURL() + getAttribute(Constants.QS_PATH);
			break;
		case MARKET:
			sql = getMarketSql(hasSearchTerm, isAutoComplete);
			mainUrl = Section.MARKET.getPageURL() + getAttribute(Constants.QS_PATH);
			break;
		case PRODUCT:
			sql = getProductSql(hasSearchTerm, isAutoComplete);
			mainUrl = Section.PRODUCT.getPageURL() + getAttribute(Constants.QS_PATH);
			break;
		case ACCOUNT:
			sql = getAccountSql(hasSearchTerm, true);
			break;
		default:
			throw new ActionException("Invalid List Type.");
		}

		List<UpdateTitleVO> vals = new ArrayList<>(2000);
		log.debug(" sql: " + sql +"|%" + searchTerm.toLowerCase() + "%");
		try(PreparedStatement ps = dbConn.prepareCall(sql)) {
			int i = 1;
			if(hasSearchTerm) {
				ps.setString(i++, "%" + searchTerm.toLowerCase() + "%");
			}
			ResultSet rs = ps.executeQuery();
			StringBuilder main = null;
			while(rs.next()) {

				UpdateTitleVO vo = new UpdateTitleVO(rs);

				main = new StringBuilder(rs.getString("MAIN_ID"));
				if(!StringUtil.isEmpty(mainUrl)) {
					main.insert(0, mainUrl);
					vo.setMainUrl(main.toString());
				}

				vals.add(vo);
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
	@Override
	protected String getCompanySql(boolean hasSearchTerm, boolean isAutoComplete) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select company_id as MAIN_ID, company_nm as FULL_NM, short_nm_txt as SHORT_NM from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_company ");
		sql.append("where status_no = 'P' ");
		if(hasSearchTerm) {
			sql.append("and lower(company_nm) like ? ");
		}
		sql.append("order by company_nm ");
		if(isAutoComplete) sql.append("limit 100");

		return sql.toString();
	}

	/**
	 * Build market list sql
	 * @return
	 */
	@Override
	protected String getMarketSql(boolean hasSearchTerm, boolean isAutoComplete) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select market_id as MAIN_ID, market_nm as FULL_NM, short_nm as SHORT_NM from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_MARKET where status_no = 'P' ");
		if(hasSearchTerm) {
			sql.append("and lower(market_nm) like ? ");
		}
		sql.append("order by market_nm ");
		if(isAutoComplete) sql.append("limit 100");

		return sql.toString();
	}

	/**
	 * Build product list sql
	 * @return
	 */
	@Override
	protected String getProductSql(boolean hasSearchTerm, boolean isAutoComplete) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select product_id as MAIN_ID, p.product_nm as FULL_NM, p.short_nm as SHORT_NM from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_PRODUCT p ");
		sql.append("inner join custom.biomedgps_company c on p.company_id = c.company_id ");
		sql.append("where p.status_no = 'P' ");
		if(hasSearchTerm) {
			sql.append("and lower(product_nm) like ? ");
		}
		sql.append("order by product_nm ");
		if(isAutoComplete) sql.append("limit 100");

		return sql.toString();
	}
}
