package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.AbstractApprover;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.constants.Constants;

public class IFUApprover extends AbstractApprover{
	
	public IFUApprover(SMTDBConnection conn, Map<String, Object> attributes) {
		super(conn, attributes);
	}


	@SuppressWarnings("incomplete-switch")
	@Override
	public void approve(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String archive = "UPDATE " + customDb + "DEPUY_IFU SET ARCHIVE_FLG = 1 WHERE DEPUY_IFU_ID = ?";
		String activate = "UPDATE " + customDb + "DEPUY_IFU SET DEPUY_IFU_GROUP_ID = null WHERE DEPUY_IFU_ID = ?";
		String delete = "DELETE " + customDb + "DEPUY_IFU WHERE DEPUY_IFU_ID = ?";
		
		for (ApprovalVO vo : items) {			
			try {
				switch (vo.getSyncStatus()) {
					case DELETE:
						executeQuery(delete, vo.getWcKeyId());
						break;
	
					case APPROVED:
						// Check if the new item is a new version of the old item
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
						
				}

				vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
				
			} catch (DatabaseException e) {
				log.error("Unable to to execute query for approval vo with status " + vo.getSyncStatus() + 
						", key id " + vo.getWcKeyId() + ", and orig id " + vo.getOrigWcKeyId());
				vo.setSyncStatus(SyncStatus.PENDING);
				throw new ApprovalException(e);
			}
		}
	}

	
	/**
	 * Check if this is a new version of the old document
	 * @param vo
	 * @return
	 * @throws ApprovalException
	 */
	private boolean isNewVersion(ApprovalVO vo) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String inProg = "";
		String active = "";
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ").append(customDb).append("DEPUY_IFU ");
		sql.append("WHERE DEPUY_IFU_ID in (?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, vo.getWcKeyId());
			ps.setString(2, vo.getOrigWcKeyId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				if (rs.getString("DEPUY_IFU_ID").equals(vo.getWcKeyId())) {
					inProg = rs.getString("VERSION_TXT");
				} else {
					active = rs.getString("VERSION_TXT");
				}
			}
			
		} catch (SQLException e) {
			log.error("Unable to get information of DEPUY IFUs " + vo.getWcKeyId() + " and  " + vo.getOrigWcKeyId(), e);
			throw new ApprovalException(e);
		}
		
		if (inProg.equals(active)) {
			return false;
		} else {
			return true;
		}
	}

	
	public List<ApprovalVO> list(SyncStatus status, ApprovalVO... items)
			throws ApprovalException {
		
		// If they've told us what they want just give it to them
		if (items.length > 0) return Arrays.asList(items);
		
		List<ApprovalVO> appItems = new ArrayList<>();

		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(80);
		
		sql.append("SELECT * FROM ").append(customDb).append("DEPUY_IFU WHERE PENDING_SYNC_FLG = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, status.intValue());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				ApprovalVO app = new ApprovalVO();
				app.setWcKeyId(rs.getString("DEPUY_IFU_ID"));
				app.setOrigWcKeyId(rs.getString("DEPUY_IFU_GROUP_ID"));
				appItems.add(app);
			}
		} catch (SQLException e) {
			log.error("Unable to get list of IFUs for approval status: " + status.toString(), e);
			throw new ApprovalException(e);
		}
		
		return appItems;
	}

	
	@Override
	public void reject(ApprovalVO... items) throws ApprovalException {
		for(ApprovalVO vo : items) {
			vo.setSyncStatus(SyncStatus.INPROGRESS);
		}
		
		super.cancel(items);
	}
	
	
	public void cancel(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String delete = "DELETE " + customDb + "DEPUY_IFU SET WHERE DEPUY_IFU_ID = ?";
		
		for (ApprovalVO vo : items) {
			try {
				executeQuery(delete, vo.getWcKeyId());
				vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
			} catch (DatabaseException e) {
				log.error("Undable to cancel approval with status " + vo.getSyncStatus() + " and id " + vo.getWcKeyId());
				vo.setSyncStatus(SyncStatus.PENDING);
			}
		}
	}


	@Override
	public List<ApprovalVO> list(SyncStatus status) throws ApprovalException {
		List<ApprovalVO> appItems = new ArrayList<>();

		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(80);
		
		sql.append("SELECT * FROM ").append(customDb).append("DEPUY_IFU di ");
		sql.append("left join WC_SYNC ws on ws.WC_KEY_ID = di.DEPUY_IFU_ID ");
		sql.append("WHERE WC_SYNC_STATUS_CD = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, status.toString());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				ApprovalVO app = new ApprovalVO(rs);
				appItems.add(app);
			}
		} catch (SQLException e) {
			log.error("Unable to get list of IFUs for approval status: " + status.toString(), e);
			throw new ApprovalException(e);
		}
		
		return appItems;
	}
	
	
}
