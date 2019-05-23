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
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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

	private static final String RD_STORE = "RD_STORE"; //the session attribute, used in view too

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
	 * retrieve the user's available store options, and cache them in session
	 * @param req
	 */
	protected void loadStoreOptions(ActionRequest req) {
		SMTSession ses = req.getSession();
		if (ses.getAttribute(RD_STORE) != null) return; //list is already cached

		ses.setAttribute(RD_STORE, retrieveMemberships(req));
	}

	/**
	 * Retrieves list of memberships
	 * @param req
	 * @return
	 */
	public List<MembershipVO> retrieveMemberships(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("rezdox_membership ");

		String[] membershipIds = req.getParameterValues(MEMBERSHIP_ID);
		String[] groupCodes = getMembershipExclusions(req);

		if (membershipIds != null && membershipIds.length > 0) {
			sql.append("where membership_id in (");
			DBUtil.preparedStatmentQuestion(membershipIds.length, sql);
			sql.append(") ");
			params.addAll(Arrays.asList(membershipIds));

		} else if (req.hasParameter("getNewMemberDefault")) {
			sql.append("where new_mbr_dflt_flg=1 and group_cd=? ");
			params.add(req.getParameter("groupCode"));

		} else if (groupCodes != null && groupCodes.length > 0) {
			sql.append("where group_cd not in (");
			DBUtil.preparedStatmentQuestion(groupCodes.length, sql);
			sql.append(") ");
			params.addAll(Arrays.asList(groupCodes));
		}
		sql.append("order by order_no");

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		return dbp.executeSelect(sql.toString(), params, new MembershipVO());
	}

	/**
	 * Checks the member's role to determine which subscriptions they don't have access to
	 * 
	 * @param req
	 * @return
	 */
	private String[] getMembershipExclusions(ActionRequest req) {
		List<String> exclusions = new ArrayList<>();
		SBUserRole role = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA));

		if (RezDoxUtils.REZDOX_BUSINESS_ROLE.equals(role.getRoleId())) {
			// Exclude residence memberships if this is a business-only role
			exclusions.add(Group.HO.name());
		} else if (RezDoxUtils.REZDOX_RESIDENCE_ROLE.equals(role.getRoleId())) {
			// Exclude business memberships if this is a residence-only role
			exclusions.add(Group.BU.name());
		}

		return exclusions.toArray(new String[exclusions.size()]);
	}


	/**
	 * Gets the default membership for a given membership group.
	 * Given free when signing up.
	 * @param membershipGroup
	 * @return
	 */
	public MembershipVO retrieveDefaultMembership(Group membershipGroup) {
		ActionRequest membershipReq = new ActionRequest();
		membershipReq.setParameter("getNewMemberDefault", "true");
		membershipReq.setParameter("groupCode", membershipGroup.name());

		List<MembershipVO> membership = retrieveMemberships(membershipReq);
		return !membership.isEmpty() ? membership.get(0) : new MembershipVO();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = null;

		// Change the status or a full insert/update
		if (req.hasParameter("toggleStatus")) {
			setMembershipStatus(req.getParameter(MEMBERSHIP_ID), req.getIntegerParameter("toggleStatus"));
			msg = "The membership status has been updated.";
		} else {
			updateMembership(req);
			msg = "The membership has been updated.";
		}

		sbUtil.adminRedirect(req, msg, (String) getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	/**
	 * Adds or updates a membership
	 * @param req
	 */
	protected void updateMembership(ActionRequest req) {
		try {
			DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
			dbp.save(new MembershipVO(req));
		} catch (Exception e) {
			log.error("Unable to save RezDox membership.", e);
		}
	}


	/**
	 * Updates the status (active/inactive) of a membership
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

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		try {
			dbp.executeSqlUpdate(sql.toString(), membership, fields);
		} catch (Exception e) {
			log.error("Unable to change status of RezDox membership. ", e);
		}
	}
}