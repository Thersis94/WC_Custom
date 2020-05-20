package com.biomed.smarttrak.util;

//Java 8
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//WC Custom libs
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.UserVO.LicenseType;
//SB libs
import com.smt.sitebuilder.util.PageViewRetriever;
import com.smt.sitebuilder.util.PageViewUserVO;

/****************************************************************************
 * Title: SmarttrakPageViewRetriever.java <p/>
 * Project: WC_Custom <p/>
 * Description: Custom Pageview retriever for Smarttrak that fetches additional data from smartttrak tables<p/>
 * Copyright: Copyright (c) 2018<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Feb 12, 2018
 ****************************************************************************/

public class SmarttrakPageViewRetriever extends PageViewRetriever {
	private String customSchema;
	
	/**
	 * @param dbConn
	 */
	public SmarttrakPageViewRetriever(Connection dbConn) {
		super(dbConn);
	}
	
	/**
	 * @param dbConn
	 */
	public SmarttrakPageViewRetriever(Connection dbConn, String customSchema) {
		super(dbConn);
		setCustomSchema(customSchema);
	}
	
	/**
	 * Formats the standard (non-rollup) query for smarttrak to include page titles for various sections
	 */
	@Override
	protected StringBuilder formatStandardQuery() {
		boolean isSiteAdmin = getProfileId() == null;
		StringBuilder sql = new StringBuilder(1000);
		/*if this page is a market, company, product, or insight and has a query string, go fetch it's name
		 *otherwise default to it's page name*/
		sql.append("select a.pageview_user_id, a.site_id, a.profile_id, a.session_id, a.page_id, ");
		sql.append("b.page_display_nm, a.request_uri_txt, a.query_str_txt, a.src_pageview_id, visit_dt, ");
		sql.append("(CASE WHEN request_uri_txt like '/markets%' and query_str_txt is not null then ");
		sql.append("(select market_nm from ").append(customSchema).append("biomedgps_market where market_id = query_str_txt) ");
		sql.append("WHEN request_uri_txt like '/companies%' and query_str_txt is not null then ");
		sql.append("(select company_nm from ").append(customSchema).append("biomedgps_company where company_id = query_str_txt) ");
		sql.append("WHEN request_uri_txt like '/analysis%' and query_str_txt is not null then ");
		sql.append("(select title_txt from ").append(customSchema).append("biomedgps_insight where insight_id = query_str_txt) ");
		sql.append("WHEN request_uri_txt like '/products%' and query_str_txt is not null then ");
		sql.append("(select product_nm from ").append(customSchema).append("biomedgps_product where product_id = query_str_txt) ");
		sql.append("ELSE b.page_display_nm END) as page_title_nm ");
		sql.append("from pageview_user a left outer join page b on a.page_id = b.page_id ");
		if(isSiteAdmin) {
			sql.append("inner join profile_role c on a.profile_id = c.profile_id inner join role d on c.role_id = d.role_id ");
			sql.append("left join ").append(customSchema).append("biomedgps_user u on u.profile_id = a.profile_id ");
		}
		formatCommonQuery(sql);
		if(isSiteAdmin) {
			sql.append(" and d.role_order_no < ? ");
			sql.append(" and u.status_cd != ? "); //if this is a site admin, filter out staff roles
		}
		sql.append("order by a.visit_dt ");
		
		log.debug("Smarttrak page view query: " + sql);
		return sql;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.util.PageViewRetriever#formatPreparedStatement(java.sql.PreparedStatement)
	 */
	@Override
	protected int formatPreparedStatement(PreparedStatement ps) throws SQLException {
		//call super and return the current index
		int currentIdx = super.formatPreparedStatement(ps);
		
		//if this is a site admin, set the id for staff
		if(getProfileId() == null) {
			ps.setInt(currentIdx++, AdminControllerAction.STAFF_ROLE_LEVEL);
			ps.setString(currentIdx++, LicenseType.STAFF.getCode());
		}
		
		return currentIdx;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.util.PageViewRetriever#generatePageViewVO(java.sql.ResultSet)
	 */
	@Override
	protected PageViewUserVO generatePageViewVO(ResultSet rs) throws SQLException {
		PageViewUserVO pageView = super.generatePageViewVO(rs);
		pageView.setPageTitleName(rs.getString("page_title_nm")); //set the custom field onto the vo
		return pageView;
	}
	
	/**
	 * @return the customSchema
	 */
	public String getCustomSchema() {
		return customSchema;
	}

	/**
	 * @param customSchema the customSchema to set
	 */
	public void setCustomSchema(String customSchema) {
		this.customSchema = customSchema;
	}

}
