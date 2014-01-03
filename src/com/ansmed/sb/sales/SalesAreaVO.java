package com.ansmed.sb.sales;

// JDK 1.5.0
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

// SB Libs
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

// SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: SalesAreaVO.java</p>
 <p>Description: <b/> Stores the data for the Sales Area</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 5, 2007
 Last Updated:
 ***************************************************************************/

public class SalesAreaVO extends AbstractSiteBuilderVO {
	public static final long serialVersionUID = 1l;
	private String areaName = null;
	private Map<String, String> regions = null;
	
	/**
	 * 
	 */
	public SalesAreaVO() {
		regions = new HashMap<String, String>();
	}
	
	/**
	 * Initializes the params with the sql row data 
	 * @param rs
	 */
	public SalesAreaVO(ResultSet rs) {
		regions = new HashMap<String, String>();
		setData(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		actionId = db.getStringVal("area_id", rs);
		areaName = db.getStringVal("area_nm", rs);
		
		// Add the region
		String regionId = StringUtil.checkVal(db.getStringVal("region_id", rs));
		String regionName = db.getStringVal("region_nm", rs);
		if (regionId.length() > 0) addRegion(regionId, regionName);
	}
	
	/**
	 * Adds the provided info to the regions map
	 * @param id
	 * @param name
	 */
	public void addRegion(String id, String name) {
		regions.put(id, name);
	}

	/**
	 * @return the areaName
	 */
	public String getAreaName() {
		return areaName;
	}

	/**
	 * @param areaName the areaName to set
	 */
	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	/**
	 * @return the regions
	 */
	public Map<String, String> getRegions() {
		return regions;
	}

	/**
	 * @param regions the regions to set
	 */
	public void setRegions(Map<String, String> regions) {
		this.regions = regions;
	}

}
