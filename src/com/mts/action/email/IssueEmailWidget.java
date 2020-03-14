package com.mts.action.email;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: IssueEmailWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This widget injects the latest issue articles and overview
 * into an MTS email showing/describing the contents of the latest issue
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 11, 2020
 * @updates:
 ****************************************************************************/
public class IssueEmailWidget extends SimpleActionAdapter {

	/**
	 * 
	 */
	public IssueEmailWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public IssueEmailWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("retrieving");
	}
}
