package com.depuysynthes.srt;

import static com.siliconmtn.util.MapUtil.entry;

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
import com.depuysynthes.srt.util.SRTUtil.SRTList;
import com.depuysynthes.srt.util.SRTUtil.SrtPage;
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
import com.siliconmtn.util.MapUtil;
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
	public static final String DB_PROJECT_ID = "PROJECT_ID";
	protected static final Map<String, String> sortCols = MapUtil.asMap (
		entry("projectName", "p.project_name"),
		entry("surgeonNm", "surgeon_nm"),
		entry("projectTypeTxt", "p.proj_type_id"),
		entry("projectStatusTxt", "p.proj_stat_id"),
		entry("createDt", "p.create_dt"),
		entry("surgDt", "p.surg_dt"),
		entry("deliveryDt", "p.delivery_dt"),
		entry("distributorship", "dld.label_txt"),
		entry("supplierNm", "sld.label_txt"),
		entry("mfgPoToVendor", "p.mfg_po_to_vendor"),
		entry("makeFromScratch", "p.make_from_scratch"),
		entry("total", "t.total")
	);

	public SRTProjectAction() {
		super();
	}

	public SRTProjectAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SRTMilestoneAction sma = (SRTMilestoneAction) ActionControllerFactoryImpl.loadAction(SRTMilestoneAction.class.getName(), this);
		MilestoneTypeId typeId = MilestoneTypeId.DATE;

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
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		//Determine what kind of build action is being performed.
		if(req.hasParameter("isSplit") && req.getBooleanParameter("isSplit")) {
			splitProject(req);
		} else if(req.hasParameter("isAdd") && req.getBooleanParameter("isAdd")) {
			copyProject(req);
		} else if(req.hasParameter("releaseLocks") && req.getBooleanParameter("releaseLocks")) {
			releaseLocks(req);
		} else {
			if(manageLock(req, false)) {
				saveProject(req);

				//Release all Locks on save.
				releaseLocks(req);
			} else {
				msg = "You do not own the lock on this record.  Save Rejected.";
			}
		}

		//Redirect the User.
		sbUtil.moduleRedirect(req, msg, SrtPage.PROJECT.getUrlPath());
	}


	/**
	 * Perform Copy of Project Record.
	 * @param req
	 * @throws ActionException 
	 */
	private void copyProject(ActionRequest req) throws ActionException {

		//Load Roster Data.
		SRTRosterAction sra = (SRTRosterAction)ActionControllerFactoryImpl.loadAction(SRTRosterAction.class.getName(), this);
		GridDataVO<SRTRosterVO> rosters = sra.loadRosterUsers(req);
		SRTRosterVO roster = rosters.getRowData().get(0);

		//Save the Request and set Roster Data on it.
		SRTRequestVO request = new SRTRequestVO(req);
		request.setRequestor(roster);
		request.setReqTerritoryId(roster.getTerritoryId());

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

		//Load Projects
		GridDataVO<SRTProjectVO> projects = new DBProcessor(dbConn).executeSQLWithCount(sql, vals, new SRTProjectVO(), req.getIntegerParameter("limit", 10), req.getIntegerParameter("offset", 0));

		if(!projects.getRowData().isEmpty()) {

			loadDetailData(projects.getRowData(), req);

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
		SRTMilestoneAction sma = (SRTMilestoneAction) ActionControllerFactoryImpl.loadAction(SRTMilestoneAction.class.getName(), this);
		sma.populateMilestones(projects);
	}

	/**
	 * Loads Request Data when we are looking at a single record.
	 * @param projects
	 * @param req
	 */
	private void loadDetailData(List<SRTProjectVO> projects, ActionRequest req) {

		//Load request Information and assign on Project Record.
		if(req.hasParameter(SRT_PROJECT_ID)) {
			SRTProjectVO p = projects.get(0);
			req.setParameter(SRTRequestAction.SRT_REQUEST_ID, p.getRequestId());
			SRTRequestAction sra = (SRTRequestAction) ActionControllerFactoryImpl.loadAction(SRTRequestAction.class.getName(), this);
			GridDataVO<SRTRequestVO> reqData = sra.loadRequests(req);
			if(reqData != null && !reqData.getRowData().isEmpty()) {
				p.setRequest(reqData.getRowData().get(0));
			}
		}

		//Load Master Record Data and assign on Project Record.
		SRTMasterRecordAction smra = (SRTMasterRecordAction) ActionControllerFactoryImpl.loadAction(SRTMasterRecordAction.class.getName(), this);
		smra.populateMasterRecordXR(projects);
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
				p.setBuyerNm(SRTUtil.decryptName(p.getBuyerNm(), se));
			}
		} catch (EncryptionException e) {
			log.error("Error Decrypting Project Names", e);
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
		String opCoId = SRTUtil.getOpCO(req);
		StringBuilder sql = new StringBuilder(2000);
		sql.append("select p.*, concat(pr.first_nm, ' ', pr.last_nm) as requestor_nm, ");
		sql.append("concat(req.surgeon_first_nm, ' ', req.surgeon_last_nm) as SURGEON_NM, ");
		sql.append("case when l.lock_id is not null then true else false end as LOCK_STATUS, ");
		sql.append("l.LOCKED_BY_ID, m.milestone_nm as proj_stat_txt, ");
		sql.append("type.label_txt as PROJ_TYPE_TXT ");
		//If this isn't a detail load, get Names for display purposes.
		if(!req.hasParameter(SRT_PROJECT_ID)) {
			sql.append(", concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
			sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
			sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm, ");
			sql.append("concat(bp.first_nm, ' ', bp.last_nm) as buyer_nm, ");
			sql.append("case when dld.label_txt is not null and dld.label_txt != '' then dld.label_txt else r.territory_id end as distributorship, t.total, sld.label_txt as supplier_nm ");
		}
		if("coProjectId".equals(req.getParameter("sort"))) {
			sql.append(", case when priority_id = 'FASTEST' or surg_dt is not null then 1 else 0 end as priority_flg, ");
			sql.append(" case when delivery_dt < current_timestamp then 1 else 0 end as lateFlg ");
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
		SRTUtil.buildListJoin(sql, "type", "p.proj_type_id");
		vals.add(SRTUtil.getListId(opCoId, SRTList.PROJ_TYPE));

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

			//Get Optional QA Engineer Information
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_ROSTER b ");
			sql.append("on p.BUYER_ID = b.ROSTER_ID ");
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE bp ");
			sql.append("on b.PROFILE_ID = bp.PROFILE_ID ");

			//Get Optional Supplier Nm
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("LIST_DATA sld ");
			sql.append("on sld.value_txt = p.supplier_id and sld.list_id = ? ");
			vals.add(SRTUtil.getListId(opCoId, SRTList.PROJ_VENDOR));

			//Get Optional Distributorship
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("LIST_DATA dld ");
			sql.append("on dld.value_txt = r.territory_id and dld.list_id = ? ");
			vals.add(SRTUtil.getListId(opCoId, SRTList.SRT_TERRITORIES));

			//Get Project Total
			sql.append(DBUtil.LEFT_OUTER_JOIN).append("(select cast(sum(case when part_count is null then 0 else part_count end) as int) as total, ");
			sql.append("project_id ").append(DBUtil.FROM_CLAUSE).append(custom);
			sql.append("dpy_syn_srt_master_record_project_xr xr ");
			sql.append(DBUtil.GROUP_BY).append(" project_id) as t on p.project_id = t.project_id ");
		}

		//Build Where Conditional and set and clause values on the vals list.
		buildWhereClause(sql, req, vals, statusType);

		//Add Order By clause.  Order by status (coProjectId) is special.
		if("coProjectId".equals(req.getParameter("sort"))) {
			String order = StringUtil.checkVal(req.getParameter("order"), "desc");
			sql.append(DBUtil.ORDER_BY).append("lateFlg ").append(order);
			sql.append(", priority_flg ").append(order).append(", project_hold_flg ").append(order);
			sql.append(", delivery_dt ").append(order).append(", surg_dt ").append(order);
		} else {
			sql.append(DBUtil.ORDER_BY).append(StringUtil.checkVal(sortCols.get(req.getParameter("sort")), sortCols.get("createDt")));
			sql.append(" ").append(StringUtil.checkVal(req.getParameter("order"), "desc"));
		}

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
			sql.append("and p.PROJECT_ID = ? or p.CO_PROJECT_ID = ? ");
			vals.add(req.getParameter(SRT_PROJECT_ID));
			vals.add(req.getParameter(SRT_PROJECT_ID));
		} else if(!StringUtil.isEmpty(statusType)) {
			sql.append("and p.PROJ_STAT_ID = ? ");
			vals.add(statusType);
		}

		if(req.hasParameter("search")) {
			sql.append("and lower(p.PROJECT_NAME) like ? ");
			vals.add("%" + req.getParameter("search").toLowerCase() + "%");
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
			log.error("Error Managing Lock", e);
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
				log.error("Error Retrieving Locks for a user", e);
			}
		}

		return activeLocks;
	}
}