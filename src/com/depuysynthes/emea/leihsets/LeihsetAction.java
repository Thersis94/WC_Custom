package com.depuysynthes.emea.leihsets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalDecoratorAction;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: LeihsetAction.java<p/>
 * <b>Description: manages the DPY_SYN_LEIGHSET database table - invoked from the Leihset Data Tool portlet.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 1, 2015
 ****************************************************************************/
public class LeihsetAction extends SBActionAdapter {

	public LeihsetAction() {
	}

	/**
	 * @param actionInit
	 */
	public LeihsetAction(ActionInitVO actionInit) {
		super(actionInit);
	}



	public void list(SMTServletRequest req) throws ActionException {
		loadLeihsets(req);
	}

	
	/**
	 * Get a single IFU document instance along with its related technique guides
	 * @param req
	 */
	protected void loadLeihsets(SMTServletRequest req) {
		String leihsetId = StringUtil.checkVal(req.getParameter("leihsetId"), null);
		String leihsetAssetId = StringUtil.checkVal(req.getParameter("leihsetAssetId"), null);
		log.debug("Retriving leihsets " + leihsetId + "|" + leihsetAssetId);

		String sql = getSelectSql(leihsetId, leihsetAssetId);
		log.debug(sql);

		int x = 1;
		Map<String,LeihsetVO> data = new LinkedHashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(x++, req.getParameter("organizationId"));
			if (leihsetId != null) ps.setString(x++, leihsetId);
			if (leihsetAssetId != null) ps.setString(x++, leihsetAssetId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String groupId = rs.getString("leihset_group_id");
				if (groupId == null || groupId.length() == 0) groupId = rs.getString("leihset_id");
				LeihsetVO vo = data.get(groupId);
				if (vo == null)
					vo = new LeihsetVO(rs, false);
				
				if (vo.getLeihsetGroupId() == null) vo.setLeihsetGroupId(rs.getString("leihset_group_id"));
				if (rs.getString("leihset_asset_id") != null) vo.addResource(new LeihsetVO(rs, true));
				data.put(groupId, vo);
			}
		} catch (SQLException e) {
			log.error("Unable to get data for leihset: " + leihsetId, e);
		}
		
		log.debug("loaded " + data.size() + " liehsets");
		List<LeihsetVO> list = new ArrayList<LeihsetVO>(data.values());
		Collections.sort(list);
		

		LeihsetCategoryAction ca = new LeihsetCategoryAction();
		ca.setDBConnection(dbConn);
		ca.setAttributes(getAttributes());
		
		//if we're loading a single Leihset, load the category tree
		for (LeihsetVO vo : list) {
			vo.setCategoryTree(ca.loadCategoryTree(vo.getLeihsetId()));
		}
		if (list.size() == 0) { //add form
			req.setAttribute("categories",  ca.loadCategoryTree(null));
		}
		
		super.putModuleData(list);
	}
	

	/**
	 * Create the sql query to get a complete single IFU document instance
	 * @return
	 */
	private String getSelectSql(String leihsetId, String leihsetAssetId) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		sql.append("SELECT l.*, la.*, dsm.TITLE_TXT, dsm.TRACKING_NO_TXT, ws.* ");
		sql.append("FROM ").append(customDb).append("DPY_SYN_LEIHSET l ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_LEIHSET_ASSET la on la.leihset_id=l.leihset_id ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_MEDIABIN dsm on dsm.DPY_SYN_MEDIABIN_ID = la.DPY_SYN_MEDIABIN_ID ");
		sql.append("LEFT JOIN WC_SYNC ws on ws.WC_KEY_ID = l.LEIHSET_ID and ws.WC_SYNC_STATUS_CD not in ('Approved', 'Declined') ");
		sql.append("WHERE l.archive_flg=0 and l.ORGANIZATION_ID=? ");
		if (leihsetId != null) sql.append("and l.LEIHSET_ID=? ");
		if (leihsetAssetId != null) sql.append("and la.LEIHSET_ASSET_ID=? ");
		//putting groupId first ensures we get pending records before live ones, which then get replaced on our Map and sorted by the Comparator
		sql.append("ORDER BY l.leihset_group_id desc, l.ORDER_NO, l.LEIHSET_NM, la.ORDER_NO, la.ASSET_NM");
		return sql.toString();
	}


	/**
	 * Delete the supplied Leihset and redirect the user
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String leihsetId = req.getParameter("leihsetId");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(80);
		sql.append("DELETE FROM ").append(customDb).append("DPY_SYN_LEIHSET WHERE LEIHSET_ID=?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, leihsetId);

			if (ps.executeUpdate() < 1)
				log.warn("No records deleted with id: " + leihsetId);
		} catch (SQLException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("Unable to delete document: " + leihsetId, e);
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
			LeihsetVO vo = new LeihsetVO(req, false);
			if (req.getFile("imageFile") != null)
				vo.setImageUrl(writeFile(req.getFile("imageFile")));

			this.update(vo);
			req.setAttribute(SBActionAdapter.SB_ACTION_ID, vo.getLeihsetId());

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
	 * Update method that works with a premade IFUDocumentVO instead of the 
	 * request object.
	 * @param vo
	 * @throws ActionException
	 */
	private void update(LeihsetVO vo) throws ActionException {
		boolean isInsert = (StringUtil.checkVal(vo.getLeihsetId()).length() == 0);
		if (isInsert) {
			vo.setLeihsetId(new UUIDGenerator().getUUID());
			vo.setLeihsetGroupId(vo.getLeihsetId());
		}
		String sql = buildUpdateSql(isInsert);
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, vo.getOrganizationId());
			ps.setString(2, vo.getLeihsetName());
			ps.setString(3, vo.getImageUrl());
			ps.setInt(4, vo.getOrderNo());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, vo.getLeihsetGroupId());
			ps.setInt(7, vo.getArchiveFlg());
			ps.setString(8, vo.getLeihsetId());

			if (ps.executeUpdate() < 1)
				log.warn("No leihsets updated for " + vo.getLeihsetId());

		} catch (SQLException e) {
			log.error("Unable to update leihset: " + vo.getLeihsetId(), e);
		}
		
		saveCategories(vo);
	}
	
	
	/**
	 * write the body regions and business units to the _category table
	 * @param vo
	 */
	private void saveCategories(LeihsetVO vo) {
		LeihsetCategoryAction ca = new LeihsetCategoryAction();
		ca.setDBConnection(dbConn);
		ca.setAttributes(getAttributes());
		ca.saveXRCategories(vo);
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
			sql.append("INSERT INTO ").append(customDb).append("DPY_SYN_LEIHSET ");
			sql.append("(ORGANIZATION_ID, LEIHSET_NM, IMAGE_URL, ");
			sql.append("ORDER_NO, CREATE_DT, LEIHSET_GROUP_ID, ARCHIVE_FLG, ");
			sql.append("LEIHSET_ID) VALUES(?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(customDb).append("DPY_SYN_LEIHSET ");
			sql.append("set ORGANIZATION_ID=?, LEIHSET_NM=?, IMAGE_URL=?, ");
			sql.append("ORDER_NO=?, UPDATE_DT=?, LEIHSET_GROUP_ID=?, ARCHIVE_FLG=? ");
			sql.append("WHERE LEIHSET_ID=?");
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
		if (req.hasParameter("wasDelete")) {
			redirect.append("?facadeType=leihset&cPage=manage_leihset");
			redirect.append("&leihsetId=").append(req.getParameter("sbActionId"));
			redirect.append("&leihsetGroupId=").append(req.getParameter("leihsetGroupId"));
		}

		return redirect.toString();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String oldLeihset = req.getParameter("sbActionId");
		// Get id of the item that was potentially deleted to trigger this copy in order to exclude it from the new Leihset
		String excludeId = StringUtil.checkVal(req.getParameter("excludeId"));

		try {
			dbConn.setAutoCommit(false);

			// Copy the Leihset
			Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);

			// Replace the group id in order to create continuity between the copy and the original
			Map<String, String> groupId = new HashMap<>();
			groupId.put("", oldLeihset);
			replaceVals.put("LEIHSET_GROUP_ID", groupId);
			replaceVals.put("CREATE_DT", Convert.getCurrentTimestamp());

			RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DPY_SYN_LEIHSET", "LEIHSET_ID", true);
			rdu.addWhereClause("LEIHSET_ID", oldLeihset);
			Map<String, String> ids = rdu.copy();
			replaceVals.put("LEIHSET_ID", ids);

			// Copy all assets of this leihset
			rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DPY_SYN_LEIHSET_ASSET", "LEIHSET_ASSET_ID", true);
			rdu.addWhereListClause("LEIHSET_ID");
			// Prevent the indicated id from being copied in order to simulate a delete of that IMPL
			if (excludeId.length() > 0) rdu.addWhereClause("LEIHSET_ASSET_ID!", excludeId);
			rdu.returnGeneratedKeys(false);
			rdu.copy();

			// Copy all category relationships of this leihset
			rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DPY_SYN_LEIHSET_CATEGORY_XR", "LEIHSET_CATEGORY_XR_ID", true);
			rdu.addWhereListClause("LEIHSET_ID");
			rdu.returnGeneratedKeys(false);
			rdu.copy();
			
			dbConn.commit();

			// Put the new id on the request object so we update the NEW record and not the old one.
			log.error("newId=" + ids.get(oldLeihset));
			req.setAttribute(ApprovalDecoratorAction.SB_ACTION_ID,ids.get(oldLeihset)); //for the ApprovalDecorator
			req.setParameter("sbActionId",ids.get(oldLeihset)); //for the LeihsetVO
			req.setParameter("leihsetGroupId", oldLeihset);

		} catch (Exception e) {
			log.error("Unable to copy record " + oldLeihset, e);
			try {
				dbConn.rollback();
			} catch (Exception e2) {
				log.error("Error rolling back Leihset copy, ", e2);
			}
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e3) {log.error("Error resetting autocommit to 'true', ", e3);}
		}
	}
}