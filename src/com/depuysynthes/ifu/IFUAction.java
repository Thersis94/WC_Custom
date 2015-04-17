package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: IFUAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Handles instance specific information and metadata for the IFU documents.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUAction  extends SBActionAdapter {
	
	public IFUAction() {
		super();
	}
	
	public IFUAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	/**
	 * Determine whether or not we are getting a single IFU or all of them
	 */
	public void list(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("ifuId") || req.hasParameter("add")) {
			getSingleIFU(req);
		} else {
			getAllIFU(req);
		}
	}
	
	
	/**
	 * Get all IFU documents
	 * @param req
	 */
	private void getAllIFU(SMTServletRequest req) {
		log.debug("Listing all IFUs");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(55);
		sql.append("SELECT * FROM ").append(customDb).append("DEPUY_IFU di ");
	     sql.append("LEFT JOIN WC_SYNC ws on ws.WC_KEY_ID = di.DEPUY_IFU_ID and WC_SYNC_STATUS_CD not in ('Approved', 'Declined')");
	     sql.append("WHERE DEPUY_IFU_ID not in (SELECT DEPUY_IFU_GROUP_ID FROM ");
	     sql.append(customDb).append("DEPUY_IFU di WHERE DEPUY_IFU_GROUP_ID is not null and DEPUY_IFU_GROUP_ID != DEPUY_IFU_ID) ");
	     sql.append("ORDER BY ORDER_NO, TITLE_TXT");
		
		log.debug(sql);
		List<IFUVO> data = new ArrayList<IFUVO>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				data.add(new IFUVO(rs));
			}
		} catch (SQLException e) {
			log.error("Could not get list of IFU containers.", e);
		}
		
		String previewApiKey = ApprovalController.generatePreviewApiKey(attributes);
	     req.setParameter(Constants.PAGE_PREVIEW, previewApiKey);
		
		super.putModuleData(data);
	}

	
	/**
	 * Get a single IFU document along with all it's language instances
	 * @param req
	 * @throws ActionException
	 */
	public void getSingleIFU(SMTServletRequest req) throws ActionException {
		log.debug("Reftriving IFUs");
		String ifuId = req.getParameter("ifuId");
		String sql = buildSingleIFUSql();
		log.debug(sql+"|"+ifuId);
		
		IFUVO vo = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, ifuId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				if (vo == null) {
					vo = new IFUVO(rs);
				}
				if (rs.getString("DEPUY_IFU_IMPL_ID") != null) {
					IFUDocumentVO doc = new IFUDocumentVO(rs);
					doc.setTitleText(rs.getString("IMPL_TITLE_TXT"));
					
					vo.addIfuDocument(doc.getImplId(), doc);
				}
			}
		} catch (SQLException e) {
			log.error("Unable to get data for document: " + ifuId, e);
		}
		
		super.putModuleData(vo);
	}
	
	
	/**
	 * Creates the query to get a single IFU document and all it's language instances
	 * @return
	 */
	private String buildSingleIFUSql() {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT *, dii.TITLE_TXT as IMPL_TITLE_TXT FROM ").append(customDb).append("DEPUY_IFU di ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU_IMPL dii on ");
		sql.append("di.DEPUY_IFU_ID = dii.DEPUY_IFU_ID ");
		sql.append("LEFT JOIN LANGUAGE l on l.LANGUAGE_CD = dii.LANGUAGE_CD ");
	     sql.append("LEFT JOIN WC_SYNC ws on ws.WC_KEY_ID = di.DEPUY_IFU_ID and WC_SYNC_STATUS_CD not in ('Approved', 'Declined') ");
		sql.append("WHERE di.DEPUY_IFU_ID = ? ");
		
		return sql.toString();
	}
	
	
	/**
	 * Delete the supplied IFU document
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		log.debug("Deleting document");
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String ifuId = req.getParameter("ifuId");
		
		StringBuilder sql = new StringBuilder(70);
		sql.append("DELETE ").append(customDb).append("DEPUY_IFU WHERE DEPUY_IFU_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ifuId);
			
			if (ps.executeUpdate() < 1)
				log.warn("No records deleted for ifu: " + ifuId);
		} catch (SQLException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("Unable to delete ifu with id: " + ifuId, e);
		}

		super.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	
	/**
	 * Create an IFUVO from the request object and and send it to the
	 *  vo specific updater
	 */
	public void update(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			IFUVO ifu = new IFUVO(req);
			this.update(ifu);
			req.setAttribute(SBActionAdapter.SB_ACTION_ID, ifu.getIfuId());
		} catch (ActionException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			throw e;
		} finally {
			super.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		}
	}
	
	
	/**
	 * VO specific update method that updates a record with information from a 
	 * vo instead of the request object.
	 * @param vo
	 * @throws ActionException
	 */
	public void update(IFUVO vo) throws ActionException {
		log.debug("Updating IFU Document");
		boolean isInsert = (StringUtil.checkVal(vo.getIfuId()).length() == 0);
		if  (isInsert) {
			vo.setIfuId(new UUIDGenerator().getUUID());
			vo.setIfuGroupId(vo.getIfuId());
		}
		
		String sql = buildUpdateSql(isInsert);
		log.debug(sql+"|"+vo.getIfuGroupId()+"|"+vo.getTitleText()+"|"+vo.getBusinessUnitName()+
				"|"+vo.getArchiveFlg()+"|"+vo.getOrderNo()+"|"+vo.getVersionText()+"|"+vo.getIfuId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, vo.getIfuGroupId());
			ps.setString(i++, vo.getTitleText());
			ps.setInt(i++, vo.getArchiveFlg());
			ps.setString(i++, vo.getBusinessUnitName());
			ps.setInt(i++, vo.getOrderNo());
			ps.setString(i++, vo.getVersionText());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getIfuId());
			
			if (ps.executeUpdate() < 1) 
				log.warn("Nothing updated for IFU with id: " + vo.getIfuId());
		} catch (SQLException e) {
			log.error("Unable to update IFU with id: " + vo.getIfuId(), e);
		}
	}
	
	
	/**
	 * Build the update/insert query for the IFU document
	 * @param isInsert
	 * @return
	 */
	private String buildUpdateSql(boolean isInsert) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		if (isInsert) {
			sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU (");
			sql.append("DEPUY_IFU_GROUP_ID, TITLE_TXT, ARCHIVE_FLG, BUSINESS_UNIT_NM, ORDER_NO, VERSION_TXT, CREATE_DT, DEPUY_IFU_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(customDb).append("DEPUY_IFU SET ");
			sql.append("DEPUY_IFU_GROUP_ID = ?, TITLE_TXT = ?, ARCHIVE_FLG = ?, BUSINESS_UNIT_NM = ?, ");
			sql.append("ORDER_NO = ?, VERSION_TXT = ?, UPDATE_DT = ? WHERE DEPUY_IFU_ID = ? ");
		}
		
		return sql.toString();
	}
	
	
	/**
	 * Create a copy of the supplied IFU 
	 */
	@SuppressWarnings("unchecked")
	public void copy(SMTServletRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String oldIFU = req.getParameter("ifuId");
		// Get id of the item that was potentially deleted to trigger this copy in order to exclude it from the new IFU
		String excludeId = StringUtil.checkVal(req.getParameter("excludeId"));
		
		try {
			dbConn.setAutoCommit(false);
			
			// Copy the IFU
			Map<String, Object> replaceVals = (Map<String, Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
			
			// Replace the group id in order to create continuity between the copy and the original
			Map<String, String> groupId = new HashMap<>();
			groupId.put("", oldIFU);
			replaceVals.put("DEPUY_IFU_GROUP_ID", groupId);
			replaceVals.put("CREATE_DT", Convert.getCurrentTimestamp());
			
			RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DEPUY_IFU", "DEPUY_IFU_ID", true);
			rdu.addWhereClause("DEPUY_IFU_ID", oldIFU);
			Map<String, String> ifuIds = rdu.copy();
			replaceVals.put("DEPUY_IFU_ID", ifuIds);
			
			// Copy all implementations of this ifu
			rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DEPUY_IFU_IMPL", "DEPUY_IFU_IMPL_ID", true);
			rdu.addWhereListClause("DEPUY_IFU_ID");
			// Prevent the indicated id from being copied in order to simulate a delete of that IMPL
			rdu.addWhereClause("DEPUY_IFU_IMPL_ID!", excludeId);
			Map<String, String> implIds = rdu.copy();
			replaceVals.put("DEPUY_IFU_IMPL_ID", implIds);
			
			// Copy all technique guides for all implementations
			rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DEPUY_IFU_TG", "DEPUY_IFU_TG_ID", true);
			rdu.setWhereSQL("DEPUY_IFU_TG_ID in (SELECT DEPUY_IFU_TG_ID FROM " + customDb + "DEPUY_IFU_TG_XR WHERE " +rdu.buildWhereListClause("DEPUY_IFU_IMPL_ID",true)+")");
			// Prevent the indicated id from being copied in order to simulate a delete of that TG
			rdu.addWhereClause("DEPUY_IFU_TG_ID!", excludeId);
			Map<String, String> tgIds = rdu.copy();
			replaceVals.put("DEPUY_IFU_TG_ID", tgIds);
			
			// Copy all xr records for the copied implementations and their technique guides
			rdu = new RecordDuplicatorUtility(attributes, dbConn, customDb + "DEPUY_IFU_TG_XR", "DEPUY_IFU_TG_XR_ID", true);
			rdu.addWhereListClause("DEPUY_IFU_TG_ID");
			rdu.addWhereListClause("DEPUY_IFU_IMPL_ID");
			
			rdu.copy();
			
			
			dbConn.commit();
			
			// Get the id of the copied ifu as well as any implementation or
			// technique guide ids that we passed on the request object.
			String ifuId = ifuIds.get(ifuIds.keySet().toArray()[0]);
			String implId = implIds.get(req.getParameter("implId"));
			String tgId = tgIds.get(req.getParameter("tgId"));
			
			// Put the new id on the request object for both the base and 
			// group id so that the new one is treated as in progress as well
			// as the new tg and impl ids so that we don't end up calling update
			// on the old version of the documents.
			req.setParameter("ifuId", ifuId);
			req.setParameter("implId", implId);
			req.setParameter("tgId", tgId);
			req.setParameter("ifuGroupId", oldIFU);
			req.setAttribute("sbActionId",ifuId);
		} catch (SQLException e) {
			log.error("Unable to copy record " + oldIFU, e);
			try {
				dbConn.rollback();
			} catch (Exception e2) {
				log.error("Error rolling back IFU Copy, ", e2);
			}
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e3) {log.error("Error resetting autocommit to 'true', ", e3);}
		}
		
	}
	
	
}
