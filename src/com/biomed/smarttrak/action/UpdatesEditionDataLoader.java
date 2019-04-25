package com.biomed.smarttrak.action;

//Java 1.8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.admin.UpdatesManageReportAction;
//WC Custom
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;
//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
//WC libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: UpdatesScheduledAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Handles retrieving the updates for a scheduled email send.
 * "My Updates" - the user will login before seeing this page.<p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Apr 10, 2017
 ****************************************************************************/
public class UpdatesEditionDataLoader extends SimpleActionAdapter {

	private static final Pattern HREF_START_REGEX = Pattern.compile("href([ ]{0,1})=([ ]{0,1})(['\"])");
	public static final String REDIRECT_DEST = "redirectDestination";
	protected static final String IS_MANAGE_TOOL = "isManageTool";
	private StringEncoder se;

	/**
	 * No-arg constructor for initialization
	 */
	public UpdatesEditionDataLoader() {
		super();
		se = new StringEncoder();
	}

	/**
	 * 
	 * @param init
	 */
	public UpdatesEditionDataLoader(ActionInitVO init) {
		super(init);
		se = new StringEncoder();
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String emailDate = req.getParameter("date"); //the date the email was sent.  Prefer to use this to generate 'today's or "last week's" update list.
		String timeRangeCd = StringUtil.checkVal(req.getParameter("timeRangeCd"));
		//Get Date Range off Request (Set in DataSource)
		int dailyRange = req.getIntegerParameter("dailyRange", 1);

		//Protect Date Range.
		dailyRange = Math.min(Math.max(dailyRange, -7), 7);

		String profileId = StringUtil.checkVal(req.getParameter("profileId"));

		//the end date is where we start subtracting from (base)
		Date endDt = !StringUtil.isEmpty(emailDate) ? Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, emailDate) : null;
		if (endDt == null) endDt = Calendar.getInstance().getTime();

		int days = UpdatesManageReportAction.TIME_RANGE_WEEKLY.equalsIgnoreCase(timeRangeCd) ? 7 : dailyRange;

		//if today is monday and the range is 1 (daily), we'll need to rollback to Friday as a start date (days=3)
		if (days == 1) {
			Calendar end = Calendar.getInstance();
			end.setTime(endDt);
			if (Calendar.MONDAY == end.get(Calendar.DAY_OF_WEEK)) days = 3;
		}

		//establish the date ranges
		Date[] endpoints = establishDateRanges(endDt, days);
		//pull values from map
		Date startDate = endpoints[0];
		Date endDate = endpoints[1];

		//get list of updates
		List<UpdateVO> updates = loadUpdates(req, profileId, startDate, endDate);
		loadAnnouncements(req, startDate, endDate, updates);

		//set cosmetic label
		Calendar dt = Calendar.getInstance();
		dt.setTime(endDate);
		dt.add(Calendar.SECOND, -1); //rollback one second before midnight, so the date label looks correct
		String label = Convert.formatDate(startDate, days == 1 ? "MMM dd, YYYY" : "MMM dd");
		if (days > 1) label += " - " + Convert.formatDate(dt.getTime(), "MMM dd, YYYY");
		req.setAttribute("dateRange", label);

		putModuleData(updates);
	}

	
	/**
	 * Load the special announcement type updates that do not have market sections.
	 * @param req
	 * @param startDate
	 * @param endDate
	 * @param updates
	 */
	private void loadAnnouncements(ActionRequest req, Date startDate, Date endDate, List<UpdateVO> updates) {
		String sql = getAnnouncementSql((String)getAttribute(Constants.CUSTOM_DB_SCHEMA), Convert.formatBoolean(req.getParameter("orderSort")));
		DBProcessor db = new DBProcessor(dbConn);
		boolean redirectLinks = Convert.formatBoolean(req.getParameter("redirectLinks"));
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, AdminControllerAction.PUBLIC_SITE_ID);
			ps.setDate(2, Convert.formatSQLDate(startDate));
			ps.setDate(3, Convert.formatSQLDate(endDate));
			
			ResultSet rs = ps.executeQuery();
			String baseUrl = "";
			
			while(rs.next()) {
				UpdateVO up = new UpdateVO();
				db.executePopulate(up, rs);
				up.setProductNm(StringEncoder.encodeExtendedAscii(up.getProductNm()));
				up.setMarketNm(StringEncoder.encodeExtendedAscii(up.getMarketNm()));
				up.setCompanyNm(StringEncoder.encodeExtendedAscii(up.getCompanyNm()));
				up.setTitle(StringEncoder.encodeExtendedAscii(up.getTitle()));
				if (baseUrl.isEmpty()) baseUrl = buildBaseUrl(up.getSSLFlg(), up.getSiteAliasUrl());
				if (redirectLinks) up.setMessageTxt(buildRedirectLinks(up.getMessageTxt(), baseUrl));
				updates.add(up);
			}
		} catch(Exception e) {
			log.error(e);
		}		
		/**
		 * This does not work due to an bug in DBProcessor details in the SMTInteral ticket GC-74
		 * This code has been commented out instead of deleted to provide whoever works on that ticket
		 * an easy way to replicate the issue and allow them to restore the DBProcessor version of this
		 * method when the error has been fixed.
		List<Object> params = new ArrayList<>(2);
		params.add(AdminControllerAction.PUBLIC_SITE_ID);
		params.add(Convert.formatSQLDate(startDate));
		params.add(Convert.formatSQLDate(endDate));
		
		
		for (Object o : results) {
			UpdateVO up = (UpdateVO)o;
			if (redirectLinks) up.setMessageTxt(buildRedirectLinks(up.getMessageTxt(), baseUrl));
			up.setProductNm(StringEncoder.encodeExtendedAscii(up.getProductNm()));
			up.setMarketNm(StringEncoder.encodeExtendedAscii(up.getMarketNm()));
			up.setCompanyNm(StringEncoder.encodeExtendedAscii(up.getCompanyNm()));
			up.setTitle(StringEncoder.encodeExtendedAscii(up.getTitle()));
			updates.add(up);
		}
		**/
	}
	
	
	/**
	 * Build the base url from the supplied alias and ssl level.
	 * @param sslFlg
	 * @param siteAliasUrl
	 * @return
	 */
	private String buildBaseUrl(int sslFlg, String siteAliasUrl) {
		StringBuilder url = new StringBuilder(75);
		url.append(sslFlg == SiteVO.SITE_SSL ? "https://" : "http://");
		url.append(siteAliasUrl).append(Section.UPDATES_EDITION.getPageURL());
		return url.toString();
	}

	
	/**
	 * Change all links in this content to point to the smarttrak site
	 * where the user will only be directed to the desired location
	 * if they are logged in.
	 * @param text
	 * @param baseUrl
	 * @return
	 */
	private String buildRedirectLinks(String text, String baseUrl) {
		if (StringUtil.isEmpty(text)) return text;
		
		Matcher matcher = HREF_START_REGEX.matcher(text);
		StringBuilder newText = new StringBuilder(text.length() + 200);
		int curLoc = 0;
		while(matcher.find()) {
			// Get the start of a link's href property
			int valueStart = matcher.end();
			// Append all text from the current location to here
			newText.append(text.substring(curLoc, valueStart));
			// Get the proper wrapper for the property value " or ' 
			// so that we can get the whole property value
			char propEndcap = text.charAt(valueStart-1);
			curLoc = text.indexOf(propEndcap, valueStart);
			// Append the redirect link and continue
			newText.append(buildRedirectHref(text.substring(valueStart, curLoc), baseUrl));
		}
		// Append the remainder of the content
		newText.append(text.substring(curLoc));
		
		return newText.toString();
	}
	
	
	/**
	 * Build a redirect link based off the original link
	 * @param link
	 * @param baseUrl
	 * @return
	 */
	private String buildRedirectHref(String link, String baseUrl) {
		StringBuilder redirectLink = new StringBuilder(250);
		// Replace ampersands so that they are not lost between login and redirect.
		if (link.contains("&amp;"))
			link = link.replaceAll("&amp;", "|");
		redirectLink.append(baseUrl).append("?");
		redirectLink.append(REDIRECT_DEST).append("=").append(StringEncoder.urlEncode(se.decodeValue(link)));
		return redirectLink.toString();
	}
	
	
	/**
	 * Build the sql for the announcement updates.
	 * @param schema
	 * @param orderSort
	 * @return
	 */
	protected String getAnnouncementSql(String schema, boolean orderSort) {
		StringBuilder sql = new StringBuilder(350);
		sql.append("select up.*, sa.site_alias_url, st.ssl_flg from ").append(schema).append("biomedgps_update up ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site st on st.site_id = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site_alias sa on st.site_id = sa.site_id and sa.primary_flg = 1 ");
		sql.append("where coalesce(publish_dt, up.create_dt) >= ? and coalesce(publish_dt, up.create_dt) < ? ");
		sql.append("and announcement_type > 0 ");
		sql.append("order by announcement_type, type_cd, ");
		if (orderSort) {
			sql.append("coalesce(up.order_no,0), coalesce(up.publish_dt, up.create_dt) desc ");
		} else {
			sql.append("coalesce(up.publish_dt, up.create_dt) desc, coalesce(up.order_no,0) ");
		}
		return sql.toString();
	}

	/**
	 * Helper method that establishes the appropriate days
	 * @param days
	 * @return
	 */
	protected Date[] establishDateRanges(Date endDt, int days) {
		if (days == 7) {
			return makeWeeklyDateRange(endDt, days);
		} else {
			return makeDailyDateRange(endDt, days);
		}
	}

	/**
	 * Returns the daily date range of Dates
	 * @param endDt
	 * @param daysToGoBack
	 * @return
	 */
	protected Date[] makeDailyDateRange(Date endDt, int days) {
		//subtract X days from the base date for start date
		Calendar start = Calendar.getInstance();
		start.setTime(endDt);
		start.add(Calendar.DATE, 0-days);
		start.set(Calendar.HOUR_OF_DAY,0);
		start.set(Calendar.MINUTE,0);
		start.set(Calendar.SECOND,0);

		//zero-out end date
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(endDt);
		endDate.set(Calendar.HOUR_OF_DAY,0);
		endDate.set(Calendar.MINUTE,0);
		endDate.set(Calendar.SECOND,0);

		//add the start/end dates and daysToGoBack to collection.
		return new Date[]{ start.getTime(), endDate.getTime()};
	}


	/**
	 *  Returns the weekly date range of Dates
	 * @param endDt
	 * @param daysToGoBack
	 * @return
	 */
	protected Date[] makeWeeklyDateRange(Date endDt, int days) {
		Calendar cal = Calendar.getInstance();
		//set the first day to monday
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(endDt);
		cal.set(Calendar.HOUR_OF_DAY,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);

		//subtract that from the end date to get start range. Then go back a week(previous week)
		cal.add(Calendar.DATE, -(cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek()));
		cal.add(Calendar.DATE, -days);
		Date startDt = cal.getTime();

		//go seven days out to get the end range
		cal.add(Calendar.DATE, 7);

		//add the start/end dates and daysToGoBack to collection.
		return new Date[]{ startDt, cal.getTime()};
	}


	/**
	 * Returns a list of scheduled updates for a specified profile
	 * @param profileId
	 * @param timeRangeCd
	 * @return
	 */
	protected  List<UpdateVO> loadUpdates(ActionRequest req, String profileId, Date startDt, Date endDt) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//build the query
		String sql;
		String[] sectionIds = null;
		boolean orderSort = Convert.formatBoolean(req.getParameter("orderSort"));
		if (req.getAttribute(IS_MANAGE_TOOL) != null) { //set by the subclass
			sectionIds = req.getParameterValues("sectionId");
			if (sectionIds == null || sectionIds.length == 0 || "ALL".equalsIgnoreCase(sectionIds[0])) 
				sectionIds = null; //consolidate alt scenarios
			sql = buildManageUpdatesSQL(schema, sectionIds, orderSort);
		} else {
			boolean isEmail = false;
			// If this is being called for an email the data needs to be
			// restricted to the user's set viewable content.
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			if (site == null) isEmail = true;
			sql = StringUtil.isEmpty(profileId) ? buildAllUpdatesSQL(schema, orderSort) : buildMyUpdatesSQL(schema, orderSort, isEmail);
		}
		log.debug(sql + "|" + profileId + "|" + Convert.formatSQLDate(startDt) + "|" + Convert.formatSQLDate(endDt));

		int x=0;
		UpdateVO vo = null;
		Map<String, UpdateVO>  updates = new LinkedHashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(++x, AdminControllerAction.PUBLIC_SITE_ID);
			if (!StringUtil.isEmpty(profileId)) ps.setString(++x, profileId);
			ps.setDate(++x, Convert.formatSQLDate(startDt));
			ps.setDate(++x, Convert.formatSQLDate(endDt));
			if (sectionIds != null) {
				for (String sec : sectionIds)
					ps.setString(++x, sec);
			}
			String baseUrl = "";
			boolean redirectLinks = Convert.formatBoolean(req.getParameter("redirectLinks"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = updates.get(rs.getString("update_id"));

				if (vo == null) {
					vo = new UpdateVO();
					vo.setUpdateId(rs.getString("update_id"));
					vo.setTitle(StringEncoder.encodeExtendedAscii(rs.getString("title_txt")));
					vo.setMessageTxt(rs.getString("message_txt"));
					vo.setPublishDt(rs.getDate("publish_dt"));
					vo.setTypeCd(rs.getInt("type_cd"));
					vo.setCompanyId(rs.getString("company_id"));
					vo.setCompanyNm(StringEncoder.encodeExtendedAscii(rs.getString("company_nm")));
					vo.setProductId(rs.getString("product_id"));
					vo.setProductNm(StringEncoder.encodeExtendedAscii(rs.getString("product_nm")));
					vo.setMarketId(rs.getString("market_id"));
					vo.setMarketNm(StringEncoder.encodeExtendedAscii(rs.getString("market_nm")));
					vo.setStatusCd(rs.getString("status_cd"));
					vo.setEmailFlg(rs.getInt("email_flg"));
					vo.setQsPath((String)attributes.get(Constants.QS_PATH));
					vo.setSSLFlg(rs.getInt("ssl_flg"));
					vo.setSiteAliasUrl(rs.getString("site_alias_url"));
					vo.setOrderNo(rs.getInt("order_no"));

					// If we have not created the base url yet do so with this data
					if (baseUrl.isEmpty()) baseUrl = buildBaseUrl(vo.getSSLFlg(), vo.getSiteAliasUrl());
					if (redirectLinks) vo.setMessageTxt(buildRedirectLinks(vo.getMessageTxt(), baseUrl));

					//log.debug("loaded update: " + vo.getUpdateId())
				}

				//add the new section to it
				UpdateXRVO xrvo = new UpdateXRVO(vo.getUpdateId(), rs.getString("section_id"));
				xrvo.setUpdateSectionXrId(rs.getString("update_section_xr_id"));
				vo.addUpdateXrVO(xrvo);

				updates.put(vo.getUpdateId(), vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load updates", sqle);
		}

		log.debug("loaded " + updates.size() + " updates");
		return new ArrayList<>(updates.values());
	}


	/**
	 * Builds the webpage query
	 * @param schema
	 * @return
	 */
	protected String buildMyUpdatesSQL(String schema, boolean orderSort, boolean isEmail) {
		StringBuilder sql = new StringBuilder(800);
		appendSelect(sql);
		sql.append("from profile p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_user u on p.profile_id=u.profile_id and u.active_flg = 1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_account a on a.account_id=u.account_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_account_acl acl on acl.account_id=a.account_id and acl.updates_no=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_section s on s.section_id=acl.section_id "); //lvl3 hierarchy
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_section s2 on s.parent_id=s2.section_id "); //lvl2 hierarchy
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_section s3 on s2.parent_id=s3.section_id "); //lvl1 hierarchy
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_update_section us on us.section_id in (s.section_id,s2.section_id,s3.section_id) "); //update attached to either of the 3 hierarchy levels; acl, acl-parent, acl-grandparent
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_update up on up.update_id=us.update_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_product prod on up.product_id=prod.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c on c.company_id=coalesce(up.company_id,prod.company_id) "); //join from the update, or from the product. Prefer company
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m on up.market_id=m.market_id ");
		if (isEmail) sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_user_updates_skip uus on uus.user_id = u.user_id and uus.section_id in (us.section_id, s2.parent_id, s3.parent_id, s3.section_id) ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site st on st.site_id = ?");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site_alias sa on st.site_id = sa.site_id and sa.primary_flg = 1");
		sql.append("where p.profile_id=? and up.email_flg=1 and up.status_cd in ('R','N') ");
		sql.append("and coalesce(up.publish_dt, up.create_dt) >= ? and coalesce(up.publish_dt, up.create_dt) < ? ");
		// Filter out anything that should be skipped
		if (isEmail) sql.append("and uus.user_updates_skip_id is null ");
		// Determine whether order no or publish dt has priority in the sort.
		sql.append("order by up.type_cd, ");
		if (orderSort) {
			sql.append("coalesce(up.order_no,0), coalesce(up.publish_dt, up.create_dt) desc");
		} else {
			sql.append("coalesce(up.publish_dt, up.create_dt) desc, coalesce(up.order_no,0) ");
		}
		log.debug(sql);
		return sql.toString();
	}

	/**
	 * Builds the scheduled email query
	 * @param schema
	 * @param req - used by subclasses
	 * @return
	 */
	protected String buildAllUpdatesSQL(String schema, boolean orderSort) {
		StringBuilder sql = new StringBuilder(800);
		appendSelect(sql);
		sql.append("from ").append(schema).append("biomedgps_update_section us ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_update up on up.update_id=us.update_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_product prod on up.product_id=prod.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c on c.company_id=coalesce(up.company_id,prod.company_id) "); //join from the update, or from the product. Prefer company
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m on up.market_id=m.market_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site st on st.site_id = ?");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site_alias sa on st.site_id = sa.site_id and sa.primary_flg = 1");
		sql.append("where up.email_flg=1 and up.status_cd in ('R','N') ");
		sql.append("and coalesce(up.publish_dt, up.create_dt) >= ? and coalesce(up.publish_dt, up.create_dt) < ? ");
		sql.append("order by up.type_cd, ");
		// Determine whether order no or publish dt has priority in the sort.
		if (orderSort) {
			sql.append("coalesce(up.order_no,0), coalesce(up.publish_dt, up.create_dt) desc");
		} else {
			sql.append("coalesce(up.publish_dt, up.create_dt) desc, coalesce(up.order_no,0) ");
		}
		return sql.toString();
	}


	/**
	 * builds the /manage tool query (Manage Updates page)
	 * @param schema
	 * @param sectionIds
	 * @return
	 */
	protected String buildManageUpdatesSQL(String schema, String[] sectionIds, boolean orderSort) {
		StringBuilder sql = new StringBuilder(800);
		appendSelect(sql);
		sql.append("from ").append(schema).append("biomedgps_update_section us ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_update up on up.update_id=us.update_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_product prod on up.product_id=prod.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c on c.company_id=coalesce(up.company_id,prod.company_id) "); //join from the update, or from the product. Prefer company
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m on up.market_id=m.market_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site st on st.site_id = ?");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("site_alias sa on st.site_id = sa.site_id and sa.primary_flg = 1");
		sql.append("where coalesce(up.publish_dt, up.create_dt) >= ? and coalesce(up.publish_dt, up.create_dt) < ? ");

		//note this query ignores email_flg=1 (compared to the two above)

		//without a section only show un-reviewed (New) status level
		sql.append("and up.status_cd in ('N', 'R') ");
		sql.append("order by up.type_cd, ");

		if (orderSort) {
			sql.append("coalesce(up.order_no,0), coalesce(up.publish_dt, up.create_dt) desc ");
		} else {
			sql.append("coalesce(up.publish_dt, up.create_dt) desc, coalesce(up.order_no,0) ");
		}
		return sql.toString();
	}


	/**
	 * The select columns for all 3 queries - ensures consistency.  If you change this make sure all 3 scenarios get tested!
	 * @param sql
	 */
	private void appendSelect(StringBuilder sql) {
		sql.append("select distinct up.update_id, up.title_txt, up.message_txt, coalesce(up.publish_dt, up.create_dt) as publish_dt, up.status_cd, up.email_flg, ");
		sql.append("up.type_cd, coalesce(up.order_no,0) as order_no, us.update_section_xr_id, us.section_id, ");
		sql.append("c.short_nm_txt as company_nm, prod.short_nm as product_nm, ");
		sql.append("coalesce(up.product_id,prod.product_id) as product_id, coalesce(up.company_id, c.company_id) as company_id, ");
		sql.append("m.short_nm as market_nm, coalesce(up.market_id, m.market_id) as market_id, sa.site_alias_url, st.ssl_flg ");
	}
}