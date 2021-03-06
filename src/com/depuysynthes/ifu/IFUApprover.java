package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.AbstractApprover;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> IFUApprover.java
 * <b>Description:</b> Handles all tasks related to approving/rejecting/canceling
 * changes to IFU documents.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Eric Damschroder
 * @version 3.0
 * @since Mar 18, 2017
 ****************************************************************************/

public class IFUApprover extends AbstractApprover {
	
	public IFUApprover(SMTDBConnection conn, Map<String, Object> attributes) {
		super(conn, attributes);
	}


	/**
	 * Approve all items that have been passed to the this approved
	 */
	@SuppressWarnings("incomplete-switch")
	@Override
	public void approve(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String archive = "UPDATE " + customDb + "DEPUY_IFU SET ARCHIVE_FLG = 1 WHERE DEPUY_IFU_ID = ?";
		String activate = "UPDATE " + customDb + "DEPUY_IFU SET DEPUY_IFU_GROUP_ID = null WHERE DEPUY_IFU_ID = ?";
		String delete = "DELETE FROM " + customDb + "DEPUY_IFU WHERE DEPUY_IFU_ID = ?";
		
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
						boolean newVersion = vo.getOrigWcKeyId() == null? false : isNewVersion(vo);
						
						if (newVersion) {
							// Since we are dealing with a new version of an old document we archive the old 
							// document instead of just deleting it.
							executeQuery(archive, vo.getOrigWcKeyId());
							executeQuery(activate, vo.getWcKeyId());
						} else {
							if (vo.getOrigWcKeyId() != null) {
								executeQuery(delete, vo.getOrigWcKeyId());
							}
							
							executeQuery(activate, vo.getWcKeyId());
						}
						break;
					default:
						//do nothing
				}

				vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
				vo.setSyncStatus(SyncStatus.Approved);
				
			} catch (DatabaseException e) {
				log.error("Unable to to execute query for approval vo with status " + vo.getSyncStatus() + 
						", key id " + vo.getWcKeyId() + ", and orig id " + vo.getOrigWcKeyId());
				vo.setSyncStatus(store);
				throw new ApprovalException(e);
			}
		}
	}

	
	/**
	 * Check if this is a new version of the old document.  If the version text
	 * is different between the original document and the new one we will need 
	 * to archive the old document instead of deleting it.
	 * @param vo
	 * @return
	 * @throws ApprovalException
	 */
	private boolean isNewVersion(ApprovalVO vo) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String newVer = "";
		String oldVer = "";
		
		StringBuilder sql = new StringBuilder(50);
		sql.append("SELECT DEPUY_IFU_ID, VERSION_TXT FROM ").append(customDb).append("DEPUY_IFU ");
		sql.append("WHERE DEPUY_IFU_ID in (?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, vo.getWcKeyId());
			ps.setString(2, vo.getOrigWcKeyId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				if (rs.getString("DEPUY_IFU_ID").equals(vo.getWcKeyId())) {
					newVer = rs.getString("VERSION_TXT");
				} else {
					oldVer = rs.getString("VERSION_TXT");
				}
			}
			
		} catch (SQLException e) {
			log.error("Unable to get information of DEPUY IFUs " + vo.getWcKeyId() + " and  " + vo.getOrigWcKeyId(), e);
			throw new ApprovalException(e);
		}
		
		return !oldVer.equalsIgnoreCase(newVer);
	}
	
	
	/**
	 * Delete the in progress item
	 */
	@Override
	public void cancel(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String delete = "DELETE FROM " + customDb + "DEPUY_IFU WHERE DEPUY_IFU_ID = ?";
		
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
		StringBuilder sql = new StringBuilder(450);
		sql.append("SELECT di.title_txt as PORTLET_NM, ws.*, FIRST_NM as admin_first, ");
		sql.append("LAST_NM as admin_last FROM ");
		sql.append(customDb).append("DEPUY_IFU di ");
		sql.append("left join WC_SYNC ws on ws.WC_KEY_ID = di.DEPUY_IFU_ID ");
		sql.append("left join PROFILE p on p.PROFILE_ID = ws.ADMIN_PROFILE_ID ");
		sql.append("where ws.module_type_id=? ");
		if (status != null) {
			sql.append("and WC_SYNC_STATUS_CD = ?");
		} else {
			sql.append("and WC_SYNC_STATUS_CD in (?,?,?)");
		}
		log.debug(sql);

		StringEncrypter se = StringEncrypter.getInstance((String) getAttribute(Constants.ENCRYPT_KEY));
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ModuleType.DePuyIFU.name());
			if (status != null) {
				ps.setString(2, status.name());
			} else {
				ps.setString(2, SyncStatus.PendingCreate.name());
				ps.setString(3, SyncStatus.PendingDelete.name());
				ps.setString(4, SyncStatus.PendingUpdate.name());
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				appItems.add(new ApprovalVO(rs, se));
			
		} catch (SQLException e) {
			log.error("Unable to get list of IFUs for approval status: " + status, e);
			throw new ApprovalException(e);
		}
		return appItems;
	}
}