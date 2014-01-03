package com.fastsigns.product.keystone.vo;

import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: AccountVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 5, 2012
 ****************************************************************************/
public class AccountVO implements Serializable {
	private static final long serialVersionUID = 4359598729416725935L;
	private String accounts_id = null;
	private String display_id = null;
	private String group_id = null;
	private String parent_id = null;
	private String company_name = null;
	private Double payments = null;
	private Double total_charges = null;
	private Double credit_on_acct = null;
	private Double balance = null;
	private String rollup = null;
	private String currencyCode = "USD";
	
	
	public String getCompany_name() {
		return company_name;
	}
	public void setCompany_name(String company_name) {
		this.company_name = company_name;
	}
	public String getAccounts_id() {
		return accounts_id;
	}
	public void setAccounts_id(String accounts_id) {
		this.accounts_id = accounts_id;
	}
	public Double getTotal_charges() {
		return total_charges;
	}
	public void setTotal_charges(Double total_charges) {
		this.total_charges = total_charges;
	}
	public Double getPayments() {
		return payments;
	}
	public void setPayments(Double payments) {
		this.payments = payments;
	}
	public Double getCredit_on_acct() {
		return credit_on_acct;
	}
	public void setCredit_on_acct(Double credit_on_acct) {
		this.credit_on_acct = credit_on_acct;
	}
	public Double getBalance() {
		if (balance == null) return balance;
		if (balance < 0) return balance + (Math.abs(balance)*2);
		else return balance - (Math.abs(balance)*2);
	}
	public void setBalance(Double balance) {
		this.balance = balance;
	}
	public String getCurrencyCode() {
		return currencyCode;
	}
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}
	public String getDisplay_id() {
		return display_id;
	}
	public void setDisplay_id(String display_id) {
		this.display_id = display_id;
	}
	public String getParent_id() {
		return parent_id;
	}
	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}
	public String getGroup_id() {
		return group_id;
	}
	public void setGroup_id(String group_id) {
		this.group_id = group_id;
	}
	public String getRollup() {
		return rollup;
	}
	public void setRollup(String rollup) {
		this.rollup = rollup;
	}
	
}
