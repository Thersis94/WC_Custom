package com.mts.subscriber.action;

import java.util.ArrayList;
import java.util.List;

import com.mts.publication.data.MTSDocumentVO;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.UserExtendedVO;
import com.mts.subscriber.data.UserExtendedVO.TypeCode;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: UserInfoWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the extended user information and preferences 
 * for items such as bookmarks, notes, etc...  Data is stored in an entity model
 * and typed on codes
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 11, 2019
 * @updates:
 ****************************************************************************/

public class UserInfoWidget extends SimpleActionAdapter {
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "user-info";
	
	/**
	 * 
	 */
	public UserInfoWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public UserInfoWidget(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.getBooleanParameter("json")) return;
		
		UserDataVO user = this.getAdminUser(req);
		String userId = ((MTSUserVO) user.getUserExtendedInfo()).getUserId();
		if ("BOOKMARK".equalsIgnoreCase(req.getParameter("typeCode"))) 
			setModuleData(getBookmarks(userId));
		else if (! StringUtil.isEmpty(req.getParameter("typeCode")))
			setModuleData(getData(userId, TypeCode.valueOf(req.getParameter("typeCode"))));
		else 
			setModuleData(getData(userId, null));
		
	}
	
	/**
	 * Gets the list of extended attributes
	 * @param userId USer to filter data
	 * @param typeCode type of extended data to retrieve.  null retrieves all types
	 * @return
	 */
	public List<UserExtendedVO> getData(String userId, TypeCode typeCode) {
		List<Object> vals = new ArrayList<>();
		vals.add(userId);
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("mts_user_info ");
		sql.append("where user_id = ? ");
		if (typeCode != null) {
			sql.append("and user_info_type_cd = ? ");
			vals.add(typeCode.name());
		}
		
		sql.append("order by user_info_type_cd ");
		log.info(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new UserExtendedVO());
	}
	
	/**
	 * Gets the list of bookmarks.  This method is separated out as it joins to 
	 * the document to get the article name and description
	 * @param userId
	 * @return
	 */
	public List<MTSDocumentVO> getBookmarks(String userId) {
		List<Object> vals = new ArrayList<>();
		vals.add(userId);
		
		StringBuilder sql = new StringBuilder(320);
		sql.append("select * from ").append(getCustomSchema()).append("mts_user_info a ");
		sql.append("inner join ").append(getCustomSchema()).append("mts_document d ");
		sql.append("on a.value_txt = d.unique_cd ");
		sql.append("inner join sb_action b ");
		sql.append("on d.document_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("inner join document doc on b.action_id = doc.action_id ");
		sql.append("inner join ").append(getCustomSchema()).append("mts_issue i ");
		sql.append("on d.issue_id = i.issue_id ");
		sql.append("inner join ").append(getCustomSchema()).append("mts_user u ");
		sql.append("on d.author_id = u.user_id ");
		sql.append("where a.user_id = ? ");
		sql.append("order by b.action_nm ");
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new MTSDocumentVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if ("0".equals(role.getRoleId())) {
			setModuleData(null, 0, "UNAUTHORIZED");
			return;
		}
		
		UserExtendedVO vo = new UserExtendedVO(req);
		try {
			if (req.hasParameter("delete")) {
				deleteUserInfo(vo);
			} else {
				UserDataVO user = this.getAdminUser(req);
				vo.setUserId(((MTSUserVO) user.getUserExtendedInfo()).getUserId());
				this.saveUserInfo(vo);
			}
		} catch (Exception e) {
			log.error("Unable to build extebded data: " + vo, e);
			setModuleData(vo, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Inserts or updates extended data element
	 * @param vo
	 * @throws DatabaseException 
	 */
	public void saveUserInfo(UserExtendedVO vo) throws DatabaseException {
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(vo);
		} catch (Exception e) {
			throw new DatabaseException("Unable to delete extended data for: " + vo, e);
		}
	}
	
	/**
	 * Deletes a user extended data item
	 * @param vo
	 * @throws DatabaseException 
	 */
	public void deleteUserInfo(UserExtendedVO vo) throws DatabaseException {
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.delete(vo);
		} catch (Exception e) {
			throw new DatabaseException("Unable to delete extended data for: " + vo, e);
		}
	}
}

