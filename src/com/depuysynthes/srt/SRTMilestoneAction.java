package com.depuysynthes.srt;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.vo.SRTMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.google.common.collect.Maps;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> MilestoneAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Milestone Data and processes Project Records
 * for Milestone Status.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 4, 2018
 ****************************************************************************/
public class SRTMilestoneAction extends SimpleActionAdapter {

	public static final String MILESTONE_ID = "milestoneId";
	public SRTMilestoneAction() {
		super();
	}

	public SRTMilestoneAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("json")) {
			
			loadMilestones(req);
		}
	}

	/**
	 * Load Milestones for management.
	 * @param req
	 */
	private void loadMilestones(ActionRequest req) {
		
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		
	}

	/**
	 * Run SRTProjectVO through available MilestoneFilters and add
	 * Milestones as necessary.
	 * @param p
	 */
	public void processProject(SRTProjectVO p) {
		
	}

	/**
	 * Helper method that loads Milestones into a list of Project Records.
	 * @param rowData
	 */
	public void populateMilestones(List<SRTProjectVO> projects) {

		//Map Projects by ProjectId
		Map<String, SRTProjectVO> pMap = Maps.uniqueIndex(projects, SRTProjectVO::getProjectId);

		//Create list of keys via pMap keySet.
		List<Object> vals = new ArrayList<>(pMap.keySet());

		//Retrieve all Milestones for the project Ids in vals.
		List<SRTMilestoneVO> milestones = new DBProcessor(dbConn).executeSelect(buildMilestoneQuery(vals.size(), getCustomSchema()), vals, new SRTMilestoneVO());

		//Add Milestones.  Will reflect in passed rowData by references.
		for(SRTMilestoneVO m : milestones) {
			pMap.get(m.getProjectId()).addMilestone(m);
		}
	}

	/**
	 * Build Milestone Retrieval Sql.
	 * @param size
	 * @return
	 */
	private String buildMilestoneQuery(int size, String schema) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(schema);
		sql.append("SRT_PROJECT_MILESTONE_XR where PROJECT_ID in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by PROJECT_ID");

		return sql.toString();
	}
}