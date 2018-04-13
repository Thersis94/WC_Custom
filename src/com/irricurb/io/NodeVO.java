package com.irricurb.io;

// JDK 1.8.x
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: NodeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean holding a collection of nodes
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Dec 22, 2017
 * @updates:
 ****************************************************************************/
public class NodeVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4238883054437292727L;
	
	// Members
	private String id;
	private List<SensorVO> sensors = new ArrayList<>();

	/**
	 * 
	 */
	public NodeVO() {
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the sensors
	 */
	public List<SensorVO> getSensors() {
		return sensors;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param sensors the sensors to set
	 */
	public void setSensors(List<SensorVO> sensors) {
		this.sensors = sensors;
	}
	
	/**
	 * 
	 * @param sensor
	 */
	public void addSensor(SensorVO sensor) {
		sensors.add(sensor);
	}

}
