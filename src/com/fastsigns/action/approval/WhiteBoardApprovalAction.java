package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.WhiteBoardLogVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: WhiteBoardApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class performs all white board specific approval 
 * actions regarding submission, approval and denial requests.
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
public class WhiteBoardApprovalAction extends ApprovalTemplateAction {

	@Override
	public void approveRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning White Board Approval Process...");
		StringBuilder s = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("white_board_text = new_white_board_text, new_white_board_text = ?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			for(AbstractChangeLogVO vo : vos){
				ps.setNull(1, java.sql.Types.VARCHAR);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getComponentId());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.error(e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		updateStatus(vos, AbstractChangeLogVO.Status.APPROVED.ordinal());
		logger.logChange(req, vos);
	}

	@Override
	public void denyRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning White Board Delete Process...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("new_white_board_text = ?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			for(AbstractChangeLogVO vo : vos){
				ps.setNull(1, java.sql.Types.VARCHAR);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getComponentId());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.error(e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		updateStatus(vos, AbstractChangeLogVO.Status.DENIED.ordinal());
		logger.logChange(req, vos);
	}

	@Override
	public void submitRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) {
		//Not Used
	}

	@Override
	public String getHFriendlyType() {
		return WhiteBoardLogVO.FRIENDLY_NAME;
	}

	@Override
	public String getDbTypeId() {
		return WhiteBoardLogVO.TYPE_ID;
	}

}
