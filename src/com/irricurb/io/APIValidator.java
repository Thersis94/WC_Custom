package com.irricurb.io;

import java.util.Date;

// Gson 2.4
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.irricurb.action.data.vo.DeviceDataVO;
import com.irricurb.action.data.vo.ProjectDeviceAttributeVO;
import com.irricurb.action.data.vo.ProjectDeviceVO;
import com.irricurb.action.data.vo.ProjectLocationVO;
import com.irricurb.action.data.vo.ProjectZoneVO;

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
		testDeviceEntity();
	}
	
	/**
	 * 
	 */
	protected static void testDeviceEntity() {
		DeviceDataVO data = new DeviceDataVO();
		data.setCreateDate(new Date());
		data.setReadingDate(new Date());
		data.setProjectDeviceDataId("12344567");
		data.setProjectDeviceId("");
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	protected static void testGateway() throws Exception {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] json = conn.retrieveData(URL);
		
		Gson gson = new Gson();
		GatewayVO gateway = gson.fromJson(new String(json), GatewayVO.class);
		System.out.println(gateway);
	}
	
	
	/**
	 * 
	 * @throws Exception
	 */
	protected static void testDevice() throws Exception {
		// Define the location
		ProjectLocationVO location = new ProjectLocationVO();
		location.setProjectLocationId("Location1");
		
		// Define the zone
		ProjectZoneVO zone = new ProjectZoneVO();
		zone.setProjectZoneId("ZONE_5");
		
		// Define the device
		ProjectDeviceVO device = new ProjectDeviceVO();
		device.setProjectDeviceId("PRO_DEVICE_1");
		
		// Add attributes to the device
		ProjectDeviceAttributeVO attribute = new ProjectDeviceAttributeVO();
		attribute.setAttributeDeviceId("1138");
		attribute.setValue("On");
		attribute.setDeviceAttributeId("ENGAGE");
		device.addAttribute(attribute);
		
		attribute = new ProjectDeviceAttributeVO();
		attribute.setAttributeDeviceId("1139");
		attribute.setValue("RED");
		attribute.setDeviceAttributeId("COLOR_ROYGBIV");
		device.addAttribute(attribute);
		
		// Add the hierarchies
		zone.addDevice(device);
		location.addZone(zone);
		
		// Serialize the object
		Gson g = new GsonBuilder().setExclusionStrategies(new ProjectLocationExclusionStrategy()).create();
		String json = g.toJson(location);
		System.out.println(json);
		
	}

}
