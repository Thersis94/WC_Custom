package com.ram.action.products;

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

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.report.vo.ProductCartReport;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>ProductCartAction.java<p/>
 * <b>Description: Handles cart functionality for the ram site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 * 		June 30, 2017 - Moved case search functionality to CaseSearchAction
 ****************************************************************************/

public class ProductCartAction extends SimpleActionAdapter {

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
	public static final String SALES_SIGNATURE_DT = "salesDt";
	public static final String ADMIN_SIGNATURE = "admin";
	public static final String ADMIN_SIGNATURE_DT = "adminDt";
	public static final String OTHER_ID = "other";
	public static final String PRODUCT_FROM = "productFrom";
	public static final String REP_ID = "repId";
	public static final String KIT_ID = "kitId";
	public static final String COMPLETE_DT = "formComplete";
	public static final String FINALIZED = "finalized";
	public static final String LOT_NO = "lotNo";
	public static final String PRODUCT_SOURCE = "productSource";
	public static final String DATE_PATTERN = "MM-dd-yyyy -- hh:mm";
	public static final String SIGN_DATE_PATTERN = "MM/dd/yyyy hh:mm";
	
	private enum SearchFields {
		productName("PRODUCT_NM"),
		customerName("c.CUSTOMER_NM"),
		gtinProductId("c.GTIN_NUMBER_TXT || CAST(p.GTIN_PRODUCT_ID as VARCHAR(64))");
		
		private String cloumnNm;
		
		SearchFields(String columnNm) {
			this.cloumnNm = columnNm;
		}
		
		public String getColumnName (){
			return cloumnNm;
		}
	}
	
	/**
	 * Build actions this widget can perform, sent by the request.
	 * 
	 */
	private enum WidgetBuildAction {saveCaseInfo, addProduct, deleteProduct, addSignature, finalize, sendEmails}
	
	/**
	 * Retrieve actions this widget can perform, sent by the request.
	 */
	private enum WidgetRetrieveAction {loadCase, loadReport, searchProducts}
	
	
	public ProductCartAction() {
		super();
	}

	/**
	 * @param avo
	 */
	public ProductCartAction(ActionInitVO avo) {
		super(avo);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
		WidgetBuildAction wa = WidgetBuildAction.valueOf(req.getParameter("widgetAction"));

		try {
			switch (wa) {
				case saveCaseInfo:
					RAMCaseVO cvo = rcm.saveCase(req);
					putModuleData(cvo);
					break;
				case addProduct:
					RAMCaseItemVO civo = rcm.updateItem(req);
					putModuleData(civo);
					break;
				case deleteProduct:
					Integer productId = rcm.removeCaseItem(req);
					putModuleData(productId);
					break;
				case addSignature:
					rcm.addSignature(req);
					break;
				case finalize:
					rcm.finalizeCaseInfo();
					UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
					req.setParameter("emails", user.getEmailAddress());
					sendEmails(req);
					break;
				case sendEmails:
					sendEmails(req);
					break;
			}
		} catch (Exception e) {
			log.error("Error managing case", e);
			throw new ActionException(e);
		}
	}

	/**
	 * Deals with the various actions that a user can enact that affect their cart
	 * @param req
	 * @throws ActionException
	 */
	private void editCart(ActionRequest req) throws ActionException {
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		if (Convert.formatBoolean(req.getParameter("clearCart"))) {
			deleteItem(cart, req);
		} else {
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
	private void editCartProduct(ActionRequest req, int pos, List<GenericVO> addedItems, String oldLot, ShoppingCartVO cart) {
		ShoppingCartItemVO p = cart.getItems().get(req.getParameterValues("productId")[pos] + oldLot);
		Map<String, Object> prodAttributes = p.getProduct().getProdAttributes();
		p.setProductId(req.getParameterValues("productId")[pos] + req.getParameterValues(LOT_NO)[pos]);
		prodAttributes.put(LOT_NO, req.getParameterValues(LOT_NO)[pos]);
		prodAttributes.put(WASTED, req.getParameterValues(WASTED)[pos]);
		prodAttributes.put(BILLABLE, req.getParameterValues(BILLABLE)[pos]);
		prodAttributes.put(PRODUCT_FROM, req.getParameterValues(PRODUCT_FROM)[pos]);
		prodAttributes.put(PRODUCT_SOURCE, req.getParameterValues(PRODUCT_SOURCE)[pos]);
		prodAttributes.put("oldId", p.getProduct().getProductId() + oldLot);
		int qty = Convert.formatInteger(req.getParameter("qty"),1);

		String[] edits = req.getParameterValues("editItem");
		boolean edit = edits!=null && edits.length > pos? Convert.formatBoolean(edits[pos]) : false;
		
		if (!edit)qty += p.getQuantity();
		p.setQuantity(qty > 99? 99:qty);
		cart.add(p);
		
		// Remove the old product if we have changed the lot no
		if (!oldLot.equals(p.getProduct().getProdAttributes().get(LOT_NO)) ) {
			cart.remove(p.getProduct().getProductId()+oldLot);
		}
		
		addedItems.add(new GenericVO("update", p));
	}
	
	
	/**
	 * Deletes the requested item or clears the cart completely
	 * @param cart
	 * @param req
	 */
	private void deleteItem(ShoppingCartVO cart, ActionRequest req) {
		// Determine whether we are deleting a single item or the entire cart
		if (req.hasParameter("productId")){
			cart.remove(req.getParameter("productId"));
			super.putModuleData(req.getParameter("productId"));
		} else {
			cart.flush();
		}
	}


	/**
	 * Create a single ShoppingCartItemVO from the request object
	 * @param req
	 * @return
	 */
	private ShoppingCartItemVO buildProduct(ActionRequest req, int pos) {
		ProductVO product = new ProductVO();

		product.setProductId(req.getParameterValues("productId")[pos]);
		product.setProductName(req.getParameterValues("productName")[pos]);
		product.addProdAttribute("customer", req.getParameterValues("customer")[pos]);
		product.addProdAttribute("gtin", req.getParameterValues("gtin")[pos]);
		product.addProdAttribute(LOT_NO, req.getParameterValues(LOT_NO)[pos]);
		product.addProdAttribute(BILLABLE, req.getParameterValues(BILLABLE)[pos]);
		product.addProdAttribute(WASTED, req.getParameterValues(WASTED)[pos]);
		product.addProdAttribute(PRODUCT_FROM, req.getParameterValues(PRODUCT_FROM)[pos]);
		product.addProdAttribute(PRODUCT_SOURCE, req.getParameterValues(PRODUCT_SOURCE)[pos]);
		ShoppingCartItemVO item = new ShoppingCartItemVO(product);
		item.setProductId(product.getProductId()+product.getProdAttributes().get(LOT_NO));
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
		if (!oldLot.equals(item.getProduct().getProdAttributes().get(LOT_NO)) ) {
			cart.remove(item.getProduct().getProductId()+oldLot);
		}
		
		// Ensure that the map is properly ordered by product id
		List<String> sortedKeys = new ArrayList<>(cart.getItems().keySet());
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
	private Storage retrieveContainer(ActionRequest req) 
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


	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
		WidgetRetrieveAction wa = WidgetRetrieveAction.valueOf(req.getParameter("widgetAction"));
		String caseId = req.getParameter("caseId");
		log.debug("widget retrieve action: " + wa);
		try {
			switch (wa) {
				case loadCase:
					RAMCaseVO cvo = rcm.retrieveCase(caseId);
					putModuleData(cvo);
					break;
				case loadReport:
					buildReport(req);
					break;
				case searchProducts:
					if (!StringUtil.isEmpty(req.getParameter("search")))
						searchProducts(req);
					break;
			}
		} catch (Exception e) {
			log.error("Error retrieving case", e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Search for products that match the supplied search crteria
	 * @param req
	 * @throws ActionException
	 */
	private void searchProducts(ActionRequest req) throws ActionException {
		List<RAMProductVO> products = new ArrayList<>();
		String searchData = StringUtil.checkVal(req.getParameter("search")).toLowerCase();
		String sql = getProductSearchSQL(req);
		int count = 0;

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int i = 1;
			for (int j = 0; j < 3; j++) {
				ps.setString(i++, "%" + searchData + "%");
			}
			
			ResultSet rs = ps.executeQuery();
			int page = Convert.formatInteger(req.getParameter("offset"), 0);
			int rpp = Convert.formatInteger(req.getParameter("limit"));
			rpp = rpp == 0 ? 10 : rpp;
			
			while(rs.next()) {
				count++;
				if (count <= rpp*page || count > rpp*(page+1)) continue;
				RAMProductVO p = new RAMProductVO(rs);
				// Kits with layer ids can either come from kits or as a single
				// product and must be marked as such.
				if (!StringUtil.checkVal(rs.getString("KIT_LAYER_ID")).isEmpty())
					p.setKitFlag(1);
				products.add(p);
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
	private String getProductSearchSQL (ActionRequest req) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);

		sql.append("SELECT p.PRODUCT_ID, p.CUST_PRODUCT_ID, c.GTIN_NUMBER_TXT || CAST(p.GTIN_PRODUCT_ID as VARCHAR(64)) as GTIN_NUMBER_TXT, PRODUCT_NM, ");
		sql.append("DESC_TXT, SHORT_DESC, c.CUSTOMER_NM, l.KIT_LAYER_ID FROM ").append(customDb).append("RAM_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_CUSTOMER c on c.CUSTOMER_ID = p.CUSTOMER_ID ");
		sql.append("left join ").append(customDb).append("RAM_KIT_LAYER l on l.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("WHERE p.CUSTOMER_ID is not null and p.GTIN_PRODUCT_ID is not null AND  c.GTIN_NUMBER_TXT is not null ");
		sql.append("AND p.CUSTOMER_ID > 0 AND p.GTIN_PRODUCT_ID != '' AND c.GTIN_NUMBER_TXT != '' ");

		sql.append("AND (");
		sql.append("lower(PRODUCT_NM) like ? ");
		sql.append("OR lower(CUST_PRODUCT_ID) like ? ");
		sql.append("OR lower(c.GTIN_NUMBER_TXT || CAST(p.GTIN_PRODUCT_ID as VARCHAR(64))) like ? ");
		sql.append(") ");
		
		sql.append("ORDER BY ");
		
		if (req.hasParameter("sort")) {
			sql.append(SearchFields.valueOf(req.getParameter("sort")).getColumnName());
			
			String order = StringUtil.checkVal(req.getParameter("order"));
			if ("desc".equalsIgnoreCase(order)) {
				sql.append(" DESC ");
			} else {
				sql.append(" ASC ");
			}
		} else {
			sql.append("PRODUCT_NM ");
		}

		log.debug(sql);
		return sql.toString();
	}
	
	
	/**
	 * Build the requested report based off of the request servlet and the 
	 * shopping cart
	 * @param cart
	 * @param req
	 * @throws ActionException 
	 */
	private void buildReport(ActionRequest req) throws ActionException {
		ShoppingCartVO cart = retrieveContainer(req).load();
		AbstractSBReportVO report;
		String filename;
		String caseId = StringUtil.checkVal(req.getAttribute(CASE_ID));
		if (caseId.length() != 0) {
			filename = "case-" + caseId;
		} else {
			filename = "RAM-" + new SimpleDateFormat("YYYYMMdd").format(Convert.getCurrentTimestamp());
		}
		report = new ProductCartReport();
		report.setFileName(filename + ".pdf");
		
		Map<String, Object> data = new HashMap<>();
		SMTSession sess = req.getSession();
		data.put("cart", cart.getItems().values());
		data.put(HOSPITAL,StringUtil.checkVal(sess.getAttribute(HOSPITAL)));
		data.put(ROOM, StringUtil.checkVal(sess.getAttribute(ROOM)));
		data.put(SURGEON, StringUtil.checkVal(sess.getAttribute(SURGEON)));
		data.put(TIME, StringUtil.checkVal(sess.getAttribute(TIME)));
		data.put(CASE_ID, StringUtil.checkVal(sess.getAttribute(CASE_ID)));
		data.put(SALES_SIGNATURE, StringUtil.checkVal(sess.getAttribute(SALES_SIGNATURE)));
		data.put(ADMIN_SIGNATURE, StringUtil.checkVal(sess.getAttribute(ADMIN_SIGNATURE)));
		data.put(SALES_SIGNATURE_DT, StringUtil.checkVal(sess.getAttribute(SALES_SIGNATURE_DT)));
		data.put(ADMIN_SIGNATURE_DT, StringUtil.checkVal(sess.getAttribute(ADMIN_SIGNATURE_DT)));
		data.put(RESELLER, StringUtil.checkVal(sess.getAttribute(RESELLER)));
		data.put(OTHER_ID, StringUtil.checkVal(sess.getAttribute(OTHER_ID)));
		data.put(REP_ID, StringUtil.checkVal(sess.getAttribute(REP_ID)));
		data.put(COMPLETE_DT, sess.getAttribute(COMPLETE_DT));
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
	private void deleteCart(ActionRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(125);
		sql.append("DELETE from ").append(customDb).append("ram_case_info ");
		// We will only ever delete non finalized kits
		sql.append("WHERE FINALIZED_FLG = 0 AND ram_case_info_id = ? ");
		
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
	private void saveCart(ActionRequest req, int finalizeCart) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(225);
		SMTSession sess = req.getSession();

		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (StringUtil.checkVal(req.getSession().getAttribute(KIT_ID)).length() == 0) {
			sql.append("INSERT INTO ").append(customDb).append("ram_case_info ");
			sql.append("(HOSPITAL_NM,OPERATING_ROOM,SURGERY_DT,SURGEON_NM,");
			sql.append("RESELLER_NM,CASE_ID,CREATE_DT,PROFILE_ID,FINALIZED_FLG,");
			sql.append("RESELLER_SIGNATURE,ADMIN_SIGNATURE,RESELLER_SIGN_DT,");
			sql.append("ADMIN_SIGN_DT,OTHER_ID,REP_ID,ram_case_info_ID)");
			sql.append("VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			sess.setAttribute(KIT_ID, new UUIDGenerator().getUUID());
		} else {
			sql.append("UPDATE ").append(customDb).append("ram_case_info SET ");
			sql.append("HOSPITAL_NM=?,OPERATING_ROOM=?,SURGERY_DT=?,");
			sql.append("SURGEON_NM=?,RESELLER_NM=?,CASE_ID=?,UPDATE_DT=?, ");
			sql.append("PROFILE_ID=?, FINALIZED_FLG=?, RESELLER_SIGNATURE=?, ");
			sql.append("ADMIN_SIGNATURE=?,RESELLER_SIGN_DT=?,");
			sql.append("ADMIN_SIGN_DT=?,OTHER_ID=?,REP_ID=? WHERE ram_case_info_ID=? ");
		}
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 1;
			ps.setString(i++, (String) sess.getAttribute(HOSPITAL));
			ps.setString(i++, (String) sess.getAttribute(ROOM));
			ps.setTimestamp(i++, Convert.formatTimestamp(DATE_PATTERN, (String) sess.getAttribute(TIME)));
			ps.setString(i++, (String) sess.getAttribute(SURGEON));
			ps.setString(i++, (String) sess.getAttribute(RESELLER));
			ps.setString(i++, (String) sess.getAttribute(CASE_ID));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, user.getProfileId());
			ps.setInt(i++, finalizeCart);
			ps.setString(i++, (String) sess.getAttribute(SALES_SIGNATURE));
			ps.setString(i++, (String) sess.getAttribute(ADMIN_SIGNATURE));
			ps.setTimestamp(i++, Convert.formatTimestamp(SIGN_DATE_PATTERN, (String)  sess.getAttribute(SALES_SIGNATURE_DT)));
			ps.setTimestamp(i++, Convert.formatTimestamp(SIGN_DATE_PATTERN, (String)  sess.getAttribute(ADMIN_SIGNATURE_DT)));
			ps.setString(i++, (String) sess.getAttribute(OTHER_ID));
			ps.setString(i++, (String) sess.getAttribute(REP_ID));
			ps.setString(i++, (String) sess.getAttribute(KIT_ID));
			
			ps.executeUpdate();
			
			saveProducts(req, (String)sess.getAttribute(KIT_ID));
			
		} catch (SQLException e) {
			sess.setAttribute(KIT_ID,"");
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Get all products from the cart and save them to the database
	 * @param req
	 * @param kitId
	 * @throws ActionException
	 */
	private void saveProducts(ActionRequest req, String kitId) throws ActionException {
		// Delete all associated products to prevent duplicates
		purgeProducts(kitId);

		ShoppingCartVO cart = retrieveContainer(req).load();
		
		StringBuilder sql = new StringBuilder(175);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_case_product (case_product_id, product_id,ram_case_info_id,order_no,lot_no,qty,billable_flg,wasted_flg,product_from,create_dt, kit_flg) ");
		sql.append("values(?,?,?,?,?,?,?,?,?,?,?)");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i=1;
			for (String key : cart.getItems().keySet()) {
				ShoppingCartItemVO p = cart.getItems().get(key);
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setInt(2, Convert.formatInteger(p.getProduct().getProductId()));
				ps.setString(3, kitId);
				ps.setInt(4, i++);
				ps.setString(5, (String) p.getProduct().getProdAttributes().get(LOT_NO));
				ps.setInt(6, p.getQuantity());
				ps.setInt(7, Convert.formatInteger(StringUtil.checkVal(p.getProduct().getProdAttributes().get(BILLABLE))));
				ps.setInt(8, Convert.formatInteger(StringUtil.checkVal(p.getProduct().getProdAttributes().get(WASTED))));
				ps.setString(9, (String) p.getProduct().getProdAttributes().get(PRODUCT_FROM));
				ps.setTimestamp(10, Convert.getCurrentTimestamp());
				ps.setInt(11, Convert.formatInteger(StringUtil.checkVal(p.getProduct().getProdAttributes().get(PRODUCT_SOURCE))));
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
		sql.append("DELETE from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_case_product WHERE ram_case_info_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kitId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	/**
	 * Get all information and products associated with the current kit
	 * @param req
	 * @throws ActionException
	 */
	private void populateCart(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		SMTSession sess = req.getSession();
		
		sql.append("SELECT * FROM ").append(customDb).append("ram_case_info k ");
		sql.append("LEFT JOIN ").append(customDb).append("ram_case_product xr ");
		sql.append("on k.ram_case_info_id = xr.ram_case_info_id ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_PRODUCT p ");
		sql.append("on xr.PRODUCT_ID = p.PRODUCT_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_CUSTOMER c ");
		sql.append("on c.CUSTOMER_ID = p.CUSTOMER_ID ");
		sql.append("WHERE k.PROFILE_ID = ? and k.ram_case_info_ID = ?");
		UserDataVO user = (UserDataVO) sess.getAttribute(Constants.USER_DATA);
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
					sess.setAttribute(HOSPITAL, rs.getString("HOSPITAL_NM"));
					sess.setAttribute(ROOM, rs.getString("OPERATING_ROOM"));
					sess.setAttribute(SURGEON, rs.getString("SURGEON_NM"));
					if (rs.getTimestamp("SURGERY_DT") != null)
						sess.setAttribute(TIME, new SimpleDateFormat(DATE_PATTERN).format(rs.getTimestamp("SURGERY_DT")));
					sess.setAttribute(CASE_ID, rs.getString("CASE_ID"));
					sess.setAttribute(KIT_ID, rs.getString("ram_case_info_ID"));
					sess.setAttribute(FINALIZED, Convert.formatBoolean(rs.getString("FINALIZED_FLG")));
					sess.setAttribute(RESELLER, rs.getString("RESELLER_NM"));
					sess.setAttribute(SALES_SIGNATURE, rs.getString("RESELLER_SIGNATURE"));
					sess.setAttribute(ADMIN_SIGNATURE, rs.getString("ADMIN_SIGNATURE"));
					sess.setAttribute(SALES_SIGNATURE_DT, rs.getString("RESELLER_SIGN_DT"));
					sess.setAttribute(ADMIN_SIGNATURE_DT, rs.getString("ADMIN_SIGN_DT"));
					sess.setAttribute(OTHER_ID, rs.getString("OTHER_ID"));
					sess.setAttribute(REP_ID, rs.getString("REP_ID"));
					if (rs.getTimestamp("UPDATE_DT") != null)
						sess.setAttribute(COMPLETE_DT, new SimpleDateFormat("MM/dd/yyyy").format(rs.getTimestamp("UPDATE_DT")));
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
		product.addProdAttribute(LOT_NO, StringUtil.checkVal(rs.getString("LOT_NO")));
		product.addProdAttribute(BILLABLE, rs.getInt("BILLABLE_FLG"));
		product.addProdAttribute(PRODUCT_FROM, rs.getString("PRODUCT_FROM"));
		product.addProdAttribute(WASTED, rs.getInt("WASTED_FLG"));
		product.addProdAttribute(PRODUCT_SOURCE, rs.getInt("KIT_FLG"));
		ShoppingCartItemVO item = new ShoppingCartItemVO(product);
		item.setProductId(product.getProductId()+product.getProdAttributes().get(LOT_NO));
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
	private void newKit(ActionRequest req) throws ActionException {
		SMTSession sess = req.getSession();
		sess.removeAttribute(HOSPITAL);
		sess.removeAttribute(ROOM);
		sess.removeAttribute(SURGEON);
		sess.removeAttribute(TIME);
		sess.removeAttribute(CASE_ID);
		sess.removeAttribute(KIT_ID);
		sess.removeAttribute(FINALIZED);
		sess.removeAttribute(SALES_SIGNATURE);
		sess.removeAttribute(ADMIN_SIGNATURE);
		sess.removeAttribute(OTHER_ID);
		sess.removeAttribute(REP_ID);
		sess.removeAttribute(RESELLER);
		
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		cart.flush();
		store.save(cart);
	}
	
	
	/**
	 * Send emails to the representative and the hospital
	 * @param req
	 */
	private void sendEmails(ActionRequest req) throws ActionException {
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(req.getParameterValues("emails"));
			if (req.getSession().getAttribute(CASE_ID) != null) {
				mail.setSubject("Product Summary for Case " + req.getSession().getAttribute(CASE_ID));
			} else if (req.getSession().getAttribute(TIME) != null) {
				mail.setSubject("Product Summary for Surgery on " + new SimpleDateFormat(DATE_PATTERN).format(req.getSession().getAttribute(TIME)));
			} else {
				mail.setSubject("RAM OR Case Summary");
			}
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			mail.setFrom(site.getAdminEmail());
			buildReport(req);
	
			AbstractSBReportVO report = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, false);
			mail.addAttachment(report.getFileName(), report.generateReport());
			mail.setHtmlBody("Placeholder text for now");
	
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
}
