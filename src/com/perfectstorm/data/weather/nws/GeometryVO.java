package com.perfectstorm.data.weather.nws;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: GeometryVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds the weather.gov geometry info
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class GeometryVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3733973898304151544L;
	
	// Members
	private String type;
	private double[][][] coordinates;

	/**
	 * 
	 */
	public GeometryVO() {
		super();
	}

	/**
	 * @param req
	 */
	public GeometryVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public GeometryVO(ResultSet rs) {
		super(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("type: ").append(type).append(", ");
		sb.append("coordinates: [");
		for (int a = 0; a < coordinates.length; a++) {
			for (int b = 0; b < coordinates[a].length; b++) {
				for (int c = 0; c < coordinates[a][b].length; c++) {
					if (c == 0) sb.append("[").append(coordinates[a][b][c]).append(",");
					else sb.append(coordinates[a][b][c]).append("]");
				}
			}
		}
		sb.append("]");
		
		return sb.toString();
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the coordinates
	 */
	public double[][][] getCoordinates() {
		return coordinates;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param coordinates the coordinates to set
	 */
	public void setCoordinates(double[][][] coordinates) {
		this.coordinates = coordinates;
	}

}

