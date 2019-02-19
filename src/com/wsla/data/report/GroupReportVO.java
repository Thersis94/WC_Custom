package com.wsla.data.report;

import java.sql.ResultSet;

import com.siliconmtn.action.ActionRequest;
// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: GroupReportVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the Group Data Report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 31, 2019
 * @updates:
 ****************************************************************************/

public class GroupReportVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8391842568257796046L;
	
	// Members
	private String groupStatus;
	private double daysInStatus;
	private long numberTickets;

	/**
	 * 
	 */
	public GroupReportVO() {
		super();
	}
	
	/**
	 * @param req
	 */
	public GroupReportVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public GroupReportVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the groupStatus
	 */
	@Column(name="group_status_cd", isPrimaryKey=true)
	public String getGroupStatus() {
		return groupStatus;
	}

	/**
	 * @return the daysInStatus
	 */
	@Column(name="avg_days_status")
	public double getDaysInStatus() {
		return daysInStatus;
	}

	/**
	 * @return the numberTickets
	 */
	@Column(name="total_ticket_no")
	public long getNumberTickets() {
		return numberTickets;
	}

	/**
	 * @param groupStatus the groupStatus to set
	 */
	public void setGroupStatus(String groupStatus) {
		this.groupStatus = StringUtil.capitalize(groupStatus);
	}

	/**
	 * @param daysInStatus the daysInStatus to set
	 */
	public void setDaysInStatus(double daysInStatus) {
		this.daysInStatus = daysInStatus;
	}

	/**
	 * @param numberTickets the numberTickets to set
	 */
	public void setNumberTickets(long numberTickets) {
		this.numberTickets = numberTickets;
	}
}

