package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.depuysynthes.srt.data.ProjectDataProcessor;
import com.depuysynthes.srt.data.RequestDataProcessor;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SrtPage;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO.MilestoneTypeId;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.util.LockUtil;
import com.smt.sitebuilder.util.LockVO;

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

	public enum DisplayType {ENGINEERING, PRODUCTION, UNASSIGNED, MY_PROJECTS}
	public static final String SRT_PROJECT_ID = "projectId";
	private static final String SRT_PROJECT_LOCKS = "srtProjectLocks";

	public SRTProjectAction() {
		super();
	}

	public SRTProjectAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void copy(ActionRequest req) throws ActionException {

		//Load Roster Data.
		SRTRosterAction sra = (SRTRosterAction)ActionControllerFactoryImpl.loadAction(SRTRosterAction.class.getName(), this);
		List<SRTRosterVO> rosters = sra.loadRosterUsers(req);
		SRTRosterVO roster = rosters.get(0);

		//Save the Request and set Roster Data on it.
		SRTRequestVO request = new SRTRequestVO(req);
		request.setRequestor(roster);
		request.setReqTerritoryId(roster.getTerritory());

		try {

			//Generate a RequestDataProcessor
			RequestDataProcessor rdp = new RequestDataProcessor(dbConn, attributes, req);
			SRTRequestAddressVO address = new SRTRequestAddressVO(req);
			SRTProjectVO project = null;

			rdp.saveRequestData(request, address, null);

			/*
			 * If this is a Copy, Project record will be copied via given
			 * projectId.  Otherwise, generate a new Project as we would
			 * on a standard Project Request.
			 */
			if(!req.hasParameter(SRT_PROJECT_ID)) {
				project = rdp.buildProjectRecord(roster);
			} else {
				GridDataVO<SRTProjectVO> projects = loadProjects(req);
				if(!projects.getRowData().isEmpty()) {
					project = new SRTProjectVO(projects.getRowData().get(0));
					project.setRequestId(request.getRequestId());
					project.setRequest(request);
				}
			}

			//Save Project Record.
			new ProjectDataProcessor(dbConn, attributes, req).saveProjectRecord(project);

			log.debug(project);

		} catch (Exception e) {
			log.error("Unable to add/copy Project.", e);
		}
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SRTMilestoneAction sma = (SRTMilestoneAction) ActionControllerFactoryImpl.loadAction(SRTMilestoneAction.class.getName(), this);
		MilestoneTypeId typeId = null;

		if(req.hasParameter(SRT_PROJECT_ID) || req.hasParameter("json")) {
			GridDataVO<SRTProjectVO> projects = loadProjects(req);

			if(req.hasParameter(SRT_PROJECT_ID)) {
				loadDataFromForms(req);
				req.setAttribute("ownsLock", StringUtil.checkVal(manageLock(req, false)));
			}

			putModuleData(projects.getRowData(), projects.getTotal(), false);
		} else {
			typeId = MilestoneTypeId.STATUS;
		}

		/*
		 * Always Ensure we have a valid up to date Lock Map.  Should only
		 * be relevant on first hit to the system.  Ensures that if a user
		 * leaves and session times out before save is hit to release locks,
		 * the next time they log in and access the project page, their
		 * old locks are loaded.
		 */
		if(req.getSession().getAttribute(SRT_PROJECT_LOCKS) == null) {
			req.getSession().setAttribute(SRT_PROJECT_LOCKS, getMyLocks(req));
		}

		//Retrieve list of Milestones from DB for Request.
		List<SRTProjectMilestoneVO> milestones = sma.loadMilestoneData(SRTUtil.getOpCO(req), typeId, null, false);
		req.setAttribute("milestones", milestones);
	}


	@Override
	public void build(ActionRequest req) throws ActionException {

		//Determine what kind of build action is being performed.
		if(req.hasParameter("isSplit") && req.getBooleanParameter("isSplit")) {
			splitProject(req);
		} else if(req.hasParameter("isAdd") && req.getBooleanParameter("isAdd")) {
			copy(req);
		} else if(req.hasParameter("releaseLocks") && req.getBooleanParameter("releaseLocks")) {
			releaseLocks(req);
		} else {
			saveProject(req);

			//Release all Locks on save.
			releaseLocks(req);
		}

		//Redirect the User.
		sbUtil.moduleRedirect(req, attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE), SrtPage.PROJECT.getUrlPath());
	}

	/**
	 * Splits a Project Record.  If a project has more than one MasterRecords
	 * tied to it, new Project Records are created for each Master Record.
	 * @param req
	 */
	private void splitProject(ActionRequest req) {

		//Attempt to Load Project Record
		GridDataVO<SRTProjectVO> project = loadProjects(req);

		//Fast fail if we don't find a project.
		if(project.getRowData().isEmpty()) {
			log.debug("No Project Found for provided projectId");
			return;
		}

		//Get Actual Project Record and verify we can split it.
		SRTProjectVO p = project.getRowData().get(0);
		if(p.getMasterRecords().isEmpty() || p.getMasterRecords().size() < 2) {
			log.debug("Project has a single MasterRecord and can't be split.");
			return;
		}

		//overwrite current Master Records with new Empty List.
		p.setMasterRecords(new ArrayList<>());

		/*
		 * Update/Clone the Project Record with each MasterRecord.
		 */
		boolean autoCommit = true;
		try {
			autoCommit = dbConn.getAutoCommit();
			dbConn.setAutoCommit(false);

			//Get ProjectDataProcessor for Saving Records.
			ProjectDataProcessor pdp = new ProjectDataProcessor(dbConn, attributes, req);

			//Save the Project.
			pdp.saveProjectRecord(p);

			dbConn.commit();
			dbConn.setAutoCommit(autoCommit);
		} catch(Exception e) {
			log.error(StringUtil.join("Problem Splitting the Project Record: ", req.getParameter(SRT_PROJECT_ID)));
			DBUtil.setAutoCommit(dbConn, autoCommit);
		}
	}

	/**
	 * Save the Project Record on a Form Submission.
	 * @param req
	 */
	private void saveProject(ActionRequest req) {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		//Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		//Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, ProjectDataProcessor.class);
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

		//Holds Engineering, Post Engineering, Unassigned, My Projects.
		String statusType = req.getParameter("statusType");

		//Build Query and populate required vals at same time.
		String sql = buildProjectRetrievalQuery(req, vals, statusType);

		log.debug(sql);

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
			loadMilestoneDetails(projects.getRowData());
		}

		//Return Projects
		return projects;
	}

	/**
	 * Load MilestoneData for the given project Records.
	 * @param projects
	 */
	private void loadMilestoneDetails(List<SRTProjectVO> projects) {
		SRTMilestoneAction sma = (SRTMilestoneAction) getConfiguredAction(SRTMilestoneAction.class.getName());
		sma.populateMilestones(projects);
	}

	/**
	 * Loads Request Data when we are looking at a single record.
	 * @param project
	 * @param req
	 */
	private void loadDetailData(SRTProjectVO project, ActionRequest req) {

		//Load request Information and assign on Project Record.
		req.setParameter(SRTRequestAction.SRT_REQUEST_ID, project.getRequestId());
		SRTRequestAction sra = (SRTRequestAction) getConfiguredAction(SRTRequestAction.class.getName());
		GridDataVO<SRTRequestVO> reqData = sra.loadRequests(req);
		if(reqData != null && !reqData.getRowData().isEmpty()) {
			project.setRequest(reqData.getRowData().get(0));
		}

		//Load Master Record Data and assign on Project Record.
		SRTMasterRecordAction smra = (SRTMasterRecordAction) getConfiguredAction(SRTMasterRecordAction.class.getName());
		List<SRTMasterRecordVO> prodData = smra.loadMasterRecordXR(project);
		for(SRTMasterRecordVO mr : prodData) {
			project.addMasterRecord(mr);
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
	 * @param req 
	 * @param vals 
	 * @param statusType
	 * @return
	 */
	private String buildProjectRetrievalQuery(ActionRequest req, List<Object> vals, String statusType) {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(100);
		sql.append("select p.*, concat(pr.first_nm, ' ', pr.last_nm) as requestor_nm, ");
		sql.append("req.surgeon_first_nm, req.surgeon_last_nm, ");
		sql.append("case when l.lock_id is not null then true else false end as LOCK_STATUS, ");
		sql.append("m.milestone_nm as proj_stat_txt, ");
		sql.append("type.label_txt as PROJ_TYPE_TXT ");
		//If this isn't a detail load, get Names for display purposes.
		if(!req.hasParameter(SRT_PROJECT_ID)) {
			sql.append(", concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
			sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
			sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm ");
		}

		//Joins to tables.
		sql.append(DBUtil.FROM_CLAUSE).append(custom).append("DPY_SYN_SRT_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(custom).append("DPY_SYN_SRT_REQUEST req ");
		sql.append("on p.request_id = req.request_id ");

		//Load Lock Status
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("RECORD_LOCK l ");
		sql.append("on l.record_id = p.project_id and unlock_dt is null ");

		//Get Requestor Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_ROSTER r ");
		sql.append("on req.ROSTER_ID = r.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE pr ");
		sql.append("on r.PROFILE_ID = pr.PROFILE_ID ");

		//Load Human Friendly Text for List Ref Values.

		//Status
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom);
		sql.append("DPY_SYN_SRT_MILESTONE m on p.PROJ_STAT_ID = m.milestone_id ");

		//Project Type
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("LIST_DATA type on ");
		sql.append("type.value_txt = p.PROJ_TYPE_ID and type.list_id = ? ");
		vals.add("PROJ_TYPE");

		//Load Optional User Data if this isn't a detail view.
		if(!req.hasParameter(SRT_PROJECT_ID)) {
			//Get Optional Engineer Information
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_ROSTER e ");
			sql.append("on p.ENGINEER_ID = e.ROSTER_ID ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE ep ");
			sql.append("on e.PROFILE_ID = ep.PROFILE_ID ");

			//Get Optional Designer Information
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_ROSTER d ");
			sql.append("on p.DESIGNER_ID = d.ROSTER_ID ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE dp ");
			sql.append("on d.PROFILE_ID = dp.PROFILE_ID ");

			//Get Optional QA Engineer Information
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_ROSTER q ");
			sql.append("on p.QUALITY_ENGINEER_ID = q.ROSTER_ID ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE qp ");
			sql.append("on q.PROFILE_ID = qp.PROFILE_ID ");
		}

		//Build Where Conditional and set and clause values on the vals list.
		buildWhereClause(sql, req, vals, statusType);

		//Add Order By clause.
		sql.append(DBUtil.ORDER_BY).append(StringUtil.checkVal(req.getParameter("orderBy"), "p.create_dt desc"));

		return sql.toString();
	}

	/**
	 * Build the where clause of the query.
	 * @param sql
	 * @param req
	 * @param vals
	 * @param statusType
	 */
	private void buildWhereClause(StringBuilder sql, ActionRequest req, List<Object> vals, String statusType) {
		sql.append(DBUtil.WHERE_1_CLAUSE).append(" and p.OP_CO_ID = ? ");
		vals.add(SRTUtil.getRoster(req).getOpCoId());

		if(req.hasParameter(SRT_PROJECT_ID)) {
			sql.append("and p.PROJECT_ID = ? ");
			vals.add(req.getParameter(SRT_PROJECT_ID));
		} else if(!StringUtil.isEmpty(statusType)) {
			sql.append("and p.PROJ_STAT_ID = ? ");
			vals.add(statusType);
		}

		if(req.getBooleanParameter("myProjectsFlg")) {
			SRTRosterVO roster = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);
			buildMyProjectsClause(sql, vals, roster);
		}
	}

	/**
	 * Builds the My Project Clause of the Project Retrieval Query.
	 * @param sql
	 * @param vals
	 * @param roster
	 */
	private void buildMyProjectsClause(StringBuilder sql, List<Object> vals, SRTRosterVO roster) {
		sql.append("and (req.roster_id = ? or engineer_id = ? ");
		sql.append("or quality_engineer_id = ? or designer_id = ?) ");

		vals.add(roster.getRosterId());
		vals.add(roster.getRosterId());
		vals.add(roster.getRosterId());
		vals.add(roster.getRosterId());
	}

	/**
	 * Helper method that manages locking records and determining if a
	 * user owns the lock on a record or not.
	 * @param req
	 * @param unlock
	 * @return
	 */
	private boolean manageLock(ActionRequest req, boolean unlock) {
		LockUtil locker = new LockUtil(dbConn);
		Map<String, LockVO> activeLocks = getMyLocks(req);

		String projectId = req.getParameter(SRT_PROJECT_ID);
		SRTRosterVO rosterVO = SRTUtil.getRoster(req);
		LockVO lock = null;
		boolean ownsLock = true;

		try {
			if(unlock) {
				locker.releaseLock(projectId, rosterVO);
				activeLocks.remove(projectId);
			} else {
				lock = locker.getLock(projectId);
				if(lock != null) {
					ownsLock = lock.getLockedById().equals(rosterVO.getProfileId());
				} else {
					activeLocks.put(projectId, locker.createLock(projectId, "SRT_PROJECTS", rosterVO));
				}
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}

		req.getSession().setAttribute(SRT_PROJECT_LOCKS, activeLocks);
		return ownsLock;
	}

	/**
	 * Helper method that releases all locks tied to the user.
	 * @param req
	 */
	private void releaseLocks(ActionRequest req) {
		LockUtil locker = new LockUtil(dbConn);
		SRTRosterVO rosterVO = SRTUtil.getRoster(req);

		Map<String, LockVO> locks = getMyLocks(req);
		Iterator<Entry<String, LockVO>> iter = locks.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, LockVO> e = iter.next();
			LockVO lock = e.getValue();
			if(lock.getUnlockDt() == null) {
				try {
					locker.releaseLock(lock.getRecordId(), rosterVO);
				} catch (InvalidDataException | DatabaseException err) {
					log.error(StringUtil.join("Unable to release Lock: ", lock.getRecordId()), err);
				}
				iter.remove();
			}
		}
	}

	/**
	 * Helper method to get a Users Locks out of the system and ensure they
	 * are on their session Object.  If already loaded, then just returns
	 * the session map.
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, LockVO> getMyLocks(ActionRequest req) {
		Map<String, LockVO> activeLocks = (Map<String, LockVO>) req.getSession().getAttribute(SRT_PROJECT_LOCKS);
		if(activeLocks == null) {
			activeLocks = new HashMap<>();
			LockUtil locker = new LockUtil(dbConn);
			SRTRosterVO rosterVO = SRTUtil.getRoster(req);
			try {
				List<LockVO> locks = locker.getUserLocks(rosterVO, "SRT_PROJECTS");

				//Filter out all unlocked Records and convert to map.
				activeLocks = locks
							.stream()
							.filter(l -> l.getUnlockDt() == null)
							.collect(Collectors.toMap(LockVO::getRecordId, Function.identity()));
			} catch (InvalidDataException e) {
				log.error("Error Processing Code", e);
			}
		}

		return activeLocks;
	}
}