package com.perfectstorm.data.weather.nws.detail;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: WeatherPointVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Main class for parsing weather point metadata
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 14, 2019
 * @updates:
 ****************************************************************************/

public class WeatherPointVO extends BeanDataVO {

	private static final long serialVersionUID = -2766935653628802200L;

	// Members
	PointPropertyVO properties;
	
	/**
	 * 
	 */
	public WeatherPointVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WeatherPointVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WeatherPointVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the properties
	 */
	public PointPropertyVO getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(PointPropertyVO properties) {
		this.properties = properties;
	}

}

