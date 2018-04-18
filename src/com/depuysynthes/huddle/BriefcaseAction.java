package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: HuddleBriefcaseAction.java<p/>
 * <b>Description: API designed to be called by the Huddle App in order to get the
 * items that have been marked as part of the user's briefcase. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Oct 23, 2015
 ****************************************************************************/
public class BriefcaseAction extends MyFavoritesAction {
	public static final String API_KEY_NAME = "key";
	public static final String GROUP_CD = "BRIEFCASE";
	private static final String API_KEY = "dsHuddl3K3y|SMT";

	public BriefcaseAction() {
		super();
	}

	public BriefcaseAction(ActionInitVO arg0) {
		super(arg0);
	}
	

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			executeRetrieve(req);
		} catch (Exception e) {
			ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			// set cause (also sets error condition to true and sets message.)
			mod.setError(e.getCause() != null ? e.getCause() : e);
			if (req.hasParameter("amid")) mod.setDisplayPage(null); //circumvents going to view.
		}
	}
	
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (req.hasParameter("amid")) mod.setDisplayPage(null); //circumvents going to view.
		try {
			executeBuild(req);
		} catch (Exception e) {
			// set cause (also sets error condition to true and sets message.)
			mod.setError(e.getCause() != null ? e.getCause() : e);
		}
	}
	
	
	/**
	 * an encapsulated retrieve method, so we can catch any thrown exceptions and 
	 * upload the API's contract for error messages. (documented in TDS) 
	 * @param req
	 * @throws ActionException
	 */
	private void executeRetrieve(ActionRequest req) throws ActionException {
		//perform token validation to deter deviants
		validateApiKey(req);
		setSessionArgs(req);

		SMTCookie sort = req.getCookie(HuddleUtils.SORT_COOKIE);
		if (sort != null) req.setParameter("sort", sort.getValue());
		
		req.setParameter("formatJson", "true");
		req.setParameter("groupingCd", GROUP_CD);
		super.retrieve(req);
	}
	
	
	/**
	 * an encapsulated build method, so we can catch any thrown exceptions and 
	 * upload the API's contract for error messages. (documented in TDS) 
	 * @param req
	 * @throws ActionException
	 */
	private void executeBuild(ActionRequest req) throws ActionException {
		//perform token validation to deter deviants
		validateApiKey(req);
		setSessionArgs(req);
		
		if (req.hasParameter("insert")) {
			if (!req.hasParameter("relId")) req.setParameter("relId",req.getParameter("insert"));
			if (!req.hasParameter("uriTxt")) req.setParameter("uriTxt","");
			if (!req.hasParameter("typeCd")) req.setParameter("typeCd", "MEDIABIN");
			req.setParameter("groupingCd", GROUP_CD);
			super.build(req);
		} else if (req.hasParameter("isDelete")) { //deletes from the website use URI
			req.setParameter("groupingCd", GROUP_CD);
			super.build(req);
		} else if (req.hasParameter("briefcaseAssetId")) { //deletes from the App use profileFavoriteId
			deleteItem(req);
		}
	}
	
	
	/**
	 * Delete all briefcase items that were passed via the request
	 * @param req
	 * @throws ActionException 
	 */
	private void deleteItem(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		String[] assets = req.getParameterValues("briefcaseAssetId");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from PROFILE_FAVORITE where PROFILE_ID=? ");
		sql.append("and SITE_ID=? and GROUPING_CD=? and PROFILE_FAVORITE_ID in (");
		DBUtil.preparedStatmentQuestion(assets.length, sql);
		sql.append(") ");
		log.debug(sql+"|"+user+"|"+site.getSiteId()+"|"+GROUP_CD);

		int i = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(i++, user.getProfileId());
			ps.setString(i++, site.getSiteId());
			ps.setString(i++, GROUP_CD);
			for (String asset : assets)
				ps.setInt(i++, Convert.formatInteger(asset));
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		}
	}
	
	
	/**
	 * Ensure that the call has the required passcode to access the app,
	 * or the call came from a SitePage
	 * @param key
	 * @return
	 */
	private void validateApiKey(ActionRequest req) throws ActionException {
		if (!API_KEY.equals(req.getParameter(API_KEY_NAME)) && req.getSession().getAttribute(Constants.USER_DATA) == null) 
			throw new ActionException("Invalid or missing security key");
	}
	
	
	/**
	 * ensures a valid UserDataVO and SBRoleVO are put on the session object,
	 * which are required by our two superclasses.
	 * @param req
	 */
	private void setSessionArgs(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		
		if (user == null && !req.hasParameter("wwid")) {
			throw new ActionException("unknown user");
		} else if (user != null && req.hasParameter("wwid")) {
			//verify WWID did not change from what's on session. If so replace the logged-in user
			String myWWID = StringUtil.checkVal(user.getAttribute(HuddleUtils.WWID));
			if (!req.getParameter("wwid").equals(myWWID))
				user = null; //flush, so we can rebuild a new one below.
		}
		
		//build a new UserDataVO using wwid
		if (user == null) {
			user = new UserDataVO();
			user.setProfileId(getProfileIdFromWWID(req.getParameter("wwid"), site.getSiteId()));
			req.getSession().setAttribute(Constants.USER_DATA, user);
		}
		
		if (role == null) {
			role = new SBUserRole();
			role.setRoleLevel(SecurityController.PUBLIC_ROLE_LEVEL);
			role.setOrganizationId(site.getOrganizationId());
			role.setSiteId(site.getSiteId());
			role.setIpAddress(req.getRemoteAddr());
			req.getSession().setAttribute(Constants.ROLE_DATA, role);
		}
	}
	
	
	/**
	 * Get the profile id from the request object.  If the user is logged in we get the profile id from the
	 * session.  Otherwise we get the id from the request parameters
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private String getProfileIdFromWWID(String wwid, String siteId) throws ActionException {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select rs.profile_id from register_submittal rs ");
		sql.append("inner join register_data rd on rs.register_submittal_id=rd.register_submittal_id and rd.register_field_id=? ");
		sql.append("inner join profile_role pr on rs.profile_id=pr.profile_id and rs.site_id=pr.site_id and pr.status_id=? ");
		sql.append("where cast(rd.value_txt as varchar)=? and rs.site_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, HuddleUtils.WWID_REGISTER_FIELD_ID);
			ps.setInt(2, SecurityController.STATUS_ACTIVE);
			ps.setString(3, wwid);
			ps.setString(4, siteId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getString(1);
			
		} catch (SQLException sqle) {
			log.error("could not load profileIds from WWIDs", sqle);
		}
		
		throw new ActionException("unknown user");
	}
}