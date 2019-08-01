package com.biomed.smarttrak.admin.report;

import java.util.List;
import java.util.stream.Collectors;

import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title:</b> RedGreenUsageReportAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Load Data for the Red Yellow Green Report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 29, 2019
 ****************************************************************************/
public class RedYellowGreenReportAction extends UserListReportAction {

	public RedYellowGreenReportAction() {
		super();
	}

	public RedYellowGreenReportAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * Retrieve a List of users and then convert them to RedGreenVOs for
	 * reporting.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public List<RedYellowGreenVO> retrieveMergedUsers(ActionRequest req) throws ActionException {
		return super.retrieveUserList(req).stream().map(RedYellowGreenVO::new).collect(Collectors.toList());
	}

	/**
	 * Builds the base accounts/users query.
	 * @return
	 */
	@Override
	protected StringBuilder buildAccountsUsersQuery(String schema) {
		StringBuilder sql = new StringBuilder(650);
		sql.append("select ac.account_id, ac.account_nm, ac.expiration_dt as acct_expiration_dt, ac.status_no, ");
		sql.append("ac.start_dt as acct_start_dt, ac.classification_id, ac.type_id, us.acct_owner_flg, ");
		sql.append("us.status_cd, us.expiration_dt, us.fd_auth_flg, us.create_dt, us.active_flg as active_flg, ");
		sql.append("pf.profile_id, pf.authentication_id, pf.first_nm, pf.last_nm, pf.email_address_txt, ");
		sql.append("pfa.address_txt, pfa.address2_txt, pfa.city_nm, pfa.state_cd, pfa.zip_cd, pfa.country_cd, ");
		sql.append("ph.phone_number_txt, ph.phone_type_cd, us.create_dt as user_create_dt, ");
		sql.append("rd.register_field_id, rd.value_txt, us.user_id, rfo.option_desc ");
		sql.append("from ").append(schema).append("biomedgps_account ac ");
		sql.append("inner join ").append(schema).append("biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("inner join profile pf on us.profile_id = pf.profile_id ");
		sql.append("left join profile_address pfa on pf.profile_id = pfa.profile_id ");
		sql.append("left join phone_number ph on pf.profile_id = ph.profile_id ");
		sql.append("inner join register_submittal rs on pf.profile_id = rs.profile_id ");
		sql.append("inner join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("left join register_field_option rfo on rd.register_field_id = rfo.register_field_id ");  
		sql.append("and rd.value_txt = rfo.option_value_txt ");
		sql.append(DBUtil.WHERE_CLAUSE).append("ac.status_no = ? and ac.type_id  in (?,?) and us.status_cd in (?,?) ");
		sql.append("order by ac.account_nm, us.user_id, us.profile_id, phone_type_cd ");
		log.debug("user retrieval SQL: " + sql.toString());
		return sql;
	}

	/**
	 * Build Params list for the RetrieveAccountUsers Query.
	 * @return
	 */
	@Override
	protected String[] getRetrieveAccountUsersParams() {
		return new String[] {AccountVO.Status.ACTIVE.getStatusNo(), AccountVO.Type.FULL.getId(), AccountVO.Type.TRIAL.getId(), UserVO.LicenseType.ACTIVE.getCode(), UserVO.LicenseType.EXTRA.getCode()};
	}
}