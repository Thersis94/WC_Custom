package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UserBusinessList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> generates a dynamic list of connected businesses
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 5, 2018
 * @updates:
 ****************************************************************************/
public class UserBusinessList extends SimpleActionAdapter {
	
	public UserBusinessList() {
		super();
	}

	public UserBusinessList(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String custom = getCustomSchema();
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		List<Object> params = new ArrayList<>();
				
		StringBuilder sql = new StringBuilder(325);
		sql.append("select rb.business_nm as value, rb.business_id as key from custom.rezdox_business rb ");
		sql.append("inner join custom.rezdox_connection rc on rc.sndr_business_id = rb.business_id or rc.rcpt_business_id = rb.business_id ");
		sql.append("where (rcpt_business_id is not null or sndr_business_id is not null) ");
		sql.append("and (sndr_member_id = ? or rcpt_member_id = ? )");
		params.add(member.getMemberId());
		params.add(member.getMemberId());
				
		DBProcessor db = new DBProcessor(getDBConnection(), custom);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug("sql " + sql +"|"+ params+" businesses selected " + data.size());
		putModuleData(data, data.size(), false);
	
	}

}

