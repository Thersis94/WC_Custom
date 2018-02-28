package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;

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

	public SRTMasterRecordAction() {
		super();
	}

	public SRTMasterRecordAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("masterRecordId")) {
			loadMasterRecord(req);
		} else {
			List<SRTMasterRecordVO> masterRecords = listRecords(req);
			super.putModuleData(masterRecords);
		}
	}

	/**
	 * Load the Form and if necessary, load the Form Data.
	 * @param req
	 */
	private void loadMasterRecord(ActionRequest req) {
		//Load Form Information
		FormAction fa = (FormAction) this.getConfiguredAction(FormAction.class.getName());
		//Load formData if necessary
		if(!"ADD".equals(req.getParameter("masterRecordId"))) {
			fa.retrieveSubmittedForm(req);
		}
	}

	/**
	 * List All MasterRecords for an opCo
	 * @param req
	 * @return
	 */
	private List<SRTMasterRecordVO> listRecords(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		vals.add(SRTUtil.getOpCO(req));
		return new DBProcessor(dbConn).executeSelect(listMasterRecordsSql(req), vals, new SRTMasterRecordVO());
	}

	/**
	 * Build the Master Record Retrieval Query.
	 * @return
	 */
	private String listMasterRecordsSql(ActionRequest req) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("SRT_MASTER_RECORD mr ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" mr.OP_CO_ID = ? ");
		sql.append("order by ").append(StringUtil.checkVal(req.getParameter("order"), "mr.create_dt"));

		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		//TODO Save Master Record Data in Form Framework.
	}
}