package com.depuy.events.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.depuy.events.OutstandingItems.ActionItem;
import com.depuy.events.vo.CoopAdVO;
import com.depuy.events.vo.LeadCityVO;
import com.depuy.events.vo.DePuyEventLeadSourceVO;
import com.depuy.events.vo.DePuyEventSurgeonVO;
import com.depuy.events.vo.LeadCityVO.LeadType;
import com.depuy.events.vo.PersonVO.Role;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.Location;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.event.EventFacadeAction;
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

	private List<DePuyEventSurgeonVO> surgeons = null;
	private Set<String> joints = null;  //comes from DEPUY_EVENT_SPECIALTY_XR
	private Set<String> products = null;  //comes from DEPUY_EVENT_SPECIALTY_XR
	private Set<PersonVO> people = null;
	private Map<Long, ConsigneeVO> consignees = null; //JSTL wants us to use a Long key instead of an Integer here.
	private Set<ActionItem> actionItems = null;

	private CoopAdVO radioAd = null;
	private List<CoopAdVO> newspaperAds = null;
	private List<CoopAdVO> onlineAds = null;
	private List<DePuyEventLeadSourceVO> leadSources = null;;
	private List<UserDataVO> leadsData = null;
	private int rsvpCount = 0;
	private String baseUrl = null; //used in reports to give AbsURLs to urls in the Excel files.

	private Map<String, Integer> rsvpReferralSources = null; 
	private Map<String, String> surveyResponses = null;
	private Map<Location, LeadCityVO> targetLeads = null;
	private int totalSelectedLeads = 0;
	private int totalSelectedEmailLeads = 0;
	private int totalSelectedBothLeads = 0;
	private int upfrontFeeFlg = 0;

	private boolean readOnly = false;

	public DePuyEventSeminarVO() {
		super();
		joints = new HashSet<>();
		products = new HashSet<>();
		people = new HashSet<>();
		newspaperAds = new ArrayList<>();
		onlineAds = new ArrayList<>();
		consignees = new HashMap<>();
		surgeons = new ArrayList<>();
	}

	/**
	 * This constructor is called from Select when a single Seminar is being loaded.
	 * It's intended to complement a single row in the RS.
	 * @param rs
	 */
	public DePuyEventSeminarVO(ResultSet rs) {
		this();
		super.setData(rs);
		DBUtil db = new DBUtil();
		super.setStatusFlg(db.getIntVal("pc_status_flg", rs));

		UserDataVO owner = new UserDataVO();
		owner.setProfileId(super.getProfileId());
		super.setOwner(owner);

		addProduct(db.getStringVal("product_id", rs));

		//add the Event
		List<EventEntryVO> lst = new ArrayList<>();
		lst.add(new EventEntryVO(rs));
		super.setEvents(lst);
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
		UserDataVO owner = new UserDataVO();
		owner.setProfileId(super.getProfileId());
		super.setOwner(owner);
		super.setPostcardTypeFlg(db.getIntegerVal("content_no", rs));
		super.setAuthorizationText(db.getStringVal("authorization_txt", rs));
		super.setPostcardFileStatusFlg(db.getIntVal("postcard_file_status_no", rs));
		super.setLanguageCode( db.getStringVal("language_cd", rs) );
		super.setTerritoryNumber( db.getIntegerVal("territory_no", rs));
		super.setPostcardMailDate(db.getDateVal("postcard_mail_dt", rs));
		super.setConsumableOrderDate(db.getDateVal("CONSUMABLE_ORDER_DT", rs));
		super.setInviteFileUrl(db.getStringVal("INVITE_FILE_URL", rs));
		super.setInviteFileFlg(db.getIntegerVal("INVITE_FILE_FLG", rs));

		List<EventEntryVO> lst = new ArrayList<>();
		EventEntryVO event = new EventEntryVO();
		event.setActionId(db.getStringVal("event_entry_id", rs));
		event.setEventName(db.getStringVal("event_nm", rs));
		event.setRsvpCode(db.getStringVal("rsvp_code_txt", rs));
		event.setStartDate(db.getDateVal("start_dt", rs));
		event.setEventTypeCd(db.getStringVal("type_nm", rs));
		event.setCityName(db.getStringVal("city_nm", rs));
		event.setStateCode(db.getStringVal("state_cd", rs));
		event.setEventDesc(db.getStringVal("event_desc", rs));
		lst.add(event);
		super.setEvents(lst);

		DePuyEventSurgeonVO surgeon = new DePuyEventSurgeonVO();
		surgeon.setSurgeonName(db.getStringVal("surgeon_nm", rs));
		addSurgeon(surgeon);

		upfrontFeeFlg = db.getIntVal("upfront_cost_flg", rs);

		int joint = db.getIntVal("joint_id", rs);
		if (joint > 0) joints.add(Integer.toString(joint));

		addProduct(db.getStringVal("product_id", rs));

		rsvpCount = db.getIntVal("rsvp_no", rs);

		CoopAdVO ad = new CoopAdVO();
		ad.setStatusFlg(db.getIntVal("ad_status_flg", rs));
		newspaperAds.add(ad);

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
		return getEarliestEventDate().after(c.getTime());
	}

	public Date getEarliestEventDate() {
		if (firstEventDate == null) {  //only need to iterate once to set the member variable
			if (super.getEventCount() == 1) {
				firstEventDate = super.getEvents().get(0).getStartDate();

			} else {
				try {
					for (EventEntryVO event : super.getEvents()) {
						if (firstEventDate == null || event.getStartDate().before(firstEventDate))
							firstEventDate = event.getStartDate();
					}
				} catch (Exception e) {
					firstEventDate = Calendar.getInstance().getTime();
				}
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
		return (surgeons != null && !surgeons.isEmpty()) ? surgeons.get(0) : null;
	}

	public List<DePuyEventSurgeonVO> getSurgeonList() {
		return surgeons;
	}

	public void addSurgeon(DePuyEventSurgeonVO surgeon) {
		this.surgeons.add(surgeon);
	}

	public List<CoopAdVO> getNewspaperAds() {
		return newspaperAds;
	}

	public void setNewspaperAds(List<CoopAdVO> list) {
		this.newspaperAds = list;
	}

	public List<CoopAdVO> getOnlineAds(){
		return onlineAds;
	}

	public void setOnlineAds( List<CoopAdVO> list ){
		this.onlineAds = list;
	}

	public List<CoopAdVO> getAllAds(){
		List<CoopAdVO> list = new ArrayList<>(newspaperAds);
		list.addAll(onlineAds);
		return list;
	}

	public void addAdvertisement(CoopAdVO ad) {
		if (ad.getOnlineFlg() != null && ad.getOnlineFlg() == 1)
			onlineAds.add(ad);
		else
			newspaperAds.add(ad);
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

	/**
	 * returns the combined total of email leads plus print leads
	 * @return
	 */
	public int getTotalSelectedLeads() {
		return getTotalSelectedPrintLeads() + getTotalSelectedEmailLeads();
	}

	/**
	 * returns the total number of postcard-eligible leads based on the LeadCityVO selections
	 * @return
	 */
	public int getTotalSelectedPrintLeads() {
		if (totalSelectedLeads == 0 && targetLeads != null) {
			for (Map.Entry<Location, LeadCityVO> entry: targetLeads.entrySet()) {
				LeadCityVO vo = entry.getValue();
				if (vo.getTierFourChecked()) totalSelectedLeads += vo.getTierFour(LeadType.Print);
				else if (vo.getTierThreeChecked()) totalSelectedLeads += vo.getTierThree(LeadType.Print);
				else if (vo.getTierTwoChecked()) totalSelectedLeads += vo.getTierTwo(LeadType.Print);
				else if (vo.getTierOneChecked()) totalSelectedLeads += vo.getTierOne(LeadType.Print);
			}
		} else if (targetLeads == null) {
			totalSelectedLeads = Convert.formatInteger(super.getPcAttribute1(), 0);
		}
		return totalSelectedLeads;
	}


	/**
	 * returns the total number of email-eligible leads based on the LeadCityVO selections
	 * @return
	 */
	public int getTotalSelectedEmailLeads() {
		//calculate the total based on targetLeads selected.  This is used on the leads page
		if (totalSelectedEmailLeads == 0 && targetLeads != null) {
			for (Map.Entry<Location, LeadCityVO> entry: targetLeads.entrySet()) {
				LeadCityVO vo = entry.getValue();
				if (vo.getTierFourChecked()) totalSelectedEmailLeads += vo.getTierFour(LeadType.Email);
				else if (vo.getTierThreeChecked()) totalSelectedEmailLeads += vo.getTierThree(LeadType.Email);
				else if (vo.getTierTwoChecked()) totalSelectedEmailLeads += vo.getTierTwo(LeadType.Email);
				else if (vo.getTierOneChecked()) totalSelectedEmailLeads += vo.getTierOne(LeadType.Email);
			}
		} else if (targetLeads == null) {
			totalSelectedEmailLeads = Convert.formatInteger(super.getPcAttribute3(), 0);
		}
		return totalSelectedEmailLeads;
	}

	/**
	 * returns the total number of email AND print eligible leads based on the LeadCityVO selections
	 * @return
	 */
	public int getTotalSelectedBothLeads() {
		//calculate the total based on targetLeads selected.  This is used on the leads page
		if (totalSelectedBothLeads == 0 && targetLeads != null) {
			for (Map.Entry<Location, LeadCityVO> entry: targetLeads.entrySet()) {
				LeadCityVO vo = entry.getValue();
				if (vo.getTierFourChecked()) totalSelectedBothLeads += vo.getTierFour(LeadType.Both);
				else if (vo.getTierThreeChecked()) totalSelectedBothLeads += vo.getTierThree(LeadType.Both);
				else if (vo.getTierTwoChecked()) totalSelectedBothLeads += vo.getTierTwo(LeadType.Both);
				else if (vo.getTierOneChecked()) totalSelectedBothLeads += vo.getTierOne(LeadType.Both);
			}
		} else if (targetLeads == null) {
			totalSelectedBothLeads = Convert.formatInteger(super.getPcAttribute4(), 0);
		}
		return totalSelectedBothLeads;
	}

	public boolean isDayOf() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return this.getEarliestEventDate().equals(cal.getTime());
	}

	public boolean isTimeToOrderConsumables() {
		if (this.isComplete()) return false;

		Calendar cal = Calendar.getInstance();		
		Calendar eventDtM10 = Calendar.getInstance();
		eventDtM10.setTime(this.getEarliestEventDate());
		eventDtM10.add(Calendar.DATE, -10);

		return cal.getTime().after(eventDtM10.getTime());
	}

	public boolean isComplete() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return this.getEarliestEventDate().before(cal.getTime());
	}

	public boolean isMinDaysAway(int days) {
		Calendar milestone = Calendar.getInstance();
		milestone.setTime(this.getEarliestEventDate());
		milestone.add(Calendar.DAY_OF_YEAR, 0 - days);

		//true if the today is greater-than or equal to the milestone
		return Calendar.getInstance().getTime().compareTo(milestone.getTime()) > -1;
	}

	public String getLeadSortType() {
		return super.getPcAttribute2();
	}

	public boolean isPromotePgCompleted() {
		//CPSEM does not use the Ads system, so they get a free pass here
		boolean adApproved = "CPSEM".equalsIgnoreCase(getEvents().get(0).getEventTypeCd());
		if (!adApproved && newspaperAds != null && !newspaperAds.isEmpty() ) {
			//if there is a non-empty list, loop through to see if all entries are complete
			adApproved = true;
			//if any ad is not complete, change adApproved back to false and proceed
			for( CoopAdVO ad : newspaperAds ){
				if ( ad.getStatusFlg() == null || ad.getStatusFlg() != 3 )
					adApproved = false;
			}
		}
		return getPostcardFileStatusFlg() == 3 && adApproved;
	}


	/**
	 * returns total Ad costs for a specific party
	 * @param forWhom
	 * @return
	 */
	public Double getAdCost(String forWhom) {
		Double cost = 0.0;

		for (CoopAdVO ad : this.getAllAds()) {
			switch (forWhom) {
				case "depuy":
					cost += ad.getCostToDepuyNo();
					break;
				case "hospital":
					cost += ad.getCostToHospitalNo();
					break;
				case "surgeon":
					cost += ad.getCostToSurgeonNo();
					break;
				case "rep":
					cost += ad.getCostToRepNo();
					break;
				case "total":
					cost += ad.getTotalCostNo();
					break;
				default:
			}
		}

		return cost;
	}


	/**
	 * used by reports to cosmetically label the statusNo
	 * @return
	 */
	public String getStatusName() {
		switch (getStatusFlg()) {
			case EventFacadeAction.STATUS_PENDING: return "Pending ADV Approval";
			case EventFacadeAction.STATUS_PENDING_PREV_ATT: return "Pending SRC Review";
			case EventFacadeAction.STATUS_APPROVED_SRC: return "Approved by SRC";
			case EventFacadeAction.STATUS_PENDING_SURG: return "Awaiting Signed Contract";
			case EventFacadeAction.STATUS_APPROVED: return "Approved";
			case EventFacadeAction.STATUS_CANCELLED: return "Cancelled";
			case EventFacadeAction.STATUS_COMPLETE: return "Completed";
			default: return "";
		}
	}

	/**
	 * @return the upfrontFeeFlg
	 */
	public int getUpfrontFeeFlg() {
		return upfrontFeeFlg;
	}

	/**
	 * @param upfrontFeeFlg the upfrontFeeFlg to set
	 */
	public void setUpfrontFeeFlg(int upfrontFeeFlg) {
		this.upfrontFeeFlg = upfrontFeeFlg;
	}

	public Map<Long, ConsigneeVO> getConsignees() {
		return consignees;
	}

	public void setConsignees(Map<Long, ConsigneeVO> consignees) {
		this.consignees = consignees;
	}

	public void addConsignee(ConsigneeVO vo) {
		this.consignees.put(Long.valueOf(vo.getTypeNo()), vo);
	}

	public Set<ActionItem> getActionItems() {
		return actionItems;
	}

	public void setActionItems(Set<ActionItem> actionItems) {
		this.actionItems = actionItems;
	}
	public void addActionItem(ActionItem actionItem) {
		if (actionItems == null) actionItems = new HashSet<>();
		actionItems.add(actionItem);
	}

	public boolean isMitekSeminar() {
		String typeCd = "";
		try {
			typeCd = super.getEvents().get(0).getEventTypeCd();
		} catch (Exception e) {}
		return typeCd.startsWith("MITEK-");
	}

	public boolean isHospitalSponsored() {
		String typeCd = "";
		try {
			typeCd = super.getEvents().get(0).getEventTypeCd();
		} catch (Exception e) {}
		return "HSEM".equals(typeCd);
	}


	public String getAllSurgeonNames() {
		if (surgeons == null || surgeons.isEmpty()) return "";
		StringBuilder names = new StringBuilder(100);
		for (DePuyEventSurgeonVO vo : surgeons) {
			if (names.length() > 0) names.append(" &amp; ");
			names.append(vo.getSurgeonName());
		}
		return names.toString();
	}


	/**
	 * survey responses are used on the Custom Report - loaded from the Survey portlet
	 * @param key
	 * @param val
	 */
	public void addSurveyResponse(String key, String val) {
		if (surveyResponses == null) surveyResponses = new HashMap<>();
		surveyResponses.put(key, val);
	}
	public String getSurveyResponse(String key) {
		if (surveyResponses == null) return null;
		return surveyResponses.get(key);
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public Set<String> getProducts() {
		return products;
	}

	public void setProducts(Set<String> products) {
		this.products = products;
	}

	public void addProduct(String prod) {
		if (prod == null || prod.length() == 0) return;
		this.products.add(prod);
	}

	/**
	 * returns the joint name, UCASE, for the passed code (int)
	 * This maps AAMD's IDs to DataFeed's ucase-names.
	 * @param id
	 * @return
	 */
	public String getProductName(String id) {
		if ("HA".equals(id)) { 
			return "HA";
		} else if ("PRP".equals(id)) {
			return "PRP";
		} else if ("PEAK".equals(id)) {
			return "PEAK";
		}
		return null;
	}

	public String getProductCodes() {
		StringBuilder sb = new StringBuilder();
		for (String j : products) {
			if (sb.length() > 0) sb.append(",");
			sb.append(j);
		}
		return sb.toString();
	}
}