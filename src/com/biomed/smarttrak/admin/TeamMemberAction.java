package com.biomed.smarttrak.admin;

//Java 7
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

//WC_Custom
import com.biomed.smarttrak.vo.TeamMemberVO;

/*****************************************************************************
 <p><b>Title</b>: TeamMemberAction.java</p>
 <p><b>Description: Manages the _XR records (relationship) between Teams and Users (for an Account) for Smartrak.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 11, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class TeamMemberAction extends SBActionAdapter {

	protected static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param
	protected static final String TEAM_ID = TeamAction.TEAM_ID; //req param

	public TeamMemberAction() {
		super();
	}

	public TeamMemberAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//this action requires accountId & teamId.  If not present throw an exception
		String accountId = req.getParameter(ACCOUNT_ID);
		String teamId = req.hasParameter(TEAM_ID) ? req.getParameter(TEAM_ID) : null;
		if (teamId == null || accountId == null) throw new ActionException("missing teamId and/or accountId");

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(schema);

		List<Object> params = new ArrayList<>();
		params.add(teamId);
		params.add(accountId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  data = db.executeSelect(sql, params, new TeamMemberVO());
		log.debug("loaded " + data.size() + " records");

		//decrypt the owner profiles
		decryptNames(data);
		Collections.sort(data, new NameComparator());
		
		putModuleData(data);
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	protected String formatRetrieveQuery(String schema) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select p.first_nm, p.last_nm, u.user_id, x.team_id, newid() as user_team_xr_id, x.user_team_xr_id as pkid ");
		sql.append("from ").append(schema).append("biomedgps_user u ");
		sql.append("inner join profile p on p.profile_id=u.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_user_team_xr x on u.user_id=x.user_id and x.team_id=? ");
		sql.append("where u.account_id=? ");
		log.debug(sql);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		saveRecord(req, false);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		saveRecord(req, true);
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(ActionRequest req, boolean isDelete) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			if (isDelete) {
				db.delete(new TeamMemberVO(req));
				putModuleData("deleted"); //unused placeholder to prevent redirection
			} else {
				db.save(new TeamMemberVO(req));
				putModuleData(db.getGeneratedPKId());
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}