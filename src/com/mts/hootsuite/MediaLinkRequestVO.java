package com.mts.hootsuite;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: MediaLinkRequestVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the body of a Media Link Request
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 18, 2020
 * @updates:
 ****************************************************************************/
public class MediaLinkRequestVO extends BeanDataVO {

	int sizeBytes;
	String mimeType;
	/**
	 * @return the sizeBytes
	 */
	public int getSizeBytes() {
		return sizeBytes;
	}
	/**
	 * @param sizeBytes the sizeBytes to set
	 */
	public void setSizeBytes(int sizeBytes) {
		this.sizeBytes = sizeBytes;
	}
	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}
	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
}
