package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

public class ExerciseIntensityVO extends ExerciseVO implements Serializable {

	private static final long serialVersionUID = 198734645315L;
	private Integer intensityLvlNo = 0;
	private Integer reqSetsNo = 0;
	private String hospitalInstId = null;
	private String routineId = null;
	private String shortDescText = null;
	private Integer orderNo = 0;
	private Map<String, ExerciseAttributeVO> listAttributes = new LinkedHashMap<String, ExerciseAttributeVO>();
	private String exerciseIntensityId = null;
	private String exerciseRoutineId = null;
	private StageVO stage = null;
	
	public enum intensities {
		Low, Medium, High
	};


	public ExerciseIntensityVO() {
	}
	
	public ExerciseIntensityVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		intensityLvlNo = db.getIntegerVal("intensity_level_no", rs);
		reqSetsNo = db.getIntegerVal("req_sets_no", rs);
		exerciseIntensityId = db.getStringVal("exercise_intensity_id", rs);
		exerciseRoutineId = db.getStringVal("exercise_routine_xr_id", rs);
		hospitalInstId = db.getStringVal("hospital_inst_id", rs);
		routineId = db.getStringVal("routine_id", rs);
		orderNo = db.getIntegerVal("order_no", rs);
		shortDescText = db.getStringVal("short_desc_txt", rs);
		
		stage = new StageVO(rs);
		db = null;
	}
	
	public ExerciseIntensityVO(SMTServletRequest req) {
		super(req);
		hospitalInstId = StringUtil.checkVal(req.getParameter("hospitalInstId"), null);
		routineId = StringUtil.checkVal(req.getParameter("routineId"), null);
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		intensityLvlNo = Convert.formatInteger(req.getParameter("intensityLvlNo"));
		reqSetsNo = Convert.formatInteger(req.getParameter("reqSetsNo"));
		exerciseIntensityId = StringUtil.checkVal(req.getParameter("exerciseIntensityId"), null);
		exerciseRoutineId = req.getParameter("exerciseRoutineId");
		shortDescText = req.getParameter("shortDescText");
	}
	
	public ExerciseIntensityVO(SMTServletRequest req, int iLvl) {
		super(req);
		intensityLvlNo = Convert.formatInteger(req.getParameter("intensityLvlNo_" + iLvl));
		reqSetsNo = Convert.formatInteger(req.getParameter("reqSetsNo_" + iLvl), 0);
		exerciseIntensityId = StringUtil.checkVal(req.getParameter("exerciseIntensityId_" + iLvl), null);
		shortDescText = req.getParameter("shortDescText_" + iLvl);
	}
	
	/**
	 * @return the intensityLvlNo
	 */
	public Integer getIntensityLvlNo() {
		return intensityLvlNo;
	}

	/**
	 * @param intensityLvlNo the intensityLvlNo to set
	 */
	public void setIntensityLvlNo(Integer intensityLvlNo) {
		this.intensityLvlNo = intensityLvlNo;
	}

	/**
	 * @return the reqSetsNo
	 */
	public Integer getReqSetsNo() {
		return reqSetsNo;
	}

	/**
	 * @param reqSetsNo the reqSetsNo to set
	 */
	public void setReqSetsNo(Integer reqSetsNo) {
		this.reqSetsNo = reqSetsNo;
	}

	/**
	 * @return the listAttributes
	 */
	public List<ExerciseAttributeVO> getListAttributes() {
		return new ArrayList<ExerciseAttributeVO>(listAttributes.values());
	}

	/**
	 * @param listAttributes the listAttributes to set
	 */
	public void setListAttributes(List<ExerciseAttributeVO> attrs) {
		for (ExerciseAttributeVO vo: attrs)
			listAttributes.put(vo.getExerciseAttributeId(), vo);
	}

	/**
	 * @return the exerciseIntensityId
	 */
	public String getExerciseIntensityId() {
		return exerciseIntensityId;
	}

	/**
	 * 
	 * @param iVo
	 * @return
	 */
	public boolean equals(ExerciseIntensityVO iVo) {
		if(iVo != null && this.exerciseIntensityId != null)
			return this.exerciseIntensityId.equals(iVo.exerciseIntensityId);
		
		return false;
	}
	
	/**
	 * @param exerciseIntensityId the exerciseIntensityId to set
	 */
	public void setExerciseIntensityId(String exerciseIntensityId) {
		this.exerciseIntensityId = exerciseIntensityId;
	}
	
	/**
	 * 
	 * @param vo the vo to add
	 */
	public void addListAttribute(ExerciseAttributeVO vo) {
			listAttributes.put(vo.getExerciseAttributeId(), vo);
	}
	
	/**
	 * 
	 * @param vo the vo to remove
	 */
	public void removeListAttribute(ExerciseAttributeVO vo){
		if(vo != null && listAttributes.containsKey(vo.getExerciseAttributeId()))
			listAttributes.remove(vo);
	}
	
	/**
	 * @return the hospitalInstId
	 */
	public String getHospitalInstId() {
		return hospitalInstId;
	}

	/**
	 * @param hospitalInstId the hospitalInstId to set
	 */
	public void setHospitalInstId(String hospitalInstId) {
		this.hospitalInstId = hospitalInstId;
	}

	/**
	 * @return the stageId
	 */
	public String getRoutineId() {
		return routineId;
	}

	/**
	 * @param stageId the stageId to set
	 */
	public void setRoutineId(String routineId) {
		this.routineId = routineId;
	}

	/**
	 * @return the orderNo
	 */
	public Integer getOrderNo() {
		return orderNo;
	}

	/**
	 * @param orderNo the orderNo to set
	 */
	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}
	
	
	public String getIntensityLevelString(){
		return intensities.values()[intensityLvlNo].name();
	}

	/**
	 * @return the shortDescText
	 */
	public String getShortDescText() {
		return shortDescText;
	}

	/**
	 * @param shortDescText the shortDescText to set
	 */
	public void setShortDescText(String shortDescText) {
		this.shortDescText = shortDescText;
	}

	public StageVO getStage() {
		return stage;
	}

	public void setStage(StageVO stage) {
		this.stage = stage;
	}

	public String getExerciseRoutineId() {
		return exerciseRoutineId;
	}

	public void setExerciseRoutineId(String exerciseRoutineId) {
		this.exerciseRoutineId = exerciseRoutineId;
	}
	
}
