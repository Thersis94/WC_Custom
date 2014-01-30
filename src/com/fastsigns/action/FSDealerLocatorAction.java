package com.fastsigns.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FSDealerLocatorAction.java<p/>
 * <b>Description: Specialized dealer locator for Fastsigns sets a canonical
 * url for </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 30, 2014
 ****************************************************************************/

public class FSDealerLocatorAction extends DealerLocatorAction {
	
	public FSDealerLocatorAction () {
		super();
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
		if (StringUtil.checkVal(req.getParameter(Constants.DEALER_LOCATION_ID_KEY)).length() > 0) {
			PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
			page.setCanonicalPageUrl("/" + req.getParameter(Constants.DEALER_LOCATION_ID_KEY));
		}
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		super.delete(req);		
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		super.build(req);
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}

}
