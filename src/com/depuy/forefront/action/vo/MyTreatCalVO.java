package com.depuy.forefront.action.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MyTreatCalVO extends CoreForeFrontVO {

	private static final long serialVersionUID = 198798798724L;
	
	private Map<String, TreatCalVO> stages = new LinkedHashMap<String, TreatCalVO>();
	
	public MyTreatCalVO() {
	}
	
	public MyTreatCalVO(ResultSet rs) {
		super(rs);
	}
	
	public void addStage(TreatCalVO stage) {
		stages.put(stage.getStageId(), stage);
	}
	
	/**
	 * @return the stages
	 */
	public List<TreatCalVO> getStages() {
		return new ArrayList<TreatCalVO>(stages.values());
	}
	
	public TreatCalVO getStage(String stageId) {
		return stages.get(stageId);
	}
	
	public boolean containsStage(String stageId) {
		return stages.containsKey(stageId);
	}

}
