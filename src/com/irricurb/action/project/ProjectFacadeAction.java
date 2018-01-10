package com.irricurb.action.project;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;

/****************************************************************************
 * <b>Title</b>: ProjectFascadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Facade action controlling which project action is called.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 8, 2017
 * @updates:
 ****************************************************************************/
public class ProjectFacadeAction extends FacadeActionAdapter {
	
	// Constants for use in the action
	public static final String WIDGET_ACTION = "widgetAction";
	public static final String PROJECT_ID = "projectId";

	/**
	 * 
	 */
	public ProjectFacadeAction() {
		super();
		assignActions();
	}

	/**
	 * 
	 * @param arg0
	 */
	public ProjectFacadeAction(ActionInitVO arg0) {
		super(arg0);
		assignActions();
	}
	
	/**
	 * Assigns the facade actions need to be called
	 */
	public void assignActions() {
        actionMap.put(ProjectDeviceAction.DEVICE, ProjectDeviceAction.class);
        actionMap.put(ProjectManageAction.MANAGE, ProjectManageAction.class);
        actionMap.put(ProjectMapAction.MAP, ProjectMapAction.class);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.action.ActionRequest)
	 */
	@Override
    public void copy(ActionRequest req) throws ActionException {
		//empty for the time being
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req ) throws ActionException {
		log.debug("facade action retrieve called");
		if (!req.hasParameter(WIDGET_ACTION)) return;
		
		String key = StringUtil.checkVal(req.getParameter(WIDGET_ACTION));
		
		loadAction(key).retrieve(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req ) throws ActionException {
		log.debug("facade action build called");
		if (!req.hasParameter(WIDGET_ACTION)) return;
		
		String key = StringUtil.checkVal(req.getParameter(WIDGET_ACTION));
		
		loadAction(key).build(req);
	}
}
