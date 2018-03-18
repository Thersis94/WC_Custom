package com.rezdox.data;

import java.util.List;

import com.rezdox.vo.RoomTypeVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: RoomTypeList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> generates a list of room types
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 16, 2018
 * @updates:
 ****************************************************************************/
public class RoomTypeList  extends SimpleActionAdapter {
	public RoomTypeList() {
		super();
	}

	public RoomTypeList(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		if (Convert.formatBoolean(req.getParameter("withCategory"))) {
			List<RoomTypeVO> data = getCompleteRoomTypeList();
			putModuleData(data);
		}else {
			List<GenericVO> data = getGenericRoomTypeList();
			putModuleData(data);
		}
	}
	
	/**
	 * returns the list of room types as a key value pair
	 * @return 
	 */
	private List<GenericVO> getGenericRoomTypeList() {
	String schema = getCustomSchema();
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("select room_type_cd as key, type_nm as value from ");
		sql.append(schema).append("REZDOX_ROOM_TYPE order by type_nm ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), null, new GenericVO());
		
		log.debug("number of room types returned " + data.size());
		
		return data;
	}

	/**
	 * This method generates a list of room type vo's for other classes
	 * @return
	 */
	public List<RoomTypeVO> getCompleteRoomTypeList(){
		String schema = getCustomSchema();
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ");
		sql.append(schema).append("REZDOX_ROOM_TYPE order by room_category_cd, type_nm ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<RoomTypeVO> data = db.executeSelect(sql.toString(), null, new RoomTypeVO());
		
		log.debug("number of room types returned " + data.size());
		
		return data;
	}
}
