package com.mindbody.vo.classes;

/****************************************************************************
 * <b>Title:</b> MBSessionTypeVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Mindbody SessionType Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 21, 2017
 ****************************************************************************/
public class MBSessionTypeVO {

	private int defaultTimeLength;
	private int programId;
	private int numDeducted;
	private int id;
	private String name;

	public MBSessionTypeVO() {
		//Default Constructor
	}

	/**
	 * @return the defaultTimeLength
	 */
	public int getDefaultTimeLength() {
		return defaultTimeLength;
	}

	/**
	 * @return the programId
	 */
	public int getProgramId() {
		return programId;
	}

	/**
	 * @return the numDeducted
	 */
	public int getNumDeducted() {
		return numDeducted;
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
	 * @param defaultTimeLength the defaultTimeLength to set.
	 */
	public void setDefaultTimeLength(int defaultTimeLength) {
		this.defaultTimeLength = defaultTimeLength;
	}

	/**
	 * @param programId the programId to set.
	 */
	public void setProgramId(int programId) {
		this.programId = programId;
	}

	/**
	 * @param numDeducted the numDeducted to set.
	 */
	public void setNumDeducted(int numDeducted) {
		this.numDeducted = numDeducted;
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
}