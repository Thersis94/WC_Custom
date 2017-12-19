package com.irricurb.action.project;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
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
	
	public static final String WIDGET_ACTION = "widgetAction";
	public static final String PROJECT_ID = "projectId";
	public static final String SELECT_STAR = "select * from ";

	
    private static final Map<String,  Class<? extends ActionInterface>> ACTION_MAP;
    
    /**
     * builds the static map so these methods can be used anywhere
     */
    static {
        Map<String,  Class<? extends ActionInterface>> statMap = new HashMap<>();
        statMap.put(ProjectManageAction.MANAGE, ProjectManageAction.class);
        statMap.put(ProjectDeviceAction.DEVICE, ProjectDeviceAction.class);
        ACTION_MAP = Collections.unmodifiableMap(statMap);
    }
    
	public ProjectFacadeAction() {
		super();
	}

	public ProjectFacadeAction(ActionInitVO arg0) {
		super(arg0);
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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req ) throws ActionException {
		//empty for the time being
		super.delete(req);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req ) throws ActionException {
		//empty for the time being
		super.update(req);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req ) throws ActionException {
		if (!req.hasParameter(WIDGET_ACTION)) return;
		
		String key = StringUtil.checkVal(req.getParameter(WIDGET_ACTION));
		
		loadAction(key).retrieve(req);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req ) throws ActionException {
		//empty for the time being
		super.list(req);
	}
	
	/**
	 * Based on the passed actionType, instantiate the appropriate class and return.
	 * 
	 * @param actionType
	 * @return
	 * @throws ActionException
	 */
	protected ActionInterface loadAction(String actionType) throws ActionException {
		Class<?> c = ACTION_MAP.get(actionType);
		if (c == null) 
			throw new ActionException("Unknown action type: " + actionType);

		// Instantiate the action & return it - pass attributes & dbConn
		try {
			ActionInterface action = (ActionInterface) c.newInstance();
			action.setDBConnection(dbConn);
			action.setAttributes(getAttributes());
			return action;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ActionException("Problem instantiating action type: " + actionType);
		}
	}
	

	
	
}
