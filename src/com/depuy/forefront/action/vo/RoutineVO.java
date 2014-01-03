package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

public class RoutineVO extends StageVO implements Serializable {

	private static final long serialVersionUID = 19873464123135315L;
	private String routineId = null;
	private String hospitalInstId = null;
	private String headerText = null;
	private Map<String, ExerciseIntensityVO> exercises = new LinkedHashMap<String, ExerciseIntensityVO>();
	
	public RoutineVO() {
		super();
	}
	
	public RoutineVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		setRoutineId(db.getStringVal("routine_id", rs));
		setHospitalInstId(db.getStringVal("hospital_inst_id", rs));
		setHeaderText(db.getStringVal("header_txt", rs));
		db = null;
	}
	
	public RoutineVO(SMTServletRequest req) {
		super(req);
		if (req.hasParameter("routineId")) setRoutineId(req.getParameter("routineId"));
		if (req.hasParameter("hospitalInstId")) setHospitalInstId(req.getParameter("hospitalInstId"));
		setHeaderText(req.getParameter("headerText"));
	}
	
	public String getRoutineId() {
		return routineId;
	}

	public void setRoutineId(String routineId) {
		this.routineId = routineId;
	}

	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public List<ExerciseIntensityVO> getExercises() {
		return new ArrayList<ExerciseIntensityVO>(exercises.values());
	}
	
	public ExerciseIntensityVO getExercise(String exId) {
		return exercises.get(exId);
	}

	public void addExercise(ExerciseIntensityVO ex) {
		exercises.put(ex.getExerciseIntensityId(), ex);
	}
		
	public boolean containsExercise(String itemId) {
		return exercises.containsKey(itemId);
	}

	public String getHospitalInstId() {
		return hospitalInstId;
	}

	public void setHospitalInstId(String hospitalInstId) {
		this.hospitalInstId = hospitalInstId;
	}
}
