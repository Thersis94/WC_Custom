package com.ansmed.sb.report;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>:AreaVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 13, 2008
 ****************************************************************************/
public class AreaVO implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public String areaName = null;
	private int total = 0;
	private Map<String, RegionVO> regions = new LinkedHashMap<String, RegionVO>();
	private Map <String, Integer> totals = new LinkedHashMap<String, Integer>();
	
	public AreaVO(ResultSet rs) {
		this.setData(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		String regionName = db.getStringVal("region_nm", rs);
		
		// Add the data to the totals for the area
		this.addElement(db.getStringVal("type_nm", rs), db.getIntVal("total", rs));
		
		RegionVO region = null;
		if (regions.containsKey(regionName)) {
			region = regions.get(regionName);
			region.addType(db.getStringVal("type_nm", rs), db.getIntVal("total", rs));
		} else {
			region = new RegionVO(rs);
			regions.put(region.getRegionName(), region);
		}
	}
	
	/**
	 * Assigns the overall total and individual totals for the area
	 * @param type
	 * @param element
	 */
	public void addElement(String type, int element) {
		total += element;
		Integer val = Convert.formatInteger(totals.get(type));
		
		// Update the totals
		if (totals.containsKey(type)) {
			val += element;
			totals.put(type, val);
		} else {
			totals.put(type, element);
		}
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
	 * @return the total
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @param total the total to set
	 */
	public void setTotal(int total) {
		this.total = total;
	}

	/**
	 * @return the regions
	 */
	public Map<String, RegionVO> getRegions() {
		return regions;
	}

	/**
	 * @param regions the regions to set
	 */
	public void setRegions(Map<String, RegionVO> regions) {
		this.regions = regions;
	}

	/**
	 * @return the totals
	 */
	public Map<String, Integer> getTotals() {
		return totals;
	}

	/**
	 * @param totals the totals to set
	 */
	public void setTotals(Map<String, Integer> totals) {
		this.totals = totals;
	}
}
