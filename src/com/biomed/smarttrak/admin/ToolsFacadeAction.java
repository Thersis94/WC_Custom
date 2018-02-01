package com.biomed.smarttrak.admin;

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
		PRODUCT_EXPLORER("explorer", "com.biomed.smarttrak.action.ProductExplorer"), 
		GAP_ANALYSIS("analysis", "com.biomed.smarttrak.action.GapAnalysisAction");
		
		private String facadeTarget; 
		private String className; //the fully qualified class name
		private FacadeType(String facadeTarget, String className) {
			this.facadeTarget = facadeTarget;
			this.className = className;
		}
		/*===Getters===*/
		public String getFacadeTarget() {
			return facadeTarget;
		}
		public String getClassName() {
			return className;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve (ActionRequest req) throws ActionException {
		FacadeType type = getFacadeType(req.getParameter("facadeType"));
		
		if(FacadeType.PRODUCT_EXPLORER.equals(type)){
			configurePEData();
		}
		
		//load action and execute retrieve
		loadAction(type.getClassName()).retrieve(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException{
		FacadeType type = getFacadeType(req.getParameter("facadeType"));
		
		if(FacadeType.PRODUCT_EXPLORER.equals(type)){
			configurePEData();
		}
		
		//load action and execute build
		loadAction(type.getClassName()).build(req);
	}
	
	/**
	 * Configures data required to run Product Explorer for public site
	 */
	protected void configurePEData() {
		//set proper action id to process solr request correctly for action
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String actionId = mod.getIntroText();
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, actionId);
		setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Dynamically instantiates the appropriate action class based on given class name
	 * @param className - the fully qualified class name
	 * @return - the corresponding action class
	 * @throws ActionException
	 */
	private ActionInterface loadAction(String className) throws ActionException {
		ActionInterface ai = null;
		
		//load the class and set attributes/dbConn
		try {
			Class<?> c = Class.forName(className);
			ai = (ActionInterface) c.newInstance();
			ai.setActionInit(actionInit);
			ai.setAttributes(attributes);
			ai.setDBConnection(dbConn);
		} catch (Exception e) {
			throw new ActionException("Could not instantiate tool facade class: ", e);
		}
		return ai;
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
