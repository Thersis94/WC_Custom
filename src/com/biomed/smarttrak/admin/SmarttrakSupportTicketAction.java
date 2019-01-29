/**
 *
 */
package com.biomed.smarttrak.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedSupportEmailUtil;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.AssigneeSection;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.DirectoryParser;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.support.SupportTicketAction;
import com.smt.sitebuilder.action.support.TicketVO;
import com.smt.sitebuilder.admin.action.OrganizationAction;
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

	@Override
	public void delete(ActionRequest req) throws ActionException {
		if(req.hasParameter("pkId")) {
			TicketVO t = new TicketVO(req);
			t.setTicketId(req.getParameter("pkId"));
			commitData(t, true);
		}
	}

	/**
	 * @param ticketId
	 * @param schema
	 * @return
	 */
	@Override
	public String formatRetrieveQuery(Map<String, Object> params) {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		
		
		sql.append("select acc.account_nm as organization_id, a.ticket_id, a.reporter_id, b.first_nm as reporter_first_nm, ");
		sql.append("a.assigned_id, a.ticket_no, a.status_cd, a.notify_flg, a.desc_txt, a.referrer_url, a.create_dt, a.update_dt, ");
		sql.append("b.last_nm as reporter_last_nm, c.first_nm as assigned_first_nm, ");
		sql.append("c.last_nm as assigned_last_nm ");
		sql.append("from support_ticket a ");
		sql.append("left outer join profile b on a.reporter_id = b.profile_id ");
		sql.append("left outer join profile c on a.assigned_id = c.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_user u on u.profile_id = a.reporter_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_account acc on acc.account_id = u.account_id ");
		sql.append("where a.organization_id = ? ");

		if(params.containsKey(TICKET_ID)) {
			sql.append("and a.ticket_id = ? ");
		}
	
		if(params.containsKey("profileId")) {
			sql.append("and b.profile_id = ? ");
		}

		sql.append("order by a.create_dt desc ");
		log.debug(sql);
		return sql.toString();
	}

	@Override
	protected LinkedHashMap<String, Object> getParams(ActionRequest req) throws ActionException {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		if(StringUtil.isEmpty(orgId)) {
			throw new ActionException("Missing OrgId on Request.");
		}
		params.put(OrganizationAction.ORGANIZATION_ID, orgId);

		if(!StringUtil.isEmpty(req.getParameter(TICKET_ID))) {
			params.put(TICKET_ID, req.getParameter(TICKET_ID));
		} else if(req.hasParameter(DirectoryParser.PARAMETER_PREFIX + "1")) {
			params.put(TICKET_ID, req.getParameter(DirectoryParser.PARAMETER_PREFIX + "1"));
			req.setParameter(TICKET_ID, req.getParameter(DirectoryParser.PARAMETER_PREFIX + "1"));
		}

		/*
		 * Check user Role.  If they are only a registered user, restrict tickets
		 * they see to their own only.
		 */
		SmarttrakRoleVO r = (SmarttrakRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(AdminControllerAction.DEFAULT_ROLE_LEVEL >= r.getRoleLevel()) {
			params.put("profileId", r.getProfileId());
		}

		return params;

	}

	@Override
	public void buildCallback(ActionRequest req, TicketVO item) throws ActionException {
		if (req.hasParameter("effortNo") || req.hasParameter("costNo")) {
			req.setParameter("descText", "Ticket Status Updated");
			ActionInterface a = new SmarttrakSupportTicketActivityAction(this.actionInit);
			a.setAttributes(getAttributes());
			a.setDBConnection(getDBConnection());
			a.build(req);
			
			// Set the cost and effort to 0 to prevent 
			// double billing in the future activities
			req.setParameter("effortNo", "");
			req.setParameter("costNo", "");
		}
		super.buildCallback(req, item);
	}

	/*
	 * Override basic retrieval callback and additionally retrieve managers for assignment.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.support.SupportTicketAction#retrieveCallback(com.siliconmtn.action.ActionRequest, java.util.List)
	 */
	@Override
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
	protected void loadManagers(ActionRequest req) {
		AccountAction aa = new AccountAction(this.actionInit);
		aa.setAttributes(getAttributes());
		aa.setDBConnection(getDBConnection());
		aa.loadManagerList(req, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA), AssigneeSection.DIRECT_ACCESS);
	}

	/*
	 * Helper method that loads Profile Data for a Smarttrak User.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.support.SupportTicketAction#getProfileData(java.lang.String, java.lang.String, com.siliconmtn.action.ActionRequest)
	 */
	@Override
	protected UserDataVO getProfileData(String profileId, String orgId, ActionRequest req) throws ActionException {
		//Get Profile Data for Ticket Reporter
		AccountUserAction aua = new AccountUserAction(this.actionInit);
		aua.setDBConnection(getDBConnection());
		aua.setAttributes(getAttributes());
		List<UserVO> users = aua.loadAccountUsers(req, profileId);

		if (!users.isEmpty())
			return users.get(0);

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.support.SupportTicketAction#sendEmail(com.smt.sitebuilder.action.support.TicketVO, com.smt.sitebuilder.action.support.SupportTicketAction.ChangeType, java.lang.String)
	 */
	@Override
	protected void sendEmail(TicketVO t, ChangeType type, String orgId) {
		try {
			new BiomedSupportEmailUtil(getDBConnection(), getAttributes()).sendEmail(t.getTicketId(), type);
		} catch (Exception e) {
			log.error("Problem Sending Email.", e);
		}
	}
}