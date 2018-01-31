package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/*****************************************************************************
 <p><b>Title</b>: PaymentTypeVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox payment type.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 26, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_PAYMENT_TYPE")
public class PaymentTypeVO implements Serializable {
	private static final long serialVersionUID = -2320409186167844402L;

	private String paymentTypeId;
	private String typeName;
	private Date createDate;
	private Date updateDate;

	public PaymentTypeVO() {}

	/**
	 * @param req
	 */
	public PaymentTypeVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from the request
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setPaymentTypeId(req.getParameter("paymentTypeId"));
		setTypeName(req.getParameter("typeName"));
	}

	/**
	 * @return the paymentTypeId
	 */
	@Column(name="payment_type_id", isPrimaryKey=true)
	public String getPaymentTypeId() {
		return paymentTypeId;
	}

	/**
	 * @param paymentTypeId the paymentTypeId to set
	 */
	public void setPaymentTypeId(String paymentTypeId) {
		this.paymentTypeId = paymentTypeId;
	}

	/**
	 * @return the typeName
	 */
	@Column(name="type_nm")
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @param typeName the typeName to set
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}
