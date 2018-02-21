package com.biomed.smarttrak.admin;

//WC_Custom libs
import com.biomed.smarttrak.action.GapAnalysisAction;
import com.biomed.smarttrak.action.ProductExplorer;
//Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
//SMT libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: ToolsFacadeAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Facade action controls which actions are called within the manage site tools menu.<p/>
 * Copyright: Copyright (c) 2018<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Jan 12, 2018
 ****************************************************************************/

public class ToolsFacadeAction extends SBActionAdapter {

	public ToolsFacadeAction() {
		super();
	}
	
	public ToolsFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	} 
	
	protected enum FacadeType{
		PRODUCT_EXPLORER("explorer"), GAP_ANALYSIS("analysis");
		
		private String facadeTarget;
		private FacadeType(String facadeTarget) {
			this.facadeTarget = facadeTarget;
		}
		/*===Getters===*/
		public String getFacadeTarget() {
			return facadeTarget;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve (ActionRequest req) throws ActionException {
		FacadeType type = getFacadeType(req.getParameter("facadeType"));
		
		//call the appropriate action
		switch(type) {
			case PRODUCT_EXPLORER : 
				loadProductExplorer(req);
				break;
			case GAP_ANALYSIS : 
				loadGapAnalysis(req);
				break;
			default : break;
		}
	}
	
	/**
	 * Loads the product explorer as it would appear on public site
	 * @param req
	 * @throws ActionException
	 */
	protected void loadProductExplorer(ActionRequest req) throws ActionException {
		//set proper action id to process solr request correctly for action
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String actionId = mod.getIntroText();
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, actionId);
		setAttribute(Constants.MODULE_DATA, mod);
				
		ActionInterface ai = new ProductExplorer(actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.retrieve(req);
	}
	
	/**
	 * Loads the gap analysis as it would appear on public site
	 * @param req
	 * @throws ActionException
	 */
	protected void loadGapAnalysis(ActionRequest req) throws ActionException {
		ActionInterface ai = new GapAnalysisAction(actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.retrieve(req);
	}
	
	/**
	 * Retrieves the facade type based on string passed
	 * @param target
	 * @return
	 * @throws ActionException
	 */
	private FacadeType getFacadeType(String target) throws ActionException {
		FacadeType ft = null;
		for (FacadeType type : FacadeType.values()) {
			if(type.facadeTarget.equalsIgnoreCase(target)) {
				ft = type;
			}
		}
		
		if(ft == null) throw new ActionException("Unknown facade type. Please check value passed.");
		return ft;
	}
}
