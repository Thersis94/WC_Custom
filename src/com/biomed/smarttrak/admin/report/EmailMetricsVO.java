package com.biomed.smarttrak.admin.report;

/*****************************************************************************
<p><b>Title</b>: EmailMetricsVO.java</p>
<p><b>Description: Store data related to email metrics.</b></p>
<p> 
<p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Eric Damschroder
@version 1.0
@since Mar 23, 2018
<b>Changes:</b> 
***************************************************************************/

public class EmailMetricsVO {
	private String accountName;
	private String campaignName;
	private String emailAddress;
	private String notesText;
	private int opens;
	private int fails;
	private int total;
	
	public String getAccountName() {
		return accountName;
	}
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	public String getCampaignName() {
		return campaignName;
	}
	public void setCampaignName(String campaignName) {
		this.campaignName = campaignName;
	}
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	public String getNotesText() {
		return notesText;
	}
	public void setNotesText(String notesText) {
		this.notesText = notesText;
	}
	public int getOpens() {
		return opens;
	}
	public void setOpens(int opens) {
		this.opens = opens;
	}
	public int getFails() {
		return fails;
	}
	public void setFails(int fails) {
		this.fails = fails;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public void addToTotal(int add) {
		total += add;
	}

}
