package com.ansmed.sb.report;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>:RegionVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 13, 2008
 ****************************************************************************/
public class RegionVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String regionName = null;
	private Map<String, Integer> types = new LinkedHashMap<String, Integer>();
	
	RegionVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		regionName = db.getStringVal("region_nm", rs);
		this.addType(db.getStringVal("type_nm", rs), db.getIntVal("total", rs));
	}
	
	public void addType(String name, Integer count) {
		types.put(name, count);
	}
	
	/**
	 * @return the regionName
	 */
	public String getRegionName() {
		return regionName;
	}
	/**
	 * @param regionName the regionName to set
	 */
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}
	/**
	 * @return the types
	 */
	public Map<String, Integer> getTypes() {
		return types;
	}
	/**
	 * @param types the types to set
	 */
	public void setTypes(Map<String, Integer> types) {
		this.types = types;
	}
}

