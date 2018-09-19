package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
	private Date createDate;
	
	// Bean Sub-Elements
	private TicketVO ticket;
	private List<TicketDataVO> data;
	private UserVO user;
	
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
	@BeanSubElement
	public void setData(List<TicketDataVO> data) {
		this.data = data;
	}
	
	/**
	 * Adds a data element to the ledger
	 * @param ticketData
	 */
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

}

