package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// PS Libs
import com.perfectstorm.data.AttributeVO.AttributeType;
import com.perfectstorm.data.VenueTourAttributeVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: VenueTourAttributeWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the attributes attached to a venue tour
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 5, 2019
 * @updates:
 ****************************************************************************/

public class VenueTourAttributeWidget extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "venue_tour_attr";
	
	/**
	 * 
	 */
	public VenueTourAttributeWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public VenueTourAttributeWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param attributes
	 * @param dbConn
	 */
	public VenueTourAttributeWidget(Map<String, Object> attributes, SMTDBConnection dbConn ) {
		super();
		
		this.attributes = attributes;
		this.dbConn = dbConn;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		this.setModuleData(getVenueTourAttributes(req.getParameter("venueTourId")));
	}
	
	/**
	 * 
	 * @param venueTourId
	 * @return
	 */
	public List<VenueTourAttributeVO> getVenueTourAttributes(String venueTourId) {
		StringBuilder sql = new StringBuilder(192);
		sql.append("select a.attribute_cd, a.attribute_nm, vta.venue_tour_attribute_id, vta.create_dt, ");
		sql.append("coalesce(vta.venue_tour_id, ?) as venue_tour_id, coalesce(vta.value_no, a.default_value_no) as value_no");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ps_attribute a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ps_venue_tour_attribute_xr vta ");
		sql.append("on a.attribute_cd = vta.attribute_cd and venue_tour_id = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("a.attribute_type_cd = ? ");
		sql.append(DBUtil.ORDER_BY).append("a.attribute_nm ");
		log.debug("Venue Tour Attributes: " + sql);
		
		// Set the values
		List<Object> vals = new ArrayList<>(); 
		vals.add(venueTourId);
		vals.add(venueTourId);
		vals.add(AttributeType.THRESHOLD);
		
		// Return the data
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), vals, new VenueTourAttributeVO(), "attribute_cd");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Assumes the attributes will be updated one at a time
		VenueTourAttributeVO attr = new VenueTourAttributeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(attr);
			putModuleData(attr);
		} catch (Exception e) {
			putModuleData(attr, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

