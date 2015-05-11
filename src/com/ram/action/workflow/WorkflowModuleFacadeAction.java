/**
 * 
 */
package com.ram.action.workflow;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: WorkflowModuleFacadeAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Facade Action manages RAM Workflow Module and Parameter 
 * configuration.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Apr 29, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class WorkflowModuleFacadeAction extends SBActionAdapter {

	public enum CallType {module, param, config}
	public static final String CALL_TYPE = "callType";
	/**
	 * 
	 */
	public WorkflowModuleFacadeAction() {
	}

	public WorkflowModuleFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(SMTServletRequest req) throws ActionException {
		CallType type = getCallType(req.getParameter(CALL_TYPE));
		if(type != null)
			getAction(type).build(req);
	}

	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		CallType type = getCallType(req.getParameter(CALL_TYPE));
		if(type != null)
			getAction(type).copy(req);
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		CallType type = getCallType(req.getParameter(CALL_TYPE));
		if(type != null)
			getAction(type).delete(req);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		CallType type = getCallType(req.getParameter(CALL_TYPE));
		if(type != null)
			getAction(type).list(req);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		CallType type = getCallType(req.getParameter(CALL_TYPE));
		if(type != null)
			getAction(type).retrieve(req);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
		CallType type = getCallType(req.getParameter(CALL_TYPE));
		getAction(type).update(req);
	}

	/**
	 * Helper method that returns proper action for the given CallType.
	 * @param type
	 * @return
	 */
	public SMTActionInterface getAction(CallType type) {
		SMTActionInterface action = null;
		switch(type) {
			default:
			case module:
				action = new WorkflowModuleAction(this.actionInit);
				break;
			case param:
				action = new WorkflowParamAction(this.actionInit);
				break;
			case config:
				action = new WorkflowConfigTypeAction(this.actionInit);
				break;
		}

		//Set Additional Action Params.
		action.setDBConnection(dbConn);
		action.setAttributes(attributes);

		return action;
	}

	/**
	 * Helper method that converts a given String to a CallType
	 * @param callType
	 * @return
	 */
	public CallType getCallType(String callType) {
		CallType type = null;
		try {
			type = CallType.valueOf(callType);
		} catch(Exception e) {
			type = null;
		}

		return type;
	}
	
}
