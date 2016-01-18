package com.depuysynthes.huddle;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

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
import com.smt.sitebuilder.search.SearchDocumentHandler;

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

		// Only add filters if this is the main portlet on the page.
		if (req.hasParameter("searchData")) {
			if (req.getCookie(HuddleUtils.RPP_COOKIE) != null)
				req.setParameter("rpp", req.getCookie(HuddleUtils.RPP_COOKIE).getValue());

			Cookie sortCook = req.getCookie(HuddleUtils.SORT_COOKIE);
			String sort = (sortCook != null) ? sortCook.getValue() : "titleAZ";

			if ("recentlyAdded".equals(sort)) {
				req.setParameter("fieldSort", SearchDocumentHandler.UPDATE_DATE, true);
				req.setParameter("sortDirection", ORDER.desc.toString(), true);
			} else if ("titleZA".equals(sort)) {
				req.setParameter("fieldSort", SearchDocumentHandler.TITLE_SORT, true);
				req.setParameter("sortDirection", ORDER.desc.toString(), true);
			} else if ("titleAZ".equals(sort)) {
				req.setParameter("fieldSort", SearchDocumentHandler.TITLE_SORT, true);
				req.setParameter("sortDirection", ORDER.asc.toString(), true);
			}
		}

		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}

}
