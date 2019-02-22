package com.restpeer.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.restpeer.action.account.MemberLocationUserAction;
// RP Libs
import com.restpeer.common.RPConstants;
import com.restpeer.data.LocationUserVO;
import com.restpeer.data.RPUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.UserBaseWidget;

/****************************************************************************
 * <b>Title</b>: UserWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the assignment of Users to a location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 19, 2019
 * @updates:
 ****************************************************************************/

public class UserWidget extends UserBaseWidget {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "user";
	
	/**
	 * 
	 */
	public UserWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public UserWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public UserWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("profileId")) {
				setModuleData(getUserProfile(req.getParameter("profileId")));
			} else {
				BSTableControlVO bst = new BSTableControlVO(req, RPUserVO.class);
				setModuleData(getUsers(req.getParameter("memberLocationId"), bst));
			}
		} catch (Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param profileId
	 * @return
	 * @throws DatabaseException
	 */
	public UserDataVO getUserProfile(String profileId) throws DatabaseException {
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		return pm.getProfile(profileId, getDBConnection(), "", RPConstants.ORGANIZATON_ID);
	}
	
	/**
	 * Loads the list of users.  if a user is already associated to the location
	 * then do not return them in the list
	 * @param mlid
	 * @param bst
	 * @return
	 */
	public GridDataVO<RPUserVO> getUsers(String mlid, BSTableControlVO bst) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(344);
		sql.append("select a.*, coalesce(user_total, 0) as member_assoc_no from ");
		sql.append(getCustomSchema()).append("rp_user a "); 
		sql.append("left outer join ( ");
		sql.append("select user_id, count(*) as user_total from ");
		sql.append(getCustomSchema()).append("rp_location_user_xr b ");
		sql.append("group by user_id ");
		sql.append(") as b on a.user_id = b.user_id ");
		sql.append("where 1=1 ");
		
		if (! StringUtil.isEmpty(mlid)) {
			sql.append("and a.user_id not in ( "); 
			sql.append("select user_id from ").append(getCustomSchema());
			sql.append("rp_location_user_xr where member_location_id = ? "); 
			sql.append(") "); 
			
			vals.add(mlid);
		}
		
		// Do a like search against first and last name, email and phone
		if (bst.hasSearch()) {
			sql.append("and (lower(first_nm) like ? or lower(last_nm) like ? ");
			sql.append("or lower(email_address_txt) like ? or phone_number_txt like ?) ");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append("order by ").append(bst.getDBSortColumnName("last_nm"));
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new RPUserVO(), bst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Call the base class and process the user. Assign to the RP User
		super.build(req);
		
		RPUserVO user = new RPUserVO(this.extUser);
		user.setDriverLicense(req.getParameter("driverLicense"));
		user.setDriverLicensePath(req.getParameter("driverLicensePath"));
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.getBooleanParameter("isInsert")) {
				db.insert(user);
				
				if (! StringUtil.isEmpty(req.getParameter("memberLocationId"))) {
					saveRoleInfo(user, req);
				}
			} else {
				db.update(user);
			}
			
			setModuleData(user);
		} catch (Exception e) {
			log.error("Unable to add user: " + user, e);
			setModuleData(user, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Assigns a user to the appropriate member location for login permissions
	 * @param user
	 * @param req
	 * @throws ActionException
	 */
	public void saveRoleInfo(RPUserVO user, ActionRequest req) throws ActionException {
		try {
			log.info("Assigning user to Location");
			// Assign the data to the location object.  Xref member type to the appropriate role
			MemberLocationUserAction ua = new MemberLocationUserAction(getDBConnection(), getAttributes());
			LocationUserVO loc = new LocationUserVO(req);
			loc.setUserId(user.getUserId());
			if (StringUtil.isEmpty(loc.getRoleId())) loc.setRoleId(ua.getRoleId(req));
			
			// Assign the user to the location
			ua.save(loc);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException("Unable to assign user to member location", e);
		}
	}
}

