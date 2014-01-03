package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: InvoiceVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 10, 2012
 ****************************************************************************/
public class InvoiceVO implements Serializable {
	private static final long serialVersionUID = -3275369663146659330L;
	private String accounts_id = null;
	private String invoice_id = null;
	private Integer invoice_sequence = null;
	private String created_on = null;
	private Double e_price = null;
	private Double paid = null;
	private Double balance = null;
	private String parent_id = null;
	private String job_id = null;
	private Integer sequence = null;
	private int days_past_due = 0;
	private String first_name = null;
	private String last_name = null;
	private String currencyCode = "USD";
	
	
	public String getAccounts_id() {
		return accounts_id;
	}
	public void setAccounts_id(String accounts_id) {
		this.accounts_id = accounts_id;
	}
	public String getInvoice_id() {
		return invoice_id;
	}
	public void setInvoice_id(String invoice_id) {
		this.invoice_id = invoice_id;
	}
	public Integer getInvoice_sequence() {
		return invoice_sequence;
	}
	public void setInvoice_sequence(Integer invoice_sequence) {
		this.invoice_sequence = invoice_sequence;
	}
	public String getCreated_on() {
		return created_on;
	}
	public void setCreated_on(String created_on) {
		this.created_on = created_on;
	}
	public Date getCreatedOn() {
		return Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, created_on);
	}
	public Double getE_price() {
		return e_price;
	}
	public void setE_price(Double e_price) {
		this.e_price = e_price;
	}
	public Double getPaid() {
		return paid;
	}
	public void setPaid(Double paid) {
		this.paid = paid;
	}
	public Double getBalance() {
		return balance;
	}
	public void setBalance(Double balance) {
		this.balance = balance;
	}
	public String getParent_id() {
		return parent_id;
	}
	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}
	public String getJob_id() {
		return job_id;
	}
	public void setJob_id(String job_id) {
		this.job_id = job_id;
	}
	public Integer getSequence() {
		return sequence;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}
	public String getCurrencyCode() {
		return currencyCode;
	}
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}
	public int getDays_past_due() {
		return days_past_due;
	}
	public void setDays_past_due(int days_past_due) {
		this.days_past_due = days_past_due;
	}
	public String getFirst_name() {
		return first_name;
	}
	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}
	public String getLast_name() {
		return last_name;
	}
	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}
	
}
