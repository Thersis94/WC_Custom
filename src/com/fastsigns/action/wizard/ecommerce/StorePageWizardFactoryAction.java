package com.fastsigns.action.wizard.ecommerce;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: StorePageWizardFactoryAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Registered Action in the database processes the request 
 * by choosing the appropriate Config based on the sites countryCode and then 
 * forwarding the call to StorePageWizardAction.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 2013<p/>
 ****************************************************************************/
public class StorePageWizardFactoryAction extends SBActionAdapter{
	public StorePageWizardFactoryAction(){
		
	}
	public StorePageWizardFactoryAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	/**
	 * Load the proper StoreConfig and then run the build through StorePageWizardAction.
	 */
	public void build(SMTServletRequest req) throws ActionException {
		String locale = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		StoreConfig conf = retrieveConfig(locale);
		StorePageWizardAction spa = new StorePageWizardAction();
		spa.setAttributes(attributes);
		spa.setDBConnection(dbConn);
		spa.setConfig(conf);
		spa.build(req);
	}
	
	/**
	 * Load the proper config based on locale (countryCode)
	 * @param locale
	 * @return
	 */
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
