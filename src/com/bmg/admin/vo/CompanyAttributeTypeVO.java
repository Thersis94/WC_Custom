package com.bmg.admin.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CompanyAttributeTypeVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * the types of attributes that can be assigned to a company.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 25, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_COMPANY_ATTRIBUTE")
public class CompanyAttributeTypeVO {
	
	private String attributeId;
	private String attributeName;
	private String attributeTypeName;
	private String urlAlias;
	private String parentId;
	private int orderNo;
	private int activeFlag;
	
	public CompanyAttributeTypeVO () {
		// default constructor
	}
	
	public CompanyAttributeTypeVO (SMTServletRequest req) {
		setData(req);
	}
	
	private void setData(SMTServletRequest req) {
		attributeId = req.getParameter("attributeId");
		attributeName = req.getParameter("attributeName");
		attributeTypeName = req.getParameter("attributeTypeName");
		urlAlias = req.getParameter("urlAlias");
		parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		activeFlag = Convert.formatInteger(req.getParameter("activeFlag"));
	}

	@Column(name="attribute_id", isPrimaryKey=true)
	public String getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}
	@Column(name="attribute_nm")
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	@Column(name="type_nm")
	public String getAttributeTypeName() {
		return attributeTypeName;
	}
	public void setAttributeTypeName(String attributeTypeName) {
		this.attributeTypeName = attributeTypeName;
	}
	@Column(name="url_alias_txt")
	public String getUrlAlias() {
		return urlAlias;
	}
	public void setUrlAlias(String urlAlias) {
		this.urlAlias = urlAlias;
	}
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Column(name="display_order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}
}
