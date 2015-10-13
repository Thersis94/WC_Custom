package com.willowgreen.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: GriefEmailWrapper.java<p/>
 * <b>Description: Facade around the email sign-up (Contact Us Portlet) and reporting
 * (simplified ContactDataTool)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 28, 2013
 ****************************************************************************/
public class DailyCaregiversEmailWrapper extends EmailWrapper {
	
	public DailyCaregiversEmailWrapper() {
		super();
	}
	
	public DailyCaregiversEmailWrapper(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	public void build(SMTServletRequest req) throws ActionException {
		req.setAttribute("series", "DIFC");
		super.build(req);
	}
	
}
