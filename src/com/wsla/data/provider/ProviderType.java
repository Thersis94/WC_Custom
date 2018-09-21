package com.wsla.data.provider;

/****************************************************************************
 * <b>Title</b>: ProviderType.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Enum supporting the provider types for WSLA
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/

public enum ProviderType {
	OEM ("Manufacturer"),
	RETAILER ("Retail Store"),
	CAS ("Service Center"),
	WSLA ("Warranty Service Latin America");
	
	public final String typeName;
	ProviderType(String typeName) { this.typeName = typeName; }
}

