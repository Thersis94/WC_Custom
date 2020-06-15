package com.mts.hootsuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class HootsuiteClientData {

	private Map<String, String> socialProfiles = new HashMap<>();

	public HootsuiteClientData() {
		socialProfiles.put("TWITTER", "131070214");
		socialProfiles.put("FACEBOOK", "130968924");
	}

	/**
	 * Get the ids of all of the social media accounts
	 * 
	 * @return List of social media ids
	 */
	public List<String> getSocialIds() {
		List<String> socialIds = new ArrayList<>();
		for (String name : socialProfiles.keySet()) {
			socialIds.add(socialProfiles.get(name));
		}
		return socialIds;
	}
}
