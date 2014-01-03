package com.fastsigns.cutover;

import com.siliconmtn.data.GenericVO;

/****************************************************************************
 * <b>Title</b>: TextModuleVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 21, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class TextModuleVO extends GenericVO {

	private static final long serialVersionUID = -5667256774649482181L;
	private String dataText = null;
	
	public TextModuleVO() {
	}

	/**
	 * @param key
	 * @param value
	 */
	public TextModuleVO(Object key, Object value) {
		super(key, value);
	}

	/**
	 * @return the dataText
	 */
	public String getDataText() {
		return dataText;
	}

	/**
	 * @param dataText the dataText to set
	 */
	public void setDataText(String dataText) {
		this.dataText = dataText;
	}
}
