package com.irricurb.util;

import java.net.URLEncoder;
// JDK 1.8.x
import java.util.Date;

// Log4j 1.2.17
import org.apache.log4j.Logger;

import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.EncryptionException;
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
	 * Maximum amount of time in milliseconds for the request.  Times out the request key at 5 minutes
	 */
	public static final long REQ_MAX_TIME = 1000l * 60 * 5;
	
	/**
	 * 
	 */
	private ControllerUtil() {
		super();
	}
	
	/**
	 * Builds the security key to communicate to the on site controller
	 * @param encKey
	 * @param locationId
	 * @param portalIp
	 * @return URLEncoded Security Key
	 */
	public static String getSecurityKey(String encKey, String projectId, String ipAddr) {
		String key = null;
		
		try {
			StringBuilder response = new StringBuilder(64);
			response.append(ipAddr).append("|");
			response.append(projectId).append("|");
			response.append(new Date().getTime());
			
			// Encrypt the value
			StringEncrypter se = new StringEncrypter(encKey);
			key = se.encrypt(response.toString());
			key = URLEncoder.encode(key, "UTF-8");
		} catch (Exception e) {
			log.error("Unable to generate security key");
		}
		
		return key;
	}

	/**
	 * Decrypts the key to ensure the request is from the portal
	 * @param securityKey ip address of portal(requester) | project id | timestamp in long format (GMT) (within 1 hour)
	 * @return
	 * @throws EncryptionException
	 */
	public static void checkAuthorization(String encryptKey, String securityKey, String projectId, String reqIp, String endpointIp) 
	throws EncryptionException, AuthorizationException {
		if (securityKey == null || securityKey.isEmpty()) 
			throw new AuthorizationException(ICConstants.SECURITY_ERROR_RESPONSE);
		
		StringEncrypter se = new StringEncrypter(encryptKey);
		String value = se.decrypt(securityKey);
		
		// Get the decrypted values and parse the items in the list
		String[] vals = value.split("\\|");
		if (vals.length != 3) throw new AuthorizationException(ICConstants.SECURITY_ERROR_RESPONSE);
		log.info(vals[0] + "|" + vals[1] + "|" + vals[2] + "^^^" + reqIp + "|" + endpointIp);
		
		// Check the values to ensure they match
		// Compare the dates to ensure they are within REQ_MAX_TIME value.  Using get time (long) as it is already in 
		// GMT Format which takes into account the timezone differences
		if (! endpointIp.equals(vals[0]) && !vals[0].equals(reqIp) ||	// Check the ip matches the portal and matches the request IP
				! projectId.equalsIgnoreCase(vals[1]) || 	// Compare the requested Project Id to the controller project id
				Math.abs(Long.parseLong(vals[2]) - new Date().getTime()) > REQ_MAX_TIME) { // Make sure the request was made inside the value of REQ_MAX_TIME
			
			throw new AuthorizationException(ICConstants.SECURITY_ERROR_RESPONSE);
		}
	}
}

