package com.sjm.corp.mobile.collection;


import java.util.Map;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: RegionVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Handles data for properties that are on a per-region basis for the sjm mobile data collection portlet/app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Aug 2, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class RegionVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private String regionId;
	private int maxSelected;
	private String name;
	private Map<String, String> nameMap;
	
	public RegionVO(){
		maxSelected = 1;
	}
	
	public int getMaxSelected() {
		return maxSelected;
	}
	public void setMaxSelected(int maxSelected) {
		this.maxSelected = maxSelected;
	}

	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getNameMap() {
		return nameMap;
	}

	public void setNameMap(Map<String, String> nameMap) {
		this.nameMap = nameMap;
	}
}
	