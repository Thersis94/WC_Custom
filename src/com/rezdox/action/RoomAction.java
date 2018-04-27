package com.rezdox.action;

import java.util.Arrays;
import java.util.List;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

//WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

//WC Custom
import com.rezdox.data.RoomCategoryList;
import com.rezdox.vo.RoomVO;

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
		String roomId = req.getParameter(ROOM_ID);
		if (StringUtil.isEmpty(req.getParameter(RESIDENCE_ID)) && StringUtil.isEmpty(roomId)) return;

		//if there is just a residence get the list of rooms
		if (!StringUtil.isEmpty(req.getParameter(RESIDENCE_ID)) && StringUtil.isEmpty(roomId)) {
			List<RoomVO> data = generateRoomList(req.getParameter(RESIDENCE_ID));
			log.debug("number of rooms found " + data.size() );
			putModuleData(data);

		} else if (!StringUtil.isEmpty(roomId)) {
			//if its a new room or an edit add the room category list
			RoomCategoryList rtl = new RoomCategoryList();
			rtl.setActionInit(actionInit);
			rtl.setDBConnection(dbConn);
			rtl.setAttributes(attributes);
			ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			modVo.setAttribute(ROOM_TYPE_LIST, rtl.listData(req));
			setAttribute(Constants.MODULE_DATA, modVo);
		}

		//if there is a room id and a residence id get that particular room, also generate the complete
		if (!StringUtil.isEmpty(roomId) && !NEW.equalsIgnoreCase(roomId)) {
			putModuleData(generateRoomDetail(roomId));
		}
	}


	/**
	 * generates the room data for the room for which data is requested.  
	 * @return
	 * @throws InvalidDataException 
	 */
	private RoomVO generateRoomDetail(String roomId) {
		RoomVO vo = new RoomVO();
		vo.setRoomId(StringUtil.checkVal(roomId));

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
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
	private List<RoomVO> generateRoomList(String resId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select rr.*, c.category_nm from ").append(schema).append("rezdox_room rr ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_ROOM_CATEGORY c on rr.room_category_cd=c.room_category_cd ");
		sql.append("where rr.residence_id=? order by rr.room_nm");
		log.debug(sql +"|"+ resId);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(StringUtil.checkVal(resId)), new RoomVO());
	}


	/*
	 * Load a Treasure Box from the DB - including all it's sub-components if a pkId was passed
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		RoomVO vo = RoomVO.instanceOf(req);
		log.debug("RoomVO: " + vo);

		//clean out the 'new' string so the processor knows it is a new vo
		if ("new".equalsIgnoreCase(vo.getRoomId()))
			vo.setRoomId(null);

		//ensure any rooms coming from the UI have a residenceId - prevent a hacker from contributing to the global scope
		if (StringUtil.isEmpty(vo.getResidenceId()))
			throw new ActionException("cannot save room without residenceId");

		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(vo);
			} else {
				dbp.save(vo);
			}
		} catch(Exception e) {
			log.error("Couldn't save RezDox room", e);
		}

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(150);
		url.append(page.getRequestURI());

		if (!StringUtil.isEmpty(req.getParameter(RESIDENCE_ID)))
			url.append("?residenceId=").append(req.getParameter(RESIDENCE_ID));

		sendRedirect(url.toString(), (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE), req);
	}
}