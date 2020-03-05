package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
// WC Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;

// SMT Base Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/********************************************************************
 * <b>Title: </b>SearchUtilAction.java<br/>
 * <b>Description: </b>Utility action to handle non-standard custom search requests via JSON<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Apr 12, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class SearchUtilAction extends SBActionAdapter {

	/**
	 * 
	 */
	public SearchUtilAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SearchUtilAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		findCompanyProductMarketSearch(req);
	}
	
	/**
	 * Performs a like search across products, markets and companies for a type ahead search
	 * @param req
	 */
	public void findCompanyProductMarketSearch(ActionRequest req) {
		String searchData = ("%" + req.getParameter("searchData") + "%").toLowerCase();
		
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String qsPath = (String) attributes.get(Constants.QS_PATH);
		String companyPath = AdminControllerAction.Section.COMPANY.getPageURL() + qsPath;
		String marketPath = AdminControllerAction.Section.MARKET.getPageURL() + qsPath;
		String productPath = AdminControllerAction.Section.PRODUCT.getPageURL() + qsPath;
		String analysisPath = AdminControllerAction.Section.INSIGHT.getPageURL() + qsPath;
		log.debug("Company Path: " + companyPath);
		
		StringBuilder sql = new StringBuilder(400);
		sql.append("select '").append(companyPath).append("' + company_id, short_nm_txt, 'Companies:' as RESULT_TYPE from ").append(schema).append("biomedgps_company ");
		sql.append("where lower(company_nm) like ? and status_no = 'P' ");
		sql.append("union ");
		sql.append("select '").append(productPath).append("' + product_id, short_nm, 'Products:' as RESULT_TYPE from ").append(schema).append("biomedgps_product ");
		sql.append("where lower(product_nm) like ? and status_no = 'P' ");
		sql.append("union ");
		sql.append("select '").append(marketPath).append("' + market_id, market_nm, 'Markets:' as RESULT_TYPE from ").append(schema).append("biomedgps_market ");
		sql.append("where lower(market_nm) like ? and status_no = 'P' ");
		sql.append("union ");
		sql.append("select '").append(analysisPath).append("' + insight_id, title_txt, 'Analyses:' as RESULT_TYPE from ").append(schema).append("biomedgps_insight ");
		sql.append("where lower(title_txt) like ? and status_cd = 'P' ");
		sql.append("order by RESULT_TYPE, short_nm_txt ");
		log.debug("SQL: " + sql + "|" + searchData);
		
		List<GenericVO> data = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, searchData);
			ps.setString(2, searchData);
			ps.setString(3, searchData);
			ps.setString(4, searchData);
			ResultSet rs = ps.executeQuery();
			String type = "";
			while (rs.next()) {
				if (!type.equals(rs.getString(3))) {
					data.add(new GenericVO("", rs.getString(3)));
					type = rs.getString(3);
				}
				data.add(new GenericVO(rs.getString(1), rs.getString(2)));
			}
			
			this.putModuleData(data);
		} catch(SQLException sqle) {
			String msg = "Unable to retrieve query for company, product and market"; 
			log.error(msg, sqle);
			this.putModuleData(null, 0, false, msg, true);
		}
	}
}
