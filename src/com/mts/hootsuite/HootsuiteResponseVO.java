package com.mts.hootsuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: HootsuiteResponseVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Contains variable that are populated with error if a hootsuite request fails.
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since Jun 29, 2020
 * @updates:
 ****************************************************************************/
public class HootsuiteResponseVO extends BeanDataVO {

	private ArrayList<HashMap<String, String>> errors = new ArrayList<>();
	
	/**
	 * @return the errors
	 */
	public List<HashMap<String, String>> getErrors() {
		return errors;
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(List<HashMap<String, String>> errors) {
		this.errors = (ArrayList<HashMap<String, String>>) errors;
	}
	
	/**
	 * 
	 * @return the error message
	 */
	public String getErrorMessage() {
		String errorMessage = "";
		for(HashMap<String, String> error: errors) {
			errorMessage = errorMessage + " | " + "Error code: " + error.get("code") + ". Error Message: " + error.get("message");
		}
		return errorMessage;
	}
}
