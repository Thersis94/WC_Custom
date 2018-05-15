package com.depuysynthes.srt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.srt.vo.SRTFileVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title:</b> SRTFileAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Loading SRT File Records and also authorizing
 * access to them via BinaryFileHandlerServlet.  Files are written via
 * relevant saveHandlers.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 1, 2018
 ****************************************************************************/
public class SRTFileAction extends SimpleActionAdapter {

	public enum RelationType {
			PROJECT(SRTProjectAction.SRT_PROJECT_ID), MASTER_RECORD(SRTMasterRecordAction.SRT_MASTER_RECORD_ID), REQUEST(SRTRequestAction.SRT_REQUEST_ID);
		private String reqParamNm;
		RelationType(String reqParamNm) {
			this.reqParamNm = reqParamNm;
		}
		public String getReqParamNm() {return this.reqParamNm;}
	}

	public SRTFileAction() {
		super();
	}

	public SRTFileAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter(ProfileDocumentAction.PROFILE_DOC_ID)) {
			String profileDocumentId = req.getParameter(ProfileDocumentAction.PROFILE_DOC_ID);
			RelationType rel = getRelType(req);

			if (rel != null) {
				processProfileDocumentRequest(profileDocumentId, rel, req.getParameter(rel.getReqParamNm()));
			}
		} else if(req.hasParameter(RelationType.REQUEST.getReqParamNm()) || req.hasParameter(RelationType.MASTER_RECORD.getReqParamNm())){
			List<SRTFileVO> files = loadFiles(req.getParameter("requestId"), req.getParameter("masterRecordId"));
			putModuleData(files, files.size(), false);
		}
	}

	/**
	 * Load Files related to the given requestId and/or masterRecordId
	 * @param requestId
	 * @param masterRecordId
	 * @return
	 */
	private List<SRTFileVO> loadFiles(String requestId, String masterRecordId) {
		List<Object> vals = new ArrayList<>();
		String sql = getLoadFilesSql(requestId, masterRecordId, vals);
		return new DBProcessor(dbConn, getCustomSchema()).executeSelect(sql, vals, new SRTFileVO());
	}

	/**
	 * @param requestId
	 * @param masterRecordId
	 * @param vals
	 * @return
	 */
	private String getLoadFilesSql(String requestId, String masterRecordId, List<Object> vals) {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("DPY_SYN_SRT_FILE ").append(DBUtil.WHERE_1_CLAUSE);

		/*
		 * Set MasterRecordId if requestId is empty.
		 * Set RequestId if masterRecordId is empty.
		 * Set both if neither is empty.
		 */
		if(StringUtil.isEmpty(requestId)) {
			sql.append("and master_Record_Id = ? ");
			vals.add(masterRecordId);
		} else if(StringUtil.isEmpty(masterRecordId)) {
			sql.append("and request_id = ? ");
			vals.add(requestId);
		} else {
			sql.append("and (master_record_id = ? or request_id = ? ");
			vals.add(masterRecordId);
			vals.add(requestId);
		}

		return sql.toString();
	}

	/**
	 * Look for the Relation Type on the Request.
	 * @param req
	 * @return
	 */
	private RelationType getRelType(ActionRequest req) {
		for(RelationType r : RelationType.values()) {
			if(req.hasParameter(r.getReqParamNm())) {
				return r;
			}
		}
		return null;
	}

	/**
	 * processes a request for a profile document
	 * @param user 
	 * @param profileDocumentId 
	 * @param rel 
	 * @throws ActionException 
	 */
	private void processProfileDocumentRequest(String profileDocumentId, RelationType rel, String relId) throws ActionException {
		String schema = getCustomSchema();
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setActionInit(actionInit);
		pda.setDBConnection(dbConn);
		pda.setAttributes(attributes);
		ProfileDocumentVO pvo = pda.getDocumentByProfileDocumentId(profileDocumentId);

		//This determines if they can actually see it.
		StringBuilder sql = new StringBuilder(205);
		sql.append("select count(*) from profile_document pd ");
		sql.append("inner join ").append(schema).append("dpy_syn_srt_file f ");
		sql.append("on f.profile_document_id = pd.profile_document_id ");
		sql.append("and pd.feature_id = ? ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		switch(rel) {
			case MASTER_RECORD:
				sql.append("DPY_SYN_SRT_MASTER_RECORD mr on ");
				sql.append("mr.master_record_id = pd.feature_id ");
				break;
			case PROJECT:
				sql.append("DPY_SYN_SRT_PROJECT p on ");
				sql.append("p.project_id = pd.feature_id ");
				break;
			case REQUEST:
				sql.append("DPY_SYN_SRT_REQUEST r on ");
				sql.append("r.request_id = pd.feature_id ");
				break;
			default:
				break;
			
		}
		sql.append("where pd.profile_document_id = ? ");

		log.debug("sql: " + sql + "|" + profileDocumentId );

		authorizeFileRetrieval(sql,profileDocumentId, relId, pvo);
	}

	/**
	 * this method checks that the profile document id matches one that is an insight and also has the featured flg set 
	 * to 1 or it throws and catches a not authorized exception.
	 * @param profileDocumentId 
	 * @param sql 
	 * @param pvo 
	 */
	private void authorizeFileRetrieval(StringBuilder sql, String profileDocumentId, String relId, ProfileDocumentVO pvo){

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, relId);
			ps.setString(2, profileDocumentId);
			int rowCount = 0;

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				rowCount = rs.getInt(1);
			}

			if (rowCount != 0 ){
				//will need a module data vo to send data back to the file handler
				ModuleVO modVo = new ModuleVO();
				modVo.setActionData(pvo);
				attributes.put(Constants.MODULE_DATA, modVo);
			}

		} catch(SQLException sqle) {
			log.error("could not load or verify profile document ", sqle);
		}
	}
}
