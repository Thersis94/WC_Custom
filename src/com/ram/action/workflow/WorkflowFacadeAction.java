package com.ram.action.workflow;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

import com.siliconmtn.util.StringUtil;
// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title: </b>WorkflowFacadeAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0<p/>
 * @since Apr 27, 2015<p/>
 *<b>Changes: </b>
 * Apr 27, 2015: Tim Johnson: Created class.
 ****************************************************************************/
public class WorkflowFacadeAction extends SBActionAdapter {
	
	//Used to hold type param we look for and the values that are valid.
	public static final String STEP_PARAM = "bType";
	public static enum WORKFLOW_TYPE {workflow, module, transaction, transactionNotes}
	/**
	 * 
	 */
	public WorkflowFacadeAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public WorkflowFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 * updated to use getAction method.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("WorkflowFacadeAction retrieve...");
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			// perform search
			performSearch(req);
		} else {
			String bType = StringUtil.checkVal(req.getParameter(STEP_PARAM), WORKFLOW_TYPE.workflow.name());
			getAction(bType).retrieve(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 * updated to use getAction method
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// check to see if we are cloning a workflow or module
		boolean isCopy = Convert.formatBoolean(req.getParameter("isCopy"));
		if (isCopy) {
			copy(req);
			return;
		}

		log.debug("WorkflowFacadeAction build...");
		String bType = req.getParameter(STEP_PARAM);
		getAction(bType).build(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException{
		log.debug("WorkflowFacadeAction copy...");
		String bType = req.getParameter(STEP_PARAM);
		getAction(bType).copy(req);
	}
	
	/**
	 * Clean interface that returns the proper action we want pre populated with all the required data.
	 * @param action
	 * @return
	 */
	public ActionInterface getAction(String action) {
		ActionInterface sai = null;
		switch(WORKFLOW_TYPE.valueOf(action)) {
		case transaction:
			sai = new WorkflowTransactionAction(actionInit);
			break;
		case transactionNotes:
			sai = new WorkflowTransactionNotesAction(actionInit);
			break;
		case module:
			sai = new WorkflowModuleAction(actionInit);
			break;
		default:
			sai = new WorkflowAction(actionInit);
			break;
		}
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		return sai;
	}
	
	/**
	 * Performs search against workflow table(s)
	 * @param req
	 * @throws ActionException 
	 */
	private void performSearch(ActionRequest req) throws ActionException {
		log.debug("WorkflowFacadeAction performSearch...");
		ActionInterface sai = new WorkflowSearchAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
        super.retrieve(req);
	}

}
