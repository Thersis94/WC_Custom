package com.irricurb.io;

// JDK 1.8.x
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libsß
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: GatewayVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean managing gateways and their associated nodes and sensors
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Dec 22, 2017
 * @updates:
 ****************************************************************************/
public class GatewayVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1996227236278666325L;
	
	// Member Variables
	private String id;
	private boolean connected;
	private List<NodeVO> nodes = new ArrayList<>();
	
	/**
	 * 
	 */
	public GatewayVO() {
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
	 * @return the connected
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * @return the nodes
	 */
	public List<NodeVO> getNodes() {
		return nodes;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param connected the connected to set
	 */
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	/**
	 * @param nodes the nodes to set
	 */
	public void setNodes(List<NodeVO> nodes) {
		this.nodes = nodes;
	}
	
	/**
	 * 
	 * @param node
	 */
	public void addNode(NodeVO node) {
		nodes.add(node);
	}

}
