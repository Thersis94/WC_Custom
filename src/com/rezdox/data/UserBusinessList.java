package com.rezdox.data;

import java.util.Arrays;
import java.util.List;

import com.rezdox.action.RezDoxUtils;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: UserBusinessList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> generates a dynamic list of businesses owned or shared with this user.
 * Used on Store modal when buying Connections.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 3.0
 * @since Jun 17, 2018
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
		String schema = getCustomSchema();
		String memberId = RezDoxUtils.getMemberId(req);

		StringBuilder sql = new StringBuilder(325);
		sql.append("select rb.business_id as key, rb.business_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business rb ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_member_xr rc on rc.business_id=rb.business_id ");
		sql.append("where rc.member_id=? and rc.status_flg > 0");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), Arrays.asList(memberId), new GenericVO(), "business_id");

		log.debug(sql +"|"+ memberId+" businesses selected " + data.size());
		putModuleData(data, data.size(), false);
	}
}
