package com.wsla.common;

// JDK 1.8.x
import java.util.Locale;

// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: LocaleWrapper.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Convenience class to take a language_Country locale
 * and get a Locale Object 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 16, 2018
 * @updates:
 ****************************************************************************/

public class LocaleWrapper extends BeanDataVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1141735696743427694L;
	private Locale locale;
	
	/**
	 * Builds a Locale from the combine language/country
	 * @param strLocale
	 */
	public LocaleWrapper(String strLocale) {
		if (strLocale == null || strLocale.indexOf('_') == -1) return;
		
		String[] vals = strLocale.split("_");
		locale = new Locale(vals[0], vals[1]);
 	}
	
	/**
	 * Takes the enum and coverts to a Locale
	 * @param wslaLocale
	 */
	public LocaleWrapper(WSLALocales wslaLocale) {
		this(wslaLocale.name());
	}
	
	/**
	 * Returns the locale
	 * @return
	 */
	public Locale getLocale() {
		return locale;
	}

}

