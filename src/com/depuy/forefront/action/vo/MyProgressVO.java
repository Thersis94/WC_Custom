package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class MyProgressVO implements Serializable {

	private static final long serialVersionUID = 1988725828724L;
	private String exerciseName = null;
	private Map<Integer, DetailVO> completed = new LinkedHashMap<Integer, DetailVO>(45);
	
	@SuppressWarnings("unused")
	private Integer completedIdx = Integer.valueOf(0); //for JSP useBean syntax

	public MyProgressVO(String exNm) {
		this.setExerciseName(exNm);
		
		//init our map for consistency in the views
		for (int x=0;x<28;x++)
			completed.put(x, null);
	}

	public String getExerciseName() {
		return exerciseName;
	}

	public void setExerciseName(String exerciseName) {
		this.exerciseName = exerciseName;
	}

	public Map<Integer, DetailVO> getCompleted() {
		return completed;
	}
	
	public DetailVO getDetails(int idx) {
		return completed.get(idx);
	}

	public void setCompletedIdx(Integer completedIdx) {
		this.completedIdx = completedIdx;
	}

	public void addStage(int surgWeek, int reqSets) {
		int idx = Math.abs(surgWeek * 7);
		if (surgWeek < 0) idx = Math.abs(28-idx);
		else idx = idx -7;
		
		for (int x=0; x < 7; x++)
			this.setCompletedIdx(idx+x, reqSets, 0);
	}
	
	public void setCompletedIdx(Integer key, int reqSets, int setsCompleted) {
		this.completed.put(key, new DetailVO(key, reqSets, setsCompleted));
	}
	
	public void setRepsCompleted(Integer key, int setsCompleted) {
		if (key == null) return;
		
		DetailVO vo = completed.get(key);
		if (vo == null) vo = new DetailVO(key, 0, 0);
			
		vo.setsCompleted += setsCompleted;
		completed.put(key, vo);
	}
	

	public class DetailVO {
		public int reqSetsNo = 0;
		public int setsCompleted = 0;
		public int dayOffset = 0;
		
		public DetailVO(int dayOffset, int reqSets, int completed) {
			this.dayOffset = dayOffset;
			this.reqSetsNo = reqSets;
			this.setsCompleted = completed;

		}
		public int getPercentage() {
			int val = 0;
			try {
				Double d = (new Double(setsCompleted)/new Double(reqSetsNo))  * 100;
				val = d.intValue();
				val = Math.min(val, 100);
			} catch (Exception e) {
				val = 0;
			}
			
			return val;
		}
	}
	
}
