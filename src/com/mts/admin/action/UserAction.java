package com.mts.admin.action;

// JDK 1.8.x
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


// MTS Libs
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;
import com.siliconmtn.db.pool.SMTDBConnection;

//WC Libs
import com.smt.sitebuilder.action.user.UserBaseWidget;

/****************************************************************************
 * <b>Title</b>: UserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action to manage mts users
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class UserAction extends UserBaseWidget {
	
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "users";
	
	/**
	 * 
	 */
	public UserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public UserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public UserAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		BSTableControlVO bst = new BSTableControlVO(req, MTSUserVO.class);
		setModuleData(getAllUsers(bst));
	}
	
	/**
	 * 
	 * @return
	 */
	public GridDataVO<MTSUserVO> getAllUsers(BSTableControlVO bst) {
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("mts_user a ");
		sql.append(DBUtil.INNER_JOIN).append("role b on a.role_id = b.role_id ");
		sql.append("where 1=1 ");
		sql.append("order by last_nm, first_nm ");
		log.info(sql.length() + "|" + sql);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MTSUserVO(), bst);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MTSUserVO> getEditors() {
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("mts_user ");
		sql.append("where role_id in ('100', 'AUTHOR') ");
		sql.append("order by last_nm, first_nm ");
		log.debug(sql.length() + "|" + sql);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new MTSUserVO());
	}
}

