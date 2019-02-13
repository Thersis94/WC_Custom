package com.perfectstorm.data.weather.detail;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: WeatherAttribueVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> The Weather.gov json format has a set of values for each 
 * attribute.  This class stores those values
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class WeatherAttribueVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7842794412177592990L;

	// Members
	private String sourceUnit;
	private String uom;
	private List<TimeValueVO> values;
	
	/**
	 * 
	 */
	public WeatherAttribueVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WeatherAttribueVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WeatherAttribueVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the sourceUnit
	 */
	public String getSourceUnit() {
		return sourceUnit;
	}

	/**
	 * @return the uom
	 */
	public String getUom() {
		return uom;
	}

	/**
	 * @return the values
	 */
	public List<TimeValueVO> getValues() {
		return values;
	}

	/**
	 * @param sourceUnit the sourceUnit to set
	 */
	public void setSourceUnit(String sourceUnit) {
		this.sourceUnit = sourceUnit;
	}

	/**
	 * @param uom the uom to set
	 */
	public void setUom(String uom) {
		this.uom = uom;
	}

	/**
	 * @param values the values to set
	 */
	public void setValues(List<TimeValueVO> values) {
		this.values = values;
	}

}

