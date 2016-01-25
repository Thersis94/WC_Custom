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
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/**
 * **************************************************************************
 * <b>Title</b>: HuddleSolrSearch.java<p/>
 * <b>Description: Allows the usage of huddle's cookie based sort and rpp
 * system in the site search.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 22, 2016
 ***************************************************************************
 */
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
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		if (req.hasParameter("searchData") && page.getDefaultColumn() == mod.getDisplayColumn())
			this.build(req);
	}
	
	
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		actionInit.setActionId(solrActionId);

		//apply sorting
		HuddleUtils.setSearchParameters(req, req.getParameter("siteSort"));
		
		//apply section-based boosting if there is no sort order requested by the user
		if (!req.hasParameter("siteSort")) {
			if (req.hasParameter("specialty")) {
				req.setParameter("customParam", "bq|"+HuddleUtils.SOLR_OPCO_FIELD+":"+req.getParameter("specialty"));
			} else if (req.hasParameter("category")) {
				req.setParameter("customParam", "bq|"+SearchDocumentHandler.HIERARCHY_LCASE+":"+req.getParameter("category")+"*");
			}
			log.debug("boost=" + req.getParameter("customParam"));
		}
		
		
		
		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
}