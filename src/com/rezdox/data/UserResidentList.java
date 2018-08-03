package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UserResidentList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> this list will generate a list of residences the user 
 *       has to be returned of use as a select picker
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/
public class UserResidentList extends SimpleActionAdapter {
	
	public static final String BUSINESS_ID = "businessId";
	
	public UserResidentList() {
		super();
	}

	public UserResidentList(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(500);
		sql.append("select r.residence_nm as value, r.residence_id as key from ").append(schema).append("rezdox_residence r ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_residence_member_xr rmxr on r.residence_id = rmxr.residence_id and rmxr.status_flg > 0 "); //1=mine, 2=shared w/me
				
		if(!StringUtil.checkVal(req.getParameter(BUSINESS_ID)).isEmpty()) {
			sql.append("inner join schema.rezdox_connection rc on rmxr.member_id = rc.rcpt_member_id or rmxr.member_id = rc.sndr_member_id ");
			sql.append("where (rc.sndr_member_id is not null or rc.rcpt_member_id is not null ) and (rc.sndr_business_id = ? or rc.rcpt_business_id = ? ");
			params.add(req.getParameter(BUSINESS_ID));
			params.add(req.getParameter(BUSINESS_ID));
		}else {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			sql.append("inner join ").append(schema).append("rezdox_member m on rmxr.member_id = m.member_id ");
			sql.append("where m.profile_id = ? ");
			params.add(user.getProfileId());
		}
		
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		putModuleData(data, data.size(), false);
	
	}

}
