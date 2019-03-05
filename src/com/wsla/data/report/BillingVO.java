package com.wsla.data.report;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: BillingVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for the billing report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 28, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket")
public class BillingVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2387553271546388173L;
	
	// Data Members
	private String ticketIdText;
	private String phoneNumber;
	private String formattedPhoneNumber;
	private String oem;
	private String oemId;
	private String productName;
	private String country = "MX";
	private long totalTickets;
	private int daysBeforeCas;
	private int daysInCas;
	private double amount;
	private double failureRate;
	private double avgDaysOpen;
	private int avgCreateTime;
	private Date openedDate;
	private Date closedDate;
	
	/**
	 * 
	 */
	public BillingVO() {
		super();
	}

	/**
	 * @param req
	 */
	public BillingVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public BillingVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the phoneNumber
	 */
	@Column(name="phone_number_txt")
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	/**
	 * @return the formattedPhoneNumber
	 */
	public String getFormattedPhoneNumber() {
		return formattedPhoneNumber;
	}

	/**
	 * @return the oem
	 */
	@Column(name="provider_nm")
	public String getOem() {
		return oem;
	}

	/**
	 * @return the productName
	 */
	@Column(name="product_nm")
	public String getProductName() {
		return productName;
	}

	/**
	 * @return the totalTickets
	 */
	@Column(name="total_tickets")
	public long getTotalTickets() {
		return totalTickets;
	}

	/**
	 * @return the amount
	 */
	@Column(name="amount_no")
	public double getAmount() {
		return amount;
	}

	/**
	 * @return the failureRate
	 */
	@Column(name="failure_rate_no")
	public double getFailureRate() {
		return failureRate;
	}

	/**
	 * @return the country
	 */
	@Column(name="country_cd")
	public String getCountry() {
		return country;
	}

	/**
	 * @return the avgDaysOpen
	 */
	@Column(name="avg_days_open_no")
	public double getAvgDaysOpen() {
		return avgDaysOpen;
	}

	/**
	 * @return the ticketIdText
	 */
	@Column(name="ticket_no")
	public String getTicketIdText() {
		return ticketIdText;
	}

	/**
	 * @return the daysBeforeCas
	 */
	@Column(name="days_to_cas")
	public int getDaysBeforeCas() {
		return daysBeforeCas;
	}

	/**
	 * @return the daysinCas
	 */
	@Column(name="days_in_cas")
	public int getDaysInCas() {
		return daysInCas;
	}

	/**
	 * @return the openedDate
	 */
	@Column(name="opened_dt")
	public Date getOpenedDate() {
		return openedDate;
	}

	/**
	 * @return the closedDate
	 */
	@Column(name="closed_dt")
	public Date getClosedDate() {
		return closedDate;
	}

	/**
	 * @return the oemId
	 */
	@Column(name="oem_id")
	public String getOemId() {
		return oemId;
	}

	/**
	 * @return the avgCreateTime
	 */
	@Column(name="avg_create_time_no")
	public int getAvgCreateTime() {
		return avgCreateTime;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		
		if (! StringUtil.isEmpty(phoneNumber)) {
			PhoneNumberFormat pnf = new PhoneNumberFormat(phoneNumber, country, PhoneNumberFormat.INTERNATIONAL_FORMAT);
			this.formattedPhoneNumber = pnf.getFormattedNumber();
		}
		
	}

	/**
	 * @param oem the oem to set
	 */
	public void setOem(String oem) {
		this.oem = oem;
	}

	/**
	 * @param productName the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * @param totalTickets the totalTickets to set
	 */
	public void setTotalTickets(long totalTickets) {
		this.totalTickets = totalTickets;
	}

	/**
	 * @param amount the amount to set
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}

	/**
	 * @param failureRate the failureRate to set
	 */
	public void setFailureRate(double failureRate) {
		this.failureRate = failureRate;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @param avgDaysOpen the avgDaysOpen to set
	 */
	public void setAvgDaysOpen(double avgDaysOpen) {
		this.avgDaysOpen = avgDaysOpen;
	}

	/**
	 * @param ticketIdText the ticketIdText to set
	 */
	public void setTicketIdText(String ticketIdText) {
		this.ticketIdText = ticketIdText;
	}

	/**
	 * @param daysBeforeCas the daysBeforeCas to set
	 */
	public void setDaysBeforeCas(int daysBeforeCas) {
		this.daysBeforeCas = daysBeforeCas;
	}

	/**
	 * @param daysinCas the daysinCas to set
	 */
	public void setDaysInCas(int daysInCas) {
		this.daysInCas = daysInCas;
	}

	/**
	 * @param openedDate the openedDate to set
	 */
	public void setOpenedDate(Date openedDate) {
		this.openedDate = openedDate;
	}

	/**
	 * @param closedDate the closedDate to set
	 */
	public void setClosedDate(Date closedDate) {
		this.closedDate = closedDate;
	}


	/**
	 * @param formattedPhoneNumber the formattedPhoneNumber to set
	 */
	public void setFormattedPhoneNumber(String formattedPhoneNumber) {
		this.formattedPhoneNumber = formattedPhoneNumber;
	}

	/**
	 * @param oemId the oemId to set
	 */
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}

	/**
	 * @param avgCreateTime the avgCreateTime to set
	 */
	public void setAvgCreateTime(int avgCreateTime) {
		this.avgCreateTime = avgCreateTime;
	}

}

