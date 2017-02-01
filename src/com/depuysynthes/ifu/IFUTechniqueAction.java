package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
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
	
	/**
	 * Determine if we are getting all the technique guides or just one
	 */
	public void list(ActionRequest req) throws ActionException {
		if (req.hasParameter("tgId") || req.hasParameter("add")) {
			getSingleTechniqueGuide(req);
		} else {
			getAllTechniqueGuides(req);
		}
	}
	
	/**
	 * Get a single technique guide along with its parent document and the name
	 * of its mediabin file, if any.
	 * @param req
	 * @throws ActionException
	 */
	private void getSingleTechniqueGuide(ActionRequest req) {
		String tgId = req.getParameter("tgId");
		log.debug("Getting single technique guide with id: " + tgId);
		
		String sql = buildSingleGuideSql();
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
	
	/**
	 * Build the sql for getting a single technique guide
	 * @return
	 */
	private String buildSingleGuideSql() {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(90);
		
		sql.append("SELECT *, dit.DPY_SYN_MEDIABIN_ID as tg_mediabin_id, dit.URL_TXT as tg_url FROM ");
		sql.append(customDb).append("DEPUY_IFU_TG dit ");
		sql.append("left join ").append(customDb).append("DEPUY_IFU_TG_XR ditx on ");
		sql.append("dit.DEPUY_IFU_TG_ID = ditx.DEPUY_IFU_TG_ID ");
		sql.append("left join ").append(customDb).append("DPY_SYN_MEDIABIN dsm on ");
		sql.append("dsm.DPY_SYN_MEDIABIN_ID = dit.DPY_SYN_MEDIABIN_ID WHERE dit.DEPUY_IFU_TG_ID = ?");
		
		return sql.toString();
	}
	
	/**
	 * Get all the technique guides for the current IFU document instance.
	 * @param req
	 */
	private void getAllTechniqueGuides(ActionRequest req) {
		String instanceId = req.getParameter("documentId");
		log.debug("Getting all technique guides for document instance: " + instanceId);
		String sql = buildListSql();
		List<IFUTechniqueGuideVO> data = new ArrayList<IFUTechniqueGuideVO>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, instanceId);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(new IFUTechniqueGuideVO(rs));
			}
		} catch (SQLException e) {
			log.error("Could not get technique guides for document with id: " + instanceId, e);
		} 
	}
	
	/**
	 * Build the sql query for the
	 * @return
	 */
	private String buildListSql() {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT dit.* FROM ").append(customDb).append("DEPUY_IFU_IMPL dii ");
		sql.append("left join ").append(customDb).append("DEPUY_IFU_TG_XR ditx on ");
		sql.append("ditx.DEPUY_IFU_IMPL_ID = dii.DEPUY_IFU_IMPL_ID ");
		sql.append("left join ").append(customDb).append("DEPUY_IFU_TG dit on ");
		sql.append("dit.DEPUY_IFU_TG_ID = ditx.DEPUY_IFU_TG_ID ");
		sql.append("WHERE DEPUY_IFU_IMPL_ID = ?");
		
		return sql.toString();
	}
	
	/**
	 * Delete the supplied technique guide
	 */
	public void delete(ActionRequest req) throws ActionException {
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

		super.adminRedirect(req, msg, buildRedirect(req));
	}

	/**
	 * Create a technique guide vo from the request object and pass it along
	 * to a vo based update method then redirect the user to the technique guide's
	 * parent IFU document instance
	 */
	public void update(ActionRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			this.update(new IFUTechniqueGuideVO(req));
		} catch (ActionException e) {
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
			throw e;
		} finally {
			super.adminRedirect(req, msg, buildRedirect(req));
		}
	}
	
	/**
	 * Update method based around an IFUTechniqueGuideVO instead of the
	 * request object.
	 * @param vo
	 * @throws ActionException
	 */
	public void update(IFUTechniqueGuideVO vo ) throws ActionException {
		boolean isInsert = (StringUtil.checkVal(vo.getTgId()).length() == 0);
		if (isInsert)
			vo.setTgId(new UUIDGenerator().getUUID());
		
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
			
			updateXR(vo, isInsert);
			
		} catch (SQLException e) {
			log.error("Unable to update Technique Guide with id: " + vo.getTgId(), e);
		}
	}
	
	/**
	 * Set up the XR record for the new technique guide.
	 * @param vo
	 * @throws SQLException
	 */
	private void updateXR(IFUTechniqueGuideVO vo, boolean isInsert) throws SQLException {
		String sql = buildXRInsert(isInsert);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 1;
			ps.setInt(i++, vo.getOrderNo());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getTgId());
			ps.setString(i++, vo.getImplId());
			
			if (ps.executeUpdate() < 1) 
				log.warn("Unable to insert xr record for instance " + vo.getImplId() + " and Technique Guide " + vo.getTgId());
		}
	}
	
	private String buildXRInsert(boolean isInsert) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		
		if (isInsert) {
			sql.append("INSERT INTO ").append(customDb).append("DEPUY_IFU_TG_XR (");
			sql.append("ORDER_NO, CREATE_DT, DEPUY_IFU_TG_ID, DEPUY_IFU_IMPL_ID) ");
			sql.append("VALUES(?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(customDb).append("DEPUY_IFU_TG_XR SET ");
			sql.append("ORDER_NO=?, CREATE_DT=? WHERE DEPUY_IFU_TG_ID=? AND DEPUY_IFU_IMPL_ID=? ");
		}
		
		return sql.toString();
	}

	/**
	 * Build the update or insert query for the update methods
	 * @param isInsert
	 * @return
	 */
	private String buildUpdateSql(boolean isInsert) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
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
	
	/**
	 * Build up the redirect url with extra parameters to make sure that
	 * we are sent back to the IFU document instance that this technique guide
	 * belongs to.
	 * @param req
	 * @return
	 */
	private String buildRedirect(ActionRequest req) {
		StringBuilder redirect = new StringBuilder(100);
		redirect.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		redirect.append("?facadeType=instance");
		redirect.append("&implId=").append(req.getParameter("implId"));
		
		return redirect.toString();
	}
}
