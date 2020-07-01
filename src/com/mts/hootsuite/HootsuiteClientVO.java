package com.mts.hootsuite;

//JDK 1.8.x
import java.util.HashMap;
import java.util.Map;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: HootsuiteClientData.java <b>Project</b>: Hootsuite
 * <b>Description: </b> A VO that holds a client unique hootsuite information. <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 28, 2020
 * @updates:
 ****************************************************************************/
public class HootsuiteClientVO extends BeanDataVO {

	private Map<String, String> socialProfiles = new HashMap<>();

	private String refreshToken;
	private String accessToken;
	
	/**
	 * @return the refreshToken
	 */
	public String getRefreshToken() {
		return refreshToken;
	}


	/**
	 * @param refreshToken the refreshToken to set
	 */
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}


	/**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}


	/**
	 * @param accessToken the accessToken to set
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}


	/**
	 * @return the socialProfiles
	 */
	public Map<String, String> getSocialProfiles() {
		return socialProfiles;
	}


	/**
	 * @param socialProfiles the socialProfiles to set
	 */
	public void setSocialProfiles(Map<String, String> socialProfiles) {
		this.socialProfiles = socialProfiles;
	}
}
