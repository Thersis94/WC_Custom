package com.irricurb.io;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// Gson 2.4
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.irricurb.action.data.vo.DeviceDataVO;
import com.irricurb.action.data.vo.DeviceEntityDataVO;
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
	public static final String PORTAL_URL = "http://irricurb.dev.siliconmtn.com/json";
	
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
	protected static void testDeviceEntity() throws Exception {
		ProjectDeviceVO device = new ProjectDeviceVO();
		device.setProjectDeviceId("PRO_DEVICE_32");
		
		// Add the attributes to the data
		ProjectDeviceAttributeVO attr = new ProjectDeviceAttributeVO();
		attr.setDeviceAttributeId("COLOR_ROYGBIV");
		attr.setValue("Green");
		device.addAttribute(attr);
		
		attr = new ProjectDeviceAttributeVO();
		attr.setDeviceAttributeId("BRIGHT");
		attr.setValue("10");
		device.addAttribute(attr);
		
		attr = new ProjectDeviceAttributeVO();
		attr.setDeviceAttributeId("ENGAGE");
		attr.setValue("Off");
		device.addAttribute(attr);
		
		// Send the data to the portal
		Gson gson = new Gson();
		String json = gson.toJson(device);
		
		Map<String, Object> params = new HashMap<>();
		params.put("type", "DEVICE");
		params.put("data", json);
		params.put("amid", "data_rec");
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] res = conn.retrieveDataViaPost(PORTAL_URL, params);
		System.out.println(new String(res));
	}
	
	/**
	 * 
	 */
	protected static void testSensorEntity() throws Exception {
		DeviceDataVO data = new DeviceDataVO();
		data.setCreateDate(new Date());
		data.setReadingDate(new Date());
		data.setProjectDeviceId("PRO_DEVICE_28");
		
		DeviceEntityDataVO reading = new DeviceEntityDataVO();
		reading.setCreateDate(new Date());
		reading.setDeviceAttributeId("MOISTURE");
		reading.setReadingValue(.42);
		
		data.addReading(reading);
		
		Gson gson = new Gson();
		String json = gson.toJson(data);
		
		Map<String, Object> params = new HashMap<>();
		params.put("type", "SENSOR");
		params.put("data", json);
		params.put("amid", "data_rec");
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] res = conn.retrieveDataViaPost(PORTAL_URL, params);
		
		System.out.println(new String(res));
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
