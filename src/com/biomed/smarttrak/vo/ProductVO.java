package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
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
public class ProductVO {
	
	private String productId;
	private String parentId;
	private String companyId;
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
	private List<ProductAttributeVO> attributes;
	private List<GenericVO> sections;
	
	public ProductVO () {
		attributes = new ArrayList<>();
		sections = new ArrayList<>();
	}
	
	
	public ProductVO(ActionRequest req) {
		super();
		setData(req);
	}
	

	private void setData(ActionRequest req) {
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
	}


	@Column(name="product_id", isPrimaryKey=true)
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
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
	

	public List<ProductAttributeVO> getAttributes() {
		return attributes;
	}


	public void setAttributes(List<ProductAttributeVO> attributes) {
		this.attributes = attributes;
	}
	
	public void addAttribute(ProductAttributeVO attribute) {
		this.attributes.add(attribute);
	}

	public List<GenericVO> getSections() {
		return sections;
	}


	public void setSections(List<GenericVO> sections) {
		this.sections = sections;
	}
	
	public void addSection(GenericVO section) {
		this.sections.add(section);
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

	
}
