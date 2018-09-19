package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TicketDataVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object storing one data element for a given ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 14, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_data")
public class TicketDataVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Vars
	private String dataEntryId;
	private String ledgerEntryId;
	private String ticketId;
	private String attributeCode;
	private String value;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-elements
	private TicketAttributeVO attribute;
	
	/**
	 * 
	 */
	public TicketDataVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketDataVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketDataVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the dataEntryId
	 */
	@Column(name="data_entry_id", isPrimaryKey=true)
	public String getDataEntryId() {
		return dataEntryId;
	}

	/**
	 * @return the ledgerEntryId
	 */
	@Column(name="ledger_entry_id")
	public String getLedgerEntryId() {
		return ledgerEntryId;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the attributeCode
	 */
	@Column(name="attribute_cd")
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the attribute
	 */
	@BeanSubElement
	public TicketAttributeVO getAttribute() {
		return attribute;
	}

	/**
	 * @param dataEntryId the dataEntryId to set
	 */
	public void setDataEntryId(String dataEntryId) {
		this.dataEntryId = dataEntryId;
	}

	/**
	 * @param ledgerEntryId the ledgerEntryId to set
	 */
	public void setLedgerEntryId(String ledgerEntryId) {
		this.ledgerEntryId = ledgerEntryId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param attribute the attribute to set
	 */
	public void setAttribute(TicketAttributeVO attribute) {
		this.attribute = attribute;
	}

}

