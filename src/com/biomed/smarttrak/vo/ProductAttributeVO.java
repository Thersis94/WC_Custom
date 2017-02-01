package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

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
	private String attributeId;
	private String productId;
	private String valueText;
	private String titleText;
	private String attr1Text;
	private String attr2Text;
	private String attr3Text;
	private String attributeTypeCd;
	private int orderNo;
	
	public ProductAttributeVO() {
		// Empty default constructor
	}
	
	
	public ProductAttributeVO(ActionRequest req) {
		setData(req);
	}
	
	
	public void setData(ActionRequest req) {
		productAttributeId = req.getParameter("productAttributeId");
		productId = req.getParameter("productId");
		attributeId = req.getParameter("attributeId");
		valueText = req.getParameter("valueText");
		titleText = req.getParameter("titleText");
		attr1Text = req.getParameter("attr1Text");
		attr2Text = req.getParameter("attr2Text");
		attr3Text = req.getParameter("attr3Text");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
	}


	@Column(name="product_attribute_id", isPrimaryKey=true)
	public String getProductAttributeId() {
		return productAttributeId;
	}
	public void setProductAttributeId(String productAttributeId) {
		this.productAttributeId = productAttributeId;
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
	@Column(name="attrib1_txt")
	public String getAttr1Text() {
		return attr1Text;
	}
	public void setAttr1Text(String attr1Text) {
		this.attr1Text = attr1Text;
	}
	@Column(name="attrib2_txt")
	public String getAttr2Text() {
		return attr2Text;
	}
	public void setAttr2Text(String attr2Text) {
		this.attr2Text = attr2Text;
	}
	@Column(name="attrib3_txt")
	public String getAttr3Text() {
		return attr3Text;
	}
	public void setAttr3Text(String attr3Text) {
		this.attr3Text = attr3Text;
	}
	@Column(name="type_cd", isReadOnly=true)
	public String getAttributeTypeCd() {
		return attributeTypeCd;
	}


	public void setAttributeTypeCd(String attributeTypeCd) {
		this.attributeTypeCd = attributeTypeCd;
	}


	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	

	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

}
