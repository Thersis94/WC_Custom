/**
 *
 */
package com.biomed.smarttrak.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.support.SupportTicketAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SupportTicketAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom Support Ticket Action that extends the Base
 * SupportTicketAction for Biomed Smarttrak.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 24, 2017
 ****************************************************************************/
public class SmarttrakSupportTicketAction extends SupportTicketAction {

	public SmarttrakSupportTicketAction() {
		super();
	}

	public SmarttrakSupportTicketAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param ticketId
	 * @param schema
	 * @return
	 */
	@Override
	public String formatRetrieveQuery(Map<String, Object> params) {
		StringBuilder sql = new StringBuilder(500);
		sql.append("select a.*, b.first_nm as reporter_first_nm, ");
		sql.append("b.last_nm as reporter_last_nm, c.first_nm as assigned_first_nm, ");
		sql.append("c.last_nm as assigned_last_nm ");
		sql.append("from support_ticket a ");
		sql.append("left outer join profile b on a.reporter_id = b.profile_id ");
		sql.append("left outer join profile c on a.assigned_id = c.profile_id ");
		sql.append("where a.organization_id = ? ");

		if(params.containsKey("ticketId")) {
			sql.append("and a.ticket_id = ? ");
		}
	
		if(params.containsKey("profileId")) {
			sql.append("and b.profile_id = ? ");
		}

		sql.append("order by a.create_dt desc ");
		return sql.toString();
	}

	@Override
	protected LinkedHashMap<String, Object> getParams(ActionRequest req) throws ActionException {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		if(StringUtil.isEmpty(orgId)) {
			throw new ActionException("Missing OrgId on Request.");
		}
		params.put("organizationId", orgId);

		if(!StringUtil.isEmpty(req.getParameter("ticketId"))) {
			params.put("ticketId", StringUtil.checkVal(req.getParameter("ticketId")));
		}

		/*
		 * Check user Role.  If they are only a registered user, restrict tickets
		 * they see to their own only.
		 */
		SmarttrakRoleVO r = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(r.getRoleLevel() == AdminControllerAction.DEFAULT_ROLE_LEVEL) {
			params.put("profileId", r.getProfileId());
		}

		return params;

	}

	/**
	 * Override basic retrieval callback and additionally retrieve managers for
	 * assignment.
	 */
	public void retrieveCallback(ActionRequest req, List<Object> items) throws ActionException {
		super.retrieveCallback(req, items);

		//Load Managers for assigning tickets.
		loadManagers(req);
	}

	/**
	 * Load smarttrak managers for use with assigning tickets.
	 * @param req
	 * @throws ActionException
	 */
	protected void loadManagers(ActionRequest req) throws ActionException {
		AccountAction aa = new AccountAction(this.actionInit);
		aa.setAttributes(getAttributes());
		aa.setDBConnection(getDBConnection());
		aa.loadManagerList(req, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
	}

	/**
	 * Helper method that loads Profile Data for a Smarttrak User.
	 * @param orgId 
	 * @param profileId 
	 * @param req
	 * @return
	 */
	protected UserDataVO getProfileData(String profileId, String orgId, ActionRequest req) throws ActionException {
		UserDataVO u = null;
	
		//Get Profile Data for Ticket Reporter
		AccountUserAction aua = new AccountUserAction(this.actionInit);
		aua.setDBConnection(getDBConnection());
		aua.setAttributes(getAttributes());
		List<Object> users = aua.loadAccountUsers(req, profileId);

		if(!users.isEmpty()) {
			u = (UserDataVO) users.get(0);
		}

		return u;
	}
}