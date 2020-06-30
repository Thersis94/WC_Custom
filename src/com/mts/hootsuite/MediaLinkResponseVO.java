package com.mts.hootsuite;

//JDK 1.8.x
import java.util.HashMap;
import java.util.Map;

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
public class MediaLinkResponseVO extends HootsuiteResponseVO {

	HashMap<String, String> data = new HashMap<>();
	
	/**
	 * @return the data
	 */
	public Map<String, String> getData() {
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

	public boolean isSuccessfulRequest() {
		return data.get("uploadUrl") != null && data.get("id") != null && data.get("uploadUrlDurationSeconds") != null;
	}

}
