package com.wsla.action.ticket;

import java.util.HashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.util.StringUtil;

//WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;
import com.wsla.action.ticket.transaction.DiagnosticTransaction;
import com.wsla.action.ticket.transaction.ProductSerialTransaction;
import com.wsla.action.ticket.transaction.ProviderLocationTransaction;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction;
// WSLA libs
import com.wsla.action.ticket.transaction.TicketAssetTransaction;
import com.wsla.action.ticket.transaction.TicketAssignmentTransaction;
import com.wsla.action.ticket.transaction.TicketCloneTransaction;
import com.wsla.action.ticket.transaction.TicketCommentTransaction;
import com.wsla.action.ticket.transaction.TicketDataTransaction;
import com.wsla.action.ticket.transaction.TicketPDFCreator;
import com.wsla.action.ticket.transaction.TicketPartsTransaction;
import com.wsla.action.ticket.transaction.TicketRepairTransaction;
import com.wsla.action.ticket.transaction.TicketUtilityTransaction;
import com.wsla.action.ticket.transaction.UserTransaction;
import com.wsla.action.ticket.transaction.creditMemoPDFCreator;
import com.wsla.action.ticket.transaction.TicketScheduleTransaction;
import com.wsla.action.ticket.transaction.TicketSearchTransaction;
import com.wsla.action.ticket.transaction.TicketTransaction;

/****************************************************************************
 * <b>Title</b>: TicketTransactionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the small transaction updates to the service order
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 22, 2018
 * @updates:
 ****************************************************************************/

public class TicketTransactionAction extends FacadeActionAdapter {
	/**
	 * Key for the AMID Registration to utilize when calling this class
	 */
	public static final String AMID_KEY = "ticketTransaction";
	
	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "transactionType";
	
	/**
	 * 
	 */
	public TicketTransactionAction() {
		super();
		assignActionMap();
	}

	/**
	 * @param actionInit
	 */
	public TicketTransactionAction(ActionInitVO actionInit) {
		super(actionInit);
		assignActionMap();
	}
	
	/**
	 * Assigns the keys and classes for the facade
	 */
	public void assignActionMap() {
		actionMap.put(TicketAssetTransaction.AJAX_KEY, TicketAssetTransaction.class);
		actionMap.put(UserTransaction.AJAX_KEY, UserTransaction.class);
		actionMap.put(ProviderLocationTransaction.AJAX_KEY, ProviderLocationTransaction.class);
		actionMap.put(DiagnosticTransaction.AJAX_KEY, DiagnosticTransaction.class);
		actionMap.put(TicketScheduleTransaction.AJAX_KEY, TicketScheduleTransaction.class);
		actionMap.put(TicketCommentTransaction.AJAX_KEY, TicketCommentTransaction.class);
		actionMap.put(TicketAssignmentTransaction.AJAX_KEY, TicketAssignmentTransaction.class);
		actionMap.put(TicketCloneTransaction.AJAX_KEY, TicketCloneTransaction.class);
		actionMap.put(TicketDataTransaction.AJAX_KEY, TicketDataTransaction.class);
		actionMap.put(TicketUtilityTransaction.AJAX_KEY, TicketUtilityTransaction.class);
		actionMap.put(TicketPDFCreator.AJAX_KEY, TicketPDFCreator.class);
		actionMap.put(creditMemoPDFCreator.AJAX_KEY, creditMemoPDFCreator.class);
		actionMap.put(TicketTransaction.AJAX_KEY, TicketTransaction.class);
		actionMap.put(TicketPartsTransaction.AJAX_KEY, TicketPartsTransaction.class);
		actionMap.put(TicketRepairTransaction.AJAX_KEY, TicketRepairTransaction.class);
		actionMap.put(ProductSerialTransaction.AJAX_KEY, ProductSerialTransaction.class);
		actionMap.put(TicketSearchTransaction.AJAX_KEY, TicketSearchTransaction.class);
		actionMap.put(RefundReplacementTransaction.AJAX_KEY, RefundReplacementTransaction.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ActionInterface action = loadActionByType(req.getParameter(SELECT_KEY));
		action.build(req);

		// Add in the Next Step data to the returned data
		if (action instanceof BaseTransactionAction) {

			ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			Map<String, Object> data = new HashMap<>();
			data.put(GlobalConfig.SUCCESS_KEY, !mod.getErrorCondition());
			data.put(GlobalConfig.ACTION_DATA_KEY, mod.getActionData());
			data.put(GlobalConfig.ACTION_DATA_COUNT, mod.getDataSize());
			data.put(ErrorCodes.ERR_JSON_ACTION, StringUtil.checkVal(mod.getErrorMessage()));
			data.put("nextStep", ((BaseTransactionAction) action).getNextStep());
			putModuleData(data);
		}

	}
}

