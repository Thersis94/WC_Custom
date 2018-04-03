package com.rezdox.action;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.data.MemberFormProcessor;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
 * <b>Title</b>: MemberAction.java<p/>
 * <b>Description: Manages member settings.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 24, 2018
 ****************************************************************************/
public class MemberAction extends SBActionAdapter {

	public enum MemberColumnName {
		MEMBER_ID, CREATE_DT, UPDATE_DT
	}

	public MemberAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MemberAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public MemberAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(retrieveMemberForm(req));
	}

	/**
	 * Retrieves the Member Settings form & saved form data
	 * @param req
	 */
	protected FormVO retrieveMemberForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Member Form: " + formId);

		// Get the form
		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadForm(formId, req, MemberFormProcessor.class);
		req.setAttribute(FormAction.FORM_DATA, dc);

		return dc.getForm();
	}


	/**
	 * Retrieves member settings data for the specified memberId
	 * @param memberId
	 * @return
	 */
	public MemberVO retrieveMemberData(String memberId) {
		return retrieveMemberData(memberId, null);
	}

	/**
	 * Retrieves member settings data for the specified memberId or profileId
	 * Overloaded to support the login module
	 * @param memberId
	 * @param profileId 
	 * @return
	 */
	public MemberVO retrieveMemberData(String memberId, String profileId) {
		String sql;
		List<Object> params;
		if (!StringUtil.isEmpty(profileId)) {
			sql = StringUtil.join(DBUtil.SELECT_FROM_STAR, getCustomSchema(), "rezdox_member where profile_id=?");
			params = Arrays.asList(profileId);
		} else {
			sql = StringUtil.join(DBUtil.SELECT_FROM_STAR, getCustomSchema(), "rezdox_member where member_id=?");
			params = Arrays.asList(memberId);
		}

		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<MemberVO> data = dbp.executeSelect(sql, params, new MemberVO());
		return !data.isEmpty() ? data.get(0) : new MemberVO();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		saveForm(req);

		MemberVO member = retrieveMemberData(req.getParameter("memberId"));
		putModuleData(member.getProfilePicPath(), 1, false);

		MemberVO userData = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
		userData.setPrivacyFlg(member.getPrivacyFlg());
		userData.setProfilePicPath(member.getProfilePicPath());
	}

	/**
	 * Saves a member form builder form
	 */
	protected void saveForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());

		// Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, MemberFormProcessor.class);
	}

	/**
	 * Saves partial member data
	 * 
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void saveSettings(ActionRequest req) throws DatabaseException {
		MemberVO newData = new MemberVO(req);

		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("rezdox_member ");
		sql.append("set privacy_flg = ?, profile_pic_pth = ? ");
		sql.append("where member_id = ? ");

		List<String> fields = Arrays.asList("privacy_flg", "profile_pic_pth", "member_id");

		// Save the member's updated settings
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), newData, fields);
		} catch(Exception e) {
			throw new DatabaseException(e);
		}
	}


	/**
	 * Create a list of members in the system, agnostic of login ability or status.
	 * used for the residence transfer UI.  Exclude the user ('self')
	 * @param req
	 * @return
	 */
	public List<MemberVO> listMembers(ActionRequest req) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(150);
		sql.append("select member_id, profile_id, first_nm, last_nm, email_address_txt from ").append(schema).append("REZDOX_MEMBER ");
		sql.append("where member_id != ? and profile_id is not null and email_address_txt is not null ");
		sql.append("order by last_nm, first_nm ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), Arrays.asList(RezDoxUtils.getMemberId(req)), new MemberVO());
	}
}