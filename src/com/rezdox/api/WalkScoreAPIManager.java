package com.rezdox.api;

// JDk 1.8.x
import java.net.URLEncoder;

// Gson 2.8
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// WC Libs
import com.rezdox.vo.WalkScoreVO;

// SMT Base Libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: WalkScoreAPIManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> API Manager to retrieve data from the Walk Score API
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/

public class WalkScoreAPIManager {

	// Member Variables
	private String apiKey;
	private String baseUrl;
	
	// API Information
	public static final String API_KEY = "c666fb6d1c648e56e30696cb7d277851";
	public static final String BASE_URL = "http://api.walkscore.com/score";
	
	/**
	 * Defines the API key and base URL for the connection
	 * @param id
	 * @param baseUrl
	 */
	public WalkScoreAPIManager(String apiKey, String baseUrl) {
		super();
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
	}
	
	/**
	 * Assigns the default data inthe constants to the API key and URL
	 */
	public WalkScoreAPIManager() {
		super();
		this.apiKey = API_KEY;
		this.baseUrl = BASE_URL;
	}
	
	/**
	 * Retrieves the walk score for the given location
	 * @param loc
	 * @return
	 * @throws InvalidDataException
	 */
	public WalkScoreVO retrieveWalkScore(GeocodeLocation loc) throws InvalidDataException {
		StringBuilder path = new StringBuilder(128);
		WalkScoreVO ws = new WalkScoreVO();
		
		try {
			path.append(baseUrl).append("?format=json&transit=1&bike=1");
			path.append("&wsapikey=").append(apiKey);
			path.append("&address=").append(URLEncoder.encode(loc.getFormattedLocation().trim(), "UTF-8"));
			path.append("&lat=").append(loc.getLatitude());
			path.append("&lon=").append(loc.getLongitude());
			
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			byte[] response = conn.retrieveData(path.toString());
			
			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd h:m:s").create();
			ws = gson.fromJson(new String(response), WalkScoreVO.class);
		} catch(Exception e) {
			throw new InvalidDataException("unable to retrieve walkscore", e);
		}
		
		return ws;
	}
}

