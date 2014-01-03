package com.orthopediatrics.action;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: SRSFieldVO.java <p/>
 * <b>Project</b>: SB_Orthopediatrics <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 6, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SRSFieldVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private String fieldId = null;
	private String fieldName = null;
	private String fieldValue = null;
	private String fieldLabel = null;
	private String fieldTypeName = null;
	private String dataTypeName = null;
	private String dataTypeId = null;
	private int fieldTypeId = 0;
	private int maxRange = 0;
	
	/**
	 * 
	 */
	public SRSFieldVO() {
		
	}

	public SRSFieldVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	public SRSFieldVO(SMTServletRequest req) {
		this.assignData(req);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		fieldId = db.getStringVal("field_id", rs);
		fieldName = db.getStringVal("field_nm", rs);
		fieldValue = db.getStringVal("field_value_txt", rs);
		fieldLabel = db.getStringVal("field_label_txt", rs);
		fieldTypeName = db.getStringVal("field_type_nm", rs);
		dataTypeId = db.getStringVal("data_type_id", rs);
		dataTypeName = db.getStringVal("data_type_nm", rs);
		fieldTypeId = db.getIntVal("field_type_id", rs);
		maxRange = db.getIntVal("max_range_no", rs);
	}
	
	/**
	 * 
	 * @param req
	 */
	public void assignData(SMTServletRequest req) {
		actionId = req.getParameter("sbActionId");
		fieldName = req.getParameter("fieldName");
		fieldId = req.getParameter("fieldId");
		fieldValue = req.getParameter("fieldValue");
		fieldTypeName = req.getParameter("fieldTypeName");
		fieldLabel = req.getParameter("fieldLabel");
		dataTypeId = req.getParameter("dataTypeId");
		dataTypeName = req.getParameter("dataTypeName");
		maxRange = Convert.formatInteger(req.getParameter("maxRange"));
		fieldTypeId = Convert.formatInteger(req.getParameter("fieldTypeId"));
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

	/**
	 * @return the fieldValue
	 */
	public String getFieldValue() {
		return fieldValue;
	}

	/**
	 * @param fieldValue the fieldValue to set
	 */
	public void setFieldValue(String fieldValue) {
		this.fieldValue = fieldValue;
	}

	/**
	 * @return the fieldTypeName
	 */
	public String getFieldTypeName() {
		return fieldTypeName;
	}

	/**
	 * @param fieldTypeName the fieldTypeName to set
	 */
	public void setFieldTypeName(String fieldTypeName) {
		this.fieldTypeName = fieldTypeName;
	}

	/**
	 * @return the filedTypeId
	 */
	public int getFieldTypeId() {
		return fieldTypeId;
	}

	/**
	 * @param filedTypeId the filedTypeId to set
	 */
	public void setFieldTypeId(int filedTypeId) {
		this.fieldTypeId = filedTypeId;
	}

	/**
	 * @return the dynamicFlag
	 */
	public int getMaxRange() {
		return maxRange;
	}

	/**
	 * @param dynamicFlag the dynamicFlag to set
	 */
	public void setMaxRange(int dynamicFlag) {
		this.maxRange = dynamicFlag;
	}


	/**
	 * @return the fieldId
	 */
	public String getFieldId() {
		return fieldId;
	}


	/**
	 * @param fieldId the fieldId to set
	 */
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}

	/**
	 * @return the fieldLabel
	 */
	public String getFieldLabel() {
		return fieldLabel;
	}

	/**
	 * @param fieldLabel the fieldLabel to set
	 */
	public void setFieldLabel(String fieldLabel) {
		this.fieldLabel = fieldLabel;
	}

	/**
	 * @return the dataTypeName
	 */
	public String getDataTypeName() {
		return dataTypeName;
	}

	/**
	 * @param dataTypeName the dataTypeName to set
	 */
	public void setDataTypeName(String dataTypeName) {
		this.dataTypeName = dataTypeName;
	}

	/**
	 * @return the dataTypeId
	 */
	public String getDataTypeId() {
		return dataTypeId;
	}

	/**
	 * @param dataTypeId the dataTypeId to set
	 */
	public void setDataTypeId(String dataTypeId) {
		this.dataTypeId = dataTypeId;
	}

}
