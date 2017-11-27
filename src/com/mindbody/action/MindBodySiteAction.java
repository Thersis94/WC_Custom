package com.mindbody.action;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> MindBodySiteAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Site Data Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodySiteAction extends SimpleActionAdapter {

	public MindBodySiteAction() {
		super();
	}

	public MindBodySiteAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) {
		
	}
}
