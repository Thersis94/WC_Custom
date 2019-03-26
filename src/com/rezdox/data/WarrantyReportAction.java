package com.rezdox.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <p><b>Title:</b> WarrantyReportAction.java</p>
 * <p><b>Description:</b> Generates the Warranty Report for admins (WC->Reports) over the given date range.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Mar 18, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class WarrantyReportAction extends SimpleActionAdapter {

	public WarrantyReportAction() {
		super();
	}

	public WarrantyReportAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void update(ActionRequest req) throws ActionException {
		//retrieve the data
		Date startDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate"));
		Date endDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("endDate"));
		List<WarrantyReportVO> data = retrieveData(startDt, endDt);
		log.debug("data size=" + data.size());

		//decrypt phone#s for residence members
		StringEncrypter se = StringEncrypter.getInstance((String)getAttribute(Constants.ENCRYPT_KEY));
		for (WarrantyReportVO vo : data) {
			try {
				vo.setAddress(se.decrypt(vo.getAddress()));
				vo.setPhone(se.decrypt(vo.getPhone()));
			} catch (EncryptionException e) {
				//ignorable - data will stay in raw form
			}
		}

		//format the Excel and attach it to the request
		WarrantyReport rpt = new WarrantyReport();
		rpt.setData(data);
		rpt.setStartDate(startDt);
		rpt.setEndDate(endDt);

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}


	/**
	 * Retrieves the user list report data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private List<WarrantyReportVO> retrieveData(Date startDt, Date endDt) {
		String schema = getCustomSchema();
		String sql = getReportSql(schema, startDt != null, endDt != null);

		List<Object> params = new ArrayList<>();
		if (startDt != null) params.add(startDt);
		if (endDt != null) params.add(endDt);
		//for the unioned query
		if (startDt != null) params.add(startDt);
		if (endDt != null) params.add(endDt);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		return dbp.executeSelect(sql, params, new WarrantyReportVO(), "guid");
	}


	/**
	 * @param schema
	 * @param startDt
	 * @param hasParameter
	 * @param hasParameter2
	 * @return
	 */
	private String getReportSql(String schema, boolean useStartDt, boolean useEndDt) {
		StringBuilder sql = new StringBuilder(2000);
		//Projects / project materials (includes business projects and home-history)
		sql.append("select pm.project_material_id as guid, pm.create_dt as purchase_dt, coalesce(m.first_nm, pav.project_owner) as first_nm, ");
		sql.append("m.last_nm, coalesce(m.email_address_txt, pav.project_email) as email_address_txt, ");
		sql.append("coalesce(r.address_txt, pav.project_address) as address_txt, r.address2_txt, coalesce(r.city_nm, pav.project_city) as city_nm, ");
		sql.append("coalesce(r.state_cd, pav.project_state) as state_cd, coalesce(r.zip_cd, pav.project_zip_code) as zip_cd, ");
		sql.append("coalesce(ph.phone_number_txt, pav.project_phone) as phone_number_txt, ");
		sql.append("pmv.project_material_pbrand as brand, pmv.project_material_pmodel as model, pmv.project_material_pretailer as retailer, ");
		sql.append("pmv.project_material_pserialno as serial, pmv.project_product_pmanufacturer as manufacturer, ");
		sql.append("case when p.residence_view_flg=1 then 'Home History' else 'Project Log' end as source ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_PROJECT_MATERIAL pm on p.project_id=pm.project_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_project_material_warranty_view pmv on pm.project_material_id=pmv.project_material_id and pmv.submit_warranty='1' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_project_attribute_warranty_view pav on p.project_id=pav.project_id "); //non-connected homeowner data
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE r on p.residence_id=r.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 "); //connected homeowner data
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("phone_number ph on m.profile_id=ph.profile_id and ph.phone_type_cd='HOME' ");
		sql.append("where 1=1 ");
		if (useStartDt) sql.append("and pm.create_dt >= ? ");
		if (useEndDt) sql.append("and pm.create_dt <= ? ");

		sql.append(DBUtil.UNION);

		//Home inventory
		sql.append("select ti.treasure_item_id as guid, ti.create_dt as purchase_dt, ");
		sql.append("m.first_nm, m.last_nm, m.email_address_txt, ");
		sql.append("coalesce(r.address_txt, pa.address_txt) as address_txt, coalesce(r.address2_txt, pa.address2_txt) as address2_txt, ");
		sql.append("coalesce(r.city_nm, pa.city_nm) as city_nm, ");
		sql.append("coalesce(r.state_cd, pa.state_cd) as state_cd, coalesce(r.zip_cd, pa.zip_cd) as zip_cd, ");
		sql.append("ph.phone_number_txt, tav.brand, tav.model, tav.retailer, tav.serialno as serial, '' as manufacturer, 'Home Inventory' as source ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_TREASURE_ITEM ti ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_treasure_item_warranty_view tav on ti.treasure_item_id=tav.treasure_item_id and tav.submit_warranty='1' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE r on ti.residence_id=r.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 "); // homeowner
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id or ti.owner_member_id=m.member_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("phone_number ph on m.profile_id=ph.profile_id and ph.phone_type_cd='HOME' ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile_address pa on m.profile_id=pa.profile_id ");
		sql.append("where 1=1 ");
		if (useStartDt) sql.append("and ti.create_dt >= ? ");
		if (useEndDt) sql.append("and ti.create_dt <= ? ");

		return sql.toString();
	}
}