package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ProductAttributeTypeVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * the types of attributes that can be assigned to a product.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 30, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_PRODUCT_ATTRIBUTE")
public class ProductAttributeTypeVO {
	
	private String attributeId;
	private String attributeName;
	private String parentId;
	private String typeCd;
	private String abbrName;
	private int orderNo;
	private int activeFlag;
	private List<String> sectionIds;
	
	public ProductAttributeTypeVO () {
		this.sectionIds = new ArrayList<>();
	}
	
	public ProductAttributeTypeVO (ActionRequest req) {
		setData(req);
	}
	
	protected void setData(ActionRequest req) {
		attributeId = req.getParameter("attributeId");
		attributeName = req.getParameter("attributeName");
		parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
		typeCd = req.getParameter("typeCd");
		abbrName = req.getParameter("abbrName");
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
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Column(name="type_cd")
	public String getTypeCd() {
		return typeCd;
	}
	public void setTypeCd(String typeCd) {
		this.typeCd = typeCd;
	}

	@Column(name="abbr_name")
	public String getAbbrName() {
		return abbrName;
	}
	public void setAbbrName(String abbrName) {
		this.abbrName = abbrName;
	}

	@Column(name="order_no")
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
	public List<String> getSectionIds() {
		return sectionIds;
	}
	public void setSectionIds(List<String> sectionIds) {
		this.sectionIds = sectionIds;
	}
	public void addSectionIds(String sectionIds) {
		if (sectionIds == null) return;
		for (String sectionId : sectionIds.split(",")) 
			this.sectionIds.add(sectionId);
	}
	public String getSectionIdClassList() {
		StringBuilder classes = new StringBuilder();
		for (String s : sectionIds) {
			classes.append(s).append(" ");
		}
		return classes.toString();
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

	/**
	 * @param rs
	 */
	protected void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		attributeId = db.getStringVal("attribute_id", rs);
		parentId = db.getStringVal("parent_id", rs);
		attributeName = db.getStringVal("attribute_nm", rs);
		activeFlag = db.getIntVal("active_flg", rs);
		orderNo = db.getIntVal("order_no", rs);
		typeCd = db.getStringVal("type_cd", rs);
		abbrName = db.getStringVal("abbr_nm", rs);
	}
}
