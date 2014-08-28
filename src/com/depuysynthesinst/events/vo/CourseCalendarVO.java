/**
 * 
 */
package com.depuysynthesinst.events.vo;

import java.sql.ResultSet;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.DatabaseColumn;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
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

	private String eventDesc2;
	private String eventDesc3;
	private String eventDesc4;
	
	public CourseCalendarVO(){
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
	public CourseCalendarVO(SMTServletRequest req) {
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
	@Importable(name = "Displayed Learning Objective #1", type = DataType.STRING)
	@DatabaseColumn(column = "EVENT_DESC", dataType = "ntext", table = "EVENT_ENTRY")
	public void setEventDesc(String eventDesc) {
		super.setEventDesc(eventDesc);
	}

	/**
	 * @param eventName The eventName to set.
	 */
	@Importable(name = "Event Title", type = DataType.STRING)
	@DatabaseColumn(column = "EVENT_NM", dataType = "nvarchar(100)", table = "EVENT_ENTRY")
	public void setEventName(String eventName) {
		super.setEventName(eventName);
	}
	
	/**
	 * @param locationDesc The locationDesc to set.
	 */
	@Importable(name = "Displayed Event Description", type = DataType.STRING)
	@DatabaseColumn(column = "LOCATION_DESC", dataType = "nvarchar(1000)", table = "EVENT_ENTRY")
	public void setLocationDesc(String locationDesc) {
		super.setLocationDesc(locationDesc);
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
	@Importable(name = "Anatomical Focus #1", type = DataType.STRING)
	@DatabaseColumn(column = "SERVICE_TXT", dataType = "nvarchar(100)", table = "EVENT_ENTRY")
	public void setServiceText(String serviceText) {
		super.setServiceText(serviceText);
	}
	
    /**
     * @param eventDesc The eventDesc2 to set.
     */
    @Importable(name = "Displayed Learning Objective #2", type = DataType.STRING)
    public void setEventDesc2(String eventDesc2) {
        this.eventDesc2 = eventDesc2;
    }
    
    /**
     * @param eventDesc The eventDesc3 to set.
     */
    @Importable(name = "Displayed Learning Objective #3", type = DataType.STRING)
    public void setEventDesc3(String eventDesc3) {
        this.eventDesc3 = eventDesc3;
    }
    
    /**
     * @param eventDesc The eventDesc to set.
     */
    @Importable(name = "Displayed Learning Objective #4", type = DataType.STRING)
    public void setEventDesc4(String eventDesc4) {
        this.eventDesc4 = eventDesc4;
    }
    
    /**
     * @see com.smt.sitebuilder.action.event.vo.EventEntryVO#getEventDesc()
     */
    public String getEventDesc(){
    	StringBuilder sb = new StringBuilder();
    	String[] descList = {super.getEventDesc(),eventDesc2,eventDesc3,eventDesc4};
    	
    	//Concatenates all eventDesc values into a single unordered list
		sb.append("<ul>");
		for( String s : descList ){
			String value = StringUtil.checkVal(s);
			if ( value.length() > 0 ){
				sb.append("<li>");
				sb.append(value);
				sb.append("</li>");
			}
		}
		sb.append("</ul>");
		
		return sb.toString();
    }

    /**
	 * @return the eventDesc2
	 */
	public String getEventDesc2() {
		return eventDesc2;
	}

	/**
	 * @return the eventDesc3
	 */
	public String getEventDesc3() {
		return eventDesc3;
	}

	/**
	 * @return the eventDesc4
	 */
	public String getEventDesc4() {
		return eventDesc4;
	}
}
