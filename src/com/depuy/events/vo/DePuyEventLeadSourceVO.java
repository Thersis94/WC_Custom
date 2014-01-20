package com.depuy.events.vo;

import java.util.List;
import java.util.ArrayList;

import com.smt.sitebuilder.action.SBModuleVO;

/*****************************************************************************
 <p><b>Title</b>: EventGroupVO.java</p>
 <p>Data Bean that stores information for a single event category</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Mar 9, 2006
 Code Updates
 James Camire, Mar 9, 2006 - Creating Initial Class File
 ***************************************************************************/

public class DePuyEventLeadSourceVO extends SBModuleVO {
    private static final long serialVersionUID = 1l;
    private String stateCode = null;
    private String stateName = null;
    private List<LeadCityVO> leadCities = new ArrayList<LeadCityVO>();
    private List<String> leadZips = new ArrayList<String>();
    private int leadsCnt = 0;
    private String productName = null;    
    
    public DePuyEventLeadSourceVO() {
    	super();
    }

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}
	
	public List<LeadCityVO> getLeadCities() {
		return leadCities;
	}


	public void setLeadCities(List<LeadCityVO> leadCities) {
		this.leadCities = leadCities;
	}

	public List<String> getLeadZips() {
		return leadZips;
	}
	
	public String getStringZips() {
		int start = 0;
		StringBuffer zips = new StringBuffer();
		for (String zip : leadZips)
			zips.append(", ").append(zip);
		
		if (zips.length() > 2) start = 2; //remove the leading comma
		return zips.substring(start, zips.length());
	}


	public void setLeadZips(List<String> leadZips) {
		this.leadZips = leadZips;
	}

	
	public int getLeadsCnt() {
		return leadsCnt;
	}


	public void setLeadsCnt(int leadsCnt) {
		this.leadsCnt = leadsCnt;
	}


	public String getStateName() {
		return stateName;
	}


	public void setStateName(String stateName) {
		this.stateName = stateName;
	}


	public String getProductName() {
		return productName;
	}


	public void setProductName(String productName) {
		this.productName = productName;
	}
		

}
