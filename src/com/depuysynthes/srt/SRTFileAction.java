package com.depuysynthes.srt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTFileAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 1, 2018
 ****************************************************************************/
public class SRTFileAction extends SimpleActionAdapter {

	public enum RelationType {
			PROJECT("projectId"), MASTER_RECORD("masterRecordId"), REQUEST("requestId");
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

	public void retrieve(ActionRequest req) throws ActionException {
		String profileDocumentId = req.getParameter("profileDocumentId");
		RelationType rel = getRelType(req);
		if (!StringUtil.isEmpty(profileDocumentId) && rel != null) {
			processProfileDocumentRequest(profileDocumentId, rel, req.getParameter(rel.getReqParamNm()));
		} 
	}

	/**
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
		String schema = "custom.";
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setActionInit(actionInit);
		pda.setDBConnection(dbConn);
		pda.setAttributes(attributes);
		ProfileDocumentVO pvo = pda.getDocumentByProfileDocumentId(profileDocumentId);

		//This determines if they can actually see it.
		StringBuilder sql = new StringBuilder(205);
		sql.append("select count(*) from profile_document pd ");
		sql.append("inner join ").append(schema).append("dpy_syn_srt_file f ");
		sql.append("on f.file_id = pd.profile_document_id ");
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
		sql.append("where profile_document_id = ? ");

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
