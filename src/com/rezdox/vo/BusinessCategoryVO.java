package com.rezdox.vo;

import java.io.Serializable;

import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <p><b>Title</b>: BusinessCategoryVO.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jun 7, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class BusinessCategoryVO implements Serializable {

	private static final long serialVersionUID = -7323768688044815119L;
	private String categoryId;
	private String parentId;
	private String name;

	public BusinessCategoryVO() {
		super();
	}

	@Column(name="business_category_cd", isPrimaryKey=true)
	public String getCategoryId() {
		return categoryId;
	}

	@Column(name="parent_cd")
	public String getParentId() {
		return parentId;
	}

	@Column(name="category_nm")
	public String getName() {
		return name;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public void setName(String name) {
		this.name = name;
	}

}
