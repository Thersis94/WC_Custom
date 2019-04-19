package com.mts.admin.action;

// JDK 1.8.x
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

// MTS Libs
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.StringUtil;
//WC Libs
import com.smt.sitebuilder.action.user.UserBaseWidget;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action to manage mts users
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 8, 2019
 * @updates:
 ****************************************************************************/

public class UserAction extends UserBaseWidget {
	
	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "users";
	
	/**
	 * 
	 */
	public UserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public UserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public UserAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Assign the core user data
		MTSUserVO user = new MTSUserVO(req);
		if (! StringUtil.isEmpty(req.getParameter("value")))
			user.setImagePath(req.getParameter("value"));
		
		// Get the columns to be updated and store the user info
		String[] cols = null;
		try {
			if (req.getBooleanParameter("isAuthor")) {
				cols = getAuthorUserColumns();
			} else {
				cols = getCoreUserColumns();
				saveUser((SiteVO) req.getAttribute(Constants.SITE_DATA), user, true);
			}

			updateUser(user, cols);
			setModuleData(user);
		} catch(Exception e) {
			setModuleData(user, 1, e.getLocalizedMessage());
			log.error("unable to save author info", e);
		}
	}
	
	/**
	 * Returns the list of columns to be updated 
	 * @return
	 */
	private String[] getAuthorUserColumns() {
		return new String[]{ 
			"user_id", "img_path", "twitter_txt", "facebook_txt", "update_dt",
			"linkedin_txt", "yrs_experience_no", "cv_desc"
		};
	}
	
	/**
	 * List of columns to be updated when saving the core user info
	 * @return
	 */
	private String[] getCoreUserColumns() {
		return new String[]{ 
			"user_id", "profile_id", "first_nm", "last_nm", 
			"email_address_txt", "phone_number_txt", "locale_txt", "update_dt",
			"active_flg", "role_id", "pro_title_nm","company_nm", "sec_user_id"
		};
	}
	
	/**
	 * Updates the author portion of the user table
	 * @param user
	 * @throws DatabaseException
	 */
	public void updateUser(MTSUserVO user, String[] cols) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.update(user, Arrays.asList(cols));
		} catch (Exception e) {
			throw new DatabaseException("Unable to save author info", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		BSTableControlVO bst = new BSTableControlVO(req, MTSUserVO.class);
		setModuleData(getAllUsers(bst, req.getParameter("roleId")));
	}
	
	/**
	 * 
	 * @return
	 */
	public GridDataVO<MTSUserVO> getAllUsers(BSTableControlVO bst, String roleId) {
		// Add the params
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(448);
		sql.append("select a.*, c.role_nm, b.profile_role_id, d.authentication_id from ");
		sql.append(getCustomSchema()).append("mts_user a ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role b ");
		sql.append("on a.profile_id = b.profile_id and site_id = 'MTS_2' ");
		sql.append(DBUtil.INNER_JOIN).append("role c ");
		sql.append("on b.role_id = c.role_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile d ");
		sql.append("on a.profile_id = d.profile_id ");
		sql.append("where 1=1 ");
		
		// Filter by Roles
		if (! StringUtil.isEmpty(roleId)) {
			sql.append("and a.role_id = ? ");
			vals.add(roleId);
		}
		
		// Filter by the search box
		if (bst.hasSearch()) {
			sql.append("and (lower(a.last_nm) like ? or lower(a.first_nm) like ? ");
			sql.append("or lower(a.email_address_txt) like ?) ");
			vals.add(bst.getLikeSearch());
			vals.add(bst.getLikeSearch());
			vals.add(bst.getLikeSearch());
		}
		
		sql.append(bst.getSQLOrderBy("a.last_nm", "asc"));
		log.debug(sql.length() + "|" + sql + "|" + bst.getLikeSearch());
		
		// Query
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new MTSUserVO(), bst);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MTSUserVO> getEditors() {
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("mts_user ");
		sql.append("where role_id in ('100', 'AUTHOR') ");
		sql.append("order by last_nm, first_nm ");
		log.debug(sql.length() + "|" + sql);
		
		// Add the params
		List<Object> vals = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new MTSUserVO());
	}
}

