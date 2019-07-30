package com.wsla.action.admin;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.wsla.action.BasePortalAction;
import com.wsla.data.provider.ProviderUserVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: UserAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Will control administration of users outside of the provider user relationship
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Jul 24, 2019
 * @updates:
 ****************************************************************************/
public class UserAction extends BasePortalAction {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "users";
	
	private static final String PUBLIC_SITE_ID = "WSLA_1";

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
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("user action retreve called");
		boolean hasActiveFlag = req.hasParameter("activeFlag");
		int activeFlag = Convert.formatInteger(req.getParameter("activeFlag"));
		String providerId = req.getParameter("providerId");
		setModuleData(getUsers( hasActiveFlag, activeFlag, providerId));
	}

	/**
	 * @param bsTableControlVO
	 * @param hasActiveFlag 
	 * @param activeFlagValue 
	 * @param hasActiveFlag 
	 * @param providerId 
	 * @return
	 */
	private List<ProviderUserVO> getUsers(boolean hasActiveFlag, Integer activeFlagValue, String providerId) {
		log.debug("Users action get users called");
		
		List<Object> vals = new ArrayList<>();
		vals.add(PUBLIC_SITE_ID);
		
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_CLAUSE).append("pro.provider_nm, pro.provider_id, pl.*, u.*, r.role_nm, r.role_id from ").append(getCustomSchema()).append("wsla_user u ");
		sql.append(DBUtil.INNER_JOIN).append("profile p on u.profile_id = p.profile_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role pr on u.profile_id = pr.profile_id ");
		sql.append(DBUtil.INNER_JOIN).append("role r on pr.role_id = r.role_id ");
		
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_user_xr uxr on u.user_id = uxr.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location pl on uxr.location_id = pl.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider pro on pl.provider_id = pro.provider_id ");
		
		sql.append(DBUtil.WHERE_CLAUSE).append("pr.site_id = ? ");
		
		if(hasActiveFlag) {
			sql.append("and u.active_flg = ? ");
			vals.add(activeFlagValue);
		}
		
		if (!StringUtil.isEmpty(providerId)) {
			sql.append("and pro.provider_id = ? ");
			vals.add(providerId);
		}

		DBProcessor db = new DBProcessor(getDBConnection());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<ProviderUserVO> users = db.executeSelect(sql.toString(), vals, new ProviderUserVO());
		
		ProviderLocationUserAction plua = new ProviderLocationUserAction();
		plua.setActionInit(actionInit);
		plua.setAttributes(getAttributes());
		plua.setDBConnection(getDBConnection());
		
		try {
			plua.assignProfileData(users);
		} catch (DatabaseException e) {
			log.error("Unable to assign user details", e);
			putModuleData("", 0, false, AdminConstants.KEY_ERROR_MESSAGE, true);
		}
		
		return users;
	}

}
