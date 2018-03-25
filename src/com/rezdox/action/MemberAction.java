package com.rezdox.action;

import java.util.ArrayList;
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
	 * 
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
	 * Retrieves member settings data for the specified member
	 * 
	 * @param memberId
	 * @return
	 */
	public MemberVO retrieveMemberData(String memberId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select member_id, profile_id, register_submittal_id, status_flg, privacy_flg, profile_pic_pth, create_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("rezdox_member ");
		sql.append("where member_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(memberId);
		
		DBProcessor dbp = new DBProcessor(dbConn);
		List<MemberVO> data = dbp.executeSelect(sql.toString(), params, new MemberVO());
		
		return data.get(0);
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
}