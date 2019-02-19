package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.TicketVO.UnitLocation;

/****************************************************************************
 * <b>Title</b>: TicketLedgerVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the ticket timeline data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 14, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_ledger")
public class TicketLedgerVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2368041673523793932L;

	// Member Variables
	private String ledgerEntryId;
	private String dispositionBy;
	private String ticketId;
	private String summary;
	private StatusCode statusCode;
	private UnitLocation unitLocation;
	private String billableActivityCode;
	private Double billableAmtNo;
	private Date createDate;
	
	// Bean Sub-Elements
	private TicketVO ticket;
	private List<TicketDataVO> data = new ArrayList<>();
	private UserVO user;
	private StatusCodeVO status;
	
	/**
	 * 
	 */
	public TicketLedgerVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketLedgerVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketLedgerVO(ResultSet rs) {
		super(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#populateData(java.sql.ResultSet)
	 */
	@Override
	public void populateData(ResultSet rs) {
		super.populateData(rs);
		
		if (user == null && ! StringUtil.isEmpty(dispositionBy)) {
			user = new UserVO(rs);
		}
	}
	/**
	 * @return the ledgerEntryId
	 */
	@Column(name="ledger_entry_id", isPrimaryKey=true)
	public String getLedgerEntryId() {
		return ledgerEntryId;
	}

	/**
	 * @return the dispositionBy
	 */
	@Column(name="disposition_by_id")
	public String getDispositionBy() {
		return dispositionBy;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the summary
	 */
	@Column(name="summary_txt")
	public String getSummary() {
		return summary;
	}

	/**
	 * @return the statusCode
	 */
	@Column(name="status_cd")
	public StatusCode getStatusCode() {
		return statusCode;
	}

	/**
	 * @return the unitLocation
	 */
	@Column(name="unit_location_cd")
	public UnitLocation getUnitLocation() {
		return unitLocation;
	}

	/**
	 * @return the billableActivityCode
	 */
	@Column(name="billable_activity_cd")
	public String getBillableActivityCode() {
		return billableActivityCode;
	}

	/**
	 * @return the billableAmtNo
	 */
	@Column(name="billable_amt_no")
	public Double getBillableAmtNo() {
		return billableAmtNo;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the ticket
	 */
	public TicketVO getTicket() {
		return ticket;
	}

	/**
	 * @return the data
	 */
	public List<TicketDataVO> getData() {
		return data;
	}

	/**
	 * @return the user
	 */
	public UserVO getUser() {
		return user;
	}

	/**
	 * @return the status
	 */
	public StatusCodeVO getStatus() {
		return status;
	}

	/**
	 * @param ledgerEntryId the ledgerEntryId to set
	 */
	public void setLedgerEntryId(String ledgerEntryId) {
		this.ledgerEntryId = ledgerEntryId;
	}

	/**
	 * @param dispositionBy the dispositionBy to set
	 */
	public void setDispositionBy(String dispositionBy) {
		this.dispositionBy = dispositionBy;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param summary the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param ticket the ticket to set
	 */
	@BeanSubElement
	public void setTicket(TicketVO ticket) {
		this.ticket = ticket;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(List<TicketDataVO> data) {
		this.data = data;
	}
	
	/**
	 * Adds a data element to the ledger
	 * @param ticketData
	 */
	@BeanSubElement
	public void addData(TicketDataVO ticketData) {
		if (ticketData != null)
			data.add(ticketData);
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(UserVO user) {
		this.user = user;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @param unitLocation the unitLocation to set
	 */
	public void setUnitLocation(UnitLocation unitLocation) {
		this.unitLocation = unitLocation;
	}

	/**
	 * @param billableActivityCode the billableActivityCode to set
	 */
	public void setBillableActivityCode(String billableActivityCode) {
		this.billableActivityCode = billableActivityCode;
	}

	/**
	 * @param billableAmtNo the billableAmtNo to set
	 */
	public void setBillableAmtNo(Double billableAmtNo) {
		this.billableAmtNo = billableAmtNo;
	}

	/**
	 * @param status the status to set
	 */
	@BeanSubElement
	public void setStatus(StatusCodeVO status) {
		this.status = status;
	}

}

