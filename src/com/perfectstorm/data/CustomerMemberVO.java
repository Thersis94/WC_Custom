package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;

import com.perfectstorm.data.CustomerVO.CustomerType;
// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CustomerMemberVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the customer member data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 10, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_customer_member_xr")
public class CustomerMemberVO extends MemberVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6581210891929304935L;
	
	// Members
	private String customerMemberId;
	private String customerId;
	private int defaultFlag;
	
	// Helpers
	private String customerName;
	private CustomerType typeCode;

	/**
	 * 
	 */
	public CustomerMemberVO() {
		super();
	}

	/**
	 * @param req
	 */
	public CustomerMemberVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public CustomerMemberVO(ResultSet rs) {
		super(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the customerMemberId
	 */
	@Column(name="customer_member_id", isPrimaryKey=true)
	public String getCustomerMemberId() {
		return customerMemberId;
	}

	/**
	 * @return the customerId
	 */
	@Column(name="customer_id")
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @return the defaultFlag
	 */
	@Column(name="default_flg")
	public int getDefaultFlag() {
		return defaultFlag;
	}

	/**
	 * @param customerMemberId the customerMemberId to set
	 */
	public void setCustomerMemberId(String customerMemberId) {
		this.customerMemberId = customerMemberId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * @param defaultFlag the defaultFlag to set
	 */
	public void setDefaultFlag(int defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * @return the customerName
	 */
	@Column(name="customer_nm", isReadOnly=true)
	public String getCustomerName() {
		return customerName;
	}

	/**
	 * @param customerName the customerName to set
	 */
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	/**
	 * @return the typeCode
	 */
	@Column(name="customer_type_cd", isReadOnly=true)
	public CustomerType getTypeCode() {
		return typeCode;
	}

	/**
	 * @param typeCode the typeCode to set
	 */
	public void setTypeCode(CustomerType typeCode) {
		this.typeCode = typeCode;
	}
}
