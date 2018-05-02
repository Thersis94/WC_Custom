package com.rezdox.data;

import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: RoomCategoryList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> generates a list of room categories
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 16, 2018
 * @updates:
 ****************************************************************************/
public class RoomCategoryList  extends SimpleActionAdapter {

	public RoomCategoryList() {
		super();
	}

	public RoomCategoryList(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<GenericVO> data = listData(req);
		putModuleData(data);
	}


	/**
	 * @param req
	 * @return
	 */
	public List<GenericVO> listData(ActionRequest req) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select room_category_cd as key, category_nm as value from ");
		sql.append(schema).append("REZDOX_ROOM_CATEGORY order by category_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), null, new GenericVO());

		log.debug("number of room types returned " + data.size());

		return data;
	}
}