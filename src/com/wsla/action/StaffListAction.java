package com.wsla.action;

import java.util.Arrays;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.wsla.data.provider.ProviderType;

/****************************************************************************
 * <p><b>Title:</b> CustomerListAction.java</p>
 * <p><b>Description:</b> Called from DynamicListLoader to populate a dropdown for Tasks tool.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 21, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class StaffListAction extends SimpleActionAdapter {

	public StaffListAction() {
		super();
	}

	public StaffListAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(listStaff());
	}


	/**
	 * Reusable retrieve to get the list outside of a Request
	 * @return
	 */
	private List<GenericVO> listStaff() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(300);
		sql.append("select u.profile_id as key, u.last_nm + ', ' + u.first_nm + ' (' + u.email_address_txt + ')' as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_user u");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_user_xr xr on u.user_id=xr.user_id and xr.active_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location loc on xr.location_id=loc.location_id");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider prov on loc.provider_id=prov.provider_id and prov.provider_type_id=?");
		sql.append("where length(u.email_address_txt) > 0 and u.active_flg=1 order by 2");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		return db.executeSelect(sql.toString(), Arrays.asList(ProviderType.WSLA.name()), new GenericVO());
	}
}
