package com.irricurb.server;

// JDK 1.8.x
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Log4j 1.2.17
import org.apache.log4j.Logger;

import com.irricurb.lookup.DeviceAttributeEnum;
// SMTBase Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.util.Convert;

//WC Libs
import com.smt.sitebuilder.common.constants.ErrorCodes;

/****************************************************************************
 * <b>Title</b>: LEDRingServer.java 
 * <b>Project</b>: Sandbox 
 * <b>Description: </b> Server to manage the LedLight Ring
 * <b>Copyright:</b> Copyright (c) 2018 
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 11, 2018
 * @updates:
 ****************************************************************************/

public class LEDRingServer {

	private static final int NUMBER_THREADS = 10;
	private static final Executor THREAD_POOL = Executors.newFixedThreadPool(NUMBER_THREADS);
	private static final int DEFAULT_PORT_NUMBER = 1234;
	public static final String LIGHTS_ACTIVE = "lightsActive";
	
	private static Logger log = Logger.getLogger(LEDRingServer.class);
	private boolean lightsActive = false;
	private boolean closeLights = false;
	private InetAddress address;
	private int port;
	
	/**
	 * 
	 * @param address
	 * @param port
	 * @throws UnknownHostException
	 */
	public LEDRingServer(String address, int port) throws UnknownHostException {
		this.address = InetAddress.getByName(address);
		this.port = port;
	}
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		LEDRingServer server = new LEDRingServer("0.0.0.0", DEFAULT_PORT_NUMBER);
		server.startServer();
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	public void startServer() throws IOException {
		log.info("Starting server on port: " + port);
		
		while (true) {
			try (ServerSocket serverSocket = new ServerSocket(port, 10, address)) {
				
				// Read the request and spawn a thread to handle the request
				final Socket connection = serverSocket.accept();
				Runnable task = () -> handleRequest(connection);
				THREAD_POOL.execute(task);
			}
		}
	}
	
	/**
	 * 
	 */
	private void ledRingManager() {
		lightsActive = true;
		while (true) {
			
			log.info("Inside Light Manager");
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				log.error("Unable to pause thread", e);
			}
			
			// Check to see if a request was received to turn off the manager
			if (closeLights) {
				log.info("Shutting lights off");
				lightsActive = false; 
				closeLights = false;
				break;
			}
		}
	}
	
	
	/**
	 * Parses the request and performs the requested action
	 * @param s
	 */
	private void handleRequest(Socket s) {
		
		// Manage the request and response
		RequestVO req = new RequestVO(s);
		ResponseHandler res = new ResponseHandler(s);
		
		// Build the Json Output
		Map<String, Object> resData = new HashMap<>();
		boolean success = true;
		String errorMsg = "";
		
		// Open the thread for managing the ring
		if (! lightsActive && Convert.formatBoolean(req.getParameter("start"))) {
			Runnable task = () -> ledRingManager();
			THREAD_POOL.execute(task);
		
		} else if (lightsActive && Convert.formatBoolean(req.getParameter("start"))) {
			success = false;
			errorMsg = ResponseHandler.ErrorCode.RING_RUNNING.getDisplay();

		} else if (lightsActive && Convert.formatBoolean(req.getParameter("stop"))) {
			closeLights = true;
			
		} else if (! lightsActive && Convert.formatBoolean(req.getParameter("stop"))) {
			success = false;
			errorMsg = ResponseHandler.ErrorCode.RING_STOPPED.getDisplay();
		
			
		} else if (Convert.formatBoolean(req.getParameter("status"))) {
			resData.put(DeviceAttributeEnum.ENGAGE.name(), lightsActive ? "On" : "Off");
			
		} else {
			success = false;
			errorMsg = ResponseHandler.ErrorCode.INVALID_DATA.getDisplay();
		}
		
		resData.put(GlobalConfig.SUCCESS_KEY, success);
		resData.put(ErrorCodes.ERR_JSON_ACTION, errorMsg);
		
		// Send the response
		try {
			res.sendResponse(resData);
		} catch (IOException e) {
			log.error("Failed respond to client request: ", e);
		}
		
		

		
		return;
	}
}
