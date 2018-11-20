package com.wsla.data.ticket;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.ProductVO;
import com.wsla.data.ticket.ShipmentVO.ShipmentStatus;

/****************************************************************************
 * <p><b>Title:</b> PartsVO.java</p>
 * <p><b>Description:</b> POJO correlating to the wsla_part table.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Nov 2, 2018
 * <b>Changes:</b>
 ****************************************************************************/
@Table(name="wsla_part")
public class PartVO extends ProductVO {

	private static final long serialVersionUID = 6778621308082091097L;

	private String partId;
	private String ticketId;
	private String shipmentId;
	private int quantity;
	private int quantityReceived;
	private int destEstQuantity;
	private int harvestedFlag;
	private String availabilityCode;
	private Date availabilityDate;

	private int quantityOnHand; //used for display, if negative we display "cas not assigned".  If >0 we display 'in stock'
	private ShipmentStatus shipmentStatus;

	public PartVO() {
		super();
	}

	public PartVO(ActionRequest req) {
		super(req);
	}

	@Column(name="part_id", isPrimaryKey=true)
	public String getPartId() {
		return partId;
	}

	@Override
	@Column(name="product_id")
	public String getProductId() {
		return super.getProductId();
	}

	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	@Column(name="shipment_id")
	public String getShipmentId() {
		return shipmentId;
	}

	@Column(name="quantity_no")
	public int getQuantity() {
		return quantity;
	}

	@Column(name="rcvd_qnty_no")
	public int getQuantityReceived() {
		return quantityReceived;
	}

	@Column(name="harvested_flg")
	public int getHarvestedFlag() {
		return harvestedFlag;
	}

	@Column(name="availability_cd")
	public String getAvailabilityCode() {
		return availabilityCode;
	}

	@Column(name="availability_dt")
	public Date getAvailabilityDate() {
		return availabilityDate;
	}

	@Column(name="actual_qnty_no", isReadOnly=true)
	public int getQuantityOnHand() {
		return quantityOnHand;
	}

	@Column(name="dest_actual_qnty_no", isReadOnly=true)
	public int getDestEstQuantity() {
		return destEstQuantity;
	}

	@Column(name="status_cd", isReadOnly=true)
	public ShipmentStatus getShipmentStatus() {
		return shipmentStatus;
	}



	public void setPartId(String partId) {
		this.partId = partId;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = StringUtil.checkVal(ticketId, null);
	}

	public void setShipmentId(String shipmentId) {
		this.shipmentId = StringUtil.checkVal(shipmentId, null);
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void setHarvestedFlag(int harvestedFlag) {
		this.harvestedFlag = harvestedFlag;
	}

	public void setAvailabilityCode(String availabilityCode) {
		this.availabilityCode = availabilityCode;
	}

	public void setAvailabilityDate(Date availabilityDate) {
		this.availabilityDate = availabilityDate;
	}

	public void setQuantityOnHand(int quantityOnHand) {
		this.quantityOnHand = quantityOnHand;
	}

	public void setQuantityReceived(int quantityReceived) {
		this.quantityReceived = quantityReceived;
	}

	public void setDestEstQuantity(int destEstQuantity) {
		this.destEstQuantity = destEstQuantity;
	}

	public void setShipmentStatus(ShipmentStatus status) {
		this.shipmentStatus = status;
	}
}