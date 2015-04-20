package com.fastsigns.action.approval;

import java.util.List;
import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.AbstractApprover;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalController.SyncTransaction;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CenterImageApprover.java<p/>
 * <b>Description: Center Image specific approval</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Apr 6, 2015
 ****************************************************************************/

public class CenterImageApprover extends AbstractApprover {

	public CenterImageApprover(SMTDBConnection conn,
			Map<String, Object> attributes) {
		super(conn, attributes);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void approve(ApprovalVO... approvables) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder update = new StringBuilder(315);
		update.append("UPDATE ").append(customDb).append("FTS_FRANCHISE ");
		update.append("SET CENTER_IMAGE_URL = NEW_CENTER_IMAGE_URL, ");
		update.append("CENTER_IMAGE_ALT_TXT = NEW_CENTER_IMAGE_ALT_TXT, ");
		update.append("NEW_CENTER_IMAGE_ALT_TXT = null, NEW_CENTER_IMAGE_URL = null ");
		update.append("WHERE FRANCHISE_ID = ?");
		for (ApprovalVO vo : approvables) {
			try {
				switch(vo.getSyncStatus()) {
				// Only updates go through approval.  Anything else does nothing.
				case PendingUpdate:
					executeQuery(update.toString(), vo.getWcKeyId().substring(0, vo.getWcKeyId().indexOf('_')));
					break;
				}
				
				vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
				vo.setSyncStatus(SyncStatus.Approved);

			} catch (DatabaseException e) {
				log.error("Unable to to execute query for approval vo with status " + vo.getSyncStatus() + 
						", key id " + vo.getWcKeyId() + ", and orig id " + vo.getOrigWcKeyId());
				throw new ApprovalException(e);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.approval.Approver#reject(com.smt.sitebuilder.approval.ApprovalVO[])
	 */
	@Override
	public void reject(ApprovalVO... items) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(175);
		sql.append("UPDATE ").append(customDb).append("FTS_FRANCHISE ");
		sql.append("SET NEW_CENTER_IMAGE_ALT_TXT = null, NEW_CENTER_IMAGE_URL = null ");
		sql.append("WHERE FRANCHISE_ID = ?");
		for (ApprovalVO vo : items) {
			try {
				executeQuery(sql.toString(), vo.getWcKeyId().substring(0, vo.getWcKeyId().indexOf('_')));
				vo.setSyncTransaction(SyncTransaction.Reject);
				vo.setSyncStatus(SyncStatus.Declined);
			} catch (DatabaseException e) {
				log.error("Unable to reject center image approval for center " + vo.getWcKeyId().substring(0, vo.getWcKeyId().indexOf('_')), e);
			}
		}
	}
	
	

	@Override
	public List<ApprovalVO> list(SyncStatus arg0) throws ApprovalException {
		return null;
	}

}
