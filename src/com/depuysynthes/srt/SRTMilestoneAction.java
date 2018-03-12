package com.depuysynthes.srt;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.depuysynthes.srt.util.SRTMilestoneUtil;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTMilestoneRuleVO;
import com.depuysynthes.srt.vo.SRTMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.IndexBeanDataMapper;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
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
	public static final String MILESTONE_RULE_PREFIX = "fieldNm";

	public SRTMilestoneAction() {
		super();
	}

	public SRTMilestoneAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("json")) {

			/*
			 * Load Milestone Data.
			 * Only load rules if we are looking at a specific milestone.
			 */
			loadMilestoneData(SRTUtil.getOpCO(req), req.getParameter(MILESTONE_ID), req.hasParameter(MILESTONE_ID));
		}
	}

	/**
	 * Load Milestones by OpCoId with optional milestoneId for edit purposes.
	 * Pass loadRules to specify if we should load the Milestone Rules
	 * after loading basic Milestone Data.
	 * @param opCoId
	 * @param milestoneId
	 * @param loadRules - Load Rules in addition to Milestone data.
	 */
	private List<SRTMilestoneVO> loadMilestoneData(String opCoId, String milestoneId, boolean loadRules) {
		List<SRTMilestoneVO> milestones = loadMilestones(opCoId, milestoneId);

		/*
		 * If we are loading rules and milestones isn't empty, load Rules
		 * for each Milestone in the list.
		 */
		if(loadRules && !milestones.isEmpty()) {
			for(SRTMilestoneVO m : milestones) {
				loadMilestoneRules(m);
			}
		}

		return milestones;
	}

	/**
	 * Load Milestone Rules.
	 * @param parameter
	 * @return
	 */
	private void loadMilestoneRules(SRTMilestoneVO milestone) {
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		milestone.setRules(dbp.executeSelect(loadMilestonRulesSql(), Arrays.asList(milestone.getMilestoneId()), new SRTMilestoneRuleVO()));
	}

	/**
	 * Build milestone rule retrieval query.
	 * @return
	 */
	private String loadMilestonRulesSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_MILESTONE_REQ_RULE ").append(DBUtil.WHERE_CLAUSE);
		sql.append(" MILESTONE_ID = ? ");

		return sql.toString();
	}

	/**
	 * Load Milestones List.
	 * @return
	 */
	private List<SRTMilestoneVO> loadMilestones(String opCoId, String milestoneId) {
		List<Object> vals = new ArrayList<>();
		vals.add(opCoId);
		if(!StringUtil.isEmpty(milestoneId)) {
			vals.add(milestoneId);
		}
		return new DBProcessor(dbConn, getCustomSchema()).executeSelect(loadMilestonesSql(!StringUtil.isEmpty(milestoneId)), vals, new SRTMilestoneVO());
	}

	/**
	 * Build Milestone List Sql.
	 * @return
	 */
	private String loadMilestonesSql(boolean hasId) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("DPY_SYN_SRT_MILESTONE ");
		sql.append(DBUtil.WHERE_CLAUSE).append("OP_CO_ID = ? ");
		if(hasId) {
			sql.append("and MILE_STONE_ID = ? ");
		}
		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {

		//If we have a milestoneId on the request, save the Milestone.
		if(req.hasParameter(MILESTONE_ID)) {
			SRTMilestoneVO milestone = new SRTMilestoneVO(req);
			milestone.setRules(new IndexBeanDataMapper<SRTMilestoneRuleVO>(new SRTMilestoneRuleVO()).populate(req.getParameterMap()));
			saveMilestone(milestone);
		}
	}

	/**
	 * Save Milestone after Edit.
	 * @param milestone
	 */
	private void saveMilestone(SRTMilestoneVO milestone) {
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());

		/*
		 * Wrap Milestone Update in transaction to ensure we always
		 * successfully create/update the milestone and all associated
		 * rules.
		 */

		//Default AutoCommit to true as that is default state.
		boolean autoCommit = true;
		try {

			//Get current AutoCommit Status.
			autoCommit = dbConn.getAutoCommit();

			//Turn off Autocommit status.
			DBUtil.setAutoCommit(dbConn, false);

			//Update the Milestone Record.
			dbp.save(milestone);

			//Remove existing Rules.
			flushRules(milestone.getMilestoneId());

			//Create new rules.
			addRules(dbp, milestone);

			//Commit Transactions.
			dbConn.commit();
		} catch (SQLException | InvalidDataException | DatabaseException e) {
			log.error("Error Updating Milestone/Rules", e);
		} finally {
			//Return DBConn to former Commit behavior.
			DBUtil.setAutoCommit(dbConn, autoCommit);
		}
	}

	/**
	 * Save all Milestone Rules.
	 * @param dbp
	 * @param milestone
	 * @throws DatabaseException
	 */
	private void addRules(DBProcessor dbp, SRTMilestoneVO milestone) throws DatabaseException {
		//Ensure MilestoneId is set for all milestone Rules.
		milestone.getRules().stream().forEach(r -> r.setMilestoneId(milestone.getMilestoneId()));

		//Save all Milestone Rules.
		dbp.executeBatch(milestone.getRules());
	}

	/**
	 * Remove existing rules from the database for a given milestoneId.
	 * @param dbp
	 * @param milestoneId
	 */
	private void flushRules(String milestoneId) {
		try(PreparedStatement ps = dbConn.prepareStatement(flushRulesSql())) {
			ps.setString(1, milestoneId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Build sql for removing Rules for a given milestoneId
	 * @return
	 */
	private String flushRulesSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.DELETE_CLAUSE).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("DPY_SYN_SRT_MILESTONE_RULE ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" milestone_id = ? ");
		return sql.toString();
	}

	/**
	 * Run SRTProjectVO through available MilestoneFilters and add
	 * Milestones as necessary.
	 * @param p
	 * @throws DatabaseException
	 */
	public void processProject(SRTProjectVO p) throws DatabaseException {

		//Ensure we have all milestones for this project from the db.
		populateMilestones(Arrays.asList(p));

		//Load all Available Milestones for projects under this OpCo with rules.
		List<SRTMilestoneVO> milestones = loadMilestoneData(p.getOpCoId(), null, true);

		//Run project through milestone rules.
		SRTMilestoneUtil.checkGates(p, milestones);

		//Save New Milestones.
		saveMilestones(p);
	}

	/**
	 * Save Project Milestones Xrs.
	 * @param p
	 * @throws DatabaseException
	 */
	private void saveMilestones(SRTProjectVO p) throws DatabaseException {

		//Filter out new Milestones to save.
		List<SRTProjectMilestoneVO> newMilestones = p.getMilestones()
													.values()
													.stream()
													.filter(m -> StringUtil.isEmpty(m.getProjectMilestoneXRId()))
													.collect(Collectors.toList());

		//Save any new Milestones in list.
		if(!newMilestones.isEmpty())
			new DBProcessor(dbConn, getCustomSchema()).executeBatch(newMilestones);
	}

	/**
	 * Helper method that loads Milestones into a list of Project Records.
	 * @param rowData
	 */
	public void populateMilestones(List<SRTProjectVO> projects) {

		//Map Projects by ProjectId
		Map<String, SRTProjectVO> pMap = projects.stream().collect(Collectors.toMap(SRTProjectVO::getProjectId, Function.identity()));

		//Create list of keys via pMap keySet.
		List<Object> vals = new ArrayList<>(pMap.keySet());

		//Retrieve all Milestones for the project Ids in vals.
		List<SRTProjectMilestoneVO> milestones = new DBProcessor(dbConn).executeSelect(populateMilestonesSql(vals.size(), getCustomSchema()), vals, new SRTProjectMilestoneVO());

		//Add Milestones.  Will reflect in passed rowData by references.
		for(SRTProjectMilestoneVO m : milestones) {
			pMap.get(m.getProjectId()).addMilestone(m);
		}
	}

	/**
	 * Build Milestone Retrieval Sql.
	 * @param size
	 * @return
	 */
	private String populateMilestonesSql(int size, String schema) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(schema);
		sql.append("DPY_SYN_SRT_PROJECT_MILESTONE_XR where PROJECT_ID in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by PROJECT_ID");

		return sql.toString();
	}
}