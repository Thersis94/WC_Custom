package com.depuysynthesinst.events;

import java.sql.ResultSet;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.DatabaseColumn;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;

/*****************************************************************************
<p><b>Title</b>: EventEntryVO.java</p>
<p>CourseCalendar specific implementation of EventEntryVO. Overrides 
certain setters to change annotation values, and adds fields for the extra
event description fields in the imported excel files.</p>
<p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Erik Wingo
@since Aug 28, 2014
 ***************************************************************************/
public class CourseCalendarVO extends EventEntryVO {

	private static final long serialVersionUID = 1L;
	private String objective2;
	private String objective3;
	private String objective4;

	public CourseCalendarVO() {
		super();
	}

	/**
	 * @param rs
	 */
	public CourseCalendarVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @param req
	 */
	public CourseCalendarVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param contactName The contactName to set.
	 */
	@Importable(name = "Event Owner", type = DataType.STRING)
	@DatabaseColumn(column = "CONTACT_NM", dataType = "nvarchar(80)", table = "EVENT_ENTRY")
	public void setContactName(String contactName) {
		super.setContactName(contactName); 
	}

	/**
	 * @param eventDesc The eventDesc to set.
	 */
	@Importable(name = "Displayed Event Description", type = DataType.STRING)
	@DatabaseColumn(column = "EVENT_DESC", dataType = "ntext", table = "EVENT_ENTRY")
	public void setEventDesc(String eventDesc) {
		super.setEventDesc(eventDesc);
	}

	/**
	 * @param eventName The eventName to set.
	 */
	@Importable(name = "Event Title", type = DataType.STRING)
	@DatabaseColumn(column = "EVENT_NM", dataType = "nvarchar(150)", table = "EVENT_ENTRY")
	public void setEventName(String eventName) {
		super.setEventName(eventName);
	}

	/**
	 * @param shortDesc The shortDesc to set.
	 */
	@Importable(name = "Intended Audience", type = DataType.STRING)
	@DatabaseColumn(column = "SHORT_DESC", dataType = "nvarchar(500)", table = "EVENT_ENTRY")
	public void setShortDesc(String shortDesc) {
		super.setShortDesc(shortDesc);
	}

	/**
	 * @param The cityName to set.
	 */
	@Importable(name = "Event City", type = DataType.STRING)
	@DatabaseColumn(column = "CITY_NM", dataType = "nvarchar(40)", table = "EVENT_ENTRY")
	public void setCityName(String cityName) {
		super.setCityName(cityName);
	}

	/**
	 * @param The stateCode to set.
	 */
	@Importable(name = "Event State/Prov. Code", type = DataType.STRING)
	@DatabaseColumn(column = "STATE_CD", dataType = "nvarchar(5)", table = "EVENT_ENTRY")
	public void setStateCode(String stateCode) {
		super.setStateCode(stateCode);
	}

	/**
	 * @param The serviceText to set.
	 */
	@Importable(name = "Anatomical Focus 1", type = DataType.STRING)
	@DatabaseColumn(column = "SERVICE_TXT", dataType = "nvarchar(100)", table = "EVENT_ENTRY")
	public void setServiceText(String serviceText) {
		super.setServiceText(serviceText);
	}

	/**
	 * @param objective The objective2 to set.
	 */
	@Importable(name = "Displayed Learning Objective 1", type = DataType.STRING)
	@DatabaseColumn(column = "OBJECTIVES_TXT", dataType = "nvarchar(4000)", table = "EVENT_ENTRY")
	public void setObjectivesText(String objective) {
		super.setObjectivesText(objective);
	}
	
	/**
	 * @param objective The Level 1 Event Type to set.
	 */
	@Importable(name = "Level 1 Event Type", type = DataType.STRING)
	@DatabaseColumn(column = "LOCATION_DESC", dataType = "nvarchar(1000)", table = "EVENT_ENTRY")
	public void setLocationDesc(String locn) {
		super.setLocationDesc(locn);
	}

	/**
	 * @param objective The objective2 to set.
	 */
	@Importable(name = "Displayed Learning Objective 2", type = DataType.STRING)
	public void setObjective2(String objective2) {
		this.objective2 = objective2;
	}

	/**
	 * @param objective The objective3 to set.
	 */
	@Importable(name = "Displayed Learning Objective 3", type = DataType.STRING)
	public void setObjective3(String objective3) {
		this.objective3 = objective3;
	}

	/**
	 * @param objective The objective to set.
	 */
	@Importable(name = "Displayed Learning Objective 4", type = DataType.STRING)
	public void setObjective4(String objective4) {
		this.objective4 = objective4;
	}

	/**
	 * @see com.smt.sitebuilder.action.event.vo.EventEntryVO#getObjective()
	 * overloaded getter so we can contatenate the 4 fields into a <UL> when we go to insert it into the DB.
	 */
	public String getObjectivesText() {
		StringBuilder sb = new StringBuilder();
		String[] descList = {super.getObjectivesText(),objective2,objective3,objective4};

		//Concatenates all objective values into a single unordered list
		sb.append("<ul>");
		for ( String s : descList ) {
			if (s == null || s.length() == 0) continue;
			sb.append("<li>");
			sb.append(s);
			sb.append("</li>");
		}
		sb.append("</ul>");

		return sb.toString();
	}


	/**
	 * @param rsvpCodeText the rsvpCodeText to set
	 */
	@Importable( name = "Event Code", type = DataType.STRING )
	@DatabaseColumn( column = "RSVP_CODE_TXT", dataType = "nvarchar(10)", table = "EVENT_ENTRY" )
	public void setRSVPCode(String rsvp) {
		super.setRSVPCode(rsvp);
	}
}
