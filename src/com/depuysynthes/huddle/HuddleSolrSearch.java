package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: HuddleSolrSearch.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Allows the usage of huddle's cookie based sort and rpp
 * system in the site search.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Sep 5, 2014<p/>
 * @updates:
 ****************************************************************************/
public class HuddleSolrSearch  extends SimpleActionAdapter {
	
	public HuddleSolrSearch() {
		super();
	}

	public HuddleSolrSearch(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		actionInit.setActionId(solrActionId);
		
		if (req.hasParameter("section")) {
			log.debug("bq|"+HuddleUtils.SOLR_OPCO_FIELD+":"+req.getParameter("section"));
			req.setParameter("customParam", "bq|"+HuddleUtils.SOLR_OPCO_FIELD+":"+req.getParameter("section"));
		}
		
		HuddleUtils.setSearchParameters(req, req.getParameter("siteSort"));
		log.debug(req.getParameter("fieldSort"));
		log.debug(req.getParameter("sortDirection"));
		
		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}

}
