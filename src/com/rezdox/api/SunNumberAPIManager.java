package com.rezdox.api;

import com.google.gson.Gson;
import com.rezdox.vo.SunNumberVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: SunNumberAPIManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> API to manage retrieving data from the sun number API
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/

public class SunNumberAPIManager {
	// Member Variables
	private String apiKey;
	private String baseUrl;
	
	// API Information
	public static final String API_KEY = "ad0b9979a935fc75c896554c579eeb86";
	public static final String BASE_URL = "http://api.sunnumber.com/v2/buildingsonly/";
	
	/**
	 * Defines the API key and base URL for the connection
	 * @param apiKey
	 * @param baseUrl
	 */
	public SunNumberAPIManager(String apiKey, String baseUrl) {
		super();
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
	}
	
	/**
	 * Assigns the default info in the constants
	 */
	public SunNumberAPIManager() {
		super();
		this.apiKey = API_KEY;
		this.baseUrl = BASE_URL;
	}
	
	/**
	 * Retrieves the sun number data from the api call
	 * @param loc
	 * @return
	 * @throws InvalidDataException
	 */
	public SunNumberVO retrieveSunNumber(GeocodeLocation loc) throws InvalidDataException {
		StringBuilder path = new StringBuilder(128);
		path.append(baseUrl).append(loc.getLatitude()).append("/").append(loc.getLongitude()).append("/1");
		path.append("?api_key=").append(apiKey);
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		SunNumberVO vo = null;
		try {
			byte[] response = conn.retrieveData(path.toString());
			
			Gson gson = new Gson();
			vo = gson.fromJson(new String(response), SunNumberVO.class);
		} catch(Exception e) {
			throw new InvalidDataException("Unable to retrieve sun number", e);
		}
		
		return vo;
	}
}

