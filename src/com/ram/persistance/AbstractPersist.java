package com.ram.persistance;

import java.util.Map;

import org.apache.log4j.Logger;

/****************************************************************************
 * <b>Title:</b> AbstractPersist.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 3, 2017
 ****************************************************************************/
public abstract class AbstractPersist<T, S> implements PersistenceIntfc<T, S> {

	//Pass Req/DB on attributes.
	protected Map<String, Object> attributes;
	protected Logger log;

	public AbstractPersist() {
		log = Logger.getLogger(getClass());
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set.
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
