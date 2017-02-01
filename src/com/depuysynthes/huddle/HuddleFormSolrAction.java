package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.formbuilder.FormBuilderFacadeAction;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: HuddleFormSolrAction.java<p/>
 * <b>Description: Wraps a Solr call around Forms, so we have Facet support for the View.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 16, 2016
 ****************************************************************************/
public class HuddleFormSolrAction extends SimpleActionAdapter {

	public HuddleFormSolrAction() {
		super();
	}

	public HuddleFormSolrAction(ActionInitVO arg0) {
		super(arg0);
	}


	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/**
	 * Call to Solr for a list of Blogs for the given actionId.
	 * Solr makes faceting easy; otherwise it's a ton of manual labor we need to code.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		/*
		 * If there is a qs field on the request, forward the call to
		 * FormBuilderFacadeActions retrieve method.  Otherwise 
		 * just query Solr for a list of Forms
		 */
		Object formData = null;
		if (req.hasParameter("reqParam_1")) {
			getFormBuilderFacadeAction(req.getParameter("reqParam_1")).retrieve(req);
			mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			formData = mod.getActionData();
			
			//the formId in reqParam_1 will slip through to Solr as documentId - coincidentally and necessary
			//the solrResponse will provide a roleLevel for how this form appears on Huddle (Secure or public)
			//this value determined whether the form can be added to the users briefcase or not.
		}

		//OTHERWISE: Do a solr search and forward to the grid view
		SMTCookie rppCook = req.getCookie(HuddleUtils.RPP_COOKIE);
		if (rppCook != null)
			req.setParameter("rpp", rppCook.getValue());

		//if we have specialty add it as a filter - this allows the section homepages to target pre-filtered form lists
		if (req.hasParameter("specialty"))
			req.setParameter("fq", HuddleUtils.SOLR_OPCO_FIELD + ":" + req.getParameter("specialty"));

		req.setParameter("fmid",mod.getPageModuleId());
		//NOTE: page & start get picked up by SolrActionVO automatically, because we set "fmid"

		//call to solr for a list of sales consultants
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1)); //the solrActionId we're wrapping
		actionInit.setActionId(solrActionId);
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(getAttributes());
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
		req.setParameter("fmid", "", true);
		
		if (req.hasParameter("reqParam_1")) {
			//separate the solr response from the form builder response
			mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			SolrResponseVO solrResp = (SolrResponseVO) mod.getActionData();
			if (solrResp != null && solrResp.getTotalResponses() == 1) 
				req.setAttribute("SOLR_DOC", solrResp.getResultDocuments().get(0));
			
			mod.setActionData(formData);
		}
	}


	/**
	 * Proxy Build request through the FormBuilderFacadeAction Build method.
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		/*
		 * If there is a formId on the request, forward the call to
		 * FormBuilderFacadeActions build method.
		 */
		if(req.hasParameter("formId")) {
			getFormBuilderFacadeAction(req.getParameter("formId")).build(req);
		}
	}


	/**
	 * Helper method that manages building a FormBuilderFacadeAction instance.
	 * @param formId
	 * @return
	 */
	protected FormBuilderFacadeAction getFormBuilderFacadeAction(String formId) {
		this.actionInit.setActionGroupId(formId);
		FormBuilderFacadeAction fbfa = new FormBuilderFacadeAction(this.actionInit);
		fbfa.setDBConnection(getDBConnection());
		fbfa.setAttributes(getAttributes());
		return fbfa;
	}
}