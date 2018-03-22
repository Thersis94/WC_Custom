package com.rezdox.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.FacadeActionAdapter;

/****************************************************************************
 * <b>Title:</b> ProjectFacadeAction.java<br/>
 * <b>Description:</b> Controller action for Project-related activities; including materials.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 25, 2018
 ****************************************************************************/
public class ProjectFacadeAction extends FacadeActionAdapter {

	/**
	 * Actions supported by the facade/controller
	 */
	public enum ActionType {
		PROJECT(ProjectAction.class), 
		MATERIAL(ProjectMaterialAction.class);

		Class<? extends ActionInterface> c;
		ActionType(Class<? extends ActionInterface> c) { this.c = c; }
		public Class<? extends ActionInterface> getClassName() { return c; } 
	}

	public ProjectFacadeAction() {
		super();
		initMap();
	}

	public ProjectFacadeAction(ActionInitVO init) {
		super(init);
		initMap();
	}

	/**
	 * called from the constructors, this method populates the actionMap in the superclass.
	 */
	private void initMap() {
		for (ActionType type : ActionType.values())
			actionMap.put(type.name(), type.getClassName());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		loadActionByType(getType(req)).retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		loadActionByType(getType(req)).build(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/**
	 * reused helper to get the actionType off the request object.  Defaults to PROJECT
	 * @param req
	 * @return
	 */
	private String getType(ActionRequest req) {
		return StringUtil.checkVal(req.getParameter("actionType"), ActionType.PROJECT.name()).toUpperCase();
	}

}