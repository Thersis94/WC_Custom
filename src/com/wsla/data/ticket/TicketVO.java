package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;

// WSLA Libs
import com.wsla.common.WSLAConstants;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.provider.ProviderVO;

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
	
	/**
	 * Stabding of the ticket in relation to how its progressing through the workflow
	 */
	public enum Standing {
		GOOD("GREEN"), DELAYED("YELLOW"), CRITICAL("RED");
		
		public final String color;
		public String getColor() {return color; }
		Standing(String color) { this.color = color; }
	}
		
	// Member Variables
	private String ticketId;
	private String ticketIdText;
	private String description;
	private String productWarrantyId;
	private String productCategoryId;
	private String productSerialId;
	private String lockedBy;
	private StatusCode statusCode;
	private Standing standingCode = Standing.GOOD;
	private int warrantyValidFlag;
	private Date purchaseDate;
	private Date createDate;
	private Date updateDate;
	private Date lockedDate;
	
	// Helper Variables
	private String retailerId;
	private String oemId;
	private String userId;
	private String statusName;
	
	// Bean Sub-Element
	private List<TicketDataVO> ticketData = new ArrayList<>(32);
	private List<TicketAssignmentVO> assignments = new ArrayList<>();
	private List<TicketLedgerVO> timeline = new ArrayList<>();
	private List<DiagnosticRunVO> diagnosticRun = new ArrayList<>();
	private ProductSerialNumberVO productSerial = new ProductSerialNumberVO();
	private ProviderLocationVO retailer;
	private ProviderVO oem;
	private UserVO originator;
	private ProductWarrantyVO warranty;
	private StatusCodeVO status;
	
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
	 * Helper method to return the assigned CAS
	 * @return
	 */
	public TicketAssignmentVO getCas() {
		
		for (TicketAssignmentVO ta : assignments) {
			if (TicketAssignmentVO.TypeCode.CAS.equals(ta.getTypeCode())) return ta;
		}
		
		return new TicketAssignmentVO();
	}
	
	/**
	 * Helper method for the view.  JSTL can't pass param to getters, so I will return
	 * the data as a map, which allows JSTL to select values
	 * @return
	 */
	public Map<String, TicketDataVO> getTicketDataMap() {
		Map<String, TicketDataVO> data = new HashMap<>();
		for (TicketDataVO td : ticketData) data.put(td.getAttributeCode(), td);
		return data;
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
	 * @return the description
	 */
	@Column(name="desc_txt")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the warrantyProductId
	 */
	@Column(name="product_warranty_id")
	public String getProductWarrantyId() {
		return StringUtil.isEmpty(productWarrantyId) ? null : productWarrantyId;
	}

	/**
	 * @return the productSerialId
	 */
	@Column(name="product_serial_id")
	public String getProductSerialId() {
		return StringUtil.isEmpty(productSerialId) ? null : productSerialId;
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
	 * @return the lockedBy
	 */
	@Column(name="locked_by_id")
	public String getLockedBy() {
		return lockedBy;
	}

	/**
	 * @return the lockedDate
	 */
	@Column(name="locked_dt")
	public Date getLockedDate() {
		return lockedDate;
	}

	/**
	 * @return the retailerId
	 */
	@Column(name="retailer_id")
	public String getRetailerId() {
		return retailerId;
	}

	/**
	 * @return the oemId
	 */
	@Column(name="oem_id")
	public String getOemId() {
		return oemId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="originator_user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the standingCode
	 */
	@Column(name="standing_cd")
	public Standing getStandingCode() {
		return standingCode;
	}

	/**
	 * @return the retailer
	 */
	public ProviderLocationVO getRetailer() {
		return retailer;
	}

	/**
	 * @return the originator
	 */
	public UserVO getOriginator() {
		return originator;
	}

	/**
	 * @return the oem
	 */
	public ProviderVO getOem() {
		return oem;
	}

	/**
	 * @return the diagnosticRun
	 */
	public List<DiagnosticRunVO> getDiagnosticRun() {
		return diagnosticRun;
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
	 * @return the productSerial
	 */
	public ProductSerialNumberVO getProductSerial() {
		return productSerial;
	}

	/**
	 * @return the warranty
	 */
	public ProductWarrantyVO getWarranty() {
		return warranty;
	}

	/**
	 * @return the status
	 */
	public StatusCodeVO getStatus() {
		return status;
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
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param productWarrantyId the productWarrantyId to set
	 */
	public void setProductWarrantyId(String productWarrantyId) {
		this.productWarrantyId = productWarrantyId;
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
	public void setTimeline(List<TicketLedgerVO> timeline) {
		this.timeline = timeline;
	}
	
	/**
	 * 
	 * @param entry
	 */
	@BeanSubElement
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

	/**
	 * @param diagnosticRun the diagnosticRun to set
	 */
	public void setDiagnosticRun(List<DiagnosticRunVO> diagnosticRun) {
		this.diagnosticRun = diagnosticRun;
	}
	
	/**
	 * 
	 * @param diag
	 */
	@BeanSubElement
	public void addDiagnosticRun(DiagnosticRunVO diag) {
		this.diagnosticRun.add(diag);
	}

	/**
	 * @param productSerialId the productSerialId to set
	 */
	public void setProductSerialId(String productSerialId) {
		this.productSerialId = productSerialId;
	}

	/**
	 * @param productSerial the productSerial to set
	 */
	@BeanSubElement
	public void setProductSerial(ProductSerialNumberVO productSerial) {
		this.productSerial = productSerial;
	}

	/**
	 * @param lockedBy the lockedBy to set
	 */
	public void setLockedBy(String lockedBy) {
		this.lockedBy = lockedBy;
	}

	/**
	 * @param lockedDate the lockedDate to set
	 */
	public void setLockedDate(Date lockedDate) {
		this.lockedDate = lockedDate;
	}

	/**
	 * @param retailerId the retailerId to set
	 */
	public void setRetailerId(String retailerId) {
		this.retailerId = retailerId;
	}

	/**
	 * @param oemId the oemId to set
	 */
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param retailer the retailer to set
	 */
	@BeanSubElement
	public void setRetailer(ProviderLocationVO retailer) {
		this.retailer = retailer;
	}

	/**
	 * @param oem the oem to set
	 */
	@BeanSubElement
	public void setOem(ProviderVO oem) {
		this.oem = oem;
	}

	/**
	 * @param originator the originator to set
	 */
	@BeanSubElement
	public void setOriginator(UserVO originator) {
		this.originator = originator;
	}

	/**
	 * @return the status_nm
	 */
	@Column(name="status_nm", isReadOnly=true)
	public String getStatusName() {
		return statusName;
	}

	/**
	 * @param statusName the statusName to set
	 */
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	/**
	 * @param standingCode the standingCode to set
	 */
	public void setStandingCode(Standing standingCode) {
		this.standingCode = standingCode;
	}

	/**
	 * @param warranty the warranty to set
	 */
	public void setWarranty(ProductWarrantyVO warranty) {
		this.warranty = warranty;
	}

	/**
	 * @param status the status to set
	 */
	@BeanSubElement
	public void setStatus(StatusCodeVO status) {
		this.status = status;
	}
}

