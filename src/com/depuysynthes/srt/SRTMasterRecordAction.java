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
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
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

	public SRTMasterRecordAction() {
		super();
	}

	public SRTMasterRecordAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter(SRT_MASTER_RECORD_ID) || req.hasParameter("json")) {
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

				//Store map of attributes on the request.
				req.setAttribute("masterRecordAttrs", masterRecordAttrs);
			}

			putModuleData(masterRecords.getRowData(), masterRecords.getTotal(), false);
		}
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
		sql.append(DBUtil.FROM_CLAUSE).append(custom).append("SRT_MR_ATTR_OP_CO_XR a ");

		if(hasMasterRecord) {
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_MR_ATTR_XR x ");
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
		if(req.hasParameter(SRT_MASTER_RECORD_ID)) {
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
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(custom);
		sql.append("SRT_MASTER_RECORD mr ");
		if(getById) {
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("SRT_FILE f ");
			sql.append("on mr.MASTER_RECORD_ID = f.MASTER_RECORD_ID ");
		}
		sql.append(DBUtil.WHERE_CLAUSE).append(" mr.OP_CO_ID = ? ");
		if(getById) {
			sql.append("and mr.master_record_id = ? ");
		}
		sql.append("order by ").append(StringUtil.checkVal(req.getParameter("order"), "mr.create_dt"));

		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String formId = (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1);

		//Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		//Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, MasterRecordDataProcessor.class);

		//Redirect the User.
		sbUtil.moduleRedirect(req, attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE), "/master-record");
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
		sql.append("select * from ").append(schema).append("SRT_MASTER_RECORD_PROJECT_XR x ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("SRT_MASTER_RECORD mr ");
		sql.append("on x.MASTER_RECORD_ID = mr.MASTER_RECORD_ID and x.PROJECT_ID = ? ");
		sql.append(DBUtil.ORDER_BY).append("x.CREATE_DT");
		return sql.toString();
	}
}