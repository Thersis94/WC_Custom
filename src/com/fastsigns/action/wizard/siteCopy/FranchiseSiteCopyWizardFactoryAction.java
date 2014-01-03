package com.fastsigns.action.wizard.siteCopy;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;

public class FranchiseSiteCopyWizardFactoryAction extends SBActionAdapter{
	
	public FranchiseSiteCopyWizardFactoryAction(){
		
	}
	
	public FranchiseSiteCopyWizardFactoryAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		String locale = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		FranchiseSiteCopyWizard fsc = retrieveWizard(locale);
		fsc.setActionInit(actionInit);
		fsc.setAttributes(attributes);
		fsc.setDBConnection(dbConn);
		fsc.build(req);
	}
	
	public FranchiseSiteCopyWizard retrieveWizard(String locale) {
		FranchiseSiteCopyWizard swa = null;
		if(locale == null || locale.equals("")) {
			locale = "US";
		}
		String classKey = "com.fastsigns.action.wizard.siteCopy.FranchiseSiteCopyWizard_" + locale;
		try {			
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(classKey);
			swa = (FranchiseSiteCopyWizard)load.newInstance();

		} catch (Exception e) {
			log.error(e);
            swa = new FranchiseSiteCopyWizard_US();
        } 
		
		return swa;
	}
}
