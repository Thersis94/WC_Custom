package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.action.MediaBinAssetVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
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

public class HuddleBriefcaseAction extends SimpleActionAdapter {
	String KEY_NAME = "briefcaseKey";
	String GROUP_CD = "BRIEFCASE";

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
		if (validateKey(req.getParameter("key"))) {
			req.setParameter("formatJson", "true"); 
			
			StringBuilder sql = new StringBuilder(275);
			sql.append("SELECT * FROM PROFILE_FAVORITE f ");
			sql.append("left join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_MEDIABIN ");
			sql.append("b on f.REL_ID = b.DPY_SYN_MEDIABIN_ID ");
			sql.append("WHERE PROFILE_ID = ? ");
			log.debug(sql +"|"+req.getParameter("wwid"));
			List<MediaBinAssetVO> items = new ArrayList<>();
			try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, req.getParameter("wwid"));
				
				ResultSet rs = ps.executeQuery();
				
				while (rs.next()) {
					items.add(new MediaBinAssetVO(rs));
				}
			} catch (SQLException e) {
				throw new ActionException(e);
			}
			super.putModuleData(items);
		} else {
			throw new ActionException("Invalid APP key");
		}
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		if (validateKey(req.getParameter("key"))) {
			deleteItem(req);
		} else {
			throw new ActionException("Invalid APP key");
		}
	}
	
	private boolean validateKey(String key) {
		if (key == null) return false;
		return key.equals(attributes.get(KEY_NAME));
	}
	
	
	/**
	 * Delete all briefcase items that were passed via the request
	 * @param req
	 */
	private void deleteItem(SMTServletRequest req) {
		String[] assets = req.getParameterValues("briefcaseAssetId");
		String user = req.getParameter("wwid");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
	
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from PROFILE_FAVORITE where PROFILE_ID = ? and SITE_ID = ? and GROUP_CD = ? and PROFILE_FAVORITE_ID in (-1");
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
			log.error("could not delete briefcase items", sqle);
		}
	}

}
