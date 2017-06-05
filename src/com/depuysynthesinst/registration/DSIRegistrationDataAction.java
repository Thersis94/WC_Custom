package com.depuysynthesinst.registration;

import com.siliconmtn.action.ActionRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.registration.RegistrationDataAction;
import com.smt.sitebuilder.action.registration.RegistrationDataContainer;
import com.smt.sitebuilder.action.registration.RegistrationDataModuleVO;


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
	protected AbstractSBReportVO buildReport(ActionRequest req) {
		String report = req.getParameter("reportCd");
		if ("complete".equals(report)) {
			return super.buildReport(req);
		} else {
			return new DSIRegistrationDataActionVO();
		}
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
	protected String buildSql(ActionRequest req, boolean useDates, String regSubtlId, String regId) {
		//"actionId" used in this query is actually the actionGroupId, which aligns all historical data
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.value_txt, a.data_enc_flg, b.register_submittal_id, ");
		sql.append("b.profile_id, b.create_dt, c.register_field_id, c.html_type_id, ");
		sql.append("r.role_nm, st.status_nm, s.site_id, s.site_nm, ");
		sql.append("pr.role_expire_dt, max(al.login_dt) as login_dt, ");
		sql.append("coalesce(p.update_dt, p.create_dt) as profile_update_dt ");
		sql.append("from register_data a ");
		sql.append("inner join register_submittal b on a.register_submittal_id=b.register_submittal_id ");
		sql.append("inner join register_field c on a.register_field_id=c.register_field_id ");
		sql.append("left outer join site s on b.site_id=s.site_id ");
		sql.append("left outer join profile p on b.profile_id=p.profile_id ");
		sql.append("left outer join profile_role pr on p.profile_id=pr.profile_id and pr.site_id=b.site_id ");
		sql.append("left outer join role r on pr.role_id=r.role_id ");
		sql.append("left outer join status st on pr.status_id=st.status_id ");
		sql.append("left outer join authentication_log al on  al.authentication_id=p.authentication_id and al.status_cd=1 and al.site_id=b.site_id ");
		sql.append("where b.action_id=? ");
		if (useDates) sql.append("and b.create_dt between ? and ? ");
		if (regSubtlId != null) sql.append("and b.register_submittal_id=? ");
		sql.append("and (b.robot_flg=0 or b.robot_flg is null) ");

		sql.append("group by a.value_txt, a.data_enc_flg, b.register_submittal_id, ");
		sql.append("b.profile_id, b.create_dt, c.register_field_id, c.html_type_id, r.role_nm, st.status_nm, s.site_id, s.site_nm, ");
		sql.append("pr.role_expire_dt, coalesce(p.update_dt, p.create_dt)  ");

		sql.append("order by b.create_dt");
		if ("dateDesc".equals(req.getParameter("orderBy"))) sql.append(" desc");
		sql.append(", b.register_submittal_id, c.register_field_id");
		log.debug(sql + "|" + regId);
		return sql.toString();
	}


	/**
	 * filters the report output to exclude or include survey metrics
	 * @param req
	 * @param cdc
	 */
	@Override
	protected void postFilter(ActionRequest req, RegistrationDataContainer cdc) {
		boolean completedOnly = "1".equals(req.getParameter("completedSurveyOnly"));
		String startDate = req.getParameter("gradStartDate");
		String endDate = req.getParameter("gradEndDate");

		//fail fast if there's no work to do here
		if (!completedOnly && StringUtil.isEmpty(startDate) && StringUtil.isEmpty(endDate))
			return;

		Date startDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, startDate);
		Date endDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, endDate);
		List<RegistrationDataModuleVO> results = new ArrayList<>(cdc.getData().size());

		for (RegistrationDataModuleVO vo : cdc.getData()) {
			if (!saveSurvey(vo, completedOnly, startDt, endDt)) continue;

			//if we made it this far, INCLUDE the record in the report
			results.add(vo);
		}

		cdc.setData(results);
	}


	/**
	 * tests whether this record is one we want to include in the report
	 * @param vo
	 * @param completedOnly
	 * @param startDt
	 * @param endDt
	 * @return
	 */
	private boolean saveSurvey(RegistrationDataModuleVO vo, boolean completedOnly, Date startDt, Date endDt) {
		Map<String, String> extData = vo.getExtData();
		Date gradDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, extData.get("DSI_GRAD_DT"));
		String fellowStartDate = extData.get("DSI_SRVY_FELLOW_START_DT");
		if (StringUtil.isEmpty(fellowStartDate)) fellowStartDate = extData.get("DSI_SRVY_JOB_DT");

		//test for survey completion - survey is not complete if both of the above dates are missing.
		if (completedOnly && StringUtil.isEmpty(fellowStartDate)) return false;

		//test for survey start date
		if (startDt != null && (gradDt == null || gradDt.before(startDt))) return false;

		//test for survey end date
		if (endDt != null && (gradDt == null || gradDt.after(endDt))) return false;

		return true;
	}
}