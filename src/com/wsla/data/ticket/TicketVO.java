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
import com.siliconmtn.data.Node;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.DateDiff;
import com.siliconmtn.util.StringUtil;
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

	private static final long serialVersionUID = -288262467687670031L;

	/**
	 * Standing of the ticket in relation to how its progressing through the workflow
	 */
	public enum Standing {
		GOOD("GREEN"), DELAYED("YELLOW"), CRITICAL("RED");

		private String color;
		private Standing(String color) { this.color = color; }
		public String getColor() {return color; }
	}

	
	/**
	 * Definition of who has possession the product
	 */
	public enum UnitLocation {
		CALLER, OEM, RETAILER, COURIER, WSLA, DECOMMISSIONED, CAS;
	}
		
	// String Member Variables
	private String ticketId;
	private String parentId;
	private String ticketIdText;
	private String description;
	private String productWarrantyId;
	private String productCategoryId;
	private String productSerialId;
	private String lockedBy;
	private String lockedByName;
	private StatusCode statusCode;
	private String phoneNumber;
	private int historicalFlag;
	
	// Numeric Members
	private int warrantyValidFlag;
	private int creationTime;
	
	// Date Members
	private Date purchaseDate;
	private Date createDate;
	private Date updateDate;
	private Date lockedDate;
	
	// Enum Members
	private Standing standingCode = Standing.GOOD;
	private UnitLocation unitLocation;

	// Helper Variables
	private String retailerId;
	private String oemId;
	private String userId;
	private String statusName;
	private boolean ticketLocked;

	// Bean Sub-Element
	private List<TicketDataVO> ticketData = new ArrayList<>(32);
	private List<TicketAssignmentVO> assignments = new ArrayList<>();
	private Map<String, TicketScheduleVO> schedule = new HashMap<>();
	private List<TicketLedgerVO> timeline = new ArrayList<>();
	private List<Node> comments= new ArrayList<>();
	private List<DiagnosticRunVO> diagnosticRun = new ArrayList<>();
	private ProductSerialNumberVO productSerial = new ProductSerialNumberVO();
	private ProviderLocationVO retailer;
	private RefundReplacementVO rar;
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
	 * Determines if the ticket is locked
	 */
	private void setLocked() {
		if (lockedDate == null) ticketLocked = false;
		else {
			try {
				DateDiff diff = new DateDiff(lockedDate, new Date());
				if (diff.getTotalMinutes() < 120) ticketLocked = true;
			} catch (Exception e) {
				ticketLocked = false;
			}
		}
	}
	
	/**
	 * used to loop the ticket data list and find the attribute by key and return that ticket data vo
	 * 
	 * will return null when it can not find the attribute by key
	 * 
	 * @param key
	 * @return
	 */
	public TicketDataVO getAttribute(String key) {
		
		for (TicketDataVO d : ticketData) {
			if(d.getAttributeCode()!= null && d.getAttributeCode().equalsIgnoreCase(key)) {
				return d;
			}
		}
		
		return null;
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
	 * Returns the difference between 2 dates
	 * @return
	 */
	public DateDiff getAge() {
		DateDiff diff = null;
		try {
			diff = new DateDiff(createDate, new Date());
		} catch (InvalidDataException e) { /* Nothing to DO */ }
		
		return diff;
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
	 * Helper method for the view.  In some cases we only want back the most recent values for each data attribute
	 * @return
	 */
	public Map<String, TicketDataVO> getLatestDataMap() {
		Map<String, TicketDataVO> data = new HashMap<>();
		for (TicketDataVO td : getTicketData()) {
			if (data.containsKey(td.getAttributeCode())) {
				if(data.get(td.getAttributeCode()).getCreateDate().before(td.getCreateDate())) {
					data.put(td.getAttributeCode(), td);
				}
			}else {
				data.put(td.getAttributeCode(), td);
			}
		}
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
	 * @return the phoneNumber
	 */
	@Column(name="phone_number_txt")
	public String getPhoneNumber() {
		return phoneNumber;
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
	 * @return the ticketLocked
	 */
	public boolean isTicketLocked() {
		return ticketLocked;
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
	 * @return the unitLocation
	 */
	@Column(name="unit_location_cd")
	public UnitLocation getUnitLocation() {
		return unitLocation;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the lockedByName
	 */
	@Column(name="locked_nm", isReadOnly=true)
	public String getLockedByName() {
		return lockedByName;
	}

	/**
	 * @return the creationTime
	 */
	@Column(name="creation_time_no")
	public int getCreationTime() {
		return creationTime;
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
	 * @return the schedule
	 */
	public Map<String, TicketScheduleVO> getSchedule() {
		return schedule;
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
	 * @param schedule the schedule to set
	 */
	public void setSchedule(Map<String, TicketScheduleVO> schedule) {
		this.schedule = schedule;
	}

	/**
	 * 
	 * @param schedule
	 */
	@BeanSubElement
	public void addSchedule(TicketScheduleVO schedule) {
		this.schedule.put(schedule.getRecordTypeCode(), schedule);
	}

	/**
	 * 
	 * @param schedule
	 */
	public void addSchedules(List<TicketScheduleVO> schedules) {
		for (TicketScheduleVO ts : schedules)
			addSchedule(ts);
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
	 * When setting the locked date, it looks to see how long the ticket has been
	 * locked. If less than 2 hours, it stays locked, otherwise, the ticket is unlocked
	 * @param lockedDate the lockedDate to set
	 */
	public void setLockedDate(Date lockedDate) {
		this.lockedDate = lockedDate;
		setLocked();
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
	 * @return the comments
	 */
	public List<Node> getComments() {
		return comments;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(List<Node> comments) {
		this.comments = comments;
	}

	/**
	 * @return the historicalFlag
	 */
	@Column(name="historical_flg")
	public int getHistoricalFlag() {
		return historicalFlag;
	}

	/**
	 * @param historicalFlag the historicalFlag to set
	 */
	public void setHistoricalFlag(int historicalFlag) {
		this.historicalFlag = historicalFlag;
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

	/**
	 * @param unitLocation the unitLocation to set
	 */
	public void setUnitLocation(UnitLocation unitLocation) {
		this.unitLocation = unitLocation;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @param lockedByName the lockedByName to set
	 */
	public void setLockedByName(String lockedByName) {
		this.lockedByName = lockedByName;
	}

	/**
	 * @return the rar
	 */
	public RefundReplacementVO getRar() {
		return rar;
	}

	/**
	 * @param rar the rar to set
	 */
	public void setRar(RefundReplacementVO rar) {
		this.rar = rar;
	}

	/**
	 * @param creationTime the creationTime to set
	 */
	public void setCreationTime(int creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
}
