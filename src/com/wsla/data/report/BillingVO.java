package com.wsla.data.report;

// JDK 1.8.x
import java.sql.ResultSet;

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
	private String phoneNumber;
	private String oem;
	private String productName;
	private long totalTickets;
	private double amount;
	private double failureRate;
	private String country = "MX";

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
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
		
		if (! StringUtil.isEmpty(phoneNumber)) {
			PhoneNumberFormat pnf = new PhoneNumberFormat(phoneNumber, country, PhoneNumberFormat.INTERNATIONAL_FORMAT);
			this.phoneNumber = pnf.getFormattedNumber();
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

}

