package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.wsla.data.provider.ProviderVO;

/****************************************************************************
 * <b>Title</b>: ProductVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the product catalog
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_product_master")
public class ProductVO extends BeanDataVO {

	private static final long serialVersionUID = -737469551561460512L;

	// Member Variables
	private String productId;
	private String providerId;
	private String customerProductId;
	private String secCustomerProductId;
	private String productName;
	private String description;
	private String firmware;
	private Date discontinuedDate;
	private Date supportEndDate;
	private int warrantyDays;
	private int setFlag;
	private int activeFlag;
	private double msrpCostNo;
	private Date createDate;
	private Date updateDate;

	// Bean Sub-Elements
	private List<ProductCategoryVO> categories = new ArrayList<>();
	private List<ProductSetVO> parts = new ArrayList<>();
	private ProviderVO provider;

	/**
	 * 
	 */
	public ProductVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id", isPrimaryKey=true)
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the providerId
	 */
	@Column(name="provider_id")
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the customerProductId
	 */
	@Column(name="cust_product_id")
	public String getCustomerProductId() {
		return customerProductId;
	}

	/**
	 * @return the secCustomerProductId
	 */
	@Column(name="sec_cust_product_id")
	public String getSecCustomerProductId() {
		return secCustomerProductId;
	}

	/**
	 * @return the productName
	 */
	@Column(name="product_nm")
	public String getProductName() {
		return productName;
	}

	/**
	 * @return the firmware
	 */
	@Column(name="firmware_txt")
	public String getFirmware() {
		return firmware;
	}

	/**
	 * @return the discontinuedDate
	 */
	@Column(name="discontinued_dt")
	public Date getDiscontinuedDate() {
		return discontinuedDate;
	}

	/**
	 * @return the supportEndDate
	 */
	@Column(name="support_end_dt")
	public Date getSupportEndDate() {
		return supportEndDate;
	}

	/**
	 * @return the warrantyDays
	 */
	@Column(name="warranty_days_no")
	public int getWarrantyDays() {
		return warrantyDays;
	}

	/**
	 * @return the setFlag
	 */
	@Column(name="set_flg")
	public int getSetFlag() {
		return setFlag;
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
	 * @return the categories
	 */
	public List<ProductCategoryVO> getCategories() {
		return categories;
	}

	/**
	 * @return the parts
	 */
	public List<ProductSetVO> getParts() {
		return parts;
	}

	/**
	 * @return the provider
	 */
	public ProviderVO getProvider() {
		return provider;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @param providerId the providerId to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @param customerProductId the customerProductId to set
	 */
	public void setCustomerProductId(String customerProductId) {
		this.customerProductId = customerProductId;
	}

	/**
	 * @param secCustomerProductId the secCustomerProductId to set
	 */
	public void setSecCustomerProductId(String secCustomerProductId) {
		this.secCustomerProductId = secCustomerProductId;
	}

	/**
	 * @param productName the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/**
	 * @param firmware the firmware to set
	 */
	public void setFirmware(String firmware) {
		this.firmware = firmware;
	}

	/**
	 * @param discontinuedDate the discontinuedDate to set
	 */
	public void setDiscontinuedDate(Date discontinuedDate) {
		this.discontinuedDate = discontinuedDate;
	}

	/**
	 * @param supportEndDate the supportEndDate to set
	 */
	public void setSupportEndDate(Date supportEndDate) {
		this.supportEndDate = supportEndDate;
	}

	/**
	 * @param warrantyDays the warrantyDays to set
	 */
	public void setWarrantyDays(int warrantyDays) {
		this.warrantyDays = warrantyDays;
	}

	/**
	 * @param setFlag the setFlag to set
	 */
	public void setSetFlag(int setFlag) {
		this.setFlag = setFlag;
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
	 * @param categories the categories to set
	 */
	public void setCategories(List<ProductCategoryVO> categories) {
		this.categories = categories;
	}

	/**
	 * 
	 * @param category
	 */
	@BeanSubElement
	public void addCategory(ProductCategoryVO category) {
		if(category != null)
			categories.add(category);
	}

	/**
	 * @param parts the parts to set
	 */
	public void setParts(List<ProductSetVO> parts) {
		this.parts = parts;
	}

	/**
	 * 
	 * @param part
	 */
	@BeanSubElement
	public void addPart(ProductSetVO part) {
		if (part != null)
			parts.add(part);
	}

	/**
	 * @param provider the provider to set
	 */
	@BeanSubElement
	public void setProvider(ProviderVO provider) {
		this.provider = provider;
	}

	@Column(name="desc_txt")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	@Column(name="msrp_cost_no")
	public double getMsrpCostNo() {
		return msrpCostNo;
	}

	public void setMsrpCostNo(double msrpCostNo) {
		this.msrpCostNo = msrpCostNo;
	}
}