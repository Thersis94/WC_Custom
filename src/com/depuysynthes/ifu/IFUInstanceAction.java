package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: IFUDocumentToolAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Top level action for handling the IFU documents.  This handles
 * all the most broad metadata information pertaining to the documents and leaves
 * handling the actual instances of the documents and their associated technique
 * guides to the appropriate actions.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUInstanceAction extends SBActionAdapter {
	
	public IFUInstanceAction() {
		super();
	}
	
	public IFUInstanceAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("implId") || req.hasParameter("add")) {
			getSingleInstance(req);
		} else {
			getAllInstances(req);
		}
	}
	
	/**
	 * Get a single IFU document instance along with its related technique guides
	 * @param req
	 */
	public void getSingleInstance(SMTServletRequest req) {
		String implId = req.getParameter("implId");
		log.debug("Retriving instance " + implId);
		
		String sql = createSingleInstaceSql();
		log.debug(sql+"|"+implId);
		
		IFUDocumentVO doc = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, implId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				if (doc == null) {
					doc = new IFUDocumentVO(rs);
					doc.setDpySynAssetName(rs.getString("MEDIABIN_NM"));
				}
				IFUTechniqueGuideVO tech = new IFUTechniqueGuideVO(rs);
				tech.setDpySynMediaBinId(rs.getString("TG_MEDIABIN_ID"));
				tech.setUrlText(rs.getString("TG_URL"));
				doc.addTg(tech, true);
			}
		} catch (SQLException e) {
			log.error("Unable to get data for document: " + implId, e);
		}
		
		super.putModuleData(doc);
	}
	
	/**
	 * Create the sql query to get a complete single IFU document instance
	 * @return
	 */
	private String createSingleInstaceSql() {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(400);
		
		sql.append("SELECT *, dit.URL_TXT as TG_URL, dit.DPY_SYN_MEDIABIN_ID as TG_MEDIABIN_ID, ");
		sql.append("dsm.TITLE_TXT as MEDIABIN_NM ");
		sql.append("FROM ").append(customDb).append("DEPUY_IFU_IMPL dii ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU di on ");
		sql.append("di.DEPUY_IFU_ID = dii.DEPUY_IFU_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU_TG_XR ditx on ");
		sql.append("dii.DEPUY_IFU_IMPL_ID = ditx.DEPUY_IFU_IMPL_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU_TG dit on ");
		sql.append("ditx.DEPUY_IFU_TG_ID = dit.DEPUY_IFU_TG_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_MEDIABIN dsm on ");
		sql.append("dsm.DPY_SYN_MEDIABIN_ID = dii.DPY_SYN_MEDIABIN_ID ");
	     sql.append("LEFT JOIN WC_SYNC ws on ws.WC_KEY_ID = dii.DEPUY_IFU_ID and WC_SYNC_STATUS_CD not in ('Approved', 'Declined')");
		sql.append("WHERE dii.DEPUY_IFU_IMPL_ID = ? ");
		sql.append("ORDER BY ditx.ORDER_NO");
		
		return sql.toString();
	}
	
	/**
	 * Get all the instances of the supplied IFU document
	 * @param req
	 */
	private void getAllInstances(SMTServletRequest req) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String ifuId = req.getParameter("ifuId");
		StringBuilder sql = new StringBuilder(260);
		log.debug("Getting list of instances for document " + ifuId);
		
		sql.append("SELECT *, dii.TITLE_TXT as IMPL_TITLE_TXT FROM ").append(customDb).append("DEPUY_IFU di ");
		sql.append("LEFT JOIN ").append(customDb).append("DEPUY_IFU_IMPL dii ");
		sql.append("on di.DEPUY_IFU_ID = dii.DEPUY_IFU_ID ");
		sql.append("WHERE di.DEPUY_IFU_ID = ? ");
		log.debug(sql + "|" + ifuId);
		
		IFUVO con = null;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ifuId);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if (con == null) {
					con = new IFUVO(rs);
				}
				String docTitle = rs.getString("IMPL_TITLE_TXT");
				IFUDocumentVO doc = new IFUDocumentVO(rs);
				doc.setTitleText(docTitle);
				
				con.addIfuDocument(docTitle, doc);
			}
		} catch (SQLException e) {
			log.error("Unable to get instances of IFU: " + ifuId, e);
		}
		
		super.putModuleData(con);
	}
	
	/**
	 * Delete the supplied IFU document instance and redirect the user to the 
	 * parent IFU document.
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String implId = req.getParameter("implId");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(80);
		sql.append("DELETE FROM ").append(customDb).append("DEPUY_IFU_IMPL WHERE DEPUY_IFU_IMPL_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, implId);
			
			if (ps.executeUpdate() < 1)
				log.warn("No records deleted with id: " + implId);
		} catch (SQLException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("Unable to delete document: " + implId, e);
		}
		super.adminRedirect(req, msg, buildRedirect(req));
	}

	/**
	 * Builds an IFUDocumentVO from the request object and passes it along to the
	 * vo specific update method and then redirects the user to the parent IFU
	 * document's page.
	 */
	public void update(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			IFUDocumentVO vo = new IFUDocumentVO(req);
			if (req.getFile("instanceFile") != null)
				vo.setUrlText(writeNewFile(req));
			this.update(vo);
		} catch (ActionException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			throw e;
		} finally {
			super.adminRedirect(req, msg, buildRedirect(req));
		}
	}
	
	/**
	 * Write the new file for this IFU document instance
	 * @param req
	 * @throws ActionException 
	 */
	private String writeNewFile(SMTServletRequest req) throws ActionException {
		try {
			FilePartDataBean file = req.getFile("instanceFile");
			String path = (String)getAttribute(Constants.BINARY_PATH) + IFUFacadeAction.ORG_PATH + req.getParameter("businessUnitName")+"/";
			FileManager fm = new FileManager();
			fm.setPath(path);
			fm.writeFiles(file.getFileData(), path, file.getFileName(), true, false);
			log.debug("Wrote file to " + path + fm.getFileName());
			
			return fm.getFileName();
		} catch (Exception e) {
			log.error("Unable to upload file for new IFU document instance.", e);
			throw new ActionException(e);
		}
	}

	/**
	 * Update method that works with a premade IFUDocumentVO instead of the 
	 * request object.
	 * @param vo
	 * @throws ActionException
	 */
	public void update(IFUDocumentVO vo) throws ActionException {
		boolean isInsert = (StringUtil.checkVal(vo.getImplId()).length() == 0);
		if (isInsert)
			vo.setImplId(new UUIDGenerator().getUUID());
		
		String sql = buildUpdateSql(isInsert);
		log.debug(sql+"|"+vo.getIfuId()+"|"+vo.getTitleText()+"|"+vo.getLanguageCd()+"|"
				+vo.getUrlText()+"|"+vo.getDpySynMediaBinId()+"|"+vo.getArticleText()+"|"
				+vo.getPartNoText()+"|"+vo.getDefaultMsgText()+"|"+vo.getImplId());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, vo.getIfuId());
			ps.setString(i++, vo.getTitleText());
			ps.setString(i++, vo.getLanguageCd());
			ps.setString(i++, vo.getUrlText());
			ps.setString(i++, vo.getDpySynMediaBinId().length() > 0? vo.getDpySynMediaBinId() : null);
			ps.setString(i++, vo.getArticleText());
			ps.setString(i++, vo.getPartNoText());
			ps.setString(i++, vo.getDefaultMsgText());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getImplId());
			
			if (ps.executeUpdate() < 1)
				log.warn("No documents updated for " + vo.getImplId());
			
		} catch (SQLException e) {
			log.error("Unable to update document: " + vo.getImplId(), e);
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
			sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_IMPL ");
			sql.append("(DEPUY_IFU_ID, TITLE_TXT, LANGUAGE_CD, URL_TXT, DPY_SYN_MEDIABIN_ID, ");
			sql.append("ARTICLE_TXT, PART_NO_TXT, DEFAULT_MSG_TXT, CREATE_DT, DEPUY_IFU_IMPL_ID )");
			sql.append("VALUES(?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(customDb).append("DEPUY_IFU_IMPL ");
			sql.append("SET DEPUY_IFU_ID = ?, TITLE_TXT = ?, LANGUAGE_CD = ?, URL_TXT = ?, ");
			sql.append("DPY_SYN_MEDIABIN_ID = ?, ARTICLE_TXT = ?, PART_NO_TXT = ?, ");
			sql.append("DEFAULT_MSG_TXT = ?, UPDATE_DT = ? WHERE DEPUY_IFU_IMPL_ID = ?");
		}
		return sql.toString();
	}
	
	/**
	 * Append extra parameters to the redirect query in order to make sure that
	 * the user is redirected to the parent IFU document of the current document
	 * instance.
	 * @param req
	 * @return
	 */
	private String buildRedirect(SMTServletRequest req) {
		StringBuilder redirect = new StringBuilder(100);
		redirect.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		redirect.append("?facadeType=ifu");
		redirect.append("&ifuId=").append(req.getParameter("ifuId"));
		
		return redirect.toString();
	}
}
