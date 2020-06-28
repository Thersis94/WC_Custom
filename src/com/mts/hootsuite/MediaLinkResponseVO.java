package com.mts.hootsuite;

//JDK 1.8.x
import java.util.HashMap;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: MediaLinkResponseVO.java <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the Media Link Response <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 18, 2020
 * @updates:
 ****************************************************************************/
public class MediaLinkResponseVO extends BeanDataVO {

	HashMap<String, String> data = new HashMap<>();
	private String error;
	private String error_description;
	
	/**
	 * @return the data
	 */
	public HashMap<String, String> getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(HashMap<String, String> data) {
		this.data = data;
	}

	/**
	 * 
	 * @return the upload url
	 */
	public String getUploadUrl() {
		return data.get("uploadUrl");
	}

	/**
	 * 
	 * @return the upload request ID
	 */
	public String getId() {
		return data.get("id");
	}

	/**
	 * 
	 * @return the number of seconds before the upload link expires
	 */
	public int getUploadDurationSeconds() {
		return Integer.parseInt(data.get("uploadUrlDurationSeconds"));
	}

	public boolean successfulRequest() {
		if (data.get("uploadUrl") != null && data.get("id") != null && data.get("uploadUrlDurationSeconds") != null) {
			return true;
		} else
			return false;
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * @return the error_description
	 */
	public String getError_description() {
		return error_description;
	}

	/**
	 * @param error_description the error_description to set
	 */
	public void setError_description(String error_description) {
		this.error_description = error_description;
	}

}
