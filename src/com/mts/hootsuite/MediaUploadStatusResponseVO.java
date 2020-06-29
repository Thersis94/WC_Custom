package com.mts.hootsuite;

//JDK 1.8.x
import java.util.HashMap;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: MediaUploadStatusResponseVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the Media Status Response
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 20, 2020
 * @updates:
 ****************************************************************************/
public class MediaUploadStatusResponseVO extends BeanDataVO {

	HashMap<String, String> data = new HashMap<>();
	
	public String getId() {
		return data.get("id");
	}
	
	public String getDownloadUrl() {
		return data.get("downloadUrl");
	}
	
	public String getDownloadUrlDurationSeconds() {
		return data.get("downloadUrlDurationSeconds");
	}
	
	public String getState() {
		return data.get("state");
	}
	
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
	
}
