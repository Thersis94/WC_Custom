package com.depuysynthes.srt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.data.MasterRecordDataProcessor;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.util.SRTUtil.SrtPage;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
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
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;

/****************************************************************************
 * <b>Title:</b> SRTMasterRecordAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages SRT Master Records.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
public class SRTMasterRecordAction extends SimpleActionAdapter {

	public static final String SRT_MASTER_RECORD_ID = "masterRecordId";
	public static final String MASTER_RECORD_DATA = "masterRecordData";

	public SRTMasterRecordAction() {
		super();
	}

	public SRTMasterRecordAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void copy(ActionRequest req) throws ActionException {
		if(req.hasParameter(SRT_MASTER_RECORD_ID)) {
			GridDataVO<SRTMasterRecordVO> record = loadMasterRecordData(req);
			if(!record.getRowData().isEmpty()) {

				//Get first record available.
				SRTMasterRecordVO r = record.getRowData().get(0);

				//TODO - Get feedback on what fields need reset on a copy.
				//Wipe out Master Record Id to ensure Copy
				r.setMasterRecordId(null);

				//Wipe out Part No so new one is created
				r.setPartNo("replaceMe");

				//Update title Text
				r.setTitleTxt(StringUtil.join(r.getTitleTxt(), " (copy)"));

				//Get a MasterRecordDataProcessor for Saving
				MasterRecordDataProcessor mrdp = new MasterRecordDataProcessor(dbConn, attributes, req);

				//Save
				try {
					mrdp.saveMasterRecordData(r);
				} catch (DatabaseException e) {
					log.error("Error Copying Record", e);
				}
			}
		}
	}


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter(SRT_MASTER_RECORD_ID) || req.hasParameter("json")) {
			GridDataVO<SRTMasterRecordVO> masterRecords = loadMasterRecordData(req);

			putModuleData(masterRecords.getRowData(), masterRecords.getTotal(), false);
		}
	}

	/**
	 * Load Master Record Data.
	 * @param req
	 * @return
	 */
	private GridDataVO<SRTMasterRecordVO> loadMasterRecordData(ActionRequest req) {
		GridDataVO<SRTMasterRecordVO> masterRecords = loadMasterRecords(req);

		/*
		 * If masterRecordId is present, Load Data from Form Retrieval,
		 * Else list SRTMasterRecordVOs for Tables.
		 */
		if(req.hasParameter(SRT_MASTER_RECORD_ID)) {

			//Load Form Data.
			loadDataFromForms(req);

			//Load Map of Attributes.
			Map<String, String> masterRecordAttrs = loadRecordAttributes(req.getParameter(SRT_MASTER_RECORD_ID), SRTUtil.getOpCO(req));

			//Ensure MasterRecordAttributes are set on the VO.
			if(!masterRecords.getRowData().isEmpty()) {
				masterRecords.getRowData().get(0).setAttributes(masterRecordAttrs);
			}

			//Store map of attributes on the request.
			req.setAttribute("masterRecordAttrs", masterRecordAttrs);
		}

		return masterRecords;
	}

	/**
	 * Load Map of Attributes for the Given Op Co.  If masterRecordId is
	 * also passed, then attempt to load stored values for the Attributes.
	 * @param srtMasterRecordVO
	 * @return 
	 * @return
	 */
	public Map<String, String> loadRecordAttributes(String masterRecordId, String opCoId) {
		boolean hasMasterRecord = !StringUtil.isEmpty(masterRecordId) && !"ADD".equals(masterRecordId);
		Map<String, String> attrs = new HashMap<>();
		try(PreparedStatement ps = dbConn.prepareStatement(buildAttrLoadSql(hasMasterRecord))) {
			int cnt = 1;
			if(hasMasterRecord) { 
				ps.setString(cnt++, masterRecordId);	
			}

			ps.setString(cnt, opCoId);
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				attrs.put(rs.getString("ATTR_ID"), rs.getString("VALUE_TXT"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		return attrs;
	}

	/**
	 * Build Sql to retrieve all for a given opco and if we have a
	 * MasterRecord, join out to get stored values for each Attribute.
	 * @return
	 */
	private String buildAttrLoadSql(boolean hasMasterRecord) {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.attr_id, ");
		if(hasMasterRecord) {
			sql.append(" x.value_txt ");
		} else {
			sql.append(" '' as value_txt ");
		}
		sql.append(DBUtil.FROM_CLAUSE).append(custom).append("DPY_SYN_SRT_MR_ATTR_OP_CO_XR a ");

		if(hasMasterRecord) {
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_MR_ATTR_XR x ");
			sql.append("on a.attr_id = x.attr_id and x.master_record_id = ? ");
		}

		sql.append(DBUtil.WHERE_CLAUSE).append(" a.op_co_id = ? ");

		return sql.toString();
	}

	/**
	 * Load Master Record Form.
	 * @param req
	 */
	private void loadDataFromForms(ActionRequest req) {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		log.debug("Retrieving Form : " + formId);

		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadFormWithData(formId, req, null, MasterRecordDataProcessor.class);
		req.setAttribute(FormAction.FORM_DATA, dc);
	}

	/**
	 * List All MasterRecords for an opCo
	 * @param req
	 * @return
	 */
	private GridDataVO<SRTMasterRecordVO> loadMasterRecords(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		vals.add(SRTUtil.getOpCO(req));
		if(req.hasParameter("isSearch")) {
			vals.add(req.getParameter("term").toLowerCase());
			vals.add(req.getParameter("term").toLowerCase());
		} else if(req.hasParameter(SRT_MASTER_RECORD_ID)) {
			vals.add(req.getParameter(SRT_MASTER_RECORD_ID));
		}
		int limit = req.getIntegerParameter("limit", 10);
		int offset = req.getIntegerParameter("offset", 0);
		return new DBProcessor(dbConn).executeSQLWithCount(listMasterRecordsSql(req), vals, new SRTMasterRecordVO(), limit, offset);
	}

	/**
	 * Build the Master Record Retrieval Query.
	 * @return
	 */
	private String listMasterRecordsSql(ActionRequest req) {
		boolean getById = req.hasParameter(SRT_MASTER_RECORD_ID);
		boolean isSearch = req.hasParameter("isSearch");

		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(custom);
		sql.append("DPY_SYN_SRT_MASTER_RECORD mr ");
		if(getById) {
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_FILE f ");
			sql.append("on mr.MASTER_RECORD_ID = f.MASTER_RECORD_ID ");
		}
		sql.append(DBUtil.WHERE_CLAUSE).append(" mr.OP_CO_ID = ? ");
		if(isSearch) {
			sql.append("and lower(mr.part_no) like '%' + ? + '%' or lower(mr.title_txt) like '%' + ? + '%' ");
		}
		else if(getById) {
			sql.append("and mr.master_record_id = ? ");
		}
		sql.append("order by ").append(StringUtil.checkVal(req.getParameter("order"), "mr.create_dt desc"));

		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		//Check if we're doing a copy or regular save.
		if(req.hasParameter("isCopy")) {
			copy(req);
		} else {
			saveMasterRecord(req);
		}
		/*
		 * If this is an ajax call, return the MasterRecord we worked on.
		 * Otherwise this is a standard edit, redirect.
		 */
		if(req.hasParameter("amid")) {
			super.putModuleData(req.getAttribute(MASTER_RECORD_DATA));
		} else {
			//Redirect the User.
			sbUtil.moduleRedirect(req, msg, SrtPage.MASTER_RECORD.getUrlPath());
		}
	}

	/**
	 * Saves Master Record Data.
	 * @param req
	 */
	private void saveMasterRecord(ActionRequest req) {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		//Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		//Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, MasterRecordDataProcessor.class);

	}

	/**
	 * Load the Master Records associated with given SRTProjectVO
	 * @param p
	 * @return
	 */
	public List<SRTMasterRecordVO> loadMasterRecordXR(SRTProjectVO p) {
		return new DBProcessor(dbConn, getCustomSchema()).executeSelect(buildXrQuery(), Arrays.asList(p.getProjectId()), new SRTMasterRecordVO());
	}

	/**
	 * Build the Master Record Lookup Query against the Xr Table.
	 * @return
	 */
	private String buildXrQuery() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select x.*, mr.title_txt, mr.part_no from ").append(schema).append("DPY_SYN_SRT_MASTER_RECORD_PROJECT_XR x ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("DPY_SYN_SRT_MASTER_RECORD mr ");
		sql.append("on x.MASTER_RECORD_ID = mr.MASTER_RECORD_ID and x.PROJECT_ID = ? ");
		sql.append(DBUtil.ORDER_BY).append("x.CREATE_DT");
		return sql.toString();
	}
}