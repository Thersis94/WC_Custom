package com.perfectstorm.data;

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
 * <b>Description: </b> Value Object for the attributes
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_attribute")
public class AttributeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1078133238271341507L;
	
	/**
	 * Enum for the attributeTypeCode.
	 */
	public enum AttributeType {
		VENUE("Venue Attribute"),
		THRESHOLD("Tour Venue Threshold");
		
		private String typeName;
		
		private AttributeType(String typeName) {
			this.typeName = typeName;
		}
		
		public String getTypeName() {
			return typeName;
		}
	}

	// Members
	private String attributeCode;
	private String name;
	private AttributeType attributeType;
	private int defaultValueNo;
	private Date createDate;
	
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
	 * @return the name
	 */
	@Column(name="attribute_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
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

	/**
	 * @return the attributeType
	 */
	@Column(name="attribute_type_cd")
	public AttributeType getAttributeType() {
		return attributeType;
	}

	/**
	 * @param attributeType the attributeType to set
	 */
	public void setAttributeType(AttributeType attributeType) {
		this.attributeType = attributeType;
	}

	/**
	 * @return the defaultValueNo
	 */
	@Column(name="default_value_no")
	public int getDefaultValueNo() {
		return defaultValueNo;
	}

	/**
	 * @param defaultValueNo the defaultValueNo to set
	 */
	public void setDefaultValueNo(int defaultValueNo) {
		this.defaultValueNo = defaultValueNo;
	}

}

