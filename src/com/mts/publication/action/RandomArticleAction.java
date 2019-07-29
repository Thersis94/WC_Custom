package com.mts.publication.action;

import com.mts.action.SelectLookupAction;
import com.mts.publication.data.MTSDocumentVO;
import com.siliconmtn.action.ActionException;
// SMT BAse Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: RandomArticleAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gets a rando set of articles for the given publication.  
 * If no publication is selected, randomizes across all articles
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 26, 2019
 * @updates:
 ****************************************************************************/
public class RandomArticleAction extends SBActionAdapter {

	/**
	 * 
	 */
	public RandomArticleAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RandomArticleAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Format the data
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String pubId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		int count = Convert.formatInteger((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		if (count > 0) req.setParameter("limit", count + "");
		req.setParameter("order", "random()");
		
		// Query the publication for random articles
		BSTableControlVO bst = new BSTableControlVO(req, MTSDocumentVO.class);
		DocumentBrowseAction dba = new DocumentBrowseAction(dbConn, attributes);
		GridDataVO<MTSDocumentVO> results = dba.search(bst, pubId, null, null, null, null);
		putModuleData(results.getRowData());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
		
		SelectLookupAction sla = new SelectLookupAction();
		sla.setDBConnection(getDBConnection());
		sla.setAttributes(getAttributes());
		req.setAttribute("mts_publications", sla.getPublications(req));
		
	}

}
