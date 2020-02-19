package com.mts.report;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.UserLoginReport;
import com.smt.sitebuilder.common.constants.Constants;

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
	
	
	/**
	 * gathers the data adding the custom fields needed 
	 * @param req
	 * @return
	 */
	@Override
	public List<GenericVO> detailReport(ActionRequest req) { 
		String site = StringUtil.checkVal(req.getParameter("siteData"));
		if (site.isEmpty()) return Collections.emptyList();

		int index = site.indexOf('|');
		String siteId = site.substring(0,index);
		String siteNm = site.substring(++index);
		boolean excludeGlobalAdmins = Convert.formatBoolean(req.getParameter("excludeGlobalAdmins"));
		Date start = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");
		Date end = Convert.formatEndDate(req.getParameter("endDate"));
		Integer roleOrderNo = req.getIntegerParameter("roleLevel");

		StringBuilder sb = new StringBuilder(750);
		sb.append("select d.profile_Id, d.first_nm, d.last_nm, d.email_address_txt,  b.login_dt, b.status_cd, b.session_id, ");
		sb.append("b.authentication_id, a.site_nm, c.user_nm, f.role_nm, g.phone_number_txt, g.phone_country_cd, ");
		sb.append("u.company_nm ");
		sb.append("from site a inner join authentication_log b on a.site_id = b.site_id ");
		sb.append("inner join authentication c on b.authentication_id = c.authentication_id ");
		sb.append("inner join profile d on b.authentication_id = d.authentication_id ");
		sb.append("inner join ").append(getCustomSchema()).append("mts_user u on d.profile_id = u.profile_id ");
		sb.append("left join (select profile_id, phone_country_cd, array_agg(phone_number_txt) as phone_number_txt from "); 
		sb.append("phone_number group by profile_id, phone_country_cd) as g on d.PROFILE_ID = g.PROFILE_ID ");
		sb.append("left join PROFILE_ROLE e on d.PROFILE_ID = e.PROFILE_ID and e.SITE_ID = a.SITE_ID ");
		sb.append("left join ROLE f on e.role_id = f.role_id ");
		sb.append("where a.site_id=? and login_dt between ? and ? ");
		
		if (excludeGlobalAdmins) {
			sb.append("and (c.user_nm is null or d.global_admin_flg = 0) ");
		} else if (roleOrderNo != null) {
			sb.append("and f.role_order_no=? ");
		}
		sb.append("order by c.user_nm, b.login_dt");
		log.info(sb + "|" + siteId + "|" + start + "|" + end);

		List<GenericVO> data = new ArrayList<>();
		DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm a");
		StringEncrypter se = StringEncrypter.getInstance((String) attributes.get(Constants.ENCRYPT_KEY));
		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setString(1, siteId);
			ps.setDate(2, Convert.formatSQLDate(start));
			ps.setDate(3, Convert.formatSQLDate(end));
			if (roleOrderNo != null)
				ps.setInt(4, roleOrderNo);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new GenericVO("hi", new MtsProfileReportVO(rs, df, se)));

		} catch(Exception e) {
			log.error("Unable to retrieve user login detail report", e);
		}

		//set the site name for the report on the request.
		req.setAttribute("siteName", siteNm);
		log.debug("Size: " + data.size());
		return data;
	}
	
	
	/**
	 * Bean class to send data back to view.
	 * @author smt_user
	 *
	 */
	public class MtsProfileReportVO extends UserLoginReport.ProfileReportVO implements Serializable {
		private static final long serialVersionUID = 6022640754286679825L;
		String companyName;

		public MtsProfileReportVO(ResultSet rs, DateFormat df, StringEncrypter se) {
			super(rs, df, se);
			try {
				companyName = rs.getString("company_nm");
			} catch (Exception e) {
				log.error("could not parse login row", e);
			}
		}

		public String getCompanyName() {
			return companyName;
		}
		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}

	}
	

}
