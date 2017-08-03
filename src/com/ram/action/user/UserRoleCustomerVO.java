package com.ram.action.user;

// JDK 1.8.x
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>UserRoleAttributeVO.java<br/>
 * <b>Description: </b>VO for the cross ref between ram user roles and customers<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jul 31, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ram_user_role_customer_xr")
public class UserRoleCustomerVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String userRoleCustomerId;
	private int customerId;
	private int userRoleId;
	private Date createDate;

	/**
	 * 
	 */
	public UserRoleCustomerVO() {
		super();
	}

	/**
	 * @return the userRoleCustomerId
	 */
	@Column(name="user_role_customer_id", isPrimaryKey=true)
	public String getUserRoleCustomerId() {
		return userRoleCustomerId;
	}

	/**
	 * @param userRoleCustomerId the userRoleCustomerId to set
	 */
	public void setUserRoleCustomerId(String userRoleCustomerId) {
		this.userRoleCustomerId = userRoleCustomerId;
	}

	/**
	 * @return the customerId
	 */
	@Column(name="customer_id")
	public int getCustomerId() {
		return customerId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	/**
	 * @return the userRoleId
	 */
	@Column(name="user_role_id")
	public int getUserRoleId() {
		return userRoleId;
	}

	/**
	 * @param userRoleId the userRoleId to set
	 */
	public void setUserRoleId(int userRoleId) {
		this.userRoleId = userRoleId;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt")
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
