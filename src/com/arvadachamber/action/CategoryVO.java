package com.arvadachamber.action;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: CategoryVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 22, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CategoryVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private Integer memberId = null;
	private Integer parentId = null;
	private String categoryName = null;
	private Integer categoryId = null;
	private Integer primaryFlag = null;

	/**
	 * 
	 * @param categoryId
	 * @param memberId
	 */
	public CategoryVO(int categoryId, int memberId, boolean primary) {
		this.categoryId = categoryId;
		this.memberId = memberId;
		if (primary) primaryFlag = 1;
	}
	
	/**
	 * 
	 */
	public CategoryVO() {
		
	}
	
	/**
	 * 
	 * @param rs
	 */
	public CategoryVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	/**
	 * Assigns the DB data o the params
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		memberId = db.getIntegerVal("member_id", rs);
		parentId = db.getIntegerVal("parent_id", rs);
		primaryFlag = db.getIntegerVal("primary_flg", rs);
		categoryName = db.getStringVal("category_nm", rs);
		categoryId = db.getIntegerVal("category_id", rs);
	}

	/**
	 * @return the memberId
	 */
	public Integer getMemberId() {
		return memberId;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(Integer memberId) {
		this.memberId = memberId;
	}

	/**
	 * @return the parentId
	 */
	public Integer getParentId() {
		return parentId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	/**
	 * @return the categoryName
	 */
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @param categoryName the categoryName to set
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * @return the primaryFlag
	 */
	public Integer getPrimaryFlag() {
		return primaryFlag;
	}

	/**
	 * @param primaryFlag the primaryFlag to set
	 */
	public void setPrimaryFlag(Integer primaryFlag) {
		this.primaryFlag = primaryFlag;
	}

	/**
	 * @return the categoryId
	 */
	public Integer getCategoryId() {
		return categoryId;
	}

	/**
	 * @param categoryId the categoryId to set
	 */
	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}

}
