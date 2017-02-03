/**
 *
 */
package com.biomed.smarttrak.admin.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: GapColumnVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO For managing Gap Analysis Column Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 1, 2017
 ****************************************************************************/
@Table(name="biomedgps_ga_column")
public class GapColumnVO implements Serializable {

	private static final long serialVersionUID = 4972635452116906666L;
	private String gaColumnId;
	private String sectionId;
	private String columnNm;
	private int orderNo;
	private String buttonTxt;
	private String specialRulesTxt;
	private Date createDt;
	private Date updateDt;
	private List<GapColumnAttributeVO> attributes;
	public GapColumnVO() {
		attributes = new ArrayList<>();
	}

	public GapColumnVO(ActionRequest req) {
		this();
		setData(req);
	}

	public GapColumnVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public void setData(ActionRequest req) {
		gaColumnId = req.getParameter("gaColumnId");
		sectionId = req.getParameter("sectionId");
		columnNm = req.getParameter("columnNm");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		buttonTxt = req.getParameter("buttonTxt");
		specialRulesTxt = req.getParameter("specialRulesTxt");
	}

	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		gaColumnId = db.getStringVal("ga_column_id", rs);
		sectionId = db.getStringVal("section_id", rs);
		columnNm = db.getStringVal("column_nm", rs);
		orderNo = db.getIntVal("order_no", rs);
		buttonTxt = db.getStringVal("button_txt", rs);
		specialRulesTxt = db.getStringVal("special_rules_txt", rs);
	}

	/**
	 * @return the gaColumnId
	 */
	@Column(name="ga_column_id", isPrimaryKey=true)
	public String getGaColumnId() {
		return gaColumnId;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="section_id")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the columnNm
	 */
	@Column(name="column_nm")
	public String getColumnNm() {
		return columnNm;
	}

	/**
	 * @return the orderNo
	 */
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	/**
	 * @return the buttonTxt
	 */
	@Column(name="button_txt")
	public String getButtonTxt() {
		return buttonTxt;
	}

	/**
	 * @return the specialRulesTxt
	 */
	@Column(name="special_rules_txt")
	public String getSpecialRulesTxt() {
		return specialRulesTxt;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * 
	 * @return the attributes
	 */
	public List<GapColumnAttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @param gaColumnId the gaColumnId to set.
	 */
	public void setGaColumnId(String gaColumnId) {
		this.gaColumnId = gaColumnId;
	}

	/**
	 * @param sectionId the sectionId to set.
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param columnNm the columnNm to set.
	 */
	public void setColumnNm(String columnNm) {
		this.columnNm = columnNm;
	}

	/**
	 * @param orderNo the orderNo to set.
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	/**
	 * @param buttonTxt the buttonTxt to set.
	 */
	public void setButtonTxt(String buttonTxt) {
		this.buttonTxt = buttonTxt;
	}

	/**
	 * @param specialRulesTxt the specialRulesTxt to set.
	 */
	public void setSpecialRulesTxt(String specialRulesTxt) {
		this.specialRulesTxt = specialRulesTxt;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * 
	 * @param attributes the attributes to set.
	 */
	public void setAttributes(List<GapColumnAttributeVO> attributes) {
		this.attributes = attributes;
	}

	/**
	 * 
	 * @param attribute the attribute to add.
	 */
	public void addAttribute(GapColumnAttributeVO attribute) {
		this.attributes.add(attribute);
	}
}