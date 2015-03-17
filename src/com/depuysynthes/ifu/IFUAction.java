package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

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
		sql.append("SELECT * FROM ").append(customDb).append("DEPUY_IFU ORDER BY TITLE_TXT");
		
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
			String oldVersion = StringUtil.checkVal(req.getParameter("oldVersion"));
			if (oldVersion.length() != 0 && !oldVersion.equals(req.getParameter("versionTxt"))) {
				this.copy(req);
			}
			
			this.update(new IFUVO(req));
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
	public void copy(SMTServletRequest req) throws ActionException {
		
		try {
			dbConn.setAutoCommit(false);
			
			IFUVO ifu = getImplemenatations(req);
			archiveIFU(ifu);
			copyIfu(ifu);
			copyImpl(ifu);
			copyTG(ifu);
			addXRs(ifu);
			
			dbConn.commit();
			
			// Put the new id on the request object
			req.setParameter("ifuId", ifu.getIfuId());

		} catch(Exception e) {
			try {
				dbConn.rollback();
			} catch (SQLException sqle) {
				log.error("A Problem Occured During Rollback.", sqle);
			}
			throw new ActionException(e);
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e) {}
		}
	}
	
	
	/**
	 * Archive the current version of the IFU
	 * @param ifu
	 */
	private void archiveIFU(IFUVO ifu) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		
		sql.append("UPDATE ").append(customDb).append("DEPUY_IFU ");
		sql.append("SET ARCHIVE_FLG=1, UPDATE_DT=? WHERE DEPUY_IFU_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ifu.getIfuId());
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			
			if (ps.executeUpdate() < 1)
				throw new SQLException("No records updated when attempting to archive ifu with id: " + ifu.getIfuId());
		} catch(SQLException e) {
			log.error("Unable to archive ifu with id: " + ifu.getIfuId(), e);
			throw e;
		}
		
	}
	
	
	/**
	 * Get all the implementations of the current document as well as all
	 * technique guides associated with those implementations
	 * @param ifuId
	 */
	private IFUVO getImplemenatations(SMTServletRequest req) throws SQLException {
		IFUVO ifu = null;
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(340);
		
		sql.append("SELECT *, dii.TITLE_TXT as IMPL_TITLE_TXT, dit.DPY_SYN_MEDIABIN_ID as TG_MEDIABIN_ID FROM ");
		sql.append(customDb).append("DEPUY_IFU di LEFT JOIN ").append(customDb).append("DEPUY_IFU_IMPL dii on ");
		sql.append("di.DEPUY_IFU_ID = dii.DEPUY_IFU_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU_TG_XR ditx on ditx.DEPUY_IFU_IMPL_ID = dii.DEPUY_IFU_IMPL_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU_TG dit on dit.DEPUY_IFU_TG_ID = ditx.DEPUY_IFU_TG_ID ");
		sql.append("WHERE di.DEPUY_IFU_ID = ?");
		log.debug(sql+"|"+req.getParameter("ifuId"));
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter("ifuId"));
			
			ResultSet rs = ps.executeQuery();
			String oldId = "";
			IFUDocumentVO doc = null;
			
			while(rs.next()) {
				if (ifu == null) {
					ifu = new IFUVO(rs);
				}
				
				if (StringUtil.checkVal(rs.getString("DEPUY_IFU_IMPL_ID")).length() > 0) {
					if (!oldId.equals(rs.getString("DEPUY_IFU_IMPL_ID"))) {
						if (doc != null) ifu.addIfuDocument(doc.getImplId(), doc);
						doc = new IFUDocumentVO(rs);
						oldId = doc.getImplId();
						doc.setImplId(new UUIDGenerator().getUUID());
						doc.setTitleText(rs.getString("IMPL_TITLE_TXT"));
					}
					IFUTechniqueGuideVO tech = new IFUTechniqueGuideVO(rs);
					tech.setTgId(new UUIDGenerator().getUUID());
					doc.addTg(tech, true);
				}
			}
			
			// Add the straggler as long as it isn't null
			if (doc != null)
				ifu.addIfuDocument(doc.getImplId(), doc);
			
		} catch (SQLException e) {
			log.error("Unable to get documents for ifu " + req.getParameter("ifuId"), e);
			throw e;
		}
		return ifu;
	}
	
	
	/**
	 * Generate a new id for the copy and place it into the database as the new
	 * current version of the document.
	 * @param vo
	 * @throws SQLException
	 */
	private void copyIfu(IFUVO vo) throws SQLException {
		String sql = buildUpdateSql(true);
		
		vo.setIfuId(new UUIDGenerator().getUUID());
		
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
				throw new SQLException("Insert failed on IFU level for IFU document copy");
		} catch (SQLException e) {
			log.error("Unable to insert IFU copy", e);
			throw e;
		}
	}
	
	
	/**
	 * Copy all the ifu documents implementations
	 * @param ifu
	 * @throws SQLException
	 */
	private void copyImpl(IFUVO ifu) throws SQLException {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_IMPL ");
		sql.append("(DEPUY_IFU_ID, TITLE_TXT, LANGUAGE_CD, URL_TXT, DPY_SYN_MEDIABIN_ID, ");
		sql.append("ARTICLE_TXT, PART_NO_TXT, DEFAULT_MSG_TXT, CREATE_DT, DEPUY_IFU_IMPL_ID )");
		sql.append("VALUES(?,?,?,?,?,?,?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			for (String key : ifu.getIfuDocuments().keySet()) {
				IFUDocumentVO vo = ifu.getIfuDocuments().get(key);
				
				int i = 1;
				ps.setString(i++, ifu.getIfuId());
				ps.setString(i++, vo.getTitleText());
				ps.setString(i++, vo.getLanguageCd());
				ps.setString(i++, vo.getUrlText());
				ps.setString(i++, vo.getDpySynMediaBinId());
				ps.setString(i++, vo.getArticleText());
				ps.setString(i++, vo.getPartNoText());
				ps.setString(i++, vo.getDefaultMsgText());
				ps.setTimestamp(i++, Convert.getCurrentTimestamp());
				ps.setString(i++, vo.getImplId());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			log.error("Unable to copy implementations for ifu id: " + ifu.getIfuId(), e);
			throw e;
		}
	}
	
	
	/**
	 * Copies all the technique guides for all the implementations for
	 * the current ifu
	 * @param ifu
	 * @throws SQLException
	 */
	private void copyTG(IFUVO ifu) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(175);
		
		sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_TG (");
		sql.append("TG_NM, URL_TXT, DPY_SYN_MEDIABIN_ID, CREATE_DT, DEPUY_IFU_TG_ID) ");
		sql.append("VALUES(?,?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			for (String key : ifu.getIfuDocuments().keySet()) {
				IFUDocumentVO vo = ifu.getIfuDocuments().get(key);
				
				for (IFUTechniqueGuideVO tech : vo.getTgList()) {
					
					int i = 1;
					ps.setString(i++, tech.getTgName());
					ps.setString(i++, tech.getUrlText());
					ps.setString(i++, tech.getDpySynMediaBinId());
					ps.setTimestamp(i++, Convert.getCurrentTimestamp());
					ps.setString(i++, tech.getTgId());
					
					ps.addBatch();
					
				}
				
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			log.error("Unable to copy Technique Guides for ifu id" + ifu.getIfuId(), e);
			throw e;
		}
	}
	
	
	/**
	 * Create the xrs for the implementations and their technique guides
	 * @param ifu
	 * @throws SQLException
	 */
	private void addXRs(IFUVO ifu) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(165);
		
		sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_TG_XR (");
		sql.append("ORDER_NO, CREATE_DT, DEPUY_IFU_TG_ID, DEPUY_IFU_IMPL_ID) ");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			
			for (String key : ifu.getIfuDocuments().keySet()) {
				IFUDocumentVO vo = ifu.getIfuDocuments().get(key);
				
				for (IFUTechniqueGuideVO tech : vo.getTgList()) {
					
					int i = 1;
					ps.setInt(i++, tech.getOrderNo());
					ps.setTimestamp(i++, Convert.getCurrentTimestamp());
					ps.setString(i++, tech.getTgId());
					ps.setString(i++, vo.getImplId());
					
					ps.addBatch();
				}
				
			}
			
			ps.executeBatch();
			
		} catch (SQLException e) {
			log.error("Unable to create xrs for ifu id" + ifu.getIfuId(), e);
			throw e;
		}
	}


}
