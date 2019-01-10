package com.biomed.smarttrak.vo;

import java.util.Comparator;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProductAttributeVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * the types of attributes that can be assigned to a product.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 31, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_PRODUCT_ATTRIBUTE_XR")
public class ProductAttributeVO {
	
	private String productAttributeId;
	private String productAttributeGroupId;
	private String attributeId;
	private String attributeName;
	private String productId;
	private String valueText;
	private String titleText;
	private String altText;
	private String attributeTypeCd;
	private String groupName;
	private String statusNo;
	private String revisionNote;
	private int orderNo;
	private int hasArchives;
	private String authorNm;

	private Date createDt;
	private Date updateDt;

	
	/**
	 * Special comparator used in ordering the detail attributes
	 */
	public static final Comparator<ProductAttributeVO> detailComparator = (ProductAttributeVO a1, ProductAttributeVO a2)->
		StringUtil.checkVal(a1.getAttributeName()).compareTo(StringUtil.checkVal(a2.getAttributeName()));
	
	public ProductAttributeVO() {
		// Empty default constructor
	}
	
	
	public ProductAttributeVO(ActionRequest req) {
		setData(req);
	}
	
	
	public void setData(ActionRequest req) {
		productAttributeId = req.getParameter("productAttributeId");
		productAttributeGroupId = req.getParameter("productAttributeGroupId");
		productId = req.getParameter("productId");
		attributeId = req.getParameter("attributeId");
		valueText = req.getParameter("valueText");
		titleText = req.getParameter("titleText");
		altText = req.getParameter("altText");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		statusNo = req.getParameter("statusNo");
		revisionNote = req.getParameter("revisionNote");
		authorNm = req.getParameter("authorNm");
	}


	@Column(name="product_attribute_id", isPrimaryKey=true)
	public String getProductAttributeId() {
		return productAttributeId;
	}
	public void setProductAttributeId(String productAttributeId) {
		this.productAttributeId = productAttributeId;
	}
	@Column(name="product_attribute_group_id")
	public String getProductAttributeGroupId() {
		return productAttributeGroupId;
	}
	public void setProductAttributeGroupId(String productAttributeGroupId) {
		this.productAttributeGroupId = productAttributeGroupId;
	}
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	@Column(name="attribute_id")
	public String getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}
	@Column(name="attribute_nm", isReadOnly=true)
	public String getAttributeName() {
		return attributeName;
	}


	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}


	@Column(name="value_txt")
	public String getValueText() {
		return valueText;
	}
	public void setValueText(String valueText) {
		this.valueText = valueText;
	}
	@Column(name="title_txt")
	public String getTitleText() {
		return titleText;
	}
	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}
	
	@Column(name="alt_title_txt")
	public String getAltText() {
		return altText;
	}


	public void setAltText(String altText) {
		this.altText = altText;
	}


	@Column(name="type_cd", isReadOnly=true)
	public String getAttributeTypeCd() {
		return attributeTypeCd;
	}


	public void setAttributeTypeCd(String attributeTypeCd) {
		this.attributeTypeCd = attributeTypeCd;
	}


	public String getGroupName() {
		return groupName;
	}


	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}


	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}


	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}


	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	/**
	 * @return the hasArchives
	 */
	@Column(name="has_archives", isReadOnly=true)
	public int getHasArchives() {
		return hasArchives;
	}

	/**
	 * @param hasArchives the hasArchives to set.
	 */
	public void setHasArchives(int hasArchives) {
		this.hasArchives = hasArchives;
	}

	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return updateDt;}
	public void setUpdateDate(Date updateDt) {this.updateDt = updateDt;}

	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return createDt;}
	public void setCreateDate(Date createDt) {this.createDt = createDt;}

	/**
	 * @return the revisionNote
	 */
	@Column(name="REVISION_NOTE")
	public String getRevisionNote() {
		return revisionNote;
	}


	/**
	 * @param revisionNote the revisionNote to set.
	 */
	public void setRevisionNote(String revisionNote) {
		this.revisionNote = revisionNote;
	}

	@Column(name="author_nm")
	public String getAuthorNm() {
		return authorNm;
	}

	public void setAuthorNm(String authorNm) {
		this.authorNm = authorNm;
	}

	/**
	 * Helper method that determines proper orderNo value based on titleTxt
	 * business rules.
	 */
	public void calulateOrderNo() {
		int tOrderNo = 100;
		String title = StringUtil.checkVal(titleText).toLowerCase();
		if(title.contains("description")) {
			tOrderNo = 1;
		} else if(title.contains("indication")) {
			tOrderNo = 5;
		} else if(title.contains("clinical") || title.contains("update")) {
			tOrderNo = 10;
		} else if(title.contains("regulatory") || title.contains("status")) {
			tOrderNo = 15;
		} else if(title.contains("published") || title.contains("studies")) {
			tOrderNo = 20;
		} else if(title.contains("reimbursement")) {
			tOrderNo = 25;
		} else if(title.contains("sales") || title.contains("distribution")) {
			tOrderNo = 30;
		}

		orderNo = tOrderNo;
	}
}