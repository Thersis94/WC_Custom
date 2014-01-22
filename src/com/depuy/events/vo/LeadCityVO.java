package com.depuy.events.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.util.Convert;
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
    
    private Map<Integer, Integer> leads = null;
    private boolean checkedTierOne = false;
    private boolean checkedTierTwo = false;
    private boolean checkedTierThree = false;
    private boolean checkedTierFour = false;
    
    
    public LeadCityVO() {
    	super();
    	setLeads(new HashMap<Integer, Integer>());
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

	public Map<Integer, Integer> getLeads() {
		return leads;
	}

	public void setLeads(Map<Integer, Integer> leads) {
		this.leads = leads;
	}

	public void addLead(Integer range, Integer leadCnt, boolean checked) {
		Integer cnt = this.leads.get(range);
		if (cnt == null) cnt = 0;
		cnt += leadCnt;
		this.leads.put(range, cnt);
		if (checked) this.setChecked(range, checked); //they're already false, only change to true
	}
	
	/**
	 * returns all leads <3mos.
	 * NOTE in these 4 methods; the lead only fits in ONE of them when we load the data,
	 * so when returning "all through X date", we must also combine the newer buckets
	 * e.g. "all <12mos" =  all3mos + all6mos + all12mos.
	 * @return
	 */
	public Integer getTierOne() {
		return Convert.formatInteger(leads.get(3));
	}
	
	/**
	 * returns all leads <6mos.  sum all smaller (newer) buckets
	 * @return
	 */
	public Integer getTierTwo() {
		return Convert.formatInteger(leads.get(6)) + getTierOne();
	}
	
	/**
	 * returns all leads <12mos.  sum all smaller (newer) buckets
	 * tier 2 already includes 1+2
	 * @return
	 */
	public Integer getTierThree() {
		return Convert.formatInteger(leads.get(12)) + getTierTwo();
	}
	
	/**
	 * returns all leads, regardless of age.  sum all 4 buckets
	 * tier 3 already includes 1+2
	 * @return
	 */
	public Integer getTierFour() {
		return Convert.formatInteger(leads.get(240)) + getTierThree();
	}

	public boolean getTierOneChecked() {
		return checkedTierOne;
	}

	public boolean getTierTwoChecked() {
		return checkedTierTwo;
	}

	public boolean getTierThreeChecked() {
		return checkedTierThree;
	}

	public boolean getTierFourChecked() {
		return checkedTierFour;
	}

	public void setChecked(Integer range, boolean checked) {
		if (range == null) return;
		switch (range) {
			case 3:
				this.checkedTierOne = checked; break;
			case 6:
				this.checkedTierTwo = checked; break;
			case 12:
				this.checkedTierThree = checked; break;
			case 240:
				this.checkedTierFour = checked; break;
		}
	}
	
}
