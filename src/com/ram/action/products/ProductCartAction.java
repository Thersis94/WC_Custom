package com.ram.action.products;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.data.ORKitVO;
import com.ram.action.report.vo.KitExcelReport;
import com.ram.action.report.vo.ProductCartReport;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>ProductCartAction.java<p/>
 * <b>Description: Handles product search and cart functionality for the 
 * ram site</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 ****************************************************************************/

public class ProductCartAction extends SBActionAdapter {

	// Names for the request parameters related to this action
	public static final String HOSPITAL = "hospital";
	public static final String ROOM = "room";
	public static final String SURGEON = "surgeon";
	public static final String TIME = "time";
	public static final String CASE_ID = "caseId";
	public static final String RESELLER = "reseller";
	public static final String BILLABLE = "billable";
	public static final String WASTED = "wasted";
	public static final String SALES_SIGNATURE = "sales";
	public static final String ADMIN_SIGNATURE = "admin";
	public static final String OTHER_ID = "other";
	public static final String PRODUCT_FROM = "productFrom";
	public static final String REP_ID = "repId";
	public static final String KIT_ID = "kitId";
	public static final String COMPLETE_DT = "formComplete";
	
	private enum SearchFields {
		productName("PRODUCT_NM"),
		productDesc("DESC_TXT"),
		productSKU("CUST_PRODUCT_ID"),
		productGTIN("c.GTIN_NUMBER_TXT + CAST(p.GTIN_PRODUCT_ID as NVARCHAR(64))"),
		surgeonName("SURGEON_NM"),
		hospital("HOSPITAL_NM"),
		repId("REP_ID"),
		caseId("CASE_ID");
		
		private String cloumnNm;
		SearchFields(String columnNm) {
			this.cloumnNm = columnNm;
		}
		
		public String getColumnName (){
			return cloumnNm;
		}
	}

	public ProductCartAction() {
		super();
	}

	public ProductCartAction(ActionInitVO avo) {
		super(avo);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("deleteKit")) {
			deleteCart(req);
		} else if (req.hasParameter("editAttr")) {
			editAttr(req);
			saveCart(req, 0);
		} else if(req.hasParameter("newCart")) {
			newKit(req);
		} else if (req.hasParameter("loadCart")){
			if (req.hasParameter("kitId")) {
				populateCart(req);
			} else {
				loadKits(req, 0);
			}
		} else if (Convert.formatBoolean(req.getParameter("finalize"))) {
			saveCart(req, 1);
			req.getSession().setAttribute("finalized", true);
			sendEmails(req);
		} else {
			editCart(req);
			saveCart(req, 0);
		}
	}
	
	
	/**
	 * Add the supplied parameter to the user's session.
	 * These values are sent along via post parameters in order to get around
	 * the character limit in get requests and allow 64-bit image strings to be
	 * added
	 * @param req
	 * @throws ActionException
	 */
	private void editAttr(SMTServletRequest req) throws ActionException {
		StringBuilder postParam = new StringBuilder();
		BufferedReader reader;
		try {
			reader = req.getReader();
			String line;
			while((line = reader.readLine()) != null) postParam.append(line);
			
			req.getSession().setAttribute(req.getParameter("editAttr"), postParam.toString());
		} catch (IOException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Deals with the various actions that a user can enact that affect their cart
	 * @param req
	 * @throws ActionException
	 */
	private void editCart(SMTServletRequest req) throws ActionException {
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		if (Convert.formatBoolean(req.getParameter("clearCart"))) {
			deleteItem(cart, req);
		} else  {
			List<GenericVO> addedItems = new ArrayList<>();
			for (int i = 0; i < req.getParameterValues("productId").length; i++) {
				String[] oldLots = req.getParameterValues("oldLot");
				String oldLot = (oldLots!=null && oldLots.length > i)? oldLots[i] : "";
				if (cart.getItems().containsKey(req.getParameterValues("productId")[i]+oldLot)) {
					editCartProduct(req, i, addedItems, oldLot, cart);
				} else {
					ShoppingCartItemVO item = buildProduct(req, i);
					addedItems.add(new GenericVO("insert", item));
					addItem(cart, item, oldLot);
				}

			}
			// Put the added products onto the request object so the page can
			// be updated accordingly.
			super.putModuleData(addedItems);
		}
		
		store.save(cart);
	}
	
	
	/**
	 * Edits a product in the cart with information from the request object
	 * @param req
	 * @param pos
	 * @param addedItems
	 * @param oldLot
	 * @param cart
	 */
	private void editCartProduct(SMTServletRequest req, int pos, List<GenericVO> addedItems, String oldLot, ShoppingCartVO cart) {
		ShoppingCartItemVO p = cart.getItems().get(req.getParameterValues("productId")[pos] + oldLot);
		p.setProductId(req.getParameterValues("productId")[pos] + req.getParameterValues("lotNo")[pos]);
		p.getProduct().getProdAttributes().put("lotNo", req.getParameterValues("lotNo")[pos]);
		p.getProduct().getProdAttributes().put(WASTED, req.getParameterValues(WASTED)[pos]);
		p.getProduct().getProdAttributes().put(BILLABLE, req.getParameterValues(BILLABLE)[pos]);
		p.getProduct().getProdAttributes().put(PRODUCT_FROM, req.getParameterValues(PRODUCT_FROM)[pos]);
		p.getProduct().getProdAttributes().put("oldId", p.getProduct().getProductId() + oldLot);
		int qty = Convert.formatInteger(req.getParameter("qty"),1);

		String[] edits = req.getParameterValues("editItem");
		boolean edit = edits!=null && edits.length > pos? Convert.formatBoolean(edits[pos]) : false;
		
		if (!edit)qty += p.getQuantity();
		p.setQuantity(qty > 99? 99:qty);
		cart.add(p);
		
		// Remove the old product if we have changed the lot no
		if (!oldLot.equals(p.getProduct().getProdAttributes().get("lotNo")) ) {
			cart.remove(p.getProduct().getProductId()+oldLot);
		}
		
		addedItems.add(new GenericVO("update", p));
	}
	
	
	/**
	 * Deletes the requested item or clears the cart completely
	 * @param cart
	 * @param req
	 */
	private void deleteItem(ShoppingCartVO cart, SMTServletRequest req) {
		// Determine whether we are deleting a single item or the entire cart
		if (req.hasParameter("productId")){
			cart.remove(req.getParameter("productId"));
		} else {
			cart.flush();
		}
		super.putModuleData(req.getParameter("productId"));
	}


	/**
	 * Create a single ShoppingCartItemVO from the request object
	 * @param req
	 * @return
	 */
	private ShoppingCartItemVO buildProduct(SMTServletRequest req, int pos) {
		ProductVO product = new ProductVO();
		
		product.setProductId(req.getParameterValues("productId")[pos]);
		product.setProductName(req.getParameterValues("productName")[pos]);
		product.addProdAttribute("customer", req.getParameterValues("customer")[pos]);
		product.addProdAttribute("gtin", req.getParameterValues("gtin")[pos]);
		product.addProdAttribute("lotNo", req.getParameterValues("lotNo")[pos]);
		product.addProdAttribute(BILLABLE, req.getParameterValues(BILLABLE)[pos]);
		product.addProdAttribute(WASTED, req.getParameterValues(WASTED)[pos]);
		product.addProdAttribute(PRODUCT_FROM, req.getParameterValues(PRODUCT_FROM)[pos]);
		ShoppingCartItemVO item = new ShoppingCartItemVO(product);
		item.setProductId(product.getProductId()+product.getProdAttributes().get("lotNo"));
		item.setQuantity(Convert.formatInteger(req.getParameter("qty"),1));
		
		return item;
	}


	/**
	 * Add the item on the request object to the cart, removing any old versions
	 * of that product and reordering them in order to ensure they are ordered
	 * by product id
	 * @param cart
	 * @param req
	 */
	private void addItem(ShoppingCartVO cart, ShoppingCartItemVO item, String oldLot) {
		cart.add(item);
		log.debug("Addded item with id of" + item.getProductId());
		log.debug("Cart now contains " + cart.getSize() + " items.");
		// Remove the old product if we have changed the lot no
		if (!oldLot.equals(item.getProduct().getProdAttributes().get("lotNo")) ) {
			cart.remove(item.getProduct().getProductId()+oldLot);
		}
		
		// Ensure that the map is properly ordered by product id
		List<String> sortedKeys = new ArrayList<String>(cart.getItems().keySet());
		Collections.sort(sortedKeys);
		Map<String, ShoppingCartItemVO> orderedCart = new LinkedHashMap<>();
		for (String key : sortedKeys) {
			orderedCart.put(key, cart.getItems().get(key));
		}
		cart.setItems(orderedCart);
	}



	/**
	 * Retrieves the Storage container
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private Storage retrieveContainer(SMTServletRequest req) 
			throws ActionException {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put(GlobalConfig.HTTP_REQUEST, req);
		attrs.put(GlobalConfig.HTTP_RESPONSE, attributes.get(GlobalConfig.HTTP_RESPONSE));
		attrs.put(GlobalConfig.KEY_DB_CONN, dbConn);
		
		Storage container = null;
		
		try {
			container = StorageFactory.getInstance(StorageFactory.SESSION_STORAGE, attrs);
		} catch (Exception ex) {
			throw new ActionException(ex);
		}
		return container;
	}



	public void retrieve(SMTServletRequest req) throws ActionException {
		// If a kitId has been passed along we need to load the kit first.
		if (req.hasParameter(KIT_ID))populateCart(req);
		
		// If no reseller name has been set we default to the logged in user's name
		if (req.getSession().getAttribute(RESELLER) == null) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			req.getSession().setAttribute(RESELLER, user.getFullName());
		}

		if (req.getSession().getAttribute("companies") == null) getCustomers(req);
		if (req.getSession().getAttribute("hospitals") == null) getHospitals(req);
		
		// Load the cart first since it is always needed
		ShoppingCartVO cart = retrieveContainer(req).load();
		req.setAttribute("cart", cart.getItems());
		
		if (req.hasParameter("exportKits")) {
			buildKitSummaryReport(req);
		}else if (req.hasParameter("buildFile")) {
			buildReport(req);
		} else if ("review".equals(req.getParameter("step"))) {
			loadKits(req, 1);
		} else if ("load".equals(req.getParameter("step")) && req.hasParameter("searchData")) {
			searchProducts(req);
		} else if (!"load".equals(req.getParameter("step"))) {
			loadKits(req, 0);
		}
	}
	
	
	/**
	 * Build an excel file with the all kits from the
	 * current search and add it to the request
	 * @param req
	 */
	private void buildKitSummaryReport(SMTServletRequest req) throws ActionException {
		loadKits(req, 1);
		AbstractSBReportVO report = new KitExcelReport();
		report.setData(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData());
		report.setFileName("kit_summary_report.xls");
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}
	
	
	/**
	 * Get a list of companies that produce parts 
	 * that can be found in the OR module.
	 * @param req
	 */
	private void getCustomers(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("SELECT CUSTOMER_ID, CUSTOMER_NM FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER WHERE GTIN_NUMBER_TXT is not null and GTIN_NUMBER_TXT != '' and CUSTOMER_TYPE_ID = ?");
		
		List<GenericVO> companies = new ArrayList<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "OEM");
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				companies.add(new GenericVO(rs.getString("CUSTOMER_ID"), rs.getString("CUSTOMER_NM")));
			}
		} catch (SQLException e) {
			log.error("Unable to get list of hospitals", e);
		} 
		req.getSession().setAttribute("companies", companies);
	}
	
	
	/**
	 * Get the valid hospitals that can be part of the OR module
	 * @param req
	 * @throws ActionException
	 */
	private void getHospitals(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("SELECT CUSTOMER_NM FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		// We will only ever need the provider customers
		sql.append("RAM_CUSTOMER WHERE CUSTOMER_TYPE_ID = 'PROVIDER' ");
		sql.append("ORDER BY CUSTOMER_NM DESC ");
		List<String> hospitals = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				hospitals.add(rs.getString("CUSTOMER_NM"));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		req.getSession().setAttribute("hospitals", hospitals);
	}
	
	
	/**
	 * Search for products that match the supplied search crteria
	 * @param req
	 * @throws ActionException
	 */
	private void searchProducts(SMTServletRequest req) throws ActionException {
		List<RAMProductVO> products = new ArrayList<>();
		String[] fields = req.getParameterValues("searchFields");
		int searchType = Convert.formatInteger(req.getParameter("searchType"), 1);
		// A search type greater than 2 means an exact search
		String sql = getProductSearchSQL(req, fields, searchType);
		int count = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			if (req.hasParameter("searchCustomer")) ps.setString(i++, req.getParameter("searchCustomer"));
			if (fields != null) {
				String searchData = req.getParameter("searchData");
				for (int j=0; j<fields.length; j++) {
					ps.setString(i++, (searchType == 3? "%":"") + searchData + (searchType > 1 ? "%":""));
				}
			}
			if (req.hasParameter("orgName")) ps.setString(i++, req.getParameter("orgName"));
			
			ResultSet rs = ps.executeQuery();
			
			int page = Convert.formatInteger(req.getParameter("page"), 0);
			int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
			while(rs.next()) {
				count++;
				if (count <= rpp*page || count > rpp*(page+1)) continue;
				products.add(new RAMProductVO(rs));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		super.putModuleData(products, count, false);
	}
	
	
	/**
	 * Build the sql for the product search
	 * @param req
	 * @param fields
	 * @param searchType
	 * @return
	 */
	private String getProductSearchSQL (SMTServletRequest req, String[] fields, int searchType) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String searchComaparator = searchType > 1? " like ":" = ";
		StringBuilder sql = new StringBuilder(300);
		sql.append("SELECT p.PRODUCT_ID, p.CUST_PRODUCT_ID, c.GTIN_NUMBER_TXT + CAST(p.GTIN_PRODUCT_ID as NVARCHAR(64)) as GTIN_NUMBER_TXT, PRODUCT_NM, ");
		sql.append("DESC_TXT, SHORT_DESC, c.CUSTOMER_NM FROM ").append(customDb).append("RAM_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_CUSTOMER c on c.CUSTOMER_ID = p.CUSTOMER_ID ");
		sql.append("WHERE p.CUSTOMER_ID is not null and p.GTIN_PRODUCT_ID is not null AND  c.GTIN_NUMBER_TXT is not null ");
		sql.append("AND p.CUSTOMER_ID != '' AND p.GTIN_PRODUCT_ID != '' AND c.GTIN_NUMBER_TXT != '' ");
		if (req.hasParameter("searchCustomer")) sql.append("AND p.CUSTOMER_ID = ? ");
		if (fields != null) {
			// Add fail condition to allow for multiple OR clauses
			sql.append("AND (1=2 ");
			// Loop over the selected search fields and add each one to the query
			for (String field : fields) {
				sql.append("OR ").append(SearchFields.valueOf(field).getColumnName()).append(searchComaparator).append("? ");
			}
			sql.append(") ");
		}
		if (req.hasParameter("orgName")) sql.append("AND p.CUSTOMER_ID = ? ");
		
		return sql.toString();
	}
	
	
	/**
	 * Build the requested report based off of the request servlet and the 
	 * shopping cart
	 * @param cart
	 * @param req
	 * @throws ActionException 
	 */
	private void buildReport(SMTServletRequest req) throws ActionException {
		ShoppingCartVO cart = retrieveContainer(req).load();
		AbstractSBReportVO report;
		String filename;
		String caseId = StringUtil.checkVal(req.getAttribute(CASE_ID));
		if (caseId.length() != 0) {
			filename = "case-" + caseId;
		} else {
			filename = "RAM-" + new SimpleDateFormat("YYYYMMdd").format(Convert.getCurrentTimestamp());;
		}
		report = new ProductCartReport();
		report.setFileName(filename + ".pdf");
		
		Map<String, Object> data = new HashMap<>();
		data.put("cart", cart.getItems());
		data.put(HOSPITAL,StringUtil.checkVal(req.getSession().getAttribute(HOSPITAL)));
		data.put(ROOM, StringUtil.checkVal(req.getSession().getAttribute(ROOM)));
		data.put(SURGEON, StringUtil.checkVal(req.getSession().getAttribute(SURGEON)));
		data.put(TIME, StringUtil.checkVal(req.getSession().getAttribute(TIME)));
		data.put(CASE_ID, StringUtil.checkVal(req.getSession().getAttribute(CASE_ID)));
		data.put(SALES_SIGNATURE, StringUtil.checkVal(req.getSession().getAttribute(SALES_SIGNATURE)));
		data.put(ADMIN_SIGNATURE, StringUtil.checkVal(req.getSession().getAttribute(ADMIN_SIGNATURE)));
		data.put(RESELLER, StringUtil.checkVal(req.getSession().getAttribute(RESELLER)));
		data.put(OTHER_ID, StringUtil.checkVal(req.getSession().getAttribute(OTHER_ID)));
		data.put(REP_ID, StringUtil.checkVal(req.getSession().getAttribute(REP_ID)));
		data.put(COMPLETE_DT, req.getSession().getAttribute(COMPLETE_DT));
		data.put("baseDomain", req.getHostName());
		data.put("format", req.getParameter("format"));
		
		report.setData(data);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}
	
	
	/**
	 * Delete a non finalized surgical case
	 * @param req
	 * @throws ActionException
	 */
	private void deleteCart(SMTServletRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(125);
		sql.append("DELETE ").append(customDb).append("RAM_KIT_INFO ");
		// We will only ever delete non finalized kits
		sql.append("WHERE FINALIZED_FLG = 0 AND RAM_KIT_INFO_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter(KIT_ID));
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		// Return the id of the kit that was deleted so that we know what to
		// remove from the list after the call completes
		super.putModuleData(req.getParameter(KIT_ID));
	}
	
	
	/**
	 * Save the current cart and all products associated with it.
	 * @param req
	 * @param finalizeCart
	 * @throws ActionException
	 */
	private void saveCart(SMTServletRequest req, int finalizeCart) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(225);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (StringUtil.checkVal(req.getSession().getAttribute(KIT_ID)).length() == 0) {
			sql.append("INSERT INTO ").append(customDb).append("RAM_KIT_INFO ");
			sql.append("(HOSPITAL_NM,OPERATING_ROOM,SURGERY_DT,SURGEON_NM,");
			sql.append("RESELLER_NM,CASE_ID,CREATE_DT,PROFILE_ID,FINALIZED_FLG,");
			sql.append("RESELLER_SIGNATURE,ADMIN_SIGNATURE,OTHER_ID,REP_ID,RAM_KIT_INFO_ID)");
			sql.append("VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			req.getSession().setAttribute(KIT_ID, new UUIDGenerator().getUUID());
		} else {
			sql.append("UPDATE ").append(customDb).append("RAM_KIT_INFO SET ");
			sql.append("HOSPITAL_NM=?,OPERATING_ROOM=?,SURGERY_DT=?,");
			sql.append("SURGEON_NM=?,RESELLER_NM=?,CASE_ID=?,UPDATE_DT=?, ");
			sql.append("PROFILE_ID=?, FINALIZED_FLG=?, RESELLER_SIGNATURE=?, ");
			sql.append("ADMIN_SIGNATURE=?,OTHER_ID=?,REP_ID=? WHERE RAM_KIT_INFO_ID=? ");
		}
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 1;
			ps.setString(i++, (String) req.getSession().getAttribute(HOSPITAL));
			ps.setString(i++, (String) req.getSession().getAttribute(ROOM));
			ps.setTimestamp(i++, Convert.formatTimestamp("MM-dd-yyyy -- hh:mm", (String) req.getSession().getAttribute(TIME)));
			ps.setString(i++, (String) req.getSession().getAttribute(SURGEON));
			ps.setString(i++, (String) req.getSession().getAttribute(RESELLER));
			ps.setString(i++, (String) req.getSession().getAttribute(CASE_ID));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, user.getProfileId());
			ps.setInt(i++, finalizeCart);
			ps.setString(i++, (String) req.getSession().getAttribute(SALES_SIGNATURE));
			ps.setString(i++, (String) req.getSession().getAttribute(ADMIN_SIGNATURE));
			ps.setString(i++, (String) req.getSession().getAttribute(OTHER_ID));
			ps.setString(i++, (String) req.getSession().getAttribute(REP_ID));
			ps.setString(i++, (String)req.getSession().getAttribute(KIT_ID));
			
			ps.executeUpdate();
			
			saveProducts(req, (String)req.getSession().getAttribute(KIT_ID));
			
		} catch (SQLException e) {
			req.getSession().setAttribute(KIT_ID,"");
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Get all products from the cart and save them to the database
	 * @param req
	 * @param kitId
	 * @throws ActionException
	 */
	private void saveProducts(SMTServletRequest req, String kitId) throws ActionException {
		// Delete all associated products to prevent duplicates
		purgeProducts(kitId);
		
		ShoppingCartVO cart = retrieveContainer(req).load();
		
		StringBuilder sql = new StringBuilder(175);
		sql.append("INSERT INTO ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_KIT_PRODUCT_XR (KIT_PRODUCT_ID, PRODUCT_ID,RAM_KIT_INFO_ID,ORDER_NO,LOT_NO,QTY,BILLABLE_FLG,WASTED_FLG,PRODUCT_FROM,CREATE_DT) ");
		sql.append("VALUES(?,?,?,?,?,?,?,?,?,?)");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i=1;
			for (String key : cart.getItems().keySet()) {
				ShoppingCartItemVO p = cart.getItems().get(key);
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setInt(2, Convert.formatInteger(p.getProduct().getProductId()));
				ps.setString(3, kitId);
				ps.setInt(4, i++);
				ps.setString(5, (String) p.getProduct().getProdAttributes().get("lotNo"));
				ps.setInt(6, p.getQuantity());
				ps.setInt(7, Convert.formatInteger(StringUtil.checkVal(p.getProduct().getProdAttributes().get(BILLABLE))));
				ps.setInt(8, Convert.formatInteger(StringUtil.checkVal(p.getProduct().getProdAttributes().get(WASTED))));
				ps.setString(9, (String) p.getProduct().getProdAttributes().get(PRODUCT_FROM));
				ps.setTimestamp(10, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Delete all products associated with the supplied kit
	 * @param kitId
	 * @throws ActionException
	 */
	private void purgeProducts(String kitId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("DELETE ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_KIT_PRODUCT_XR WHERE RAM_KIT_INFO_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kitId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Load all kits, potentially filtering them down based on supplied search criteria
	 * @param req
	 * @param finalized
	 */
	private void loadKits(SMTServletRequest req, int finalized) throws ActionException {
		List<ORKitVO> kits = new ArrayList<>();
		String sql = buildKitSearchSQL(req);
		int count = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			int i =1;
			ps.setString(i++, user.getProfileId());
			if (req.hasParameter("searchData") && req.hasParameter("searchType")) ps.setString(i++, "%" + req.getParameter("searchData")+ "%");
			if (req.hasParameter("startDate")) ps.setTimestamp(i++, Convert.getTimestamp(Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter("startDate")), false));
			if (req.hasParameter("endDate")) ps.setTimestamp(i++, Convert.getTimestamp(Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter("endDate")), false));
			
			ResultSet rs = ps.executeQuery();
			int page = Convert.formatInteger(req.getParameter("page"), 0);
			int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
			int start = page * rpp;
			int end = rpp * (page+1);
			boolean loadAll = Convert.formatBoolean(req.getParameter("loadAll"));
			while(rs.next()) {
				count++;
				if ((count <= start || count > end) && !loadAll) continue; 
				kits.add(new ORKitVO(rs));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		super.putModuleData(kits, count, false);
	}
	
	
	/**
	 * Build the kit search sql query
	 * @param req
	 * @return
	 */
	private String buildKitSearchSQL(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT HOSPITAL_NM, OPERATING_ROOM, SURGERY_DT, SURGEON_NM, REP_ID, OTHER_ID, CASE_ID, RESELLER_NM, k.RAM_KIT_INFO_ID, COUNT(k.RAM_KIT_INFO_ID) as NUM_PRODUCTS, FINALIZED_FLG ");
		sql.append("FROM ").append(customDb).append("RAM_KIT_INFO k ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_KIT_PRODUCT_XR xr ");
		sql.append("on k.RAM_KIT_INFO_ID = xr.RAM_KIT_INFO_ID ");
		sql.append("WHERE k.PROFILE_ID = ? ");
		if (req.hasParameter("searchData") && req.hasParameter("searchType")) sql.append("AND ").append(SearchFields.valueOf(req.getParameter("searchType")).getColumnName()).append(" like ? ");
		if (req.hasParameter("startDate")) sql.append("AND k.CREATE_DT > ? ");
		if (req.hasParameter("endDate")) sql.append("AND k.CREATE_DT < ? ");
		sql.append("GROUP BY HOSPITAL_NM, OPERATING_ROOM, SURGERY_DT, SURGEON_NM, CASE_ID, RESELLER_NM, k.RAM_KIT_INFO_ID, FINALIZED_FLG, REP_ID, OTHER_ID ");
		sql.append("ORDER BY FINALIZED_FLG ");
		
		return sql.toString();
	}
	
	
	/**
	 * Get all information and products associated with the current kit
	 * @param req
	 * @throws ActionException
	 */
	private void populateCart(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT * FROM ").append(customDb).append("RAM_KIT_INFO k ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_KIT_PRODUCT_XR xr ");
		sql.append("on k.RAM_KIT_INFO_ID = xr.RAM_KIT_INFO_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_PRODUCT p ");
		sql.append("on xr.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_CUSTOMER c ");
		sql.append("on c.CUSTOMER_ID = p.CUSTOMER_ID ");
		sql.append("WHERE k.PROFILE_ID = ? and k.RAM_KIT_INFO_ID = ?");
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		log.debug(sql+"|"+req.getParameter(KIT_ID)+"|"+user.getProfileId());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getProfileId());
			ps.setString(2, req.getParameter(KIT_ID));
			
			ResultSet rs = ps.executeQuery();

			Storage store = retrieveContainer(req);
			ShoppingCartVO cart = null;
			while(rs.next()) {
				if (cart == null) {
					cart = store.load();
					cart.flush();
					req.getSession().setAttribute(HOSPITAL, rs.getString("HOSPITAL_NM"));
					req.getSession().setAttribute(ROOM, rs.getString("OPERATING_ROOM"));
					req.getSession().setAttribute(SURGEON, rs.getString("SURGEON_NM"));
					if (rs.getTimestamp("SURGERY_DT") != null)
						req.getSession().setAttribute(TIME, new SimpleDateFormat("MM-dd-yyyy -- hh:mm").format(rs.getTimestamp("SURGERY_DT")));
					req.getSession().setAttribute(CASE_ID, rs.getString("CASE_ID"));
					req.getSession().setAttribute(KIT_ID, rs.getString("RAM_KIT_INFO_ID"));
					req.getSession().setAttribute("finalized", Convert.formatBoolean(rs.getString("FINALIZED_FLG")));
					req.getSession().setAttribute(RESELLER, rs.getString("RESELLER_NM"));
					req.getSession().setAttribute(SALES_SIGNATURE, rs.getString("RESELLER_SIGNATURE"));
					req.getSession().setAttribute(ADMIN_SIGNATURE, rs.getString("ADMIN_SIGNATURE"));
					req.getSession().setAttribute(OTHER_ID, rs.getString("OTHER_ID"));
					req.getSession().setAttribute(REP_ID, rs.getString("REP_ID"));
					if (rs.getTimestamp("UPDATE_DT") != null)
						req.getSession().setAttribute(COMPLETE_DT, new SimpleDateFormat("MM/dd/yyyy").format(rs.getTimestamp("UPDATE_DT")));
				}
				cart.add(buildProduct(rs));
			}
			
			store.save(cart);
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	
	}
	
	
	/**
	 * Build a product from data in the result set
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private ShoppingCartItemVO buildProduct(ResultSet rs) throws SQLException {
		ProductVO product = new ProductVO();
		product.setProductId(rs.getString("PRODUCT_ID"));
		product.setProductName(rs.getString("PRODUCT_NM"));
		product.addProdAttribute("customer", rs.getString("CUSTOMER_NM"));
		product.addProdAttribute("gtin",rs.getString("CUSTOMER_ID") + rs.getString("GTIN_PRODUCT_ID"));
		product.addProdAttribute("lotNo", StringUtil.checkVal(rs.getString("LOT_NO")));
		product.addProdAttribute(BILLABLE, rs.getInt("BILLABLE_FLG"));
		product.addProdAttribute(PRODUCT_FROM, rs.getString("PRODUCT_FROM"));
		product.addProdAttribute(WASTED, rs.getInt("WASTED_FLG"));
		ShoppingCartItemVO item = new ShoppingCartItemVO(product);
		item.setProductId(product.getProductId()+product.getProdAttributes().get("lotNo"));
		item.setQuantity(Convert.formatInteger(rs.getInt("QTY")));
		
		return item;
	}
	
	
	/**
	 * Flush all paramters used to store kit information, flush the cart,
	 * and save the empty cart in order to purge everything associated with
	 * the current cart.
	 * @param req
	 * @throws ActionException
	 */
	private void newKit(SMTServletRequest req) throws ActionException {
		req.getSession().removeAttribute(HOSPITAL);
		req.getSession().removeAttribute(ROOM);
		req.getSession().removeAttribute(SURGEON);
		req.getSession().removeAttribute(TIME);
		req.getSession().removeAttribute(CASE_ID);
		req.getSession().removeAttribute(KIT_ID);
		req.getSession().removeAttribute("finalized");
		req.getSession().removeAttribute(SALES_SIGNATURE);
		req.getSession().removeAttribute(ADMIN_SIGNATURE);
		req.getSession().removeAttribute(OTHER_ID);
		req.getSession().removeAttribute(REP_ID);
		req.getSession().removeAttribute(RESELLER);
		
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		cart.flush();
		store.save(cart);
	}
	
	
	/**
	 * Send emails to the representative and the hospital
	 * @param req
	 */
	private void sendEmails(SMTServletRequest req) {
		
		try {
			EmailMessageVO mail = new EmailMessageVO();
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			mail.addRecipient(user.getEmailAddress());
			mail.addRecipient(getHospitalEmail(req));
			if (req.getSession().getAttribute(CASE_ID) != null) {
				mail.setSubject("Product Summary for Case " + req.getSession().getAttribute(CASE_ID));
			} else {
				mail.setSubject("Product Summary for Surgery on " + new SimpleDateFormat("MM-dd-yyyy -- hh:mm").format(req.getSession().getAttribute(TIME)));
			}
			mail.setFrom("info@ramgrp.com");
			buildReport(req);
	
			AbstractSBReportVO report = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, false);
			mail.addAttachment(report.getFileName(), report.generateReport());
			mail.setHtmlBody("Placeholder text for now");
	
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (Exception e) {
			
		}
	}

	
	/**
	 * Get the hospital email address from the database
	 * @param req
	 * @return
	 */
	private String getHospitalEmail(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("SELECT CASE_CONTACT_EMAIL FROM ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		// We will only ever need the provider customers
		sql.append("RAM_CUSTOMER WHERE CUSTOMER_NM = ? ");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, (String) req.getSession().getAttribute(HOSPITAL));
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return rs.getString("CASE_CONTACT_EMAIL");
			}
		} catch (SQLException e) {
			log.error("Unable to get email for hospital");
		}
		return "";
	}
}
