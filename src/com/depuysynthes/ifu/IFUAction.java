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
import com.smt.sitebuilder.common.SiteBuilderUtil;
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
			return;
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
		
		StringBuilder sql = new StringBuilder();
		
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

		SiteBuilderUtil util = new SiteBuilderUtil();
		util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	/**
	 * Create an IFUVO from the request object and and send it to the
	 *  vo specific updater
	 */
	public void update(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			this.update(new IFUVO(req));
		} catch (ActionException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			throw e;
		} finally {
			SiteBuilderUtil util = new SiteBuilderUtil();
			util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
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
		boolean isInsert = false;
		if (StringUtil.checkVal(vo.getIfuId()).length() == 0) {
			isInsert = true;
			vo.setIfuId(new UUIDGenerator().getUUID());
			vo.setIfuGroupId(vo.getIfuId());
		}
		
		String sql = buildUpdateSql(isInsert);
		log.debug(sql+"|"+vo.getIfuGroupId()+"|"+vo.getTitleText()+"|"+vo.getBusinessUnitName()+"|"+vo.getArchiveFlg()+"|"+vo.getOrderNo()+"|"+vo.getVersionText()+"|"+vo.getIfuId());
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
		StringBuilder sql = new StringBuilder();
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

}
