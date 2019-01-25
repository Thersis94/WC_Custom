package com.wsla.util.migration.vo;

import com.siliconmtn.annotations.Importable;

/****************************************************************************
 * <p><b>Title:</b> RetailerFileVO.java</p>
 * <p><b>Description:</b> models WSLA Retailers.xlsx emailed by Steve</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 23, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class RetailerFileVO {

	private String cd;
	private String providerId;
	private String providerName;


	public String getCd() {
		return cd;
	}
	public String getProviderId() {
		return providerId;
	}
	public String getProviderName() {
		return providerName;
	}
	@Importable(name="Cd")
	public void setCd(String cd) {
		this.cd = cd;
	}
	@Importable(name="Short Desc")
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	@Importable(name="Full Description")
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}
}