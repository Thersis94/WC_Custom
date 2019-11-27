package com.wsla.data.admin;

//JDK 1.8.x
import java.sql.ResultSet;

// SMT BAse Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;

// WC Libs
import com.wsla.data.ticket.TicketVO.ProfecoStatus;

/****************************************************************************
 * <b>Title</b>: ProfecoVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the profeco report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 7, 2019
 * @updates:
 ****************************************************************************/
public class ProfecoVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7302123367246283673L;
	
	// Members
	private String ticketId;
	private String ticketIdText;
	private ProfecoStatus profecoStatus;
	private String caseNumber;
	private String name;
	private String startDate;
	private String resolutionDate;

	/**
	 * 
	 */
	public ProfecoVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProfecoVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProfecoVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id", isPrimaryKey=true)
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the ticketTextId
	 */
	@Column(name="ticket_no")
	public String getTicketIdText() {
		return ticketIdText;
	}

	/**
	 * @return the profecoStatus
	 */
	@Column(name="profeco_status_cd")
	public ProfecoStatus getProfecoStatus() {
		return profecoStatus;
	}

	/**
	 * @return the caseNumber
	 */
	@Column(name="case_number_txt")
	public String getCaseNumber() {
		return caseNumber;
	}

	/**
	 * @return the startDate
	 */
	@Column(name="start_dt")
	public String getStartDate() {
		return startDate;
	}

	/**
	 * @return the resolutionDate
	 */
	@Column(name="resolution_dt")
	public String getResolutionDate() {
		return resolutionDate;
	}

	/**
	 * @return the name
	 */
	@Column(name="user_nm")
	public String getName() {
		return name;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param ticketTextId the ticketTextId to set
	 */
	public void setTicketIdText(String ticketIdText) {
		this.ticketIdText = ticketIdText;
	}

	/**
	 * @param profecoStatus the profecoStatus to set
	 */
	public void setProfecoStatus(ProfecoStatus profecoStatus) {
		this.profecoStatus = profecoStatus;
	}

	/**
	 * @param caseNumber the caseNumber to set
	 */
	public void setCaseNumber(String caseNumber) {
		this.caseNumber = caseNumber;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param resolutionDate the resolutionDate to set
	 */
	public void setResolutionDate(String resolutionDate) {
		this.resolutionDate = resolutionDate;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
