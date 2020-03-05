package com.restpeer.action.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.restpeer.action.account.MemberLocationUserAction;
// RP Libs
import com.restpeer.common.RPConstants;
import com.restpeer.data.RPUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.LocationProfileVO;
// WC Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.UserBaseWidget;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UserAction.java
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

public class UserAction extends UserBaseWidget {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "user";
	
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
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String siteId = StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId());

		try {
			if (req.hasParameter("profileId")) {
				setModuleData(getUserProfile(req.getParameter("profileId")));
			} else {
				BSTableControlVO bst = new BSTableControlVO(req, RPUserVO.class);
				setModuleData(getUsers(req.getParameter("dealerLocationId"), siteId, bst));
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
	 * Returns the user record for the given profileId.
	 * 
	 * @param profileId
	 * @return
	 */
	public RPUserVO getUserByProfileId(String profileId) {
		StringBuilder sql = new StringBuilder(256);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("rp_user ");
		sql.append(DBUtil.WHERE_CLAUSE).append("profile_id = ? ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<RPUserVO> data = db.executeSelect(sql.toString(), Arrays.asList(profileId), new RPUserVO());
		
		return data != null && !data.isEmpty() ? data.get(0) : new RPUserVO();
	}
	
	/**
	 * Gets the User Id from the profile id
	 * @param profileId
	 * @return
	 * @throws SQLException
	 */
	public String getUserIdFromProfileId(String profileId) throws SQLException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("select user_id from ").append(getCustomSchema()).append("rp_user ");
		sql.append("where profile_id = ?");
		log.debug(sql + "|" + profileId);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return rs.getString(1);
				else throw new SQLException("No user found");
			}
		}
	}

	/**
	 * Loads the list of users.  if a user is already associated to the location
	 * then do not return them in the list
	 * @param dlid
	 * @param siteId
	 * @param bst
	 * @return
	 */
	public GridDataVO<RPUserVO> getUsers(String dlid, String siteId, BSTableControlVO bst) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(344);
		sql.append("select a.*, coalesce(user_total, 0) as member_assoc_no, pr.role_id from ");
		sql.append(getCustomSchema()).append("rp_user a "); 
		sql.append("left outer join ( ");
		sql.append("select profile_id, count(*) as user_total from ");
		sql.append("dealer_location_profile_xr b ");
		sql.append("group by profile_id ");
		sql.append(") as b on a.profile_id = b.profile_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role pr on a.profile_id = pr.profile_id and pr.site_id = ? ");
		sql.append("where 1=1 ");
		vals.add(siteId);
		
		if (! StringUtil.isEmpty(dlid)) {
			sql.append("and a.profile_id not in ( "); 
			sql.append("select profile_id from ");
			sql.append("dealer_location_profile_xr where dealer_location_id = ? "); 
			sql.append(") "); 
			
			vals.add(dlid);
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
		req.setParameter("workPhone", req.getParameter("phoneNumber"));
		super.build(req, "rp_user");
		
		RPUserVO user = new RPUserVO(this.extUser);
		user.setDriverLicense(req.getParameter("driverLicense"));
		user.setDriverLicensePath(req.getParameter("driverLicensePath"));
		
		try {
			saveCustomUser(user, req);
			setModuleData(user);
		} catch (Exception e) {
			setModuleData(user, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Saves a custom user record.
	 * 
	 * @param user
	 * @param req
	 * @throws ActionException
	 */
	public void saveCustomUser(RPUserVO user, ActionRequest req) throws ActionException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		try {
			if (req.getBooleanParameter("isInsert")) {
				db.insert(user);
				if (!StringUtil.isEmpty(req.getParameter("dealerLocationId"))) {
					saveRoleInfo(user, req);
				}
			} else {
				db.update(user);
			}
		} catch (Exception e) {
			log.error("Unable to add user: " + user, e);
			throw new ActionException(e);
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
			LocationProfileVO loc = new LocationProfileVO(req);
			loc.setProfileId(user.getProfileId());
			if (StringUtil.isEmpty(loc.getRoleId())) loc.setRoleId(ua.getRoleId(req));
			
			// Assign the user to the location
			ua.save(loc);
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException("Unable to assign user to member location", e);
		}
	}
}

