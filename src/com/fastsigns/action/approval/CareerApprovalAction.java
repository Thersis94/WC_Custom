package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.CareerLogVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: CareerApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class performs all career specific approval actions
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
public class CareerApprovalAction extends ApprovalTemplateAction {

	public CareerApprovalAction() {
	}

	public CareerApprovalAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	/**
	 * Approves the Center Image and updates Changelog
	 * @param req
	 * @throws SQLException
	 */
	@Override
	public void approveRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning Job Post Approval Process...");
		StringBuilder s = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("update ").append(customDb).append("FTS_JOB_POSTING set ");
		s.append("JOB_APPROVAL_FLG = ?, update_dt = ? where JOB_POSTING_ID = ? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			for(AbstractChangeLogVO v : vos){
				ps.setInt(1, AbstractChangeLogVO.Status.APPROVED.ordinal());
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, v.getComponentId());
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
	/**
	 * Removes the new_center_image_url from a franchise in event of denial.
	 * @param req
	 * @throws SQLException
	 */
	@Override
	public void denyRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning Job Post Deny Process...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("FTS_JOB_POSTING set ");
		s.append("JOB_APPROVAL_FLG = ?, ");
		s.append("update_dt = ? where JOB_POSTING_ID = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			for(AbstractChangeLogVO v : vos){
				ps.setInt(1, -1);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, v.getComponentId());
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

	/**
	 * Handles updating the FTS_JOB_POSTING Table to be in a state of Pending Approval. 
	 */
	@Override
	public void submitRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) {
		log.debug("Beginning Job Post Approval Process...");
		StringBuilder s = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("update ").append(customDb).append("FTS_JOB_POSTING set ");
		s.append("JOB_APPROVAL_FLG = ?, update_dt = ? where JOB_POSTING_ID = ? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			for(AbstractChangeLogVO v : vos){
				ps.setInt(1, AbstractChangeLogVO.Status.PENDING.ordinal());
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, v.getComponentId());
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
		updateStatus(vos, AbstractChangeLogVO.Status.PENDING.ordinal());
		logger.logChange(req, vos);	
	}

	@Override
	public String getHFriendlyType() {
		return CareerLogVO.FRIENDLY_NAME;
	}

	@Override
	public String getDbTypeId() {
		return CareerLogVO.TYPE_ID;
	}

}
