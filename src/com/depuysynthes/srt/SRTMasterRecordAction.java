package com.depuysynthes.srt;

import static com.siliconmtn.util.MapUtil.entry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import com.siliconmtn.util.MapUtil;
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
	public static final String SRT_MASTER_RECORD_PART_NO = "partNo";
	protected static final Map<String, String> sortCols = MapUtil.asMap (
			entry(SRT_MASTER_RECORD_PART_NO, "mr.part_no"),
			entry("titleTxt", "mr.title_txt"),
			entry("totalBuilt", "mr.total_built"),
			entry("obsoleteFlg", "mr.obsolete_flg"),
			entry("qualitySystemId", "quality_system_id"),
			entry("prodTypeId", "prod_type_id"),
			entry("complexityId", "complexity_id"),
			entry("prodCatId", "prod_cat_id"),
			entry("prodFamilyId", "prod_family_id"),
			entry("createDt", "mr.create_dt")
			);
	public SRTMasterRecordAction() {
		super();
	}

	public SRTMasterRecordAction(ActionInitVO init) {
		super(init);
	}


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if((req.hasParameter(SRT_MASTER_RECORD_ID) || req.hasParameter(SRT_MASTER_RECORD_PART_NO)) || req.hasParameter("json")) {
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
		int limit = req.getIntegerParameter("limit", 10);
		int offset = req.getIntegerParameter("offset", 0);
		return new DBProcessor(dbConn).executeSQLWithCount(listMasterRecordsSql(req, vals), vals, new SRTMasterRecordVO(), limit, offset);
	}

	/**
	 * Build the Master Record Retrieval Query.
	 * @param vals 
	 * @return
	 */
	private String listMasterRecordsSql(ActionRequest req, List<Object> vals) {
		boolean idLookup = req.hasParameter(SRT_MASTER_RECORD_ID) || req.hasParameter(SRT_MASTER_RECORD_PART_NO);
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(1200);

		if(!idLookup) {
			sql.append("select mr.master_record_id, mr.op_co_id, mr.part_no, ");
			sql.append("mr.title_txt, mr.make_from_part_nos, mr.total_built, ");
			sql.append("mr.obsolete_flg, mr.obsolete_reason, mr.create_dt, ");
			sql.append("lqs.label_txt as quality_system_id, lpt.label_txt as prod_type_id, ");
			sql.append("lc.label_txt as complexity_id, lpc.label_txt as prod_cat_id, ");
			sql.append("lpf.label_txt as prod_family_id ");
		} else {
			sql.append("select mr.*, f.* ");
		}

		sql.append(DBUtil.FROM_CLAUSE).append(custom);
		sql.append("DPY_SYN_SRT_MASTER_RECORD mr ");
		if(idLookup) {
			sql.append(DBUtil.LEFT_OUTER_JOIN).append(custom).append("DPY_SYN_SRT_FILE f ");
			sql.append("on mr.MASTER_RECORD_ID = f.MASTER_RECORD_ID ");
		}

		if(!idLookup) {
			//Join out for List Values.
			SRTUtil.buildListJoin(sql, "lqs", "mr.quality_system_id");
			vals.add(SRTUtil.SRTList.QUALITY_SYSTEM.name());

			SRTUtil.buildListJoin(sql, "lpt", "mr.prod_type_id");
			vals.add(SRTUtil.SRTList.PRODUCT_TYPE.name());

			SRTUtil.buildListJoin(sql, "lc", "mr.complexity_id");
			vals.add(SRTUtil.SRTList.COMPLEXITY.name());

			SRTUtil.buildListJoin(sql, "lpc", "mr.prod_cat_id");
			vals.add(SRTUtil.SRTList.PROD_CAT.name());

			SRTUtil.buildListJoin(sql, "lpf", "mr.prod_family_id");
			vals.add(SRTUtil.SRTList.PROD_FAMILY.name());
		}

		//Build Where Clause
		buildWhereListClause(sql, req, vals);

		//Add Order By clause.
		sql.append(DBUtil.ORDER_BY).append(StringUtil.checkVal(sortCols.get(req.getParameter("sort")), sortCols.get(SRT_MASTER_RECORD_PART_NO)));
		sql.append(" ").append(StringUtil.checkVal(req.getParameter("order"), "desc"));

		return sql.toString();
	}

	/**
	 * Helper method builds the Where clause for MasterRecord Lookup.
	 * Manages setting data on the vals which will return by ref.
	 * @param sql
	 * @param req
	 * @param vals
	 */
	private void buildWhereListClause(StringBuilder sql, ActionRequest req, List<Object> vals) {
		boolean isSearch = req.hasParameter("isSearch") || req.hasParameter("search");

		sql.append(DBUtil.WHERE_CLAUSE).append(" mr.OP_CO_ID = ? ");
		vals.add(SRTUtil.getOpCO(req));

		/*
		 * Search comes in 2 different ways.
		 * search - From MasterRecord BS Table
		 * term - From Project MasterRecord TypeAhead
		 */
		if(isSearch) {
			sql.append("and lower(mr.part_no) like '%' + ? + '%' or lower(mr.title_txt) like '%' + ? + '%' ");
			if(req.hasParameter("isSearch")) {
				vals.add(req.getParameter("term").toLowerCase());
				vals.add(req.getParameter("term").toLowerCase());
			} else {
				vals.add(req.getParameter("search").toLowerCase());
				vals.add(req.getParameter("search").toLowerCase());
			}
		} else if(req.hasParameter(SRT_MASTER_RECORD_ID)) {
			sql.append("and mr.master_record_id = ? ");
			vals.add(req.getParameter(SRT_MASTER_RECORD_ID));
		} else if (req.hasParameter(SRT_MASTER_RECORD_PART_NO)) {
			sql.append("and mr.part_no = ? ");
			vals.add(req.getParameter(SRT_MASTER_RECORD_PART_NO));
		}
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		//Check if we're doing a copy or regular save.
		if(req.hasParameter("isCopy")) {
			copyMasterRecord(req);
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
	 * Copies a Master Record.
	 * @param req
	 */
	private void copyMasterRecord(ActionRequest req) {
		if(req.hasParameter(SRT_MASTER_RECORD_ID)) {
			GridDataVO<SRTMasterRecordVO> record = loadMasterRecordData(req);
			if(!record.getRowData().isEmpty()) {

				//Get first record available.
				SRTMasterRecordVO r = record.getRowData().get(0);

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
	 * @param projects
	 * @return
	 */
	public void populateMasterRecordXR(List<SRTProjectVO> projects) {
		List<Object> vals = new ArrayList<>();
		for(SRTProjectVO p : projects) {
			vals.add(p.getProjectId());
		}

		/*
		 * Load MasterRecordXrs using ProjectVO to prevent MasterRecordVO
		 * aggregating XRs with same MasterRecordId
		 */
		List<SRTProjectVO> projXrs = new DBProcessor(dbConn, getCustomSchema()).executeSelect(buildXrQuery(vals.size()), vals, new SRTProjectVO());

		if(!projXrs.isEmpty()) {
			Map<String, SRTProjectVO> pMap = SRTUtil.mapProjects(projects);
			//Migrate all masterRecordXR Results out of projectVO to List.
			for(SRTProjectVO p : projXrs) {
				pMap.get(p.getProjectId()).setMasterRecords(p.getMasterRecords());
			}
		}

	}

	/**
	 * Build the Master Record Lookup Query against the Xr Table.
	 * @param size 
	 * @return
	 */
	private String buildXrQuery(int size) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select x.*, mr.title_txt, mr.part_no from ").append(schema).append("DPY_SYN_SRT_MASTER_RECORD_PROJECT_XR x ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("DPY_SYN_SRT_MASTER_RECORD mr ");
		sql.append("on x.MASTER_RECORD_ID = mr.MASTER_RECORD_ID and x.PROJECT_ID in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") ").append(DBUtil.ORDER_BY).append("x.CREATE_DT");
		log.debug(sql.toString());
		return sql.toString();
	}
}