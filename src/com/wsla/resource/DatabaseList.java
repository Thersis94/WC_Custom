package com.wsla.resource;

// Log4J 1.2.17
import org.apache.log4j.Logger;

// WC Libs
import com.smt.sitebuilder.resource.SMTBaseResourceBundle;

/****************************************************************************
 * <b>Title</b>: DatabaseList.java
 * <b>Project</b>: SMTBaseLibs
 * <b>Description: </b> Base Bundle Wrapper for the Database Bundle Approach
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 11, 2018
 * @updates:
 ****************************************************************************/

public class DatabaseList extends SMTBaseResourceBundle {
	/**
	 * Sets whether the class has been initialized
	 */
	private static boolean initialized = false;
	
	// Members
    protected static final Logger log = Logger.getLogger(DatabaseList.class);
    
	/**
	 * 
	 */
	public DatabaseList() {
		super();
		initialized = true;
	}

	
	/**
	 * Used by the caching mechanism to know if the bundle has been initialized
	 * @return
	 */
	public static boolean isInitialized() {
		return initialized;
	}

	
	/**
	 * Resets the initialized parameter after cache reset
	 * @param init
	 */
	public static void setInitialized(boolean init) {
		initialized = init;
	}
}

