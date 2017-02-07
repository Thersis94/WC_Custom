package com.bmg.admin.vo;

import java.util.Date;
import java.util.List;

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
	private String attr1Text;
	private String attr2Text;
	private String attr3Text;
	private List<NoteVO> notes;
	
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
		attr1Text = req.getParameter("attr1Text");
		attr2Text = req.getParameter("attr2Text");
		attr3Text = req.getParameter("attr3Text");
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

}
