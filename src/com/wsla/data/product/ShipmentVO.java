package com.wsla.data.product;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ShipmentVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for storing information about a shipment
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_shipment")
public class ShipmentVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 444528208968762263L;
	
	// Member Variables
	private String shipmentId;
	private String ticketId;
	private String shipToLocationId;
	private String fromLocationId;
	private String carrierTypeId;
	private String carrierTrackingNumber;
	private String shippedById;
	private String comments;
	private Date shipmentDate;
	private Date arrivalDate;
	private Date createDate;

	// Bean Sub-Elements
	private List<ShipmentItemVO> items = new ArrayList<>();
	
	/**
	 * 
	 */
	public ShipmentVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ShipmentVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ShipmentVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the shipmentId
	 */
	@Column(name="shipment_id", isPrimaryKey=true)
	public String getShipmentId() {
		return shipmentId;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the shipToLocationId
	 */
	@Column(name="ship_to_location_id")
	public String getShipToLocationId() {
		return shipToLocationId;
	}

	/**
	 * @return the fromLocationId
	 */
	@Column(name="from_location_id")
	public String getFromLocationId() {
		return fromLocationId;
	}

	/**
	 * @return the carrierTypeId
	 */
	@Column(name="carrier_type_id")
	public String getCarrierTypeId() {
		return carrierTypeId;
	}

	/**
	 * @return the carrierTrackingNumber
	 */
	@Column(name="carrier_tracking_number_txt")
	public String getCarrierTrackingNumber() {
		return carrierTrackingNumber;
	}

	/**
	 * @return the shippedById
	 */
	@Column(name="shipped_by_id")
	public String getShippedById() {
		return shippedById;
	}

	/**
	 * @return the comments
	 */
	@Column(name="comments")
	public String getComments() {
		return comments;
	}

	/**
	 * @return the shipmentDate
	 */
	@Column(name="shipment_dt")
	public Date getShipmentDate() {
		return shipmentDate;
	}

	/**
	 * @return the arrivalDate
	 */
	@Column(name="arrival_dt")
	public Date getArrivalDate() {
		return arrivalDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the items
	 */
	public List<ShipmentItemVO> getItems() {
		return items;
	}

	/**
	 * @param shipmentId the shipmentId to set
	 */
	public void setShipmentId(String shipmentId) {
		this.shipmentId = shipmentId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param shipToLocationId the shipToLocationId to set
	 */
	public void setShipToLocationId(String shipToLocationId) {
		this.shipToLocationId = shipToLocationId;
	}

	/**
	 * @param fromLocationId the fromLocationId to set
	 */
	public void setFromLocationId(String fromLocationId) {
		this.fromLocationId = fromLocationId;
	}

	/**
	 * @param carrierTypeId the carrierTypeId to set
	 */
	public void setCarrierTypeId(String carrierTypeId) {
		this.carrierTypeId = carrierTypeId;
	}

	/**
	 * @param carrierTrackingNumber the carrierTrackingNumber to set
	 */
	public void setCarrierTrackingNumber(String carrierTrackingNumber) {
		this.carrierTrackingNumber = carrierTrackingNumber;
	}

	/**
	 * @param shippedById the shippedById to set
	 */
	public void setShippedById(String shippedById) {
		this.shippedById = shippedById;
	}

	/**
	 * @param shipmentComments the shipmentComments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @param shipmentDate the shipmentDate to set
	 */
	public void setShipmentDate(Date shipmentDate) {
		this.shipmentDate = shipmentDate;
	}

	/**
	 * @param arrivalDate the arrivalDate to set
	 */
	public void setArrivalDate(Date arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param items the items to set
	 */
	@BeanSubElement
	public void setItems(List<ShipmentItemVO> items) {
		this.items = items;
	}
	
	/**
	 * 
	 * @param item
	 */
	public void addItem(ShipmentItemVO item) {
		this.items.add(item);
	}

}

