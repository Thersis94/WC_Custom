package com.fastsigns.action.approval;

import java.util.List;
import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.AbstractApprover;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CenterPageModuleApprover.java<p/>
 * <b>Description: Approval methods specific to center module assets</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Apr 6, 2015
 ****************************************************************************/

public class CenterPageModuleApprover extends AbstractApprover {

	public CenterPageModuleApprover(SMTDBConnection conn,
			Map<String, Object> attributes) {
		super(conn, attributes);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void approve(ApprovalVO... approvables) throws ApprovalException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder update = new StringBuilder(850);
		update.append("UPDATE mo ");
		update.append("SET mo.OPTION_NM = mo1.OPTION_NM, mo.CONTENT_PATH_TXT = mo1.CONTENT_PATH_TXT, mo.OPTION_DESC = mo1.OPTION_DESC, ");
		update.append("mo.ARTICLE_TXT = mo1.ARTICLE_TXT, mo.LINK_URL = mo1.LINK_URL, mo.FILE_PATH_URL = mo1.FILE_PATH_URL, mo.THUMB_PATH_URL = mo1.THUMB_PATH_URL, ");
		update.append("mo.VIDEO_STILLFRAME_URL = mo1.VIDEO_STILLFRAME_URL, mo.START_DT = mo1.START_DT, mo.END_DT = mo1.END_DT, ");
		update.append("mo.STANDARD_FLG = mo1.STANDARD_FLG, mo.FRANCHISE_ID = mo1.FRANCHISE_ID, mo.RANK_NO = mo1.RANK_NO, mo.RESPONSE_TXT = mo1.RESPONSE_TXT ");
		update.append("FROM ").append(customDb).append("FTS_CP_MODULE_OPTION mo INNER JOIN ").append(customDb).append("FTS_CP_MODULE_OPTION mo1 on mo.CP_MODULE_OPTION_ID = mo1.PARENT_ID ");
		update.append("WHERE mo.CP_MODULE_OPTION_ID = ? and mo1.CP_MODULE_OPTION_ID = ?");
		
		StringBuilder delete = new StringBuilder(110);
		
		delete.append("DELETE ").append(customDb).append("FTS_CP_MODULE_OPTION ");
		delete.append("WHERE CP_MODULE_OPTION_ID = ?");
		
		
		
		for (ApprovalVO vo : approvables) {
			try {
				switch(vo.getSyncStatus()) {
				// Only updates need to be changed for approval.  All else needs nothing
				case PendingUpdate:
					executeQuery(update.toString(), vo.getOrigWcKeyId(), vo.getWcKeyId());
					executeQuery(delete.toString(), vo.getWcKeyId());
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

	@Override
	public List<ApprovalVO> list(SyncStatus arg0) throws ApprovalException {
		return null;
	}

}
