package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

public class ExerciseAttributeVO implements Serializable {

	private static final long serialVersionUID = 1234551234L;
	private String exerciseAttributeId = null;
	private String exerciseIntensityId = null;
	private String labelText = null;
	private String unitText = null;
	private String htmlTypeName = null;
	private String defaultValueText = null;
	private Map<Integer, String> values;
	
	public ExerciseAttributeVO() {	
		
	}
	
	public ExerciseAttributeVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		exerciseAttributeId = db.getStringVal("attribute_id", rs);
		exerciseIntensityId = db.getStringVal("exercise_intensity_id", rs);
		labelText = db.getStringVal("label_Txt", rs);
		unitText = db.getStringVal("unit_Txt", rs);
		htmlTypeName = db.getStringVal("html_type_nm", rs);
		defaultValueText = db.getStringVal("default_value_txt", rs);
		values = new HashMap<Integer, String>();
		db = null;
	}
	
	public ExerciseAttributeVO(SMTServletRequest req) {
		exerciseAttributeId = req.getParameter("exerciseAttributeId");
		exerciseIntensityId = req.getParameter("exerciseIntensityId");
		labelText = req.getParameter("labelText");
		unitText = req.getParameter("unitText");
		htmlTypeName = req.getParameter("htmlTypeName");
		defaultValueText = req.getParameter("defaultValueText");
		values = new HashMap<Integer, String>();
	}
	
	public ExerciseAttributeVO(SMTServletRequest req, int iLvl, int count) {
		setData(req, iLvl, count);
	}
	
	public void setData(SMTServletRequest req, int iLvl, int count) {
		exerciseAttributeId = req.getParameter("exerciseAttributeId_" + iLvl + "_" + count);
		exerciseIntensityId = req.getParameter("exerciseIntensityId_" + iLvl);
		labelText = req.getParameter("labelText_" + iLvl + "_" + count);
		unitText = req.getParameter("unitText_" + iLvl + "_" + count);
		htmlTypeName = req.getParameter("htmlTypeName_" + iLvl + "_" + count);
		defaultValueText = req.getParameter("defaultValueText_" + iLvl + "_" + count);
	}

	/**
	 * @return the exerciseAttributeId
	 */
	public String getExerciseAttributeId() {
		return exerciseAttributeId;
	}

	/**
	 * @param exerciseAttributeId the exerciseAttributeId to set
	 */
	public void setExerciseAttributeId(String exerciseAttributeId) {
		this.exerciseAttributeId = exerciseAttributeId;
	}

	/**
	 * @return the exerciseIntensityId
	 */
	public String getExerciseIntensityId() {
		return exerciseIntensityId;
	}

	/**
	 * @param exerciseID the exerciseIntensityId to set
	 */
	public void setExerciseIntensityId(String exerciseIntensityId) {
		this.exerciseIntensityId = exerciseIntensityId;
	}

	/**
	 * @return the labelText
	 */
	public String getLabelText() {
		return labelText;
	}

	/**
	 * @param labelText the labelText to set
	 */
	public void setLabelText(String labelText) {
		this.labelText = labelText;
	}

	/**
	 * @return the unitText
	 */
	public String getUnitText() {
		return unitText;
	}

	/**
	 * @param unitText the unitText to set
	 */
	public void setUnitText(String unitText) {
		this.unitText = unitText;
	}

	/**
	 * @return the htmlTypeName
	 */
	public String getHtmlTypeName() {
		return htmlTypeName;
	}

	/**
	 * @param htmlTypeName the htmlTypeName to set
	 */
	public void setHtmlTypeName(String htmlTypeName) {
		this.htmlTypeName = htmlTypeName;
	}

	/**
	 * @return the defaultValueText
	 */
	public String getDefaultValueText() {
		return defaultValueText;
	}

	/**
	 * @param defaultValueText the defaultValueText to set
	 */
	public void setDefaultValueText(String defaultValueText) {
		this.defaultValueText = defaultValueText;
	}
	
	public boolean equals(ExerciseAttributeVO aVo) {
		if(aVo != null)
		return this.exerciseAttributeId.equals(aVo.exerciseAttributeId);
		
		return false;
	}
	
	public Map<Integer, String> getValues(){
		return values;
	}
	
	public void setValues(Map<Integer, String> values){
		this.values = values;
	}
	
}
