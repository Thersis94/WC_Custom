package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.tools.FavoriteVO;
import com.smt.sitebuilder.action.tools.MyFavoritesAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

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

public class HuddleBriefcaseAction extends MyFavoritesAction {
	public final String KEY_NAME = "briefcaseKey";
	public final String GROUP_CD = "BRIEFCASE";
	private final String API_KEY = "dsHuddl3K3y|SMT";

	public HuddleBriefcaseAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public HuddleBriefcaseAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		if (!validateKey(req.getParameter("key"))) throw new ActionException("Invalid APP key");
		
		req.setParameter("groupingCd", GROUP_CD);
		req.setParameter("profileId", getProfileId(req));
		super.retrieve(req);
		req.setParameter("formatJson", "true"); 
		
		@SuppressWarnings("unchecked")
		List<FavoriteVO> favs = (List<FavoriteVO>) req.getAttribute(MyFavoritesAction.MY_FAVORITES);
		if (favs.size() == 0) throw new ActionException("No Favorites Found for Current User");
		
		// Create a map of bookmark create dates and mediabin ids 
		// so that we can keep the date and the document together
		Map<String, Date> created = new HashMap<>();
		for (FavoriteVO fav : favs) {
			created.put(fav.getRelId(), fav.getCreateDt());
		}
		
		StringBuilder sql = new StringBuilder(275);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_MEDIABIN ");
		sql.append("WHERE DPY_SYN_MEDIABIN_ID in ('skip' ");
		for (int i=0; i<favs.size(); i++) sql.append(",?");
		sql.append(")");
		
		Map<String, MediaBinDeltaVO> items = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int x=1;
			for (FavoriteVO fav : favs) ps.setString(x++, fav.getRelId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				MediaBinDeltaVO asset = new MediaBinDeltaVO(rs);
				asset.setCreateDate(created.get(asset.getDpySynMediaBinId()));
				items.put(rs.getString("DPY_SYN_MEDIABIN_ID"), asset);
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		req.setAttribute(GROUP_CD, items);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		if (!validateKey(req.getParameter("key"))) throw new ActionException("Invalid APP key");
		
		if(req.hasParameter("insert")) {
			deleteFavorite(req);
		} else {
			deleteItem(req);
		}
	}
	
	/**
	 * Ensure that the call has the required passcode to access the app.
	 * @param key
	 * @return
	 */
	private boolean validateKey(String key) {
		return API_KEY.equals(key);
	}
	
	/**
	 * Get the profile id from the request object.  If the user is logged in we get the profile id from the
	 * session.  Otherwise we get the id from the request parameters
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private String getProfileId(SMTServletRequest req) throws ActionException {
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		if (user != null && user.getProfileId() != null) return user.getProfileId();
		if (req.hasParameter("wwid")) {
			//TODO Implement global user checking
			return req.getParameter("wwid");
		}
		throw new ActionException("No/Invalid user id provided.");
	}
	
	
	/**
	 * Delete all briefcase items that were passed via the request
	 * @param req
	 * @throws ActionException 
	 */
	private void deleteItem(SMTServletRequest req) throws ActionException {
		String[] assets = req.getParameterValues("briefcaseAssetId");
		String user = getProfileId(req);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from PROFILE_FAVORITE where PROFILE_ID = ? and SITE_ID = ? and GROUPING_CD = ? and PROFILE_FAVORITE_ID in (-1");
		for (int i=0; i < assets.length; i++) sql.append(",?");
		sql.append(") ");
		log.debug(sql+"|"+user+"|"+site.getSiteId()+"|"+GROUP_CD);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, user);
			ps.setString(i++, site.getSiteId());
			ps.setString(i++, GROUP_CD);
			for (String asset : assets)
				ps.setInt(i++, Convert.formatInteger(asset));
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		}
	}

}
