package com.mts.hootsuite;

//JDK 1.8.x
import java.util.ArrayList;
import java.util.HashMap;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: SchedulePostResponseVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the Schedule Post Response
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 22, 2020
 * @updates:
 ****************************************************************************/
public class SchedulePostResponseVO extends BeanDataVO {

	ArrayList<SocialMediaProfileVO> data = new ArrayList<>();
	ArrayList<HashMap<String, String>> errors = new ArrayList<>();
	/**
	 * @return the data
	 */
	public ArrayList<SocialMediaProfileVO> getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(ArrayList<SocialMediaProfileVO> data) {
		this.data = data;
	}
	
	/**
	 * 
	 * @return the Id
	 */
	public String getId() {
		return data.get(0).getId();
	}

	/**
	 * @return the errors
	 */
	public ArrayList<HashMap<String, String>> getErrors() {
		return errors;
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(ArrayList<HashMap<String, String>> errors) {
		this.errors = errors;
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
