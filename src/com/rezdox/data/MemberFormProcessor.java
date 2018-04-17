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
		MEMBER_PRIVACY_FLAG("privacyFlg", false), 
		MEMBER_PROFILE_PIC_PATH("profilePicPath", true);
		//TODO move all other member fields off the form onto the request where MemberVO will expect them.

		private String reqParam;
		private boolean isFile;
		private MemberField(String reqParam, boolean isFile) {
			this.reqParam = reqParam;
			this.isFile = isFile;
		}

		public String getReqParam() { return reqParam; }
		public boolean isFile() { return isFile; }
	}


	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public MemberFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}


	/* 
	 * Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Member Form Data");

		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			MemberField param = EnumUtil.safeValueOf(MemberField.class, entry.getValue().getSlugTxt());
			
			// Add parameters to the request to be saved to the member or wc-profile tables
			if (param != null) {
				String response = param.isFile() ? saveFile(req, param.getReqParam()) : entry.getValue().getResponseText();
				req.setParameter(param.getReqParam(), response);
				iter.remove();
			}
		}
	}


	/* 
	 * Saves the profile image to secure binary
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataProcessor#saveFiles(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFile(ActionRequest req, String paramName) {
		FilePartDataBean fpdb = req.getFile(paramName);
		//no file, return the exist file presumed to be in a specific hidden field
		if (fpdb == null || fpdb.getFile() == null) return req.getParameter(paramName + "_orig");

		String secBinaryPath = (String) getAttribute(com.siliconmtn.http.filter.fileupload.Constants.SECURE_PATH_TO_BINARY);
		String orgId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		// Root for the organization (RezDox)
		String orgRoot = StringUtil.join(secBinaryPath, (String)getAttribute(Constants.ORG_ALIAS), orgId);

		// Root for this member's files
		String rootMemberPath = StringUti.join("/member/", RezDoxUtils.getMemberId(req), "/");

		// Store the files to the file system
		try {
			FileLoader fl = new FileLoader(getAttributes());
			fl.setData(fpdb.getFileData());
			fl.setFileName(fpdb.getFileName());
			fl.setPath(orgRoot + rootMemberPath);
			fl.writeFiles();

			return rootMemberPath + fpdb.getFileName();

		} catch (Exception e) {
			log.error("Could not write RezDox member file", e);
			return null;
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
