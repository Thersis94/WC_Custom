package com.ram.action.data;

// JDK 1.7.x
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import com.ram.datafeed.data.InventoryEventVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
// SMT Base Libs
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: InventoryEventGroupVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>Manages the information for a group of ebvents and their recurrence
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 29, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
@Table(name="ram_inventory_event_group")
public class InventoryEventGroupVO implements Serializable {

	/**
	 * Default SVID
	 */
	private static final long serialVersionUID = 1L;
	
	// Member variables
	private String inventoryEventGroupId = null;
	private String defaultTime = null;
	private int totalWeek = 0;
	private int sundayFlag = 0;
	private int mondayFlag = 0;
	private int tuesdayFlag = 0;
	private int wednesdayFlag = 0;
	private int thursdayFlag = 0;
	private int fridayFlag = 0;
	private int saturdayFlag = 0;
	private Date createDate = null;
	private Date updateDate = null;

	private List<InventoryEventVO> events = null;
	/**
	 * Default no parameter constructor
	 */
	public InventoryEventGroupVO() {
		
	}
	
	/**
	 * Assigns req params to the appropriate variables
	 * @param req
	 */
	public InventoryEventGroupVO(SMTServletRequest req) {
		this.assignData(req);
	}
	
	/**
	 * Assigns fields from a single row in the database to the appropriate variables
	 * @param rs
	 */
	public InventoryEventGroupVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	/**
	 * Assigns req params to the appropriate variables
	 * @param req
	 */
	public void assignData(SMTServletRequest req) {
		inventoryEventGroupId = req.getParameter("inventoryEventGroupId");
		defaultTime = req.getParameter("defaultTime");
		totalWeek = Convert.formatInteger(req.getParameter("totalWeek"));
		sundayFlag = Convert.formatInteger(req.getParameter("sundayFlag"));
		mondayFlag = Convert.formatInteger(req.getParameter("mondayFlag"));
		tuesdayFlag = Convert.formatInteger(req.getParameter("tuesdayFlag"));
		wednesdayFlag = Convert.formatInteger(req.getParameter("wednesdayFlag"));
		thursdayFlag = Convert.formatInteger(req.getParameter("thursdayFlag"));
		fridayFlag = Convert.formatInteger(req.getParameter("fridayFlag"));
		saturdayFlag = Convert.formatInteger(req.getParameter("saturdayFlag"));
	}
	
	/**
	 * Assigns fields from a single row in the database to the appropriate variables
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		inventoryEventGroupId = db.getStringVal("inventory_event_group_id", rs);
		defaultTime = db.getStringVal("default_time_txt", rs);
		totalWeek = db.getIntVal("total_week_no", rs);
		sundayFlag = db.getIntVal("sunday_flg", rs);
		mondayFlag = db.getIntVal("monday_flg", rs);
		tuesdayFlag = db.getIntVal("tuesday_flg", rs);
		wednesdayFlag = db.getIntVal("wednesday_flg", rs);
		thursdayFlag = db.getIntVal("thursday_flg", rs);
		fridayFlag = db.getIntVal("friday_flg", rs);
		saturdayFlag = db.getIntVal("saturday_flg", rs);
	}

	/**
	 * @return the inventoryEventGroupId
	 */
	@Column(name="inventory_event_group_id", isPrimaryKey = true)
	public String getInventoryEventGroupId() {
		return inventoryEventGroupId;
	}

	/**
	 * @param inventoryEventGroupId the inventoryEventGroupId to set
	 */
	public void setInventoryEventGroupId(String inventoryEventGroupId) {
		this.inventoryEventGroupId = inventoryEventGroupId;
	}

	/**
	 * @return the defaultTime
	 */
	@Column(name="default_time_txt")
	public String getDefaultTime() {
		return defaultTime;
	}

	/**
	 * @param defaultTime the defaultTime to set
	 */
	public void setDefaultTime(String defaultTime) {
		this.defaultTime = defaultTime;
	}

	/**
	 * @return the totalWeek
	 */
	@Column(name="total_week_no")
	public int getTotalWeek() {
		return totalWeek;
	}

	/**
	 * @param totalWeek the totalWeek to set
	 */
	public void setTotalWeek(int totalWeek) {
		this.totalWeek = totalWeek;
	}

	/**
	 * @return the sundayFlag
	 */
	@Column(name="sunday_flg")
	public int getSundayFlag() {
		return sundayFlag;
	}

	/**
	 * @param sundayFlag the sundayFlag to set
	 */
	public void setSundayFlag(int sundayFlag) {
		this.sundayFlag = sundayFlag;
	}

	/**
	 * @return the mondayFlag
	 */
	@Column(name="monday_flg")
	public int getMondayFlag() {
		return mondayFlag;
	}

	/**
	 * @param mondayFlag the mondayFlag to set
	 */
	public void setMondayFlag(int mondayFlag) {
		this.mondayFlag = mondayFlag;
	}

	/**
	 * @return the tuesdayFlag
	 */
	@Column(name="tuesday_flg")
	public int getTuesdayFlag() {
		return tuesdayFlag;
	}

	/**
	 * @param tuesdayFlag the tuesdayFlag to set
	 */
	public void setTuesdayFlag(int tuesdayFlag) {
		this.tuesdayFlag = tuesdayFlag;
	}

	/**
	 * @return the wednesdayFlag
	 */
	@Column(name="wednesday_flg")
	public int getWednesdayFlag() {
		return wednesdayFlag;
	}

	/**
	 * @param wednesdayFlag the wednesdayFlag to set
	 */
	public void setWednesdayFlag(int wednesdayFlag) {
		this.wednesdayFlag = wednesdayFlag;
	}

	/**
	 * @return the thursdayFlag
	 */
	@Column(name="thursday_flg")
	public int getThursdayFlag() {
		return thursdayFlag;
	}

	/**
	 * @param thursdayFlag the thursdayFlag to set
	 */
	public void setThursdayFlag(int thursdayFlag) {
		this.thursdayFlag = thursdayFlag;
	}

	/**
	 * @return the fridayFlag
	 */
	@Column(name="friday_flg")
	public int getFridayFlag() {
		return fridayFlag;
	}

	/**
	 * @param fridayFlag the fridayFlag to set
	 */
	public void setFridayFlag(int fridayFlag) {
		this.fridayFlag = fridayFlag;
	}

	/**
	 * @return the saturdayFlag
	 */
	@Column(name="saturday_flg")
	public int getSaturdayFlag() {
		return saturdayFlag;
	}

	/**
	 * @param saturdayFlag the saturdayFlag to set
	 */
	public void setSaturdayFlag(int saturdayFlag) {
		this.saturdayFlag = saturdayFlag;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt")
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt")
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	
	/**
	 * counts how many events we need to create in this series
	 * @return
	 */
	public int getRecurrenceCount() {
		return (sundayFlag + mondayFlag + tuesdayFlag + wednesdayFlag + thursdayFlag + fridayFlag + saturdayFlag) * totalWeek;
	}

	/**
	 * @return the events
	 */
	public List<InventoryEventVO> getEvents() {
		return events;
	}

	/**
	 * @param events the events to set
	 */
	public void setEvents(List<InventoryEventVO> events) {
		this.events = events;
	}

}
