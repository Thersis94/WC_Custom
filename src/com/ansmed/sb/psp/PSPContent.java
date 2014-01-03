package com.ansmed.sb.psp;

import java.net.URLEncoder;

/****************************************************************************
 * <b>Title</b>PSPContent.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Dec 3, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PSPContent {
	private String contentText = null;
	private String contentName = null;
	private int column = 0;
	
	/**
	 * 
	 */
	public PSPContent() {
		
	}

	/**
	 * @return the contentText
	 */
	public String getContentText() {
		return contentText;
	}
	
	/**
	 * Returns the article as encoded text
	 * @return
	 */
	public String getEncodedContentText() {
		String s = "";
		try {
			s = URLEncoder.encode(contentText, "UTF-8");
		} catch(Exception e) {}
		
		return s;
	}

	/**
	 * @param contentText the contentText to set
	 */
	public void setContentText(String contentText) {
		this.contentText = contentText;
	}

	/**
	 * @return the column
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @param column the column to set
	 */
	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * @return the contentName
	 */
	public String getContentName() {
		return contentName;
	}

	/**
	 * @param contentName the contentName to set
	 */
	public void setContentName(String contentName) {
		this.contentName = contentName;
	}

	/**
	 * Returns the article as encoded text
	 * @return
	 */
	public String getEncodedContentName() {
		String s = "";
		try {
			s = URLEncoder.encode(contentName, "UTF-8");
		} catch(Exception e) {}
		
		return s;
	}
}
