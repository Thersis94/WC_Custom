package com.irricurb.lookup;

/****************************************************************************
 * <b>Title</b>: DisplayTypeEnum.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Will hold the enum values fr the display type table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since May 21, 2018
 * @updates:
 ****************************************************************************/
public enum DisplayTypeEnum {
	SLIDER2D("2 Directional Slider"),
	COLOR("Color Slider"),
	SWITCH("On / Off Switch"),
	SELECT("Select Picker"),
	DISPLAY("Display Value"),
	SLIDER("Range Slider");
	

	// Member variable for the Name of the enum 
	private String formattedName;
	
	/**
	 * Constructor to add the enum name
	 * @param name
	 */
	DisplayTypeEnum(String formattedName) {
		this.formattedName = formattedName;
	}
	
	/**
	 * Returns the name of the enum
	 * @return
	 */
	public String getFormattedName() {
		return this.formattedName;
	}
}
