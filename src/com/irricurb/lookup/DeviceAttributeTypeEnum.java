package com.irricurb.lookup;

/****************************************************************************
 * <b>Title</b>: DeviceAttributeTypeEnum.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Enum used in code for all of the Device Attributes types such as DATA, SWITCH, etc ...
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since May 21, 2018
 * @updates:
 ****************************************************************************/
public enum DeviceAttributeTypeEnum {
	// List of Attribute types
	DATA("Data"),
	SWITCH("Switch"),
	LIST("List"),
	SCALE("Scale"),
	OPTION("Option"),
	TEXT("Text"),
	SCALE_MIN("Scale Minimum"),
	SCALE_MAX("Scale Maximum"),
	HEX_COLOR("Hex Color");
	
	// Member variable for the Name of the enum 
	private String name;
	
	/**
	 * Constructor to add the enum name
	 * @param name
	 */
	DeviceAttributeTypeEnum(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the name of the enum
	 * @return
	 */
	public String getName() {
		return this.name;
	}
}
