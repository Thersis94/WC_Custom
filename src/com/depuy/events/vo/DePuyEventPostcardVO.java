package com.depuy.events.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

import com.depuy.events.vo.DePuyEventLeadSourceVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.event.vo.EventPostcardVO;

/*****************************************************************************
 <p><b>Title</b>: DePuyEventPostcardVO.java</p>
 <p>Data Bean that stores information for a single event postcard</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Mar 9, 2006
 Code Updates
 James Camire, Mar 9, 2006 - Creating Initial Class File
 ***************************************************************************/

public class DePuyEventPostcardVO extends EventPostcardVO {
    private static final long serialVersionUID = 1l;
    public static final String COOP_AD = "coopAd"; //newspaper ad
    public static final String RADIO_AD = "radioAd"; //radio ad
    public static final String PROD_HIP = "4";
    public static final String PROD_KNEE = "5";
    public static final String PROD_SHOULDER = "6";
    public static final String PROD_ORTHOVISC = "15";
    
    private List<DePuyEventEntryVO> depuyEvents = new ArrayList<DePuyEventEntryVO>();
    private List<DePuyEventLeadSourceVO> leadSources = new ArrayList<DePuyEventLeadSourceVO>();
    private List<UserDataVO> leadsData = new ArrayList<UserDataVO>();
    private String groupCd = null;  //used only for Locator reporting
    private Map<String,CoopAdVO> ads;
	private String language = "en";
    
    public DePuyEventPostcardVO() {
    	super();
    	ads = new HashMap<String,CoopAdVO>();
    	ads.put(COOP_AD, new CoopAdVO());
    	ads.put(RADIO_AD, new CoopAdVO());
    }
    
    
    /**
     * Assigns the event data retrieved from the database to the appropriate
     * variables
     * @param rs
     */
    public void setData(ResultSet rs) {
    	DBUtil db = new DBUtil();
    	
    	super.setData(rs);
    	setGroupCd(db.getStringVal("group_cd", rs));
    	
    	CoopAdVO ad = new CoopAdVO(rs);
    	ad.setStatusFlg(db.getIntVal("coop_ad_status_flg", rs));
    	this.setCoopAd(ad);
    	
    	ad = new CoopAdVO();
    	ad.setAdType("radio");
    	ad.setNewspaper1Text(db.getStringVal("radio_nm", rs));
    	ad.setNewspaper1Phone(db.getStringVal("radio_ph", rs));
    	ad.setNewspaper2Text(db.getStringVal("radio_contact_nm", rs));
    	ad.setNewspaper2Phone(db.getStringVal("radio_contact_email", rs));
    	ad.setAdDatesText(db.getStringVal("radio_deadline", rs));
    	ad.setCoopAdId(db.getStringVal("radio_id", rs));
    	this.setRadioAd(ad);
    	
    	db = null;
    }


	public List<DePuyEventEntryVO> getDePuyEvents() {
		return depuyEvents;
	}

	public void setDePuyEvents(List<DePuyEventEntryVO> depuyEvents) {
		this.depuyEvents = depuyEvents;
	}

	public List<DePuyEventLeadSourceVO> getLeadSources() {
		return leadSources;
	}

	public void setLeadSources(List<DePuyEventLeadSourceVO> leadSources) {
		this.leadSources = leadSources;
	}


	public void setLeadsData(List<UserDataVO> d) {
		this.leadsData = d;
	}
	
	public List<UserDataVO> getLeadsData() {
		return this.leadsData;
	}

	public String getRSVPCodes() {
		StringBuffer codes = new StringBuffer();
		for (DePuyEventEntryVO event : depuyEvents) {
			codes.append(event.getRSVPCode() + ", ");
		}
		if (codes.length() > 2) return codes.substring(0, codes.length()-2);
		else return codes.toString();
	}

	/**
	 * JSP helper method tells if the earliest event is at least 21 days from today (3wks).
	 * @return true if it is, false otherwise.
	 */
	public boolean getAdEligible() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, 21);
		return (getEarliestEventDate().after(c.getTime()));
	}
	
	public Date getEarliestEventDate() {
		if (firstEventDate == null) {  //only need to iterator once to set the member variable

			try {
				for (DePuyEventEntryVO event : depuyEvents) {
					if (firstEventDate == null || event.getStartDate().before(firstEventDate))
						firstEventDate = event.getStartDate();
				}
			} catch (Exception e) {
				firstEventDate = new Date();
			}
		}
		
		if (firstEventDate == null) firstEventDate = new Date();
		//log.debug("firstEventDate=" + firstEventDate);
		return firstEventDate;
	}
	
	public Date getLatestEventDate() {
		if (lastEventDate == null) {  //only need to iterator once to set the member variable

			try {
				for (DePuyEventEntryVO event : depuyEvents) {
					if (lastEventDate == null || event.getStartDate().after(lastEventDate))
						lastEventDate = event.getStartDate();
				}
			} catch (Exception e) {
				firstEventDate = new Date();
			}
		}
		
		if (lastEventDate == null) lastEventDate = new Date();
		//log.debug("lastEventDate=" + lastEventDate);
		return lastEventDate;
	}
	
	public DePuyEventEntryVO getEventById(String eventEntryId) {
		DePuyEventEntryVO thisEvent = new DePuyEventEntryVO();
		try {
			for (DePuyEventEntryVO event : depuyEvents) {
				if (event.getActionId().equals(eventEntryId)) {
					thisEvent = event;
					break;
				}
			}
		} catch (Exception e) {
		}
		return thisEvent;
	}
	
	public Date getRSVPDate() {
		return new Date(getEarliestEventDate().getTime() - (2*86400000)); //2 days prior to earliest event
	}
	
	public Date getReminderPostcardSendDate() {
		return new Date(getEarliestEventDate().getTime() - (10*86400000)); //10 dates prior to earliest event
	}
	
	public Date getPostcardSendDate() {
		return new Date(getEarliestEventDate().getTime() - (21*86400000)); //21 days prior to earliest event
	}
	
	public Date getAddtlPostcardSendDate() {
		return new Date(new Date().getTime() + (2*86400000)); //2 days from now send the addtl postcards
	}
	
	public Integer getEventCount() {
		Integer cnt = new Integer(0);
		if (depuyEvents != null) cnt = depuyEvents.size();
		return cnt;
	}
	
	public String getEventLocations() {
		StringBuffer locs = new StringBuffer("");
		String lastLoc = "";
		for (DePuyEventEntryVO event : depuyEvents) {
			lastLoc = (event.getCityName() + ", " + event.getStateCode() + " " + event.getZipCode() + "  ");
			locs.append(lastLoc);
		}
		if (locs.length() > 2) return locs.substring(0, locs.length()-2);
		else return locs.toString();
	}

	public int getLeadsCount() {
		int cnt = 0;
		for (DePuyEventLeadSourceVO lead : this.getLeadSources()) {
			cnt = cnt + lead.getLeadsCnt();
		}
		return cnt;
	}
	
	public boolean isExpired() {
		return this.getLatestEventDate().before(new Date());
	}
	
	public String toString() {
		return super.toString();
	}


	public String getGroupCd() {
		return groupCd;
	}


	public void setGroupCd(String groupCd) {
		this.groupCd = groupCd;
	}
	
	public String getProductName() {
		String prod = getGroupCd();
		if (prod == null) {
			return "";
		} else if (prod.equals(PROD_HIP)) {
			return "hip";
		} else if (prod.equals(PROD_SHOULDER)) {
			return "shoulder";
		} else if (prod.equals(PROD_ORTHOVISC)) {
			return "ORTHOVISC"; //orthovisc Knee
		} else { 
			return "knee";
		}
	}
	
	public void setLanguage(String lang) {
		this.language = lang;
	}
	
	public String getLanguage() { return language; }

	@Deprecated
	public String getCoopAdId() {
		return ads.get(COOP_AD).getCoopAdId();
	}

	@Deprecated
	public Integer getCoopAdStatusFlg() {
		return ads.get(COOP_AD).getStatusFlg();
	}

	public CoopAdVO getCoopAd() {
		return this.ads.get(COOP_AD);
	}


	public void setCoopAd(CoopAdVO coopAd) {
		this.ads.put(COOP_AD, coopAd);
	}
	
	public CoopAdVO getRadioAd() {
		return this.ads.get(RADIO_AD);
	}


	public void setRadioAd(CoopAdVO coopAd) {
		this.ads.put(RADIO_AD, coopAd);
	}

}
