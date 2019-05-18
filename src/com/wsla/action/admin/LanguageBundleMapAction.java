package com.wsla.action.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.resource.ResourceBundleDataVO;
import com.siliconmtn.resource.ResourceBundleKeyVO;
import com.siliconmtn.util.StringUtil;
// WC Lib
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.ResourceBundleManagerAction;
import com.smt.sitebuilder.resource.ResourceBundleLoader;
import com.wsla.common.LocaleWrapper;
import com.wsla.common.WSLALocales;

/****************************************************************************
 * <b>Title</b>: LanguageBundleMapAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the Key/Values for languages from the resource bundle
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 16, 2018
 * @updates:
 ****************************************************************************/

public class LanguageBundleMapAction extends SBActionAdapter {

	/**
	 * Ajax to utilize when calling this action
	 */
	public static final String AJAX_KEY = "languageBundle";
	public static final String KEY_CODE = "keyCode";
	
	/**
	 * 
	 */
	public LanguageBundleMapAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public LanguageBundleMapAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			ResourceBundleLoader bl = new ResourceBundleLoader(dbConn);
			String keyId = bl.getKeyId(req.getParameter(KEY_CODE));
			log.debug("keyId "+ keyId);
			
			putModuleData(getBundleInfo(keyId));
		} catch (Exception e) {
			log.error("could not load language bundel edits", e);
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}

 	
	/**
	 * Gets a list of the keys and values for a given bundle key 
	 * @param statusCode
	 * @return
	 */
	public List<ResourceBundleDataVO> getBundleInfo(String keyId) {
		StringBuilder sql = new StringBuilder(576);
		sql.append("select l.country_cd, l.language_cd, l.variant_cd, ");
		sql.append("b.resource_bundle_data_id, l.key_id, value_txt, resource_bundle_id ");
		sql.append("from ( ");
		sql.append("SELECT cast(? as varchar(32)) as key_id, ");
		sql.append(buildCase(0, "language_cd")).append(",");
		sql.append(buildCase(1, "country_cd")).append(",");
		sql.append(buildCase(2, "variant_cd"));
		sql.append("FROM generate_series(0,").append(WSLALocales.getBaseLocales().length - 1);
		sql.append(") x ").append(") as l ");
		sql.append("left outer join resource_bundle_data b ");
		sql.append("on l.key_id = b.key_id and l.country_cd = '' and l.language_cd = b.language_cd ");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<ResourceBundleDataVO> results = db.executeSelect(sql.toString(), Arrays.asList(keyId), new ResourceBundleDataVO(), "language_cd");
		log.debug("base locales length " + WSLALocales.getBaseLocales().length + " results size " + results.size());  
		return results;
	}

	/**
	 * Builds the case statement from the Enum. This will dynamically add locales
	 * as they are added to the enum
	 * @param type
	 * @param label
	 * @return
	 */
	private String buildCase(int type, String label) {
		WSLALocales[] locales = WSLALocales.getBaseLocales();

		StringBuilder cs = new StringBuilder(40);
		cs.append("case ");
		for (int i=0; i < locales.length ; i++) {
			WSLALocales localVar = locales[i];
			Locale locale = new LocaleWrapper(localVar).getLocale(); 
			String value = "";
			switch(type) {
				case 0 :
					value = locale.getLanguage();
					break;
				case 1 :
					value = locale.getCountry();
					break;
				default :
					value = locales[i].getDesc();
			}
			
			cs.append("when x = ").append(i).append(" then '");
			cs.append(value).append("' ");
		}
		
		cs.append("end as ").append(label).append(" ");
		
		return cs.toString();
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ResourceBundleDataVO dvo = new ResourceBundleDataVO(req);
		try {

			ResourceBundleManagerAction rbma = new ResourceBundleManagerAction(dbConn, attributes);
			
			ResourceBundleKeyVO key = rbma.getKeyByKeyCode(req.getParameter(KEY_CODE));
			if(StringUtil.isEmpty(key.getKeyId())) {
				log.debug("No key exists for this change adding one");
				
				String keyCode = StringUtil.checkVal(req.getParameter(KEY_CODE));
				String bundleId = StringUtil.checkVal(req.getParameter(ResourceBundleManagerAction.REQ_RES_BUNDLE_ID));
				String description = StringUtil.checkVal(req.getParameter("description"));
				//no key exists save a key first
				//get the new key id and save it to the data bundle
				dvo.setKeyId(rbma.addNewKey(keyCode, bundleId, description));
			}else {
				log.debug("using existing key " + key);
				dvo.setKeyId(key.getKeyId());
			}
			rbma.saveBundleKeyData(dvo);
			rbma.delete(req);
		} catch(Exception e) {
			log.error("Unable to save bundle", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
}
