package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.MemberVO;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ResidenceAction.java<p/>
 * <b>Description: Manages member interactions with a residence.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 7, 2018
 ****************************************************************************/
public class ResidenceAction extends SimpleActionAdapter {
	
	public ResidenceAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ResidenceAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// TODO: If they are trying to add a residence (residenceId=new), and they are at their limit,
		// they need to forward to subscription page to purchase more before they can continue.
		
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String residenceId = req.getParameter("residenceId");
		
		// Show only residences that the member has access to
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		
		// Using pivot table on the attributes to get additional data for display
		StringBuilder sql = new StringBuilder(900);
		sql.append("select r.residence_id, residence_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt) as update_dt, ");
		sql.append("beds_no, baths_no, coalesce(f_sqft_no, 0) + coalesce(uf_sqft_no, 0) as sqft_no, purchase_price_no ");
		sql.append("from ").append(schema).append("rezdox_residence r inner join ");
		sql.append(schema).append("rezdox_residence_member_xr m on r.residence_id = m.residence_id ");
		sql.append("left join (SELECT * FROM crosstab('SELECT residence_id, form_field_id, value_txt FROM ").append(schema).append("rezdox_residence_attribute ORDER BY 1', ");
		sql.append("'SELECT DISTINCT form_field_id FROM ").append(schema).append("rezdox_residence_attribute WHERE form_field_id in (''RESIDENCE_BEDS'',''RESIDENCE_BATHS'',''RESIDENCE_F_SQFT'',''RESIDENCE_UF_SQFT'', ''RESIDENCE_PURCHASE_PRICE'') ORDER BY 1') ");
		sql.append("AS (residence_id text, baths_no float, beds_no int, f_sqft_no int, purchase_price_no float, uf_sqft_no int) ");
		sql.append(") ra on r.residence_id = ra.residence_id ");
		sql.append("where member_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(member.getMemberId());
		
		// Return only a specific residence if selected
		if (!StringUtil.isEmpty(residenceId)) {
			sql.append("and r.residence_id = ? ");
			params.add(residenceId);
		}
		
		DBProcessor dbp = new DBProcessor(dbConn);
		List<ResidenceVO> residences = dbp.executeSelect(sql.toString(), params, new ResidenceVO());
		
		putModuleData(residences, residences.size(), false);
	}
	
}