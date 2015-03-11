package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: IFUTechniqueFacadeAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Handles information and metadata specific to the technique
 * guides of a particular instance of an IFU.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUTechniqueAction extends SBActionAdapter {
	
	public IFUTechniqueAction() {
		super();
	}
	
	public IFUTechniqueAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public IFUTechniqueAction(ActionInitVO actionInit, SMTDBConnection conn) {
		super(actionInit);
		super.setDBConnection(conn);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String tgId = req.getParameter("tgId");
		
		StringBuilder sql = new StringBuilder(90);
		
		sql.append("SELECT *, dit.DPY_SYN_MEDIABIN_ID as tg_mediabin_id, dit.URL_TXT as tg_url FROM ");
		sql.append(customDb).append("DEPUY_IFU_TG dit ");
		sql.append("left join ").append(customDb).append("DEPUY_IFU_TG ditx on ");
		sql.append("dit.DEPUY_IFU_TG_ID = ditx.DEPUY_IFU_TG_ID ");
		sql.append("left join ").append(customDb).append("DPY_SYN_MEDIABIN dsm on ");
		sql.append("dsm.DPY_SYN_MEDIABIN_ID = dit.DPY_SYN_MEDIABIN_ID WHERE dit.DEPUY_IFU_TG_ID = ?");
		log.debug(sql+"|"+tgId);
		IFUTechniqueGuideVO tech = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, tgId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next())
				tech = new IFUTechniqueGuideVO(rs);
			
		} catch (SQLException e) {
			log.error("Could not get Technique Guide with id: " + tgId, e);
		}
		super.putModuleData(tech);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("tgId") || req.hasParameter("add")) {
			this.retrieve(req);
			return;
		}
		
		String documentId = req.getParameter("documentId");
		String sql = buildListSql();
		List<IFUTechniqueGuideVO> data = new ArrayList<IFUTechniqueGuideVO>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, documentId);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(new IFUTechniqueGuideVO(rs));
			}
		} catch (SQLException e) {
			log.error("Could not get technique guides for document with id: " + documentId);
		} 
		
	}
	
	private String buildListSql() {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("SELECT dit.* FROM ").append(customDb).append("DEPUY_IFU_IMPL dii ");
		sql.append("left join ").append(customDb).append("DEPUY_IFU_TG_XR ditx on ");
		sql.append("ditx.DEPUY_IFU_IMPL_ID = dii.DEPUY_IFU_IMPL_ID ");
		sql.append("left join ").append(customDb).append("DEPUY_IFU_TG dit on ");
		sql.append("dit.DEPUY_IFU_TG_ID = ditx.DEPUY_IFU_TG_ID ");
		sql.append("WHERE DEPUY_IFU_IMPL_ID = ?");
		
		return sql.toString();
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String tgId = req.getParameter("tgId");
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("DELETE FROM ").append(customDb).append("DEPUY_IFU_TG WHERE DEPUY_IFU_TG_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, tgId);
			
			if (ps.executeUpdate() < 1)
				log.warn("No Technique Guides deleted with id: " + tgId);
		} catch (SQLException e) {
			log.error("Could not delete Technique Guide with id: " + tgId, e);
		}

		SiteBuilderUtil util = new SiteBuilderUtil();
		util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	public void update(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			this.update(new IFUTechniqueGuideVO(req));
		} catch (ActionException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			throw e;
		} finally {
			SiteBuilderUtil util = new SiteBuilderUtil();
			util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		}
	}
	
	public void update(IFUTechniqueGuideVO vo ) throws ActionException {
		boolean isInsert = false;
		if (StringUtil.checkVal(vo.getTgId()).length() == 0) {
			isInsert = true;
			vo.setTgId(new UUIDGenerator().getUUID());
		}
		
		String sql = buildUpdateSql(isInsert);
		log.debug(sql+"|"+vo.getTgName()+"|"+vo.getUrlText()+"|"+vo.getDpySynMediaBinId()+"|"+vo.getTgId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, vo.getTgName());
			ps.setString(i++, vo.getUrlText());
			ps.setString(i++, vo.getDpySynMediaBinId());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getTgId());
			
			if (ps.executeUpdate() < 1)
				log.warn("No Technique Guides updated for id: " + vo.getTgId());
			
			// If we are dealing with a brand new Technique guide we need to set up the xr record
			if (isInsert)
				updateXR(vo);
			
		} catch (SQLException e) {
			log.error("Unable to update Technique Guide with id: " + vo.getTgId(), e);
		}
	}
	
	private void updateXR(IFUTechniqueGuideVO vo) throws SQLException{
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		
		sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_TG_XR (");
		sql.append("DEPUY_IFU_TG_ID, DEPUY_IFU_IMPL_ID, ORDER_NO, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 1;
			ps.setString(i++, vo.getTgId());
			ps.setString(i++, vo.getImplId());
			ps.setInt(i++, vo.getOrderNo());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			
			if (ps.executeUpdate() < 1) 
				log.warn("Unable to insert xr record for instance " + vo.getImplId() + " and Technique Guide " + vo.getTgId());
		}
		
		
	}

	private String buildUpdateSql(boolean isInsert) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		if (isInsert) {
			sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_TG (");
			sql.append("TG_NM, URL_TXT, DPY_SYN_MEDIABIN_ID, CREATE_DT, DEPUY_IFU_TG_ID) ");
			sql.append("VALUES(?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(customDb).append("DEPUY_IFU_TG SET ");
			sql.append("TG_NM = ?, URL_TXT = ?, DPY_SYN_MEDIABIN_ID = ?, UPDATE_DT = ? ");
			sql.append("WHERE DEPUY_IFU_TG_ID = ? ");
		}
		return sql.toString();
	}
}
