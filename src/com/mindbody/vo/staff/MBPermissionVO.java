package com.mindbody.vo.staff;

/****************************************************************************
 * <b>Title:</b> MBPermissionVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manges MindBody Permission Data
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MBPermissionVO {

	private String displayName;
	private String name;
	private String value;
	public MBPermissionVO() {
		//Default constructor
	}
	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param displayName the displayName to set.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param value the value to set.
	 */
	public void setValue(String value) {
		this.value = value;
	}
}