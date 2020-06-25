package com.mts.hootsuite;

import java.util.List;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: URLShortenerResponseVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO that contains the response from the bitly API
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since Jun 23, 2020
 * @updates:
 ****************************************************************************/
public class URLShortenerResponseVO {
	private String link;
	private String message = "";
	private Object errors;
	private String resource;
	private String description;

	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}

	/**
	 * @param link the link to set
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the resource
	 */
	public String getResource() {
		return resource;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the errors
	 */
	public Object getErrors() {
		return errors;
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(Object errors) {
		this.errors = errors;
	}
}
