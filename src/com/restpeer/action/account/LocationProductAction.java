package com.restpeer.action.account;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// RP Libs
import com.restpeer.data.LocationProductVO;

// SMT BaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: LocationProductAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the item master and inventory levels for a given
 * member location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 22, 2019
 * @updates:
 ****************************************************************************/

public class LocationProductAction extends SBActionAdapter {
	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "locationProduct";
	
	/**
	 * 
	 */
	public LocationProductAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public LocationProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param actionInit
	 */
	public LocationProductAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getLocProducts(req.getParameter("memberLocationId")));
	}
	
	/**
	 * Retrieves a list of the items and quantities supported by a member location 
	 * @param mlid
	 * @return
	 */
	public List<LocationProductVO> getLocProducts(String mlid) {
		List<Object> vals = new ArrayList<>();
		vals.add(mlid);
		StringBuilder sql = new StringBuilder(162);
		sql.append("select a.*, b.*, c.parent_nm ");
		sql.append("from ").append(getCustomSchema()).append("rp_product a ");
		sql.append("inner join rp_location_product_xr b ");
		sql.append("on a.product_cd = b.product_cd ");
		sql.append("inner join ( ");
		sql.append("select product_cd, initcap(category_cd) || ', ' || product_nm as parent_nm ");
		sql.append("from ").append(getCustomSchema()).append("rp_product ");
		sql.append(") as c on a.parent_cd = c.product_cd ");
		sql.append("where b.member_location_id = ? ");
		sql.append("order by a.product_nm ");
		log.debug(sql.length() + "|" + sql + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new LocationProductVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		LocationProductVO pvo = new LocationProductVO(req);
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			
			if (req.getBooleanParameter("isDelete")) db.delete(pvo);
			else db.save(pvo);
			
			setModuleData(pvo);
		} catch (Exception e) {
			log.error("Unable to save / delete location product info: " + pvo);
			setModuleData(pvo, 1, e.getLocalizedMessage());
		}
		
	}
}

