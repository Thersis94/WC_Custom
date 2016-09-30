package com.depuy.events.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.depuy.events_v2.LeadsDataToolV2;
import com.siliconmtn.security.UserDataVO;
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
    
    private Map<Integer, LeadCount> leads = null;
    private boolean checkedTierOne = false;
    private boolean checkedTierTwo = false;
    private boolean checkedTierThree = false;
    private boolean checkedTierFour = false;
    
    /* 
     * classification of lead type; keeps our actions from using ints or booleans
     */
    public enum LeadType {
	    Print, Email, Both;
    }
    
    public LeadCityVO() {
    	super();
    	setLeads(new HashMap<Integer, LeadCount>());
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

	public Map<Integer, LeadCount> getLeads() {
		return leads;
	}

	public void setLeads(Map<Integer, LeadCount> leads) {
		this.leads = leads;
	}

	public void addLead(UserDataVO user, Integer leadCnt, boolean checked) {
		Integer range = user.getBirthYear();
		LeadCount counts = this.leads.get(range);
		if (counts == null) counts = new LeadCount();
		
		//count leads that are eligible for both separtely. - used for display purposes only
		if (1 == user.getGlobalAdminFlag() && 1 == user.getValidEmailFlag()) {
			counts.incrBothCnt(leadCnt);
		}
		//only count the lead as one or the other; print or email
		if (1 == user.getGlobalAdminFlag()) {
			counts.incrPrintCnt(leadCnt);
		} else if (1 == user.getValidEmailFlag()) {
			counts.incrEmailCnt(leadCnt);
		}
		this.leads.put(range, counts);
		if (checked) this.setChecked(range, checked); //they're already false, only change to true
	}
	
	/**
	 * returns all leads <3mos.
	 * NOTE in these 4 methods; the lead only fits in ONE of them when we load the data,
	 * so when returning "all through X date", we must also combine the newer buckets
	 * e.g. "all <12mos" =  all3mos + all6mos + all12mos.
	 * @return
	 */
	public Integer getTierOne(LeadType type) {
		LeadCount cnt = leads.get(LeadsDataToolV2.LeadTierOne);
		if (cnt == null) cnt = new LeadCount();
		
		if (LeadType.Print == type) {
			return Convert.formatInteger(cnt.getPrintCnt());
		} else if (LeadType.Email == type) {
			return Convert.formatInteger(cnt.getEmailCnt());
		} else if (LeadType.Both == type) {
			return Convert.formatInteger(cnt.getBothCnt());
		} else {
			return Convert.formatInteger(cnt.getTotal());
		}
	}
	
	/**
	 * returns all leads <6mos.  sum all smaller (newer) buckets
	 * @return
	 */
	public Integer getTierTwo(LeadType type) {
		LeadCount cnt = leads.get(LeadsDataToolV2.LeadTierTwo);
		if (cnt == null) cnt = new LeadCount();
		
		if (LeadType.Print == type) {
			return Convert.formatInteger(cnt.getPrintCnt()) + getTierOne(type);
		} else if (LeadType.Email == type) {
			return Convert.formatInteger(cnt.getEmailCnt()) + getTierOne(type);
		} else if (LeadType.Both == type) {
			return Convert.formatInteger(cnt.getBothCnt()) + getTierOne(type);
		} else {
			return Convert.formatInteger(cnt.getTotal()) + getTierOne(type);
		}
	}
	
	/**
	 * returns all leads <12mos.  sum all smaller (newer) buckets
	 * tier 2 already includes 1+2
	 * @return
	 */
	public Integer getTierThree(LeadType type) {
		LeadCount cnt = leads.get(LeadsDataToolV2.LeadTierThree);
		if (cnt == null) cnt = new LeadCount();
		
		if (LeadType.Print == type) {
			return Convert.formatInteger(cnt.getPrintCnt()) + getTierTwo(type);
		} else if (LeadType.Email == type) {
			return Convert.formatInteger(cnt.getEmailCnt()) + getTierTwo(type);
		} else if (LeadType.Both == type) {
			return Convert.formatInteger(cnt.getBothCnt()) + getTierTwo(type);
		} else {
			return Convert.formatInteger(cnt.getTotal()) + getTierTwo(type);
		}
	}
	
	/**
	 * returns all leads, regardless of age.  sum all 4 buckets
	 * tier 3 already includes 1+2
	 * @return
	 */
	public Integer getTierFour(LeadType type) {
		LeadCount cnt = leads.get(LeadsDataToolV2.LeadTierFour);
		if (cnt == null) cnt = new LeadCount();
		
		if (LeadType.Print == type) {
			return Convert.formatInteger(cnt.getPrintCnt()) + getTierThree(type);
		} else if (LeadType.Email == type) {
			return Convert.formatInteger(cnt.getEmailCnt()) + getTierThree(type);
		} else if (LeadType.Both == type) {
			return Convert.formatInteger(cnt.getBothCnt()) + getTierThree(type);
		} else {
			return Convert.formatInteger(cnt.getTotal()) + getTierThree(type);
		}
	}
	
	
	/**
	 * helper method for JSP; returns a count value based on conditions passed
	 * @param tier
	 * @param type
	 * @return
	 */
	public Integer getTierCount(int tier, String type) {
		LeadType lType = null;
		if (type != null && type.length() > 0) {
			try {
				lType = LeadType.valueOf(type);
			} catch (Exception e) {
				//don't care about this, a blank value is acceptable
			}
		}
		
		//return a count based on the tier requested
		switch (tier) {
			case 4:
				return getTierFour(lType);
			case 3:
				return getTierThree(lType);
			case 2:
				return getTierTwo(lType);
			default:
				return getTierOne(lType);
			}
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
		switch (range.intValue()) {
			case LeadsDataToolV2.LeadTierOne:
				this.checkedTierOne = checked; break;
			case LeadsDataToolV2.LeadTierTwo:
				this.checkedTierTwo = checked; break;
			case LeadsDataToolV2.LeadTierThree:
				this.checkedTierThree = checked; break;
			case LeadsDataToolV2.LeadTierFour:
				this.checkedTierFour = checked; break;
		}
	}
	
	
	/**
	 * **************************************************************************
	 * <b>Title</b>: LeadCityVO.java<p/>
	 * <b>Description: holds the counts for email & postcard leads</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2016<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Jul 14, 2016
	 ***************************************************************************
	 */
	public class LeadCount {
		private int printCnt = 0;
		private int emailCnt = 0;
		private int bothCnt = 0;
		
		public LeadCount() {
		}
		public void incrEmailCnt(int emailCnt) {
			this.emailCnt += emailCnt;
		}
		public void incrPrintCnt(int printCnt) {
			this.printCnt += printCnt;
		}
		public void incrBothCnt(int bothCnt) {
			this.bothCnt += bothCnt;
		}
		public int getEmailCnt() {
			return emailCnt;
		}
		public int getPrintCnt() {
			return printCnt;
		}
		public int getBothCnt() {
			return bothCnt;
		}
		public int getTotal() {
			return (printCnt + emailCnt);
		}
	}
}
