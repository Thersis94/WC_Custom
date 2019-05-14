package com.biomed.smarttrak.admin;

import com.biomed.smarttrak.action.UpdatesEditionDataLoader;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title:</b> UpdatesWeeklyReportDataLoader.java<br/>
 * <b>Description:</b> loads the data slightly different for /manage.  This class simply alters the query used to load the data
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 8, 2017
 ****************************************************************************/
public class UpdatesManageReportDataLoader extends UpdatesEditionDataLoader {

	public UpdatesManageReportDataLoader() {
		super();
	}

	/**
	 * @param init
	 */
	public UpdatesManageReportDataLoader(ActionInitVO init) {
		super(init);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		req.setParameter("profileId", null);
		req.setAttribute(IS_MANAGE_TOOL, true); //attribute - can't be spoofed by the browser
		super.retrieve(req);
	}
}