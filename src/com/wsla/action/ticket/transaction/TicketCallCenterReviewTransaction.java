package com.wsla.action.ticket.transaction;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.action.ticket.TicketOverviewAction;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;
import com.wsla.data.ticket.TicketAssignmentVO.ProductOwner;

/****************************************************************************
 * <b>Title</b>: TicketCallCenterReviewTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> handles a the transaction after the call cennter asked for more review
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Aug 15, 2019
 * @updates:
 ****************************************************************************/
public class TicketCallCenterReviewTransaction extends BaseTransactionAction {

	public static final String AJAX_KEY = "ccReview";
	
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public TicketCallCenterReviewTransaction() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.
	 * ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
	
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		TicketOverviewAction toa =  new TicketOverviewAction(getDBConnection(), getAttributes());
		UserDataVO profile = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		UserVO user = (UserVO)profile.getUserExtendedInfo();
		TicketAssetTransaction tat = new  TicketAssetTransaction();
		tat.setActionInit(actionInit);
		tat.setAttributes(getAttributes());
		tat.setDBConnection(getDBConnection());
		
		try {
			TicketVO tvo = tea.getCompleteTicket(req.getParameter("ticketId"));
			changeStatus(tvo.getTicketId(), user.getUserId(), StatusCode.USER_CALL_DATA_INCOMPLETE, null, null);
			
			ProductOwner owner = tat.getProductOwnerType(tvo.getTicketId());
			toa.checkTicketStatusOnSave(tvo, user, req, null);
			toa.postSaveStatusCheck(req, owner, tvo, tat, user);
			setModuleData(tvo);
			
		} catch (Exception e) {
			log.error("could not create restate ticket after call center review", e);
			putModuleData(tea, 0, false, e.getLocalizedMessage(), true);
		}
		
	}
	
}
