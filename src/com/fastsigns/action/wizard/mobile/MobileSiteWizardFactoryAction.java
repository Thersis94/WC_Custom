package com.fastsigns.action.wizard.mobile;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;

public class MobileSiteWizardFactoryAction extends SBActionAdapter {

	public MobileSiteWizardFactoryAction(){
		
	}
	
	public MobileSiteWizardFactoryAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		String locale = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		MobileSiteWizardAction swa = retrieveWizard(locale);
		swa.setAttributes(attributes);
		swa.setDBConnection(dbConn);
		swa.build(req);
	}
	
	public MobileSiteWizardAction retrieveWizard(String locale) {
		MobileSiteWizardAction swa = null;
		if(locale == null || locale.equals("")) {
			locale = "US";
		}
		String classKey = "com.fastsigns.action.wizard.mobile.MobileSiteWizardAction_" + locale;
		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(classKey);
			swa = (MobileSiteWizardAction)load.newInstance();

		} catch (Exception e) {
			log.error(e);
            swa = new MobileSiteWizardAction_US();
        } 
		
		return swa;
	}
}
