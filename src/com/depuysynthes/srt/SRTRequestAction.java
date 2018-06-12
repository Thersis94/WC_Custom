package com.depuysynthes.srt;

import static com.siliconmtn.util.MapUtil.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.data.RequestDataProcessor;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SRTList;
import com.depuysynthes.srt.util.SRTUtil.SrtPage;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;

/****************************************************************************
 * <b>Title:</b> SRTRequestAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages SRT Request Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
public class SRTRequestAction extends SimpleActionAdapter {

	public static final String SRT_REQUEST_ID = "requestId";
	protected static final Map<String, String> sortCols = MapUtil.asMap (
		entry("createDt", "r.create_dt"),
		entry("surgeonNm", "surgeon_nm"),
		entry("reasonTxt", "REASON_FOR_REQUEST_TXT"),
		entry("projectStatus", "proj_stat_id"),
		entry("hospitalName", "r.hospital_nm"),
		entry("qtyNo", "r.qty_no"),
		entry("estimatedRoi", "r.estimated_roi"),
		entry("chargeTo", "r.charge_to"),
		entry("reqTerritoryId", "request_territory_id")
	);

	public SRTRequestAction() {
		super();
	}

	public SRTRequestAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter(SRT_REQUEST_ID) || req.hasParameter("json")) {
			GridDataVO<SRTRequestVO> requests = loadRequests(req);

			/*
			 * If requestId is present, Load Data from Form Retrieval,
			 * Else list SRTRequestVOs for Tables.
			 */
			if(req.hasParameter(SRT_REQUEST_ID)) {
				loadDataFromForms(req);
			}

			putModuleData(requests.getRowData(), requests.getTotal(), false);
		}
	}
 
	@Override
	public void build(ActionRequest req) throws ActionException {
		if(!req.hasParameter(SRT_REQUEST_ID)) {
			ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

			//Place ActionInit on the Attributes map for the Data Save Handler.
			attributes.put(Constants.ACTION_DATA, actionInit);

			//Call DataManagerUtil to save the form.
			new DataManagerUtil(attributes, dbConn).saveForm(formId, req, RequestDataProcessor.class);

			//Redirect the User.
			sbUtil.moduleRedirect(req, attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE), SrtPage.REQUEST.getUrlPath());
		}
	}

	/**
	 * Load Form Data.
	 * @param req
	 * @throws ActionException
	 */
	private void loadDataFromForms(ActionRequest req) {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		log.debug("Retrieving Form : " + formId);

		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadFormWithData(formId, req, null, RequestDataProcessor.class);
		req.setAttribute(FormAction.FORM_DATA, dc);
	}

	/**
	 * Load SRT Requests.
	 * @param roster
	 * @param parameter
	 * @return
	 */
	public GridDataVO<SRTRequestVO> loadRequests(ActionRequest req) {
		List<Object> vals = new ArrayList<>();

		int limit = req.getIntegerParameter("limit", 10);
		int offset = req.getIntegerParameter("offset", 0);
		GridDataVO<SRTRequestVO> requests = new DBProcessor(dbConn).executeSQLWithCount(buildRequestLoadSql(req, vals), vals, new SRTRequestVO(), limit, offset);

		log.debug(StringUtil.join("Found ", Integer.toString(requests.getTotal()), " Requests."));

		//Get List of Users
		List<SRTRosterVO> users = getRosters(requests.getRowData());

		try {
			//Populate SRT Roster Records via reference.
			ProfileManagerFactory.getInstance(attributes).populateRecords(dbConn, users);
		} catch (DatabaseException e) {
			log.error("Problem Populating Requestor Records.", e);
		}

		return requests;
	}

	/**
	 * Extract Request Roster Records to a list.
	 * @param requests
	 * @return
	 */
	private List<SRTRosterVO> getRosters(List<SRTRequestVO> requests) {
		List<SRTRosterVO> users = new ArrayList<>();

		for(SRTRequestVO r : requests) {
			users.add(r.getRequestor());
		}

		return users;
	}

	/**
	 * Build the SRT Request Load Sql.
	 * @return
	 */
	private String buildRequestLoadSql(ActionRequest req, List<Object> vals) {
		String schema = getCustomSchema();
		String opCoId = SRTUtil.getOpCO(req);
		StringBuilder sql = new StringBuilder(1500);
		if(req.hasParameter(SRT_REQUEST_ID)) {
			sql.append("select r.*, a.*, u.profile_id, m.milestone_nm as PROJ_STAT_ID,");
			sql.append("l.label_txt as REASON_FOR_REQUEST_TXT ");
		} else {
			sql.append("select r.request_id, r.roster_id, r.op_co_id, r.co_req_id, ");
			sql.append("r.hospital_nm, r.surgeon_first_nm, r.surgeon_last_nm, ");
			sql.append("r.qty_no, r.request_desc, r.estimated_roi, r.create_dt, ");
			sql.append("a.*, u.profile_id, m.milestone_nm as PROJ_STAT_ID, ");
			sql.append("l.label_txt as REASON_FOR_REQUEST_TXT, ");
			sql.append("concat(r.surgeon_first_nm, ' ', r.surgeon_last_nm) as SURGEON_NM, ");
			sql.append("lct.label_txt as charge_to, lt.label_txt as request_territory_id ");
		}

		sql.append(DBUtil.FROM_CLAUSE).append(schema);
		sql.append("DPY_SYN_SRT_REQUEST r ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("DPY_SYN_SRT_ROSTER u on r.ROSTER_ID = u.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("DPY_SYN_SRT_REQUEST_ADDRESS a on r.REQUEST_ID = a.REQUEST_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("DPY_SYN_SRT_PROJECT p on p.request_id = r.request_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("DPY_SYN_SRT_MILESTONE m on p.PROJ_STAT_ID = m.milestone_id ");
		SRTUtil.buildListJoin(sql, "l", "r.reason_for_request");
		vals.add(SRTUtil.getListId(opCoId, SRTList.REQ_REASON));
		if(!req.hasParameter(SRT_REQUEST_ID)) {
			SRTUtil.buildListJoin(sql, "lct", "r.charge_to");
			vals.add(SRTUtil.getListId(opCoId, SRTList.CHARGE_TO));

			SRTUtil.buildListJoin(sql, "lt", "r.request_territory_id");
			vals.add(SRTUtil.getListId(opCoId, SRTList.SRT_TERRITORIES));

		}

		//Build Where clause
		buildWhereClause(sql, req, vals);

		//Add Order By clause.
		sql.append(DBUtil.ORDER_BY).append(StringUtil.checkVal(sortCols.get(req.getParameter("sort")), sortCols.get("createDt")));
		sql.append(" ").append(StringUtil.checkVal(req.getParameter("order"), "desc"));
		return sql.toString();
	}

	/**
	 * Build the Request Sql Where Clause
	 * @param req
	 * @param vals
	 */
	private void buildWhereClause(StringBuilder sql, ActionRequest req, List<Object> vals) {
		SRTRosterVO roster = SRTUtil.getRoster(req);
		sql.append(DBUtil.WHERE_1_CLAUSE).append("and r.OP_CO_ID = ? ");
		vals.add(SRTUtil.getOpCO(req));

		if(req.hasParameter(SRT_REQUEST_ID)) {
			sql.append("and r.request_id = ? ");
			vals.add(req.getParameter(SRT_REQUEST_ID));
		} else if(!roster.isAdmin()) {
			sql.append("and u.roster_id = ? ");
			vals.add(roster.getRosterId());
		}
	}
}