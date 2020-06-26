package com.depuysynthes.action;

// JDK 1.8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.action.ActionRequest;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
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
 * - Added check for custom file URL which is then used in place of the standard base URLs. - DGB 2020-06-03
 * </b>
 ****************************************************************************/
public class MediaBinLinkAction extends SimpleActionAdapter {
	public static final String US_BASE_URL = "http://synthes.vo.llnwd.net/o16/LLNWMB8/US%20Mobile/";
	public static final String INT_BASE_URL = "http://synthes.vo.llnwd.net/o16/LLNWMB8/INT%20Mobile/";

	public MediaBinLinkAction() {
		super();
	}

	public MediaBinLinkAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			req.setValidateInput(Boolean.FALSE);
			String path = getDocumentLink(req.getParameter("mbid"));
			req.setValidateInput(Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, path);
		} catch (Exception e) {
			//we don't care about these in production.
			log.warn("Unable to retrieve media bin file path", e);
			req.setAttribute(Constants.CFG_PAGE_NOT_FOUND, Boolean.TRUE);
		}
	}


	/**
	 * Gets the document path form the provided id
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public String getDocumentLink(String id) throws InvalidDataException {
		StringBuilder s = new StringBuilder(150);
		s.append("select asset_nm, import_file_cd, file_nm, custom_file_url_txt from ");
		s.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		s.append("dpy_syn_mediabin where dpy_syn_mediabin_id = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(s.toString())) {
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				if (StringUtil.isEmpty(rs.getString("custom_file_url_txt"))) {
					//serve Intl assets from an alternate baseUrl
					return (2 == rs.getInt(2) ? INT_BASE_URL : US_BASE_URL) + rs.getString(1);
				} else {
					// use the custom file URL as the base URL
					return rs.getString("custom_file_url_txt") + rs.getString("file_nm");
				}
			}
		} catch (SQLException sqle) {
			log.error("could not load mediaBin asset", sqle);
		}

		throw new InvalidDataException("Media Bin Document not found: " + id);
	}
}