package com.wsla.action.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.resource.ResourceBundleDataVO;
import com.smt.sitebuilder.resource.BundleKeyMgrAction;
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
 * 		extracted superclass (moved to WC core).  -JM- 12.04.2019
 ****************************************************************************/
public class LanguageBundleMapAction extends BundleKeyMgrAction {

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


	/**
	 * Gets a list of the keys and values for a given bundle key 
	 * @param keyId
	 * @param bundleId
	 * @return
	 */
	@Override
	protected List<ResourceBundleDataVO> getBundleInfo(String keyId, String bundleId) {
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
		List<ResourceBundleDataVO> results = db.executeSelect(sql.toString(), Arrays.asList(keyId), new ResourceBundleDataVO(), "language_cd");
		log.debug(String.format("base locales length=%d, results=%d", WSLALocales.getBaseLocales().length, results.size()));  
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
		StringBuilder cs = new StringBuilder(150);
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
}
