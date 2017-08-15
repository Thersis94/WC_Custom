package com.ram.action.or.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>RAMSurgeonCustomerVO.java<br/>
 * <b>Description: </b>Desc Here<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Aug 15, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ram_surgeon_customer_xr")
public class RAMSurgeonCustomerVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4728213457959688051L;

	// Member variables
	private String surgeonCustomerId;
	private String surgeonId;
	private int customerId;
	private Date createDate;
	
	/**
	 * 
	 */
	public RAMSurgeonCustomerVO() {
		super();
	}
	
	/**
	 * 
	 * @param req
	 */
	public RAMSurgeonCustomerVO(ActionRequest req) {
		super();
		populateData(req);
	}

	/**
	 * 
	 * @param rs
	 */
	public RAMSurgeonCustomerVO(ResultSet rs) {
		super();
		populateData(rs);
	}
	
	/**
	 * Helper constructor
	 * @param id
	 * @param surgeonId
	 * @param customerId
	 */
	public RAMSurgeonCustomerVO(String id, String surgeonId, int customerId) {
		this.surgeonCustomerId = id;
		this.surgeonId = surgeonId;
		this.customerId = customerId;
	}

	/**
	 * @return the surgeonCustomerId
	 */
	@Column(name="surgeon_customer_id", isPrimaryKey=true)
	public String getSurgeonCustomerId() {
		return surgeonCustomerId;
	}

	/**
	 * @return the surgeonId
	 */
	@Column(name="surgeon_id")
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @return the customerId
	 */
	@Column(name="customer_id")
	public int getCustomerId() {
		return customerId;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt")
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param surgeonCustomerId the surgeonCustomerId to set
	 */
	public void setSurgeonCustomerId(String surgeonCustomerId) {
		this.surgeonCustomerId = surgeonCustomerId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
