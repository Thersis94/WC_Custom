package com.irricurb.lookup;

/****************************************************************************
 * <b>Title</b>: DeviceTypeEnum.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Enum of device types thsat match the DB lookup table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 11, 2018
 * @updates:
 ****************************************************************************/

public enum DeviceTypeEnum {
	// List of device types
	ATMO_PRESSURE_TEMP ("Atmosphere sensor for temperature and pressure"),
	CONTROLLER ("Controller"),
	HUMIDITY ("Humidity Sensor"),
	LIGHT ("Light"),
	LUX ("Light sensor"),
	MOISTURE ("Moisture Meter"),
	PH ("Alkaline Sensor"),
	SOIL ("Soil Multidata sensor"),
	SPRINKLER ("Sprinkler"),
	TEMPERATURE ("Temperature Sensor");
	
	// Member variable for the Name of the enum 
	private String name;
	
	/**
	 * Constructor to add the enum name
	 * @param name
	 */
	DeviceTypeEnum(String name) {
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

