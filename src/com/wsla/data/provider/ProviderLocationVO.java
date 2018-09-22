package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;

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

	/**
	 * 
	 */
	private static final long serialVersionUID = -923102909693893896L;

	// Member Variables
	private String providerId;
	private String locationName;
	private String storeNumber;
	private int activeFlag;
	private int defaultFlag;
	private int manualGeocodeFlag;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-Elements
	List<AuthorizedServiceProviderVO> authorizedServiceProviders = new ArrayList<>();
	
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
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param defaultFlag the defaultFlag to set
	 */
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
	public void setStoreNumber(String storeNumber) {
		this.storeNumber = storeNumber;
	}
}