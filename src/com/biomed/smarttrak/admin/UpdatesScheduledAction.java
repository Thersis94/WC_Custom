package com.biomed.smarttrak.admin;

//Java 1.8
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.biomed.smarttrak.action.AdminControllerAction;
//WC_Custom libs
import com.biomed.smarttrak.action.UpdatesWeeklyReportAction;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;

//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionNotAuthorizedException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.AccessControlQuery;
//WC libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteBuilderUtil;
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
public class UpdatesScheduledAction extends SBActionAdapter {

	/**
	 * No-arg constructor for initialization
	 */
	public UpdatesScheduledAction() {
		super();
	}

	/**
	 * 
	 * @param init
	 */
	public UpdatesScheduledAction(ActionInitVO init) {
		super(init);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String marketNm = req.getParameter("marketNm");
		if(!StringUtil.isEmpty(marketNm)) {
			checkUserHasMarketPermission(marketNm, req);
		}
		String emailDate = req.getParameter("date"); //the date the email was sent.  Prefer to use this to generate 'today's or "last week's" update list.
		String timeRangeCd = StringUtil.checkVal(req.getParameter("timeRangeCd"));
		String profileId = StringUtil.checkVal(req.getParameter("profileId"));
		//no profileId, no updates
		if (StringUtil.isEmpty(profileId)) return;

		//the end date is where we start subtracting from (base)
		Date endDt = !StringUtil.isEmpty(emailDate) ? Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, emailDate) : null;
		if (endDt == null) endDt = Calendar.getInstance().getTime();

		int days = UpdatesWeeklyReportAction.TIME_RANGE_WEEKLY.equalsIgnoreCase(timeRangeCd) ? 7 : 1;

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
		List<UpdateVO> updates = getUpdates(profileId, startDate, endDate);

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
	 * Verify if the user has permissions to view data attached to this market.
	 * If not, redirect.
	 * @param marketNm
	 * @param req
	 * @throws ActionNotAuthorizedException
	 */
	private void checkUserHasMarketPermission(String marketNm, ActionRequest req) throws ActionNotAuthorizedException {
		SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);

		//use the same mechanisms solr is using to verify data access permissions.
		String assetAcl = getMarketAcl(marketNm);

		String[] roleAcl = role.getAuthorizedSections();
		log.debug("user ACL=" + StringUtil.getToString(roleAcl));

		/*
		 * Verify this user has acces to the generated solr token for this market.
		 * If not, redirect them to the subscribe page.
		 */
		if (roleAcl == null || roleAcl.length == 0 || !AccessControlQuery.isAllowed(assetAcl, null, roleAcl)) {
			log.debug("user is not authorized.  Setting up redirect, then throwing exception");
			StringBuilder url = new StringBuilder(150);
			url.append(AdminControllerAction.PUBLIC_401_PG).append("?ref=").append(req.getRequestURL());
			new SiteBuilderUtil().manualRedirect(req, url.toString());
			throw new ActionNotAuthorizedException("not authorized");
		}

		log.debug("user is authorized");
	}

	/**
	 * Helper method builds a Solr like Token for a given marketNm by looking for
	 * a section under the master root that matches the given marketNm.
	 * @param marketNm
	 * @return
	 */
	private String getMarketAcl(String marketNm) {
		StringBuilder sql = new StringBuilder(400);
		String custom = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select r.solr_token_txt as root_solr, ");
		sql.append("s.solr_token_txt as gps_solr from ").append(custom);
		sql.append("biomedgps_section r ");
		sql.append("inner join ").append(custom).append("biomedgps_section s ");
		sql.append("on r.section_id = s.parent_id ");
		sql.append("where s.section_nm = ? and r.section_id = ?");

		log.debug(sql.toString());
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, marketNm);
			ps.setString(2, AbstractTreeAction.MASTER_ROOT);

			ResultSet rs = ps.executeQuery();

			//If we have a result, build the solr Token as it would appear for solr.
			if(rs.next()) {
				return "+g:" + rs.getString("root_solr") + "~" + rs.getString("gps_solr");
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return null;
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
		start.set(Calendar.HOUR,0);
		start.set(Calendar.MINUTE,0);
		start.set(Calendar.SECOND,0);

		//zero-out end date
		Calendar endDate = Calendar.getInstance();
		endDate.setTime(endDt);
		endDate.set(Calendar.HOUR,0);
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
	protected Date[] makeWeeklyDateRange(Date endDt, int days){

		Calendar cal = Calendar.getInstance();
		//set the first day to monday
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(endDt);
		cal.set(Calendar.HOUR,0);
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
	protected  List<UpdateVO> getUpdates(String profileId, Date startDt, Date endDt) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//build the query
		String sql = buildMyUpdatesSQL(schema);
		log.debug(sql + "|" + profileId + "|" + Convert.formatSQLDate(startDt) + "|" + Convert.formatSQLDate(endDt));

		UpdateVO vo = null;
		Map<String, UpdateVO>  updates = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, profileId);
			ps.setDate(2, Convert.formatSQLDate(startDt));
			ps.setDate(3, Convert.formatSQLDate(endDt));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = updates.get(rs.getString("update_id"));

				if (vo == null) {
					vo = new UpdateVO();
					vo.setUpdateId(rs.getString("update_id"));
					vo.setTitle(rs.getString("title_txt"));
					vo.setMessageTxt(rs.getString("message_txt"));
					vo.setPublishDt(rs.getDate("publish_dt"));
					vo.setTypeCd(rs.getInt("type_cd"));
					vo.setCompanyId(rs.getString("company_id"));
					vo.setCompanyNm(rs.getString("company_nm"));
					vo.setProductId(rs.getString("product_id"));
					vo.setProductNm(rs.getString("product_nm"));
					vo.setMarketId(rs.getString("market_id"));
					vo.setMarketNm(rs.getString("market_nm"));
					log.debug("loaded update: " + vo.getUpdateId());
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
	 * Builds the scheduled query
	 * @param schema
	 * @return
	 */
	protected String buildMyUpdatesSQL(String schema) {
		final String innerJoin = "inner join ";
		final String leftJoin = "left outer join ";
		StringBuilder sql = new StringBuilder(800);
		sql.append("select distinct up.update_id, up.title_txt, up.message_txt, up.publish_dt, up.type_cd, us.update_section_xr_id, us.section_id, ");
		sql.append("c.short_nm_txt as company_nm, prod.short_nm as product_nm, ");
		sql.append("coalesce(up.product_id,prod.product_id) as product_id, coalesce(up.company_id, c.company_id) as company_id, ");
		sql.append("m.short_nm as market_nm, coalesce(up.market_id, m.market_id) as market_id ");
		sql.append("from profile p ");
		sql.append(innerJoin).append(schema).append("biomedgps_user u on p.profile_id=u.profile_id ");
		sql.append(innerJoin).append(schema).append("biomedgps_account a on a.account_id=u.account_id ");
		sql.append(innerJoin).append(schema).append("biomedgps_account_acl acl on acl.account_id=a.account_id and acl.updates_no=1 ");
		sql.append(innerJoin).append(schema).append("biomedgps_section s on s.section_id=acl.section_id "); //lvl3 hierarchy
		sql.append(leftJoin).append(schema).append("biomedgps_section s2 on s.parent_id=s2.section_id "); //lvl2 hierarchy
		sql.append(leftJoin).append(schema).append("biomedgps_section s3 on s2.parent_id=s3.section_id "); //lvl1 hierarchy
		sql.append(innerJoin).append(schema).append("biomedgps_update_section us on us.section_id=s.section_id or us.section_id=s2.section_id or us.section_id=s3.section_id "); //update attached to either of the 3 hierarchy levels; acl, acl-parent, acl-grandparent
		sql.append(innerJoin).append(schema).append("biomedgps_update up on up.update_id=us.update_id ");
		sql.append(leftJoin).append(schema).append("biomedgps_product prod on up.product_id=prod.product_id ");
		sql.append(leftJoin).append(schema).append("biomedgps_company c on (up.company_id is not null and up.company_id=c.company_id) or (up.product_id is not null and prod.company_id=c.company_id) "); //join from the update, or from the product.
		sql.append(leftJoin).append(schema).append("biomedgps_market m on up.market_id=m.market_id ");
		sql.append("where p.profile_id=? and up.email_flg=1 and up.status_cd in ('R','N') and up.publish_dt >= ? and publish_dt < ? ");
		sql.append("order by up.type_cd, up.publish_dt desc ");
		return sql.toString();
	}
}