package com.biomed.smarttrak.vo;

import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.vo.NoteInterface;
import com.biomed.smarttrak.vo.NoteVO;
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
public class CompanyAttributeVO implements NoteInterface  {
	private String companyAttributeId;
	private String companyId;
	private String attributeId;
	private String valueText;
	private String titleText;
	private String attributeName;
	private String altText;
	private String groupName;
	private String parentName;
	private List<NoteVO> notes;
	private String statusNo;
	private String sectionId;
	
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
		altText = req.getParameter("altText");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		statusNo = req.getParameter("statusNo");
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
	

	@Column(name="attribute_nm", isReadOnly=true)
	public String getAttributeName() {
		return attributeName;
	}


	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}


	/* (non-Javadoc)
	 * @see com.bmg.admin.vo.NoteEntityInterface#setNotes(java.util.List)
	 */
	@Override
	public void setNotes(List<NoteVO> notes) {
		this.notes= notes;
	}


	/* (non-Javadoc)
	 * @see com.bmg.admin.vo.NoteEntityInterface#getId()
	 */
	@Override
	public String getId() {
		return this.attributeId;
	}


	/* (non-Javadoc)
	 * @see com.bmg.admin.vo.NoteEntityAttributeInterface#getNotes()
	 */
	@Override
	public List<NoteVO> getNotes() {
		return this.notes;
	}


	@Column(name="alt_title_txt")
	public String getAltText() {
		return altText;
	}


	public void setAltText(String altText) {
		this.altText = altText;
	}


	public String getGroupName() {
		return groupName;
	}


	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}


	@Column(name="parent_nm", isReadOnly=true)
	public String getParentName() {
		return parentName;
	}


	public void setParentName(String parentName) {
		this.parentName = parentName;
	}


	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}


	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}

	@Column(name="section_id", isReadOnly=true)
	public String getSectionId() {
		return sectionId;
	}


	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

}
