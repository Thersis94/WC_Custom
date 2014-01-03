package com.universal.signals.action;

// JDK 1.6
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// DOM4J
import org.dom4j.DocumentException;
import org.dom4j.Element;

// SMT BAse Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.OrderCompleteVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.XMLUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

// WC_Custom libs
import com.universal.commerce.DiscountManager;
import com.universal.commerce.USADiscountVO;
import com.universal.commerce.USADiscountVO.DiscountType;
import com.universal.util.USARoleModule;
import com.universal.util.WebServiceAction;

/****************************************************************************
 * <b>Title</b>: ShoppingCartAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Custom USA shopping cart.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 27, 2012<p/>
 * <b>Changes: </b>
 * 06-13 and 06-26-2012: DBargerhuff;  Refactored to support multiple unique catalogs, unique catalog URLs. 
 * 10-03-2012: DBargerhuff; Refactored to reflect changes in WebServiceAction
 * 11-21-2012: DBargerhuff; Refactored to begin implementing new promo code (i.e. discount) processing.
 * 02-03-2012: DBargerhuff; Refactored to finalize implementation for new 
 * 					  promo code (i.e. discount) processing
 ****************************************************************************/
public class ShoppingCartAction extends SBActionAdapter {
	
	public static final int SESSION_PERSISTENCE_CART = 1;
	public static final int COOKIE_PERSISTENCE_CART = 2;
	private String catalogSiteId = null;
	private DiscountManager dMgr = null;
	
	/**
	 * 
	 */
	public ShoppingCartAction() {}

	/**
	 * @param actionInit
	 */
	public ShoppingCartAction(ActionInitVO actionInit) {
		super(actionInit);
		this.initDiscountManager(actionInit, attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("ShoppingCartAction retrieve...");
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		this.setCatalogSiteId(req);
		// Process the cart
		SBModuleVO module;
		try {
			// Get the login data if passed in
			checkAuth(req);
			module = this.retrieveModuleData(orgId, actionInit.getActionId());
			req.setAttribute("shoppingCartModule", module);
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
		log.debug("Shopping cart build....");
		this.setCatalogSiteId(req);
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
	
	/**
	 * 
	 * @param req
	 * @return
	 * @throws ActionException
	 * @throws DocumentException
	 * @throws IOException
	 */
	public ShoppingCartVO manageCart(SMTServletRequest req) 
			throws ActionException, DocumentException, IOException {
		log.debug("managing cart...");
		StringEncoder e = new StringEncoder();
		String productId = e.decodeValue(StringUtil.checkVal(req.getParameter("productId")));
		
		// Load the cart from our Storage medium.
		log.debug("retrieving cart...");
		Storage container = this.retrieveContainer(req);
		ShoppingCartVO cart = container.load();
		
		// reset cart error map
		cart.flushErrors();
		log.debug("Is cart new: " + container.isNewCart());
		if (productId.length() == 0 && container.isNewCart()) return cart;
		
		// check for final checkout processing
		if (this.processFinalCheckout(req, cart, productId)) return cart;
		
		// attempt to load cart billing/shipping data from user session
		this.retrieveCartUserData(req, cart);
		
		// Get the product information and add it to the cart.  Note, if the 
		// quantity is being updated, the new "Total" quantity must be passed here
		boolean itemRemove = Convert.formatBoolean(req.getParameter("itemRemove"));
		boolean updateShipping = Convert.formatBoolean(req.getParameter("updateShipping"));
		if (productId.length() > 0 && ! itemRemove && Convert.formatInteger(req.getParameter("qty")) > 0) {
			log.debug("processing productId length > 0, AND not itemRemove, AND qty > 0...");
			// If order is complete and user is trying to add a product 
			// to the cart (we'll assume that they are trying to create a new cart).
			if (cart.isOrderCompleted()) {
				container.flush();
				cart = container.load();
			}			
			// process the item
			this.processItem(req, cart, productId);
			
		} else if (itemRemove || StringUtil.checkVal(req.getParameter("qty")).length() > 0) {
			log.debug("processing itemRemove OR qty > 0 (values are itemRemove/qty): " + itemRemove + "/" + req.getParameter("qty"));
			cart.remove(productId);
			
		} else if (Convert.formatBoolean(req.getParameter("updatePromoCode"))) {
			log.debug("processing retrieval of new cart discount (updatePromoCode)...");
			// process promo code
			this.manageDiscount(cart, null, "update", req.getParameter("promoCode"));
			
		} else if (Convert.formatBoolean(req.getParameter("removePromoCode"))) {
			log.debug("processing removal of cart discount (removePromoCode)...");
			this.manageDiscount(cart, null, "remove", null);
			
		} else if (Convert.formatBoolean(req.getParameter("finalCheckout"))) {	
			log.debug("processing 'finalCheckout' where productId length > 0...");
			this.processFinalCheckOut(req, cart);
			
		} else if (updateShipping) {
			log.debug("processing updateShipping: " + req.getParameter("updateShipping"));
			String shippingId = req.getParameter("selShipping");
			cart.setShipping(shippingId);
			//recalculateShipping = false;
		}
		
		// If the request is for shipping manage the data
		if (StringUtil.checkVal(req.getParameter("shippingType")).length() > 0) {
			log.debug("processing shippingType: " + req.getParameter("shippingType"));
			this.manageShippingInfo(cart, req);
		}
		
		// Finally, recalculate the cart (shipping/taxes, etc.)
		this.manageDiscount(cart, null, "recalculate", null);
		this.manageShipping(cart, updateShipping);
		
		// if checking out, calculate taxes.
		boolean checkOut = Convert.formatBoolean(req.getParameter("checkout"));
		if (checkOut) this.calcTaxes(cart);

		// Resave the cart for persistence reasons
		log.debug("saving cart...");
		container.save(cart);
		
		return cart;
	}
	
	/**
	 * Retrieves the Storage container
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected Storage retrieveContainer(SMTServletRequest req) 
			throws ActionException {
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put(GlobalConfig.HTTP_REQUEST, req);
		attrs.put(GlobalConfig.HTTP_RESPONSE, attributes.get(GlobalConfig.HTTP_RESPONSE));
		
		// Load the cart from our Storage medium.
		Storage container = null;
		String classPath = StorageFactory.SESSION_STORAGE;
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		int type = Convert.formatInteger(mod.getAttribute(SBModuleVO.ATTRIBUTE_1) + "");
		if (type == COOKIE_PERSISTENCE_CART) {
			classPath = StorageFactory.PERSISTENT_STORAGE;
			attrs.put(GlobalConfig.KEY_DB_CONN, dbConn);
		}
		
		try {
			container = StorageFactory.getInstance(classPath, attrs);
		} catch (Exception ex) {
			throw new ActionException(ex);
		}
		return container;
	}
	
	/**
	 * Processes final checkout.  Returns true if final checkout was processed,
	 * otherwise returns false.  If an error occurred during final checkout processing
	 * an error message is added to the cart's errors map.
	 * @param req
	 * @param cart
	 * @param productId
	 * @return
	 */
	private boolean processFinalCheckout(SMTServletRequest req, ShoppingCartVO cart, String productId) {
		log.debug("processing initial 'finalCheckout' check...");
		boolean isFinalCheckOut = false;
		if (Convert.formatBoolean(req.getParameter("finalCheckout"))) {
			if (productId.length() == 0) {
				this.processFinalCheckOut(req, cart);
				isFinalCheckOut = true;
			}
		}
		log.debug("is final checkout? " + isFinalCheckOut);
		return isFinalCheckOut;
	}

	/**
	 * Processes final checkout.  If an error occurred during final checkout processing
	 * an error message is added to the cart's errors map.
	 * @param req
	 * @param cart
	 * @return
	 */
	private void processFinalCheckOut(SMTServletRequest req, ShoppingCartVO cart) {
		log.debug("processing final checkout...");
		String encKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
		PaymentVO payment = new PaymentVO(encKey);
		payment.setExpirationMonth(req.getParameter("expMonth"));
		payment.setExpirationYear(req.getParameter("expYear"));
		payment.setPaymentNumber(req.getParameter("creditCardNumber"));
		payment.setPaymentCode(req.getParameter("securityNumber"));
		payment.setPaymentName(req.getParameter("nameOnCard"));
		cart.setPayment(payment);
		try {
			this.payForOrder(req, cart);
		} catch (DocumentException de) {
			// adding SYSTEM_ERROR because a DocumentException is only thrown
			// if the downstream WebServiceAction call fails
			cart.addError("SYSTEM_ERROR", de.getMessage());
		}
	}
	
	/**
	 * Retrieves user billing/shipping data from the user's session for a logged-in user.
	 * @param req
	 * @param cart
	 */
	private void retrieveCartUserData(SMTServletRequest req, ShoppingCartVO cart) {
		log.debug("processing retrieval of cart user billing/shipping data...");
		// See if there is data on the session to assign from login
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		if (user != null) {
			if (cart.getBillingInfo() == null) {
				log.debug("billing info is null, setting billing info using user session data...");
				cart.setBillingInfo(user);
				cart.setShippingInfo((UserDataVO) user.getUserExtendedInfo());
			}
		}
	}
	
	/**
	 * Retrieve product item information from the cart or from the db and then updates
	 * the cart appropriately.
	 * @param req
	 * @param cart
	 * @param productId
	 */
	private void processItem(SMTServletRequest req, ShoppingCartVO cart, String productId) 
			throws DocumentException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ShoppingCartItemVO item = null;
		//attempt to retrieve item from the cart.
		item = this.retrieveCartItemInfo(cart, productId, site.getSiteId());
		
		if (item == null) {
			//not in cart so query db for product information
			item = this.getProductInfo(productId);
			//add the selected attributes to the shoppingCartItemVO.
			// NOTE: this modifies the productID to differentiate the product in 
			// the cart so it can be found again if need be.
			this.addProductAttributes(req, item);
		}
		
		// check again to see if we were able to obtain an item
		if (item != null) {
			//If the item is not in the cart, set its qty to 1 and add it.
			if(! itemInCart(item, cart)) {
				item.setQuantity(Convert.formatInteger(req.getParameter("qty")));
				this.manageDiscount(cart, item, "addItem", null);
				cart.add(item);
			} else {
				//otherwise select the item from the cart.
				item = cart.getItems().get(item.getProductId());
				//if the item is being updated, set the quantity to qty.
				if(Convert.formatBoolean(req.getParameter("itemUpdate"))) {
					item.setQuantity(Convert.formatInteger(req.getParameter("qty")));
				} else {
					//otherwise we add the new quantity to the old quantity.
					item.setQuantity(Convert.formatInteger(req.getParameter("qty")) + item.getQuantity());
				}
				//finally add the item.
				cart.add(item);
			}
		}
	}
	
	/**
	 * Helper method that utilizes the DiscountManager to manage cart discount operations
	 * @param cart
	 * @param item
	 * @param type
	 * @param processCode
	 */
	protected void manageDiscount(ShoppingCartVO cart, ShoppingCartItemVO item, 
			String type, String processCode) {
		if (dMgr == null) this.initDiscountManager(actionInit, attributes);
		dMgr.setCatalogSiteId(catalogSiteId);
		dMgr.setCart(cart);
		if (type.equalsIgnoreCase("update")) {
			dMgr.updateCartDiscount(cart, processCode);
			if (dMgr.hasErrors()) {
				cart.addError("discountCode", dMgr.getErrors().get("discountCode"));
				cart.addError("discountError", dMgr.getErrors().get("discountError"));
			}
		} else if (type.equalsIgnoreCase("remove")) {
			dMgr.removeCartDiscount(cart);
		} else if (type.equalsIgnoreCase("addItem")) {
			dMgr.manageAddItemDiscount(cart, item);
		} else if (type.equalsIgnoreCase("recalculate")) {
			dMgr.recaculateCartDiscount();
		}
	}
	
	/**
	 * 
	 * @param cart
	 * @param req
	 */
	protected void manageShippingInfo(ShoppingCartVO cart, SMTServletRequest req) {
		String shippingType = StringUtil.checkVal(req.getParameter("shippingType"));
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
			((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).setUserExtendedInfo(user);			
			// Update the shipping info
			cart.setShippingInfo(user);
			req.setParameter("type", "payment");
		}
		
		// If the user changed the shipping/billing info, have them return to the payment screen
		if (Convert.formatBoolean(req.getParameter("edit")))
			req.setParameter("type", "payment");
	}
	
	/**
	 * 
	 * @param cart
	 * @param isShippingMethodUpdate
	 * @throws DocumentException
	 */
	@SuppressWarnings("unchecked")
	public void manageShipping(ShoppingCartVO cart, boolean isShippingMethodUpdate) 
			throws DocumentException {
		if (isShippingMethodUpdate) return;
		log.debug("calculating shipping costs...");
		
		Map<String, ShippingInfoVO> shipping = null;
		// get current shipping methodId
		String currShipMethodId = null;
		if (cart.getShipping() != null) {
			currShipMethodId = cart.getShipping().getShippingMethodId();
			log.debug("current shipping method ID: " + currShipMethodId);
		}		
		
		boolean useShippingDiscount = false;
		double discShippingCost = -1.0;
		if (cart.isDiscounted()) {
			USADiscountVO uDisc = (USADiscountVO) cart.getCartDiscount().get(0);
			log.debug("cart is discounted, type is: " + uDisc.getEnumDiscountType().name());
			if (uDisc.getEnumDiscountType().equals(DiscountType.SHIPPING)) {
				log.debug("cart subTotal/order minimum: " + cart.getSubTotal() + "/" + uDisc.getOrderMinimum());
				if (cart.getSubTotal() >= uDisc.getOrderMinimum()) {
					discShippingCost = uDisc.getDiscountValue();
					useShippingDiscount = true;
				}
			}
		}
				
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, catalogSiteId);
		if (cart.getShippingInfo() != null) {
			// get shipping info from web service
			Element shipInfo = wsa.retrieveShippingInfo(cart.getShippingInfo().getZipCode(), cart.getItems());
			if (! this.checkElementError(cart, shipInfo)) {
				List<Element> sc = shipInfo.selectNodes("Method");
				shipping = new LinkedHashMap<String, ShippingInfoVO>();
				for (int i=0; i < sc.size(); i++) {
					ShippingInfoVO vo = new ShippingInfoVO();
					Element ele = sc.get(i);
					String type = ele.attributeValue("type");
					vo.setShippingMethodId(type);
					vo.setShippingMethodName(type);
					vo.setShippingCost(Convert.formatDouble(ele.getTextTrim()));
					if (type.equalsIgnoreCase(USADiscountVO.DEFAULT_SHIPPING_METHOD)) {
						if (useShippingDiscount) vo.setShippingCost(discShippingCost);
					}
					//log.debug("Shipping: " + vo.getShippingCost() + "|" + vo.getShippingMethodId());
					shipping.put(type,vo);
				}
				cart.setShippingOptions(shipping);
			}
			if (currShipMethodId != null) {
				cart.setShipping(currShipMethodId);
			}
		}
	}
	
	/**
	 * 
	 * @param cart
	 * @throws DocumentException
	 */
	public void calcTaxes(ShoppingCartVO cart) throws DocumentException {
		// Get the tax rate for the shipping address
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, catalogSiteId);
		Element taxElem = wsa.retrieveTaxInfo(cart.getShippingInfo(), cart.getItems().values(), cart.getPromotionCode());
		//if (this.checkElementError(cart, taxElem)) return;
		double tax = Convert.formatDouble(taxElem.getTextTrim());
		// Retrieve the shipping costs as they are taxed as well
		double shipping = 0.0;
		if (cart.getShipping() != null) shipping = cart.getShipping().getShippingCost();
		// Add the subtotal and shipping costs to calculate the tax
		Double taxTotal = ((cart.getSubTotal() + shipping) * tax) * .01;
		// update the tax amount on the cart
		cart.setTaxAmount(taxTotal);
	}
	

	/**
	 * Places the order and sets the OrderCompleteVO on the cart.
	 * @param req
	 * @param cart
	 * @throws DocumentException
	 */
	public void payForOrder(SMTServletRequest req, ShoppingCartVO cart) 
			throws DocumentException {
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, catalogSiteId);
		Element orderElem = wsa.placeOrder(cart, req.getRemoteAddr());
		OrderCompleteVO ocvo = this.parseOrderResponse(cart, orderElem);
		cart.setOrderComplete(ocvo);
	}
	
	/**
	 * Parses the order response Element into an OrderCompleteVO
	 * @param cart
	 * @param root
	 * @return
	 */
	private OrderCompleteVO parseOrderResponse(ShoppingCartVO cart, Element root) {
		OrderCompleteVO order = new OrderCompleteVO();
		if (this.checkElementError(cart, root)) {
			order.setOrderNumber("");
		} else {
			String orderNo = XMLUtil.checkVal(root.element("OrderNumber"));
			if (orderNo.length() > 0) {
				order.setOrderNumber(orderNo);
				order.setStatus(OrderCompleteVO.ORDER_SUCCESSFULLY_COMPLETED);
			} else {
				order.setOrderNumber("");
			}
			order.setShipping(Convert.formatDouble(XMLUtil.checkVal(root.element("ShippingTotal"),true)));
			order.setTax(Convert.formatDouble(XMLUtil.checkVal(root.element("TaxTotal"),true)));
			order.setSubTotal(Convert.formatDouble(XMLUtil.checkVal(root.element("ProductTotal"),true)));
			order.setDiscount(Convert.formatDouble(XMLUtil.checkVal(root.element("DiscountTotal"),true)));
			order.setGrandTotal(Convert.formatDouble(XMLUtil.checkVal(root.element("GrandTotal"),true)));
			order.setMsg(XMLUtil.checkVal(root.element("MSG"),true));
		}
		return order;
	}
	
	/**
	 * Calls the webservice to authenticate a user login if appropriate.
	 * @param req
	 */
	public void checkAuth(SMTServletRequest req) {
		log.debug("checking auth...");
		if (! Convert.formatBoolean(req.getParameter("login"))) {
			return;
		}
		if (Convert.formatBoolean(req.getParameter("bypass"))) {
			return;
		}
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		String loginName = req.getParameter("emailAddress");
		String password = StringUtil.checkVal(req.getParameter("password"));
		UserDataVO user;
		Element userElem = null;
		try {
			userElem = wsa.authenticateMember(loginName, password, catalogSiteId);
			user = wsa.parseUserData(userElem);
			String siteId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getSiteId();
			USARoleModule role = new USARoleModule();
			req.getSession().setAttribute(Constants.USER_DATA, user);
			req.getSession().setAttribute(Constants.ROLE_DATA, role.getUserRole(user.getProfileId(), siteId));
		} catch (DocumentException de) {
			log.error("Error checking user authentication via web service...", de);
		} catch (AuthenticationException ae) {
			log.debug("User login not authenticated: " + ae.getMessage());
			req.setParameter("authLoginFailed", "true");
			req.setParameter("login", "false");
		}
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
	 * @param orgId
	 * @param sbActionId
	 * @return
	 * @throws SQLException
	 */
	public SBModuleVO retrieveModuleData(String orgId, String sbActionId) throws SQLException {
		String s = "select * from sb_action where organization_id = ? and action_id = ? ";
		log.debug("retrieveModuleData sql: " + s + "|" + orgId + "|" + sbActionId);
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
            module.setActionGroupId(rs.getString("action_group_id"));
            module.setPendingSyncFlag(rs.getInt("pending_sync_flg"));
		}
		ps.close();
		return module;
	}
	
	/**
	 * Updated to inflate the ProductVO item on the shoppingCartItemVO
	 * @param productId
	 * @return
	 */
	private ShoppingCartItemVO getProductInfo(String productId) {
		ShoppingCartItemVO vo = null;
		String s = "select * from product where product_id = ? AND PRODUCT_GROUP_ID IS NULL ";
		log.debug("product info SQL: " + s + " | " + productId + " | " + catalogSiteId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, productId);
			ResultSet rs = ps.executeQuery();
			vo = new ShoppingCartItemVO();
			if (rs.next()) {
				vo.setProductId(rs.getString("product_id"));
				vo.setBasePrice(rs.getDouble("msrp_cost_no"));
				vo.setDescription(rs.getString("desc_txt"));
				vo.setProductName(rs.getString("product_nm"));
				vo.setProductCategory(getCategory(rs.getString("product_id")));
				ProductVO product = new ProductVO(rs);
				vo.setProduct(product);
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
	 * Retrieve a cartItem with matching productID
	 * @param cart
	 * @param productID
	 * @param siteId
	 * @return The item that matches the productID
	 */
	private ShoppingCartItemVO retrieveCartItemInfo(ShoppingCartVO cart, String productID, String siteId){
		// if productID does not contain an underscore, return null.  Did not come from cart, is new.
		if (productID.startsWith(siteId + "_")) {
			int pos = siteId.length() + 1;
			if (productID.substring(pos).indexOf("_") == -1) return null;
		} else if (productID.indexOf("_") == -1) {
			return null;
		}
		
		Set<String> keys = cart.getItems().keySet();
		for (String k : keys) {
			if(k.startsWith(productID)) {
				return cart.getItems().get(k);
			}
		}
		return null;
	}
	
	/**
	 * Check if the item is in the cart
	 * @param item
	 * @param cart
	 * @return
	 */
	private boolean itemInCart(ShoppingCartItemVO item, ShoppingCartVO cart){
		for(ShoppingCartItemVO cItem : cart.getItems().values()) {
			if(cItem.getProductId().equals(item.getProductId())) return true;
		}
		return false;
	}
	
	/**
	 * Retrieves all standard/custom attributes for a given product, determines which attributes were 
	 * supplied on the request, and adds those to the product VO.  The product attribute ID is appended
	 * to the product ID to give the item a unique ID within the cart.  The order in which the product attribute
	 * IDs are appended is guaranteed to be the same each time because the requested attributes are
	 * sorted by a comparator before they are applied to the product and to the product ID.  This ensures
	 * that when the XML is built for the order, that the attributes are specified in the XML in a specific order as
	 * required by the various USA catalogs.  
	 * @param req
	 * @param cartItem
	 */
	private void addProductAttributes(SMTServletRequest req, ShoppingCartItemVO cartItem) {
		if (cartItem == null || cartItem.getProduct() == null) return;
		ProductVO product = cartItem.getProduct();
		String pIDAdv = product.getProductId();
		Map<String, String[]> p = req.getParameterMap();
		StringBuilder sb = new StringBuilder();
		sb.append("select * from PRODUCT_ATTRIBUTE_XR where product_id = ? order by attrib2_txt, order_no");
		log.debug("prod attribute SQL: " + sb.toString());
		PreparedStatement ps = null;
		Map<String, ProductAttributeVO> attribs = new LinkedHashMap<String, ProductAttributeVO>();
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, pIDAdv);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ProductAttributeVO pavo = new ProductAttributeVO(rs);
				attribs.put(pavo.getProductAttributeId(), pavo);
			}
		} catch (SQLException sqle) {
			log.error("Unable to add product attributes: " + sqle);
		} finally { try { ps.close(); } catch (Exception e) {log.error("Error closing prepared statement, ", e);}}
		if (attribs.isEmpty()) return;
		
		List<ProductAttributeVO> prodAttribs = new ArrayList<ProductAttributeVO>();
		//iterate through the keys and find the attributes that were specified for this product/item.
		String attribKey = null;
		for(String key : p.keySet()) {
			if (key.startsWith("attribute_")) {
				// custom key is the value of the 'key' on the paramaterMap
				attribKey = p.get(key)[0]; 
			} else if (key.startsWith("custom_")) {
				// custom key is the suffix of the parameterMap 'key'
				attribKey = StringUtil.replace(key, "custom_", "");
			}
			if (attribKey != null) {
				prodAttribs.add(attribs.get(attribKey));
				attribKey = null;
			}
		}
		
		// sort the prodAttribs by attribute type and hierarchy and order
		if (prodAttribs.size() > 1) Collections.sort(prodAttribs, new ProductAttributeComparator());
		// loop the sorted prodAttribs and add their values to the item and modify the product id
		String lookup = null;
		String[] vals = null;
		for (ProductAttributeVO pavo : prodAttribs) {
			//update the cartItem's attribute cost.
			cartItem.setAttributePrice(cartItem.getAttributePrice() + pavo.getMsrpCostNo());
			lookup = pavo.getProductAttributeId();
			//if we have a custom field add its text to the ProdAttrVO's attributes Map so we can display it later.
			if (pavo.getAttributeId().contains("CUSTOM")) {
				lookup = "custom_" + lookup; // this line MUST be BEFORE the vals retrieval.
				vals= p.get(lookup);
				pavo.setAttribute("formVal", vals[0]);
				if(StringUtil.checkVal(vals[0]).length() > 0) pIDAdv += "_" + StringUtil.removeWhiteSpace(vals[0]);
			} else {
				pIDAdv += "_" + lookup;
				lookup = "attribute_" + lookup; // this line MUST be AFTER the pIDAdv update.
			}
			//add the attribute to the ProductVO 
			product.addProdAttribute(lookup, pavo);
			lookup = null;
		}
		//set the advanced productID to the cartItem so we can tell differences
		//between two of the same item with different attributes.
		log.debug("productId of item is now: " + pIDAdv);
		cartItem.setProductId(pIDAdv);
	}
	
	
	/**
	 * Retrieves the category name associated with the given product ID.
	 * @param productId
	 * @return
	 */
	private String getCategory(String productId) {
		StringBuilder s = new StringBuilder();
		s.append("with categories (parent_cd, product_category_cd, category_nm, category_desc, level) as ( ");
		s.append("select parent_cd, a.product_category_cd, a.category_nm, a.category_desc, 0 ");
		s.append("from dbo.product_category a ");
		s.append("inner join product_category_xr b on a.product_category_cd = b.product_category_cd ");
		s.append("where b.product_id = ? ");
		s.append("union all ");
		s.append("select c.parent_cd, c.product_category_cd, c.category_nm, c.category_desc, level + 1 ");
		s.append("from product_category c ");
		s.append("inner join categories pc on pc.parent_cd  = c.product_category_cd ");
		s.append(") ");
		s.append("select category_nm from categories order by level desc; ");
		
		PreparedStatement ps = null;
		String category = "";
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, productId);
			ResultSet rs = ps.executeQuery();
			for (int i=0; rs.next(); i++) {
				String cat = rs.getString(1);
				if (i == 0) {
					category += cat;
				} else {
					category += " > " + cat;
				}
			}
		} catch (SQLException sqle) {
			log.error("Unable to retrieve product", sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {}
		}
		return category;
	}
	
	/**
	 * Sets the catalog site ID based on site ID.
	 * @param req
	 */
	private void setCatalogSiteId(SMTServletRequest req) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		catalogSiteId = site.getSiteId();
	}
	
	/**
	 * Checks Element for error and sets error code and error message on the cart.
	 * @param cart
	 * @param elem
	 * @return
	 */
	private boolean checkElementError(ShoppingCartVO cart, Element elem) {
		if (elem.getName().equalsIgnoreCase("error")) {
			cart.addError("ErrorCode", elem.element("ErrorCode").getTextTrim());
			cart.addError("ErrorMessage", elem.element("ErrorMessage").getTextTrim());
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Initializes the DiscountManager for this cart.
	 * @param actionInit
	 * @param attributes
	 */
	private void initDiscountManager(ActionInitVO actionInit, Map<String,Object> attributes) {
		dMgr = new DiscountManager();
		dMgr.setActionInit(actionInit);
		dMgr.setAttributes(attributes);
	}
}
