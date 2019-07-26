package com.wsla.action.admin;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.BasePortalAction;
import com.wsla.data.provider.ProviderUserVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: ProviderLocationUserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the user accounts for the providers
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 25, 2018
 * @updates:
 ****************************************************************************/

public class ProviderLocationUserAction extends BasePortalAction {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "providerLocationUser";

	/**
	 * 
	 */
	public ProviderLocationUserAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderLocationUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		boolean isError = false;
		try {
			String locId = req.getParameter("locationId");
			setModuleData(getUsers(locId, new BSTableControlVO(req, ProviderUserVO.class)));
		} catch(Exception e) {
			isError = true;
			Object msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			putModuleData("", 0, false, (String)msg, isError);
		}
	}

	/**
	 * Manages the user data for the grid
	 * @param locationId
	 * @param providerUserId
	 * @param bst
	 * @return
	 * @throws DatabaseException 
	 */
	public List<ProviderUserVO> getUsers(String locationId, BSTableControlVO bst) 
			throws DatabaseException {
		List<Object> params = new ArrayList<>();
		params.add(locationId);

		StringBuilder sql = new StringBuilder(320);
		sql.append("select * from custom.wsla_provider_user_xr a ");
		sql.append("inner join custom.wsla_user b on a.user_id = b.user_id ");
		sql.append("inner join profile_role d on b.profile_id = d.profile_id ");
		sql.append("and site_id = 'WSLA_1' ");
		sql.append("inner join role e on d.role_id = e.role_id ");
		sql.append("where location_id = ? ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(last_nm) like ? or lower(first_nm) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}

		sql.append(bst.getSQLOrderBy("last_nm",  "asc"));

		// Get the Provider Location users
		DBProcessor db = new DBProcessor(getDBConnection());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<ProviderUserVO> users = db.executeSelect(sql.toString(), params, new ProviderUserVO());
		assignProfileData(users);

		return users;
	}

	/**
	 * Gets the profiles for each user and add the data to the user vo
	 * @param users
	 * @throws DatabaseException
	 */
	protected void assignProfileData(List<ProviderUserVO> users) throws DatabaseException {
		// Get the list of Profiles
		List<String> profileIds = new ArrayList<>();
		for (ProviderUserVO user : users) profileIds.add(user.getProfileId());

		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		List<UserDataVO> profiles = pm.searchProfile(getDBConnection(), profileIds);

		// Loop the profiles and assign to the appropriate user vo 
		for (UserDataVO profile: profiles) {
			for (ProviderUserVO user : users) {
				if (profile.getProfileId().equals(user.getProfileId())) {
					user.setProfile(profile);
					user.setFormattedPhoneNumbers(profile.getPhoneNumbers(), "<br/>");
					user.setWorkPhoneNumber(profile.getWorkPhone());
					user.setMobilePhoneNumber(profile.getMobilePhone());
					break;
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ProviderUserVO providerUser = new ProviderUserVO(req);
		UserVO user = new UserVO(req);

		try {
			//call superclass, save the users profile data
			saveUser(site, user, true, true);
			
			if(req.getBooleanParameter("providerLocationBypass")) {
				log.debug("@@@@@editing user bypassing provider");
				return;
			}
			
			if (StringUtil.isEmpty(providerUser.getUserId()))
				providerUser.setUserId(user.getUserId()); //user would be populated at this point, by saveUser

			// Update / add the wsla provider user
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(providerUser);

			// if the user is the default (and saved successfully), reset the others to not be default
			if (providerUser.getPrimaryContactFlag() == 1) 
				resetDefaultContact(providerUser);

		} catch(Exception e) {
			log.error("Unable to save provider user", e);
			putModuleData("", 0, false, AdminConstants.KEY_ERROR_MESSAGE, true);
		}
	}

	/**
	 * Reset all location users to not be the primary contact, except 'this guy'
	 * @param user
	 * @throws SQLException
	 */
	public void resetDefaultContact(ProviderUserVO user) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("wsla_provider_user_xr ");
		sql.append("set primary_contact_flg=0 where location_id=? and user_id !=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getLocationId());
			ps.setString(2, user.getUserId());
			ps.executeUpdate();
		}
	}
}