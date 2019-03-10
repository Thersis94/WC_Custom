package com.perfectstorm.common;

/****************************************************************************
 * <b>Title</b>: PSLocales.java
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

public enum PSLocales {
	en_US("US English"), es_MX("MX Spanish");
	
	private String desc;
	public String getDesc() { return desc;}
	private PSLocales(String desc) { this.desc = desc; }
}

