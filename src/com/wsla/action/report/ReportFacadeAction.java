package com.wsla.action.report;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
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
	 * Request key utilized top determine widget to call
	 */
	public static final String SELECTOR_KEY = "reportType";
	
	/**
	 * 
	 */
	public ReportFacadeAction() {
		super();
		loadTypes();
	}

	/**
	 * @param actionInit
	 */
	public ReportFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		loadTypes();
	}
	
	/**
	 * Loads the mapping to the various ajax calls
	 */
	private void loadTypes() {
		actionMap.put(BillableActivityReport.AJAX_KEY, BillableActivityReport.class);
		actionMap.put(SummaryActivityReport.AJAX_KEY, SummaryActivityReport.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String reportType = req.getStringParameter(SELECTOR_KEY, "");
		if (reportType.length() == 0 || ! actionMap.containsKey(reportType)) return;
		
		loadActionByType(reportType).retrieve(req);
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
