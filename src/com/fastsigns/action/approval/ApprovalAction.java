package com.fastsigns.action.approval;

import java.util.List;
import java.util.Map;

import com.fastsigns.action.LogAction;
import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
/****************************************************************************
 * <b>Title</b>: ApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class lays out the framework for all Approval
 * Actions.  Requests come in through either approve, deny or submit and are 
 * handled on a per action basis performing any necessary changes to move an
 * item from one approval state to another. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Sept 20, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public abstract class ApprovalAction extends SBActionAdapter {

	LogAction logger = null;

	public ApprovalAction() {
		logger = getLogger();
	}

	public ApprovalAction(ActionInitVO actionInit) {
		this.actionInit = actionInit;
		logger = getLogger();
	}

	/**
	 * Return the logger used to record ChangeLogs
	 * @return
	 */
	protected LogAction getLogger() {
		LogAction l = new LogAction(actionInit);
		l.setAttributes(attributes);
		l.setDBConnection(dbConn);
		return l;
	}
	
	@Override
	public void setActionInit(ActionInitVO actionInit){
		super.setActionInit(actionInit);
		logger.setActionInit(actionInit);
	}

	@Override
	public void setDBConnection(SMTDBConnection dbConn){
		super.setDBConnection(dbConn);
		logger.setDBConnection(dbConn);
	}
	
	@Override
	public void setAttributes(Map<String, Object> attr){
		super.setAttributes(attr);
		logger.setAttributes(attr);
	}
	
	/**
	 * This method is used to perform the updates necessary for an approval.
	 * This is action/table specific and needs to be implemented on a per action
	 * basis.
	 * @param vos
	 * @param req
	 * @throws ActionException
	 */
	public abstract void approveRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req)
			throws ActionException;
	
	/**
	 * This method is used to perform the updates necessary for a denial.
	 * This is action/table specific and needs to be implemented on a per action
	 * basis.
	 * @param vos
	 * @param req
	 * @throws ActionException
	 */
	public abstract void denyRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req)
			throws ActionException;

	/**
	 * This method is used to perform the updates necessary for an approval request.
	 * This is action/table specific and needs to be implemented on a per action
	 * basis.
	 * @param vos
	 * @param req
	 * @throws ActionException
	 */
	public abstract void submitRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req)
			throws ActionException;

	/**
	 * This method handles sending out email notifications to the requesting and
	 * approving parties.
	 * @param vo
	 */
	public abstract void sendEmail(AbstractChangeLogVO vo);
	
	/**
	 * Used to notify the eteam when a request for approval is submitted in Webedit.
	 * @param vo
	 * @param site 
	 */
	public abstract void sendRequestNotificationEmail(AbstractChangeLogVO vo, String siteId);

	/**
	 * This method returns the Human Friendly version of the Action Type
	 * @return
	 */
	public abstract String getHFriendlyType();

	/**
	 * This method returns the db version of the FTS_CHANGELOG value for each
	 * Action Type.
	 * @return
	 */
	public abstract String getDbTypeId();
	
	/**
	 * Gives a mass update feature to update all ChangeLogVOs to the given status.
	 * @param vos
	 * @param status
	 */
	protected void updateStatus(List<AbstractChangeLogVO> vos, Integer status){
		for(AbstractChangeLogVO v : vos)
			v.setStatusNo(status);
	}
}
