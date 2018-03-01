package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;

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
	public SRTRequestAction() {
		super();
	}

	public SRTRequestAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SRTRosterVO roster = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);
		GridDataVO<SRTRequestVO> requests = loadRequests(roster, req);

		/*
		 * If requestId is present, Load Data from Form Retrieval,
		 * Else list SRTRequestVOs for Tables.
		 */
		if(req.hasParameter(SRT_REQUEST_ID)) {
			loadDataFromForms(req);
		}

		putModuleData(requests.getRowData(), requests.getTotal(), false);
	}
 
	/**
	 * Load data via the FormAction Tool
	 * @param req
	 * @throws ActionException
	 */
	private void loadDataFromForms(ActionRequest req) {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		log.debug("Retrieving Form : " + formId);

		DataManagerFacade dmf = new DataManagerFacade(attributes, dbConn, req);
		DataContainer dc = dmf.loadForm(formId);
		if (dc.hasErrors()) {
			for (Entry<String, Throwable> e : dc.getErrors().entrySet()) {
				log.error("Error retrieving form: ", e.getValue());
			}
		}

		dc.getForm().setCurrentTemplateNo(1);

		// Get saved data and return for re-display on the form.
		GenericQueryVO query = new GenericQueryVO(formId);
		QueryParamVO param = new QueryParamVO("REQUEST_ID", false);
		param.setValues(req.getParameterValues(SRT_REQUEST_ID));
		query.addConditional(param);
		dc.setQuery(query);
		dmf.loadTransactions(dc);
		req.setAttribute(FormAction.FORM_DATA, dc);
	}

	/**
	 * Load SRT Requests.
	 * @param roster
	 * @param parameter
	 * @return
	 */
	private GridDataVO<SRTRequestVO> loadRequests(SRTRosterVO roster, ActionRequest req) {
		List<Object> vals = new ArrayList<>();

		//if user is not an admin, add rosterId.
		if(req.hasParameter(SRT_REQUEST_ID)) {
			vals.add(req.getParameter(SRT_REQUEST_ID));
		}
//		if(!roster.isAdmin()) {
//			vals.add(roster.getRosterId());
//		}

		int limit = req.getIntegerParameter("limit", 10);
		int offset = req.getIntegerParameter("offset", 0);
		GridDataVO<SRTRequestVO> requests = new DBProcessor(dbConn).executeSQLWithCount(buildRequestLoadSql(roster.isAdmin(), req.hasParameter(SRT_REQUEST_ID)), vals, new SRTRequestVO(), limit, offset);

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
	private String buildRequestLoadSql(boolean hasUser, boolean hasRequestId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select r.*, a.*, u.profile_id from ").append(schema);
		sql.append("SRT_REQUEST r ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("SRT_ROSTER u on r.ROSTER_ID = u.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("SRT_REQUEST_ADDRESS a on r.REQUEST_ID = a.REQUEST_ID ");

		sql.append(DBUtil.WHERE_1_CLAUSE);
		if(hasRequestId) {
			sql.append("and r.request_id = ? ");
		} else if(hasUser) {
			sql.append("and u.roster_id = ? ");
		}
		sql.append("order by r.create_dt desc ");
		return sql.toString();
	}
}