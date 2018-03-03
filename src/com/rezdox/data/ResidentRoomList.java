package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: ResidentRoomList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Generates a list of rooms for the residence submitted
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
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
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String custom = getCustomSchema();

		StringBuilder sql = new StringBuilder(176);
		sql.append("select rr.room as key_id, rt.type_nm as value from ").append(custom).append("rezdox_room rr ");
		sql.append("inner join ").append(custom).append("rezdox_room_type rt on rt.room_type_cd = rr.room_type_cd ");
		sql.append("where residence_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(req.getParameter("residenceId"));
		
		DBProcessor db = new DBProcessor(getDBConnection(), custom);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		putModuleData(data, data.size(), false);
	
	}

}

