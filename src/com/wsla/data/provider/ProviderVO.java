package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8670063898439472671L;
	
	// Member Variables
	private String providerId;
	private ProviderType providerType;
	private String providerName;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-Elements
	List<ProviderLocationVO> locations = new ArrayList<>();

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
	 * @return the locations
	 */
	public List<ProviderLocationVO> getLocations() {
		return locations;
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
	public void setProviderName(String providerName) {
		this.providerName = providerName;
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
	@BeanSubElement
	public void setLocations(List<ProviderLocationVO> locations) {
		this.locations = locations;
	}
	
	/**
	 * 
	 * @param location
	 */
	public void addLocation(ProviderLocationVO location) {
		if (location != null)
			this.locations.add(location);
	}

}

