package com.irricurb.util;

// JDK 1.8.x
import java.util.Date;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.security.StringEncrypter;

/****************************************************************************
 * <b>Title</b>: ControllerUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utility helper for communicating to the controller
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since May 7, 2018
 * @updates:
 ****************************************************************************/

public class ControllerUtil {
	
	private static final Logger log = Logger.getLogger(ControllerUtil.class);
	
	/**
	 * 
	 */
	private ControllerUtil() {
		super();
	}
	
	/**
	 * Builds the security key to communicate to the on site controller
	 * @param encKey
	 * @param projectId
	 * @param portalIp
	 * @return
	 */
	public static String getSecurityKey(String encKey, String projectId, String portalIp) {
		String key = null;
		
		try {
			StringBuilder response = new StringBuilder(64);
			response.append(portalIp).append("|");
			response.append(projectId).append("|");
			response.append(new Date().getTime());
			
			// Encrypt the value
			StringEncrypter se = new StringEncrypter(encKey);
			key = se.encrypt(response.toString());
		} catch(Exception e) {
			log.error("Unable to generate security key");
		}
		
		return key;
	}

}

