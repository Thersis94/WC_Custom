package com.fastsigns.action.wizard;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;

public class SiteWizardFactoryAction extends SBActionAdapter{
	
	public SiteWizardFactoryAction(){
		
	}
	
	public SiteWizardFactoryAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		String locale = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		SiteWizardAction swa = retrieveWizard(locale);
		swa.setAttributes(attributes);
		swa.setDBConnection(dbConn);
		swa.build(req);
	}
	
	public SiteWizardAction retrieveWizard(String locale) {
		SiteWizardAction swa = null;
		if(locale == null || locale.equals("")) {
			locale = "US";
		}
		String classKey = "com.fastsigns.action.wizard.SiteWizardAction_" + locale;
		try {			
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(classKey);
			swa = (SiteWizardAction)load.newInstance();

		} catch (Exception e) {
			log.error(e);
            swa = new SiteWizardAction_US();
        } 
		
		return swa;
	}
}
