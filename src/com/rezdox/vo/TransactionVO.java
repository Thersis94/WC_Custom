package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/*****************************************************************************
 <p><b>Title</b>: TransactionVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox transaction.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 31, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_TRANSACTION")
public class TransactionVO implements Serializable {
	private static final long serialVersionUID = 5125601991324743874L;

	private String transactionId;
	private PaymentTypeVO paymentType;
	private String transactionCode;
	private Date createDate;

	public TransactionVO() {
		super();
		setPaymentType(new PaymentTypeVO());
	}

	/**
	 * @param req
	 */
	public TransactionVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from the request
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setTransactionId(req.getParameter("transactionId"));
		setPaymentType(new PaymentTypeVO(req));
		setTransactionCode(req.getParameter("transactionCode"));
	}

	/**
	 * @return the transactionId
	 */
	@Column(name="transaction_id", isPrimaryKey=true)
	public String getTransactionId() {
		return transactionId;
	}

	/**
	 * @param transactionId the transactionId to set
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * @return the paymentType
	 */
	public PaymentTypeVO getPaymentType() {
		return paymentType;
	}

	/**
	 * @param paymentType the paymentType to set
	 */
	@BeanSubElement
	public void setPaymentType(PaymentTypeVO paymentType) {
		this.paymentType = paymentType;
	}
	
	/**
	 * @return the paymentTypeId
	 */
	@Column(name="payment_type_id")
	public String getPaymentTypeId() {
		return paymentType.getPaymentTypeId();
	}

	/**
	 * @return the transactionCode
	 */
	@Column(name="transaction_cd")
	public String getTransactionCode() {
		return transactionCode;
	}

	/**
	 * @param transactionCode the transactionCode to set
	 */
	public void setTransactionCode(String transactionCode) {
		this.transactionCode = transactionCode;
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
}
