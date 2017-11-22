package com.mindbody.vo.classes;

/****************************************************************************
 * <b>Title:</b> MBLevelVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mindbody Level Data
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 21, 2017
 ****************************************************************************/
public class MBLevelVO {

	private int id;
	private String name;
	private String description;

	public MBLevelVO() {
		//Public Constructor
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param description the description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}