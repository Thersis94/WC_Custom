package com.depuysynthes.nexus;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title</b>: NexusKitLayerVO.java<p/>
 * <b>Description: </b> Holds information pertaining to a particular layer
 * of a kit.
 * <p/>
 * <b>Copyright:</b> (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 9, 2015
 ****************************************************************************/

public class NexusKitLayerVO implements Cloneable {

	private String layerId;
	private String layerName;
	private int orderNo;
	private List<NexusProductVO> products;
	private List<NexusKitLayerVO> sublayers;
	private String parentId;
	private String parentName;
	
	NexusKitLayerVO() {
		products = new ArrayList<>();
		sublayers = new ArrayList<>();
	}
	
	NexusKitLayerVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		layerId = req.getParameter("layerId");
		layerName = req.getParameter("layerNm");
		parentId = req.getParameter("parentId");
	}
	
	NexusKitLayerVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		layerId = db.getStringVal("LAYER_ID", rs);
		layerName = db.getStringVal("LAYER_NM", rs);
		parentId = db.getStringVal("PARENT_ID", rs);
		orderNo = db.getIntVal("ORDER_NO", rs);
	}
	
	public NexusKitLayerVO clone() throws CloneNotSupportedException {
		NexusKitLayerVO clone = new NexusKitLayerVO();
		clone.setLayerId(layerId);
		clone.setLayerName(layerName);
		clone.setOrderNo(orderNo);
		clone.setParentId(parentId);
		for(NexusKitLayerVO sublayer : sublayers) {
			clone.addLayer(sublayer.clone());
		}
		for (NexusProductVO product : products) {
			clone.addProduct(product.clone());
		}
	
		return clone;
	}

	public String getLayerId() {
		return layerId;
	}

	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public List<NexusProductVO> getProducts() {
		return products;
	}

	public void setProducts(List<NexusProductVO> products) {
		this.products = products;
	}
	
	public void addProduct(NexusProductVO product) {
		products.add(product);
	}

	public List<NexusKitLayerVO> getSublayers() {
		return sublayers;
	}

	public void setSublayers(List<NexusKitLayerVO> sublayers) {
		this.sublayers = sublayers;
	}
	
	public void addLayer(NexusKitLayerVO layer) {
		sublayers.add(layer);
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}
	
}
