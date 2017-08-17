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
import com.ram.action.or.vo.RAMCaseItemVO.RAMCaseType;
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
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
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

	//TODO appears to have unused methods and several large case switch statements that should be cleaned up.
	
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
	public static final String CART = "cart";
	public static final String NOTES = "notes";
	public static final String HOSPITAL_REP = "hospitalRep";
	public static final String SALES_REP = "salesRep";
	public static final String SIGNATURES = "signatures";
	public static final String BASE_DOMAIN = "baseDomain";
	public static final String FORMAT = "format";
	public static final String SURG_DATE = "surgDate";

	private enum SearchFields {
		productName("product_nm"),
		customerName("c.customer_nm"),
		customerProductId("cust_product_id"),
		gtinProductNumber("c.gtin_number_txt || cast(p.gtin_product_id as varchar(64))"),
		gtinProductId("c.gtin_number_txt || cast(p.gtin_product_id as varchar(64))");

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
	private enum WidgetBuildAction {saveCaseInfo, deleteCase, addProduct, deleteProduct, addSignature, finalize, sendEmails, saveNote, persistCase}

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
					String caseItemId = rcm.removeCaseItem(req);
					putModuleData(caseItemId);
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
				case saveNote:
					rcm.saveNote(req);
					break;
				case persistCase:
					rcm.persistCasePerm(rcm.retrieveCase(req.getParameter(RAMCaseManager.RAM_CASE_ID)));
					break;
				case deleteCase:
					deleteCase(req);
					break;
			}
		} catch (Exception e) {
			log.error("Error managing case", e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Deletes a case form the database
	 * @param req
	 * @throws ActionException
	 */
	private void deleteCase(ActionRequest req) throws ActionException {
		RAMCaseVO cvo = new RAMCaseVO();
		cvo.setCaseId(req.getParameter(CASE_ID));
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.delete(cvo);
		} catch(Exception e) {
			log.error("unable to delete case", e);
			throw new ActionException("unable to delete case", e);
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

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
		WidgetRetrieveAction wa = WidgetRetrieveAction.valueOf(req.getParameter("widgetAction"));
		String caseId = req.getParameter(CASE_ID);
		
		try {
			RAMCaseVO cvo = rcm.retrieveCase(caseId);
			switch (wa) {
			case loadCase:
				putModuleData(cvo);
				break;
			case loadReport:
				buildReport(cvo, req);
				break;
			case searchProducts:
				searchProducts(cvo, req);
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
	private void searchProducts(RAMCaseVO cvo, ActionRequest req) throws ActionException {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(256);
		StringBuilder cSql = new StringBuilder(256);
		List<Object> params = new ArrayList<>();
		
		// Build the select clause
		cSql.append("select count(*) as key ");
		getProductSearchSelect(sql);
		
		// Build the body and where clause
		getProductSearchFilter(cSql, req, true);
		getProductSearchFilter(sql, req, false);
		
		// Add the parameters to the queries if searching
		params.add(cvo.getCustomerLocationId());
		if (req.hasParameter("search")) {
			String searchData = "%" + req.getParameter("search").toLowerCase() + "%";
			params.add(searchData);
			params.add(searchData);
			params.add(searchData);
		}
		
		// Get the count
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<Object> prodCount = db.executeSelect(cSql.toString(), params, new GenericVO());
		int size = Convert.formatInteger(((GenericVO)prodCount.get(0)).getKey()+"");
		
		// Add the nav params
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		
		// Get the product list
		List<Object> products = db.executeSelect(sql.toString(), params, new RAMProductVO());
		
		// Return the data
		super.putModuleData(products, size, false);
	}

	/**
	 * Builds the select clause for the product search
	 * @param sql
	 */
	protected void getProductSearchSelect(StringBuilder sql) {
		sql.append("select p.product_id, p.cust_product_id, desc_txt, short_desc, c.customer_nm, l.kit_layer_id, ");
		sql.append("c.gtin_number_txt || cast(p.gtin_product_id as varchar(64)) as gtin_number_txt, product_nm ");
	}

	/**
	 * Build the sql for the product search
	 * @param req
	 * @param fields
	 * @param searchType
	 * @return
	 */
	protected void getProductSearchFilter (StringBuilder sql, ActionRequest req, boolean count) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_product p ");
		sql.append("item".equalsIgnoreCase(req.getParameter("productType")) ? DBUtil.INNER_JOIN : DBUtil.LEFT_OUTER_JOIN);
		sql.append(schema).append("ram_location_item_master i on p.product_id = i.product_id and customer_location_id = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_customer c on c.customer_id = p.customer_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_kit_layer l on l.product_id = p.product_id ");
		sql.append("where p.active_flg = 1 ");
		
		// Add the search params
		if (req.hasParameter("search")) {
			sql.append("and (lower(product_nm) like ? or lower(cust_product_id) like ? ");
			sql.append("or lower(c.gtin_number_txt || cast(p.gtin_product_id as varchar(64))) like ? )");
		}
		
		// Set the order
		if (! count) {
			sql.append("order by ");
			sql.append(SearchFields.valueOf(req.getParameter("sort", "productName")).getColumnName()).append(" ");
			sql.append(StringUtil.checkVal(req.getParameter("order"), "asc"));
			sql.append(" offset ? limit ? ");
		}

		log.debug(sql);
	}


	/**
	 * Build the requested report based off of the request servlet and the 
	 * shopping cart
	 * @param cart
	 * @param cvo
	 * @param req 
	 * @throws ActionException 
	 */
	private void buildReport(RAMCaseVO cvo, ActionRequest req) {
		AbstractSBReportVO report;
		String filename;
		String caseId = StringUtil.checkVal(cvo.getCaseId());
		if (caseId.length() != 0) {
			filename = "case-" + cvo.getHospitalCaseId();
		} else {
			filename = "RAM-" + new SimpleDateFormat("YYYYMMdd").format(Convert.getCurrentTimestamp());
		}
		report = new ProductCartReport();
		report.setAttributes(attributes);
		report.setFileName(filename + ".pdf");

		Map<String, Object> data = new HashMap<>();

		data.put(CART, cvo.getItems().get(StringUtil.checkVal(req.getParameter("caseType"), RAMCaseType.OR.toString())).values());
		data.put(HOSPITAL,StringUtil.checkVal(cvo.getCustomerName()));
		data.put(ROOM, StringUtil.checkVal(cvo.getOrRoomName()));
		data.put(SURGEON, StringUtil.checkVal(cvo.getSurgeonName()));
		data.put(TIME, StringUtil.checkVal(cvo.getSurgeryDate()));
		data.put(CASE_ID, StringUtil.checkVal(cvo.getHospitalCaseId()));
		data.put(NOTES, StringUtil.checkVal(cvo.getCaseNotes()));
		data.put(HOSPITAL_REP, cvo.getHospitalRep());
		
		data.put(SURG_DATE, Convert.formatDate(cvo.getSurgeryDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR)  );
		data.put(SALES_REP, cvo.getSalesRep());

		data.put(SIGNATURES, cvo.getAllSignatures());
		data.put(BASE_DOMAIN, req.getHostName());
		data.put(FORMAT, req.getParameter(FORMAT));

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
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		String fromEmail = StringUtil.isEmpty(user.getEmailAddress() ) ? "info@ramgrp.com" : user.getEmailAddress();

		try {
			// Get the case
			RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
			RAMCaseVO cvo = rcm.retrieveCase(StringUtil.checkVal(req.getParameter(CASE_ID)));
			
			// Build the PDF
			buildReport(cvo, req);
			AbstractSBReportVO report = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, false);

			// Send the email
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Surgical Case Summary for Surgery ID: " + cvo.getHospitalCaseId());
			mail.addRecipients(req.getParameterValues("emails[]"));
			mail.setFrom(fromEmail);
			mail.setReplyTo(fromEmail);
			mail.addAttachment(report.getFileName(), report.generateReport());
			mail.setHtmlBody(buildEmailBody(user, cvo));
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	/**
	 * builds the email body
	 * @param user
	 * @param cvo
	 * @return
	 */
	private String buildEmailBody(UserDataVO user, RAMCaseVO cvo) {
		StringBuilder s = new StringBuilder(512);
		s.append("<h3>Attached is a Surgical Case Report for your Records</h3>");
		s.append("<p>The attached case report <i><u>(").append(cvo.getHospitalCaseId()).append(")</u></i> ");
		s.append("that was performed at <i><u>").append(cvo.getCustomerName()).append("</u></i> and scheduled for <i><u>");
		s.append(Convert.formatDate(cvo.getSurgeryDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		s.append("</u></i> has been sent to you for your record by <i><u>").append(user.getFullName()).append(" </u></i></p>");
		s.append("<p>If you received this report in error, please contact ").append(user.getFullName());
		s.append(" so we may update our system.  Thank you.</p>");
		s.append("<img src='http://www.ramgrp.com/binary/themes/CUSTOM/RAMGRP/PORTAL/images/ramlogo-small.png' />");
		
		return s.toString();
	}
}
