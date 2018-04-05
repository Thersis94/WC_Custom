package com.depuysynthes.srt.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.SRTProjectAction;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.ram.workflow.modules.EmailWFM;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.BaseMessageVO.InstanceName;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.StringUtil;
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
		List<String> projectIds = (List<String>) mod.getConfig(SRTProjectAction.SRT_PROJECT_ID).getConfigValues().get(0).getConfigValue();

		try {
			new MessageSender(attributes, getConnection()).sendMessage(buildEmail(buildReport(projectIds)));
		} catch (InvalidDataException e) {
			log.error(e);
			throw new WorkflowModuleException("There was a problem building the EmailMessageVO.", e);
		}
	}

	/**
	 * Retrieve Project Data and store in an AbstractReportVO.
	 * @param projectIds
	 * @return
	 */
	private AbstractSBReportVO buildReport(List<String> projectIds) {
		Map<String, SRTProjectVO> projects = loadProjects(projectIds);
		loadMilestoneData(projects);
		loadMasterRecordData(projects);
		decryptNames(projects);
		AbstractSBReportVO report = new SRTProjectExportReportVO("Search Results Export.xlsx");
		report.setData(projects);
		return report;
	}

	/**
	 * Decrypt Encrypted Fields on Project Records.
	 * @param projects
	 */
	private void decryptNames(Map<String, SRTProjectVO> projects) {
		//TODO - Decrypt Necessary Project Data.
	}

	/**
	 * Load Milestone Data for Projects.
	 * @param projects
	 */
	private void loadMilestoneData(Map<String, SRTProjectVO> projects) {
		//TODO - Load Milestones for a Projects.
	}

	/**
	 * Load Master Record Data and populate projects with MasterRecordData
	 * @param projects
	 */
	private void loadMasterRecordData(Map<String, SRTProjectVO> projects) {
		//TODO - Load Master Records for Projects
	}

	/**
	 * Loads Map of Project Records from database for given projectIds.
	 * @param projectIds
	 * @return
	 */
	private Map<String, SRTProjectVO> loadProjects(List<String> projectIds) {
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
				if(project == null || !project.getProjectId().equals(rs.getString("project_id"))) {
					if(project != null) {
						projects.put(project.getProjectId(), project);
					}
					project = new SRTProjectVO(rs);
				}
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		return projects;
	}

	/**
	 * Load Project Sql.
	 * TODO - Columns need individually listed.
	 * @return
	 */
	private String getProjectSql(int projectCount) {
		String schema = StringUtil.checkVal(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		StringBuilder sql = new StringBuilder(1000);
		sql.append("Select p.*, r.* ").append(DBUtil.FROM_CLAUSE).append(schema);
		sql.append("dpy_syn_srt_project p "); 
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_request r on p.request_id = r.request_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_roster u on r.roster_id = u.roster_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile pr on u.profile_id = pr.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_project_milestone_xr xr on p.project_id = xr.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_milestone m on xr.milestone_id = m.milestone_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_request_address a on r.request_id = a.request_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster eng on p.engineer_id = eng.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile ep on eng.profile_id = ep.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster des on p.designer_id = des.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile dp on des.profile_id = dp.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("dpy_syn_srt_roster qa on p.quality_engineer_id = qa.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("profile qp on qa.profile_id = qp.profile_id ");
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