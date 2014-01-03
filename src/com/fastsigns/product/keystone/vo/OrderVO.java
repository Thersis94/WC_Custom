package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: FIIOrderVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 1, 2012
 ****************************************************************************/
public class OrderVO implements Serializable {
	private static final long serialVersionUID = 4359597807542645935L;
	private String job_id = null;
	private String sequence = null;
	private String created_on = null;
	private String description = null;
	private String status = null;
	private Double amount = 0.0;
	private String currencyCode = "USD";
	
	public OrderVO() {
	}

	public String getJob_id() {
		return job_id;
	}

	public void setJob_id(String job_id) {
		this.job_id = job_id;
	}

	public String getCreated_on() {
		return created_on;
	}
	
	public Date getCreatedOn() {
		return Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, created_on);
	}

	public void setCreated_on(String created_on) {
		this.created_on = created_on;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
}
