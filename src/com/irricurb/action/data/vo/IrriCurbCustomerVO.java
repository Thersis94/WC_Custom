package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

//SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>IrriCurbCustomerVO.java<br/>
 * <b>Description: </b>Data Bean for the customer data<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_customer")
public class IrriCurbCustomerVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4435778339032923964L;

	// Member Variables 
	private String customerId;
	private String name;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public IrriCurbCustomerVO() {
		super();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public IrriCurbCustomerVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 * @param req
	 */
	public IrriCurbCustomerVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the deviceManufacturerId
	 */
	@Column(name="customer_id", isPrimaryKey=true)
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @return the manufacturerName
	 */
	@Column(name="customer_nm")
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
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param deviceManufacturerId the deviceTypeCode to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * @param manufacturerName the deviceName to set
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
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}
