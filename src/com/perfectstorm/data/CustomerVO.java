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
 * <b>Title</b>: CustomerVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the customer data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 10, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_customer")
public class CustomerVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6581210891929304935L;
	
	/**
	 * Enum for the customerTypeCode.
	 */
	public enum CustomerType {
		TOUR("Tour Customer"),
		VENUE("Venue Customer");
		
		private String customerName;
		private CustomerType(String customerName) { this.customerName = customerName; }
		public String getCustomerName() { return customerName; }
	}
	
	// Members
	private String customerId;
	private String customerName;
	private CustomerType typeCode;
	private Date updateDate;
	private Date createDate;

	/**
	 * 
	 */
	public CustomerVO() {
		super();
	}

	/**
	 * @param req
	 */
	public CustomerVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public CustomerVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the customerId
	 */
	@Column(name="customer_id", isPrimaryKey=true)
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @return the customerName
	 */
	@Column(name="customer_nm")
	public String getCustomerName() {
		return customerName;
	}

	/**
	 * @return the typeCode
	 */
	@Column(name="customer_type_cd")
	public CustomerType getTypeCode() {
		return typeCode;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * @param customerName the customerName to set
	 */
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	/**
	 * @param typeCode the typeCode to set
	 */
	public void setTypeCode(CustomerType typeCode) {
		this.typeCode = typeCode;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
