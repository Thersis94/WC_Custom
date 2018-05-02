package com.irricurb.io;

// JDK 1.8.x
import java.io.Serializable;

// SMT Base Libs
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SensorVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean managing the sensor information
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Dec 22, 2017
 * @updates:
 ****************************************************************************/
public class SensorVO implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8727678687523133373L;
	
	// Members
	private String id;
	private String type;
	private int value;
	private long updateTime;

	/**
	 * 
	 */
	public SensorVO() {
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @return the updateTime
	 */
	public long getUpdateTime() {
		return updateTime;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	/**
	 * @param updateTime the updateTime to set
	 */
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

}
