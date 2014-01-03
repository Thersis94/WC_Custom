package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ModuleLogVO;
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: ModuleApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class performs all module specific approval actions
 * regarding submission, approval and denial requests.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Oct 9, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class ModuleApprovalAction extends ApprovalTemplateAction {

	/**
	 * handles approval of new Module Options
	 * The live module gets updated with the data from the approved version,
	 * then the interim versions get purged from the system
	 * select approved record -> update into live record -> delete temp records/revisions -> update Changelog
	 * @param req
	 * @throws SQLException
	 */
	@Override
	public void approveRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning Approve Module Option Process...");
		StringBuilder sb = null;
		PreparedStatement ps = null;
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		for(AbstractChangeLogVO v : vos) {
			int modOptId = Convert.formatInteger(v.getComponentId());
			CenterModuleOptionVO vo = getModuleOption(modOptId, req);
			
			// update new attributes with old values (in case values have changes)
			Map<Integer, String> vals = getAttrVals(vo.getParentId(), req);
			
			if(vals.size() > 0){
				//remove old attributes (ensure clean transfer of new attributes)
				deleteAttributes(vo.getParentId(), req);
			
				// associate new Attributes (update cp_module_option_Id = parent cp_module_option_id) 
				updateAttributes(modOptId, req, vals, vo);
			}
			//update the existing module with the data from the new/approved revision
			sb = new StringBuilder();
			sb.append("update ").append(customDb);
			sb.append("FTS_CP_MODULE_OPTION set OPTION_NM=?, ");
			sb.append("OPTION_DESC=?, ARTICLE_TXT=?, RANK_NO=?, LINK_URL=?, FILE_PATH_URL=?, ");
			sb.append("THUMB_PATH_URL=?, VIDEO_STILLFRAME_URL=?, CONTENT_PATH_TXT=?, START_DT=?, END_DT=?, ");
			sb.append("CREATE_DT=?, RESPONSE_TXT=?, FTS_CP_MODULE_ACTION_ID=?, APPROVAL_FLG=?, FRANCHISE_ID=?, PARENT_ID=? ");
			sb.append("where CP_MODULE_OPTION_ID=?");
			
			int i = 0;
			try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(++i, vo.getOptionName());
				ps.setString(++i, vo.getOptionDesc());
				ps.setString(++i, vo.getArticleText());
				ps.setInt(++i, vo.getRankNo());
				ps.setString(++i, vo.getLinkUrl());
				ps.setString(++i, vo.getFilePath());
				ps.setString(++i, vo.getThumbPath());
				ps.setString(++i, vo.getStillFramePath());
				ps.setString(++i, vo.getContentPath());
				ps.setDate(++i, Convert.formatSQLDate(vo.getStartDate()));
				ps.setDate(++i, Convert.formatSQLDate(vo.getEndDate()));
				ps.setTimestamp(++i, Convert.getCurrentTimestamp());
				ps.setString(++i, vo.getResponseText());
				ps.setString(++i, vo.getActionId());
				ps.setInt(++i, 1); //approval_flg=1 means approved
				if (vo.getFranchiseId() == null || vo.getFranchiseId() == 0) {
					ps.setNull(++i, java.sql.Types.INTEGER);
				} else {
					ps.setInt(++i, vo.getFranchiseId());
				}
				ps.setNull(++i, java.sql.Types.INTEGER);
				if (Convert.formatInteger(vo.getParentId()) > 0) {
					ps.setInt(++i, vo.getParentId());
				} else {
					ps.setInt(++i, vo.getModuleOptionId());
				}
				ps.executeUpdate();
			} catch (SQLException e) {
				log.error(e);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}		
			//delete all interim revision data related to this update
			if (Convert.formatInteger(vo.getParentId()) > 0) {
				sb = new StringBuilder();
				sb.append("delete from ").append(customDb);
				sb.append("FTS_CP_MODULE_OPTION where parent_id=?");
				try {
					ps = dbConn.prepareStatement(sb.toString());
					ps.setInt(1, vo.getParentId());
					int x = ps.executeUpdate();
					log.debug("purged " + x + " modules");
	
				} catch (SQLException e) {
					log.error(e);
				} finally {
					try { ps.close(); } catch (Exception e) {}
				}
			}
			v.setComponentId(StringUtil.checkVal(vo.getParentId()));
			v.setStatusNo(AbstractChangeLogVO.Status.APPROVED.ordinal());
		}
		logger.logChange(req, vos);
	}
	/**
	 * Removes all children modules from a given parent and updates changelog.
	 * @param req
	 * @throws SQLException
	 */
	@Override
	public void denyRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		for(AbstractChangeLogVO vo : vos){
			Integer optionId = Convert.formatInteger(vo.getComponentId());
			StringBuilder sb = new StringBuilder();
			sb.append("select PARENT_ID from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("fts_cp_module_option where cp_module_option_id=? ");
			sb.append("or parent_id=?");
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setInt(1, optionId);
				ps.setInt(2, optionId);
				ResultSet rs = ps.executeQuery();
				if(rs.next()){
					vo.setComponentId(rs.getString("parent_id"));
					vo.setStatusNo(AbstractChangeLogVO.Status.DENIED.ordinal());
				}
				
			} catch (SQLException e) {
				log.debug(e);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
			
			sb = new StringBuilder();
			sb.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("fts_cp_module_option where cp_module_option_id=? ");
			sb.append("or parent_id=?");
			
			ps = null;
			try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setInt(1, optionId);
				ps.setInt(2, optionId);
				ps.executeUpdate();
				
			} catch (SQLException e) {
				log.error(e);
				
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
		}
		logger.logChange(req, vos);
	}
	/**
	 * simple wrapper to change the approval flag for a module_option.
	 * This tags the record to be visible on the Approval screen for 
	 * administrators to review and approve and updates Changelog
	 * @param req
	 * @throws SQLException
	 */
	@Override
	public void submitRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning Request Module Option Approval Process...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION ");
		sb.append("set APPROVAL_FLG=? where CP_MODULE_OPTION_ID=?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (AbstractChangeLogVO vo : vos) {
				ps.setInt(1, 100);
				ps.setInt(2, Convert.formatInteger(vo.getComponentId()));
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.error(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//TODO Need to do a map like lookup to batch sql call.
		for(AbstractChangeLogVO vo : vos){
			sb = new StringBuilder();
			sb.append("select parent_id from ").append(customDb);
			sb.append("FTS_CP_MODULE_OPTION where CP_MODULE_OPTION_ID = ?");
			try{
				ps=dbConn.prepareStatement(sb.toString());
				ps.setString(1, vo.getComponentId());
				ResultSet rs = ps.executeQuery();
				if(rs.next()){
					vo.setComponentId(rs.getString("parent_id"));
				}
			} catch (SQLException e) {
				log.error(e);
			} finally {
				try {ps.close(); } catch(Exception e){}
			}
		}
		logger.logChange(req, vos);
	}
	
	private CenterModuleOptionVO getModuleOption(int modOptId, SMTServletRequest req) throws ActionException{
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("select cp_module_option_id as 'mod_opt_id', create_dt as 'option_create_dt', * from ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION where cp_module_option_id=? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, modOptId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return new CenterModuleOptionVO(rs);
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException();
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return null;
	}
	
	private Map<Integer, String> getAttrVals(int modOptId, SMTServletRequest req){
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> vals = new HashMap<Integer, String>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb = new StringBuilder();
		sb.append("select CP_OPTION_ATTR_ID, ATTRIB_VALUE_TXT from ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("where CP_MODULE_OPTION_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, modOptId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()){
				vals.put(rs.getInt("CP_OPTION_ATTR_ID"),rs.getString("ATTRIB_VALUE_TXT"));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return vals;
	}
	
	private void deleteAttributes(int modOptId, SMTServletRequest req){
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb = new StringBuilder();
		sb.append("delete from ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("where CP_MODULE_OPTION_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, modOptId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	private void updateAttributes(int modOptId, SMTServletRequest req, Map<Integer, String> vals, CenterModuleOptionVO vo){
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("update ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("set CP_MODULE_OPTION_ID = ?, ATTRIB_VALUE_TXT = ? ");
		sb.append("where CP_MODULE_OPTION_ID = ? and ATTR_PARENT_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for(int key : vals.keySet()){
				log.debug("sql = " + sb + " | " + vo.getParentId() + " | " + vals.get(key) + " | " + modOptId + " | " + key);
			if (vo.getParentId() > 0) {
				ps.setInt(1, vo.getParentId());
			} else {
				ps.setInt(1, vo.getModuleOptionId());
			}	
			ps.setString(2, vals.get(key));
			ps.setInt(3, modOptId);
			ps.setInt(4, key);
			ps.addBatch();
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		sb = new StringBuilder();
		sb.append("update ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("set CP_MODULE_OPTION_ID = ? ");
		sb.append("where CP_MODULE_OPTION_ID = ? ");
		try{
			ps = dbConn.prepareStatement(sb.toString());
			if (vo.getParentId() > 0) {
				ps.setInt(1, vo.getParentId());
			} else {
				ps.setInt(1, vo.getModuleOptionId());
			}	
			ps.setInt(2, modOptId);
			ps.executeUpdate();

		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}

	@Override
	public String getHFriendlyType() {
		return ModuleLogVO.FRIENDLY_NAME;
	}

	@Override
	public String getDbTypeId() {
		return ModuleLogVO.TYPE_ID;
	}

}
