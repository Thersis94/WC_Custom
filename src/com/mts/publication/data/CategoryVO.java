package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: CategoryVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Category Value Object
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 1, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_category")
public class CategoryVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7890441210170820836L;
	
	// Members
	private String categoryCode;
	private String parentCode;
	private String groupCode;
	private String name;
	private String slug;
	private String description;
	private Date createDate;

	/**
	 * 
	 */
	public CategoryVO() {
		super();
	}

	/**
	 * @param req
	 */
	public CategoryVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public CategoryVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the categoryCode
	 */
	@Column(name="category_cd", isPrimaryKey=true)
	public String getCategoryCode() {
		return categoryCode;
	}

	/**
	 * @return the parentCode
	 */
	@Column(name="parent_cd")
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * @return the groupCode
	 */
	@Column(name="group_cd")
	public String getGroupCode() {
		return groupCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="category_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the slug
	 */
	@Column(name="slug_txt")
	public String getSlug() {
		return slug;
	}

	/**
	 * @return the description
	 */
	@Column(name="category_desc")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param categoryCode the categoryCode to set
	 */
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	/**
	 * @param parentCode the parentCode to set
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * @param groupCode the groupCode to set
	 */
	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param slug the slug to set
	 */
	public void setSlug(String slug) {
		this.slug = slug;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
