package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: CompanyAttributeVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * attributes relating to a company.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 17, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_COMPANY_ATTRIBUTE_XR")
public class CompanyAttributeVO {
	private String companyAttributeId;
	private String companyId;
	private String attributeId;
	private String valueText;
	private String titleText;
	private int orderNo;
	private String attributeTypeName;
	
	public CompanyAttributeVO() {
		// Empty default constructor
	}
	
	
	public CompanyAttributeVO(ActionRequest req) {
		setData(req);
	}
	
	
	public void setData(ActionRequest req) {
		companyAttributeId = req.getParameter("companyAttributeId");
		companyId = req.getParameter("companyId");
		attributeId = req.getParameter("attributeId");
		valueText = req.getParameter("valueText");
		titleText = req.getParameter("titleText");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
	}


	@Column(name="company_attribute_id", isPrimaryKey=true)
	public String getCompanyAttributeId() {
		return companyAttributeId;
	}
	public void setCompanyAttributeId(String companyAttributeId) {
		this.companyAttributeId = companyAttributeId;
	}
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
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
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	

	@Column(name="type_nm", isReadOnly=true)
	public String getAttributeTypeName() {
		return attributeTypeName;
	}
	public void setAttributeTypeName(String attributeTypeName) {
		this.attributeTypeName = attributeTypeName;
	}
	

	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

}
