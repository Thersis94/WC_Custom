package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> WaveVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 13 2019
 * @updates:
 ****************************************************************************/

public class WaveVO extends BeanDataVO {

	private static final long serialVersionUID = 5815669228479458272L;
	
	
	
	
	public WaveVO() {
	}

	/**
	 * @param req
	 */
	public WaveVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WaveVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * Gets a map of all values
	 * @return
	 */
	public Map<String, Integer> getDataMap() {
		return new HashMap<>();
	}
}
