package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.action.admin.WarrantyAction.ServiceTypeCode;

/****************************************************************************
 * <b>Title</b>: WarrantyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages Warranty data for the wsla swervice offerings 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_warranty")
public class WarrantyVO extends BeanDataVO {

	private static final long serialVersionUID = -4567698594237550575L;

	// Member Variables
	private String warrantyId;
	private WarrantyType warrantyType;
	private ServiceTypeCode serviceTypeCode;
	private String providerId;
	private String refundProviderId;
	private String refundProviderName;
	private String providerName;
	private String description;
	private int requireApprovalFlag;
	private int warrantyLength;
	private int activeFlag; 
	private int flatRateFlag; 
	
	private Date createDate;
	private Date updateDate;

	public WarrantyVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WarrantyVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WarrantyVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the warrantyId
	 */
	@Column(name="warranty_id", isPrimaryKey=true)
	public String getWarrantyId() {
		return warrantyId;
	}

	/**
	 * @return the warrantyType
	 */
	@Column(name="warranty_type_cd")
	public WarrantyType getWarrantyType() {
		return warrantyType;
	}

	/**
	 * @return the description
	 */
	@Column(name="desc_txt")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the warrantyLength
	 */
	@Column(name="warranty_days_no")
	public int getWarrantyLength() {
		return warrantyLength;
	}

	/**
	 * @return the providerId
	 */
	@Column(name="provider_id")
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the requireApprovalFlag
	 */
	@Column(name="require_approval_flg")
	public int getRequireApprovalFlag() {
		return requireApprovalFlag;
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
	 * @return the refundProviderId
	 */
	@Column(name="refund_provider_id")
	public String getRefundProviderId() {
		return refundProviderId;
	}

	/**
	 * @return the refundProviderName
	 */
	@Column(name="refund_provider_nm", isReadOnly=true)
	public String getRefundProviderName() {
		return refundProviderName;
	}

	/**
	 * @return the serviceTypeCode
	 */
	@Column(name="warranty_service_type_cd")
	public ServiceTypeCode getServiceTypeCode() {
		return serviceTypeCode;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the flatRateFlag
	 */
	@Column(name="flat_rate_flg")
	public int getFlatRateFlag() {
		return flatRateFlag;
	}

	/**
	 * @param flatRateFlag the flatRateFlag to set
	 */
	public void setFlatRateFlag(int flatRateFlag) {
		this.flatRateFlag = flatRateFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param serviceTypeCode the serviceTypeCode to set
	 */
	public void setServiceTypeCode(ServiceTypeCode serviceTypeCode) {
		this.serviceTypeCode = serviceTypeCode;
	}

	/**
	 * @param warrantyId the warrantyId to set
	 */
	public void setWarrantyId(String warrantyId) {
		this.warrantyId = warrantyId;
	}

	/**
	 * @param warrantyType the warrantyType to set
	 */
	public void setWarrantyType(WarrantyType warrantyType) {
		this.warrantyType = warrantyType;
	}

	/**
	 * @param warrantyType the warrantyType to set
	 */
	public void setWarrantyType(String strWarrantyType) {
		if (StringUtil.isEmpty(strWarrantyType)) return;
		setWarrantyType(EnumUtil.safeValueOf(WarrantyType.class, strWarrantyType));
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param warrantyLength the warrantyLength to set
	 */
	public void setWarrantyLength(int warrantyLength) {
		this.warrantyLength = warrantyLength;
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
	 * @param providerId the providerId to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @param requireApprovalFlag the requireApprovalFlag to set
	 */
	public void setRequireApprovalFlag(int requireApprovalFlag) {
		this.requireApprovalFlag = requireApprovalFlag;
	}

	@Column(name="provider_nm", isReadOnly=true)
	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	/**
	 * @param refundProviderId the refundProviderId to set
	 */
	public void setRefundProviderId(String refundProviderId) {
		this.refundProviderId = refundProviderId;
	}

	/**
	 * @param refundProviderName the refundProviderName to set
	 */
	public void setRefundProviderName(String refundProviderName) {
		this.refundProviderName = refundProviderName;
	}
}