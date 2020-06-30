package com.mts.hootsuite;

//JDK 1.8.x
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
public class SchedulePostResponseVO extends HootsuiteResponseVO {

	private List<SocialMediaProfileVO> data = new ArrayList<>();
	
	/**
	 * @return the data
	 */
	public List<SocialMediaProfileVO> getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(List<SocialMediaProfileVO> data) {
		this.data = data;
	}
	
	/**
	 * 
	 * @return the Id
	 */
	public String getId() {
		return data.get(0).getId();
	}
	
}
