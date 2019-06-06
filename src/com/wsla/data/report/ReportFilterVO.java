package com.wsla.data.report;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ReportFilterVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Processes data for the filter parameters on the reports
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
	
	// Helpers
	private Connection conn;

	/**
	 * Populates the data elements
	 * @param req
	 */
	public ReportFilterVO(ActionRequest req, Connection conn) {
		super();
		this.conn = conn;
		this.populateData(req);
		
		// Convert the comma delimited list of oems to a collection
		if (! StringUtil.isEmpty(oemId)) {
			oemIds.addAll(Arrays.asList(oemId.split("\\,")));
		}
				
		// Make sure the country s a 2 digit code and not 3
		if (StringUtil.checkVal(state).length() == 3) get2CharState();
	}
	
	/**
	 * Gets the 2 character state code from the 3 character ISO code
	 * @return
	 */
	protected String get2CharState() {
		String s = "select state_cd from state where iso_state_cd = ?";
		
		try (PreparedStatement ps = conn.prepareStatement(s)) {
			ps.setString(1, state);
			
			try(ResultSet rs = ps.executeQuery()) {
				if (rs.next()) state = rs.getString(1);
			}
		} catch (Exception e) {
			log.error("Unable to retrieve 2 character country code", e);
		}
		
		return country;
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
