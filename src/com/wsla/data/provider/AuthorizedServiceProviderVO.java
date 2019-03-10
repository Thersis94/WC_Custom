package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.wsla.data.product.ProductCategoryVO;

/****************************************************************************
 * <b>Title</b>: AuthorizedServiceProviderVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Additional information for each service center provider type
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_auth_svc_provider")
public class AuthorizedServiceProviderVO extends BeanDataVO {

	private static final long serialVersionUID = 5277512713759693823L;

	// Member Variables
	private String authorizedServiceCenterId;
	private String locationId;
	private String productCategoryId;
	private int certifiedFlag;
	private Date contractEndDate;
	private Date createDate;
	private Date updateDate;

	// Bean Sub-Elements
	private ProductCategoryVO category;
	private ProviderLocationVO location;

	public AuthorizedServiceProviderVO() {
		super();
	}

	/**
	 * @param req
	 */
	public AuthorizedServiceProviderVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public AuthorizedServiceProviderVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the authorizedServiceCenterId
	 */
	@Column(name="auth_svc_provider_id", isPrimaryKey=true)
	public String getAuthorizedServiceCenterId() {
		return authorizedServiceCenterId;
	}

	/**
	 * @return the locationId
	 */
	@Column(name="location_id")
	public String getLocationId() {
		return locationId;
	}

	/**
	 * @return the productCategoryId
	 */
	@Column(name="product_category_id")
	public String getProductCategoryId() {
		return productCategoryId;
	}

	/**
	 * @return the certifiedFlag
	 */
	@Column(name="certified_flg")
	public int getCertifiedFlag() {
		return certifiedFlag;
	}

	/**
	 * @return the contractEndDate
	 */
	@Column(name="contract_end_dt")
	public Date getContractEndDate() {
		return contractEndDate;
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
	 * @return the category
	 */
	public ProductCategoryVO getCategory() {
		return category;
	}

	/**
	 * @return the location
	 */
	public ProviderLocationVO getLocation() {
		return location;
	}

	/**
	 * @param authorizedServiceCenterId the authorizedServiceCenterId to set
	 */
	public void setAuthorizedServiceCenterId(String authorizedServiceCenterId) {
		this.authorizedServiceCenterId = authorizedServiceCenterId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @param productCategoryId the productCategoryId to set
	 */
	public void setProductCategoryId(String productCategoryId) {
		this.productCategoryId = productCategoryId;
	}

	/**
	 * @param certifiedFlag the certifiedFlag to set
	 */
	public void setCertifiedFlag(int certifiedFlag) {
		this.certifiedFlag = certifiedFlag;
	}

	/**
	 * @param contractEndDate the contractEndDate to set
	 */
	public void setContractEndDate(Date contractEndDate) {
		this.contractEndDate = contractEndDate;
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
	 * @param category the category to set
	 */
	@BeanSubElement
	public void setCategory(ProductCategoryVO category) {
		this.category = category;
	}

	/**
	 * @param location the location to set
	 */
	@BeanSubElement
	public void setLocation(ProviderLocationVO location) {
		this.location = location;
	}
}