package com.depuysynthes.emea.leihsets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.AbstractApprover;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LeihsetApprover.java<p/>
 * <b>Description: Handles all tasks related to approving/rejecting/canceling 
 * changes to Leihsets.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 1, 2015
 ****************************************************************************/

public class LeihsetApprover extends AbstractApprover {
	
	public LeihsetApprover(SMTDBConnection conn, Map<String, Object> attributes) {
		super(conn, attributes);
	}


	/**
	 * Approve all items that have been passed to the this approved
	 */
	@SuppressWarnings("incomplete-switch")
	@Override
	public void approve(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String archive = "UPDATE " + customDb + "DPY_SYN_LEIHSET SET ARCHIVE_FLG = 1 WHERE LEIHSET_ID = ?";
		String activate = "UPDATE " + customDb + "DPY_SYN_LEIHSET SET LEIHSET_GROUP_ID = null WHERE LEIHSET_ID = ?";
		String delete = "DELETE FROM " + customDb + "DPY_SYN_LEIHSET WHERE LEIHSET_ID = ?";
		
		for (ApprovalVO vo : items) {
			SyncStatus store = vo.getSyncStatus();
			try {
				switch (vo.getSyncStatus()) {
					case PendingCreate:
						executeQuery(activate, vo.getWcKeyId());
						break;
						
					case PendingDelete:
						executeQuery(delete, vo.getWcKeyId());
						break;
	
					case PendingUpdate:
						// Check if the new item is a different version that the old item						
						if (vo.getOrigWcKeyId() != null) {
							// Since we are dealing with a new version of an old document we archive the old 
							// document instead of just deleting it.
							executeQuery(archive, vo.getOrigWcKeyId());
						}
						
						executeQuery(activate, vo.getWcKeyId());
						break;
						
				}

				vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
				vo.setSyncStatus(SyncStatus.Approved);
				
			} catch (DatabaseException e) {
				log.error("Unable to execute query for approval vo with status " + vo.getSyncStatus() + 
						", key id " + vo.getWcKeyId() + ", and orig id " + vo.getOrigWcKeyId());
				vo.setSyncStatus(store);
				throw new ApprovalException(e);
			}
		}
	}
	
	
	/**
	 * Delete the in progress item
	 */
	public void cancel(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String delete = "DELETE FROM " + customDb + "DPY_SYN_LEIHSET WHERE LEIHSET_ID = ?";
		
		for (ApprovalVO vo : items) {
			try {
				// If we are not rejecting a delete, delete the associated IFU record.
				if (vo.getSyncStatus() != SyncStatus.PendingDelete)
					executeQuery(delete, vo.getWcKeyId());
				
				vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
				vo.setSyncStatus(SyncStatus.Declined);
				vo.setRejectCode("Cancelled");
				vo.setRejectReason("Cancelled");
				
				
			} catch (DatabaseException e) {
				log.error("Unable to cancel approval with status " + vo.getSyncStatus() + " and id " + vo.getWcKeyId());
				vo.setSyncStatus(SyncStatus.InProgress);
				throw new ApprovalException(e);
			}
		}
	}


	/**
	 * List all ifu documents with the supplied status code
	 */
	@Override
	public List<ApprovalVO> list(SyncStatus status) throws ApprovalException {
		List<ApprovalVO> appItems = new ArrayList<>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(80);
		sql.append("SELECT l.leihset_nm as PORTLET_NM, ws.*, FIRST_NM as admin_first, ");
		sql.append("LAST_NM as admin_last FROM ");
		sql.append(customDb).append("DPY_SYN_LEIHSET l ");
		sql.append("left join WC_SYNC ws on ws.WC_KEY_ID = l.LEIHSET_ID ");
		sql.append("left join PROFILE p on p.PROFILE_ID = ws.ADMIN_PROFILE_ID ");
		sql.append("where ws.module_type_id=? ");
		if (status != null) {
			sql.append("and WC_SYNC_STATUS_CD = ?");
		} else {
			sql.append("and WC_SYNC_STATUS_CD in (?,?,?)");
		}
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ModuleType.EMEALeihset.name());
			if (status != null) {
				ps.setString(2, status.name());
			} else {
				ps.setString(2, SyncStatus.PendingCreate.name());
				ps.setString(3, SyncStatus.PendingDelete.name());
				ps.setString(4, SyncStatus.PendingUpdate.name());
			}
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next())
				appItems.add(new ApprovalVO(rs, (String) getAttribute(Constants.ENCRYPT_KEY)));
			
		} catch (SQLException e) {
			log.error("Unable to get list of IFUs for approval status: " + status.toString(), e);
			throw new ApprovalException(e);
		}
		return appItems;
	}
}