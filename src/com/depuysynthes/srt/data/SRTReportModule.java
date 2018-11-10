package com.depuysynthes.srt.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.SRTMasterRecordAction;
import com.depuysynthes.srt.SRTMilestoneAction;
import com.depuysynthes.srt.SRTProjectAction;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SRTList;
import com.depuysynthes.srt.vo.ProjectExportReportVO;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
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
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
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
		List<String> projectIds = (List<String>) mod.getModuleConfig(SRTProjectAction.SRT_PROJECT_ID).getValue();

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
		log.debug("loading Milestones");
		List<SRTProjectMilestoneVO> milestones = loadMilestones();

		log.debug("loading MR Attributes");
		List<String> mrAttributes = loadMrAttributes();

		log.debug("loading Lists");
		Map<String, Map<String, String>> lists = SRTUtil.loadLists(getConnection());

		log.debug("loading Projects");
		//Get all projects contained in projectIds list.
		Map<String, SRTProjectVO> projects = loadProjects(projectIds, milestones);

		//Load MilestoneData on Projects.
		log.debug("loading Milestones");
		loadMilestoneData(projects);

		log.debug("loading Master Record Data");
		//Load Master Record Data on Projects.
		loadMasterRecordData(projects, mrAttributes);

		log.debug("Verifying Master Record Data");
		//Ensure all Projects have a MasterRecord so that columns line up correctly.
		checkMasterRecordData(projects, mrAttributes);

		log.debug("Decrypting Users");
		//Desrypt Name data on Project Records.
		try(Connection dbConn = getConnection()) {
			SRTUtil.decryptProjectData(new ArrayList<>(projects.values()), new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY)), ProfileManagerFactory.getInstance(attributes), dbConn);
		}

		log.debug("Replacing List Ids");
		//Replace targeted columns with List Values.
		updateListDataReferences(projects, lists);

		log.debug("Building Report Data");
		//Store projects in reportData for Header Formatting.
		ProjectExportReportVO reportData = new ProjectExportReportVO(projects, mrAttributes, milestones);

		log.debug("Building Report VO");
		//Create new Report and set data.
		AbstractSBReportVO report = new SRTProjectExportReportVO("Search Results Export.xls");
		report.setData(reportData);

		log.debug("Report Built");
		//Return Report
		return report;
	}

	/**
	 * Update Data References in Project Records with Lists in Report.
	 * @param projects
	 * @param lists
	 */
	private void updateListDataReferences(Map<String, SRTProjectVO> projects, Map<String, Map<String, String>> lists) {
		String opCoId = (String) mod.getModuleConfig(SRTUtil.OP_CO_ID).getValue();

		for(SRTProjectVO p : projects.values()) {
			try {
				//Update Project Data Values
				p.setPriority(lists.get(SRTUtil.getListId(opCoId, SRTList.PROJ_PRIORITY)).get(p.getPriority()));
				p.setMfgDtChangeReason(lists.get(SRTUtil.getListId(opCoId, SRTList.PROJ_MFG_CHANGE_REASON)).get(p.getMfgDtChangeReason()));
				p.setProjectType(lists.get(SRTUtil.getListId(opCoId, SRTList.PROJ_TYPE)).get(p.getProjectType()));
				p.setSupplierId(lists.get(SRTUtil.getListId(opCoId, SRTList.PROJ_VENDOR)).get(p.getSupplierId()));
				p.setProjectStatus(lists.get(SRTUtil.getListId(opCoId, SRTList.PROJ_STATUS)).get(p.getProjectStatus()));
				p.setMfgOrderTypeId(lists.get(SRTUtil.getListId(opCoId, SRTList.PROJ_MFG_ORDER_TYPE)).get(p.getMfgOrderTypeId()));

				//Update Request Data Values
				SRTRequestVO r = p.getRequest();
				r.setChargeTo(lists.get(SRTUtil.getListId(opCoId, SRTList.CHARGE_TO)).get(r.getChargeTo()));
				r.setReason(lists.get(SRTUtil.getListId(opCoId, SRTList.REQ_REASON)).get(r.getReason()));

				//Update Master Record Data Values
				for(SRTMasterRecordVO m : p.getMasterRecords()) {
					m.setComplexityId(lists.get(SRTUtil.getListId(opCoId, SRTList.COMPLEXITY)).get(m.getComplexityId()));
					m.setProdCatId(lists.get(SRTUtil.getListId(opCoId, SRTList.PROD_CAT)).get(m.getProdCatId()));
					m.setProdFamilyId(lists.get(SRTUtil.getListId(opCoId, SRTList.PROD_FAMILY)).get(m.getProdFamilyId()));
					m.setProdTypeId(lists.get(SRTUtil.getListId(opCoId, SRTList.PRODUCT_TYPE)).get(m.getProdTypeId()));
					m.setQualitySystemId(lists.get(SRTUtil.getListId(opCoId, SRTList.QUALITY_SYSTEM)).get(m.getQualitySystemId()));
				}
			} catch (Exception e) {
				log.debug("Problem translating field on record: " + p.getProjectId());
			}
		}
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
		String opCoId = (String) mod.getModuleConfig(SRTUtil.OP_CO_ID).getValue();
		SRTMilestoneAction sma = new SRTMilestoneAction();
		sma.setAttributes(attributes);
		sma.setDBConnection(getConnection());
		return sma.loadMilestoneData(opCoId, null, null, false, false);
	}

	/**
	 * Load Complete List of Master Record Attributes
	 * @return
	 * @throws SQLException
	 */
	private List<String> loadMrAttributes() throws SQLException {
		String opCoId = (String) mod.getModuleConfig(SRTUtil.OP_CO_ID).getValue();
		SRTMasterRecordAction sma = new SRTMasterRecordAction();
		sma.setAttributes(attributes);
		sma.setDBConnection(getConnection());
		return new ArrayList<>(sma.loadRecordAttributes(null, opCoId).keySet());
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
		log.debug(sql.toString());
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
		log.debug(sql.toString());
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

		try(PreparedStatement ps = getConnection().prepareStatement(getProjectSql(projectIds.size()))) {
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

				r.setRequestor(new SRTRosterVO(rs));

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
		sql.append("Select p.*, r.*, a.*, u.region, u.area, u.profile_id, u.roster_id, ");
		sql.append("concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
		sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
		sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm, ");
		sql.append("concat(bp.first_nm, ' ', bp.last_nm) as buyer_nm, ");
		sql.append("r.surgeon_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("dpy_syn_srt_project p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_request r on p.request_id = r.request_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_roster u on r.roster_id = u.roster_id ");
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
		log.debug(sql.toString());
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
		email.addRecipients(mod.getModuleConfig(EmailWFM.DEST_EMAIL_ADDR).toString());
		email.setSubject(mod.getModuleConfig(EmailWFM.EMAIL_TITLE).toString());

		//Ensure that we only ever get one Source Email.
		email.setFrom(mod.getModuleConfig(EmailWFM.SRC_EMAIL_ADDR).getConfigValues().get(0).toString());

		//Set Optional Fields as necessary.
		email.addAttachment(report.getFileName(), report.generateReport());

		//Return Email Object.
		return email;
	}
}