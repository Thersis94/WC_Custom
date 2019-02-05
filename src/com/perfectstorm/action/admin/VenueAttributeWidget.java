package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.VenueAttributeVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: VenueAttributeWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the attributes attached to a venue
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class VenueAttributeWidget extends SBActionAdapter {

	/**
	 * 
	 */
	public VenueAttributeWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public VenueAttributeWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		this.setModuleData(getVenueAttributes(req.getParameter("venue_id")));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public List<VenueAttributeVO> getVenueAttributes(String venueId) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		vals.add(venueId);
		
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(getCustomSchema()).append("ps_attribute a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ps_venue_attribute_xr b ");
		sql.append("on a.attribute_cd = b.attribute_cd and venue_id = ? ");
		sql.append(DBUtil.ORDER_BY).append("attribute_nm ");
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), vals, new VenueAttributeVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Assumes the attributes will be updated one at a time
		VenueAttributeVO attr = new VenueAttributeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(attr);
			putModuleData(attr);
		} catch (Exception e) {
			putModuleData(attr, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

