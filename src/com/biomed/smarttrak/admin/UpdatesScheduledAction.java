package com.biomed.smarttrak.admin;

//Java 1.8
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//WC_Custom libs
import com.biomed.smarttrak.action.UpdatesWeeklyReportAction;
import com.biomed.smarttrak.vo.UpdateVO;
//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
//WC libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: UpdatesScheduledAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Handles retrieving the updates for a scheduled email send. "My Updates" - the user will login before seeing this page.<p/>
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
		String emailDate = req.getParameter("date"); //the date the email was sent.  Prefer to use this to generate 'today's or "last week's" update list.
		String timeRangeCd = StringUtil.checkVal(req.getParameter("timeRangeCd"));
		String profileId = StringUtil.checkVal(req.getParameter("profileId"));
		//no profileId, no updates
		if (StringUtil.isEmpty(profileId)) return;

		//the end date is where we start subtracting from (base)
		Date endDt = !StringUtil.isEmpty(emailDate) ? Convert.formatDate(Convert.DATE_SLASH_PATTERN, emailDate) : null;
		if (endDt == null) endDt = Calendar.getInstance().getTime();

		int days = UpdatesWeeklyReportAction.TIME_RANGE_WEEKLY.equalsIgnoreCase(timeRangeCd) ? 7 : 1;
		
		//establish the date ranges
		Map<Integer, List<Date>> dateRangeMap = establishDateRanges(endDt, days);
		
		Set<Entry<Integer, List<Date>>> entryRange = dateRangeMap.entrySet();
		for (Entry<Integer, List<Date>> entry : entryRange) {
			//pull values from map
			int daysToGoBack = entry.getKey();
			List<Date> dateRanges = entry.getValue();
			Date startDate = dateRanges.get(0);
			Date endDate = dateRanges.get(1);
			
			//get list of updates
			List<Object> updates = getUpdates(profileId, startDate, endDate);

			//set cosmetic label
			String label = Convert.formatDate(startDate, daysToGoBack == 1 ? "MMM dd, YYYY" : "MMM dd");
			if (daysToGoBack > 1) label += " - " + Convert.formatDate(endDate, "MMM dd, YYYY");
			req.setAttribute("dateRange", label);

			putModuleData(updates);			
		}

	}
	
	/**
	 * Helper method that establishes the appropriate days
	 * @param days
	 * @return
	 */
	protected Map<Integer, List<Date>> establishDateRanges(Date endDt, int days){
		Map<Integer, List<Date>> dateRanges;
		if(days == 1){
			dateRanges = makeDailyDateRange(endDt, days);
		}else{
			dateRanges = makeWeeklyDateRange(endDt, days);
		}
		
		return dateRanges;
	}
	
	/**
	 * Returns the daily date range of Dates
	 * @param endDt
	 * @param daysToGoBack
	 * @return
	 */
	protected Map<Integer, List<Date>> makeDailyDateRange(Date endDt, int days){
		Map<Integer, List<Date>> dailyRangeMap = new HashMap<>();
		List<Date> dailyDateRange = new ArrayList<>();
		
		//subtract X days from the base date for start date
		Calendar start = Calendar.getInstance();
		start.setTime(endDt);
		start.add(Calendar.DATE, 0-days);
		
		//if today is monday and the range is 1 (daily), rollback to Friday as a start date
		int daysToGoBack = days;
		if (days == 1 && Calendar.MONDAY == start.get(Calendar.DAY_OF_WEEK)+1) {
			daysToGoBack = 3;
			start.add(Calendar.DATE, -2); //already on Sunday, go back Saturday & Friday.
		}
		
		//add the start/end dates and daysToGoBack to collection.
		dailyDateRange.add(start.getTime());
		dailyDateRange.add(endDt);
		dailyRangeMap.put(daysToGoBack, dailyDateRange);
		
		return dailyRangeMap;
	}
	
	/**
	 *  Returns the weekly date range of Dates
	 * @param endDt
	 * @param daysToGoBack
	 * @return
	 */
	protected Map<Integer, List<Date>> makeWeeklyDateRange(Date endDt, int days){
		Map<Integer, List<Date>> weeklyRangeMap = new HashMap<>();
		List<Date> weeklyDateRange = new ArrayList<>();
		
		Calendar cal = Calendar.getInstance();
		//set the first day to monday
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(endDt);
		
		//subtract that from the end date to get start range. Then go back a week(previous week)
		cal.add(Calendar.DATE, -(cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek()));
		cal.add(Calendar.DATE, -days);
		Date startDt = cal.getTime();
		
		//go seven days out to get the end range (index starts at 0)
		cal.add(Calendar.DATE, 6); 
		Date weekEndDt = cal.getTime();				
	
		//add the start/end dates and daysToGoBack to collection.
		weeklyDateRange.add(startDt);
		weeklyDateRange.add(weekEndDt);
		weeklyRangeMap.put(days, weeklyDateRange);
		
		return weeklyRangeMap;	
	}

	/**
	 * Returns a list of scheduled updates for a specified profile
	 * @param profileId
	 * @param timeRangeCd
	 * @return
	 */
	protected  List<Object> getUpdates(String profileId, Date startDt, Date endDt) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		//build the query
		String sql = buildMyUpdatesSQL(schema);
		log.debug(sql + "|" + profileId + "|" + startDt + "|" + endDt);

		//build params
		List<Object> params = new ArrayList<>();
		params.add(profileId);
		params.add(startDt);
		params.add(endDt);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
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