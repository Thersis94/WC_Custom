package com.depuysynthesinst;

import org.apache.solr.common.SolrDocument;

import com.depuysynthes.lucene.MediaBinSolrIndex;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: MediaBinVideo.java<p/>
 * <b>Description: binds a single mediabin asset and leverages solr to load the meta-data for display. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 19, 2014
 ****************************************************************************/
public class MediaBinVideo extends SimpleActionAdapter {

	public MediaBinVideo() {
	}

	/**
	 * @param arg0
	 */
	public MediaBinVideo(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		//this portlet does not run when /qs/ is present - DSI Anatomy pages
		if (req.hasParameter("reqParam_1")) return;
		
		//query Solr for the assets we need to display
		//everything we need for the View is contained in the SolrDocument(s) returned.
		SolrActionVO qData = new SolrActionVO();
		
		SolrFieldVO field = new SolrFieldVO();
		field.setBooleanType(BooleanType.AND);
		field.setFieldType(FieldType.SEARCH_FIELD);
		field.setFieldCode(SearchDocumentHandler.DOCUMENT_ID);
		field.setValue((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		qData.addSolrField(field);
		
		SolrFieldVO field2 = new SolrFieldVO();
		field2.setBooleanType(BooleanType.AND);
		field2.setFieldType(FieldType.SEARCH_FIELD);
		field2.setFieldCode(SearchDocumentHandler.INDEX_TYPE);
		field2.setValue(MediaBinSolrIndex.INDEX_TYPE);
		qData.addSolrField(field2);
		
		qData.setOrganizationId(site.getOrganizationId());
		qData.setRoleLevel((role != null) ? role.getRoleLevel() : SecurityController.PUBLIC_ROLE_LEVEL);
		qData.setNumberResponses(1);
		
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes);
		SolrResponseVO resp = sqp.processQuery(qData);
		
		SolrDocument doc = null;
		if (resp.getResultDocuments() != null && resp.getResultDocuments().size() > 0)
			doc = resp.getResultDocuments().get(0);
		
		//log.debug(doc);
		super.putModuleData(doc);
	}

}
