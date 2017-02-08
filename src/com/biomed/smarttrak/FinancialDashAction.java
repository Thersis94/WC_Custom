package com.biomed.smarttrak;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: FinancialDashAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 06, 2017
 ****************************************************************************/

public class FinancialDashAction extends SBActionAdapter {
	
	public FinancialDashAction() {
		super();
	}

	public FinancialDashAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);
		
		ActionInterface ai = getAction(req);
		ai.retrieve(req);
	}
	
	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		
		ActionInterface ai = getAction(req);
		ai.build(req);
	}
	
	/**
	 * Gets the appropriate action based on the passed data.
	 * 
	 * Base: Standard display of data.
	 * Overlay: Display of data with scenario data used in place of standard data (where applicable).
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private ActionInterface getAction(ActionRequest req) throws ActionException {
		String scenarioId = StringUtil.checkVal(req.getParameter("scenarioId"));
		ActionInterface ai = null;
		
		// Determine the request type and forward to the appropriate action
		if (scenarioId.length() > 0) {
			ai = new FinancialDashScenarioOverlayAction(this.actionInit);
		} else {
			ai = new FinancialDashBaseAction(this.actionInit);
		}

		// Set the appropriate attributes for the action
		ai.setAttributes(this.attributes);
		ai.setDBConnection(dbConn);

		return ai;
	}
}
