package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BlogAction.java<p/>
 * <b>Description: Wraps a Solr call around Blog, so we have Facet support for the View.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 30, 2015
 ****************************************************************************/
public class BlogAction extends SimpleActionAdapter {

	public BlogAction() {
		super();
	}

	public BlogAction(ActionInitVO arg0) {
		super(arg0);
	}
	

	/**
	 * Call to Solr for a list of Blogs for the given actionId.
	 * Solr makes faceting easy; otherwise it's a ton of manual labor we need to code.
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//if there's a single article, load it from the DB.  Solr does not contain ALL of the data we need.
		if (req.hasParameter("article")) {
			loadSingleArticle(req);
		} else {
			loadSolrArticleList(req);
		}
	}
	
	
	/**
	 * loads a single article from the Blog portlet.
	 * @param req
	 * @throws ActionException
	 */
	private void loadSingleArticle(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		String blogActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		req.setParameter(SBModuleAction.SB_ACTION_ID, blogActionId);
		req.setParameter("blogId", req.getParameter("article"));
		com.smt.sitebuilder.action.blog.BlogAction ba = new com.smt.sitebuilder.action.blog.BlogAction(actionInit);
		ba.setDBConnection(dbConn);
		ba.setAttributes(getAttributes());
		ba.list(req);
		
	}
	
	
	/**
	 * calls to Solr for a list of Articles to display.  w/Facets.
	 * @param req
	 * @throws ActionException
	 */
	private void loadSolrArticleList(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		//call to solr for a list of sales consultants
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1)); //the solrActionId we're wrapping
		actionInit.setActionId(solrActionId);
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
	}
}