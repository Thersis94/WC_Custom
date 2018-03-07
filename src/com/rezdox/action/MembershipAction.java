package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.MembershipVO.Group;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MembershipAction.java<p/>
 * <b>Description: Handles RezDox memberships.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 30, 2018
 ****************************************************************************/
public class MembershipAction extends SBActionAdapter {
	
	public static final String MEMBERSHIP_ID = "membershipId";
	public static final String REQ_EXC_GROUP_CD = "excGroupCd";

	public MembershipAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MembershipAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public MembershipAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		List<MembershipVO> memberships = retrieveMemberships(req);
		putModuleData(memberships, memberships.size(), true);
	}
	
	/**
	 * Retrieves list of memberships
	 * 
	 * @param req
	 * @return
	 */
	public List<MembershipVO> retrieveMemberships(ActionRequest req) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();

		sql.append("select * from ").append(schema).append("rezdox_membership ");
		
		String[] membershipIds = req.getParameterValues(MEMBERSHIP_ID);
		if (membershipIds != null && membershipIds.length > 0) {
			sql.append("where membership_id in (").append(DBUtil.preparedStatmentQuestion(membershipIds.length)).append(") ");
			params.addAll(Arrays.asList(membershipIds));
		}
		
		if (req.hasParameter("getNewMemberDefault")) {
			sql.append("where new_mbr_dflt_flg = 1 and group_cd = ? ");
			params.add(req.getParameter("groupCode"));
		}
		
		String[] groupCodes = req.getParameterValues(REQ_EXC_GROUP_CD);
		if (groupCodes != null && groupCodes.length > 0) {
			sql.append("where group_cd not in (").append(DBUtil.preparedStatmentQuestion(groupCodes.length)).append(") ");
			params.addAll(Arrays.asList(groupCodes));
		}
		
		sql.append("order by group_cd, qty_no ");
		
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new MembershipVO());
	}
	
	/**
	 * Gets the default membership for a given membership group.
	 * Given free when signing up.
	 * 
	 * @param membershipGroup
	 * @return
	 */
	public MembershipVO retrieveDefaultMembership(Group membershipGroup) {
		ActionRequest membershipReq = new ActionRequest();
		membershipReq.setParameter("getNewMemberDefault", "true");
		membershipReq.setParameter("groupCode", membershipGroup.name());

		List<MembershipVO> membership = retrieveMemberships(membershipReq);
		return membership.get(0);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = "";
		
		// Change the status or a full insert/update
		if (req.hasParameter("toggleStatus")) {
			setMembershipStatus(req.getParameter(MEMBERSHIP_ID), req.getIntegerParameter("toggleStatus"));
			msg = "The membership status has been updated.";
		} else {
			updateMembership(req);
			msg = "The membership has been updated.";
		}
		
		sbUtil.adminRedirect(req, msg, (String) getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		
		StringBuilder url = new StringBuilder(200);
		url.append(req.getAttribute(Constants.REDIRECT_URL));
		sbUtil.manualRedirect(req, url.toString());
	}
	
	/**
	 * Adds or updates a membership
	 * 
	 * @param req
	 */
	protected void updateMembership(ActionRequest req) {
		DBProcessor dbp = new DBProcessor(dbConn);
		MembershipVO membership = new MembershipVO(req);
		
		try {
			dbp.save(membership);
		} catch (Exception e) {
			log.error("Unable to save RezDox membership.", e);
		}
	}
	
	/**
	 * Updates the status (active/inactive) of a membership
	 * 
	 * @param membershipId
	 * @param statusFlag
	 */
	protected void setMembershipStatus(String membershipId, int statusFlag) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		List<String> fields = new ArrayList<>();
		
		// Update the status for a specific membership
		sql.append("update ").append(schema).append("rezdox_membership set status_flg = ? where membership_id = ? ");
		fields.add("status_flg");
		fields.add("membership_id");
		
		MembershipVO membership = new MembershipVO();
		membership.setStatusFlag(statusFlag);
		membership.setMembershipId(membershipId);
		
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), membership, fields);
		} catch (Exception e) {
			log.error("Unable to change status of RezDox membership. ", e);
		}
	}
}