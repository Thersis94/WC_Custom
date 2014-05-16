package com.fastsigns.action;

import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: CenterPageDealerLocatorAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Combines CenterPageAction.java and DealerLocatorAction.java
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author billy
 * @version 1.0
 * @since March 23, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CenterPageDealerLocatorAction extends SBActionAdapter {

	public CenterPageDealerLocatorAction() {
		super();
	}
	
	public CenterPageDealerLocatorAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
    	log.debug("Starting CenterPageDealerLocation - retrieve");
 		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
 		String orgId = ((SiteVO) req.getAttribute("siteData")).getOrganizationId();
		//if we have a searchType, forward to DealerLocatorAction
		if (req.hasParameter("searchType")) {
			actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
			SMTActionInterface dla = new DealerLocatorAction(actionInit);
			dla.setAttributes(attributes);
			dla.setDBConnection(dbConn);
			dla.retrieve(req);
			
		} else { //if not dealerLocatorSearch
			String franchiseId = null;
	 		//attempt to retrieve the franchiseId from the User.
			UserDataVO userVo = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			
			//retrieve the franchiseID off the role if its not null
			if(userVo != null){
				Map<String, Object> s = userVo.getAttributes();
				if(s != null)
					franchiseId = StringUtil.checkVal(s.get("franchiseId"));
			}
			//If no id found use the session
			if(franchiseId == null || franchiseId.length() == 0)
				franchiseId = StringUtil.checkVal(req.getSession().getAttribute("FranchiseId"));
			
			log.debug("Retrieving CenterPage Info for franchise: " + franchiseId);
			
			//If franchiseId is found 
			if (franchiseId.length() > 0) {
				String pmid = mod.getPageModuleId();
				
		 		// create a temp module VO so that the original is preserved
		 		ModuleVO tempMod = null;
		    	
		 		// try to leverage caching 
		 		log.debug("cacheId =" + pmid + franchiseId);
		 		tempMod = super.readFromCache(pmid + orgId + "_" + franchiseId + "_1");
		 		
		        if (tempMod == null) {
		        	//populate the parameters we need to run the action
		    		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		    		mod.setAttribute(SBModuleVO.ATTRIBUTE_1, franchiseId);
		    		req.setParameter(SB_ACTION_ID, this.getActionInit().getActionId());
		        	
		    		// retrieve the Center Page Data
		        	SMTActionInterface cla = new CenterPageAction(actionInit);
		        	cla.setAttributes(attributes);
		        	cla.setDBConnection(dbConn);
		        	cla.retrieve(req);
		        	
		        	tempMod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			 		log.debug("StoredcacheId =" + pmid + franchiseId);
			 		log.debug("tempMod ID = " + tempMod.getPageModuleId());
			 		
			 		tempMod.setCacheGroup(franchiseId);
		        	
			 		tempMod.setPageModuleId(pmid + orgId + "_" + franchiseId+"_1");
		    		super.writeToCache(tempMod);
		        }
		        
				mod.setActionData(tempMod.getActionData());
				attributes.put(Constants.MODULE_DATA, mod);
			}
		}
		
    }
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
