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
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProviderLocationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the provider location table data.  Extends
 * the geocode location class to obtain the elements and value of that class
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_provider_location")
public class ProviderLocationVO extends GeocodeLocation {

	private static final long serialVersionUID = -923102909693893896L;
	public static final String SINGLE_LINE_ADDRESS = "singleLineAddress";

	// Member Variables
	private String providerId;
	private String locationName;
	private String locationDescription;
	private String storeNumber;
	private int activeFlag;
	private int defaultFlag;
	private int manualGeocodeFlag;
	private int reviewFlag;
	private Date createDate;
	private Date updateDate;

	private String providerName;
	private Status status;

	// Bean Sub-Elements
	private List<AuthorizedServiceProviderVO> authorizedServiceProviders = new ArrayList<>();

	public enum Status {
		PENDING_CONTACT,
		EMAIL_SENT,
		RCVD_QUESTIONNAIRE,
		REVW_QUESTIONNAIRE,
		REJECTED,
		AUTHORIZED,
		SEND_CONTRACT,
		AWAITING_CONTRACT,
		SIGNED_CONTRACT;
	}

	/**
	 * 
	 */
	public ProviderLocationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProviderLocationVO(ActionRequest req) {
		this.populateData(req);
	}

	/**
	 * @param rs
	 */
	public ProviderLocationVO(ResultSet rs) {
		this.populateData(rs);
	}

	/**
	 * @return the locationId
	 */
	@Column(name="location_id", isPrimaryKey=true)
	@Override
	public String getLocationId() {
		return super.getLocationId();
	}

	/**
	 * @return the providerId
	 */
	@Column(name="provider_id")
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the locationName
	 */
	@Column(name="location_nm")
	public String getLocationName() {
		return locationName;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the defaultFlag
	 */
	@Column(name="default_flg")
	public int getDefaultFlag() {
		return defaultFlag;
	}

	/**
	 * @return the manualGeocodeFlag
	 */
	@Column(name="manual_geocode_flg")
	public int getManualGeocodeFlag() {
		return manualGeocodeFlag;
	}

	/**
	 * @return the storeNumber
	 */
	@Column(name="store_no")
	public String getStoreNumber() {
		return storeNumber;
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
	 * @return the authorizedServiceProvider
	 */
	public List<AuthorizedServiceProviderVO> getAuthorizedServiceProviders() {
		return authorizedServiceProviders;
	}

	/**
	 * @param providerId the providerId to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @param locationName the locationName to set
	 */
	@Importable(name="Location Name", type=DataType.STRING)
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	@Importable(name="Active ", type=DataType.INT)
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param defaultFlag the defaultFlag to set
	 */
	@Importable(name="Default", type=DataType.INT)
	public void setDefaultFlag(int defaultFlag) {
		this.defaultFlag = defaultFlag;
	}

	/**
	 * @param manualGeocodeFlag the manualGeocodeFlag to set
	 */
	public void setManualGeocodeFlag(int manualGeocodeFlag) {
		this.manualGeocodeFlag = manualGeocodeFlag;
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
	 * @param authorizedServiceProvider the authorizedServiceProvider to set
	 */
	public void setAuthorizedServiceProviders(List<AuthorizedServiceProviderVO> authorizedServiceProviders) {
		this.authorizedServiceProviders = authorizedServiceProviders;
	}

	/**
	 * 
	 * @param authorizedServiceProvider
	 */
	@BeanSubElement
	public void addAuthorizedServiceProvider(AuthorizedServiceProviderVO authorizedServiceProvider) {
		if (authorizedServiceProvider != null)
			this.authorizedServiceProviders.add(authorizedServiceProvider);
	}

	/**
	 * @param storeNumber the storeNumber to set
	 */
	@Importable(name="Store Number", type=DataType.STRING)
	public void setStoreNumber(String storeNumber) {
		this.storeNumber = storeNumber;
	}

	/**
	 * @param reviewFlag the reviewFlag to set
	 */
	@Importable(name="Review Flag", type=DataType.INT)
	public void setReviewFlag(int reviewFlag) {
		this.reviewFlag = reviewFlag;
	}

	@Column(name="provider_nm", isReadOnly=true)
	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	/**
	 * @return the locationDescription
	 */
	@Column(name="location_desc")
	public String getLocationDescription() {
		return locationDescription;
	}

	/**
	 * @param locationDescription the locationDescription to set
	 */
	public void setLocationDescription(String locationDescription) {
		this.locationDescription = locationDescription;
	}

	@Column(name="status_cd")
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public void setStatus(String status) {
		if (StringUtil.isEmpty(status)) return;
		setStatus(EnumUtil.safeValueOf(Status.class, status));
	}
}