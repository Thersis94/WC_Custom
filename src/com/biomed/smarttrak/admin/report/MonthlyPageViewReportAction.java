package com.biomed.smarttrak.admin.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> MonthlyPageViewReportAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Loads PageView Data over the requested months, processes
 * names and ids to be human friendly then returns a report.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2018
 ****************************************************************************/
public class MonthlyPageViewReportAction extends SimpleActionAdapter {

	private static final int BIOMED_ACCOUNT_NO = 1;

	public MonthlyPageViewReportAction() {
		super();
	}

	public MonthlyPageViewReportAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * Retrieves the PageViews for given Start and End Dates, prepares in Map
	 * for Report and returns data.
	 * @param req
	 * @return
	 */
	public Map<String, Object> retrieveData(ActionRequest req) {
		Calendar past30 = Calendar.getInstance();
		past30.add(Calendar.DATE, -30);
		Date startDt = req.getDateParameter(MonthlyPageViewReportVO.START_DT, past30.getTime());
		Date endDt = req.getDateParameter(MonthlyPageViewReportVO.END_DT, Calendar.getInstance().getTime());

		Map<String, Object> data = gatherPageViews(startDt, endDt);
		data.put(MonthlyPageViewReportVO.START_DT, startDt);
		data.put(MonthlyPageViewReportVO.END_DT, endDt);
		return data;
	}

	/**
	 * Loads PageView Data and 
	 * @param startDt
	 * @param endDt
	 * @return
	 */
	private Map<String, Object> gatherPageViews(Date startDt, Date endDt) {
		Map<String, MonthlyPageViewVO> pageViews = new LinkedHashMap<>();
		Set<String> dateHeaders = new LinkedHashSet<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getPageViewSql())) {
			int i = 1;
			ps.setInt(i++, 1);
			ps.setString(i++, AdminControllerAction.PUBLIC_SITE_ID);
			ps.setDate(i++, Convert.formatSQLDate(startDt));
			ps.setDate(i++, Convert.formatSQLDate(endDt));
			ps.setString(i++, Integer.toString(BIOMED_ACCOUNT_NO));

			log.debug(ps);
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				dateHeaders.add(rs.getString("visit_dt"));
				String requestUri = rs.getString("request_uri");
				MonthlyPageViewVO vo = pageViews.get(requestUri);
				if(vo == null) {
					vo = new MonthlyPageViewVO(rs);
					pageViews.put(vo.getRequestUri(), vo);
				} else {
					vo.addPageCount(rs.getString("visit_dt"), rs.getInt("hit_cnt"));
				}
			}
		} catch (SQLException e) {
			log.error("Error Loading PageViews", e);
		}

		Map<String, Object> reportData = new HashMap<>();
		reportData.put(MonthlyPageViewReportVO.PAGE_VIEW_DATA_KEY, pageViews.values());
		reportData.put(MonthlyPageViewReportVO.DATE_HEADER_KEY, dateHeaders);
		return reportData;
	}

	/**
	 * Build Query for loading PageViews
	 * @return
	 */
	private String getPageViewSql() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(1450);
		sql.append(DBUtil.SELECT_CLAUSE).append("concat('https://', a.site_alias_url, pu.request_uri_txt) as request_uri, ");
		sql.append("count(pu.request_uri_txt) as hit_cnt, concat(to_char(to_timestamp (pu.visit_month_no::text, 'MM'), 'Mon'), ' ', pu.visit_year_no) as visit_dt, ");
		sql.append("p.page_display_nm as section_nm, case when p.page_display_nm = 'Companies' then c.company_nm ");
		sql.append("when p.page_display_nm = 'Markets' then m.market_nm when p.page_display_nm = 'Products' then pr.product_nm ");
		sql.append("when p.page_display_nm = 'Analysis' then i.title_txt else p.page_display_nm end as page_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append("pageview_user pu ");
		sql.append(DBUtil.INNER_JOIN).append("page p on pu.page_id = p.page_id ");
		sql.append(DBUtil.INNER_JOIN).append("site_alias a on a.site_id = p.site_id and a.primary_flg = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c on pu.query_str_txt = c.company_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_product pr on pu.query_str_txt = pr.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m on pu.query_str_txt = m.market_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_insight i on pu.query_str_txt = i.insight_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("pu.site_id = ? and visit_dt between ? and ? ");
		sql.append("and pu.profile_id not in ("); 
		sql.append("select profile_id from custom.biomedgps_user u where u.account_id = ? "); 
		sql.append(") ");
		sql.append(DBUtil.GROUP_BY).append("pu.request_uri_txt, pu.visit_year_no, pu.visit_month_no, p.page_display_nm, c.company_nm, m.market_nm, pr.product_nm, i.title_txt, a.site_alias_url ");
		sql.append(DBUtil.ORDER_BY).append("pu.visit_year_no, pu.visit_month_no, pu.request_uri_txt");
		log.debug(sql.toString());
		return sql.toString();
	}
}