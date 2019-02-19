package com.wsla.data.product;

/****************************************************************************
 * <b>Title</b>: WarrantyType.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Types of Warranty
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/

public enum WarrantyType {
	EXTENDED ("Extended Warranty"),
	MANUFACTURER ("Manufacturer Warranty");
	
	public final String typeName;
	WarrantyType(String typeName) { this.typeName = typeName; }
}

