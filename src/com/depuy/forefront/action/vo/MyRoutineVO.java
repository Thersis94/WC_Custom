package com.depuy.forefront.action.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MyRoutineVO extends CoreForeFrontVO {
	
	private static final long serialVersionUID = 19844474598724L;
	private Map<String, RoutineVO> stages = new LinkedHashMap<String, RoutineVO>();

	public MyRoutineVO() {
	}
	
	public MyRoutineVO(ResultSet rs) {
		super(rs);
	}
	
	public void addStage(RoutineVO stage) {
		stages.put(stage.getStageId(), stage);
	}
	
	/**
	 * @return the stages
	 */
	public List<RoutineVO> getStages() {
		return new ArrayList<RoutineVO>(stages.values());
	}
	
	public RoutineVO getStage(String stageId) {
		return stages.get(stageId);
	}
	
	public boolean containsStage(String stageId) {
		return stages.containsKey(stageId);
	}
}
