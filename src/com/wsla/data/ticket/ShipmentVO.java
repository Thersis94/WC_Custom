package com.wsla.data.ticket;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

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
	private String ticketId;
	private ShipmentStatus status;
	private CarrierType carrierType;
	private String trackingNo;
	private String commentsText;
	private String purchaseOrder;
	private double cost;
	private Date shipmentDate;
	private Date arrivalDate;
	private Date createDate;
	private Date updateDate;

	private List<PartVO> parts;

	//JSTL
	private String fromLocationName;
	private String toLocationName;
	private String ticketIdText;

	public enum CarrierType {
		DHL, ESTAFETA, FEDEX, UPS
	}

	public enum ShipmentStatus {
		CREATED, BACKORDERED, SHIPPED, RECEIVED
	}

	public ShipmentVO() {
		super();
	}

	public ShipmentVO(ActionRequest req) {
		super(req);
		//set shippedBy to this user if blank
		if (StringUtil.isEmpty(getShippedById())) {
			UserDataVO userData = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			UserVO user = userData != null ? (UserVO) userData.getUserExtendedInfo() : null;
			if (user != null) setShippedById(user.getUserId());
		}
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

	@Column(name="arrival_dt")
	public Date getArrivalDate() {
		return arrivalDate;
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

	public List<PartVO> getParts() {
		return parts;
	}

	@Column(name="status_cd")
	public ShipmentStatus getStatus() {
		return status;
	}

	@Column(name="cost_no")
	public double getCost() {
		return cost;
	}

	@Column(name="purchase_order_txt")
	public String getPurchaseOrder() {
		return purchaseOrder;
	}

	@Column(name="ticket_no")
	public String getTicketIdText() {
		return ticketIdText;
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

	public void setParts(List<PartVO> parts) {
		this.parts = parts;
	}

	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	@BeanSubElement
	public void addPart(PartVO part) {
		if (parts == null) parts = new ArrayList<>();
		if (part == null || StringUtil.isEmpty(part.getPartId())) return; 
		parts.add(part);
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

	public void setStatus(ShipmentStatus status) {
		this.status = status;
	}

	public void setPurchaseOrder(String purchaseOrder) {
		this.purchaseOrder = purchaseOrder;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public void setTicketIdText(String ticketIdText) {
		this.ticketIdText = ticketIdText;
	}

	public void setArrivalDate(Date arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}
}