/**
 *
 */
package com.biomed.smarttrak.util;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.changelog.ChangeLogIntfc;
import com.smt.sitebuilder.changelog.ChangeLogUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BiomedChangeLogUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Helper Util for Generating WC_Sync and ChangeLog Records
 * for Biomed records.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 3, 2017
 ****************************************************************************/
public class BiomedChangeLogUtil {

	private ChangeLogUtil clu;
	private SMTDBConnection dbConn;
	private Map<String, Object> attributes;

	public BiomedChangeLogUtil(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this.dbConn = dbConn;
		this.clu = new ChangeLogUtil(dbConn);
		this.attributes = attributes;
	}

	/**
	 * @param original
	 * @param diff
	 */
	public void createChangeLog(ActionRequest req, ChangeLogIntfc original, ChangeLogIntfc diff) {

		//The Diff is the update so create and approval record for it.
		String origId = getPrimaryByAnnotation(original);
		String diffId = getPrimaryByAnnotation(diff);

		ApprovalVO app = buildApprovalVO(req, diffId, origId);
		app.setItemName(diff.getItemName());
		app.setItemDesc(diff.getItemDesc());

		try {

			//User ApprovalController to save ApprovalVO.
			new ApprovalController(dbConn, attributes).process(app);

			//TODO - Build/Write ChangeLogVO.
			//clu.commitRecord(clv, false);
		} catch(ApprovalException ae) {
		}
	}

	/**
	 * Helper method that builds an ApprovalVO for the given Keys.
	 * @param req
	 * @param wcKeyId
	 * @param origKeyId
	 * @return
	 */
	public ApprovalVO buildApprovalVO(ActionRequest req, String wcKeyId, String origKeyId) {
		ApprovalVO vo = new ApprovalVO();
		UserDataVO user = (UserDataVO)req.getAttribute(Constants.USER_DATA);
		vo.setOrganizationId(((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId());
		vo.setCreatorId(user.getProfileId());
		vo.setSyncStatus(SyncStatus.Approved);
		vo.setApproveDt(new Date());
		vo.setCreatorName(user.getFullName());
		vo.setModuleType(ModuleType.Portlet);
		vo.setWcKeyId(wcKeyId);
		vo.setOrigWcKeyId(origKeyId);

		return vo;
	}

	/**
	 * Helper method that looks for a Column Annotation with isPrimaryKey set
	 * so that we can get the Primary Key Value.
	 * @param o
	 * @return
	 */
	public String getPrimaryByAnnotation(Object o) {
		for (Method m : o.getClass().getMethods()) {
			Column column = m.getAnnotation(Column.class);

			// If the column is the primary key, see if the value for it exists
			if (column != null && column.isPrimaryKey()) {
				try {
					Object data = m.invoke(o);
					return String.valueOf(data);
				} catch(Exception e) {}
			}
		}
		return null;
	}
}
