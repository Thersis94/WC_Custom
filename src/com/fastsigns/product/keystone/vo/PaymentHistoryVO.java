package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: PaymentHistoryVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 3, 2012
 ****************************************************************************/
public class PaymentHistoryVO implements Serializable {

	private static final long serialVersionUID = 1706812998841387809L;
	
	private String payment_id = null;
	private String franchise_id = null;
	private String accounts_id = null;
	private String payment_method_id = null;
	private String payment_method_name = null;
	private String payment_method_type_id = null;
	private String payment_method_type_name = null;
	private Double amount = 0.0;
	private String payment_date = null;
	private String payments_status_id = null;
	private String created_on = null;
	private String created_by = null;
	private String invoice_number = null;
	private String job_number = null;
	private String status = null;
	private String reference = null;
	private String currencyCode = "USD";
	
	public String getPayment_id() {
		return payment_id;
	}
	public void setPayment_id(String payment_id) {
		this.payment_id = payment_id;
	}
	public String getFranchise_id() {
		return franchise_id;
	}
	public void setFranchise_id(String franchise_id) {
		this.franchise_id = franchise_id;
	}
	public String getAccounts_id() {
		return accounts_id;
	}
	public void setAccounts_id(String accounts_id) {
		this.accounts_id = accounts_id;
	}
	public String getPayment_method_id() {
		return payment_method_id;
	}
	public void setPayment_method_id(String payment_method_id) {
		this.payment_method_id = payment_method_id;
	}
	public String getPayment_method_type_id() {
		return payment_method_type_id;
	}
	public void setPayment_method_type_id(String payment_method_type_id) {
		this.payment_method_type_id = payment_method_type_id;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public String getPayment_date() {
		return payment_date;
	}
	public void setPayment_date(String payment_date) {
		this.payment_date = payment_date;
	}
	public Date getPaymentDate(){
		return Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, payment_date);
	}
	public String getPayments_status_id() {
		return payments_status_id;
	}
	public void setPayments_status_id(String payments_status_id) {
		this.payments_status_id = payments_status_id;
	}
	public String getCreated_on() {
		return created_on;
	}
	public void setCreated_on(String created_on) {
		this.created_on = created_on;
	}
	public Date getCreatedOn(){
		return Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, created_on);
	}
	public String getCreated_by() {
		return created_by;
	}
	public void setCreated_by(String created_by) {
		this.created_by = created_by;
	}
	public String getInvoice_number() {
		return invoice_number;
	}
	public void setInvoice_number(String invoice_number) {
		this.invoice_number = invoice_number;
	}
	public String getJob_number() {
		return job_number;
	}
	public void setJob_number(String job_number) {
		this.job_number = job_number;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getPayment_method_name() {
		return payment_method_name;
	}
	public void setPayment_method_name(String payment_method_name) {
		this.payment_method_name = payment_method_name;
	}
	public String getPayment_method_type_name() {
		return payment_method_type_name;
	}
	public void setPayment_method_type_name(String payment_method_type_name) {
		this.payment_method_type_name = payment_method_type_name;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getCurrencyCode() {
		return currencyCode;
	}
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}


}
