package com.wsla.action.report;

import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ReportFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Facade Action for the reports
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 5, 2018
 * @updates:
 ****************************************************************************/
public class ReportFacadeAction extends FacadeActionAdapter {

	/**
	 * 
	 */
	public ReportFacadeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ReportFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#lit(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		((ModuleVO) attributes.get(Constants.MODULE_DATA)).setSimpleAction(true);
	}
}
