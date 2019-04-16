package com.wsla.action.admin;

import java.util.ArrayList;
// JDK 1.8.x
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.BillableActivityVO;


/****************************************************************************
 * <b>Title</b>: BillableActivityAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the list of Billable Activities 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 7, 2018
 * @updates:
 ****************************************************************************/

public class BillableActivityAction extends SBActionAdapter {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "billable";
	//used as the parent id for all of the misc. activities
	public static final String MISC_ACT_CODE = "MISC_ACTIVITY";
	
	/**
	 * 
	 */
	public BillableActivityAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BillableActivityAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public BillableActivityAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		BSTableControlVO bst = new BSTableControlVO(req, BillableActivityVO.class);
		setModuleData(getCodes(null, req.getBooleanParameter("isMiscActivites"), bst));
	}
	
	/**
	 * Gets the complete or filtered list of codes
	 * @return
	 */
	public GridDataVO<BillableActivityVO> getCodes(String btc, boolean isMiscActivites, BSTableControlVO bst) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(80);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_billable_activity ");
		sql.append("where 1=1 ");
		if (! StringUtil.isEmpty(btc)) {
			sql.append("and billable_type_cd = ? ");
			vals.add(btc);
		}
		if(isMiscActivites) {
			sql.append("and parent_id = ? ");
			vals.add(MISC_ACT_CODE);
		}else {
			sql.append("and (parent_id != ? or parent_id is null) ");
			vals.add(MISC_ACT_CODE);
		}
		
		if(!StringUtil.isEmpty(bst.getSearch())) {
			sql.append("and lower(activity_nm) like ? ");
			vals.add(bst.getLikeSearch());
		}
		sql.append(bst.getSQLOrderBy("activity_nm", "asc"));
		log.debug(sql.length() + "|" + sql);
		DBProcessor db = new DBProcessor(getDBConnection());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		
		GridDataVO<BillableActivityVO> data = db.executeSQLWithCount(sql.toString(), vals, new BillableActivityVO(), bst);
		
		log.debug("@ " + data);
				
		return data;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		BillableActivityVO bavo = new BillableActivityVO(req);
		
		try {
			saveBillableActivity(bavo, req.getBooleanParameter("insert"));
			this.setModuleData(bavo);
		} catch (Exception e) {
			log.error("unable to save activity", e);
			this.setModuleData("", 0, e.getLocalizedMessage());
		}
		
	}
	
	/**
	 * Saves the Billable Activity
	 * @param bavo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveBillableActivity(BillableActivityVO bavo, boolean insert) 
	throws InvalidDataException, DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		if (insert) {
			db.insert(bavo);
		} else {
			db.update(bavo);
		}
		
	}
}

