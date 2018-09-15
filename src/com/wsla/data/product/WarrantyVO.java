package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: WarrantyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages Warranty data for the wsla swervice offerings 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_warranty")
public class WarrantyVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4567698594237550575L;
	
	// Member Variables
	private String warrantyId;
	private String warrantyType;
	private String description;
	private int warrantyLength;
	private Date createDate;
	private Date updateDate;

	/**
	 * 
	 */
	public WarrantyVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WarrantyVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WarrantyVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the warrantyId
	 */
	@Column(name="warranty_id", isPrimaryKey=true)
	public String getWarrantyId() {
		return warrantyId;
	}

	/**
	 * @return the warrantyType
	 */
	@Column(name="warranty_type_cd")
	public String getWarrantyType() {
		return warrantyType;
	}

	/**
	 * @return the description
	 */
	@Column(name="desc_txt")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the warrantyLength
	 */
	@Column(name="warranty_days_no")
	public int getWarrantyLength() {
		return warrantyLength;
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
	 * @param warrantyId the warrantyId to set
	 */
	public void setWarrantyId(String warrantyId) {
		this.warrantyId = warrantyId;
	}

	/**
	 * @param warrantyType the warrantyType to set
	 */
	public void setWarrantyType(String warrantyType) {
		this.warrantyType = warrantyType;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param warrantyLength the warrantyLength to set
	 */
	public void setWarrantyLength(int warrantyLength) {
		this.warrantyLength = warrantyLength;
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

}

