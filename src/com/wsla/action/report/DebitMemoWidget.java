package com.wsla.action.report;

import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: DebitMemoWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the Debit Memo Report
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 8, 2019
 * @updates:
 ****************************************************************************/

public class DebitMemoWidget extends SBActionAdapter {
	/**
	 * Ajax key for the main controller to use to call this action
	 */
	public static final String AJAX_KEY = "debitMemo";
	
	/**
	 * 
	 */
	public DebitMemoWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DebitMemoWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Test");
	}
}

