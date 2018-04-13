package com.rezdox.data;

import java.util.Arrays;

import com.rezdox.action.RezDoxUtils;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> ProjectMyProviders.java<br/>
 * <b>Description:</b> Returns a list of service providers this homeowner is connected to.  Used by Projects. 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Mar 23, 2018
 ****************************************************************************/
public class ProjectMyProviders extends SimpleActionAdapter {

	public ProjectMyProviders() {
		super();
	}

	public ProjectMyProviders(ActionInitVO arg0) {
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
		StringBuilder sql = new StringBuilder(200);
		sql.append("select b.business_id as key, b.business_nm as value from ");
		sql.append(schema).append("REZDOX_BUSINESS b ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_CONNECTION c on b.business_id=c.sndr_business_id or b.business_id=c.rcpt_business_id ");
		sql.append("where (c.sndr_member_id=? or c.rcpt_member_id=?) and c.approved_flg=1 ");
		sql.append("order by b.business_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		putModuleData(db.executeSelect(sql.toString(), Arrays.asList(memberId, memberId), new GenericVO()));
	}
}