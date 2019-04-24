package com.restpeer.data;

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
 * <b>Description: </b> Value object for the categories
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_category")
public class CategoryVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Members
	private String categoryCode;
	private String name;
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
	 * @return the name
	 */
	@Column(name="category_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
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
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

