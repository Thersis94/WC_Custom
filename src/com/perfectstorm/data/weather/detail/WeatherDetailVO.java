package com.perfectstorm.data.weather.detail;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: WeatherDetailVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Main Class for parsing the Weather.gov detailed data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class WeatherDetailVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2779348970957645286L;
	
	// Members
	private GeometryVO geometry;
	private ForecastDetailVO properties;
	
	/**
	 * 
	 */
	public WeatherDetailVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WeatherDetailVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WeatherDetailVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the geometry
	 */
	public GeometryVO getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry the geometry to set
	 */
	public void setGeometry(GeometryVO geometry) {
		this.geometry = geometry;
	}

	/**
	 * @return the properties
	 */
	public ForecastDetailVO getProperties() {
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(ForecastDetailVO properties) {
		this.properties = properties;
	}

}

