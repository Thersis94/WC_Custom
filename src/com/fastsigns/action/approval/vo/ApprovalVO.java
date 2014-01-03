package com.fastsigns.action.approval.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/****************************************************************************
 * <b>Title</b>: ApprovalVO.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This is a wrapper class designed to hold a collection
 * of ApprovalVO's.  Allows for batch processing of requests across different
 * ChangeLog Types and different Request Types.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Sept 20, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class ApprovalVO {
	private Map<String, List<AbstractChangeLogVO>> vals;
	
	/**
	 * Constructor creates an initial Map so that we never have to check for
	 * null.
	 */
	public ApprovalVO(){
		setVals(new HashMap<String, List<AbstractChangeLogVO>>());
	}
	
	public List<AbstractChangeLogVO> getChangeLogList(String key){
		return vals.get(key);
	}
	
	public void setChangeLogList(String key, List<AbstractChangeLogVO> list){
		vals.put(key, list);
	}

	public void setVals(Map<String, List<AbstractChangeLogVO>> vals) {
		this.vals = vals;
	}

	/**
	 * Return Map of ChangeLogs keyed off of ChangeLogType
	 * @return
	 */
	public Map<String, List<AbstractChangeLogVO>> getVals() {
		return vals;
	}
	
	/**
	 * Allows Batch updating of Approval Logs with the same result.
	 * @param key
	 * @param vo
	 */
	public void setChangeLogList(String key, AbstractChangeLogVO vo){
		List<AbstractChangeLogVO> list = new ArrayList<AbstractChangeLogVO>();
		list.add(vo);
		setChangeLogList(key, list);
	}
	
	
}
