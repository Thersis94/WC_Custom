package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: ResidentRoomList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Generates a list of rooms for the residence submitted
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * @author ryan
 * @version 3.0
 * @since Mar 2, 2018
 * @updates:
 ****************************************************************************/
public class ResidentRoomList extends SimpleActionAdapter {

	public ResidentRoomList() {
		super();
	}

	public ResidentRoomList(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Returns a list of rooms in the system - specific to a residence if residenceId is passed
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String custom = getCustomSchema();
		List<Object> params = new ArrayList<>();
		String residenceId = req.getParameter("residenceId");
		StringBuilder sql = new StringBuilder(200);
		sql.append("select rr.room_type_cd as key, rt.type_nm as value from ").append(custom).append("rezdox_room rr ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("rezdox_room_type rt on rr.room_type_cd=rt.room_type_cd ");

		if (!StringUtil.isEmpty(residenceId) && !"all".equalsIgnoreCase(residenceId)) {
			sql.append("where rr.residence_id=? ");
			params.add(req.getParameter("residenceId"));
		}
		sql.append("order by rt.room_category_cd, rt.room_type_cd ");

		DBProcessor db = new DBProcessor(getDBConnection(), custom);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug(sql +" | params: "+ params + " | rooms: " + data.size());

		putModuleData(data, data.size(), false);
	}
}