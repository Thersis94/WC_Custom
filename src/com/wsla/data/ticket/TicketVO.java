package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <b>Title</b>: TicketVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the ticket data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 13, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket")
public class TicketVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -288262467687670031L;
	
	// Member Variables
	private String ticketId;
	private String ticketIdText;
	private String ticketName;
	private String description;
	private String warrantyProductId;
	private StatusCode statusCode;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-Element
	private List<TicketDataVO> data = new ArrayList<>(32);
	
	/**
	 * 
	 */
	public TicketVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketVO(ResultSet rs) {
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
	 * @return the ticketIdText
	 */
	@Column(name="ticket_no")
	public String getTicketIdText() {
		return ticketIdText;
	}

	/**
	 * @return the ticketName
	 */
	@Column(name="ticket_nm")
	public String getTicketName() {
		return ticketName;
	}

	/**
	 * @return the description
	 */
	@Column(name="desc_txt")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the warrantyProductId
	 */
	@Column(name="warranty_product_id")
	public String getWarrantyProductId() {
		return warrantyProductId;
	}

	/**
	 * @return the statusCode
	 */
	@Column(name="status_cd")
	public StatusCode getStatusCode() {
		return statusCode;
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
	 * @return the data
	 */
	public List<TicketDataVO> getData() {
		return data;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param ticketIdText the ticketIdText to set
	 */
	public void setTicketIdText(String ticketIdText) {
		this.ticketIdText = ticketIdText;
	}

	/**
	 * @param ticketName the ticketName to set
	 */
	public void setTicketName(String ticketName) {
		this.ticketName = ticketName;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param warrantyProductId the warrantyProductId to set
	 */
	public void setWarrantyProductId(String warrantyProductId) {
		this.warrantyProductId = warrantyProductId;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String strStatusCode) {
		this.statusCode = StatusCode.valueOf(strStatusCode);
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
	 * @param data the data to set
	 */
	@BeanSubElement
	public void setData(List<TicketDataVO> data) {
		this.data = data;
	}

	/**
	 * @param data the data to set
	 */
	public void addData(TicketDataVO ticketData) {
		if (ticketData != null)
			this.data.add(ticketData);
	}
}

