package com.depuy.forefront.action.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MyActionPlanVO extends CoreForeFrontVO {

	private static final long serialVersionUID = 198798798724L;
	
	private Map<String, ActionPlanVO> stages = new LinkedHashMap<String, ActionPlanVO>();
	
	public MyActionPlanVO() {
	}
	
	public MyActionPlanVO(ResultSet rs) {
		super(rs);
	}
	
	public void addStage(ActionPlanVO stage) {
		stages.put(stage.getStageId(), stage);
	}
	
	/**
	 * @return the stages
	 */
	public List<ActionPlanVO> getStages() {
		return new ArrayList<ActionPlanVO>(stages.values());
	}
	
	public ActionPlanVO getStage(String stageId) {
		return stages.get(stageId);
	}
	
	public boolean containsStage(String stageId) {
		return stages.containsKey(stageId);
	}

}
