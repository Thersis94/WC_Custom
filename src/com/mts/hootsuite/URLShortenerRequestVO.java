package com.mts.hootsuite;
/****************************************************************************
 * <b>Title</b>: URLShortenerRequestVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for the values required to make a url shortening request through the bitly api.
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since Jun 23, 2020
 * @updates:
 ****************************************************************************/
public class URLShortenerRequestVO {

	private String domain = "bit.ly";
	private String long_url;
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}
	/**
	 * @param domain the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}
	/**
	 * @return the long_url
	 */
	public String getLong_url() {
		return long_url;
	}
	/**
	 * @param long_url the long_url to set
	 */
	public void setLong_url(String long_url) {
		this.long_url = long_url;
	}
	
	
}
