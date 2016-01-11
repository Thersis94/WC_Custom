package com.depuysynthes.emea.leihsets;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LiehsetAssetAction.java<p/>
 * <b>Description: manages write actions for the LeihsetAsset table in the database.
 * List, Copy, and Retrieve are done elsewhere in this package.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 1, 2015
 ****************************************************************************/
public class LeihsetAssetAction extends SBActionAdapter {

	public LeihsetAssetAction() {
	}

	/**
	 * @param actionInit
	 */
	public LeihsetAssetAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/**
	 * Delete the supplied Leihset and redirect the user
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String leihsetAssetId = req.getParameter("leihsetAssetId");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(80);
		sql.append("DELETE FROM ").append(customDb).append("DPY_SYN_LEIHSET_ASSET WHERE LEIHSET_ASSET_ID=?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, leihsetAssetId);

			if (ps.executeUpdate() < 1)
				log.warn("No records deleted with id: " + leihsetAssetId);
		} catch (SQLException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("Unable to delete asset: " + leihsetAssetId, e);
		}
		super.adminRedirect(req, msg, buildRedirect(req));
	}
	

	/**
	 * Builds a LeihsetVO from the request object and passes it along to the
	 * vo specific update method and then redirects the user
	 */
	public void update(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			LeihsetVO vo = new LeihsetVO(req, true);
			if (req.getFile("excelFile") != null)
				vo.setExcelUrl(writeFile(req.getFile("excelFile")));
			if (req.getFile("pdfFile") != null)
				vo.setPdfUrl(writeFile(req.getFile("pdfFile")));

			this.update(vo);

		} catch (ActionException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			throw e;
		} finally {
			super.adminRedirect(req, msg, buildRedirect(req));
		}
	}


	/**
	 * Write the new file to disk
	 * @param req
	 * @throws ActionException 
	 */
	private String writeFile(FilePartDataBean file) throws ActionException {
		LeihsetFacadeAction lfa = new LeihsetFacadeAction(actionInit);
		lfa.setAttributes(getAttributes());
		return lfa.writeFile(file);
	}
	

	/**
	 * Update method that works with a premade LeihsetVO instead of the 
	 * request object.
	 * @param vo
	 * @throws ActionException
	 */
	private void update(LeihsetVO vo) throws ActionException {
		boolean isInsert = (StringUtil.checkVal(vo.getLeihsetAssetId()).length() == 0);
		if (isInsert) vo.setLeihsetAssetId(new UUIDGenerator().getUUID());
		String sql = buildUpdateSql(isInsert);
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, vo.getLeihsetId());
			ps.setString(2, vo.getAssetName());
			ps.setString(3, vo.getAssetNumber());
			ps.setString(4, vo.getExcelUrl());
			ps.setString(5, vo.getPdfUrl());
			ps.setString(6, vo.getDpySynMediaBinId());
			ps.setInt(7, vo.getOrderNo());
			ps.setTimestamp(8, Convert.getCurrentTimestamp());
			ps.setString(9, vo.getLeihsetAssetId());

			if (ps.executeUpdate() < 1)
				log.warn("No leihset assets updated for " + vo.getLeihsetAssetId());

		} catch (SQLException e) {
			log.error("Unable to update leihset asset: " + vo.getLeihsetAssetId(), e);
		}
	}


	/**
	 * Build the update or insert sql.
	 * @param isInsert
	 * @return
	 */
	private String buildUpdateSql(boolean isInsert) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		if (isInsert) {
			sql.append("INSERT INTO ").append(customDb).append("DPY_SYN_LEIHSET_ASSET ");
			sql.append("(LEIHSET_ID, ASSET_NM, ASSET_NO, EXCEL_URL, PDF_URL, ");
			sql.append("DPY_SYN_MEDIABIN_ID, ORDER_NO, CREATE_DT, LEIHSET_ASSET_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(customDb).append("DPY_SYN_LEIHSET_ASSET ");
			sql.append("set LEIHSET_ID=?, ASSET_NM=?, ASSET_NO=?, EXCEL_URL=?, PDF_URL=?, ");
			sql.append("DPY_SYN_MEDIABIN_ID=?, ORDER_NO=?, UPDATE_DT=? where LEIHSET_ASSET_ID=?");
		}
		return sql.toString();
	}
	
	

	/**
	 * Append extra parameters to the redirect query in order to make sure that
	 * the user is redirected to the parent Leihset document of the current instance.
	 * @param req
	 * @return
	 */
	private String buildRedirect(SMTServletRequest req) {
		StringBuilder redirect = new StringBuilder(100);
		redirect.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		redirect.append("?facadeType=leihset&cPage=manage_leihset");
		redirect.append("&leihsetId=").append(req.getParameter("sbActionId"));
		return redirect.toString();
	}
}