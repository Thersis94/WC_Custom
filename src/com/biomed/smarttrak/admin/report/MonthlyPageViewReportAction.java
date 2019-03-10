package com.biomed.smarttrak.admin.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.vo.MarketVO;
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

		log.debug("Loading Markets");
		data.put(MonthlyPageViewReportVO.MARKET_DATA_KEY, loadMarketSections());

		log.debug("Loading Insights");
		data.put(MonthlyPageViewReportVO.INSIGHT_DATA_KEY, loadInsightSections());
		data.put(MonthlyPageViewReportVO.START_DT, startDt);
		data.put(MonthlyPageViewReportVO.END_DT, endDt);
		return data;
	}

	/**
	 * Load Insight Sections.
	 * @return
	 */
	private List<InsightVO> loadInsightSections() {
		List<InsightVO> vos = new ArrayList<>();

		try(PreparedStatement ps = dbConn.prepareStatement(getInsightSectionSql())) {
			ResultSet rs = ps.executeQuery();

			InsightVO i = null;
			while(rs.next()) {
				if(i == null || i.getInsightId() != rs.getString("insight_id")) {
					if(i != null) {
						vos.add(i);
					}
					i = new InsightVO();
					i.setTitleTxt(rs.getString("title_txt"));
					i.setInsightId(rs.getString("insight_id"));
				}
				i.addSection(rs.getString("section_nm"));
			}

			//Add Trailing Insight
			vos.add(i);
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return vos;
	}

	/**
	 * Builds Query for loading Insight Sections.
	 * @return
	 */
	private String getInsightSectionSql() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(400);
		sql.append(DBUtil.SELECT_CLAUSE).append(" i.insight_id, i.title_txt, s.section_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema);
		sql.append("biomedgps_insight i ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_insight_section isec on i.insight_id = isec.insight_id");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_section s on isec.section_id = s.section_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_section p on s.parent_id = p.section_id ");
		sql.append(DBUtil.ORDER_BY).append("i.insight_id, p.order_no, s.order_no, i.order_no, i.insight_id");
		return sql.toString();
	}

	/**
	 * Load Market Sections
	 * @return
	 */
	private List<MarketVO> loadMarketSections() {
		List<MarketVO> vos = new ArrayList<>();

		try(PreparedStatement ps = dbConn.prepareStatement(getMarketSectionSql())) {
			ResultSet rs = ps.executeQuery();

			MarketVO m = null;
			while(rs.next()) {
				if(m == null || m.getMarketId() != rs.getString("market_id")) {
					if(m != null) {
						vos.add(m);
					}
					m = new MarketVO();
					m.setMarketName(rs.getString("market_nm"));
					m.setMarketId(rs.getString("market_id"));
				}
				m.addSection(rs.getString("section_nm"));
			}

			//Add Trailing Market
			vos.add(m);
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return vos;
	}

	/**
	 * Builds Query for loading Market Sections
	 * @return
	 */
	private String getMarketSectionSql() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(400);
		sql.append(DBUtil.SELECT_CLAUSE).append("m.market_id, m.market_nm, s.section_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema);
		sql.append("biomedgps_market m ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_market_section ms on m.market_id = ms.market_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_section s on ms.section_id = s.section_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_section p on s.parent_id = p.section_id ");
		sql.append(DBUtil.ORDER_BY).append("p.order_no, s.order_no, m.order_no, m.market_id");
		return sql.toString();
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

		log.debug("Sorting Page views");

		/*
		 * Need to sort data because we order by Dates to ensure we have
		 * our headers coming back in proper order.  Once we have our headers then
		 * re-sort to be in order by URI.
		 */
		List<MonthlyPageViewVO> pageViewData = new ArrayList<>(pageViews.values());
        Collections.sort(pageViewData, new MonthlyPageviewComparator()); 
		Map<String, Object> reportData = new HashMap<>();
		reportData.put(MonthlyPageViewReportVO.PAGE_VIEW_DATA_KEY, pageViewData);
		reportData.put(MonthlyPageViewReportVO.DATE_HEADER_KEY, dateHeaders);
		return reportData;
	}

	/**
	 * Build Query for loading PageViews
	 * @return
	 */
	private String getPageViewSql() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(1400);
		sql.append(DBUtil.SELECT_CLAUSE).append("concat('https://', a.site_alias_url, pu.request_uri_txt) as request_uri, ");
		sql.append("count(pu.request_uri_txt) as hit_cnt, concat(to_char(to_timestamp (pu.visit_month_no::text, 'MM'), 'Mon'), ' ', pu.visit_year_no) as visit_dt, ");
		sql.append("p.page_display_nm as section_nm, case when p.page_display_nm = 'Companies' then c.company_nm ");
		sql.append("when p.page_display_nm = 'Markets' then m.market_nm when p.page_display_nm = 'Products' then pr.product_nm ");
		sql.append("when p.page_display_nm = 'Analysis' then i.title_txt else p.page_display_nm end as page_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append("pageview_user pu ");

		//Join Page data for Page Name
		sql.append(DBUtil.INNER_JOIN).append("page p on pu.page_id = p.page_id ");

		//Join Site Alias for Links
		sql.append(DBUtil.INNER_JOIN).append("site_alias a on a.site_id = p.site_id and a.primary_flg = ? ");

		//Join Company info
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c ");
		sql.append("on c.company_id = pu.query_str_txt ");

		//Join Product Info
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_product pr ");
		sql.append("on pr.product_id = pu.query_str_txt ");

		//Join Market Info
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m ");
		sql.append("on m.market_id = pu.query_str_txt ");

		//Join Insight Info
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_insight i ");
		sql.append("on i.insight_id = pu.query_str_txt ");

		//Build Where Clause
		sql.append(DBUtil.WHERE_CLAUSE).append("pu.site_id = ? and visit_dt between ? and ? ");

		//Using a nested sub query was roughly 20% faster than a join with exclusion. 
		sql.append("and pu.profile_id not in ("); 
		sql.append("select profile_id from custom.biomedgps_user u where u.account_id = ? "); 
		sql.append(") ");

		sql.append(DBUtil.GROUP_BY).append("pu.request_uri_txt, pu.visit_year_no, pu.visit_month_no, p.page_display_nm, c.company_nm, m.market_nm, pr.product_nm, i.title_txt, a.site_alias_url ");
		sql.append(DBUtil.ORDER_BY).append("pu.visit_year_no, pu.visit_month_no");
		log.debug(sql.toString());
		return sql.toString();
	}
}