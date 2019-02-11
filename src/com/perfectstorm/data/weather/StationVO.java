package com.perfectstorm.data.weather;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;

/****************************************************************************
 * <b>Title</b>: FeatureVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds the features for a given Station
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class StationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2335111226188087914L;
	
	// Members
	private String type;
	private List<StationExtVO> features =  new ArrayList<>(16);

	/**
	 * 
	 */
	public StationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public StationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public StationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the features
	 */
	public List<StationExtVO> getFeatures() {
		return features;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param features the features to set
	 */
	public void setFeatures(List<StationExtVO> features) {
		this.features = features;
	}
	
	/**
	 * 
	 * @param feature
	 */
	@BeanSubElement
	public void addFeature(StationExtVO feature) {
		features.add(feature);
	}
}

