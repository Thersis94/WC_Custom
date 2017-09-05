package com.ram.action.or.vo;

import java.sql.ResultSet;
// JDK 1.7
import java.util.Date;

// RAM Libs
import com.ram.action.or.RAMCaseManager;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> RamCaseItemVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages RAM Case Item Information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
@Table(name="ram_case_item")
public class RAMCaseItemVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum RAMCaseType {OR, SPD}

	private String caseItemId;
	private Integer productId;
	private String customerProductId;
	private String productName;
	private String caseKitId;
	private String caseId;
	private int quantity;
	private RAMCaseType caseType;
	private String lotNumberTxt;
	private int billableFlg;
	private int wastedFlg;
	private String wastedReason;
	private String productFromTxt;
	private String customerName;
	private String gtinProductId;
	private Date expiree;
	private Date createDt;
	private Date updateDt;

	public RAMCaseItemVO() {
		super();
	}

	public RAMCaseItemVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public RAMCaseItemVO(ResultSet rs) {
		this();
		this.populateData(rs);
	}

	public void setData(ActionRequest req) {
		caseItemId = req.getParameter("caseItemId");
		productId = Convert.formatInteger(req.getParameter("productId"));
		productName = req.getParameter("productName");
		caseKitId = StringUtil.checkVal(req.getParameter("caseKitId"), null);
		caseId = req.getParameter(RAMCaseManager.RAM_CASE_ID);
		quantity = Convert.formatInteger(req.getParameter("quantity"));
		lotNumberTxt = req.getParameter("lotNumberTxt");
		billableFlg = Convert.formatInteger(req.getParameter("billableFlg"));
		wastedFlg = Convert.formatInteger(req.getParameter("wastedFlg"));
		if(wastedFlg == 1) {
			wastedReason = req.getParameter("wastedReason");
		}
		productFromTxt = req.getParameter("productFromTxt");
		customerName = req.getParameter("customerName");
		setCaseTypeTxt(req.getParameter("caseTypeCd"));
		if (!StringUtil.isEmpty(req.getParameter("expiree")))
			setExpiree(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("expiree")));
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the caseItemId
	 */
	@Column(name="case_item_id", isPrimaryKey=true)
	public String getCaseItemId() {
		return caseItemId;
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id")
	public Integer getProductId() {
		return productId;
	}

	/**
	 * @return the productNm
	 */
	@Column(name="product_nm", isReadOnly=true)
	public String getProductName() {
		return productName;
	}

	/**
	 * @return the caseKitId
	 */
	@Column(name="case_kit_id")
	public String getCaseKitId() {
		return caseKitId;
	}

	/**
	 * @return the caseId
	 */
	@Column(name="case_id")
	public String getCaseId() {
		return caseId;
	}

	/**
	 * @return the qtyNo
	 */
	@Column(name="qty_no")
	public int getQuantity() {
		return quantity;
	}

	/**
	 * @return the caseType
	 */
	public RAMCaseType getCaseType() {
		return caseType;
	}

	/**
	 * @return the caseType
	 */
	@Column(name="case_type_cd")
	public String getCaseTypeTxt() {
		return caseType.toString();
	}

	/**
	 * @return the lotNumberTxt
	 */
	@Column(name="lot_number_txt")
	public String getLotNumberTxt() {
		return lotNumberTxt;
	}

	/**
	 * @return the billableFlg
	 */
	@Column(name="billable_flg")
	public int getBillableFlg() {
		return billableFlg;
	}

	/**
	 * @return the wastedFlg
	 */
	@Column(name="wasted_flg")
	public int getWastedFlg() {
		return wastedFlg;
	}

	/**
	 * @return the wastedReason
	 */
	@Column(name="wasted_reason")
	public String getWastedReason() {
		return wastedReason;
	}

	/**
	 * @return the productFromTxt
	 */
	@Column(name="product_from_txt")
	public String getProductFromTxt() {
		return productFromTxt;
	}

	/**
	 * @return the customerNm
	 */
	@Column(name="customer_nm", isReadOnly=true)
	public String getCustomerName() {
		return customerName;
	}

	/**
	 * @return the gtinProductId
	 */
	@Column(name="gtin_number_txt", isReadOnly=true)
	public String getGtinProductId() {
		return gtinProductId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param caseItemId the caseItemId to set.
	 */
	public void setCaseItemId(String caseItemId) {
		this.caseItemId = caseItemId;
	}

	/**
	 * @param productId the productId to set.
	 */
	public void setProductId(Integer productId) {
		this.productId = productId;
	}

	/**
	 * @param productNm the productNm to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * @param caseKitId the caseKitId to set.
	 */
	public void setCaseKitId(String caseKitId) {
		this.caseKitId = caseKitId;
	}

	/**
	 * @param caseId the caseId to set.
	 */
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	/**
	 * @param qtyNo the qtyNo to set.
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/**
	 * @param caseType the caseType to set.
	 */
	public void setCaseType(RAMCaseType caseType) {
		this.caseType = caseType;
	}

	/**
	 * @param caseType the caseType to set.
	 */
	public void setCaseTypeTxt(String caseType) {
		try{
			this.caseType = RAMCaseType.valueOf(caseType);
		} catch(Exception e) {
			//Throw away exception.
		}
	}

	/**
	 * @param lotNumberTxt the lotNumberTxt to set
	 */
	public void setLotNumberTxt(String lotNumberTxt) {
		this.lotNumberTxt = lotNumberTxt;
	}

	/**
	 * @param billableFlg the billableFlg to set
	 */
	public void setBillableFlg(int billableFlg) {
		this.billableFlg = billableFlg;
	}

	/**
	 * @param wastedFlg the wastedFlg to set
	 */
	public void setWastedFlg(int wastedFlg) {
		this.wastedFlg = wastedFlg;
	}

	/**
	 * @param wastedReason the wastedReason
	 */
	public void setWastedReason(String wastedReason) {
		this.wastedReason = wastedReason;
	}

	/**
	 * @param productFromTxt the productFromTxt to set
	 */
	public void setProductFromTxt(String productFromTxt) {
		this.productFromTxt = productFromTxt;
	}

	/**
	 * @param customerNm the customerNm to set
	 */
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	/**
	 * @param gtinProductId the gtinProductId to set
	 */
	public void setGtinProductId(String gtinProductId) {
		this.gtinProductId = gtinProductId;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * @param qtyNo2
	 */
	public void addQty(int quantity) {
		this.quantity += quantity;
	}

	/**
	 * @return the productSku
	 */
	@Column(name="cust_product_id", isReadOnly=true)
	public String getCustomerProductId() {
		return customerProductId;
	}

	/**
	 * @param productSku the productSku to set
	 */
	public void setCustomerProductId(String customerProductId) {
		this.customerProductId = customerProductId;
	}

	/**
	 * @return the expiree
	 */
	@Column(name="expiree_dt")
	public Date getExpiree() {
		return expiree;
	}

	/**
	 * @param expiree the expiree to set
	 */
	public void setExpiree(Date expiree) {
		this.expiree = expiree;
	}
}