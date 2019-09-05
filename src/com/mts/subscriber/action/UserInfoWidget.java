package com.mts.subscriber.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mts.publication.data.MTSDocumentVO;
import com.mts.subscriber.data.UserExtendedVO;
import com.mts.subscriber.data.UserExtendedVO.TypeCode;
import com.mts.util.AppUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.EnumUtil;
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


	public UserInfoWidget() {
		super();
	}

	public UserInfoWidget(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("json")) return;

		String userId = AppUtil.getMTSUserId(req);
		if (StringUtil.isEmpty(userId)) return;

		TypeCode typeCode = EnumUtil.safeValueOf(TypeCode.class, req.getParameter("typeCode"));
		if (TypeCode.BOOKMARK == typeCode) {
			setModuleData(getBookmarks(userId));
		} else {
			setModuleData(getData(userId, typeCode));
		}
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

		StringBuilder sql = new StringBuilder(500);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("mts_user_info ");
		sql.append("where user_id=? ");
		if (typeCode != null) {
			sql.append("and user_info_type_cd=? ");
			vals.add(typeCode.name());
		}
		sql.append("order by user_info_type_cd");
		log.debug(sql.length() + "|" + sql + "|" + vals);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), vals, new UserExtendedVO());
	}


	/**
	 * Gets the list of bookmarks.  This method is separated out as it joins to 
	 * the document to get the article name and description
	 * @param userId
	 * @return
	 */
	public List<MTSDocumentVO> getBookmarks(String userId) {
		List<Object> vals = Arrays.asList(userId);
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(500);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("mts_user_info a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_document d ");
		sql.append("on a.value_txt = d.unique_cd ");
		sql.append("inner join sb_action b ");
		sql.append("on d.action_group_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("inner join document doc on b.action_id = doc.action_id ");
		sql.append("inner join ").append(schema).append("mts_issue i ");
		sql.append("on d.issue_id = i.issue_id ");
		sql.append("inner join ").append(schema).append("mts_user u ");
		sql.append("on d.author_id = u.user_id ");
		sql.append("where a.user_id = ? ");
		sql.append("order by b.action_nm ");
		log.debug(sql.length() + "|" + sql + "|" + vals);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), vals, new MTSDocumentVO());
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (role == null || "0".equals(role.getRoleId())) {
			setModuleData(null, 0, "UNAUTHORIZED");
			return;
		}

		UserExtendedVO vo = new UserExtendedVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("delete")) {
				db.delete(vo);
			} else {
				vo.setUserId(AppUtil.getMTSUserId(req));
				db.save(vo);
				setModuleData(getAdminUser(req));
			}
		} catch (Exception e) {
			log.error("Unable to save extended data: " + vo, e);
			setModuleData(vo, 0, e.getLocalizedMessage());
		}
	}
}
