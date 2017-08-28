package com.ram.action.order;

// JDK 1.7.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

//RAM Libs
import com.ram.action.report.vo.ProductOrderReport;
import com.ram.action.user.RAMRoleModule;
import com.ram.action.util.SecurityUtil;
import com.ram.datafeed.data.CustomerLocationVO;
import com.ram.workflow.data.vo.order.OrderLineItemVO;
import com.ram.workflow.data.vo.order.OrderVO;

// SMT Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.AbstractSBReportVO;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/********************************************************************
 * <b>Title: </b>OrderWidget.java<br/>
 * <b>Description: </b>Displays, manages, edits and adds ram orders<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Aug 21, 2017
 * Last Updated: 
 *******************************************************************/
public class OrderWidget extends SimpleActionAdapter {

	/**
	 * Session key for the order.  Only allowed one order at a time
	 */
	public static final String RAM_ORDER_KEY = "RAM_ORDER_KEY";
	
	/**
	 * 
	 */
	public OrderWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public OrderWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		// Get the start and end dates
		Date start = Convert.formatDate(new Date(), Calendar.MONTH, -1);
		Date end = new Date();
		if(req.hasParameter("from_date")) start = Convert.formatDate(req.getParameter("from_date"));
		if(req.hasParameter("to_date")) end = Convert.formatDate(req.getParameter("to_date"));
		
		// add the dates to the req object
		req.setParameter("from_date", Convert.formatDate(start, Convert.DATE_SLASH_PATTERN));
		req.setAttribute("to_date", Convert.formatDate(end, Convert.DATE_SLASH_PATTERN));
		
		if(req.hasParameter("pmid") && ! req.hasParameter("buildOrder")) {
			GenericVO data = getOrderList(req, start, end);
			putModuleData(data.getValue(), Convert.formatInteger(data.getKey() + ""), false);
		} else if(req.hasParameter("pmid") && req.hasParameter("buildOrder")) {
			OrderVO order = getOrder(req.getParameter("orderId"));
			AbstractSBReportVO report = new ProductOrderReport();
			report.setAttributes(attributes);
			report.setFileName("Order-" + order.getOrderId() + ".pdf");
			report.setData(order);
			req.setAttribute(Constants.BINARY_DOCUMENT, report);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		}
	}
	
	/**
	 * 
	 * @param orderId
	 * @return
	 */
	public OrderVO getOrder(String orderId) throws ActionException {
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(512);
		sql.append("select rc.gtin_number_txt || cast(rp.gtin_product_id as varchar(64)) as gtin_number_txt, * ");
		buildFullSQLBody(sql, orderId, params);

		// Get most of the order data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		log.debug("sql "+sql.toString());
		List<Object> data = db.executeSelect(sql.toString(), params, new OrderVO());
		if (data.isEmpty()) return null;
		
		// Get the location data
		OrderVO order = (OrderVO)data.get(0);

		try {
			order.setProviderLocation(getCustomerLocation(order.getCustomerLocationId()));
		} catch (Exception e) {
			throw new ActionException("Unable to retrieve orde", e);
		}
		
		return order;
	}
	
	/**
	 * Gets the customer location record
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public CustomerLocationVO getCustomerLocation(int id) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * ").append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_customer_location ");
		sql.append("where customer_location_id = ?");
		
		CustomerLocationVO loc = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, id);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) loc = new CustomerLocationVO(rs);
		}
		
		return loc;
	}
	
	/**
	 * Builds the body for the retrieval of all order data for a specific order
	 * @param sql
	 * @param orderId
	 * @param params
	 */
	public void buildFullSQLBody(StringBuilder sql, String orderId, List<Object> params) {
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_order a "); 
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_customer_location cl ");
		sql.append("on a.customer_location_id = cl.customer_location_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_order_status s ");
		sql.append("on a.order_status_cd = s.order_status_cd ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_user_role u ");
		sql.append("on a.user_role_id = u.user_role_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_order_line_item li ");
		sql.append("on a.order_id = li.order_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_product rp ");
		sql.append("on li.product_id = rp.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_customer rc ");
		sql.append("on rp.customer_id = rc.customer_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_order_line_item_receipt lir ");
		sql.append("on li.order_line_item_id = lir.order_line_item_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_order_shipment os ");
		sql.append("on lir.order_shipment_id = os.order_shipment_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("a.order_id = ? ");
		
		// Add the order id
		params.add(orderId);
	}
	
	/**
	 * Gets the list of orders and the counts
	 * @param req
	 * @return
	 */
	public GenericVO getOrderList(ActionRequest req, Date start, Date end) {
		StringBuilder sql = new StringBuilder(768);
		StringBuilder cSql = new StringBuilder(768);
		List<Object> params = new ArrayList<>();
		GenericVO data = new GenericVO();
		
		// Add the selects
		buildSQLSelect(sql);
		cSql.append("select count(*) as key ");
		
		// build the sql bodies
		buildSQLBody(sql);
		buildSQLBody(cSql);
		
		// Add the where clause
		buildSQLWhere(sql, req, params, start, end);
		buildSQLWhere(cSql, req, new ArrayList<Object>(), start, end);
		
		// Get the counts
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<Object> countVo = db.executeSelect(cSql.toString(), params, new GenericVO());
		data.setKey(((GenericVO)countVo.get(0)).getKey());
		
		// Get the order by and limits
		buildSQLOrder(sql, req, params);
		log.debug(sql + "|" + params);
		
		List<Object> items = db.executeSelect(sql.toString(), params, new OrderVO());
		data.setValue(items);
		
		return data;
	}
	
	/**
	 * Builds the sql clause
	 * @param sql
	 */
	public void buildSQLSelect(StringBuilder sql) {
		sql.append("select a.order_id, location_nm, a.create_dt, fulfilled_dt, cast(items_ordered_no as int) items_ordered_no, ");
		sql.append("cast(items_received_no as int) items_received_no, a.order_status_cd, user_role_id ");
	}
	
	/**
	 * Builds the from, joins and where
	 * @param sql
	 */
	public void buildSQLBody(StringBuilder sql) {
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_order a "); 
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_customer_location cl ");
		sql.append("on a.customer_location_id = cl.customer_location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(" ( select order_id, coalesce(sum(qty_no), 0) as items_ordered_no, ");
		sql.append("coalesce(sum(rec_qty_no), 0) as items_received_no ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("ram_order_line_item group by order_id ) as items ");
		sql.append("on a.order_id = items.order_id ");
		
	}
	
	/**
	 * Builds the sql filters
	 * @param sql
	 * @param req
	 * @param params
	 * @param start
	 * @param end
	 */
	public void buildSQLWhere(StringBuilder sql, ActionRequest req, List<Object> params, Date start, Date end) {
		sql.append(SecurityUtil.addCustomerFilter(req, "cl"));
		sql.append(DBUtil.WHERE_CLAUSE).append("a.create_dt between ? and ? ");
		
		// Add the date filters
		params.add(Convert.formatStartDate(start));
		params.add(Convert.formatEndDate(end));
		
		// Add filters for customers
		if(req.hasParameter("customerLocationId")) {
			sql.append("and a.customer_location_id = ? ");
			params.add(req.getIntegerParameter("customerLocationId"));
		}
		
		// add the status filters
		if(req.hasParameter("statusCode")) {
			sql.append("and a.order_status_cd = ? ");
			params.add(req.getParameter("statusCode"));
		}
	}
	
	/**
	 * Builds the order by and limit / offset features
	 * @param sql
	 * @param req
	 * @param params
	 */
	public void buildSQLOrder(StringBuilder sql, ActionRequest req, List<Object> params) {
		String sort = StringUtil.checkVal(DBUtil.getColumn(req.getParameter("sort"), new OrderVO(), ""), "create_dt");
		String order = StringUtil.checkVal(req.getParameter("order"), "desc");
		sql.append(" order by ").append(sort).append(" ").append(order).append(" limit ? offset ? ");
		
		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SMTSession ses = req.getSession();
		OrderVO order = (OrderVO)ses.getAttribute(RAM_ORDER_KEY);
		String type = req.getParameter("actionType");
		
		// Manage an Order
		if ("createOrder".equalsIgnoreCase(type)) {
			SBUserRole role = (SBUserRole)ses.getAttribute(Constants.ROLE_DATA);
			order = new OrderVO(req);
			
			order.setOrderId(RandomAlphaNumeric.generateRandom(6));
			
			order.setFulfillmentStrategyId(req.getParameter("fulfillmentStrategyId"));
			order.setUserRoleId(Convert.formatInteger(role.getAttribute(RAMRoleModule.USER_ROLE_ID) + ""));
			ses.setAttribute(RAM_ORDER_KEY, order);
			
			putModuleData(order);
		} else if ("manageProduct".equalsIgnoreCase(type)) {
			OrderLineItemVO lineItem = order.getLineItem(req.getParameter("lineItemId"));
			
			// If there is already a product in the order, update the quantity
			if (lineItem != null) {
				lineItem.setQtyNo(req.getIntegerParameter("qtyNo"));
			} else {
				lineItem = new OrderLineItemVO(req);
				lineItem.setLineItemId(new UUIDGenerator().getUUID());
				order.addLineItem(lineItem);
			}
			
			putModuleData(lineItem);
		} else if ("deleteProduct".equalsIgnoreCase(type)) {
			order.removeLineItem(req.getParameter("lineItemId"));
			putModuleData("Success");
			
		} else if ("save".equalsIgnoreCase(type) || "cancel".equalsIgnoreCase(type)) {
			try {
				if("save".equalsIgnoreCase(type)) saveOrder(order);
				ses.setAttribute(RAM_ORDER_KEY, null);
				putModuleData("Success");
				
			} catch(Exception e) {
				putModuleData(null, 0, false, "Unable to save Order", false);
			}
		} else if ("udpateStatus".equalsIgnoreCase(type)) {
			try {
				updateOrderStatus(req);
				putModuleData("SUCCESS");
			} catch(Exception e) {
				log.error("Unable to udpate status", e);
				putModuleData(null, 0, false, "Unable to update order status", true);
			}
		}
 	}
	
	/**
	 * Updates the status of an order
	 * @param req
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void updateOrderStatus(ActionRequest req) throws InvalidDataException, DatabaseException {
		List<String> fields = new ArrayList<>();
		fields.add("order_status_cd");
		fields.add("order_iD");
		
		OrderVO order = new OrderVO();
		order.setOrderId(req.getParameter("orderId"));
		order.setOrderStatusTxt(req.getParameter("orderStatusCd"));
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("ram_order set order_status_cd = ? where order_id = ?");
		log.info("SQL: " + sql + order.getOrderId() + "|" + order.getOrderStatusCd());
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.executeSqlUpdate(sql.toString(), order, fields);
	}
	
	/**
	 * 
	 * @param order
	 * @throws Exception
	 */
	public void saveOrder(OrderVO order) throws Exception {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.insert(order);
		
		Map<String, OrderLineItemVO> items = order.getLineItems();
		for(String key : items.keySet()) {
			OrderLineItemVO item = order.getLineItem(key);
			db.insert(item);
			
			// Make sure if there is no LIMI that it gets added
			log.info("LIMI: "+ item.getLocationItemMasterId());
		}
	}

}
