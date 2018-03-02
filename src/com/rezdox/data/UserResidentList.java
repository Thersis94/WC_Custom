package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;
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
		String custom = getCustomSchema();
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);

		StringBuilder sql = new StringBuilder(304);
		sql.append("select r.residence_nm as value, r.residence_id as key from ").append(custom).append("rezdox_residence r ");
		sql.append("inner join ").append(custom).append("rezdox_residence_member_xr rmxr on r.residence_id = rmxr.residence_id and rmxr.status_flg = 1 ");
		sql.append("inner join ").append(custom).append("rezdox_member m on rmxr.member_id = m.member_id ");
		sql.append("where m.profile_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(user.getProfileId());
		
		DBProcessor db = new DBProcessor(getDBConnection(), custom);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug("^^ " + data.size());

		//TODO clearly a work in progress just a nightly update
		putModuleData(data);
	
	}

}
