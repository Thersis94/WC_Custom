package com.rezdox.vo;

import com.google.gson.annotations.SerializedName;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: SunNumberVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> //TODO Change Me
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/

public class SunNumberVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7215272825219180269L;

	// Member Variables
	@SerializedName("sunnumber")
	private String sunNumber;
	
	/**
	 * 
	 */
	public SunNumberVO() {
		super();
	}

	/**
	 * @return the sunNumber
	 */
	public String getSunNumber() {
		return sunNumber;
	}

	/**
	 * @param sunNumber the sunNumber to set
	 */
	public void setSunNumber(String sunNumber) {
		this.sunNumber = sunNumber;
	}

}

