package com.depuysynthes.ifu;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/*****************************************************************************
 <b>Title: </b>IFULanguageVO.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since Oct 9, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class IFULanguageVO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2735159673969452930L;
	private String language;
	private String languageName;
	private Map<String,String> fieldMap;
	/**
	* Constructor
	*/
	public IFULanguageVO() {
		fieldMap = new HashMap<>();
	}
	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}
	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
	/**
	 * @return the languageName
	 */
	public String getLanguageName() {
		return languageName;
	}
	/**
	 * @param languageName the languageName to set
	 */
	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}
	/**
	 * @return the fieldMap
	 */
	public Map<String, String> getFieldMap() {
		return fieldMap;
	}
	/**
	 * @param fieldMap the fieldMap to set
	 */
	public void setFieldMap(Map<String, String> fieldMap) {
		this.fieldMap = fieldMap;
	}
	
	/**
	 * Convenience method for adding key/value pair to field map.
	 * @param key
	 * @param value
	 */
	
	public void addField(String key, String value) {
		if (fieldMap == null) fieldMap = new HashMap<>();
		if (key == null) return;
		fieldMap.put(key, value);
	}

}
