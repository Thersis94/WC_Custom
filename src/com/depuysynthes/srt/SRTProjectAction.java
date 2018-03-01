package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTProjectAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages SRT Project Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
public class SRTProjectAction extends SimpleActionAdapter {

	public enum DisplayType {ENGINEERING, POST_ENGINEERING, UNASSIGNED, MY_PROJECTS}
	private FormAction fa;
	public SRTProjectAction() {
		super();
		fa = new FormAction();
	}

	public SRTProjectAction(ActionInitVO init) {
		super(init);
		fa = new FormAction();
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String projectId = req.getParameter("projectId");

		if(!StringUtil.isEmpty(projectId)) {
			SRTProjectVO project = getProjectDetails(req, projectId);
			putModuleData(project);
		} else {
			GridDataVO<SRTProjectVO> projects = loadProjects(req);
			putModuleData(projects.getRowData(), projects.getTotal(), false);
		}
	}


	/**
	 * Load Project Data via Form Framework.
	 * @param opCoId
	 * @param projectId
	 * @return
	 */
	private SRTProjectVO getProjectDetails(ActionRequest req, String projectId) {

		fa.retrieveSubmittedForm(req);

		//Only load data if this isn't a new record
		if(!"ADD".equals(projectId)) {
			log.info("Loading Existing Record.");
		}
		return null;
	}

	/**
	 * Retrieve Projects according to request attributes.
	 * @param req
	 * @return
	 */
	private GridDataVO<SRTProjectVO> loadProjects(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		SRTRosterVO roster = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);

		//Holds Engineering, Post Engineering, Unassigned, My Projects.
		DisplayType displayType = EnumUtil.safeValueOf(DisplayType.class, req.getParameter("displayType"));
		if(displayType == null) {
			displayType = DisplayType.ENGINEERING;
		}

		//We always set Op Co Id off user so they are restricted to their data set.
		vals.add(roster.getOpCoId());

		//Build Query and populate required vals at same time.
		String sql = buildProjectRetrievalQuery(req, vals, displayType);

		//Load Projects
		GridDataVO<SRTProjectVO> projects = new DBProcessor(dbConn).executeSQLWithCount(sql, vals, new SRTProjectVO(), req.getIntegerParameter("limit", 10), req.getIntegerParameter("offset", 0));

		if(!projects.getRowData().isEmpty()) {
			//Decrypt Project Record Name Fields.
			decryptProjectNames(projects);

			//Add Milestone Data
			SRTUtil.populateMilestones(projects.getRowData(), dbConn, getCustomSchema());
		}

		//Return Projects
		return projects;
	}

	/**
	 * Iterate Projects decrypting User names.
	 * @param projects
	 */
	private void decryptProjectNames(GridDataVO<SRTProjectVO> projects) {
		try {
			StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
			for(SRTProjectVO p : projects.getRowData()) {
				p.setRequestorNm(SRTUtil.decryptName(p.getRequestorNm(), se));
				p.setEngineerNm(SRTUtil.decryptName(p.getEngineerNm(), se));
				p.setDesignerNm(SRTUtil.decryptName(p.getDesignerNm(), se));
				p.setQualityEngineerNm(SRTUtil.decryptName(p.getQualityEngineerNm(), se));
			}
		} catch (EncryptionException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Builds the Project Retrieval Query.
	 * @param displayType 
	 * @param vals 
	 * @param rosterId
	 * @param projectId
	 * @return
	 */
	private String buildProjectRetrievalQuery(ActionRequest req, List<Object> vals, DisplayType displayType) {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(100);
		sql.append("select p.*, req.*, ");
		sql.append("concat(pr.first_nm, ' ', pr.last_nm) as requestor_nm, ");
		sql.append("concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
		sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
		sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(custom).append("SRT_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("SRT_MASTER_RECORD_PROJECT_XR xr ");
		sql.append("on xr.PROJECT_ID = p.PROJECT_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_REQUEST req ");
		sql.append("on p.REQUEST_ID = req.REQUEST_ID ");

		//Get Requestor Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_ROSTER r ");
		sql.append("on req.ROSTER_ID = r.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE pr ");
		sql.append("on r.PROFILE_ID = pr.PROFILE_ID ");

		//Get Optional Engineer Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_ROSTER e ");
		sql.append("on p.ENGINEER_ID = e.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE ep ");
		sql.append("on e.PROFILE_ID = ep.PROFILE_ID ");

		//Get Optional Designer Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_ROSTER d ");
		sql.append("on p.DESIGNER_ID = d.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE dp ");
		sql.append("on d.PROFILE_ID = dp.PROFILE_ID ");

		//Get Optional QA Engineer Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_ROSTER q ");
		sql.append("on p.QUALITY_ENGINEER_ID = q.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE qp ");
		sql.append("on q.PROFILE_ID = qp.PROFILE_ID ");

		buildWhereClause(sql, req, vals, displayType);

		sql.append(DBUtil.ORDER_BY).append(StringUtil.checkVal(req.getParameter("orderBy"), "p.create_dt"));

		return sql.toString();
	}

	/**
	 * Build the where clause of the query.
	 * @param sql
	 * @param req
	 * @param vals
	 * @param displayType
	 */
	private void buildWhereClause(StringBuilder sql, ActionRequest req, List<Object> vals, DisplayType displayType) {
		sql.append(DBUtil.WHERE_1_CLAUSE).append(" and p.OP_CO_ID = ? ");

		//If my Project, add roster ID for comparisons.  Else sort by status.
		if(DisplayType.MY_PROJECTS.equals(displayType)) {
			SRTRosterVO roster = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);

			sql.append("and (req.roster_id = ? or engineer_id = ? ");
			sql.append("or quality_engineer_id = ? or designer_id = ?) ");

			vals.add(roster.getRosterId());
			vals.add(roster.getRosterId());
			vals.add(roster.getRosterId());
			vals.add(roster.getRosterId());
		} else {
			sql.append("and PROJECT_STATUS = ? ");
			vals.add(displayType.name());
		}
	}
}