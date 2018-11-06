package com.wsla.data.ticket;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> ShipmentVO.java</p>
 * <p><b>Description:</b> POJO for the wsla_shipment table.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Nov 2, 2018
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_shipment")
public class ShipmentVO extends BeanDataVO {

	private static final long serialVersionUID = -3355709194290850303L;

	private String shipmentId;
	private String shippedById; //is-a userId, the person who created the shipment
	private String fromLocationId; //is-a providerLocationId
	private String toLocationId; //is-a providerLocationId
	private CarrierType carrierType;
	private String trackingNo;
	private String commentsText;
	private Date shipmentDate;
	private Date createDate;
	private Date updateDate;

	private List<PartVO> parts;

	//JSTL
	private String fromLocationName;
	private String toLocationName;

	public enum CarrierType {
		FEDEX, UPS, DHL
	}

	public ShipmentVO() {
		super();
	}

	public ShipmentVO(ActionRequest req) {
		super(req);
	}

	@Column(name="shipment_id", isPrimaryKey=true)
	public String getShipmentId() {
		return shipmentId;
	}

	@Column(name="shipped_by_id")
	public String getShippedById() {
		return shippedById;
	}

	@Column(name="from_location_id")
	public String getFromLocationId() {
		return fromLocationId;
	}

	@Column(name="to_location_id")
	public String getToLocationId() {
		return toLocationId;
	}

	@Column(name="carrier_type_id")
	public CarrierType getCarrierType() {
		return carrierType;
	}

	@Column(name="carrier_tracking_no")
	public String getTrackingNo() {
		return trackingNo;
	}

	@Column(name="comments_txt")
	public String getCommentsText() {
		return commentsText;
	}

	@Column(name="shipment_dt")
	public Date getShipmentDate() {
		return shipmentDate;
	}

	@Column(name="from_location_nm", isReadOnly=true)
	public String getFromLocationName() {
		return fromLocationName;
	}

	@Column(name="to_location_nm", isReadOnly=true)
	public String getToLocationName() {
		return toLocationName;
	}

	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	public void setShipmentId(String shipmentId) {
		this.shipmentId = shipmentId;
	}

	public void setShippedById(String shippedById) {
		this.shippedById = StringUtil.checkVal(shippedById, null);
	}

	public void setFromLocationId(String fromLocationId) {
		this.fromLocationId = StringUtil.checkVal(fromLocationId, null);
	}

	public void setToLocationId(String toLocationId) {
		this.toLocationId = StringUtil.checkVal(toLocationId, null);
	}

	public void setCarrierType(CarrierType carrierType) {
		this.carrierType = carrierType;
	}

	public void setTrackingNo(String trackingNo) {
		this.trackingNo = trackingNo;
	}

	public void setCommentsText(String commentsText) {
		this.commentsText = commentsText;
	}

	public void setShipmentDate(Date shipmentDate) {
		this.shipmentDate = shipmentDate;
	}

	public List<PartVO> getParts() {
		return parts;
	}

	public void setParts(List<PartVO> parts) {
		this.parts = parts;
	}

	@BeanSubElement
	public void addPart(PartVO part) {
		if (parts == null) parts = new ArrayList<>();
		this.parts.add(part);
	}

	public void setFromLocationName(String fromLocationName) {
		this.fromLocationName = fromLocationName;
	}

	public void setToLocationName(String toLocationName) {
		this.toLocationName = toLocationName;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}