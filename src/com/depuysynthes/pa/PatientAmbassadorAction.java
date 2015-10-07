package com.depuysynthes.pa;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorAction.java<p/>
 * <b>Description: facades the normal workflow to Solr with an alternate use-case
 * for preview mode viewing of unpublished stories, which is a call to the BlackBox,
 * not Solr.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 6, 2015
 ****************************************************************************/
public class PatientAmbassadorAction extends SimpleActionAdapter {

	public PatientAmbassadorAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PatientAmbassadorAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String storyId = StringUtil.checkVal(req.getParameter("reqParam_1"));
		boolean isPreview = storyId.startsWith("preview_") && page.isPreviewMode();
		
		if (isPreview) {
			//load a single story from the database, rather than Solr.
			storyId = storyId.substring(8);
			loadStory(req, storyId, (String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		} else {
			//call to solr per usual
			actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			SolrAction sa = new SolrAction(actionInit);
			sa.setDBConnection(dbConn);
			sa.setAttributes(getAttributes());
			sa.retrieve(req);
			sa = null;
		}
	}
	
	
	/**
	 * calls the data tool action to load the desired patient story from the black box
	 * @param req
	 */
	private void loadStory(SMTServletRequest req, String storyId, String formId) {
		req.setParameter("fsi", storyId);
		req.setParameter("formId", formId);
		PatientAmbassadorStoriesTool pa = new PatientAmbassadorStoriesTool(actionInit);
		pa.setDBConnection(dbConn);
		pa.setAttributes(attributes);
		pa.retrieveSubmittalData(req);
		ModuleVO mod = (ModuleVO) pa.getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		if (mod != null) putModuleData(mod.getActionData());
		pa = null;
		req.setAttribute("formDataPreview", "formDataPreview");
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}