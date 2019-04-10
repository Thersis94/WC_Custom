package com.biomed.smarttrak.admin;

//WC Custom libs
import com.biomed.smarttrak.action.GapAnalysisAction;
import com.biomed.smarttrak.action.ProductExplorer;
import com.siliconmtn.action.ActionControllerFactoryImpl;

//Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

//WebCrescendo libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

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
public class ToolsFacadeAction extends SimpleActionAdapter {

	public ToolsFacadeAction() {
		super();
	}

	public ToolsFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	} 

	protected enum FacadeType {
		PRODUCT_EXPLORER("explorer", ProductExplorer.class.getName()), 
		GAP_ANALYSIS("analysis", GapAnalysisAction.class.getName());

		private String facadeTarget; 
		private String className; //the fully qualified class name
		private FacadeType(String facadeTarget, String className) {
			this.facadeTarget = facadeTarget;
			this.className = className;
		}
		/*===Getters===*/
		public String getFacadeTarget() { return facadeTarget; }
		public String getClassName() { return className; }
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve (ActionRequest req) throws ActionException {
		FacadeType type = getFacadeType(req.getParameter("facadeType"));

		//load action and execute retrieve
		ActionControllerFactoryImpl.loadAction(type.getClassName(), this).retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		FacadeType type = getFacadeType(req.getParameter("facadeType"));

		//load the action and execute build
		ActionControllerFactoryImpl.loadAction(type.getClassName(), this).build(req);
	}

	/**
	 * Retrieves the facade type based on string passed
	 * @param target
	 * @return
	 * @throws ActionException
	 */
	private FacadeType getFacadeType(String target) throws ActionException {
		for (FacadeType type : FacadeType.values()) {
			if (type.getFacadeTarget().equalsIgnoreCase(target))
				return type;
		}
		//no match, throw an exception
		throw new ActionException("Unknown facade type. Please check value passed.");
	}
}
