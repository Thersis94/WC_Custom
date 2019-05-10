package com.wsla.data.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
	private List<String> oemIds = new ArrayList<>();
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
		if (! StringUtil.isEmpty(oemId)) {
			oemIds.addAll(Arrays.asList(oemId.split("\\,")));
		}
	}

	/**
	 * Returns whether or not any OEMs were passed into the filter
	 * @return
	 */
	public boolean hasOems() {
		return ! oemIds.isEmpty();
	}
	
	/**
	 * Determines if the startDate filter has been passed
	 * @return
	 */
	public boolean hasStartDate() {
		return startDate != null;
	}

	/**
	 * Determines if the startDate filter has been passed
	 * @return
	 */
	public boolean hasEndDate() {
		return endDate != null;
	}
	
	/**
	 * Gets the ??? for the SQL statement for the oem IN clause
	 * @return
	 */
	public String getOemPSQuestions() {
		return DBUtil.preparedStatmentQuestion(oemIds.size());
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
	 * This is the original comma delimited list from the selectpicker
	 * @return the oemId
	 */
	public String getOemId() {
		return oemId;
	}

	/**
	 * @return the oemIds
	 */
	public List<String> getOemIds() {
		return oemIds;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate == null ? null : Convert.formatStartDate(startDate);
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate == null ? null : Convert.formatStartDate(endDate);
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
