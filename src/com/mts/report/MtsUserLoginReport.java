package com.mts.report;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mts.subscriber.data.MTSUserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.UserLoginReport;

/****************************************************************************
 * <b>Title</b>: MtsUserLoginReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> A custom wrapper around the user login report action
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Jan 14, 2020
 * @updates:
 ****************************************************************************/
public class MtsUserLoginReport extends UserLoginReport {

	private static final long serialVersionUID = 8190916348145371293L;
	public static final String AJAX_KEY = "loginReport";


	/**
	 * 
	 */
	public MtsUserLoginReport() {
	}

	/**
	 * @param actionInit
	 */
	public MtsUserLoginReport(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param dbConn
	 * @param attributes
	 */
	public MtsUserLoginReport(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		BSTableControlVO bst = new BSTableControlVO(req, MTSUserVO.class);
		String site = StringUtil.checkVal(req.getParameter("siteData"));
		int index = site.indexOf('|');
		String siteId = site.substring(0,index);
		Date start = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");
		Date end = Convert.formatEndDate(req.getParameter("endDate"));
		String publicationId = StringUtil.checkVal(req.getParameter("publicationId"));

		
		setModuleData(getUserLoginReport(bst, start, end, siteId, publicationId));
	}
	
	
	/**
	 * this method gathers all the data for the mts user log in report and wraps it in the bootstrap table vo
	 * @param bst
	 * @param start
	 * @param end
	 * @param siteId
	 * @param order 
	 * @param sort 
	 * @return 
	 */
	private GridDataVO<MTSUserVO> getUserLoginReport(BSTableControlVO bst, Date start, Date end, String siteId, String publicationId) {
		// Add the params
		List<Object> vals = new ArrayList<>();

		StringBuilder sql = new StringBuilder(1000);
		sql.append("select mu.user_id, mu.first_nm, mu.last_nm, mu.email_address_txt, company_nm, ");
		sql.append("p.authentication_id, status_cd, al.login_dt as last_login_dt, cast (coalesce(ses.pageviews, 0) as int )as pageviews_no, al.session_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_user mu ");
		sql.append(DBUtil.INNER_JOIN).append("profile p on mu.profile_id = p.profile_id ");
		sql.append(DBUtil.INNER_JOIN).append("authentication_log al on p.authentication_id = al.authentication_id ");
		sql.append(DBUtil.INNER_JOIN).append(" ( ").append("select session_id, count(*) as pageviews ").append("from pageview_user pu ");
		sql.append("where site_id = ? and request_uri_txt not like '/portal%' ");
		
		vals.add(siteId);
		
		if(! StringUtil.isEmpty(publicationId)) {
			sql.append("and request_uri_txt like ? ");  
			vals.add(StringUtil.join("%",publicationId.toLowerCase(),"%"));
		}
		
		sql.append(" group by session_id ");
		sql.append(" ) as ses on al.session_id = ses.session_id ");
		sql.append("where mu.role_id = 'SUBSCRIBER' and al.site_id = ? ");
		
		vals.add(siteId);
		
		if(start != null && end != null) {
			sql.append("and al.login_dt between ? and ? ");
			vals.add(start);
			vals.add(end);
		}
		
		// Filter by the search box
		if (bst.hasSearch()) {
			sql.append("and (lower(mu.last_nm) like ? or lower(mu.first_nm) like ? ");
			sql.append("or lower(mu.email_address_txt) like ?) ");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append(bst.getSQLOrderBy("al.login_dt", "desc"));
		
		
		log.debug(sql + "|" + vals);
		
		// Query
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MTSUserVO(), "last_login_dt", bst);
		
	}

}
