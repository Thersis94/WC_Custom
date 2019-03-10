package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// PS Libs
import com.perfectstorm.data.WeatherDeviceVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: WeatherDeviceWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the weather devices at use at an event
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class WeatherDeviceWidget extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "device";
	
	/**
	 * 
	 */
	public WeatherDeviceWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public WeatherDeviceWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		this.setModuleData(getWeatherDevices());
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public List<WeatherDeviceVO> getWeatherDevices() {
		// Add the params
		List<Object> vals = new ArrayList<>(); 

		// Build the sql
		StringBuilder sql = new StringBuilder(80);
		sql.append("select * from ").append(getCustomSchema()).append("ps_weather_device ");
		sql.append(DBUtil.ORDER_BY).append("device_nm ");
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), vals, new WeatherDeviceVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Assumes the attributes will be updated one at a time
		WeatherDeviceVO wd = new WeatherDeviceVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(wd);
			putModuleData(wd);
		} catch (Exception e) {
			log.error("unabele to save weather device: " + wd, e);
			putModuleData(wd, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

