package com.irricurb.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

/****************************************************************************
 * <b>Title</b>: ResponseHandler.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> //TODO Change Me
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 13, 2018
 * @updates:
 ****************************************************************************/

public class ResponseHandler {
	
	// Member Variables
	private Socket socket;
	private Map<String, String> headers = new HashMap<>(8);
	protected static final Logger log = Logger.getLogger(ResponseHandler.class);
	
	/**
	 * 
	 */
	public ResponseHandler(Socket socket) {
		super();
		this.socket = socket;
		
		headers.put("","HTTP/1.0 200");
		headers.put("Content-type","application/json");
		headers.put("Server-name", "irriCURB LED Ring Server");
	}
	
	/**
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void sendResponse(Map<String, Object> message) throws IOException {
		Gson g = new Gson();
		String json = g.toJson(message);
		try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
			putHeader("Content-Length", json.length() + "");
			writeHeader(out);
			out.println("");
			out.println(json);
			out.flush();
		}
	}
		
	/**
	 * Writes the header to the print writer
	 * @param out
	 */
	private void writeHeader(PrintWriter out) {
		for (Map.Entry<String, String> kv : headers.entrySet()) {
			if (kv.getKey().isEmpty()) out.println(kv.getValue());
			else out.println(kv.getKey() + ":" + kv.getValue());
		}
	}
	
	/**
	 * Adds an entry to the header or updates an existing value
	 * @param key
	 * @param value
	 */
	public void putHeader(String key, String value) {
		headers.put(key, value);
	}
}

