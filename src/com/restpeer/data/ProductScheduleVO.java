package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProductScheduleVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for storing a single time/day for a schedule
 * of kitchen time
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 22, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_product_schedule")
public class ProductScheduleVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2927571719814414306L;

	// Members
	private String scheduleEventId;
	private String locationProductId;
	private String memberLocationId;
	private DayOfWeek dayOfWeek;
	private int startTime;
	private int endTime;
	private Date createDate;
	private Date updateDate;
	
	// Helpers
	private String startTimeText;
	private String endTimeText;
	
	/**
	 * 
	 */
	public ProductScheduleVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductScheduleVO(ActionRequest req) {
		super(req);
	}
	

	/**
	 * @param rs
	 */
	public ProductScheduleVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Determines if the provided localtime is between the start and end times
	 * @param eval
	 * @return
	 */
	public boolean isBetween(int evalSec) {
		LocalTime eval = LocalTime.ofSecondOfDay(evalSec);
		return eval.isAfter(getStartAsLocalTime()) && eval.isBefore(getEndAsLocalTime());
	}
	
	
	/**
	 * Determines if the provided localtime is between the start and end times
	 * @param eval
	 * @return
	 */
	public boolean isBetween(LocalTime eval) {
		return eval.isAfter(getStartAsLocalTime()) && eval.isBefore(getEndAsLocalTime());
	}
	
	/**
	 * @return the scheduleEventId
	 */
	@Column(name="schedule_event_id", isPrimaryKey=true)
	public String getScheduleEventId() {
		return scheduleEventId;
	}

	/**
	 * @return the locationProductId
	 */
	@Column(name="location_product_id")
	public String getLocationProductId() {
		return locationProductId;
	}

	/**
	 * @return the memberLocationId
	 */
	@Column(name="dealer_location_id")
	public String getMemberLocationId() {
		return memberLocationId;
	}

	/**
	 * @return the dayOfWeek
	 */
	@Column(name="day_of_week_txt")
	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	/**
	 * @return the startTime
	 */
	@Column(name="start_time_no")
	public int getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the LocalTime for the start
	 * @return
	 */
	public LocalTime getStartAsLocalTime() {
		return LocalTime.ofSecondOfDay(startTime);
	}

	/**
	 * @return the endTime
	 */
	@Column(name="end_time_no")
	public int getEndTime() {
		return endTime;
	}
	
	/**
	 * Returns the LocalTime for the start
	 * @return
	 */
	public LocalTime getEndAsLocalTime() {
		return LocalTime.ofSecondOfDay(endTime);
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the startTimeText
	 */
	public String getStartTimeText() {
		return startTimeText;
	}

	/**
	 * @return the endTimeText
	 */
	public String getEndTimeText() {
		return endTimeText;
	}

	/**
	 * @param scheduleEventId the scheduleEventId to set
	 */
	public void setScheduleEventId(String scheduleEventId) {
		this.scheduleEventId = scheduleEventId;
	}

	/**
	 * @param locationProductId the locationProductId to set
	 */
	public void setLocationProductId(String locationProductId) {
		this.locationProductId = locationProductId;
	}

	/**
	 * @param memberLocationId the memberLocationId to set
	 */
	public void setMemberLocationId(String memberLocationId) {
		this.memberLocationId = memberLocationId;
	}

	/**
	 * @param dayOfWeek the dayOfWeek to set
	 */
	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(int startTime) {
		this.startTime = startTime;
		startTimeText = LocalTime.ofSecondOfDay(startTime).toString();
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(int endTime) {
		this.endTime = endTime;
		endTimeText = LocalTime.ofSecondOfDay(endTime).toString();
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param startTimeText the startTimeText to set
	 */
	public void setStartTimeText(String startTimeText) {
		this.startTimeText = startTimeText;
		
		if (!StringUtil.isEmpty(startTimeText)) {
			startTime = LocalTime.parse(startTimeText).toSecondOfDay();
		}
	}

	/**
	 * @param endTimeText the endTimeText to set
	 */
	public void setEndTimeText(String endTimeText) {
		this.endTimeText = endTimeText;
		
		if (!StringUtil.isEmpty(endTimeText)) {
			endTime = LocalTime.parse(endTimeText).toSecondOfDay();
		}
	}

}
