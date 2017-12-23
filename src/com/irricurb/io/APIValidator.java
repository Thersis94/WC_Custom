package com.irricurb.io;

// Gson 2.4
import com.google.gson.Gson;

// SMT Base Libs
import com.siliconmtn.io.http.SMTHttpConnectionManager;

/****************************************************************************
 * <b>Title</b>: APIValidator.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b>Test class for the gateway to the nodes
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Dec 22, 2017
 * @updates:
 ****************************************************************************/
public class APIValidator {
	public static final String URL = "http://test-frontend.oviattgreenhouse.com/api/gateways/gateway_test_001?token=3d15359b2872548acb89b7a2c0a0fe6f";
	
	/**
	 * 
	 */
	public APIValidator() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] json = conn.retrieveData(URL);
		
		Gson gson = new Gson();
		GatewayVO gateway = gson.fromJson(new String(json), GatewayVO.class);
		System.out.println(gateway);
	}

}
