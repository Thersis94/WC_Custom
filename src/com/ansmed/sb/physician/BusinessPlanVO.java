package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: BusinessPlanVO.java</p>
 <p>Description: <b/>Stores the data for a Business Plan Object</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 13, 2007
 Last Updated:
 ***************************************************************************/

public class BusinessPlanVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String surgeonId = null;
	private String businessPlanId = null;
	private String valueText = null;
	private String fieldName = null;
	private Integer selectedFlag = null;
	private Integer bpYear = null;
	private Integer bpQuarter = null;
	
	/**
	 * 
	 */
	public BusinessPlanVO() {
		
	}
	
	
	public BusinessPlanVO(ResultSet rs) {
		super();
		this.setData(rs);
	}
	
	/**
	 * Assigns a row of data to the appropriate vars
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		actionId = db.getStringVal("surgeon_busplan_id", rs);
		surgeonId = db.getStringVal("surgeon_id", rs);
		businessPlanId = db.getStringVal("business_plan_id", rs);
		valueText = db.getStringVal("value_txt", rs);
		selectedFlag = db.getIntegerVal("selected_flg", rs);
		bpYear = db.getIntegerVal("bp_year_no", rs);
		bpQuarter = db.getIntegerVal("bp_quarter_no", rs);
		fieldName = db.getStringVal("field_nm", rs);
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the businessPlanId
	 */
	public String getBusinessPlanId() {
		return businessPlanId;
	}

	/**
	 * @param businessPlanId the businessPlanId to set
	 */
	public void setBusinessPlanId(String businessPlanId) {
		this.businessPlanId = businessPlanId;
	}

	/**
	 * @return the valueText
	 */
	public String getValueText() {
		return valueText;
	}

	/**
	 * @param valueText the valueText to set
	 */
	public void setValueText(String valueText) {
		this.valueText = valueText;
	}

	/**
	 * @return the selectedFlag
	 */
	public Integer getSelectedFlag() {
		return selectedFlag;
	}

	/**
	 * @param selectedFlag the selectedFlag to set
	 */
	public void setSelectedFlag(Integer selectedFlag) {
		this.selectedFlag = selectedFlag;
	}

	/**
	 * @return the bpQuarter
	 */
	public Integer getBpQuarter() {
		return bpQuarter;
	}
	
	/**
	 * @param the bpQuarter
	 */
	public void setBpQuarter(Integer bpQuarter) {
		this.bpQuarter = bpQuarter;
	}
	
	/**
	 * @return the bpYear
	 */
	public Integer getBpYear() {
		return bpYear;
	}

	/**
	 * @param bpYear the bpYear to set
	 */
	public void setBpYear(Integer bpYear) {
		this.bpYear = bpYear;
	}
	
	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}


	/**
	 * @param fieldName the fieldName to set
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	

}
