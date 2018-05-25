package com.irricurb.lookup;

/****************************************************************************
 * <b>Title</b>: NetworkTypeEnum.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO Put Something Here
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since May 24, 2018
 * @updates:
 ****************************************************************************/
public enum NetworkTypeEnum {
	I2C("Inter-Integrated Circuit (I2C)"),
	ZIGBEE("Zigbee (iEEE 802.15.4)"),
	LORA("Long Range Wireless (LoRa)"),
	GPIO("General-purpose input/output (GPIO)"),
	HTTP("Webserver");
	
	// Member variable for the Name of the enum 
	private String formattedName;
	
	/**
	 * Constructor to add the enum name
	 * @param name
	 */
	NetworkTypeEnum(String formattedName) {
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
