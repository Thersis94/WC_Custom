package com.biomed.smarttrak.admin.report;

// JDK 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 21, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationReportAction extends SimpleActionAdapter {

	/**
	* Constructor
	*/
	public UserUtilizationReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public UserUtilizationReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		// get user page views for the given time interval
		Map<String,Map<Integer,Integer>> userPageViews = this.retrievePageViewData(site.getSiteId(), req.getParameter("dateStart"));
		retrieveAccountData(userPageViews);
	}
	
	protected void retrieveAccountData(Map<String,Map<Integer,Integer>> userPageViews) {
		String[] profileIds = userPageViews.keySet().toArray(new String[]{});
		// build query
		
		// loop profileIds
		
		// batch retrieve by 100's
		
		// parse batches
		
		// merge with user page views
		
		// set data on report VO
	}
	
	protected Map<String, Map<Integer,Integer>> retrievePageViewData(String siteId, String dateStart) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select profile_id, visit_dt from pageview_user ");
		sql.append("where site_id = ? and profile_id is not null and visit_dt > ? ");
		sql.append("order by profile_id, visit_dt ");
		log.debug("pageview user SQL: " + sql.toString());

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, siteId);
			ps.setDate(2, Convert.formatSQLDate(checkStartDate(dateStart)));
			ResultSet rs = ps.executeQuery();
			
			String prevId = null;
			int prevMonthNo = -1;
			String currId;
			int currMonthNo = -1;
			int monthPageCnt = 0;

			// Map of month number to pageviews
			Map<Integer,Integer> userMonths =  null;
			// Map of profileId to Map of Month, pageCount
			Map< String, Map<Integer,Integer> > userPageViewsByMonth = new HashMap<>();
			Calendar cal = Calendar.getInstance();

			while (rs.next()) {
				currId = rs.getString("profile_id");
				currMonthNo = parseMonthFromDate(cal, rs.getDate("visit_dt"));
				
				if (! currId.equalsIgnoreCase(prevId)) {

					// changed users or first time through.
					if (userMonths != null) {
						// put month count on map
						userMonths.put(prevMonthNo, monthPageCnt);
						// put list on map of user|user's months
						userPageViewsByMonth.put(prevId, userMonths);
					}

					// init userMonths map
					userMonths = new HashMap<>();
					
					// init current month's view count.
					monthPageCnt = 1;
					
				} else {
					// process record for current user
					if (currMonthNo != prevMonthNo) {
						// month changed, put previous month page count on map
						userMonths.put(prevMonthNo, monthPageCnt);
						
						// reset monthCnt to 1.
						monthPageCnt = 1;
						
					} else {
						// incr this month's count
						monthPageCnt++;
					}

				}

				prevId = currId;
				prevMonthNo = currMonthNo;
			}
			
			// pick up the last record/user.
			if (userMonths != null) {
				userMonths.put(prevMonthNo, monthPageCnt);
				userPageViewsByMonth.put(prevId,userMonths);
			}
			
			return userPageViewsByMonth;
			
		} catch (SQLException sqle) {
			return new HashMap<>();
		}
		
	}
	
	/**
	 * Formats the given date into a start date.
	 * @param date
	 * @return
	 */
	protected Date checkStartDate(String date) {
		Date d;
		if (date == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, -365);
			d = Convert.formatStartDate(cal.getTime());
		} else {
			d = Convert.formatStartDate(date);
		}
		return d;
	}

	/**
	 * Parses the int value of the month for the given date.
	 * @param cal
	 * @param date
	 * @return
	 */
	protected int parseMonthFromDate(Calendar cal, Date date) {
		cal.setTime(date);
		return cal.get(Calendar.MONTH);
	}
}
