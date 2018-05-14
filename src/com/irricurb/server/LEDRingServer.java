package com.irricurb.server;

// JDK 1.8.x
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Log4j 1.2.17
import org.apache.log4j.Logger;

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
	private static final int PORT_NUMBER = 1234;
	public static final String LIGHTS_ACTIVE = "lightsActive";
	
	private static Logger log = Logger.getLogger(LEDRingServer.class);
	private static boolean shutdown = false;
	private static boolean lightsActive = false;
	private static boolean closeLights = false;
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.info("Starting server on port: " + PORT_NUMBER + "| Lights Running: " + lightsActive);
		
		while (true) {
			try (ServerSocket socket = new ServerSocket(PORT_NUMBER)) {
				// Read the request and spawn a thread to handle the request
				final Socket connection = socket.accept();
				Runnable task = () -> handleRequest(connection);
				THREAD_POOL.execute(task);
				
				// Look for the shutdown signal and close the server
				if (shutdown) break; 
			}
		}
	}
	
	/**
	 * 
	 */
	private static void ledRingManager() {
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
	 * 
	 * @param s
	 */
	private static void handleRequest(Socket s) {
		
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
		} else if (lightsActive && Convert.formatBoolean(req.getParameter("stop"))) {
			closeLights = true;
		} else {
			success = false;
			errorMsg = "Unable to Process Request, invalid data";
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
