package com.mindbody.vo.site;

/****************************************************************************
 * <b>Title:</b> MBRelationshipVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Relationship Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MBRelationshipVO {

	private int id;
	private String relationshipName1;
	private String relationshipName2;

	public MBRelationshipVO() {
		//Default Constructor
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the relationshipName1
	 */
	public String getRelationshipName1() {
		return relationshipName1;
	}

	/**
	 * @return the relationshipName2
	 */
	public String getRelationshipName2() {
		return relationshipName2;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param relationshipName1 the relationshipName1 to set.
	 */
	public void setRelationshipName1(String relationshipName1) {
		this.relationshipName1 = relationshipName1;
	}

	/**
	 * @param relationshipName2 the relationshipName2 to set.
	 */
	public void setRelationshipName2(String relationshipName2) {
		this.relationshipName2 = relationshipName2;
	}
}