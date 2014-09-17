package com.depuysynthes.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



// SMT BAse Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// 
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MediaBinLinkAction.java <p/>
 * <b>Project</b>: SB_DePuy <p/>
 * <b>Description: </b> Manages access to the media bin files used on the 
 * DS.com site.  looks up the id and finds the path to the most current version of the file
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jun 13, 2013<p/>
 * <b>Changes: 
 * added baseUrlIntl, needed to serve Intl assets separely from US ones. -JM 07-11-2013
 * added PageViewUDPUtil to track downloads/impressions as we do other PageViews -JM 09.05.14
 * </b>
 ****************************************************************************/
public class MediaBinLinkAction extends SimpleActionAdapter {
	public static final String US_BASE_URL = "http://synthes.vo.llnwd.net/o16/LLNWMB8/US%20Mobile/";
	public static final String INT_BASE_URL = "http://synthes.vo.llnwd.net/o16/LLNWMB8/INT%20Mobile/";
	
	public MediaBinLinkAction() {
		
	}

	public MediaBinLinkAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	public void list(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		mod.setSimpleAction(Boolean.TRUE);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		//boolean success = false;
		try {
			String path = this.getDocumentLink(req.getParameter("mbid"));
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, path);
			//success = true;
		} catch (Exception e) {
			//we don't care about these in production.
			log.debug("Unable to retrieve media bin file path", e);
			req.setAttribute(Constants.CFG_PAGE_NOT_FOUND, Boolean.TRUE);
		}
		
		/**
		 * This was added for DSI, then they decided not to use it.  -JM 09.12.14
		 * 
		//drop a message to UDP for PageViewReporting to capture
		//this is not done by the Filter because we're returning a 302 response header (a redirect)
		if (success) {
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			PageViewUDPUtil util = new PageViewUDPUtil();
			byte[] data = util.buildPayload(site, null, "/mediabin/" + req.getParameter("mbid"));
			try {
				util.sendDatagram(data, attributes);
			} catch (Exception e) {
				log.error("could not log mediabin pageview for " + req.getParameter("mbid"), e);
			}
		}
		 */
	}
	
	/**
	 * Gets the document path form the provided id
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public String getDocumentLink(String id) throws InvalidDataException {
		StringBuilder s = new StringBuilder();
		s.append("select asset_nm, import_file_cd from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		s.append("dpy_syn_mediabin where dpy_syn_mediabin_id = ?");
		
		String url = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				//serve Intl assets from an alternate baseUrl
				url = (Convert.formatInteger(rs.getInt(2)) == 2) ? INT_BASE_URL : US_BASE_URL;
				url += rs.getString(1);
			} else {
				throw new InvalidDataException("Media Bin Document not found: " + id);
			}
		} catch (SQLException sqle) {
			log.error("could not load mediaBin asset", sqle);
			throw new InvalidDataException("Media Bin Document not found: " + id);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return url;
	}
}
