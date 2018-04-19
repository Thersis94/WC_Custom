package com.depuysynthes.srt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.depuysynthes.srt.util.SRTEmailUtil;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SrtAdmin;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO.MilestoneTypeId;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.PrefixBeanDataMapper;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.ClassUtils;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.workflow.milestones.MilestoneRuleVO;
import com.siliconmtn.workflow.milestones.MilestoneUtil;
import com.siliconmtn.workflow.milestones.MilestoneVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;

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
		if(req.hasParameter("json") || req.hasParameter(MILESTONE_ID)) {

			/*
			 * Load Milestone Data.
			 * Only load rules if we are looking at a specific milestone.
			 */
			String opCoId = SRTUtil.getOpCO(req);
			MilestoneTypeId type = EnumUtil.safeValueOf(MilestoneTypeId.class, req.getParameter("milestoneTypeId"));

			List<SRTProjectMilestoneVO> milestones = loadMilestoneData(opCoId, type, req.getParameter(MILESTONE_ID), req.hasParameter(MILESTONE_ID));

			if(req.hasParameter(MILESTONE_ID)) {
				req.setAttribute("parents", loadMilestoneData(opCoId, null, null, false));
				req.setAttribute("fieldNames", ClassUtils.getComparableFieldNames(SRTProjectVO.class));
				req.setAttribute("milestoneDates", loadMilestoneData(opCoId, MilestoneTypeId.DATE, null, false));
				req.setAttribute("emailCampaigns", loadEmailCampaigns(opCoId));
			}

			putModuleData(milestones, milestones.size(), false);
		}
	}

	/**
	 * Load EmailCampaigns for this OpCo.
	 * @param opCoId
	 * @return
	 */
	private Map<String, String> loadEmailCampaigns(String opCoId) {
		Map<String, String> campaigns = new HashMap<>();
		StringBuilder sql = new StringBuilder(100);
		sql.append("select instance_nm, campaign_instance_id ");
		sql.append(DBUtil.FROM_CLAUSE).append("email_campaign_instance ");
		sql.append(DBUtil.WHERE_CLAUSE).append("slug_txt like ? ");
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new StringBuilder(50).append(opCoId).append("|%").toString());
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				campaigns.put(rs.getString("campaign_instance_id"), rs.getString("instance_nm"));
			}
		} catch (SQLException e) {
			log.error("Error loading Email Campaigns", e);
		}
		return campaigns;
	}

	/**
	 * Load Milestones by OpCoId with optional milestoneId for edit purposes.
	 * Pass loadRules to specify if we should load the Milestone Rules
	 * after loading basic Milestone Data.
	 * @param opCoId
	 * @param type
	 * @param milestoneId
	 * @param loadRules - Load Rules in addition to Milestone data.
	 */
	public List<SRTProjectMilestoneVO> loadMilestoneData(String opCoId, MilestoneTypeId type, String milestoneId, boolean loadRules) {
		List<SRTProjectMilestoneVO> milestones = loadMilestones(opCoId, type, milestoneId);

		/*
		 * If we are loading rules and milestones isn't empty, load Rules
		 * for each Milestone in the list.
		 */
		if(loadRules && !milestones.isEmpty()) {
			loadMilestoneRules(milestones);
		}

		return milestones;
	}

	/**
	 * Load Milestone Rules.
	 * @param milestone
	 * @return
	 */
	private void loadMilestoneRules(List<SRTProjectMilestoneVO> milestones) {
		Map<String, SRTProjectMilestoneVO> mMap = milestones.stream().collect(Collectors.toMap(SRTProjectMilestoneVO::getMilestoneId, Function.identity()));
		try(PreparedStatement ps = dbConn.prepareStatement(loadMilestoneRulesSql(milestones.size()))) {
			int i = 1;
			for(SRTProjectMilestoneVO m : milestones) {
				ps.setString(i++, m.getMilestoneId());
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				mMap.get(rs.getString("MILESTONE_ID")).addRule(new MilestoneRuleVO(rs));
			}
		} catch (SQLException e) {
			log.error("Error Loading Milestone Rules", e);
		}
	}

	/**
	 * Build milestone rule retrieval query.
	 * @param count
	 * @return
	 */
	private String loadMilestoneRulesSql(int count) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_MILESTONE_RULE ").append(DBUtil.WHERE_CLAUSE);
		sql.append(" MILESTONE_ID in (");
		DBUtil.preparedStatmentQuestion(count, sql);
		sql.append(")");
		return sql.toString();
	}

	/**
	 * Load Milestones List.
	 * @param opCoId
	 * @param type
	 * @param milestoneId
	 * @return
	 */
	private List<SRTProjectMilestoneVO> loadMilestones(String opCoId, MilestoneTypeId type, String milestoneId) {
		List<SRTProjectMilestoneVO> milestones = new ArrayList<>();
		int i = 1;

		try(PreparedStatement ps = dbConn.prepareStatement(loadMilestonesSql(type, !StringUtil.isEmpty(milestoneId)))) {
			ps.setString(i++, opCoId);
			if(!StringUtil.isEmpty(milestoneId)) {
				ps.setString(i++, milestoneId);
			}

			if(type != null) {
				ps.setString(i++, type.name());
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				milestones.add(new SRTProjectMilestoneVO(rs));
			}
		} catch (SQLException e) {
			log.error("Problem loading Milestones", e);
		}

		return milestones;
	}

	/**
	 * Build Milestone List Sql.
	 * @param type
	 * @param hasId
	 * @return
	 */
	private String loadMilestonesSql(MilestoneTypeId type, boolean hasId) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("DPY_SYN_SRT_MILESTONE ");
		sql.append(DBUtil.WHERE_CLAUSE).append("OP_CO_ID = ? ");
		if(hasId) {
			sql.append("and MILESTONE_ID = ? ");
		}
		if(type != null) {
			sql.append("and MILESTONE_TYPE_ID = ? ");
		}
		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		//If we have a milestoneId on the request, save the Milestone.
		SRTProjectMilestoneVO milestone = new SRTProjectMilestoneVO(req);
		milestone.setRules(new PrefixBeanDataMapper<MilestoneRuleVO>(new MilestoneRuleVO()).populate(req.getParameterMap(), MILESTONE_RULE_PREFIX));
		saveMilestone(milestone);

		sbUtil.moduleRedirect(req, msg, SrtAdmin.MILESTONE.getUrlPath());
	}

	/**
	 * Save Milestone after Edit.
	 * @param milestone
	 */
	private void saveMilestone(SRTProjectMilestoneVO milestone) {

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

			saveMilestoneRecord(milestone);

			//Remove existing Rules.
			flushRules(milestone.getMilestoneId());

			//Create new rules.
			addRules(milestone);

			//Commit Transactions.
			dbConn.commit();
		} catch (SQLException e) {
			log.error("Error Inserting/Updating Milestone/Rules", e);
		} finally {
			//Return DBConn to former Commit behavior.
			DBUtil.setAutoCommit(dbConn, autoCommit);
		}
	}

	/**
	 * Save the Milestone Record
	 * @param milestone
	 * @throws SQLException
	 */
	private void saveMilestoneRecord(SRTProjectMilestoneVO milestone) throws SQLException {
		boolean isInsert = StringUtil.isEmpty(milestone.getMilestoneId());

		if(isInsert) {
			milestone.setMilestoneId(new UUIDGenerator().getUUID());
		}
		try(PreparedStatement ps = dbConn.prepareStatement(saveMilestoneRecordSql(isInsert))) {
			int i = 1;
			ps.setString(i++, milestone.getMilestoneNm());
			ps.setString(i++, milestone.getOrganizationId());
			ps.setString(i++, StringUtil.checkVal(milestone.getParentId(), null));
			ps.setString(i++, milestone.getMilestoneTypeId().name());
			ps.setString(i++, milestone.getCampaignInstanceId());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, milestone.getMilestoneId());
			ps.executeUpdate();
		}
	}

	/**
	 * Build saveMilestoneRecord Sql
	 * @param isInsert
	 * @return
	 */
	private String saveMilestoneRecordSql(boolean isInsert) {
		StringBuilder sql = new StringBuilder(200);

		if(isInsert) {
			sql.append(DBUtil.INSERT_CLAUSE).append(getCustomSchema());
			sql.append("DPY_SYN_SRT_MILESTONE (MILESTONE_NM, OP_CO_ID, ");
			sql.append("PARENT_ID, MILESTONE_TYPE_ID, CAMPAIGN_INSTANCE_ID, ");
			sql.append("CREATE_DT, MILESTONE_ID) ");
			sql.append("values (?,?,?,?,?,?,?)");
		} else {
			sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema());
			sql.append("DPY_SYN_SRT_MILESTONE set MILESTONE_NM = ?, ");
			sql.append("OP_CO_ID = ?, PARENT_ID = ?, MILESTONE_TYPE_ID = ?, ");
			sql.append("CAMPAIGN_INSTANCE_ID = ?, UPDATE_DT = ? ");
			sql.append("where MILESTONE_ID = ?");
		}
		return sql.toString();
	}

	/**
	 * Save all Milestone Rules.
	 * @param milestone
	 * @throws SQLException
	 */
	private void addRules(MilestoneVO milestone) throws SQLException {
		//Ensure MilestoneId is set for all milestone Rules.
		milestone.getRules().stream().forEach(r -> r.setMilestoneId(milestone.getMilestoneId()));

		UUIDGenerator uuid = new UUIDGenerator();
		//Save all Milestone Rules.
		try(PreparedStatement ps = dbConn.prepareStatement(addRulesSql())) {
			for(MilestoneRuleVO r : milestone.getRules()) {
				int i = 1;
				ps.setString(i++, r.getMilestoneId());
				ps.setString(i++, r.getFieldNm());
				ps.setString(i++, r.getOperandType().name());
				ps.setString(i++, r.getFieldVal());
				ps.setTimestamp(i++, Convert.getCurrentTimestamp());
				ps.setString(i++, uuid.getUUID());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	/**
	 * Build Add Rules Sql.
	 * @return
	 */
	private String addRulesSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.INSERT_CLAUSE).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_MILESTONE_RULE (MILESTONE_ID, FIELD_NM, ");
		sql.append("OPERAND_TYPE, FIELD_VAL, CREATE_DT, MILESTONE_RULE_ID) ");
		sql.append("values (?,?,?,?,?,?)");

		return sql.toString();
	}

	/**
	 * Remove existing rules from the database for a given milestoneId.
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
	 * @param project
	 * @throws DatabaseException
	 */
	public void processProject(SRTProjectVO project) {

		//Ensure we have all milestones for this project from the db.
		populateMilestones(Arrays.asList(project));

		//Load all Available Milestones for projects under this OpCo with rules.
		List<SRTProjectMilestoneVO> milestones = loadMilestoneData(project.getOpCoId(), null, null, true);

		//Run project through milestone rules.
		new MilestoneUtil<SRTProjectMilestoneVO>().checkGates(project, milestones);

		//Save New Milestones.
		processNewMilestones(project);
	}

	/**
	 * Process New Milestones.
	 * @param project
	 * @throws DatabaseException
	 */
	private void processNewMilestones(SRTProjectVO project) {

		//Filter out new Milestones to save.
		List<SRTProjectMilestoneVO> newMilestones = project.getMilestones()
													.values()
													.stream()
													.filter(m -> StringUtil.isEmpty(m.getProjectMilestoneXRId()))
													.collect(Collectors.toList());

		//If no new milestones generated, fast return.
		if(newMilestones.isEmpty()) {
			return;
		}

		//Save Milestone Data.
		saveNewMilestones(project, newMilestones);

		//Filter new Milestones down to just those with a campaignInstanceId
		List<SRTProjectMilestoneVO> emailMilestones = newMilestones
														.stream()
														.filter(m -> !StringUtil.isEmpty(m.getCampaignInstanceId()))
														.collect(Collectors.toList());

		//Send Emails.
		if(!emailMilestones.isEmpty())
			postProcessMilestones(project, emailMilestones);

	}

	/**
	 * Saves New Milestones to the db.
	 * @param project
	 * @param newMilestones
	 */
	private void saveNewMilestones(SRTProjectVO project, List<SRTProjectMilestoneVO> newMilestones) {

		//Prep Save Variables.
		UUIDGenerator uuid = new UUIDGenerator();
		int i;

		//Save any new Milestones.
		try(PreparedStatement ps = dbConn.prepareStatement(saveMilestonesSql())) {

			//Save each new Milestone Record.
			for(SRTProjectMilestoneVO m : newMilestones) {

				/*
				 * If this milestone controls project status, update
				 * Project Status.
				 */
				if(MilestoneTypeId.STATUS.equals(m.getMilestoneTypeId())) {
					project.setProjectStatus(m.getMilestoneId());
				}

				//Check if there is a ledger Date recorded for this.
				Date milestoneDt = project.getLedgerDates().get(m.getMilestoneId());
				i = 1;
				ps.setString(i++, m.getProjectId());
				ps.setString(i++, m.getMilestoneId());
				ps.setString(i++, uuid.getUUID());
				ps.setTimestamp(i++, milestoneDt != null ? Convert.formatTimestamp(milestoneDt) : Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.error("Error Saving Project Milestones.", e);
		}
	}

	/**
	 * Process New Milestones that have a CampaignInstanceId
	 * @param project
	 * @param newMilestones
	 */
	private void postProcessMilestones(SRTProjectVO project, List<SRTProjectMilestoneVO> newMilestones) {
		SRTEmailUtil util = new SRTEmailUtil(dbConn, attributes);
		for(SRTProjectMilestoneVO m : newMilestones) {
			util.sendEmail(project.getProjectId(), m.getCampaignInstanceId());
		}
	}

	/**
	 * Build Project Milestone Insert Query.
	 * @return
	 */
	private String saveMilestonesSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_PROJECT_MILESTONE_XR (PROJECT_ID, ");
		sql.append("MILESTONE_ID, PROJ_MILESTONE_XR_ID, MILESTONE_DT) ");
		sql.append("values (?,?,?,?)");
		return sql.toString();
	}

	/**
	 * Helper method that loads Milestones into a list of Project Records.
	 * @param projects
	 */
	public void populateMilestones(List<SRTProjectVO> projects) {

		//Map Projects by ProjectId
		Map<String, SRTProjectVO> pMap = SRTUtil.mapProjects(projects);

		//Create list of keys via pMap keySet.
		List<String> vals = new ArrayList<>(pMap.keySet());

		//Retrieve all Milestones for the project Ids in vals.
		List<SRTProjectMilestoneVO> milestones = loadProjectMilestones(vals);

		//Add Milestones.  Will reflect in passed rowData by references.
		for(SRTProjectMilestoneVO m : milestones) {
			pMap.get(m.getProjectId()).addMilestone(m);
		}
	}

	/**
	 * Load Project Milestone Xr Records from DB.
	 * @param projectIds
	 * @return
	 */
	private List<SRTProjectMilestoneVO> loadProjectMilestones(List<String> projectIds) {
		List<SRTProjectMilestoneVO> projectMilestones = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(loadProjectMilestonesSql(projectIds.size()))) {
			int i = 1;
			for(String projectId : projectIds) {
				ps.setString(i++, projectId);
			}
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				projectMilestones.add(new SRTProjectMilestoneVO(rs));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		return projectMilestones;
	}

	/**
	 * Build Milestone Retrieval Sql.
	 * @param size
	 * @return
	 */
	private String loadProjectMilestonesSql(int size) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_PROJECT_MILESTONE_XR x ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_MILESTONE m  on x.milestone_id = m.milestone_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" PROJECT_ID in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by PROJECT_ID");

		return sql.toString();
	}
}