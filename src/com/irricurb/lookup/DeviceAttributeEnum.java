package com.irricurb.lookup;

/****************************************************************************
 * <b>Title</b>: DeviceAttributeEnum.java
 * <b>Project</b>: IC_Controller
 * <b>Description: </b> Enum used in code for all of the Device Attributes such as TEMPERATURE, PRESSURE, etc ...
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 8, 2018
 * @updates:
 ****************************************************************************/

public enum DeviceAttributeEnum {
	// List of Attributes
	BAROMETRIC ("Barometric Pressure"),
	BRIGHT ("Brightness Scale"),
	COLOR_RGB ("Hex color control"),
	COLOR_ROYGBIV ("Rainbow Color List"),
	COLOR_WHITE ("Color List"),
	ENGAGE ("Engage Switch"),
	FLOW ("Flow Rate Scale"),
	FULL_SPECTRUM ("Full Sectrum"),
	HUMIDITY ("Humidty Level"),
	INFRARED_SPECTRUM ("Infrered Spectrum"),
	LUX ("Light Sensor"),
	MOISTURE ("Moisture"),
	PH ("PH of Soil"),
	PRESSURE ("Water Pressure"),
	PSI ("Water Pressure"),
	RGB_COLOR_SCALE ("Color Scale"),
	ROTATION ("Rotation Switch"),
	RRM ("Rotations per min"),
	SALINITY ("Salinity of soil"),
	SOUND_VOLUME ("Volume Scale"),
	TEMPERATURE ("Temperature"),
	VISIBLE_SPECTRUM ("Visible Spectrum"),
	WATER_VOLUME ("Volume of Water Used");
	
	// Member variable for the Name of the enum 
	private String name;
	
	/**
	 * Constructor to add the enum name
	 * @param name
	 */
	DeviceAttributeEnum(String name) {
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

