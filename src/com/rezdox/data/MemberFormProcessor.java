package com.rezdox.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rezdox.action.MemberAction;
import com.rezdox.action.RezDoxUtils;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.FormDataProcessor;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <p><b>Title</b>: MemberFormProcessor</p>
 * <p><b>Description: </b>Writes the member form data to the db.</p>
 * <p> 
 * <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 24, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class MemberFormProcessor extends FormDataProcessor {
	
	/**
	 * Request parameter names for form field slug_txt values that go to the member table
	 */
	public enum MemberField {
		MEMBER_PRIVACY_FLAG("privacyFlg"), MEMBER_PROFILE_PIC_PATH("profilePicPath");

		private String reqParam;
		
		private MemberField(String reqParam) {
			this.reqParam = reqParam;
		}
		
		public String getReqParam() { return reqParam; }
	}

	/**
	 * DB field names, request param names for form field slug_txt values that are file uploads
	 */
	public enum MemberFile {
		MEMBER_PROFILE_IMAGE("profilePicPath", "profile_pic_pth");

		private String reqParam;
		private String dbField;
		
		private MemberFile(String reqParam, String dbField) {
			this.reqParam = reqParam;
			this.dbField = dbField;
		}
		
		public String getReqParam() { return reqParam; }
		public String getDbField() { return dbField; }
	}
	
	/**
	 * Maps submitted form builder parameter names to those expected in the member table 
	 */
	private Map<String, GenericVO> fileMap;
	
	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public MemberFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
		fileMap = new HashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Member Form Transaction Data");
		
		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		// Remove from the form field map so they aren't saved as attributes.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			
			// Add parameters to the request to be saved to the residence table
			MemberField param = EnumUtil.safeValueOf(MemberField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), entry.getValue().getResponseText());
				iter.remove();
			}
			
			// Get form builder parameter names for files and map them to their member conterparts
			MemberFile fileParam = EnumUtil.safeValueOf(MemberFile.class, entry.getValue().getSlugTxt());
			if (fileParam != null) {
				fileMap.put(entry.getValue().getFieldNm(), new GenericVO(fileParam.getReqParam(), fileParam.getDbField()));
				iter.remove();
			}
		}
		
		// Save the Member Data
		MemberAction ma = new MemberAction(dbConn, attributes);
		try {
			ma.saveSettings(req);
		} catch (Exception e) {
			throw new DatabaseException("Could not save member settings", e);
		}
	}

	/* 
	 * Saves the files to secure binary
	 * TODO: This should be genericized and moved to the RezDoxUtils class
	 * 
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataProcessor#saveFiles(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFiles(FormTransactionVO data) {
		String secBinaryPath = (String) attributes.get(com.siliconmtn.http.filter.fileupload.Constants.SECURE_PATH_TO_BINARY);
		String orgId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		
		// Root for the organization (RezDox)
		String orgRoot = secBinaryPath + (String) attributes.get("orgAlias") + orgId;
		
		// Root for this member's files
		String rootMemberPath = "/member/" + RezDoxUtils.getMemberId(req) + "/";

		// Root upload path
		String rootUploadPath = orgRoot + rootMemberPath;

		// Store the files to the file system
		List<String> dbFields = new ArrayList<>();
		try {
			for(FilePartDataBean fpdb : req.getFiles()) {
				FileLoader fl = new FileLoader(attributes);
				fl.setData(fpdb.getFileData());
				fl.setFileName(fpdb.getFileName());
				fl.setPath(rootUploadPath);
				fl.writeFiles();
				
				GenericVO field = fileMap.get(fpdb.getKey());
				if (field != null) {
					req.setParameter((String) field.getKey(), rootMemberPath + fpdb.getFileName());
					dbFields.add((String) field.getValue());
				}
			}
		} catch (Exception e) {
			log.error("Could not write RezDox member file", e);
		}
		
		// Store file data to the db
		if (!dbFields.isEmpty()) {
			saveFileInfo(dbFields);
		}
	}
	
	/**
	 * Save file data to the existing member record
	 * 
	 * @param dbFields
	 */
	private void saveFileInfo(List<String> dbFields) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_member set ");
		
		int cnt = 0;
		for (String field : dbFields) {
			sql.append(cnt++ > 0 ? ", " : "").append(field).append(" = ? ");
		}
		
		sql.append("where member_id = ? ");
		dbFields.add("member_id");
		
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), new MemberVO(req), dbFields);
		} catch (Exception e) {
			log.error("Couldn't save member file data", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataTransaction#saveFieldData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFieldData(FormTransactionVO data) throws DatabaseException {
		// Nothing to do at this time
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		// Nothing to do at this time
		return dc;
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#flushTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected void flushTransactions(DataContainer dc) {
		// Nothing to do at this time
	}

}
