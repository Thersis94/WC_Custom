package com.mts.publication.action;

// MTS Imports
import com.mts.action.SelectLookupAction;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PublicationDisplayAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the display of information on the subscriber home page
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 3, 2019
 * @updates:
 ****************************************************************************/
public class PublicationDisplayAction extends SBActionAdapter {

	/**
	 * 
	 */
	public PublicationDisplayAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PublicationDisplayAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		req.setParameter(SB_ACTION_ID, getActionInit().getActionId());
		super.retrieve(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String publicationId = (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
		
		IssueArticleAction iac = new IssueArticleAction(getDBConnection(), getAttributes());
		setModuleData(iac.getArticleTeasers(publicationId));
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
		
		// Add the categories
		req.setParameter("parentId", "CHANNELS");
		req.setAttribute("mts_channels", sla.getCategories(req));
		
	}
}
