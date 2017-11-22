package com.mindbody.vo.classes;

import com.mindbodyonline.clients.api._0_5_1.ScheduleType;

/****************************************************************************
 * <b>Title:</b> MBProgramVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mindbody Program Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 21, 2017
 ****************************************************************************/
public class MBProgramVO {

	private int id;
	private int cancelOffset;
	private String name;
	private ScheduleType.Enum scheduleType;

	public MBProgramVO() {
		//Default Constructor
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the cancelOffset
	 */
	public int getCancelOffset() {
		return cancelOffset;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the scheduleType
	 */
	public ScheduleType.Enum getScheduleType() {
		return scheduleType;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param cancelOffset the cancelOffset to set.
	 */
	public void setCancelOffset(int cancelOffset) {
		this.cancelOffset = cancelOffset;
	}

	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param scheduleType the scheduleType to set.
	 */
	public void setScheduleType(ScheduleType.Enum scheduleType) {
		this.scheduleType = scheduleType;
	}
}