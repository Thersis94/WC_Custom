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
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>ProductCartAction.java<p/>
 * <b>Description: Handles product search and cart functionality for the 
 * ram site</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since August 2, 2016
 * <b>Changes: </b>
 ****************************************************************************/

public class ProductCartAction extends SBActionAdapter {

	// Names for the cookies related to this action
	public static final String HOSPITAL = "hospital";
	public static final String ROOM = "room";
	public static final String SURGEON = "surgeon";
	public static final String TIME = "time";
	public static final String CASE_ID = "caseId";
	public static final String RESELLER = "resellerNm";
	
	private enum SearchFields {
		productName("PRODUCT_NM"),
		productDesc("DESC_TXT"),
		productSKU("CUST_PRODUCT_ID"),
		productGTIN("c.GTIN_NUMBER_TXT + CAST(p.GTIN_PRODUCT_ID as NVARCHAR(64))");
		
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
		if (req.hasParameter("editAttr")) {
			req.getSession().setAttribute(req.getParameter("editAttr"), req.getParameter("attrValue"));
		} else if(req.hasParameter("newCart")) {
			log.debug("Getting a new Cart");
			newKit(req);
		}else if (req.hasParameter("loadCart")){
			if (req.hasParameter("kitId")) {
				populateCart(req);
			} else {
				loadCart(req);
			}
		} else if (req.hasParameter("saveCart")){
			saveCart(req);
		} else {
			editCart(req);
		}
	}
	
	
	/**
	 * Deals with the various actions that a user can enact that affect thier cart
	 * @param req
	 * @throws ActionException
	 */
	private void editCart(SMTServletRequest req) throws ActionException {
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		String oldLot = StringUtil.checkVal(req.getParameter("oldLot"));
		log.debug(req.getParameter("productId")+oldLot);
		if (Convert.formatBoolean(req.getParameter("clearCart"))) {
			deleteItem(cart, req);
		} else if (cart.getItems().containsKey(req.getParameter("productId")+oldLot)) {

			ShoppingCartItemVO p = cart.getItems().get(req.getParameter("productId") + oldLot);
			p.setProductId(req.getParameter("productId") + req.getParameter("lotNo"));
			p.getProduct().getProdAttributes().put("lotNo", req.getParameter("lotNo"));
			p.getProduct().getProdAttributes().put("oldId", p.getProduct().getProductId() + oldLot);
			int qty = Convert.formatInteger(req.getParameter("qty"),1);
			if (!Convert.formatBoolean(req.getParameter("editItem")))qty += p.getQuantity();
			p.setQuantity(qty > 99? 99:qty);
			cart.add(p);
			
			// Remove the old product if we have changed the lot no
			if (!oldLot.equals(p.getProduct().getProdAttributes().get("lotNo")) ) {
				cart.remove(p.getProduct().getProductId()+oldLot);
			}
			
			super.putModuleData(new GenericVO("update", p));
		} else {
			addItem(cart, buildProduct(req), StringUtil.checkVal(req.getParameter("oldLot")));
		}
		
		store.save(cart);
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
	private ShoppingCartItemVO buildProduct(SMTServletRequest req) {
		ProductVO product = new ProductVO();
		product.setProductId(req.getParameter("productId"));
		product.setProductName(req.getParameter("productName"));
		product.setShortDesc(req.getParameter("desc"));
		product.addProdAttribute("customer", req.getParameter("customer"));
		product.addProdAttribute("gtin", req.getParameter("gtin"));
		product.addProdAttribute("lotNo", StringUtil.checkVal(req.getParameter("lotNo")));
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
		
		// Return the cart item so the cart can be updated properly on the front end.
		super.putModuleData(new GenericVO("insert", item));
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
			container = StorageFactory.getInstance(StorageFactory.PERSISTENT_STORAGE, attrs);
		} catch (Exception ex) {
			throw new ActionException(ex);
		}
		return container;
	}



	public void retrieve(SMTServletRequest req) throws ActionException {
		// Load the cart first since it is always needed
		ShoppingCartVO cart = retrieveContainer(req).load();
		req.setAttribute("cart", cart.getItems());
		
		// Check if we are building a file, create the report generator and set the pertinent information
		if (req.hasParameter("buildFile")) {
			log.debug("Building");
			buildReport(cart, req);
			return;
		}
		
		// If we are looking only at the cart don't bother doing a solr search.
		if (req.hasParameter("showCart")) return;
		
		if (req.hasParameter("searchData")) searchProducts(req);
	}
	
	
	private void searchProducts(SMTServletRequest req) throws ActionException {
		List<RAMProductVO> products = new ArrayList<>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String[] fields = req.getParameterValues("searchFields");
		int searchType = Convert.formatInteger(req.getParameter("searchType"), 1);
		// A search type greater than 2 means an exact search
		String searchComaparator = searchType > 1? " like ":" = ";
		StringBuilder sql = new StringBuilder(300);
		sql.append("SELECT p.PRODUCT_ID, p.CUST_PRODUCT_ID, c.GTIN_NUMBER_TXT + CAST(p.GTIN_PRODUCT_ID as NVARCHAR(64)) as GTIN_NUMBER_TXT, PRODUCT_NM, ");
		sql.append("DESC_TXT, SHORT_DESC, c.CUSTOMER_NM FROM ").append(customDb).append("RAM_PRODUCT p ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_CUSTOMER c on c.CUSTOMER_ID = p.CUSTOMER_ID ");
		sql.append("WHERE p.CUSTOMER_ID is not null and p.GTIN_PRODUCT_ID is not null AND ");
		sql.append("p.CUSTOMER_ID != '' AND p.GTIN_PRODUCT_ID != '' ");
		if (req.hasParameter("searchCustomer")) sql.append("AND p.CUSTOMER_ID = ? ");
		if (fields != null) {
			// Add fail condition to allow for multiple OR clauses
			sql.append("AND (1=2 ");
			for (String field : fields) {
				sql.append("OR ").append(SearchFields.valueOf(field).getColumnName()).append(searchComaparator).append("? ");
			}
			sql.append(") ");
		}
		log.debug(sql);
		int count = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			if (req.hasParameter("searchCustomer")) ps.setString(i++, req.getParameter("searchCustomer"));
			if (fields != null) {
				String searchData = req.getParameter("searchData");
				for (int j=0; j<fields.length; j++) {
					ps.setString(i++, (searchType > 1 ? "%":"") + searchData + (searchType == 3? "%":""));
				}
			}
			
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
	 * Build the requested report based off of the request servlet and the 
	 * shopping cart
	 * @param cart
	 * @param req
	 */
	private void buildReport(ShoppingCartVO cart, SMTServletRequest req) {
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
		data.put(RESELLER, StringUtil.checkVal(req.getParameter(RESELLER)));
		data.put("baseDomain", req.getHostName());
		data.put("format", req.getParameter("format"));
		
		report.setData(data);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}
	
	private void saveCart(SMTServletRequest req) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(225);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		
		if (StringUtil.checkVal(req.getSession().getAttribute("kitId")).length() == 0) {
			sql.append("INSERT INTO ").append(customDb).append("RAM_KIT_INFO ");
			sql.append("(HOSPITAL_NM,OPERATING_ROOM,SURGERY_DT,SURGEON_NM,");
			sql.append("RESELLER_NM,CASE_ID,CREATE_DT,PROFILE_ID,RAM_KIT_INFO_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?,?)");
			req.getSession().setAttribute("kitId", new UUIDGenerator().getUUID());
		} else {
			sql.append("UPDATE ").append(customDb).append("RAM_KIT_INFO SET ");
			sql.append("HOSPITAL_NM=?,OPERATING_ROOM=?,SURGERY_DT=?,");
			sql.append("SURGEON_NM=?,RESELLER_NM=?,CASE_ID=?,CREATE_DT=?, ");
			sql.append("PROFILE_ID=? WHERE RAM_KIT_INFO_ID=? ");
		}
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 1;
			ps.setString(i++, (String) req.getSession().getAttribute(HOSPITAL));
			ps.setString(i++, (String) req.getSession().getAttribute(ROOM));
			ps.setTimestamp(i++, Convert.formatTimestamp("MM-dd-yyy -- hh:mm ", (String) req.getSession().getAttribute(TIME)));
			ps.setString(i++, (String) req.getSession().getAttribute(SURGEON));
			ps.setString(i++, (String) req.getSession().getAttribute("resellerName"));
			ps.setString(i++, (String) req.getSession().getAttribute(CASE_ID));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, user.getProfileId());
			ps.setString(i++, (String)req.getSession().getAttribute("kitId"));
			
			ps.executeUpdate();
			
			saveProducts(req, (String)req.getSession().getAttribute("kitId"));
			
			super.putModuleData(req.getSession().getAttribute("kitId"), 1, false);
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	private void saveProducts(SMTServletRequest req, String kitId) throws ActionException {
		purgeProducts(kitId);
		
		ShoppingCartVO cart = retrieveContainer(req).load();
		
		StringBuilder sql = new StringBuilder(175);
		sql.append("INSERT INTO ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_KIT_PRODUCT_XR (KIT_PRODUCT_ID, PRODUCT_ID,RAM_KIT_INFO_ID,ORDER_NO,LOT_NO,QTY,CREATE_DT) ");
		sql.append("VALUES(?,?,?,?,?,?,?)");
		
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
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
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
	
	private void loadCart(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<GenericVO> kits = new ArrayList<>();
		
		sql.append("SELECT CASE_ID, k.RAM_KIT_INFO_ID, COUNT(k.RAM_KIT_INFO_ID) as NUM_PRODUCTS ");
		sql.append("FROM ").append(customDb).append("RAM_KIT_INFO k ");
		sql.append("LEFT JOIN ").append(customDb).append("RAM_KIT_PRODUCT_XR xr ");
		sql.append("on k.RAM_KIT_INFO_ID = xr.RAM_KIT_INFO_ID ");
		sql.append("WHERE k.PROFILE_ID = ? ");
		sql.append("GROUP BY HOSPITAL_NM, OPERATING_ROOM, SURGERY_DT, SURGEON_NM, CASE_ID, k.RAM_KIT_INFO_ID ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			ps.setString(1, user.getProfileId());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				kits.add(new GenericVO(rs.getString("RAM_KIT_INFO_ID"), new GenericVO(rs.getString("CASE_ID"), rs.getString("NUM_PRODUCTS"))));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		super.putModuleData(kits, kits.size(), false);
	}
	
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
		sql.append("WHERE k.PROFILE_ID = ? ");
		sql.append("and k.RAM_KIT_INFO_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			ps.setString(1, user.getProfileId());
			ps.setString(2, req.getParameter("kitId"));
			
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
					req.getSession().setAttribute(TIME, rs.getDate("SURGERY_DT"));
					req.getSession().setAttribute(CASE_ID, rs.getString("CASE_ID"));
					req.getSession().setAttribute("kitId", rs.getString("RAM_KIT_INFO_ID"));
				}
				cart.add(buildProduct(rs));
			}
			
			store.save(cart);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	private ShoppingCartItemVO buildProduct(ResultSet rs) throws SQLException {
		ProductVO product = new ProductVO();
		product.setProductId(rs.getString("PRODUCT_ID"));
		product.setProductName(rs.getString("PRODUCT_NM"));
		product.addProdAttribute("customer", rs.getString("CUSTOMER_NM"));
		product.addProdAttribute("gtin",rs.getString("CUSTOMER_ID") + rs.getString("GTIN_PRODUCT_ID"));
		product.addProdAttribute("lotNo", StringUtil.checkVal(rs.getString("LOT_NO")));
		ShoppingCartItemVO item = new ShoppingCartItemVO(product);
		item.setProductId(product.getProductId()+product.getProdAttributes().get("lotNo"));
		item.setQuantity(Convert.formatInteger(rs.getInt("QTY")));
		
		return item;
	}
	
	private void newKit(SMTServletRequest req) throws ActionException {
		req.getSession().removeAttribute(HOSPITAL);
		req.getSession().removeAttribute(ROOM);
		req.getSession().removeAttribute(SURGEON);
		req.getSession().removeAttribute(TIME);
		req.getSession().removeAttribute(CASE_ID);
		req.getSession().removeAttribute("kitId");
		Storage store = retrieveContainer(req);
		ShoppingCartVO cart = store.load();
		cart.flush();
		store.save(cart);
	}
}
