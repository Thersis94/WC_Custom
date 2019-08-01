package com.rezdox.data;

import java.util.Iterator;
import java.util.Map;

import com.rezdox.action.RezDoxUtils;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
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
 * @author James McKain
 * @version 1.0
 * @since Apr 17, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class MemberFormProcessor extends FormDataProcessor {

	/**
	 * Request parameter names for form field slug_txt values that go to the member table
	 */
	public enum MemberField {
		MEMBER_PRIVACY_FLAG("privacyFlg", false), 
		MEMBER_PROFILE_PIC_PATH("profilePicPath", true),
		FIRST_NM("firstName", false),
		LAST_NM("lastName", false),
		EMAIL("emailAddress", false),
		PASSWORD("password", false),
		ADDRESS("address", false),
		ADDRESS2("address2", false),
		CITY("city", false),
		STATE("state", false),
		ZIP("zipCode", false),
		COUNTRY("countryCode", false);

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
				String response = param.isFile() ? saveFile(req, entry.getKey(), param.getReqParam()) : entry.getValue().getResponseText();
				req.setParameter(param.getReqParam(), response);
			}
		}

		//save the user's wc-profile.  This includes profile, profile_address, phone_number, and org_profile_comm
		try {
			String profileId = addProfile(data);
			req.setParameter("profileId", profileId);
		} catch (ActionException e) {
			throw new DatabaseException(e);
		}
	}


	@Override
	protected void saveFiles(FormTransactionVO data) {
		//override to flush superclass behavior - we don't want the profile image going into ProfileDocument
	}


	/**
	 * Saves the profile image to secure binary
	 * @param req
	 * @param fieldName
	 * @param paramName
	 * @return
	 */
	protected String saveFile(ActionRequest req, String fieldName, String paramName) {
		FilePartDataBean fpdb = req.getFile("frm_" + fieldName);
		//no file, return the exist file presumed to be in a specific hidden field
		if (fpdb == null || !fpdb.isFileData()) return req.getParameter(paramName + "_orig");

		String secBinaryPath = (String) attributes.get(com.siliconmtn.http.filter.fileupload.Constants.SECURE_PATH_TO_BINARY);
		String orgId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		// Root for the organization (RezDox)
		String orgRoot = StringUtil.join(secBinaryPath, (String)attributes.get(Constants.ORGANIZATION_ALIAS), orgId);

		// Root for this member's files
		String rootMemberPath = StringUtil.join("/member/", RezDoxUtils.getMemberId(req), "/");

		// Store the files to the file system
		try {
			FileLoader fl = new FileLoader(attributes);
			fl.setData(fpdb.getFileData());
			fl.setFileName(fpdb.getFileName());
			fl.setPath(orgRoot + rootMemberPath);
			fl.writeFiles();
			fl.reorientFiles();
			log.debug("saved file to " + orgRoot + rootMemberPath);
			return rootMemberPath + fpdb.getFileName();

		} catch (Exception e) {
			log.error("Could not store member file", e);
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
