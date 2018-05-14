package com.irricurb.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

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
	private static Logger log = Logger.getLogger(LEDRingServer.class);
	private static final int PORT_NUMBER = 1234;
	private static boolean shutdown = false;
	private static Queue<Boolean> workOrder = new ArrayBlockingQueue<>(8);
	private static boolean lightsRunning = false;
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.info("Starting server on port: " + PORT_NUMBER + "| Lights Running: " + lightsRunning);
		
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
		lightsRunning = true;
		int i = 1000;
		while (true) {
			
			log.info("Inside Light Manager");
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				log.error("Unable to pause thread", e);
			}
			
			// Check to see if a request was received to turn off the manager
			if (i == 100) {
				lightsRunning = false;
				break;
			}
		}
	}
	
	
	/**
	 * 
	 * @param s
	 */
	private static void handleRequest(Socket s) {

		try {
			// Manage the request and response
			RequestVO req = new RequestVO(s);
			ResponseHandler res = new ResponseHandler(s);
			
			// Build the HTML Output
			StringBuilder message = new StringBuilder(512);
			message.append("<html><head><title>My Web Server</title></head><body>");
			message.append("<h1>Welcome to my Web Server!</h1>").append(req.toString()).append("</html>");
			
			// Send the response
			res.sendResponse(message.toString());
			
			// Open the thread for managing the ring
			if (! lightsRunning) {
				log.info("Starting Lights");
				Runnable task = () -> ledRingManager();
				THREAD_POOL.execute(task);
			}
			
		} catch (IOException e) {
			log.error("Failed respond to client request: ", e);
		}
		
		return;
	}
}
