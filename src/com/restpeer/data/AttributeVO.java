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
 * <b>Title</b>: AttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Stores the attributes
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_attribute")
public class AttributeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5465654450192747042L;
	
	public enum GroupCode {
		ADD_ONS("Facility Add Ons"),
		BUSINESS("Business Data"),
		KITCHEN_INFO("Kitchen Info");
		
		private String codeName;
		private GroupCode(String codeName) { this.codeName = codeName; }
		public String getCodeName() { return codeName; }
	}
	
	// Members
	private String attributeCode;
	private String groupCode;
	private String name;
	private int activeFlag;
	private int orderNumber;
	private Date createDate;
	private Date updateDate;

	// Helpers
	private String categoryName;
	
	/**
	 * 
	 */
	public AttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public AttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public AttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the attributeCode
	 */
	@Column(name="attribute_cd", isPrimaryKey=true)
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @return the categoryCode
	 */
	@Column(name="group_cd")
	public String getGroupCode() {
		return groupCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="attribute_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the orderNumber
	 */
	@Column(name="order_no")
	public int getOrderNumber() {
		return orderNumber;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the categoryName
	 */
	@Column(name="category_nm", isReadOnly=true)
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @param categoryCode the categoryCode to set
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
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param orderNumber the orderNumber to set
	 */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param categoryName the categoryName to set
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

}

