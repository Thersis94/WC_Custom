package com.depuysynthes.huddle;

import javax.servlet.http.Cookie;

import com.depuysynthes.huddle.solr.BlogSolrIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.blog.BlogFacadeAction;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: BlogAction.java<p/>
 * <b>Description: Wraps a Solr call around Blog, so we have Facet support for the View.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 08, 2016
 ****************************************************************************/
public class BlogAction extends SimpleActionAdapter {

	public BlogAction() {
		super();
	}

	public BlogAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(dbConn);
		bfa.delete(req);
		if (!req.hasParameter("blogId")) return;
		
		//fire the delete to Solr
		SolrActionUtil util = new SolrActionUtil(getAttributes());
		util.removeDocument(req.getParameter("blogId"));
	}


	@Override
	public void list(SMTServletRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(dbConn);
		bfa.list(req);
	}


	@Override
	public void update(SMTServletRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(dbConn);
		bfa.update(req);
		if (!req.hasParameter("blogId")) return;
		
		//fire the VO to Solr, leverage the same lookup the "full rebuild" indexer uses, which joins to Site Pages
		BlogSolrIndexer indexer = BlogSolrIndexer.makeInstance(getAttributes());
		indexer.setDBConnection(getDBConnection());
		log.debug("indexing blog article " + req.getParameter("blogId"));
		indexer.pushSingleArticle(req.getParameter("blogId")); //null here would index the entire portlet, not just one article
	}


	/**
	 * Call to Solr for a list of Blogs for the given actionId.
	 * Solr makes faceting easy; otherwise it's a ton of manual labor we need to code.
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		Cookie rppCook = req.getCookie(HuddleUtils.RPP_COOKIE);
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());
		
		//if we have specialty add it as a filter - this allows the section homepages to target pre-filtered blog lists
		if (req.hasParameter("specialty"))
			req.setParameter("fq", HuddleUtils.SOLR_OPCO_FIELD + ":" + req.getParameter("specialty"));

		req.setParameter("fmid",mod.getPageModuleId());
		//NOTE: page & start get picked up by SolrActionVO automatically, because we set "fmid"

		//call to solr for a list of sales consultants
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_2)); //the solrActionId we're wrapping
		actionInit.setActionId(solrActionId);
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
		
		req.setParameter("fmid", "", true);
	}
}