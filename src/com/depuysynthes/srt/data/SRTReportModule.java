package com.depuysynthes.srt.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.SRTMasterRecordAction;
import com.depuysynthes.srt.SRTMilestoneAction;
import com.depuysynthes.srt.SRTProjectAction;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SRTLists;
import com.depuysynthes.srt.vo.ProjectExportReportVO;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.ram.workflow.modules.EmailWFM;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.BaseMessageVO.InstanceName;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.error.WorkflowModuleException;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title:</b> SRTReportModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> SRT Report Export Workflow.  Loads Project Data,
 * dumps it to an Excel sheet and emails it to the provided email.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 3, 2018
 ****************************************************************************/
public class SRTReportModule extends AbstractWorkflowModule {

	/**
	 * @param mod
	 * @param conn
	 * @param customSchema
	 * @throws Exception
	 */
	public SRTReportModule(WorkflowModuleVO mod, Connection conn, String customSchema) throws Exception {
		super(mod, conn, customSchema);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.workflow.modules.AbstractWorkflowModule#run()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void run() throws Exception {
		List<String> projectIds = (List<String>) mod.getConfig(SRTProjectAction.SRT_PROJECT_ID).getValue();

		try {
			new MessageSender(attributes, getConnection()).sendMessage(buildEmail(buildReport(projectIds)));
		} catch (InvalidDataException e) {
			throw new WorkflowModuleException("There was a problem sending the report.", e);
		}
	}

	/**
	 * Retrieve Project Data and store in an AbstractReportVO.
	 * @param projectIds
	 * @return
	 * @throws SQLException
	 * @throws EncryptionException
	 */
	private AbstractSBReportVO buildReport(List<String> projectIds) throws SQLException, EncryptionException {

		//Load Processing Data.
		List<SRTProjectMilestoneVO> milestones = loadMilestones();
		List<String> mrAttributes = loadMrAttributes();
		Map<String, Map<String, String>> lists = loadLists();

		//Get all projects contained in projectIds list.
		Map<String, SRTProjectVO> projects = loadProjects(projectIds, milestones);

		//Load MilestoneData on Projects.
		loadMilestoneData(projects);

		//Load Master Record Data on Projects.
		loadMasterRecordData(projects, mrAttributes);

		//Ensure all Projects have a MasterRecord so that columns line up correctly.
		checkMasterRecordData(projects, mrAttributes);

		//Desrypt Name data on Project Records.
		decryptNames(projects);

		//Replace targeted columns with List Values.
		updateListDataReferences(projects, lists);

		//Store projects in reportData for Header Formatting.
		ProjectExportReportVO reportData = new ProjectExportReportVO(projects, mrAttributes, milestones);

		//Create new Report and set data.
		AbstractSBReportVO report = new SRTProjectExportReportVO("Search Results Export.xlsx");
		report.setData(reportData);

		//Return Report
		return report;
	}

	/**
	 * Update Data References in Project Records with Lists in Report.
	 * @param projects
	 * @param lists
	 */
	private void updateListDataReferences(Map<String, SRTProjectVO> projects, Map<String, Map<String, String>> lists) {
		for(SRTProjectVO p : projects.values()) {
			try {
				//Update Project Data Values
				p.setPriority(lists.get(SRTLists.PROJ_PRIORITY.name()).get(p.getPriority()));
				p.setMfgDtChangeReason(lists.get(SRTLists.PROJ_MFG_CHANGE_REASON.name()).get(p.getMfgDtChangeReason()));
				p.setProjectType(lists.get(SRTLists.PROJ_TYPE.name()).get(p.getProjectType()));
				p.setSupplierId(lists.get(SRTLists.PROJ_VENDOR.name()).get(p.getSupplierId()));
				p.setProjectStatus(lists.get(SRTLists.PROJ_STATUS.name()).get(p.getProjectStatus()));

				//Update Request Data Values
				SRTRequestVO r = p.getRequest();
				r.setChargeTo(lists.get(SRTLists.CHARGE_TO.name()).get(r.getChargeTo()));
				r.setReason(lists.get(SRTLists.REQ_REASON.name()).get(r.getReason()));

				//Update Master Record Data Values
				for(SRTMasterRecordVO m : p.getMasterRecords()) {
					m.setComplexityId(lists.get(SRTLists.COMPLEXITY.name()).get(m.getComplexityId()));
					m.setProdCatId(lists.get(SRTLists.PROD_CAT.name()).get(m.getProdCatId()));
					m.setProdFamilyId(lists.get(SRTLists.PROD_FAMILY.name()).get(m.getProdFamilyId()));
					m.setProdTypeId(lists.get(SRTLists.PRODUCT_TYPE.name()).get(m.getProdTypeId()));
					m.setQualitySystemId(lists.get(SRTLists.QUALITY_SYSTEM.name()).get(m.getQualitySystemId()));
				}
			} catch (Exception e) {
				log.debug("Problem translating field on record: " + p.getProjectId());
			}
		}
	}

	/**
	 * Load Data Lists to replace GUID/Keys with Friendly Text.
	 * @return
	 * @throws SQLException
	 */
	private Map<String, Map<String, String>> loadLists() throws SQLException {
		Map<String, Map<String, String>> lists = new HashMap<>();
		try(PreparedStatement ps = getConnection().prepareStatement(loadListsSql())) {
			ps.setString(1, SRTUtil.SRT_ORG_ID);
			ResultSet rs = ps.executeQuery();

			String listKey = null;
			Map<String, String> listData = new HashMap<>();
			while(rs.next()) {
				if(!rs.getString("LIST_ID").equals(listKey)) {
					if(listKey != null) {
						lists.put(listKey, listData);
					}
					listData = new HashMap<>();
					listKey = rs.getString("LIST_ID");
				}
				listData.put(rs.getString("VALUE_TXT"), rs.getString("LABEL_TXT"));
			}
			if(listKey != null)
				lists.put(listKey, listData);
		}
		return lists;
	}

	/**
	 * Build Data List Retrieval Sql.
	 * @return
	 */
	private String loadListsSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append("list l ");
		sql.append(DBUtil.INNER_JOIN).append("list_data d ");
		sql.append("on l.list_id = d.list_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("l.organization_id = ?");
		return sql.toString();
	}

	/**
	 * Ensure there is a Master Record on ever Project so that columns stay
	 * in proper offset Configuration.
	 * @param projects
	 * @param mrAttributes
	 */
	private void checkMasterRecordData(Map<String, SRTProjectVO> projects, List<String> mrAttributes) {
		for(SRTProjectVO p : projects.values()) {
			if(p.getMasterRecords().isEmpty()) {
				log.debug("Generating Master Record for Project: " + p.getProjectId());
				SRTMasterRecordVO mr = buildNewMR(null, mrAttributes);
				mr.setTitleTxt("No Master Record on Project");
				mr.setMasterRecordId("AUTO_GEN_ID");
				p.addMasterRecord(mr);
			}
		}
	}

	/**
	 * Load Complete List of Milestones.
	 * @return
	 * @throws SQLException
	 */
	private List<SRTProjectMilestoneVO> loadMilestones() throws SQLException {
		String opCoId = (String) mod.getConfig(SRTUtil.OP_CO_ID).getValue();
		SRTMilestoneAction sma = new SRTMilestoneAction();
		sma.setAttributes(attributes);
		sma.setDBConnection(getConnection());
		return sma.loadMilestoneData(opCoId, null, null, false);
	}

	/**
	 * Load Complete List of Master Record Attributes
	 * @return
	 * @throws SQLException
	 */
	private List<String> loadMrAttributes() throws SQLException {
		String opCoId = (String) mod.getConfig(SRTUtil.OP_CO_ID).getValue();
		SRTMasterRecordAction sma = new SRTMasterRecordAction();
		sma.setAttributes(attributes);
		sma.setDBConnection(getConnection());
		return new ArrayList<>(sma.loadRecordAttributes(null, opCoId).keySet());
	}

	/**
	 * Decrypt Encrypted Fields on Project Records.
	 * @param projects
	 * @throws EncryptionException
	 */
	private void decryptNames(Map<String, SRTProjectVO> projects) throws EncryptionException {
		StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		for(SRTProjectVO p : projects.values()) {
			p.setBuyerNm(SRTUtil.decryptName(p.getBuyerNm(), se));
			p.setEngineerNm(SRTUtil.decryptName(p.getEngineerNm(), se));
			p.setQualityEngineerNm(SRTUtil.decryptName(p.getQualityEngineerNm(), se));
			p.setDesignerNm(SRTUtil.decryptName(p.getDesignerNm(), se));
			p.setRequestorNm(SRTUtil.decryptName(p.getRequestorNm(), se));
		}
	}

	/**
	 * Load Milestone Data for Projects.
	 * @param projects
	 * @throws SQLException
	 */
	private void loadMilestoneData(Map<String, SRTProjectVO> projects) throws SQLException {
		try(PreparedStatement ps = getConnection().prepareStatement(loadMilestoneDataSql(projects.size()))) {
			int i = 1;
			for(String s : projects.keySet()) {
				ps.setString(i++, s);
			}
			ResultSet rs = ps.executeQuery();

			SRTProjectVO p = null;
			while(rs.next()) {
				if(p == null || !p.getProjectId().equals(rs.getString(SRTProjectAction.DB_PROJECT_ID))) {
					p = projects.get(rs.getString(SRTProjectAction.DB_PROJECT_ID));
				}
				//Set MilestoneDt on related Milestone.
				p.getMilestone(rs.getString("MILESTONE_ID")).setMilestoneDt(rs.getDate("MILESTONE_DT"));
			}
		}
	}

	/**
	 * Builds MilestoneData Lookup Sql.
	 * @param size
	 * @return
	 */
	private String loadMilestoneDataSql(int size) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select x.MILESTONE_ID, x.PROJECT_ID, x.MILESTONE_DT ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("dpy_syn_srt_milestone m ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_project_milestone_xr x ");
		sql.append("on m.milestone_id = x.milestone_id");
		sql.append(DBUtil.WHERE_CLAUSE).append(" x.project_id in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") ");
		sql.append(DBUtil.ORDER_BY).append(" x.project_id desc");
		return sql.toString();
	}

	/**
	 * Load Master Record Data and populate projects with MasterRecordData
	 * @param projects
	 * @param mrAttributes
	 */
	private void loadMasterRecordData(Map<String, SRTProjectVO> projects, List<String> mrAttributes) {
		try(PreparedStatement ps = getConnection().prepareStatement(loadMasterRecordDataSql(projects.size()))) {
			int i = 1;
			for(String pId : projects.keySet()) {
				ps.setString(i++, pId);
			}
			log.debug(ps.toString());
			ResultSet rs = ps.executeQuery();
			SRTMasterRecordVO mr = null;
			while(rs.next()) {
				String mrId = rs.getString("MASTER_RECORD_ID");
				String pId = rs.getString(SRTProjectAction.DB_PROJECT_ID);
				if(mr != null && (!mr.getMasterRecordId().equals(mrId) || !mr.getXrProjectId().equals(pId))) {
					projects.get(mr.getXrProjectId()).addMasterRecord(mr);
					mr = buildNewMR(rs, mrAttributes);
				} else if(mr == null) {
					mr = buildNewMR(rs, mrAttributes);
				}
				mr.addAttribute(rs.getString("ATTR_ID"), rs.getString("VALUE_TXT"));
			}
			if(mr != null)
				projects.get(mr.getXrProjectId()).addMasterRecord(mr);
		} catch (SQLException e) {
			log.error("Error Loading Master Record Code", e);
		}
	}

	/**
	 * Build and Populate Attributes of a new SRTMasterRecordVO
	 * @param rs
	 * @param mrAttributes
	 * @return
	 */
	private SRTMasterRecordVO buildNewMR(ResultSet rs, List<String> mrAttributes) {
		SRTMasterRecordVO mr = new SRTMasterRecordVO(rs);

		//Add All Empty Values
		for(String attr : mrAttributes) {
			mr.addAttribute(attr, "");
		}

		return mr;
	}

	/**
	 * Builds the Master Record Loading Sql.
	 * @param size
	 * @return
	 */
	private String loadMasterRecordDataSql(int size) {
		StringBuilder sql = new StringBuilder();
		sql.append("select m.*, x.project_id, x.part_count, a.attr_id, a.value_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("dpy_syn_srt_master_record m ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_master_record_project_xr x ");
		sql.append("on m.master_record_id = x.master_record_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_mr_attr_xr a ");
		sql.append("on a.master_record_id = m.master_record_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("x.project_id in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(")");
		sql.append(DBUtil.ORDER_BY).append("x.project_id, m.master_record_id, a.attr_id");
		return sql.toString();
	}

	/**
	 * Loads Map of Project Records from database for given projectIds.
	 * @param projectIds
	 * @param milestones
	 * @return
	 */
	private Map<String, SRTProjectVO> loadProjects(List<String> projectIds, List<SRTProjectMilestoneVO> milestones) {
		Map<String, SRTProjectVO> projects = new LinkedHashMap<>();

		String sql = getProjectSql(projectIds.size());
		log.debug(sql);
		try(PreparedStatement ps = getConnection().prepareStatement(sql)) {
			int i = 1;
			for(String projId : projectIds) {
				ps.setString(i++, projId);
			}

			SRTProjectVO project = null;
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {

				//Store last Project on map.
				if(project != null) {
					projects.put(project.getProjectId(), project);
				}

				//Build new Project
				project = new SRTProjectVO(rs);

				//Build Request
				SRTRequestVO r = new SRTRequestVO(rs);

				//Set Address on Request.
				r.setRequestAddress(new SRTRequestAddressVO(rs));

				//Set Request on Project.
				project.setRequest(r);

				//Add Empty Milestones to Project.
				storeMilestones(project, milestones);
			}

			if(project != null) {
				projects.put(project.getProjectId(), project);
			}
		} catch (SQLException e) {
			log.error("Error Loading Project Records", e);
		}

		return projects;
	}

	/**
	 * Store Milestones on the Project Record.
	 * @param project
	 * @param milestones
	 */
	private void storeMilestones(SRTProjectVO project, List<SRTProjectMilestoneVO> milestones) {
		//Add All Empty Milestones.
		for(SRTProjectMilestoneVO m : milestones)
			project.addMilestone(m.getMilestoneId(), null);
	}

	/**
	 * Load Project Sql.
	 * @return
	 */
	private String getProjectSql(int projectCount) {
		StringBuilder sql = new StringBuilder(2000);
		sql.append("Select p.*, r.*, a.*, ");
		sql.append("concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
		sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
		sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm, ");
		sql.append("concat(bp.first_nm, ' ', bp.last_nm) as buyer_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("dpy_syn_srt_project p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_request r on p.request_id = r.request_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_roster u on r.roster_id = u.roster_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile pr on u.profile_id = pr.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_request_address a on r.request_id = a.request_id ");

		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster eng on p.engineer_id = eng.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile ep on eng.profile_id = ep.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster des on p.designer_id = des.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile dp on des.profile_id = dp.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster qa on p.quality_engineer_id = qa.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile qp on qa.profile_id = qp.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster b on p.buyer_id = b.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile bp on b.profile_id = bp.profile_id ");

		sql.append(DBUtil.WHERE_CLAUSE).append("p.project_id in (");
		DBUtil.preparedStatmentQuestion(projectCount, sql);
		sql.append(") ").append(DBUtil.ORDER_BY).append("p.project_id");
		return sql.toString();
	}

	/**
	 * Helper method that builds an EmailMessageVO for sending off to the email
	 * queue.
	 *
	 * @return
	 * @throws InvalidDataException
	 */
	private EmailMessageVO buildEmail(AbstractSBReportVO report) throws InvalidDataException {

		EmailMessageVO email = new EmailMessageVO();

		//Set Basic Parameters.
		email.setInstance(InstanceName.DEPUY);
		email.addRecipients(mod.getConfig(EmailWFM.DEST_EMAIL_ADDR).toString());
		email.setSubject(mod.getConfig(EmailWFM.EMAIL_TITLE).toString());

		//Ensure that we only ever get one Source Email.
		email.setFrom(mod.getConfig(EmailWFM.SRC_EMAIL_ADDR).getConfigValues().get(0).toString());

		//Set Optional Fields as necessary.
		email.addAttachment(report.getFileName(), report.generateReport());

		//Return Email Object.
		return email;
	}
}