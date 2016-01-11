package com.depuysynthes.huddle;

import javax.servlet.http.Cookie;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>: HuddleProductAction.java <p/>
* <b>Project</b>: WebCrescendo <p/>
* <b>Description: Wrapper for the solr search that translates all cookies and
* request parameters into solr usable values.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2016<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Eric Damschroder
* @version 1.0
* @since Jan 11, 2016<p/>
****************************************************************************/

public class HuddleProductAction extends SimpleActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());
		
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		actionInit.setActionId(solrActionId);
		if (req.hasParameter("category") && !req.hasParameter("fq")) {
			req.setParameter("fq", "hierarchy:" + req.getParameter("category"));
		}
		
		if (req.hasParameter("specialty")) {
			req.setParameter("fq", "opco_ss:" + req.getParameter("specialty"));
		}
		
		req.setParameter("rpp", req.getCookie(HuddleUtils.RPP_COOKIE).getValue());
		
		Cookie sort = req.getCookie(HuddleUtils.SORT_COOKIE);
		
		if (sort == null) {
			// Default to normal sort
		} else if (sort.getValue() == "recentlyAdded") {
			req.setParameter("fieldSort", "updateDate");
			req.setParameter("sortDirection", "desc");
		} else if (sort.getValue() == "titleZA") {
			req.setParameter("fieldSort", "title");
			req.setParameter("sortDirection", "desc");
		} else if (sort.getValue() == "titleAZ") {
			req.setParameter("fieldSort", "title");
			req.setParameter("sortDirection", "asc");
		}
		
		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
}
