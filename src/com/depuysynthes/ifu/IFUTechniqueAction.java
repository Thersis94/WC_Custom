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
		String tgId = req.getParameter("guideId");
		
		StringBuilder sql = new StringBuilder(90);
		
		sql.append("SELECT * FROM ").append(customDb);
		sql.append("DEPUY_IFU_TG WHERE DPUY_IFU_TG_ID = ?");
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
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String tgId = req.getParameter("guideId");
		
		StringBuilder sql = new StringBuilder();
		
		sql.append("DELETE ").append(customDb).append("DEPUY_IFU_TG WHERE DEPUY_IFU_TG_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, tgId);
			
			if (ps.executeUpdate() < 1)
				log.warn("No Technique Guides deleted with id: " + tgId);
		} catch (SQLException e) {
			log.error("Could not delete Technique Guide with id: " + tgId, e);
		}
	}

	public void update(SMTServletRequest req) throws ActionException {
		this.update(new IFUTechniqueGuideVO(req));
	}
	
	public void update(IFUTechniqueGuideVO vo ) throws ActionException {
		boolean isInsert = false;
		if (StringUtil.checkVal(vo.getTgId()).length() == 0) {
			isInsert = true;
			vo.setTgId(new UUIDGenerator().getUUID());
		}
		
		String sql = buildUpdateSql(isInsert);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, vo.getTgNM());
			ps.setString(i++, vo.getUrlTxt());
			ps.setString(i++, vo.getDpySynMediaBinId());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getTgId());
			
			if (ps.executeUpdate() < 1)
				log.warn("No Technique Guides updated for id: " + vo.getTgId());
		} catch (SQLException e) {
			log.error("Unable to update Technique Guide with id: " + vo.getTgId(), e);
		}
	}
	
	private String buildUpdateSql(boolean isInsert) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		if (isInsert) {
			sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_TG (");
			sql.append("TG_NM, URL_TXT, DPY_SYN_MEDIABIN_ID, CREATE_DT, DEPUY_IFU_TG_ID) ");
			sql.append("VALUES(?,?,?,?,?");
		} else {
			sql.append("UPDATE ").append(customDb).append("DEPUY_IFU_TG SET ");
			sql.append("TG_NM = ?, URL_TXT = ?, DPY_SYN_MEDIABIN_ID = ?, UPDATE_DT = ? ");
			sql.append("WHERE DEPUY_IFU_TG_ID = ? ");
		}
		return sql.toString();
	}
}
