package com.sheets.action;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.OrderCompleteVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SheetsShoppingCartAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Sep 30, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SheetsShoppingCartAction extends SBActionAdapter {
	public static final int SESSION_PERSISTENCE_CART = 1;
	public static final int COOKIE_PERSISTENCE_CART = 2;
	
	/**
	 * 
	 */
	public SheetsShoppingCartAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public SheetsShoppingCartAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		
		// Process the cart
		SBModuleVO module;
		try {
			// Get the login data if passed in
			checkAuth(req);
			
			module = this.retrieveModuleData(orgId, actionInit.getActionId());
			req.setAttribute("sheetsShoppingCartModule", module);
			
			// Add the data to the moduleData
			ShoppingCartVO cart = this.manageCart(req);
			this.putModuleData(cart, cart.getSize(), false);
		} catch (Exception e) {
			log.error("Unable to retrieve cart", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Building");
		
		// Retrieve the cart
		ShoppingCartVO cart = null;
		try {
			cart = this.manageCart(req);
			
			// Store the cart onto the request object for display
			this.putModuleData(cart, cart.getSize(), false);
		} catch (Exception e) {
			log.error("Unable to manage cart", e);
		}
		
		// Make sure to set the parameter for the checkout process
		req.setParameter("checkout", "true");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		SBModuleVO vo = null;
		String msg = (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		try {
			String orgId = req.getParameter("organizationId");
			String sbActionId = req.getParameter(SB_ACTION_ID);
			vo = this.retrieveModuleData(orgId, sbActionId);

		} catch (SQLException e) {
			log.error("Error listing shopping cart portlet", e);
			msg = (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		this.putModuleData(vo, 1, true, msg);
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 * @throws DocumentException 
	 * @throws IOException 
	 */
	public ShoppingCartVO manageCart(SMTServletRequest req) 
	throws ActionException, IOException, DocumentException {

		String productId = StringUtil.checkVal(req.getParameter("productId"));
		
		// Load the cart.
		Storage container = this.getContainer(req);
		ShoppingCartVO cart = container.load();
		
		// Check the requests and make sure that the processing needs to occur.
		// For example, if the user is checked out and they go back to the
		// Cart, it should display their order info.
		if (productId.length() == 0 && container.isNewCart()) return cart;
		if (cart.isOrderCompleted() && productId.length() == 0) return cart;
		
		// See if there is data on the session to assign from login
		this.loadUser(req, cart);
		
		// See if the user has added/updated their address info:
		this.manageShippingInfo(cart, req);
		
		// Get the product information and add it to the cart. 
		this.addProduct(productId, cart, container, req);
		
		// Remove the items if requested
		this.removeItem(productId, Convert.formatBoolean(req.getParameter("itemRemove")), cart);
		
		// Perform the final checkout
		this.finalCheckout(req, cart);
		
		// Resave the cart for persistence reasons
		container.save(cart);
		
		return cart;
	}
	
	/**
	 * Adds the shipping or billing info entered by the user
	 * @param cart
	 * @param req
	 */
	protected void manageShippingInfo(ShoppingCartVO cart, SMTServletRequest req) {
		String shippingType = StringUtil.checkVal(req.getParameter("shippingType"));
		if (shippingType.length() == 0) return;
		
		UserDataVO user = new UserDataVO(req);
		
		if ("billing".equalsIgnoreCase(shippingType)) {
			cart.setBillingInfo(user);
			
			// Add the user to the session
			req.getSession().setAttribute(Constants.USER_DATA, user);
			
			if (Convert.formatBoolean(req.getParameter("useBilling"))) {
				cart.setShippingInfo(user);
				req.setParameter("type", "payment");
				
			} else { 
				req.setParameter("type", "Shipping");
			}
		} else {
			// Add the user to the session
			List<UserDataVO> locs = new ArrayList<UserDataVO>(); 
			locs.add(user);
			((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).setUserExtendedInfo(locs);
			
			// Update the shipping info
			cart.setShippingInfo(user);
			req.setParameter("type", "payment");
			
			// retrieve the shipping options and the sales tax
			try {
				this.calcTaxes(cart);
			} catch (Exception e) {
				log.error("Unable to calculate taxes", e);
			}
		}
		
		// If the user changed the shipping/billing info, have them return to the payment screen
		if (Convert.formatBoolean(req.getParameter("edit")))
			req.setParameter("type", "payment");
	}
	
	/**
	 * 
	 * @param req
	 * @param cart
	 */
	public void loadUser(SMTServletRequest req, ShoppingCartVO cart) {
		// See if there is data on the session to assign from login
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		if (cart.getBillingInfo() == null && user != null) {
			if (StringUtil.checkVal(user.getAddress()).length() > 0) {
				cart.setBillingInfo(user);
				cart.setShippingInfo(user);
			}
		}
		
		// recalulate the taxes
		this.calcTaxes(cart);
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	private Storage getContainer(SMTServletRequest req) {
		Storage container = null;
		String classPath = StorageFactory.SESSION_STORAGE;
		
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put(GlobalConfig.HTTP_REQUEST, req);
		attrs.put(GlobalConfig.HTTP_RESPONSE, attributes.get(GlobalConfig.HTTP_RESPONSE));
		
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		int type = Convert.formatInteger(mod.getAttribute(SBModuleVO.ATTRIBUTE_1) + "");
		if (type == COOKIE_PERSISTENCE_CART) {
			classPath = StorageFactory.PERSISTENT_STORAGE;
			attrs.put(GlobalConfig.KEY_DB_CONN, dbConn);
		}
		
		try {
			container = StorageFactory.getInstance(classPath, attrs);
		} catch (Exception e) {
			log.error("could not load Storage " + classPath, e);
		}
		
		return container;
	}
	
	/**
	 * 
	 * @param req
	 * @param cart
	 */
	public void finalCheckout(SMTServletRequest req, ShoppingCartVO cart) {
		if (! Convert.formatBoolean(req.getParameter("finalCheckout"))) return;
		
		String encKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
		PaymentVO payment = new PaymentVO(encKey);
		payment.setExpirationMonth(req.getParameter("expMonth"));
		payment.setExpirationYear(req.getParameter("expYear"));
		payment.setPaymentName(req.getParameter("nameOnCard"));
		payment.setPaymentCode(req.getParameter("securityNumber"));
		payment.setPaymentNumber(req.getParameter("creditCardNumber"));
		payment.setPaymentType(req.getParameter("creditCardType"));
		
		cart.setPayment(payment);
		
		// Call the checkout process
		this.payForOrder(cart, req);
	}
	
	/**
	 * 
	 * @param productId
	 * @param itemRemove
	 * @param cart
	 */
	public void removeItem(String productId, boolean itemRemove, ShoppingCartVO cart) {
		if (! itemRemove) return;
		
		// Remove the product
		cart.remove(productId);
		
		// Recalculate the taxes
		this.calcTaxes(cart);
	}
	
	/**
	 * 
	 * @param productId
	 * @param itemRemove
	 * @param cart
	 * @param container
	 * @param site
	 * @param qty
	 * @throws ActionException
	 */
	public void addProduct(String productId, ShoppingCartVO cart, Storage container, SMTServletRequest req) 
	throws ActionException {
		if (!(productId.length() > 0 && ! Convert.formatBoolean(req.getParameter("itemRemove")))) return;
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		int qty = Convert.formatInteger(req.getParameter("qty"));
		
		// Create a new cart if the order is complete and the user is trying 
		// to add a product to the cart (we'll assume that they are trying to 
		// Create a new cart.
		if (cart.isOrderCompleted()) {
			cart.flush();
		}
		
		ShoppingCartItemVO item = this.getProductInfo(productId, site.getOrganizationId());
		if (item != null) {
			item.setQuantity(qty);
			cart.add(item);
			
			log.debug("cart: " + cart + "|" + cart.getSize());
			
			// Recalculate the taxes
			this.calcTaxes(cart);
		}
			
	}
	
	/**
	 * 
	 * @param cart
	 */
	public void calcTaxes(ShoppingCartVO cart) {
		WebServiceManager wsm = new WebServiceManager();
		
		if (cart == null || cart.getShippingInfo() == null) return;
		
		try {
			double tax = wsm.calcTaxes(cart.getShippingInfo().getZipCode());
			cart.setTaxAmount(cart.getSubTotal() * tax);
		} catch (Exception e) {
			log.debug("Error calculating taxes", e);
		}
	}
	
	/**
	 * 
	 * @param cart
	 * @param req
	 */
	public void payForOrder(ShoppingCartVO cart, SMTServletRequest req) {
		// Set the order to complete
		OrderCompleteVO order = new OrderCompleteVO();
		order.setGrandTotal(cart.getCartTotal());
		order.setOrderNumber("SMT_" + RandomAlphaNumeric.generateRandom(8, false));
		order.setShipping(0.0);
		order.setStatus(OrderCompleteVO.ORDER_SUCCESSFULLY_COMPLETED);
		order.setSubTotal(cart.getSubTotal());
		order.setTax(cart.getTaxAmount());
		cart.setOrderComplete(order);
		
		// Since there are no shipping options, I need to set it
		Map<String,ShippingInfoVO> shippingOptions = new LinkedHashMap<String,ShippingInfoVO>();
		ShippingInfoVO shInfo = new ShippingInfoVO();
		shInfo.setSelected(true);
		shInfo.setShippingCost(0.0);
		shInfo.setShippingMethodId("GROUND");
		shInfo.setShippingMethodName("Ground");
		shippingOptions.put("GROUND", shInfo);
		cart.setShippingOptions(shippingOptions);
		cart.setShipping("GROUND");
		
		// Store the order
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuilder s = new StringBuilder();
		s.append("insert into product_order(order_id, product_order_status_id, ");
		s.append("organization_id, site_id, cart_data_bin, order_dt) values(?,?,?,?,?,?)");
		
		// Convert the cart to an XML file
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLEncoder ser = new XMLEncoder(baos);
		ser.writeObject(cart);
		ser.close();
		String xmlData = new String(baos.toByteArray());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, order.getOrderNumber());
			ps.setInt(2, 1);
			ps.setString(3, site.getOrganizationId());
			ps.setString(4, site.getSiteId());
			ps.setString(5, xmlData);
			ps.setTimestamp(6, Convert.getTimestamp(order.getOrderDate(), true));
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Error adding cart for order", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 
	 * @param productId
	 * @param orgId
	 * @return
	 */
	public ShoppingCartItemVO getProductInfo(String productId, String orgId) {
		ShoppingCartItemVO vo = null;
		String s = "select * from product a inner join product_catalog b ";
		s += "on a.product_catalog_id = b.product_catalog_id ";
		s += "where cust_product_no = ? and organization_id = ? AND PRODUCT_GROUP_ID IS NULL ";
		PreparedStatement ps = null;

		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, productId);
			ps.setString(2, orgId);
			ResultSet rs = ps.executeQuery();
			vo = new ShoppingCartItemVO();
			if (rs.next()) {
				//vo.setProductId(rs.getString("product_id"));
				vo.setProductId(rs.getString("cust_product_no"));
				vo.setBasePrice(rs.getDouble("msrp_cost_no"));
				vo.setDescription(rs.getString("desc_txt"));
				vo.setProductName(rs.getString("product_nm"));
			}
		} catch (SQLException sqle) {
			log.error("Unable to retrieve product", sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
		
		return vo;
	}
	
	/**
	 * 
	 * @throws SQLException
	 */
	public SBModuleVO retrieveModuleData(String orgId, String sbActionId) throws SQLException {

		String s = "select * from sb_action where organization_id = ? and action_id = ? ";
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, orgId);
		ps.setString(2, sbActionId);
		ResultSet rs = ps.executeQuery();
		
		SBModuleVO module = new SBModuleVO();
		if (rs.next()) {
            module = new SBModuleVO();
            module.setActionId(rs.getString("action_id"));
            module.setModuleTypeId(rs.getString("module_type_id"));
            module.setActionName(rs.getString("action_nm"));
            module.setActionDesc(rs.getString("action_desc"));
            module.setOrganizationId(rs.getString("organization_id"));
            module.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
            module.setAttribute(SBModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
            module.setIntroText(rs.getString("intro_txt"));
            module.setPendingSyncFlag(rs.getInt("pending_sync_flg"));
            module.setActionGroupId(rs.getString("action_group_id"));
		}
		
		ps.close();
		return module;
	}
	
	/**
	 * 
	 * @param req
	 */
	public void checkAuth(SMTServletRequest req) {
		boolean login = Convert.formatBoolean(req.getParameter("login"));
		boolean bypass = Convert.formatBoolean(req.getParameter("bypass"));
		if (! login || bypass) return;
		
		String loginName = req.getParameter("emailAddress");
		String password = StringUtil.checkVal(req.getParameter("password"));
		UserDataVO user;
		WebServiceManager wsm = new WebServiceManager();
		try {
			user = wsm.authenticate(loginName, password);
			log.debug("******************* User: " + user);
			req.getSession().setAttribute(Constants.USER_DATA, user);
			
			// Add the role
			String siteId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getSiteId();
			SheetsRoleModule role = new SheetsRoleModule();
			req.getSession().setAttribute(Constants.ROLE_DATA, role.getUserRole(user.getProfileId(), siteId));
		} catch (Exception e) {
			req.setParameter("authLoginFailed", "true");
			req.setParameter("login", "false");
		} 
	}
}
