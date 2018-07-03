package com.rezdox.data;

//Java 8
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

//WC Custom
import com.rezdox.action.RezDoxUtils;

/*****************************************************************************
 <p><b>Title</b>: UserReportAction.java</p>
 <p><b>Description: </b> Returns an XLS representation of all RezDox users.  Can be filtered 
 for business or residence (only) members, or by registration date.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since July 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class UserReportAction extends SimpleActionAdapter {

	public UserReportAction() {
		super();
	}

	public UserReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void update(ActionRequest req) throws ActionException {
		//retrieve the data
		List<UserReportVO> data = retrieveUserList(req);
		log.debug("data size=" + data.size());

		//decrypt phone#s for residence members
		StringEncrypter se = StringEncrypter.getInstance((String)getAttribute(Constants.ENCRYPT_KEY));
		for (UserReportVO vo : data) {
			//not a business and personal phone = must decrypt
			if (StringUtil.isEmpty(vo.getBusinessName()) && !StringUtil.isEmpty(vo.getPhone())) {
				try {
					log.debug("decrypting " + vo.getPhone());
					vo.setPhone(se.decrypt(vo.getPhone()));
				} catch (EncryptionException e) {
					//don't care - phone# will stay in raw form
				}
			}
		}

		//format the Excel and attach it to the request
		AbstractSBReportVO rpt = new UserReport();
		rpt.setData(data);

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}


	/**
	 * Retrieves the user list report data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private List<UserReportVO> retrieveUserList(ActionRequest req) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Date startDt = req.hasParameter("startDate") ? Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate")) : null;
		Date endDt = req.hasParameter("endDate") ? Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("endDate")) : null;
		String acctType = req.getParameter("acctType");

		String sql = getReportSql(schema, startDt != null, endDt != null, "biz".equals(acctType), "res".equals(acctType));

		List<Object> params = new ArrayList<>();
		if (!"biz".equals(acctType)) {
			params.add(RezDoxUtils.MAIN_SITE_ID);
			params.add(SecurityController.STATUS_ACTIVE);
			params.add(RezDoxUtils.REZDOX_BUSINESS_ROLE);  // != business - enables hybrids
			if (startDt != null) params.add(startDt);
			if (endDt != null) params.add(endDt);
		}
		if (!"res".equals(acctType)) {
			params.add(RezDoxUtils.MAIN_SITE_ID);
			params.add(SecurityController.STATUS_ACTIVE);
			params.add(RezDoxUtils.REZDOX_RESIDENCE_ROLE);  // != residence - enables hybrids
			if (startDt != null) params.add(startDt);
			if (endDt != null) params.add(endDt);
		}

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		return dbp.executeSelect(sql, params, new UserReportVO(), "guid");
	}


	/**
	 * @param schema
	 * @param startDt
	 * @param hasParameter
	 * @param hasParameter2
	 * @return
	 */
	private String getReportSql(String schema, boolean useStartDt, boolean useEndDt, boolean bizOnly, boolean resOnly) {
		StringBuilder sql = new StringBuilder(1000);
		//residence members
		if (!bizOnly) {
			sql.append("select newid() as guid, m.create_dt as enroll_dt, '' as business_nm, m.first_nm, m.last_nm, m.email_address_txt, ");
			sql.append("role.role_nm, r.address_txt, r.address2_txt, r.city_nm, r.state_cd, r.zip_cd, ph.phone_number_txt, '' as website_url, ");
			sql.append("case when rm.status_flg=0 then 'Inactive' when rm.status_flg < 0 then 'Residence Deleted' else 'Active' end as status_nm, ");
			sql.append("'' as sub_category_nm, '' as category_nm ");
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_MEMBER m ");
			sql.append(DBUtil.INNER_JOIN).append("PROFILE_ROLE pr on m.profile_id=pr.profile_id and pr.site_id=? and pr.status_id=? ");
			sql.append(DBUtil.INNER_JOIN).append("ROLE on pr.role_id=role.role_id and role.role_id != ? ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("phone_number ph on pr.profile_id=ph.profile_id and ph.phone_type_cd='HOME' ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on m.member_id=rm.member_id ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE r on rm.residence_id=r.residence_id ");
			sql.append("where 1=1 ");
			if (useStartDt) sql.append("and m.create_dt >= ? ");
			if (useEndDt) sql.append("and m.create_dt < ? ");
		}

		//put a union if running both halves
		if (!bizOnly && !resOnly) sql.append(DBUtil.UNION);

		//business members
		if (!resOnly) {
			sql.append("select newid() as guid, m.create_dt as enroll_dt, b.business_nm, m.first_nm, m.last_nm, m.email_address_txt, ");
			sql.append("role.role_nm, b.address_txt, b.address2_txt, b.city_nm, b.state_cd, b.zip_cd, b.main_phone_txt as phone_number_txt, b.website_url, ");
			sql.append("case when bm.status_flg=0 then 'Pending' when bm.status_flg < 0 then 'Business Deleted' else 'Active' end as status_nm, ");
			sql.append("subcat.category_nm as sub_category_nm, cat.category_nm ");
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_MEMBER m ");
			sql.append(DBUtil.INNER_JOIN).append("PROFILE_ROLE pr on m.profile_id=pr.profile_id and pr.site_id=? and pr.status_id=? ");
			sql.append(DBUtil.INNER_JOIN).append("ROLE on pr.role_id=role.role_id and role.role_id != ? ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR bm on m.member_id=bm.member_id ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_BUSINESS b on bm.business_id=b.business_id ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_BUSINESS_CATEGORY_XR catxr on b.business_id=catxr.business_id ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_BUSINESS_CATEGORY subcat on catxr.business_category_cd=subcat.business_category_cd ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_BUSINESS_CATEGORY cat on subcat.parent_cd=cat.business_category_cd ");
			sql.append("where 1=1 ");
			if (useStartDt) sql.append("and m.create_dt >= ? ");
			if (useEndDt) sql.append("and m.create_dt < ? ");
		}
		log.debug(sql);
		return sql.toString();
	}
}
