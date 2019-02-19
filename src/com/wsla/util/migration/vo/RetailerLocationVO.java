package com.wsla.util.migration.vo;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> RetailerLocationVO.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 11, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class RetailerLocationVO {

	private String name;
	private String address;
	private String city;
	private String state;

	public String getStore() {
		if (StringUtil.isEmpty(name)) return "";

		return Convert.formatInteger(name.trim().split("\\s")[0]).toString();
	}
	public String getName() {
		return name;
	}
	public String getAddress() {
		return address;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}

	@Importable(name="name")
	public void setName(String name) {
		this.name = name;
	}
	@Importable(name="address")
	public void setAddress(String address) {
		this.address = address;
	}
	@Importable(name="city")
	public void setCity(String city) {
		this.city = city;
	}
	@Importable(name="state")
	public void setState(String state) {
		this.state = state;
	}	
}