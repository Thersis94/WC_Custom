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
 ****************************************************************************/

public enum WSLALocales {
	en_US("US English"), es_MX("MX Espanol"), es_HN("HN Espanol"),es_CR("CR Espanol"),
	es_GT("GT Espanol"),es_DO("DO Espanol");
	
	private String desc;
	public String getDesc() { return desc;}
	private WSLALocales(String desc) { this.desc = desc; }
}

