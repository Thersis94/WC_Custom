package com.depuysynthesinst.registration;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.registration.RegistrationDataAction;

/****************************************************************************
 * <b>Title</b>: DSIRegistrationDataAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> extends the wc registration report and via overide allows 
 * for a custom excel sheet to be returned.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Nov 21, 2016<p/>
 * @updates:
 ****************************************************************************/
public class DSIRegistrationDataAction extends RegistrationDataAction {

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.registration.RegistrationDataAction#buildReport(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	protected AbstractSBReportVO buildReport(SMTServletRequest req) {
			return new DSIRegistrationDataActionVO();
	}
	

	/**
	 * moved to a protected method so it can be overrid in extending classes
	 * @param ps
	 * @param regId 
	 * @param regSubtlId 
	 * @param end 
	 * @param start 
	 * @param useDates 
	 * @throws SQLException 
	 */
	@Override
	protected void setSqlVars(PreparedStatement ps, int i, String regId, boolean useDates, String start, String end, String regSubtlId ) throws SQLException {
		ps.setString(++i, regId);

		if (useDates) {
			ps.setDate(++i, Convert.formatSQLDate(Convert.formatStartDate(start, "1/1/2000")));
			ps.setDate(++i, Convert.formatSQLDate(Convert.formatEndDate(end)));
		}
		if (regSubtlId != null) ps.setString(++i, regSubtlId);
	}
	
	/**
	 * builds the sql script for pulling the required data.
	 * @param req
	 * @param regId 
	 * @param end 
	 * @param start 
	 * @param regSubtlId 
	 * @param useDates 
	 * @return
	 */
	@Override
	protected String buildSql(SMTServletRequest req, boolean useDates, String regSubtlId, String regId) {
		log.debug("over ride");
		//"actionId" used in this query is actually the actionGroupId, which aligns all historical data
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.register_submittal_id, a.register_field_id, value_txt, ");
		sql.append("data_enc_flg, b.profile_id, b.create_dt, b.site_id, c.html_type_id, ");
		sql.append("pr.role_expire_dt, s.site_nm, r.role_nm, st.status_nm ");
		sql.append("from register_data a ");
		sql.append("inner join register_submittal b on a.register_submittal_id = b.register_submittal_id ");
		sql.append("inner join register_field c on a.register_field_id=c.register_field_id ");
		sql.append("left outer join site s on b.site_id=s.site_id ");
		sql.append("left outer join profile p on b.profile_id=p.profile_id ");
		sql.append("left outer join profile_role pr on p.profile_id=pr.profile_id and pr.site_id=b.site_id ");
		sql.append("left outer join role r on pr.role_id=r.role_id ");
		sql.append("left outer join status st on pr.status_id=st.status_id ");
		sql.append("where b.action_id=? ");
		if (useDates) sql.append("and b.create_dt between ? and ? ");
		if (regSubtlId != null) sql.append("and b.register_submittal_id=? ");
		sql.append("and (b.robot_flg is null or b.robot_flg=0) ");

		sql.append("group by a.register_submittal_id, a.register_field_id, value_txt, ");
		sql.append("data_enc_flg, b.profile_id, b.create_dt, b.site_id, c.html_type_id, ");
		sql.append("pr.role_expire_dt, s.site_nm, r.role_nm, st.status_nm ");

		sql.append("order by ");
		if (req.getParameter("orderBy") != null) {
			String ob = req.getParameter("orderBy");
			if ("dateDesc".equalsIgnoreCase(ob))
				sql.append("b.create_dt desc, ");
		} else {
			sql.append("b.create_dt, ");
		}
		sql.append("a.register_submittal_id, a.register_field_id");
		log.debug(sql + "|" + regId);
		return sql.toString();
	}
	
}
