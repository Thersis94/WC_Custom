package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;

// WSLA Libs
import com.wsla.common.WSLAConstants;

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
	private String productCategoryId;
	private StatusCode statusCode;
	private int warrantyValidFlag;
	private Date purchaseDate;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-Element
	private List<TicketDataVO> ticketData = new ArrayList<>(32);
	private List<TicketAssignmentVO> assignments = new ArrayList<>();
	private List<TicketLedgerVO> timeline = new ArrayList<>();
	
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
		setAttributesFromReq(req, null);
	}
	
	/**
	 * @param req
	 */
	public TicketVO(ActionRequest req, TicketLedgerVO ledger) {
		super(req);
		setAttributesFromReq(req, ledger);
	}

	/**
	 * @param rs
	 */
	public TicketVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * Assigns any request parameters with the appropriate attribute prefix
	 * to the ticket attribute collection.  Note, on new ticket create, the ticketId 
	 * will not be assigned as it does not exist in the req object.  Needs to be manually
	 * added before the info is saved
	 * @param req
	 * @param ledger
	 */
	protected void setAttributesFromReq(ActionRequest req, TicketLedgerVO ledger) {
		List<String> names = Collections.list(req.getParameterNames());
		for(String name : names) {
			if (StringUtil.checkVal(name).startsWith(WSLAConstants.ATTRIBUTE_PREFIX)) {
				TicketDataVO data = new TicketDataVO();
				data.setTicketId(getTicketId());
				data.setAttributeCode(name);
				data.setValue(req.getParameter(name));
				if (ledger != null) data.setLedgerEntryId(ledger.getLedgerEntryId());
				
				addTicketData(data);
			}
		}
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
	public List<TicketDataVO> getTicketData() {
		return ticketData;
	}

	/**
	 * @return the warrantyValidFlag
	 */
	@Column(name="warranty_valid_flg")
	public int getWarrantyValidFlag() {
		return warrantyValidFlag;
	}

	/**
	 * @return the purchase_dt
	 */
	@Column(name="purchase_dt")
	public Date getPurchaseDate() {
		return purchaseDate;
	}

	/**
	 * @return the productCategoryId
	 */
	@Column(name="product_category_id")
	public String getProductCategoryId() {
		return productCategoryId;
	}

	/**
	 * @return the timeline
	 */
	public List<TicketLedgerVO> getTimeline() {
		return timeline;
	}

	/**
	 * @return the assignments
	 */
	public List<TicketAssignmentVO> getAssignments() {
		return assignments;
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
	public void setTicketData(List<TicketDataVO> ticketData) {
		this.ticketData = ticketData;
	}

	/**
	 * @param data the data to set
	 */
	@BeanSubElement
	public void addTicketData(TicketDataVO data) {
		if (ticketData != null)
			this.ticketData.add(data);
	}

	/**
	 * @param assignments the assignments to set
	 */
	public void setAssignments(List<TicketAssignmentVO> assignments) {
		this.assignments = assignments;
	}
	
	/**
	 * 
	 * @param assignment
	 */
	@BeanSubElement
	public void addAssignment(TicketAssignmentVO assignment) {
		this.assignments.add(assignment);
	}

	/**
	 * @param timeline the timeline to set
	 */
	@BeanSubElement
	public void setTimeline(List<TicketLedgerVO> timeline) {
		this.timeline = timeline;
	}
	
	/**
	 * 
	 * @param entry
	 */
	public void addTimeline(TicketLedgerVO entry) {
		this.timeline.add(entry);
	}

	/**
	 * @param warrantyValidFlag the warrantyValidFlag to set
	 */
	public void setWarrantyValidFlag(int warrantyValidFlag) {
		this.warrantyValidFlag = warrantyValidFlag;
	}

	/**
	 * @param productCategoryId the productCategoryId to set
	 */
	public void setProductCategoryId(String productCategoryId) {
		this.productCategoryId = productCategoryId;
	}

	/**
	 * @param purchaseDate the purchaseDate to set
	 */
	public void setPurchaseDate(Date purchaseDate) {
		this.purchaseDate = purchaseDate;
	}
}
