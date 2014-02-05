package com.depuy.events_v2.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events.vo.LeadCityVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.depuy.events_v2.vo.PersonVO.Role;
import com.depuy.events.vo.DePuyEventLeadSourceVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.Location;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.action.event.vo.EventPostcardVO;

/*****************************************************************************
 <p><b>Title</b>: DePuyEventPostcardVO.java</p>
 <p>Data Bean that stores information for a single event postcard</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jan 15, 2014
 Code Updates
 ***************************************************************************/

public class DePuyEventSeminarVO extends EventPostcardVO {
	private static final long serialVersionUID = 1l;
    
	private DePuyEventSurgeonVO surgeon = null;
	private Set<String> joints = null;  //comes from DEPUY_EVENT_SPECIALTY_XR
	private Set<PersonVO> people = null;
    
	private CoopAdVO newspaperAd = null;
	private CoopAdVO radioAd = null;
	private List<DePuyEventLeadSourceVO> leadSources = null;;
	private List<UserDataVO> leadsData = null;
	private int rsvpCount = 0;
	private String baseUrl = null; //used in reports to give AbsURLs to urls in the Excel files.
	
	private Map<String, Integer> rsvpReferralSources = null; 
	
	private Map<Location, LeadCityVO> targetLeads = null;
	private int totalSelectedLeads = 0;
    
    public DePuyEventSeminarVO() {
	    super();
	    joints = new HashSet<String>();
	    people = new HashSet<PersonVO>();
    }
    
    /**
     * This constructor is called from Select when a single Seminar is being loaded.
     * It's intended to complement a single row in the RS.
     * @param rs
     */
    public DePuyEventSeminarVO(ResultSet rs) {
		this();
		super.setData(rs);
		super.setStatusFlg(new DBUtil().getIntVal("pc_status_flg", rs));
		
	    //add the Event
	    List<EventEntryVO> lst = new ArrayList<EventEntryVO>();
	    lst.add(new EventEntryVO(rs));
	    super.setEvents(lst);
	    
	    //add the Surgeon
	    surgeon = new DePuyEventSurgeonVO(rs);
    }
    
    /**
     * this RS populator is called when a list of seminars is loaded (small RS)
     * @param rs
     * @return this, because a constructor that accepts an RS is already defined.
     */
    public DePuyEventSeminarVO populateFromListRS(ResultSet rs) {
	    DBUtil db = new DBUtil();
	    super.setEventPostcardId(db.getStringVal("event_postcard_id", rs));
	    super.setStatusFlg(db.getIntVal("status_flg", rs));
	    super.setProfileId(db.getStringVal("profile_id", rs));
	    super.setPostcardTypeFlg(db.getIntegerVal("content_no", rs));
	    super.setAuthorizationText(db.getStringVal("authorization_txt", rs));
	    
	    List<EventEntryVO> lst = new ArrayList<EventEntryVO>();
	    EventEntryVO event = new EventEntryVO();
	    event.setActionId(db.getStringVal("event_entry_id", rs));
	    event.setEventName(db.getStringVal("event_nm", rs));
	    event.setRsvpCode(db.getStringVal("rsvp_code_txt", rs));
	    event.setStartDate(db.getDateVal("start_dt", rs));
	    event.setEventTypeCd(db.getStringVal("type_nm", rs));
	    event.setCityName(db.getStringVal("city_nm", rs));
	    event.setStateCode(db.getStringVal("state_cd", rs));
	    lst.add(event);
	    super.setEvents(lst);
	    
	    surgeon = new DePuyEventSurgeonVO();
	    surgeon.setSurgeonName(db.getStringVal("surgeon_nm", rs));

		if (db.getIntVal("hip", rs) > 0) joints.add("4");
	    	if (db.getIntVal("knee", rs) > 0) joints.add("5");
	    	if (db.getIntVal("shoulder", rs) > 0) joints.add("6");
	    	
	    	rsvpCount = db.getIntVal("rsvp_no", rs);
	    	
	    	String runDates = db.getStringVal("run_dates_txt", rs);
	    	if (runDates != null) {
	    		this.newspaperAd = new CoopAdVO();
	    		this.newspaperAd.setAdDatesText(runDates);
	    	}
	    db = null;
	    return this;
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
			if (super.getEventCount() == 1) {
				firstEventDate = super.getEvents().get(0).getStartDate();
				return firstEventDate;
			}
			
			try {
				for (EventEntryVO event : super.getEvents()) {
					if (firstEventDate == null || event.getStartDate().before(firstEventDate))
						firstEventDate = event.getStartDate();
				}
			} catch (Exception e) {
				firstEventDate = Calendar.getInstance().getTime();
			}
		}
		
		if (firstEventDate == null) firstEventDate = Calendar.getInstance().getTime();
		return firstEventDate;
	}
	
	
	public Date getRSVPDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(getEarliestEventDate());
		c.add(Calendar.DATE, -2);   //2 days prior to earliest event
		return c.getTime();
	}
	
	public Date getReminderPostcardSendDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(getEarliestEventDate());
		c.add(Calendar.DATE, -10);   //10 days prior to earliest event
		return c.getTime();
	}
	
	public Date getPostcardApprovalDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(getEarliestEventDate());
		c.add(Calendar.DATE, -28);   //14 days prior to earliest event
		return c.getTime();
	}
	
	public Date getPostcardSendDate() {
		Calendar c = Calendar.getInstance();
		c.setTime(getEarliestEventDate());
		c.add(Calendar.DATE, -21);   //21 days prior to earliest event
		return c.getTime();
	}
	
	public Date getAddtlPostcardSendDate() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, +2);   //2 days from today
		return c.getTime();
	}
	
	public int getLeadsCount() {
		int cnt = 0;
		for (DePuyEventLeadSourceVO lead : this.getLeadSources()) {
			cnt = cnt + lead.getLeadsCnt();
		}
		return cnt;
	}
	
	
	public String toString() {
		return super.toString();
	}


	/**
	 * concats the joints in the Set to build a proper product name to display;
	 * this is standardized here and used in JSPs, reports, and emails.
	 * @return
	 */
	public String getJointLabel() {
		if (joints == null) return "";
		StringBuilder sb = new StringBuilder();
		for (String joint : joints) {
			if (sb.length() > 0) sb.append("/");
			if ("4".equals(joint)) { 
				sb.append("Hip");
			} else if ("5".equals(joint)) {
				sb.append("Knee");
			} else if ("6".equals(joint)) {
				sb.append("Shoulder");
			}
		}
		
		return sb.toString();
	}
	
	public Set<String> getJoints() {
		return joints;
	}
	
	/**
	 * returns the joint name, UCASE, for the passed code (int)
	 * This maps AAMD's IDs to DataFeed's ucase-names.
	 * @param id
	 * @return
	 */
	public String getJointName(String id) {
		if ("4".equals(id)) { 
			return "HIP";
		} else if ("5".equals(id)) {
			return "KNEE";
		} else if ("6".equals(id)) {
			return "SHOULDER";
		}
		return null;
	}
	
	public String getJointCodes() {
		StringBuilder sb = new StringBuilder();
		for (String j : joints) {
			if (sb.length() > 0) sb.append(",");
			sb.append(j);
		}
		return sb.toString();
	}
	
	public void addJoint(String j) {
		joints.add(j);
	}
	public void setJoints(Set<String> joints) {
		this.joints = joints;
	}

	public DePuyEventSurgeonVO getSurgeon() {
		return surgeon;
	}

	public void setSurgeon(DePuyEventSurgeonVO surgeon) {
		this.surgeon = surgeon;
	}

	public CoopAdVO getNewspaperAd() {
		return newspaperAd;
	}

	public void setNewspaperAd(CoopAdVO newspaperAd) {
		this.newspaperAd = newspaperAd;
	}

	public CoopAdVO getRadioAd() {
		return radioAd;
	}

	public void setRadioAd(CoopAdVO radioAd) {
		this.radioAd = radioAd;
	}


	public int getRsvpCount() {
		return rsvpCount;
	}


	public void setRsvpCount(int rsvpCount) {
		this.rsvpCount = rsvpCount;
	}


	public Set<PersonVO> getPeople() {
		return people;
	}


	public void setPeople(Set<PersonVO> people) {
		this.people = people;
	}
	
	public void addPerson(PersonVO person) {
		if (people.contains(person)) return;
		this.people.add(person);
	}
	
	/**
	 * flattens the TGMs into a string of email addresses
	 * @return
	 */
	public String getTgmEmail() {
		StringBuilder emails = new StringBuilder();
		for (PersonVO p : people) {
			if (Role.TGM == p.getRoleCode()) {
				if (emails.length() > 0) emails.append(", ");
				if (p.getEmailAddress() != null) emails.append(p.getEmailAddress());
			}
		}
		return emails.toString().toLowerCase();
	}
	
	/**
	 * returns the first Rep found on the list
	 * @return
	 */
	public PersonVO getFirstRep() {
		for (PersonVO p : people) {
			if (Role.REP == p.getRoleCode()) return p;
		}	
		return new PersonVO();
	}
	
	/**
	 * returns the 2nd Rep found on the list
	 * @return
	 */
	public PersonVO getSecondRep() {
		int cnt = 0;
		for (PersonVO p : people) {
			if (Role.REP != p.getRoleCode()) continue;
			if (Role.REP == p.getRoleCode() && cnt == 0) {
				++cnt;
				continue;
			}
			return p;
		}	
		return new PersonVO();
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Map<String, Integer> getRsvpReferralSources() {
		return rsvpReferralSources;
	}

	public void setRsvpReferralSources(Map<String, Integer> rsvpReferralSources) {
		this.rsvpReferralSources = rsvpReferralSources;
	}

	public Map<Location, LeadCityVO> getTargetLeads() {
		return targetLeads;
	}

	public void setTargetLeads(Map<Location, LeadCityVO> targetLeads) {
		this.targetLeads = targetLeads;
	}
	
	
	public int getTotalSelectedLeads() {
		//calculate the total based on targetLeads selected.  This is used on the leads page
		if (totalSelectedLeads == 0 && targetLeads != null) {
			for (Location loc : targetLeads.keySet()) {
				LeadCityVO vo = targetLeads.get(loc);
				if (vo.getTierFourChecked()) totalSelectedLeads += vo.getTierFour();
				else if (vo.getTierThreeChecked()) totalSelectedLeads += vo.getTierThree();
				else if (vo.getTierTwoChecked()) totalSelectedLeads += vo.getTierTwo();
				else if (vo.getTierOneChecked()) totalSelectedLeads += vo.getTierOne();
			}
		} else {
			totalSelectedLeads = Convert.formatInteger(super.getPcAttribute1(), 0);
		}
		
		return totalSelectedLeads;
	}
	
	public boolean isDayOf() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return this.getEarliestEventDate().equals(cal.getTime());
	}
	
	public boolean isComplete() {
		Calendar cal = Calendar.getInstance();
		return cal.after(this.getEarliestEventDate());
	}

	public String getLeadSortType() {
		return super.getPcAttribute2();
	}

}