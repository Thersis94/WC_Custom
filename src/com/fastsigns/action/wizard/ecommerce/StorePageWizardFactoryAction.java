package com.fastsigns.action.wizard.ecommerce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.fastsigns.action.franchise.CenterPageAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;

public class StorePageWizardFactoryAction extends SBActionAdapter{
	public StorePageWizardFactoryAction(){
		
	}
	public StorePageWizardFactoryAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		String locale = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		StoreConfig conf = retrieveConfig(locale);
		StorePageWizardAction spa = new StorePageWizardAction();
		spa.setAttributes(attributes);
		spa.setDBConnection(dbConn);
		spa.setConfig(conf);
		spa.build(req);
	}
	
	public StoreConfig retrieveConfig(String locale) {
		StoreConfig conf = null;
		if(locale == null || locale.equals("")) {
			locale = "US";
		}
		String classKey = "com.fastsigns.action.wizard.ecommerce." + locale + "StoreConfig";
		try {			
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(classKey);
			conf = (StoreConfig)load.newInstance();

		} catch (Exception e) {
			log.error(e);
            conf = new USStoreConfig();
        } 
		
		return conf;
	}
}
