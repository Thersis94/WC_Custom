package com.biomed.smarttrak.vo;

import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.vo.NoteInterface;
import com.biomed.smarttrak.vo.NoteVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
	private String companyAttributeGroupId;
	private String companyId;
	private String attributeId;
	private String valueText;
	private String titleText;
	private String attributeName;
	private String altText;
	private String groupName;
	private String parentName;
	private String revisionNote;
	private List<NoteVO> notes;
	private String statusNo;
	private String sectionId;
	private String authorNm;

	private int hasArchives;
	private int orderNo;
	private String attributeTypeName;
	private Date createDt;
	private Date updateDt;
	
	public CompanyAttributeVO() {
		// Empty default constructor
	}
	
	
	public CompanyAttributeVO(ActionRequest req) {
		setData(req);
	}
	
	
	public void setData(ActionRequest req) {
		companyAttributeId = req.getParameter("companyAttributeId");
		companyAttributeGroupId = req.getParameter("companyAttributeGroupId");
		companyId = req.getParameter("companyId");
		attributeId = req.getParameter("attributeId");
		valueText = req.getParameter("valueText");
		titleText = req.getParameter("titleText");
		altText = req.getParameter("altText");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		statusNo = req.getParameter("statusNo");
		revisionNote = req.getParameter("revisionNote");
		authorNm = req.getParameter("authorNm");
	}


	@Column(name="company_attribute_id", isPrimaryKey=true)
	public String getCompanyAttributeId() {
		return companyAttributeId;
	}
	public void setCompanyAttributeId(String companyAttributeId) {
		this.companyAttributeId = companyAttributeId;
	}
	@Column(name="company_attribute_group_id")
	public String getCompanyAttributeGroupId() {
		return companyAttributeGroupId;
	}
	public void setCompanyAttributeGroupId(String companyAttributeGroupId) {
		this.companyAttributeGroupId = companyAttributeGroupId;
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

	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return updateDt;}
	public void setUpdateDate(Date updateDt) {this.updateDt = updateDt;}

	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return createDt;}
	public void setCreateDate(Date createDt) {this.createDt = createDt;}
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
		if(title.contains("overview")) {
			tOrderNo = 1;
		} else if(title.contains("funding")) {
			tOrderNo = 5;
		} else if(title.contains("revenues") || title.contains("earnings")) {
			tOrderNo = 10;
		} else if(title.contains("recent") || title.contains("commentary")) {
			tOrderNo = 15;
		} else if(title.contains("technology platform")) {
			tOrderNo = 20;
		} else if(title.contains("product")) {
			tOrderNo = 25;
		} else if(title.contains("intellectual property")) {
			tOrderNo = 30;
		} else if(title.contains("strategic alliances")) {
			tOrderNo = 35;
		}

		orderNo = tOrderNo;
	}
}