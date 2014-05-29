package com.ram.action.data;

// JDK 1.6.x
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT BAse Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: EventVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Stores the data for an individual inventory event
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 13, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class EventVO implements Serializable {
	// Member Variables 
	protected String inventoryEventId = null;
	protected String inventoryEventGroupId = null;
	protected String auditorId = null;
	protected String inventoryLocationId = null;
	protected String comment = null;
	protected String eventName = null;
	protected Date scheduleDate = null;
	protected Date completeDate = null;
	protected Date dataLoadDate = null;
	protected List<String> customerIds = new ArrayList<String>();
	protected InventoryLocationVO location = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public EventVO() {
		
	}
	

	public EventVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	/**
	 * Assigns the data from the request object to the appropriate variables
	 * @param rs
	 */
	public EventVO(SMTServletRequest req) {
		
	}
	
	/**
	 * Assigns the data from the DB row to the appropriate variables
	 * @param req
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		inventoryEventId = db.getStringVal("inventory_event_id", rs);
		inventoryEventGroupId = db.getStringVal("inventory_event_group_id", rs);
		auditorId = db.getStringVal("auditor_id", rs);
		inventoryLocationId = db.getStringVal("inventory_location_id", rs);
		comment = db.getStringVal("comment_txt", rs);
		eventName = db.getStringVal("event_nm", rs);
		scheduleDate = db.getDateVal("schedule_dt", rs);
		completeDate = db.getDateVal("complete_dt", rs);
		dataLoadDate = db.getDateVal("data_load_dt", rs);
		location = new InventoryLocationVO(rs);
		
		//  add the customer id to the collection
		this.addCustomerId(db.getStringVal("customer_id", rs));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * @return the inventoryEventId
	 */
	public String getInventoryEventId() {
		return inventoryEventId;
	}

	/**
	 * @param inventoryEventId the inventoryEventId to set
	 */
	public void setInventoryEventId(String inventoryEventId) {
		this.inventoryEventId = inventoryEventId;
	}

	/**
	 * @return the auditorId
	 */
	public String getAuditorId() {
		return auditorId;
	}

	/**
	 * @param auditorId the auditorId to set
	 */
	public void setAuditorId(String auditorId) {
		this.auditorId = auditorId;
	}

	/**
	 * @return the inventoryLocationId
	 */
	public String getInventoryLocationId() {
		return inventoryLocationId;
	}

	/**
	 * @param inventoryLocationId the inventoryLocationId to set
	 */
	public void setInventoryLocationId(String inventoryLocationId) {
		this.inventoryLocationId = inventoryLocationId;
	}

	/**
	 * @return the comments
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the scheduleDate
	 */
	public Date getScheduleDate() {
		return scheduleDate;
	}

	/**
	 * @param scheduleDate the scheduleDate to set
	 */
	public void setScheduleDate(Date scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

	/**
	 * @return the completedDate
	 */
	public Date getCompleteDate() {
		return completeDate;
	}

	/**
	 * @param completedDate the completedDate to set
	 */
	public void setCompleteDate(Date completeDate) {
		this.completeDate = completeDate;
	}

	/**
	 * @return the dataLoadDate
	 */
	public Date getDataLoadDate() {
		return dataLoadDate;
	}

	/**
	 * @param dataLoadDate the dataLoadDate to set
	 */
	public void setDataLoadDate(Date dataLoadDate) {
		this.dataLoadDate = dataLoadDate;
	}


	/**
	 * @return the customerIds
	 */
	public List<String> getCustomerIds() {
		return customerIds;
	}


	/**
	 * @param customerIds the customerIds to set
	 */
	public void setCustomerIds(List<String> customerIds) {
		this.customerIds = customerIds;
	}

	/**
	 * Adds a customer id to the list of customer ids in the inventory event
	 * @param id
	 */
	public void addCustomerId(String id) {
		if (StringUtil.checkVal(id).length() == 0) return;
		customerIds.add(id);
	}


	/**
	 * @return the eventName
	 */
	public String getEventName() {
		return eventName;
	}


	/**
	 * @param eventName the eventName to set
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}


	/**
	 * @return the location
	 */
	public InventoryLocationVO getLocation() {
		return location;
	}


	/**
	 * @param location the location to set
	 */
	public void setLocation(InventoryLocationVO location) {
		this.location = location;
	}


	/**
	 * @return the inventoryEventGroupId
	 */
	public String getInventoryEventGroupId() {
		return inventoryEventGroupId;
	}


	/**
	 * @param inventoryEventGroupId the inventoryEventGroupId to set
	 */
	public void setInventoryEventGroupId(String inventoryEventGroupId) {
		this.inventoryEventGroupId = inventoryEventGroupId;
	}
}
