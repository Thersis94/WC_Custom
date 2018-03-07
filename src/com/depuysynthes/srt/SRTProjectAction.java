package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.srt.data.ProjectDataProcessor;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
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
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;

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
	public static final String SRT_PROJECT_ID = "projectId";

	public SRTProjectAction() {
		super();
	}

	public SRTProjectAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter(SRT_PROJECT_ID) || req.hasParameter("json")) {
			GridDataVO<SRTProjectVO> projects = loadProjects(req);

			if(req.hasParameter(SRT_PROJECT_ID)) {
				loadDataFromForms(req);
			}

			putModuleData(projects.getRowData(), projects.getTotal(), false);
		}
	}


	@Override
	public void build(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		//Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		//Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, ProjectDataProcessor.class);

		//Redirect the User.
		sbUtil.moduleRedirect(req, attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE), "/projects");
	}

	/**
	 * Load Form Data.
	 * @param req
	 */
	private void loadDataFromForms(ActionRequest req) {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		log.debug("Retrieving Form : " + formId);

		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadFormWithData(formId, req, null, ProjectDataProcessor.class);
		req.setAttribute(FormAction.FORM_DATA, dc);
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

			//If this is a detail request, load additional data.
			if(req.hasParameter(SRT_PROJECT_ID)) {
				SRTProjectVO p = projects.getRowData().get(0);
				loadDetailData(p, req);
			}

			//Decrypt Project Record Name Fields.
			decryptProjectNames(projects);

			//Add Milestone Data
			SRTUtil.populateMilestones(projects.getRowData(), dbConn, getCustomSchema());
		}

		//Return Projects
		return projects;
	}

	/**
	 * Loads Request Data when we are looking at a single record.
	 * @param p
	 * @param req
	 */
	private void loadDetailData(SRTProjectVO p, ActionRequest req) {

		//Load request Information and assign on Project Record.
		req.setParameter(SRTRequestAction.SRT_REQUEST_ID, p.getRequestId());
		SRTRequestAction sra = (SRTRequestAction) getConfiguredAction(SRTRequestAction.class.getName());
		GridDataVO<SRTRequestVO> reqData = sra.loadRequests(req);
		if(reqData != null && !reqData.getRowData().isEmpty()) {
			p.setRequest(reqData.getRowData().get(0));
		}

		//Load Master Record Data and assign on Project Record.
		SRTMasterRecordAction smra = (SRTMasterRecordAction) getConfiguredAction(SRTMasterRecordAction.class.getName());
		List<SRTMasterRecordVO> prodData = smra.loadMasterRecordXR(p);
		for(SRTMasterRecordVO mr : prodData) {
			p.addMasterRecord(mr);
		}
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
		sql.append("select p.*, concat(pr.first_nm, ' ', pr.last_nm) as requestor_nm ");

		//If this isn't a detail load, get Names for display purposes.
		if(!req.hasParameter(SRT_PROJECT_ID)) {
			sql.append(", concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
			sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
			sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm ");
		}

		//Joins to tables.
		sql.append(DBUtil.FROM_CLAUSE).append(custom).append("SRT_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("SRT_REQUEST req ");
		sql.append("on p.request_id = req.request_id ");

		//Get Requestor Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_ROSTER r ");
		sql.append("on req.ROSTER_ID = r.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE pr ");
		sql.append("on r.PROFILE_ID = pr.PROFILE_ID ");

		//Load Optional User Data if this isn't a detail view.
		if(!req.hasParameter(SRT_PROJECT_ID)) {
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
		}

		//Build Where Conditional and set and clause values on the vals list.
		buildWhereClause(sql, req, vals, displayType);

		//Add Order By clause.
		sql.append(DBUtil.ORDER_BY).append(StringUtil.checkVal(req.getParameter("orderBy"), "p.create_dt desc"));

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

		if(req.hasParameter(SRT_PROJECT_ID)) {
			sql.append("and p.PROJECT_ID = ? ");
			vals.add(req.getParameter(SRT_PROJECT_ID));
		}

		//If my Project, add roster ID for comparisons.  Else sort by status.
		else if(DisplayType.MY_PROJECTS.equals(displayType)) {
			SRTRosterVO roster = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);

			sql.append("and (req.roster_id = ? or engineer_id = ? ");
			sql.append("or quality_engineer_id = ? or designer_id = ?) ");

			vals.add(roster.getRosterId());
			vals.add(roster.getRosterId());
			vals.add(roster.getRosterId());
			vals.add(roster.getRosterId());
		} else {
			//sql.append("and PROJECT_STATUS = ? ");
			//vals.add(displayType.name());
		}
	}
}