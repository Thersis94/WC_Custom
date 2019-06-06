package com.wsla.common;

/****************************************************************************
 * <b>Title</b>: WSLALocales.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> List of locales supported by the Application
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 1, 2018
 * @updates:
 * @deprecated 5/8/19 - JM - the resource bundle classes now perform auto-discovery of implementations,
 * which is how the admintool builds it's list.  As a result, this manual enum is undesireable and should be removed.
 * Recommendation is to change the enum to a static class, which mimizes downstream changes/impact
 ****************************************************************************/
@Deprecated
public enum WSLALocales {
	en_US("US English", true), es_MX("MX Español", true), es_HN("HN Español", true),es_CR("CR Español", true),
	es_GT("GT Español", true),es_DO("DO Espanol", true), en_("Base English",false), es_("Base Español", false);

	private String desc;
	private boolean isDisplayed;
	private WSLALocales(String desc, boolean isDisplayed) { this.desc = desc; this.isDisplayed = isDisplayed;}
	public String getDesc() { return desc;}
	public boolean isDisplayed() {return isDisplayed;}
	public static WSLALocales[] getBaseLocales() {
        WSLALocales[] base = new WSLALocales[2];
        base[0] = WSLALocales.en_;
        base[1] = WSLALocales.es_;
        return base;
    }
}
