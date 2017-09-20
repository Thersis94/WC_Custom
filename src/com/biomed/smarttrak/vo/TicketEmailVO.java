package com.biomed.smarttrak.vo;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.smt.sitebuilder.action.support.TicketVO;

/****************************************************************************
 * <b>Title</b>: ticketEmailVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TicketVO with extra attributes for building an email.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 2, 2017
 ****************************************************************************/
public class TicketEmailVO extends TicketVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 3525599829004297267L;
	private String companyId;
	private String phoneNo;
	private String companyNm;
	private String countryCd;
	private String ticketLink;

	public TicketEmailVO() {
		super();
	}

	/**
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the phoneNo
	 */
	@Column(name="phone_number_txt")
	public String getPhoneNo() {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		pnf.setCountryCode(countryCd);
		pnf.setPhoneNumber(phoneNo);
		return phoneNo;
	}

	/**
	 * @return the companyNm
	 */
	@Column(name="company_nm")
	public String getCompanyNm() {
		return companyNm;
	}

	/**
	 * @return the countryCd
	 */
	public String getCountryCd() {
		return countryCd;
	}

	/**
	 * @return the ticketLink
	 */
	public String getTicketLink() {
		return ticketLink;
	}

	public String getCreateDtFmt() {
		return Convert.formatDate(getCreateDt(), Convert.DATE_TIME_DASH_PATTERN_12HR);
	}
	/**
	 * @param companyId the companyId to set.
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param phoneNo the phoneNo to set.
	 */
	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	/**
	 * @param companyNm the companyNm to set.
	 */
	public void setCompanyNm(String companyNm) {
		this.companyNm = companyNm;
	}

	/**
	 * @param countryCd the countryCd to set.
	 */
	public void setCountryCd(String countryCd) {
		this.countryCd = countryCd;
	}

	/**
	 * @param ticketLink the ticketLink to set.
	 */
	public void setTicketLink(String ticketLink) {
		this.ticketLink = ticketLink;
	}
}