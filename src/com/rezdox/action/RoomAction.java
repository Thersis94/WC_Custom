package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.data.RoomTypeList;
import com.rezdox.vo.RoomVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;



/****************************************************************************
 * <b>Title</b>: RoomAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages lists and data changes for rooms
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 15, 2018
 * @updates:
 ****************************************************************************/
public class RoomAction extends SimpleActionAdapter {
	private static final String NEW = "new";
	private static final String RESIDENCE_ID = "residenceId";
	private static final String ROOM_ID = "roomId";
	private static final String ROOM_TYPE_LIST = "roomTypeList";

	public RoomAction() {
		super();
	}

	public RoomAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Load a Treasure Box from the DB - including all it's sub-components if a pkId was passed
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Rooms Retrieve called ");
		if (StringUtil.isEmpty(req.getParameter(RESIDENCE_ID)) && StringUtil.isEmpty(req.getParameter(ROOM_ID))) return;
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		
		//if there is just a residence get the list of rooms
		if (!StringUtil.isEmpty(req.getParameter(RESIDENCE_ID)) && StringUtil.isEmpty(req.getParameter(ROOM_ID))) {
			List<RoomVO> data = generateRoomList(req.getParameter(RESIDENCE_ID), schema, params);
			log.debug("number of rooms found " + data.size() );
			putModuleData(data);
		}
		
		//if its a new room or an edit add the room type list
		if (!StringUtil.isEmpty(req.getParameter(ROOM_ID))) {
			RoomTypeList rtl = new RoomTypeList();
			rtl.setActionInit(actionInit);
			rtl.setDBConnection(dbConn);
			rtl.setAttributes(attributes);
			
			ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			modVo.setAttribute(ROOM_TYPE_LIST, rtl.getCompleteRoomTypeList());
			setAttribute(Constants.MODULE_DATA, modVo);
		}

		//if there is a room id and a residence id get that particular room, also generate the complete 
		if (!StringUtil.isEmpty(req.getParameter(ROOM_ID)) && !NEW.equalsIgnoreCase(req.getParameter(ROOM_ID)) ) {
			putModuleData(generateRoomDetail(req.getParameter(ROOM_ID), schema));
		}
				
	}
	
	/**
	 * generates the room data for the room for which data is requested.  
	 * @return
	 * @throws InvalidDataException 
	 */
	private RoomVO generateRoomDetail(String roomId, String schema) {
		RoomVO vo = new RoomVO();
		vo.setRoomId(StringUtil.checkVal(roomId));
		
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		try {
			db.getByPrimaryKey(vo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("could not retrive data on room ",e);
		}
		return vo;
	}

	/**
	 * generates a list of all the room vos linked to the residence
	 * @param params 
	 * @param schema 
	 * @param string 
	 * @return 
	 * 
	 */
	private List<RoomVO> generateRoomList(String resId, String schema, List<Object> params) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(schema).append("rezdox_room rr ");
		sql.append("inner join ").append(schema).append("rezdox_room_type rt on rr.room_type_cd = rt.room_type_cd ");
		sql.append("where residence_id = ? order by rr.room_nm");
		
		params.add(StringUtil.checkVal(resId));
		log.debug("sql " + sql.toString() +"|"+ params);
		
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new RoomVO());
		
	}

	/*
	 * Load a Treasure Box from the DB - including all it's sub-components if a pkId was passed
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		RoomVO vo = new RoomVO(req);
		
		log.debug("room " + vo);
		//clean out the 'new' string so the processor knows it is a new vo
		if ("new".equalsIgnoreCase(vo.getRoomId())) {
			vo.setRoomId(null);
		}
		DBProcessor dbp = new DBProcessor(dbConn);
		
		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(vo);
			} else {
				dbp.save(vo);
			}
		} catch(Exception e) {
			log.error("Couldn't save RezDox room. ", e);
		}
		
		StringBuilder url = new StringBuilder(150);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		
		url.append(page.getRequestURI());
		
		if (!StringUtil.isEmpty(req.getParameter(RESIDENCE_ID)))
				url.append("?residenceId=").append(req.getParameter(RESIDENCE_ID));
				
		if (!StringUtil.isEmpty(req.getParameter(ROOM_ID)) && !Convert.formatBoolean(req.hasParameter("isDelete"))) {
			url.append("&roomId=").append(vo.getRoomId());
		}
			

		log.debug("REDIR=" + url);

		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
}
	
