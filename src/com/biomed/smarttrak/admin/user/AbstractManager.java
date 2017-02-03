package com.biomed.smarttrak.admin.user;

import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;

/*****************************************************************************
 <p><b>Title</b>: AbstractManager.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AbstractManager {

	private Logger log = Logger.getLogger(UserManager.class);
	private Connection dbConn;
	private Map<String,Object> attributes;
	
	/**
	* Constructor
	*/
	public AbstractManager() {
		// constructor stub
	}

	/**
	 * @return the log
	 */
	protected Logger getLog() {
		return log;
	}

	/**
	 * @param log the log to set
	 */
	protected void setLog(Logger log) {
		this.log = log;
	}

	/**
	 * @return the dbConn
	 */
	protected Connection getDbConn() {
		return dbConn;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	protected void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @return the attributes
	 */
	protected Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	protected void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
