package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.TourDeviceVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TourVenueDeviceWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the devices utilized at a tour venue event
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class TourVenueDeviceWidget extends SBActionAdapter {

	/**
	 * 
	 */
	public TourVenueDeviceWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TourVenueDeviceWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		this.setModuleData(getVenueTourDevices(req.getParameter("venueTourId")));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public List<TourDeviceVO> getVenueTourDevices(String venueTourId) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		vals.add(venueTourId);
		
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(getCustomSchema()).append("ps_weather_device a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ps_tour_device_xr b ");
		sql.append("on a.weather_device_id = b.weather_device_id and venue_tour_id = ? ");
		sql.append(DBUtil.ORDER_BY).append("device_nm ");
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), vals, new TourDeviceVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Assumes the attributes will be updated one at a time
		TourDeviceVO tdvo = new TourDeviceVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(tdvo);
			putModuleData(tdvo);
		} catch (Exception e) {
			putModuleData(tdvo, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

