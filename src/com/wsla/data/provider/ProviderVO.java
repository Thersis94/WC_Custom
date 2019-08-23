package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.admin.InvoiceTypeCasVO;

/****************************************************************************
 * <b>Title</b>: ProviderVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the WSLA providers
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_provider")
public class ProviderVO extends BeanDataVO {

	private static final long serialVersionUID = -8670063898439472671L;
	
	// Member Variables
	private String providerId;
	private ProviderType providerType;
	private String providerName;
	private String iconPath;
	private String cssPath;
	private String domainUrl;
	private String phoneNumber;
	private int reviewFlag;
	private int activeFlag;
	private int locationReviewNumber;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-Elements
	private List<ProviderLocationVO> locations = new ArrayList<>();
	private List<InvoiceTypeCasVO> invoiceItems = new ArrayList<>();
	
	/**
	 * 
	 */
	public ProviderVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProviderVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProviderVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the providerId
	 */
	@Column(name="provider_id", isPrimaryKey=true)
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the providerType
	 */
	@Column(name="provider_type_id")
	public ProviderType getProviderType() {
		return providerType;
	}

	/**
	 * @return the providerName
	 */
	@Column(name="provider_nm")
	public String getProviderName() {
		return providerName;
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
	 * @return the reviewFlag
	 */
	@Column(name="review_flg")
	public int getReviewFlag() {
		return reviewFlag;
	}

	/**
	 * @return the iconPath
	 */
	@Column(name="logo_img_path")
	public String getIconPath() {
		return iconPath;
	}

	/**
	 * @return the cssPath
	 */
	@Column(name="theme_css_path")
	public String getCssPath() {
		return cssPath;
	}

	/**
	 * @return the locations
	 */
	public List<ProviderLocationVO> getLocations() {
		return locations;
	}

	/**
	 * @return the locationReviewNumber
	 */
	@Column(name="review_location_no", isReadOnly = true)
	public int getLocationReviewNumber() {
		return locationReviewNumber;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param locationReviewNumber the locationReviewNumber to set
	 */
	public void setLocationReviewNumber(int locationReviewNumber) {
		this.locationReviewNumber = locationReviewNumber;
	}

	/**
	 * @param providerId the providerId to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @param providerType the providerType to set
	 */
	public void setProviderType(ProviderType providerType) {
		this.providerType = providerType;
	}
	
	/**
	 * @param providerName the providerName to set
	 */
	@Importable(name="Provider Name", type=DataType.STRING)
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	/**
	 * @return the invoiceItems
	 */
	public List<InvoiceTypeCasVO> getInvoiceItems() {
		return invoiceItems;
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
	 * @param locations the locations to set
	 */
	public void setLocations(List<ProviderLocationVO> locations) {
		this.locations = locations;
	}
	
	/**
	 * 
	 * @param location
	 */
	@BeanSubElement
	public void addLocation(ProviderLocationVO location) {
		if (location != null)
			this.locations.add(location);
	}

	/**
	 * @param reviewFlag the reviewFlag to set
	 */
	@Importable(name="Review Flag", type=DataType.INT)
	public void setReviewFlag(int reviewFlag) {
		this.reviewFlag = reviewFlag;
	}

	/**
	 * @param iconPath the iconPath to set
	 */
	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}

	/**
	 * @param cssPath the cssPath to set
	 */
	public void setCssPath(String cssPath) {
		this.cssPath = cssPath;
	}

	/**
	 * @return the domainUrl
	 */
	@Column(name="domain_url")
	public String getDomainUrl() {
		return domainUrl;
	}

	/**
	 * @param domainUrl the domainUrl to set
	 */
	public void setDomainUrl(String domainUrl) {
		this.domainUrl = domainUrl;
	}

	/**
	 * @return the phoneNumber
	 */
	@Column(name="phone_number_txt")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * @param invoiceItems the invoiceItems to set
	 */
	public void setInvoiceItems(List<InvoiceTypeCasVO> invoiceItems) {
		this.invoiceItems = invoiceItems;
	}

	/**
	 * 
	 * @param invoiceItem
	 */
	@BeanSubElement
	public void addInvoiceItem(InvoiceTypeCasVO invoiceItem) {
		if (invoiceItem != null && ! StringUtil.isEmpty(invoiceItem.getProviderId()))
			this.invoiceItems.add(invoiceItem);
	}
}