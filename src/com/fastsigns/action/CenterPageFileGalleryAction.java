package com.fastsigns.action;

import com.fastsigns.action.franchise.CenterPageAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.file.gallery.GalleryFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: CenterPageFileGalleryAction,java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Combines CenterPageAction.java and GalleryFacadeAction.java
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author billy
 * @version 1.0
 * @since March 28, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CenterPageFileGalleryAction extends SBActionAdapter {

	public CenterPageFileGalleryAction() {
		super();
	}

	public CenterPageFileGalleryAction(ActionInitVO arg0) {
		super(arg0);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Starting CenterPageFileGallery - retrieve");
		
		//Grab Site Id of the Franchise we're looking up and substring it down to the center number.
		SiteVO site = ((SiteVO)req.getAttribute(Constants.SITE_DATA));
		String siteId = site.getSiteId();
		String orgId = site.getAliasPathOrgId();
		
		String franNum = (String) siteId.subSequence(orgId.length() + 1, siteId.length()-2);
		
		//Store original versions of data for use at end.
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String galleryId = (String) mod.getAttribute(SBModuleVO.ATTRIBUTE_2);

		//Make call to retrieve the ctrPgAction's franchise ID
		req.setParameter(SB_ACTION_ID, orgId + "_CENTER_PAGE_" + franNum);
		super.retrieve(req);
		ModuleVO tempMod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SBModuleVO tempMod2 = (SBModuleVO) tempMod.getActionData();
		String franchiseId = (String)tempMod2.getAttribute(SBModuleVO.ATTRIBUTE_1);

		log.debug("Retrieving CenterPage Info for franchise: " + franchiseId);

		//Populate the parameters to retrieve the File Gallery
		actionInit.setActionId((String) mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
		req.setParameter(SB_ACTION_ID, this.getActionInit().getActionId());

		//Create an ActionInitVO to use with Gallery, prevents problems with secondary action call and caching.
		ActionInitVO ai = new ActionInitVO(null, null, galleryId);
		
		//Create GalleryFacadeAction and retrieve Gallery.
		SMTActionInterface gfa = new GalleryFacadeAction(ai);
		gfa.setAttributes(this.attributes);
		gfa.setDBConnection(dbConn);
		gfa.retrieve(req);
		
		//Store Data returned by Gallery Facade Action
		Object vo = ((ModuleVO) getAttribute(Constants.MODULE_DATA)).getActionData();

		//Populate the parameters we need to run the Center Page Action
		mod.setAttribute(SBModuleVO.ATTRIBUTE_1, franchiseId);
		actionInit.setActionId(franchiseId);
		req.setParameter(SB_ACTION_ID, franchiseId);

		//Retrieve the Center Page Data
		SMTActionInterface cla = new CenterPageAction(actionInit);
		cla.setAttributes(attributes);
		cla.setDBConnection(dbConn);
		cla.retrieve(req);

		//Store data returned by Center Page Action
		tempMod = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		//Store data in GenericVO on mod and place mod back in attributes.
		mod.setActionData(new GenericVO(vo, tempMod.getActionData()));
		attributes.put(Constants.MODULE_DATA, mod);
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
