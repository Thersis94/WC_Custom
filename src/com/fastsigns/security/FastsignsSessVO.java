package com.fastsigns.security;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: FastsignsSessVO.java<p/>
 * <b>Description: This object serves as the session container for a FASTSIGNS
 * logged-in user.  Most of these attributes are returned from Keystone.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 1, 2012
 ****************************************************************************/
public class FastsignsSessVO implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String FRANCHISE_ID = "FranchiseId"; //legacy session constant
	
	private Map<String,FranchiseVO> franchiseMap = new HashMap<String, FranchiseVO>();  //key=franchiseWebId ([0-9]{3})
	private Map<String, KeystoneUserDataVO> profileMap = new HashMap<String, KeystoneUserDataVO>(); //key = franchiseWebId ([0-9]{3})
	
	public FastsignsSessVO() {
	}
	
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * returns the desired Franchise from the map
	 * We ALWAYS return an object here, to keep our actions from having to test every method call for null
	 * @param webId
	 * @return
	 */
	public FranchiseVO getFranchise(String webId) {
		if (franchiseMap.containsKey(webId)) {
			return franchiseMap.get(webId);
		} else {
			return new FranchiseVO();
		}
	}

	public void addFranchise(FranchiseVO franchise) {
		this.franchiseMap.put(franchise.getWebId(), franchise);
	}
	
	/**
	 * return the user's profile attached to 'this' Franchise
	 * We ALWAYS return an object here, to keep our actions from having to test every method call for null
	 * @param webId
	 * @return
	 */
	public KeystoneUserDataVO getProfile(String webId) {
		if (profileMap.containsKey(webId)) {
			return profileMap.get(webId);
		} else {
			return new KeystoneUserDataVO();
		}
	}
	
	public Map<String, KeystoneUserDataVO> getProfiles() {
		return profileMap;
	}

	public void addProfile(KeystoneUserDataVO profile) {
		this.profileMap.put(profile.getWebId(), profile);
	}

	/**
	 * helper method for when we load profiles at login; they have franchiseId, but we need them
	 * to be stored using webId instead (to be WC-side compliant for easy lookups in the actions).
	 * @param franchiseId
	 * @return
	 */
	public String lookupWebId(String franchiseId) {
		for (FranchiseVO vo : franchiseMap.values()) {
			if (franchiseId.equals(vo.getFranchiseId()))
				return vo.getWebId();
		}
		return "";
	}
	
	public Map<String, FranchiseVO> getFranchises() {
		return franchiseMap;
	}
}
