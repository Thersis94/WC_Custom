package com.mts.admin.action;

// JDK 1.8.x
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import com.mts.common.MTSConstants;
// MTS Libs
import com.mts.subscriber.action.SubscriptionAction;
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
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
		boolean updateSubscription = false;
		try {
			if (req.getBooleanParameter("isAuthor")) {
				cols = getAuthorUserColumns();
			} else if (req.getBooleanParameter("isSubscriber")) {
				cols = getSubscriberUserColumns();
				updateSubscription = true;
			} else {
				cols = getCoreUserColumns();
				saveUser((SiteVO) req.getAttribute(Constants.SITE_DATA), user, true);
			}
			
			updateUser(user, cols);
			if (updateSubscription) updateSubscriptions(req, user);
			setModuleData(user);
		} catch(Exception e) {
			setModuleData(user, 1, e.getLocalizedMessage());
			log.error("unable to save author info", e);
		}
	}
	
	/**
	 * Updates the subscriptions for a given user
	 * @param req
	 * @param user
	 * @throws DatabaseException
	 */
	private void updateSubscriptions(ActionRequest req, MTSUserVO user) 
	throws DatabaseException {
		List<String> subs = new ArrayList<>();
		String[] subscriptions = req.getParameterValues("subscriptions");
		if (subscriptions != null && subscriptions.length > 0)
			subs = Arrays.asList(subscriptions);
		
		SubscriptionAction sa = new SubscriptionAction(getDBConnection(), getAttributes());
		sa.assignSubscriptions(user.getUserId(), subs);
	}
	
	/**
	 * Returns the list of columns to be updated 
	 * @return
	 */
	private String[] getAuthorUserColumns() {
		return new String[]{ 
			"user_id", "img_path", "twitter_txt", "update_dt",
			"linkedin_txt", "yrs_experience_no", "cv_desc"
		};
	}
	
	/**
	 * Returns the list of columns to be updated 
	 * @return
	 */
	private String[] getSubscriberUserColumns() {
		return new String[]{ 
			"user_id", "sec_user_id", "subscription_type_cd", "update_dt",
			"print_copy_flg", "expiration_dt", "note_txt"
		};
	}
	
	/**
	 * List of columns to be updated when saving the core user info
	 * @return
	 */
	private String[] getCoreUserColumns() {
		return new String[]{ 
			"user_id", "profile_id", "first_nm", "last_nm", 
			"email_address_txt", "locale_txt", "update_dt",
			"active_flg", "role_id", "pro_title_nm","company_nm", "address_txt",
			"address2_txt", "city_nm", "state_cd", "zip_cd"
		};
	}
	
	/**
	 * 
	 * @param userId
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public MTSUserVO getUserProfile(String userId) throws DatabaseException {
		try {
			MTSUserVO user = new MTSUserVO();
			user.setUserId(userId);
			
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.getByPrimaryKey(user);
			
			user.setProfile(this.getProfile(user.getProfileId(), MTSConstants.ORGANIZATON_ID));
			
			return user;
		} catch (Exception e) {
			throw new DatabaseException("Unable to retrieve user detail info", e);
		}
	}
	
	/**
	 * Updates the author portion of the user table
	 * @param user
	 * @throws DatabaseException
	 */
	public void updateUser(MTSUserVO user, String[] cols) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			if (StringUtil.isEmpty(user.getUserId())) {
				db.insert(user);
			} else {
				db.update(user, Arrays.asList(cols));
			}
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
		try {
			
			if (req.getBooleanParameter("isSubscription")) {
				setModuleData(getSubscriptions(req.getParameter("userId")));
			} else if (req.getBooleanParameter("isProfile")) {
				setModuleData(getUserProfile(req.getParameter("userId")));
			} else {
				BSTableControlVO bst = new BSTableControlVO(req, MTSUserVO.class);
				setModuleData(getAllUsers(bst, req.getParameter("roleId")));
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Gets the subscriptions for a given user
	 * @param userId
	 * @return
	 */
	public List<GenericVO> getSubscriptions(String userId) {
		StringBuilder sql = new StringBuilder(92);
		sql.append("select subscription_publication_id as key, publication_id as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("mts_subscription_publication_xr where user_id = ?");
		
		List<Object> vals = new ArrayList<>();
		vals.add(userId);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new GenericVO());
	}
	
 	/**
	 * 
	 * @return
	 */
	public GridDataVO<MTSUserVO> getAllUsers(BSTableControlVO bst, String roleId) {
		// Add the params
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(768);
		sql.append("select last_login_dt, a.*, c.role_nm, b.profile_role_id, d.authentication_id, a.create_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_user a ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role b ");
		sql.append("on a.profile_id = b.profile_id and site_id = 'MTS_2' ");
		sql.append(DBUtil.INNER_JOIN).append("role c ");
		sql.append("on b.role_id = c.role_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile d ");
		sql.append("on a.profile_id = d.profile_id ");
		sql.append("left outer join ( ");
		sql.append("select authentication_id, max(login_dt) as last_login_dt ");
		sql.append("from authentication_log ");
		sql.append("where site_id in ('MTS_1', 'MTS_2') ");
		sql.append("group by authentication_id ");
		sql.append(") g on d.authentication_id = g.authentication_id "); 
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

