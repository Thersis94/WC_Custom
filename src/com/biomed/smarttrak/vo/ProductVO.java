package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.util.BiomedProductIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ProductVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores all information about
 * a product.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 17, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_PRODUCT")
public class ProductVO extends AuthorVO {
	
	private String productId;
	private String parentId;
	private String companyId;
	private String companyName;
	private String companyShortName;
	private String productName;
	private int orderNo;
	private String metaKeyword;
	private String metaDesc;
	private int gaFlag;
	private String refText;
	private String authorProfileId;
	private String statusNo;
	private String shortName;
	private String aliasName;
	private String productGroupId;
	private List<ProductAttributeVO> productAttributes;
	private List<SectionVO> productSections;
	private List<ProductAllianceVO> alliances;
	private List<RegulationVO> regulations;
	private Map<String, List<ProductAttributeVO>> details;
	private Map<String, List<ProductVO>> relatedProducts;
	private Node[] detailsList;
	private int publicFlag;
	
	public ProductVO () {
		super(BiomedProductIndexer.INDEX_TYPE);
		productAttributes = new ArrayList<>();
		productSections = new ArrayList<>();
		alliances = new ArrayList<>();
		regulations = new ArrayList<>();
		details = new HashMap<>();
		relatedProducts = new HashMap<>();
	}
	
	
	public ProductVO(ActionRequest req) {
		this();
		setData(req);
	}
	

	protected void setData(ActionRequest req) {
		super.setData(req); //set the creator_profile_id
		productId = req.getParameter("productId");
		parentId = req.getParameter("parentId");
		companyId = req.getParameter("companyId");
		productName = req.getParameter("productName");
		shortName = req.getParameter("shortName");
		aliasName = req.getParameter("aliasName");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		metaKeyword = req.getParameter("metaKeyword");
		metaDesc = req.getParameter("metaDesc");
		gaFlag = Convert.formatInteger(req.getParameter("gaFlag"));
		refText = req.getParameter("refText");
		authorProfileId = req.getParameter("authorProfileId");
		statusNo = req.getParameter("statusNo");
		productGroupId = req.getParameter("productGroupId");
		setPublicFlag(Convert.formatInteger(req.getParameter("publicFlag")));
	}


	@Column(name="product_id", isPrimaryKey=true)
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		super.setDocumentId(productId);
		this.productId = productId;
	}
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	@Column(name="company_nm", isReadOnly=true)
	public String getCompanyName() {
		return companyName;
	}


	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}


	@Column(name="short_nm_txt", isReadOnly=true)
	public String getCompanyShortName() {
		return companyShortName;
	}


	public void setCompanyShortName(String companyShortName) {
		this.companyShortName = companyShortName;
	}


	@Column(name="product_nm")
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	@Column(name="meta_kywd_txt")
	public String getMetaKeyword() {
		return metaKeyword;
	}
	public void setMetaKeyword(String metaKeyword) {
		this.metaKeyword = metaKeyword;
	}
	@Column(name="meta_desc")
	public String getMetaDesc() {
		return metaDesc;
	}
	public void setMetaDesc(String metaDesc) {
		this.metaDesc = metaDesc;
	}
	@Column(name="ga_usage_flg")
	public int getGaFlag() {
		return gaFlag;
	}
	public void setGaFlag(int gaFlag) {
		this.gaFlag = gaFlag;
	}
	@Column(name="reference_txt")
	public String getRefText() {
		return refText;
	}
	public void setRefText(String refText) {
		this.refText = refText;
	}
	@Column(name="author_profile_id")
	public String getAuthorProfileId() {
		return authorProfileId;
	}
	public void setAuthorProfileId(String authorProfileId) {
		this.authorProfileId = authorProfileId;
	}
	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}
	@Column(name="short_nm")
	public String getShortName() {
		return shortName;
	}


	public void setShortName(String shortName) {
		this.shortName = shortName;
	}


	@Column(name="alias_nm")
	public String getAliasName() {
		return aliasName;
	}


	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}


	@Column(name="product_group_id")
	public String getProductGroupId() {
		return productGroupId;
	}
	public void setProductGroupId(String productGroupId) {
		this.productGroupId = productGroupId;
	}
	

	public List<ProductAttributeVO> getProductAttributes() {
		return productAttributes;
	}


	public void setProductAttributes(List<ProductAttributeVO> attributes) {
		this.productAttributes = attributes;
	}
	
	public void addProductAttribute(ProductAttributeVO attribute) {
		this.productAttributes.add(attribute);
	}

	public List<SectionVO> getProductSections() {
		return productSections;
	}


	public void setProductSections(List<SectionVO> sections) {
		this.productSections = sections;
	}
	
	public void addProductSection(SectionVO section) {
		this.productSections.add(section);
	}


	public List<ProductAllianceVO> getAlliances() {
		return alliances;
	}


	public void setAlliances(List<ProductAllianceVO> alliances) {
		this.alliances = alliances;
	}
	
	public void addAlliance(ProductAllianceVO alliance) {
		this.alliances.add(alliance);
	}


	public List<RegulationVO> getRegulations() {
		return regulations;
	}


	public void setRegulations(List<RegulationVO> regulations) {
		this.regulations = regulations;
	}
	
	public void addRegulation(RegulationVO regulation) {
		this.regulations.add(regulation);
	}


	public Map<String, List<ProductAttributeVO>> getDetails() {
		return details;
	}


	public void setDetails(Map<String, List<ProductAttributeVO>> details) {
		this.details = details;
	}
	
	public void addDetail(String key, ProductAttributeVO detail) {
		if (!details.containsKey(key)) details.put(key, new ArrayList<ProductAttributeVO>());
		details.get(key).add(detail);
	}


	public Map<String, List<ProductVO>> getRelatedProducts() {
		return relatedProducts;
	}


	public void setRelatedProducts(Map<String, List<ProductVO>> relatedProducts) {
		this.relatedProducts = relatedProducts;
	}
	
	public void addRelatedProduct(String key, ProductVO product) {
		if (!relatedProducts.containsKey(key)) relatedProducts.put(key, new ArrayList<ProductVO>());
		relatedProducts.get(key).add(product);
	}
	
	public String getAllSections() {
		StringBuilder sections = new StringBuilder(200);
		for (SectionVO section : productSections) {
			sections.append(section.getSectionId()).append("|");
		}
		return sections.toString();
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}
	

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (this == obj)
			return true;

		if (this.getClass() != obj.getClass())
			return false;

		ProductVO p = (ProductVO) obj;

		// Check to see if the core data for these products are equivalent
		if ((productId == null && p.getProductId() != null) || (productId != null && p.getProductId() == null)) return false;
		if ((productId != null && p.getProductId() != null) && !productId.equals(p.getProductId())) return false;
		if ((parentId == null && p.getParentId() != null) || (parentId != null && p.getParentId() == null)) return false;
		if ((parentId != null && p.getParentId() != null) && parentId.equals(p.getParentId())) return false;
		if ((companyId == null && p.getCompanyId() != null) || (companyId != null && p.getCompanyId() == null)) return false;
		if ((companyId != null && p.getCompanyId() != null) && !companyId.equals(p.getCompanyId())) return false;
		if ((companyName == null && p.getCompanyName() != null) || (companyName != null && p.getCompanyName() == null)) return false;
		if ((companyName != null && p.getCompanyName() != null) && !companyName.equals(p.getCompanyName())) return false;

		// In all situations where equivalency is used for product VOs only
		// the core data is used. Therefore if it reaches here the two
		// products are equal or close enough that using either one for its
		// intended purpose will get the same results.
		return true;
	}


	public Node[] getDetailsList() {
		return detailsList;
	}


	public void setDetailsList(Node[] detailsList) {
		this.detailsList = detailsList;
	}

	@Column(name="public_flg")
	public int getPublicFlag() {
		return publicFlag;
	}


	public void setPublicFlag(int publicFlag) {
		this.publicFlag = publicFlag;
	}
	
	/**
	 * @return the creatorProfileId
	 */
	@Column(name="creator_profile_id")
	public String getCreatorProfileId() {
		return creatorProfileId;
	}
	
}