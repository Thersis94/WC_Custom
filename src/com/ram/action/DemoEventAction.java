/**
 *
 */
package com.ram.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.AuditorVO;
import com.ram.datafeed.data.CustomerEventVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;
import com.ram.datafeed.data.InventoryEventGroupVO;
import com.ram.datafeed.data.InventoryEventReturnVO;
import com.ram.datafeed.data.InventoryEventVO;
import com.ram.workflow.data.vo.order.OrderLineItemReceiptVO;
import com.ram.workflow.data.vo.order.OrderLineItemVO;
import com.ram.workflow.data.vo.order.OrderShipmentVO;
import com.ram.workflow.data.vo.order.OrderVO;
import com.ram.workflow.data.vo.order.OrderVO.OrderStatus;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DemoEventAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> This action manages creation and deletion of a RAM
 * Demo Event that will be the same each time it is run.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 16, 2016
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class DemoEventAction extends SBActionAdapter {

	private DBProcessor db = null;
	private String schema = null;
	/**
	 * 
	 */
	public DemoEventAction() {
	}

	/**
	 * @param actionInit
	 */
	public DemoEventAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.list(req);
	}

	public void update(SMTServletRequest req) throws ActionException {
		schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		db = new DBProcessor(getDBConnection(), schema);
		
		/*
		 * If the request is a buildDemo request, forward to build, otherwise
		 * run normal update against SB_ACTION Record in super.update
		 */
		if(req.hasParameter("buildDemo")) {
			build(req);
		} else {
			super.update(req);
		}
	}

	public void build(SMTServletRequest req) throws ActionException {

		//Flush all existing Demo Data
		deleteDemoData();

		//Build New Demo Data
		addDemoData();
	}

	/**
	 * Delete all Demo Related records.  Need to get the EventID and the Auditor
	 * Id from the database first then execute a series of target deletions that
	 * will remove all the Demo Event Records before we insert new ones.
	 */
	public void deleteDemoData() {

		try {

			//Get the eventId for the Demo. 
			Integer eventId = getDemoEventId();
			if(eventId != null) {
				Integer auditorXRId = getDemoAuditorXrId(eventId);

				//delete receipts
				runDelete("delete from " + schema + "RAM_ORDER_LINE_ITEM_RECEIPT where ORDER_SHIPMENT_ID = ?", "DEMO_SHIPMENT");
				//delete shipment
				runDelete("delete from " + schema + "RAM_ORDER_SHIPMENT where ORDER_SHIPMENT_ID = ?", "DEMO_SHIPMENT");
				//delete line Items
				runDelete("delete from " + schema + "RAM_ORDER_LINE_ITEM where ORDER_ID = ?", "DEMO_ORDER");
				//delete order
				runDelete("delete from " + schema + "RAM_ORDER where ORDER_ID = ?", "DEMO_ORDER");
				//delete demo Customer XR
				runDelete("delete from " + schema + "RAM_CUSTOMER_EVENT_XR where INVENTORY_EVENT_ID = ?", eventId);
				//delete demo Return XR
				runDelete("delete from " + schema + "RAM_EVENT_RETURN_XR where INVENTORY_EVENT_ID = ?", eventId);
				//delete datafeed transaction
				runDelete("delete from " + schema + "RAM_DATAFEED_TRANSACTION where INVENTORY_EVENT_AUDITOR_XR_ID = ?", auditorXRId);
				//delete demo Event
				runDelete("delete from " + schema + "RAM_INVENTORY_EVENT where INVENTORY_EVENT_ID = ?", eventId);
				//delete demo Group
				runDelete("delete from " + schema + "RAM_INVENTORY_EVENT_GROUP where INVENTORY_EVENT_GROUP_ID= ?", "DEMO_EVENT");
			}
		} catch(SQLException e) {
			log.error("Exception occurred flushing old demo records.", e);
		}
	}

	/**
	 * Helper method that retrieves the Demo AuditorXr Id.
	 * @param eventId
	 * @return
	 * @throws SQLException 
	 */
	private Integer getDemoAuditorXrId(Integer eventId) throws SQLException {
		Integer auditorXRId = null;
		try(PreparedStatement ps = dbConn.prepareStatement(getDemoAuditorXrIdSql())) {
			ps.setInt(1, eventId);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				auditorXRId = rs.getInt("INVENTORY_EVENT_AUDITOR_XR_ID");
			}
		}
		return auditorXRId;
	}

	/**
	 * Helper method retrieves the Demo Event Id
	 * @return
	 * @throws SQLException 
	 */
	private Integer getDemoEventId() throws SQLException {
		Integer eventId = null;
		try(PreparedStatement ps = dbConn.prepareStatement(getDemoEventIdSql())) {
			ps.setString(1, "DEMO_EVENT");
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				eventId = rs.getInt("INVENTORY_EVENT_ID");
			}
		}
		return eventId;
	}

	/**
	 * Helper method that builds the getDemoAuditorXRID Query
	 * @return
	 */
	private String getDemoAuditorXrIdSql() {
		StringBuilder sql = new StringBuilder(175);
		sql.append("select INVENTORY_EVENT_AUDITOR_XR_ID from ");
		sql.append(schema).append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
		sql.append("where INVENTORY_EVENT_ID = ?");

		return sql.toString();
	}
	/**
	 * Returns the Demo Event Id Query.
	 * @return
	 */
	private String getDemoEventIdSql() {
		StringBuilder sql = new StringBuilder(125);
		sql.append("select INVENTORY_EVENT_ID from ").append(schema);
		sql.append("RAM_INVENTORY_EVENT where VENDOR_EVENT_ID = ?");
		return sql.toString();
	}

	/**
	 * Runs the given delete query with any parameters passed.
	 * @param query
	 * @param params
	 * @throws SQLException
	 */
	private void runDelete(String query, Object... params) throws SQLException {
		try(PreparedStatement ps = dbConn.prepareStatement(query)) {
			int cnt = 1;
			for(Object o : params) {
				if(o instanceof Integer) {
					ps.setInt(cnt++, (Integer) o);
				} else if(o instanceof String) {
					ps.setString(cnt++, (String) o);
				}
			}
			ps.executeUpdate();
		}
	}

	/**
	 * Add New Demo Records after all previous Demo Data has been deleted.
	 */
	public void addDemoData() {
		try {
			//Add Event Group
			addEventGroup();
			//Add Event
			InventoryEventVO e = addEvent();
			//Add Event Customers
			addEventCustomers(e);
			//Add Event Auditors
			addEventAuditor(e);
			//Add Event Returns
			addEventReturn(e);

			//Build Order and shipment Information.
			OrderVO o = buildOrder();
			OrderShipmentVO s = buildShipment(o);

			//Save Order Information
			insertRecord(o);
			for(OrderLineItemVO l : o.getLineItems().values()) {
				insertRecord(l);
			}

			//Save Shipment Information
			insertRecord(s);
			for(OrderLineItemReceiptVO r : s.getReceipts().values()) {
				insertRecord(r);
			}
		} catch(Exception e) {
			log.error(e);
		}
	}

	//Builds and Inserts the EventGroupVO.
	private void addEventGroup() throws InvalidDataException, DatabaseException {
		InventoryEventGroupVO v = new InventoryEventGroupVO();
		v.setInventoryEventGroupId("DEMO_EVENT");
		db.insert(v);
	}

	/**
	 * Helper method that adds and returns an Inventory Event.  We need the 
	 * inventoryEventId that is generated to build the rest of the tables. 
	 * @return
	 */
	private InventoryEventVO addEvent() {
		StringBuilder sql = new StringBuilder(275);
		sql.append("insert into ").append(schema).append("RAM_INVENTORY_EVENT ");
		sql.append("(INVENTORY_EVENT_GROUP_ID, CUSTOMER_LOCATION_ID, COMMENT_TXT, ");
		sql.append("SCHEDULE_DT, ACTIVE_FLG, VENDOR_EVENT_ID, CREATE_DT) ");
		sql.append("values (?,?,?,?,?,?,?) ");

		// Build Event
		InventoryEventVO e = new InventoryEventVO();
		e.setCustomerLocationId(175468001);
		e.setComment("DEMO_EVENT");
		e.setVendorEventId("DEMO_EVENT");
		e.setActiveFlag(1);
		e.setPartialInventoryFlag(0);
		e.setInventoryEventGroupId("DEMO_EVENT");

		// Insert Event
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, e.getInventoryEventGroupId());
			ps.setInt(2, e.getCustomerLocationId());
			ps.setString(3, e.getComment());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setInt(5, e.getActiveFlag());
			ps.setString(6, e.getVendorEventId());
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.executeUpdate();

			// Get the identity column id on an insert
			ResultSet generatedKeys = ps.getGeneratedKeys();
			if (generatedKeys.next()) {
				e.setInventoryEventId(generatedKeys.getInt(1));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		}

		return e;
	}

	/**
	 * Helper method that adds the Event Customers for the Demo
	 * @param event
	 */
	private void addEventCustomers(InventoryEventVO event) {
		// Generate base customerVO
		List<CustomerEventVO> cList = getCustomers(event.getInventoryEventId());

		// Insert record to database
		try(PreparedStatement ps = dbConn.prepareStatement(buildCustomerInsertRecord().toString())) {
			for(CustomerEventVO c : cList) {
				ps.setInt(1, c.getInventoryEventId());
				ps.setInt(2, c.getCustomerId()); // Depuy Orthopaedics
				ps.setInt(3, c.getActiveFlag());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error(sqle);
		}

		// store customer record on eventVO
		event.setEventCustomers(cList);
	}

	/**
	 * Helper method that builds a Customer List for the Demo.
	 * @param eventId
	 * @return
	 */
	private List<CustomerEventVO> getCustomers(int eventId) {
		List<CustomerEventVO> c = new ArrayList<>();
		c.add(getCustomer(eventId, 9230));
		c.add(getCustomer(eventId, 9000));
		c.add(getCustomer(eventId, 894785));
		c.add(getCustomer(eventId, 894786));

		return c;
	}

	/**
	 * Helper method that builds a Customer for the Demo
	 * 
	 * @param inventoryEventId
	 * @return
	 */
	private CustomerEventVO getCustomer(int inventoryEventId, int customerId) {
		CustomerEventVO c = new CustomerEventVO();
		c.setActiveFlag(1);
		c.setInventoryEventId(inventoryEventId);
		c.setCustomerId(customerId);
		return c;
	}

	/**
	 * This method builds and stores the InventoryEventAuditor record to the db
	 * and sets the resulting vo on the event.
	 * 
	 * @param groups
	 * @throws ActionException
	 */
	private void addEventAuditor(InventoryEventVO eventVO) throws ActionException {
		try(PreparedStatement ps = dbConn.prepareStatement(buildAuditorInsertRecord().toString(), Statement.RETURN_GENERATED_KEYS)) {
			// Generate basic random auditorVO
			InventoryEventAuditorVO auditor = getAuditor(eventVO.getInventoryEventId());

			// Store it to the db.
			ps.setInt(1, auditor.getInventoryEventId());
			ps.setInt(2, auditor.getAuditor().getAuditorId());
			ps.setInt(3, auditor.getEventLeaderFlag());
			ps.setInt(4, auditor.getActiveFlag());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();

			// Get the identity column id on an insert
			ResultSet generatedKeys = ps.getGeneratedKeys();
			if (generatedKeys.next()) {
				auditor.setInventoryEventAuditorId(generatedKeys.getInt(1));
			}

			// Add vo back to the eventVO.
			eventVO.addAuditor(auditor);
		} catch (SQLException sqle) {
			throw new ActionException("", sqle);
		}
	}

	/**
	 * This method generates a base AuditorVO for the Demo.
	 * 
	 * @return
	 */
	private InventoryEventAuditorVO getAuditor(int eventId) {
		// Create AuditorVO and assign random auditor from list above.
		AuditorVO a = new AuditorVO();
		a.setAuditorId(13);

		// build event AuditorVO and set auditor along with basic info.
		InventoryEventAuditorVO auditor = new InventoryEventAuditorVO();
		auditor.setInventoryEventId(eventId);
		auditor.setActiveFlag(1);
		auditor.setEventLeaderFlag(1);
		auditor.setAuditor(a);
		return auditor;
	}

	/**
	 * Method is responsible for building the return items, storing them in the
	 * database, updating the eventVO with the resulting list of values and also
	 * returning a list of the inventoryItems the returns are based on for use
	 * later in inserting the inventoryItem records.
	 * 
	 * @param groups
	 * @param prods
	 * @return
	 * @throws ActionException
	 */
	private void addEventReturn(InventoryEventVO eventVO) throws ActionException {
		try(PreparedStatement ps = dbConn.prepareStatement(buildEventReturnInsertRecord().toString(), Statement.RETURN_GENERATED_KEYS)) {
				InventoryEventReturnVO er = getReturn(eventVO.getInventoryEventId());

				ps.setInt(1, er.getInventoryEventId());
				ps.setInt(2, er.getProductId());
				ps.setInt(3, er.getQuantity());
				ps.setString(4, er.getLotNumber());
				ps.setInt(5, er.getActiveFlag());
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.executeUpdate();

				// Get the identity column id on an insert
				ResultSet generatedKeys = ps.getGeneratedKeys();
				if (generatedKeys.next())
					er.setEventReturnId(generatedKeys.getInt(1));

				eventVO.addReturnProduct(er);
		} catch (SQLException sqle) {
			throw new ActionException("", sqle);
		}
	}

	/**
	 * This method builds a generic eventReturnVO based off the given
	 * inventoryItemvO and eventId.
	 * 
	 * @param inventoryEventId
	 * @param inventoryItemVO
	 * @return
	 */
	private InventoryEventReturnVO getReturn(Integer inventoryEventId) {
		InventoryEventReturnVO er = new InventoryEventReturnVO();
		er.setInventoryEventId(inventoryEventId);
		er.setProductId(146451);
		er.setQuantity(1);
		er.setLotNumber("62562113");
		er.setActiveFlag(1);
		return er;
	}

	/**
	 * Helper method that builds the Demo Order and adds LineItems to it.
	 * @return
	 */
	private OrderVO buildOrder() {
		OrderVO o = new OrderVO();
		o.setOrderId("DEMO_ORDER");
		o.setOrderStatusCd(OrderStatus.SUBMITTED);
		o.setCustomerLocationId(175468001);
		o.setCustomerLocFstratId("CLOC_FSTRAT_9000");

		o.addLineItem(buildLineItem(1, 145101,1));
		o.addLineItem(buildLineItem(2, 147451,1));
		o.addLineItem(buildLineItem(3, 147454,1));
		return o;
	}

	/**
	 * Helper method that builds a LineItem for the Order
	 * @param pos
	 * @param productId
	 * @param qty
	 * @return
	 */
	private OrderLineItemVO buildLineItem(int pos, int productId, int qty) {
		OrderLineItemVO l = new OrderLineItemVO();
		l.setLineItemId("DEMO_EVENT_LINE_" + pos);
		l.setOrderId("DEMO_ORDER");
		l.setOrderNo(pos);
		l.setProductId(productId);
		l.setQtyNo(qty);
		return l;
	}

	/**
	 * Helper method that builds the Shipment for the Demo Order
	 * @param o
	 * @return
	 */
	private OrderShipmentVO buildShipment(OrderVO o) {
		OrderShipmentVO s = new OrderShipmentVO();
		s.setOrderShipmentId("DEMO_SHIPMENT");
		s.setOrderId("DEMO_ORDER");
		s.setShipmentStatus(OrderStatus.SUBMITTED);
		s.setAsnNoTXt("123456789");
		s.setTrackingNoTxt("123456789");
		s.setExpQtyNo(3);
		s.setReceipts(buildReceipts(o.getLineItems()));
		return s;
	}

	/**
	 * Helper method that builds the map of ShipmentLineItems for the Demo
	 * Shipment.
	 * @param map
	 * @return
	 */
	private Map<Integer, OrderLineItemReceiptVO> buildReceipts(Map<String, OrderLineItemVO> map) {
		Map<Integer, OrderLineItemReceiptVO> receipts = new HashMap<>();
		for(OrderLineItemVO l : map.values()) {
			receipts.put(l.getProductId(), buildReceipt(l));
		}

		return receipts;
	}

	/**
	 * Helper method builds a canned OrderLineItemReceiptVO based on given
	 * OrderLineItemVO
	 * @param i
	 * @return
	 */
	private OrderLineItemReceiptVO buildReceipt(OrderLineItemVO l) {
		OrderLineItemReceiptVO o = new OrderLineItemReceiptVO();
		o.setLineItemReceiptId(l.getLineItemId());
		o.setLineItemId(l.getLineItemId());
		o.setShipmentId("DEMO_SHIPMENT");
		o.setProductId(l.getProductId());
		o.setExpQtyNo(l.getQtyNo());
		return o;
	}

	/**
	 * Helper method that inserts a record via dbProcessor into the database. 
	 * @param r
	 */
	private void insertRecord(Object r) {
		try {
			db.insert(r);
		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	/**
	 * Builds the Customer insert statement
	 * 
	 * @return
	 */
	private StringBuilder buildCustomerInsertRecord() {
		StringBuilder sql = new StringBuilder(175);
		sql.append("insert into ").append(schema).append("RAM_CUSTOMER_EVENT_Xr ");
		sql.append("(INVENTORY_EVENT_ID, CUSTOMER_ID, ACTIVE_FLG, CREATE_DT) ");
		sql.append("values(?,?,?,?)");

		return sql;
	}

	/**
	 * Builds the Auditor insert statement
	 * 
	 * @return
	 */
	private StringBuilder buildAuditorInsertRecord() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(schema).append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
		sql.append("(INVENTORY_EVENT_ID, AUDITOR_ID, EVENT_LEADER_FLG, ");
		sql.append("ACTIVE_FLG, CREATE_DT) values(?,?,?,?,?)");

		return sql;
	}

	/**
	 * Builds the Event Return insert statement
	 * 
	 * @return
	 */
	private StringBuilder buildEventReturnInsertRecord() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(schema).append("RAM_EVENT_RETURN_XR ");
		sql.append("(INVENTORY_EVENT_ID, PRODUCT_ID, QUANTITY_NO, LOT_NUMBER_TXT, ");
		sql.append("ACTIVE_FLG, CREATE_DT) values(?,?,?,?,?,?)");

		return sql;
	}
}