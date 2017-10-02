package com.depuysynthes.action;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title:</b> DSSolrSearchAction.java<br/>
 * <b>Description:</b> Allows the usage of huddle's cookie based sort and rpp
 * system in the site search.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Oct 2, 2017
 ****************************************************************************/
public class DSSolrSearchAction extends SimpleActionAdapter {

	private static final String FIELD_SORT = "fieldSort";
	private static final String SORT_DIR = "sortDirection";

	public DSSolrSearchAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public DSSolrSearchAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		if (req.hasParameter("searchData") && mod.getDisplayColumn() != null && mod.getDisplayColumn().equals(page.getDefaultColumn()))
			this.build(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		actionInit.setActionId(solrActionId);

		SMTCookie rppCook = req.getCookie("dsRpp");
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());

		//apply sorting
		SMTCookie sCook = req.getCookie("dsSort");
		String sort = sCook != null ? sCook.getValue() : null;
		if ("recentlyAdded".equals(sort)) {
			req.setParameter(FIELD_SORT, SearchDocumentHandler.UPDATE_DATE, true);
			req.setParameter(SORT_DIR, ORDER.desc.toString(), true);
		} else if ("titleZA".equals(sort)) {
			req.setParameter(FIELD_SORT, SearchDocumentHandler.TITLE_LCASE, true);
			req.setParameter(SORT_DIR, ORDER.desc.toString(), true);
		} else if ("titleAZ".equals(sort)) {
			req.setParameter(FIELD_SORT, SearchDocumentHandler.TITLE_LCASE, true);
			req.setParameter(SORT_DIR, ORDER.asc.toString(), true);
		} //default sorting is defined on the widget config (likely score)

		ActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}	
}
