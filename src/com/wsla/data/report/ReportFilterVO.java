package com.wsla.data.report;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: ReportFilterVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> ***Change Me
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 9, 2019
 * @updates:
 ****************************************************************************/
public class ReportFilterVO extends BeanDataVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Members
	private String country;
	private String state;
	private String oemId;
	private String[] oemIds;
	private Date startDate;
	private Date endDate;

	/**
	 * 
	 */
	public ReportFilterVO() {
		super();
	}

	/**
	 * Populates the data elements
	 * @param req
	 */
	public ReportFilterVO(ActionRequest req) {
		this();
		this.populateData(req);
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return the oemId
	 */
	public String getOemId() {
		return oemId;
	}

	/**
	 * @return the oemIds
	 */
	public String[] getOemIds() {
		return oemIds;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @param oemId the oemId to set
	 */
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}

	/**
	 * @param oemIds the oemIds to set
	 */
	public void setOemIds(String[] oemIds) {
		this.oemIds = oemIds;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}
