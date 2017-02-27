package com.biomed.smarttrak.admin;

//Java 7
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// WC_Custom
import com.biomed.smarttrak.vo.TeamVO;
import com.biomed.smarttrak.action.AdminControllerAction;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: TeamAction.java</p>
 <p><b>Description: Manages the Account records for Smartrak.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class TeamAction extends SBActionAdapter {

	protected static final String ACCOUNT_ID = AccountAction.ACCOUNT_ID; //req param
	public static final String TEAM_ID = "teamId"; //req param

	public TeamAction() {
		super();
	}

	public TeamAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter(TEAM_ID)) return;

		String accountId = req.getParameter(ACCOUNT_ID);
		//accountId is required
		if (StringUtil.isEmpty(accountId)) throw new ActionException("No AccountId passed");

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String teamId = req.hasParameter(TEAM_ID) ? req.getParameter(TEAM_ID) : null;
		String sql = formatRetrieveQuery(teamId, schema);

		List<Object> params = new ArrayList<>();
		params.add(accountId);
		if (teamId != null) params.add(teamId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  data = db.executeSelect(sql, params, new TeamVO());
		log.debug("loaded " + data.size() + " teams");

		putModuleData(data);
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public String formatRetrieveQuery(String teamId, String schema) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.team_id, a.account_id, a.team_nm, a.default_flg, a.private_flg, cast(count(b.user_id) as integer) as members ");
		sql.append("from ").append(schema).append("biomedgps_team a ");
		sql.append("left outer join ").append(schema).append("biomedgps_user_team_xr b on a.team_id=b.team_id ");
		sql.append("where a.account_id=? ");
		if (teamId != null) sql.append("and a.team_id=? "); 
		sql.append("group by a.team_id, a.account_id, a.team_nm, a.default_flg, a.private_flg ");
		sql.append("order by a.team_nm");

		log.debug(sql);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		saveRecord(req, false);
		resetDefaultTeam(req);
		setupRedirect(req);
	}


	/**
	 * checks if we just saved a default team.  If so, run a query to ensure we didn't just create two defaults within this account.
	 * @param req
	 */
	protected void resetDefaultTeam(ActionRequest req) {
		//check if the incoming account was saved as the default.  If not we're done.
		if (!"1".equals(req.getParameter("defaultFlag"))) return;

		StringBuilder sql = new StringBuilder(200);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_TEAM set default_flg=0, update_dt=CURRENT_TIMESTAMP where ");
		sql.append("account_id=? and team_id != ? and default_flg=1");
		log.debug(sql);

		String accountId = req.getParameter(ACCOUNT_ID);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1,  accountId);
			ps.setString(2, req.getParameter(TEAM_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not reset default team for account=" + accountId, sqle);
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		saveRecord(req, true);
		setupRedirect(req);
	}


	/**
	 * builds the redirect URL that takes us back to the list of teams page.
	 * @param req
	 */
	protected void setupRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?actionType=").append(req.getParameter("actionType"));
		url.append("&accountId=").append(req.getParameter("accountId"));
		url.append("&accountName=").append(AdminControllerAction.urlEncode(req.getParameter("accountName")));
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
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
				//move the generic pkId in to the teamId field, so the VO picks it up
				if (req.hasParameter("pkId") && !req.hasParameter(TEAM_ID))
					req.setParameter(TEAM_ID, req.getParameter("pkId"));
				db.delete(new TeamVO(req));
			} else {
				db.save(new TeamVO(req));
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}