package com.depuysynthes.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MediaBinAdminAction.java<p/>
 * <b>Description: This action lists out MediaBin assets that get loaded by a batch process.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 7, 2013
 * @updates
 * 		JM 09.24.14 - added DSI support and filtering by opco_nm when in the US (import_file_cd=1)
 ****************************************************************************/
public class MediaBinAdminAction extends SimpleActionAdapter {

	public static final String[] VIDEO_ASSETS = { "multimedia file", "quicktime", "flash video" };
	public static final String[] PDF_ASSETS = { "pdf", "adobe illustrator" };

	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.ActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
	public void list(SMTServletRequest req) throws ActionException {
		String orgId = req.getParameter("organizationId");
		MediaBinDistChannels mb = new MediaBinDistChannels(orgId);
		int typeCd = mb.getTypeCd();
		String opCoNm = mb.getOpCoNm();

		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_MEDIABIN where import_file_cd=?");
		if (req.hasParameter("sDivision")) sql.append(" and business_unit_nm like ?");
		if (req.hasParameter("sProduct"))  sql.append(" and (prod_family like ? or prod_nm like ?)");
		if (req.hasParameter("sTracking")) sql.append(" and tracking_no_txt like ?");
		//DS and DSI need to be sub-filtered here, using opco_nm
		if (typeCd == 1) sql.append(" and opco_nm like ?");

		String assetType = StringUtil.checkVal(req.getParameter("assetType"));
		log.debug("assetType=" + assetType);
		if ("quicktime".equalsIgnoreCase(assetType)) { //loop all video types
			sql.append(" and lower(asset_type) in (?");
			for (int x=VIDEO_ASSETS.length; x > 0; x--) sql.append(",?");
			sql.append(")");
		} else if (assetType.length() > 0) {
			sql.append(" and lower(asset_type) in (?"); //loop all pdf types
			for (int x=PDF_ASSETS.length; x > 0; x--) sql.append(",?");
			sql.append(")");
		}

		log.debug(sql);

		List<MediaBinAssetVO> data = new ArrayList<MediaBinAssetVO>();
		PreparedStatement ps = null;
		int i=0;
		try { 
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(++i, typeCd); //organization/country
			if (req.hasParameter("sDivision")) ps.setString(++i, "%" + req.getParameter("sDivision") + "%");
			if (req.hasParameter("sProduct")) {
				ps.setString(++i, req.getParameter("sProduct") + "%");
				ps.setString(++i, req.getParameter("sProduct") + "%");
			}
			if (req.hasParameter("sTracking")) ps.setString(++i, req.getParameter("sTracking") + "%");
			if (typeCd == 1) ps.setString(++i, "%" + opCoNm + "%");
			
			if ("quicktime".equalsIgnoreCase(assetType)) {
				ps.setString(++i, "video");
				for (String at: VIDEO_ASSETS) ps.setString(++i, at);
			} else if (assetType.length() > 0) {
				ps.setString(++i, "pdf");
				for (String at: PDF_ASSETS) ps.setString(++i, at);
			}

			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new MediaBinAssetVO(rs));

		} catch (SQLException sqle) {
			throw new ActionException("Error loading MediaBin data", sqle);
		} finally {
			DBUtil.close(ps);
		}

		log.debug("size=" + data.size());
		super.putModuleData(data, data.size(), true);
	}

}
