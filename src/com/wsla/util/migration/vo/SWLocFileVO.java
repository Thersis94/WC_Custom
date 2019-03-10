package com.wsla.util.migration.vo;

import com.siliconmtn.annotations.Importable;

/****************************************************************************
 * <p><b>Title:</b> CASFileVO.java</p>
 * <p><b>Description:</b> models DM-CAS.xlsx provided by Steve</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SWLocFileVO {

	private String loc;
	private String name;
	private String city;
	private String state;
	private String zip;


	public String getLoc() {
		return loc;
	}
	public String getName() {
		return name;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}
	public String getZip() {
		return zip;
	}
	@Importable(name="Loc")
	public void setLoc(String loc) {
		this.loc = loc;
	}
	@Importable(name="Name")
	public void setName(String name) {
		this.name = name;
	}
	@Importable(name="City")
	public void setCity(String city) {
		this.city = city;
	}
	@Importable(name="St")
	public void setState(String state) {
		this.state = state;
	}
	@Importable(name="Zip")
	public void setZip(String zip) {
		this.zip = zip;
	}
}