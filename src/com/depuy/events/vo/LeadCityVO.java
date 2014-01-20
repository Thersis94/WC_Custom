package com.depuy.events.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: LeadCityVO.java</p>
 <p>Data Bean that stores information for a targeted lead area (geographic locale)</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Mar 9, 2006
 Code Updates
 James Camire, Mar 9, 2006 - Creating Initial Class File
 James McKain, Jan 20, 2014 - dropped unneeded superclass, added maxAgeNo
 ***************************************************************************/

public class LeadCityVO implements Serializable {
    private static final long serialVersionUID = 1l;
    private String stateCd = null;
    private String cityNm = null;
    private String countyNm = null;
    private String zipCd = null;
    private int leadsCnt = 0;
    private String selected = null;
    private Date lastMailingDt = null;
    private int maxAgeNo = 0;
    
    public LeadCityVO() {
    	super();
    }
    
	public String getCityNm() {
		return cityNm;
	}

	public String getCountyNm() {
		if (StringUtil.checkVal(countyNm).length() == 0) countyNm="Unknown";
		
		countyNm = StringUtil.replace(countyNm,"'","&#39;");
		return StringUtil.capitalize(countyNm);
	}

	public int getLeadsCnt() {
		return leadsCnt;
	}

	public String getSelected() {
		return selected;
	}

	public String getStateCd() {
		return stateCd;
	}

	public void setCityNm(String cityNm) {
		this.cityNm = cityNm;
	}

	public void setCountyNm(String countyNm) {
		this.countyNm = countyNm;
	}

	public void setLeadsCnt(int leadsCnt) {
		this.leadsCnt = leadsCnt;
	}

	public void setSelected(String selected) {
		this.selected = selected;
	}

	public void setStateCd(String stateCd) {
		this.stateCd = stateCd;
	}

	public String getZipCd() {
		return zipCd;
	}

	public void setZipCd(String zipCd) {
		this.zipCd = zipCd;
	}

	public Date getLastMailingDt() {
		return lastMailingDt;
	}

	public void setLastMailingDt(Date lastMailingDt) {
		this.lastMailingDt = lastMailingDt;
	}

	public int getMaxAgeNo() {
		return maxAgeNo;
	}

	public void setMaxAgeNo(int maxAgeNo) {
		this.maxAgeNo = maxAgeNo;
	}
}
