package com.rezdox.data;

import java.util.Arrays;
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
		List<GenericVO> data = listRooms(req);
		putModuleData(data, data.size(), false);
	}

	/**
	 * @param req
	 * @return
	 */
	public List<GenericVO> listRooms(ActionRequest req) {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select room_id as key, room_nm as value from ").append(custom).append("rezdox_room ");
		sql.append("where residence_id=? or residence_id is null ");
		sql.append("order by room_nm");
		log.debug(sql);

		List<Object> params = Arrays.asList(req.getParameter("residenceId"));
		DBProcessor db = new DBProcessor(getDBConnection(), custom);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug(sql +" | params: "+ params + " | rooms: " + data.size());
		return data;
	}
}