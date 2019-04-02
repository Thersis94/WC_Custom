package com.wsla.data.ticket;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <b>Title</b>: LocationDistanceVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO Put Something Here
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Apr 1, 2019
 * @updates:
 ****************************************************************************/
public class LocationDistanceVO extends GenericVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6632397145561736821L;
	private double distance;

	/**
	 * @return the distance
	 */
	@Column(name="distance")
	public double getDistance() {
		return distance;
	}

	/**
	 * @param distance the distance to set
	 */
	public void setDistance(double distance) {
		this.distance = distance;
	}
}
