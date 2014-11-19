package com.codman.cu.tracking.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codman.cu.tracking.AbstractTransAction.Status;
import com.codman.cu.tracking.vo.UnitVO.ProdType;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.Location;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: TransactionVO<p/>
 * <b>Description: Data bean for Transactions</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 03, 2010
 ****************************************************************************/
public class TransactionVO {
	
	private String transactionId = null;
	private Integer transactionTypeId = null;
	private String accountId = null;
	private Status status = null;
	private String statusName = null;
	private Integer requestNo = null;
	private Date approvalDate = null;
	private Date completedDate = null;
	private Integer unitCount = null;
	private Integer dropShipFlag = Integer.valueOf(0);
	private Location dropShipAddress = new Location();
	private Location repAddress = new Location();
	private String shipToName = null; //"ship to" name
	private String requestorName = null;
	private String notesText = null;
	private Date createDate = null;
	private PhysicianVO physician = new PhysicianVO();
	private Map<String, UnitVO> units = new HashMap<String,UnitVO>();
	private String approvorName = null;
	private String creditText = null;
	private ProdType productType = null;
	public TransactionVO() {
	}
	
	public TransactionVO(SMTServletRequest req) {
		
		transactionId = req.getParameter("transactionId");
		transactionTypeId = Convert.formatInteger(req.getParameter("transactionTypeId"));
		accountId = req.getParameter("accountId");
		setStatus(Convert.formatInteger(req.getParameter("statusId")));
		physician.setPhysicianId(req.getParameter("physicianId"));
		requestNo = Convert.formatInteger(req.getParameter("requestNo"));
		approvalDate = Convert.formatDate(req.getParameter("approvalDate"));
		completedDate = Convert.formatDate(req.getParameter("completedDate"));
		unitCount = Convert.formatInteger(req.getParameter("unitCount"));
		dropShipFlag = Convert.formatInteger(req.getParameter("dropShipFlag"));
		requestorName = req.getParameter("requestorName");
		notesText = req.getParameter("notesText");
		approvorName = req.getParameter("approvorName");
		shipToName = req.getParameter("shipToName");
		dropShipAddress.setAddress(req.getParameter("dropShipAddress"));
		dropShipAddress.setAddress2(req.getParameter("dropShipAddress2"));
		dropShipAddress.setCity(req.getParameter("dropShipCity"));
		dropShipAddress.setState(req.getParameter("dropShipState"));
		dropShipAddress.setZipCode(req.getParameter("dropShipZipCode"));
		dropShipAddress.setCountry(req.getParameter("dropShipCountry"));
		setCreditText(StringUtil.checkVal(req.getParameter("creditText")));
		setProductType(req.getParameter("productType"));
	}

	public TransactionVO(ResultSet rs) {
		DBUtil util = new DBUtil();
		transactionId = util.getStringVal("transaction_id", rs);
		transactionTypeId = util.getIntegerVal("transaction_type_id", rs);
		accountId = util.getStringVal("account_id", rs);
		setStatus(util.getIntegerVal("status_id", rs));
		physician.setPhysicianId(util.getStringVal("physician_id", rs));
		requestNo = util.getIntegerVal("request_no", rs);
		approvalDate = util.getDateVal("approval_dt", rs);
		completedDate = util.getDateVal("completed_dt", rs);
		unitCount = util.getIntegerVal("unit_cnt_no", rs);
		dropShipFlag = util.getIntegerVal("dropship_flg", rs);
		requestorName = util.getStringVal("requesting_party_nm", rs);
		approvorName = util.getStringVal("approving_party_nm", rs);
		notesText = util.getLargeStringVal("notes_txt", rs).toString();
		createDate = util.getDateVal("trans_create_dt", rs);
		setCreditText(util.getStringVal("credit_txt", rs));
		
		shipToName = util.getStringVal("ship_to_nm", rs);
		dropShipAddress.setAddress(util.getStringVal("address_txt", rs));
		dropShipAddress.setAddress2(util.getStringVal("address2_txt", rs));
		dropShipAddress.setCity(util.getStringVal("city_nm", rs));
		dropShipAddress.setState(util.getStringVal("state_cd", rs));
		dropShipAddress.setZipCode(util.getStringVal("zip_cd", rs));
		dropShipAddress.setCountry(util.getStringVal("country_cd", rs));
		
		setProductType(util.getStringVal("trans_product_cd", rs));

		util = null;
	}

	public String toString() {
		return StringUtil.getToString(this) + super.toString();
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	
	/**
	 * @return the transactionId
	 */
	public String getTransactionId() {
		return transactionId;
	}

	/**
	 * @param transactionId the transactionId to set
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * @return the transactionTypeId
	 */
	public Integer getTransactionTypeId() {
		return transactionTypeId;
	}

	/**
	 * @param transactionTypeId the transactionTypeId to set
	 */
	public void setTransactionTypeId(Integer transactionTypeId) {
		this.transactionTypeId = transactionTypeId;
	}

	/**
	 * @return the statusId
	 */
	public Integer getStatusId() {
		if (status != null) return status.getStatusCode();
		return Integer.valueOf(0);
	}

	/**
	 * @param statusId the statusId to set
	 */
	public void setStatus(Integer statusId) {
		if (statusId == null || statusId == 0) return;
		for (Status s : Status.values()) {
			if (s.getStatusCode() == statusId)
				this.status = s;
		}
	}
	public void setStatus(Status s) {
		this.status = s;
	}
	public Status getStatus() {
		return status;
	}
	public String getStatusStr() {
		if (status != null) return status.getStatusName();
		return null;
	}

	/**
	 * @return the requestNo
	 */
	public Integer getRequestNo() {
		return requestNo;
	}

	/**
	 * @param requestNo the requestNo to set
	 */
	public void setRequestNo(Integer requestNo) {
		this.requestNo = requestNo;
	}

	/**
	 * @return the approvalDate
	 */
	public Date getApprovalDate() {
		return approvalDate;
	}

	/**
	 * @param approvalDate the approvalDate to set
	 */
	public void setApprovalDate(Date approvalDate) {
		this.approvalDate = approvalDate;
	}

	/**
	 * @return the unitCount
	 */
	public Integer getUnitCount() {
		return unitCount;
	}

	/**
	 * @param unitCount the unitCount to set
	 */
	public void setUnitCount(Integer unitCount) {
		this.unitCount = unitCount;
	}

	/**
	 * @return the dropShipFlag
	 */
	public Integer getDropShipFlag() {
		return dropShipFlag;
	}

	/**
	 * @param dropShipFlag the dropShipFlag to set
	 */
	public void setDropShipFlag(Integer dropShipFlag) {
		this.dropShipFlag = dropShipFlag;
	}
	
	public boolean isDropShip() {
		return (dropShipFlag == 1);
	}

	/**
	 * @return the requestorName
	 */
	public String getRequestorName() {
		return requestorName;
	}

	/**
	 * @param requestorName the requestorName to set
	 */
	public void setRequestorName(String requestorName) {
		this.requestorName = requestorName;
	}
	/**
	 * @return the notesText
	 */
	public String getNotesText() {
		return notesText;
	}

	/**
	 * @param notesText the notesText to set
	 */
	public void setNotesText(String notesText) {
		this.notesText = notesText;
	}

	/**
	 * @return the dropShipAddress
	 */
	public Location getDropShipAddress() {
		return dropShipAddress;
	}

	/**
	 * @param dropShipAddress the dropShipAddress to set
	 */
	public void setDropShipAddress(Location dropShipAddress) {
		this.dropShipAddress = dropShipAddress;
	}

	/**
	 * @return the validated shipping address
	 */
	public Location getShippingAddress() {
		return (dropShipFlag == 1 && dropShipAddress.isValidAddress()) ? dropShipAddress : repAddress;
	}

	/**
	 * @param dropShipAddress the dropShipAddress to set
	 */
	public void setRepsAddress(Location repAddress) {
		this.repAddress = repAddress;
	}


	/**
	 * @return the createDate
	 */
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setPhysician(PhysicianVO physician) {
		this.physician = physician;
	}

	public PhysicianVO getPhysician() {
		return physician;
	}

	public void setUnits(Map<String, UnitVO> units) {
		this.units = units;
	}

	public List<UnitVO> getUnits() {
		List<UnitVO> data = new ArrayList<UnitVO>(units.values());
		Collections.sort(data, new UnitComparator());
		return data;
	}
	
	public Map<String, UnitVO> getUnitMap() {
		return units;
	}
	
	public void addUnit(UnitVO vo) {
		this.units.put(vo.getUnitId(), vo);
	}
	
	public String getUnitSerialNos() {
		String s = "";
		for (UnitVO vo : units.values()) {
			if (s.length() > 0) s += ", ";
			s += vo.getSerialNo();
		}
		
		return s;
	}

	public void setCompletedDate(Date completedDate) {
		this.completedDate = completedDate;
	}

	public Date getCompletedDate() {
		return completedDate;
	}

	public void setShipToName(String shipToName) {
		this.shipToName = shipToName;
	}

	public String getShipToName() {
		return shipToName;
	}

	public String getApprovorName() {
		return approvorName;
	}

	public void setApprovorName(String approvorName) {
		this.approvorName = approvorName;
	}

	/**
	 * @return the statusName
	 */
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
	 * @return the creditText
	 */
	public String getCreditText() {
		return creditText;
	}

	/**
	 * @param creditText the creditText to set
	 */
	public void setCreditText(String creditText) {
		this.creditText = creditText;
	}

	public ProdType getProductType() {
		return productType;
	}
	public String getProductTypeStr() {
		if (productType != null) return productType.toString();
		else return null;
	}

	public void setProductType(String productType) {
		try {
			this.productType = ProdType.valueOf(productType);
		} catch (Exception e) {}
	}
	public void setProductType(ProdType productType) {
		this.productType = productType;
	}

}

class TransactionComparator implements Comparator<TransactionVO> {
	public static final long serialVersionUID = 1l;
	
	/**
	 * Compares using the last name and then first name and then state
	 */
	public int compare(TransactionVO o1, TransactionVO o2) {
		// Check the objects for null
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		else if (o2 == null) return 1;
		return o2.getRequestNo().compareTo(o1.getRequestNo());
		
	}

}
