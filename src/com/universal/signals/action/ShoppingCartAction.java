package com.universal.signals.action;

// Java 7
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
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.XMLUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
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
 * 02-03-2012: DBargerhuff; Refactored to finalize implementation for new promo code (i.e. discount) processing
 * 2014-08-30: DBargerhuff: Added support for billing comments, support for abandoned cart tracking.
 * 2014-12-01: DBargerhuff: Added support for PayPal checkout.
 ****************************************************************************/
public class ShoppingCartAction extends SBActionAdapter {
	
	public static final int SESSION_PERSISTENCE_CART = 1;
	public static final int COOKIE_PERSISTENCE_CART = 2;
	public static final String BILLING_COMMENTS = "billingComments"; 
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
		log.debug("ShoppingCartAction build....");
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

		// build redirect
		StringBuilder url = new StringBuilder();
		url.append(req.getRequestURI()).append("?");
		url.append("checkout=true");
		url.append("&type=");
		if (StringUtil.checkVal(req.getParameter("type")).length() > 0) {
			url.append(req.getParameter("type"));
		} else {
			url.append("payment");
		}
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
		
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
		StringEncoder se = new StringEncoder();
		String productId = se.decodeValue(StringUtil.checkVal(req.getParameter("productId")));
		
		// Load the cart from our Storage medium.
		Storage container = this.retrieveContainer(req);
		ShoppingCartVO cart = container.load();
		
		// reset cart error map
		cart.flushErrors();
		if (productId.length() == 0 && container.isNewCart()) return cart;
		
		// if this is potentially a paypal checkout process, manage it here (type=paypal).
		if (isPayPalCheckout(req)) {
			return processPayPalCheckout(req, container, cart);
		}

		// check for final checkout processing
		if (isFinalCheckout(req, productId)) return processFinalCheckOut(req, container, cart);
		
		// attempt to load cart billing/shipping data from user session
		this.retrieveCartUserData(req, cart);
		
		// Get the product information and add it to the cart.  Note, if the 
		// quantity is being updated, the new "Total" quantity must be passed here
		boolean itemRemove = Convert.formatBoolean(req.getParameter("itemRemove"));
		boolean updateShipping = Convert.formatBoolean(req.getParameter("updateShipping"));
		if (productId.length() > 0 && ! itemRemove && Convert.formatInteger(req.getParameter("qty")) > 0) {
			// If order is complete and user is trying to add a product 
			// to the cart (we'll assume that they are trying to create a new cart).
			if (cart.isOrderCompleted()) cart = flushCart(container);
			
			// process the item
			this.processItem(req, cart, productId);
			
		} else if (itemRemove || StringUtil.checkVal(req.getParameter("qty")).length() > 0) {
			cart.remove(productId);
			
		} else if (Convert.formatBoolean(req.getParameter("updatePromoCode"))) {
			// process promo code
			this.manageDiscount(cart, null, "update", req.getParameter("promoCode"));
			
		} else if (Convert.formatBoolean(req.getParameter("removePromoCode"))) {
			this.manageDiscount(cart, null, "remove", null);
			
		} else if (Convert.formatBoolean(req.getParameter("finalCheckout"))) {	
			return this.processFinalCheckOut(req, container, cart);
			
		} else if (updateShipping) {
			String shippingId = req.getParameter("selShipping");
			//log.debug("updateShipping is true, setting shipping to shippingId: " + shippingId);
			cart.setShipping(shippingId);
		}
		
		// If the request is for shipping manage the data
		if (StringUtil.checkVal(req.getParameter("shippingType")).length() > 0) {
			//log.debug("shippingType has length: " + req.getParameter("shippingType"));
			this.manageShippingInfo(cart, req);
		}
		
		// Finally, recalculate the cart (shipping/taxes, etc.)
		this.manageDiscount(cart, null, "recalculate", null);
		this.manageShipping(req, cart, updateShipping);
		
		// if checking out, calculate taxes.
		boolean checkOut = Convert.formatBoolean(req.getParameter("checkout"));
		if (checkOut) this.calcTaxes(cart);
		
		// Resave the cart for persistence reasons
		saveCart(req, container, cart);
		
		return cart;
	}
	
	/**
	 * Persists the current state of the cart.
	 * @param req
	 * @param container
	 * @param cart
	 * @throws ActionException
	 */
	private void saveCart(SMTServletRequest req, Storage container, ShoppingCartVO cart) 
			throws ActionException {
		// Resave the cart for persistence reasons
		UserDataVO sessUser = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (sessUser != null) container.setProfileId(sessUser.getProfileId());
		container.setSourceId(catalogSiteId);
		container.save(cart);
	}
	
	/**
	 * Retrieves the Storage container
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected Storage retrieveContainer(SMTServletRequest req) 
			throws ActionException {
		Map<String, Object> attrs = new HashMap<>();
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
	 * Determines if this is a PayPal checkout operation.
	 * @param req
	 * @return
	 */
	private boolean isPayPalCheckout(SMTServletRequest req) {
		boolean doPayPal = false;
		if (StringUtil.checkVal(req.getParameter("type")).equalsIgnoreCase("paypal")) {
			// if this is a 'start' operation, we return false.
			String pp = StringUtil.checkVal(req.getParameter("paypal"), null);
			if (pp != null && ! pp.equalsIgnoreCase("start")) doPayPal = true;
		}
		log.debug("isPayPalCheckout: " + doPayPal);
		return doPayPal;
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
	private boolean isFinalCheckout(SMTServletRequest req, String productId) {
		log.debug("processing initial 'finalCheckout' check...");
		boolean isFinalCheckOut = false;
		if (Convert.formatBoolean(req.getParameter("finalCheckout"))) {
			if (productId.length() == 0) isFinalCheckOut = true;
		}
		log.debug("isFinalCheckout: " + isFinalCheckOut);
		return isFinalCheckOut;
	}

	/**
	 * Processes final checkout.  If an error occurred during final checkout processing
	 * an error message is added to the cart's errors map.
	 * @param req
	 * @param container
	 * @param cart
	 */
	private ShoppingCartVO processFinalCheckOut(SMTServletRequest req, Storage container,
			ShoppingCartVO cart) {
		log.debug("processing final checkout...");
		// format the payment
		formatPayment(req, cart);
		try {
			// pay for the order via the webservice if not a 'paypal' order
			payForOrder(req, cart);
		} catch (DocumentException de) {
			// adding SYSTEM_ERROR because a DocumentException is only thrown
			// if the downstream WebServiceAction call fails
			cart.addError("SYSTEM_ERROR", de.getMessage());
		}
		
		// if order complete, return a cart for display and flush the original cart.
		if (cart.isOrderCompleted()) {
			ShoppingCartVO displayCart = new ShoppingCartVO();
			displayCart.setBillingInfo(cart.getBillingInfo());
			displayCart.setShippingInfo(cart.getShippingInfo());
			displayCart.setShippingOptions(cart.getShippingOptions());
			displayCart.setItems(cart.getItems());
			displayCart.setOrderComplete(cart.getOrderComplete());
			flushCart(container);
			return displayCart;
		} else {
			return cart;
		}
	}
	
	/**
	 * @throws InvalidDataException 
	 * Processes PayPal checkout transaction.
	 * @param req
	 * @param container
	 * @param cart
	 * @return
	 */
	private ShoppingCartVO processPayPalCheckout(SMTServletRequest req, 
			Storage container, ShoppingCartVO cart) throws DocumentException {
		log.debug("processPayPalCheckout...");
		String payPalAction = StringUtil.checkVal(req.getParameter("paypal"));
		log.debug("operation is: " + payPalAction);
		if (payPalAction.equalsIgnoreCase("start")) {
			manageShipping(req, cart, false);
			return cart;
		} else if (payPalAction.equalsIgnoreCase("set")) {
			
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			StringBuilder siteAlias = new StringBuilder(site.getFullSiteAlias());
			siteAlias.append("/cart/");
			
			// set the cart cancel urls
			cart.setCartCheckoutCancelUrl(siteAlias.toString());
			log.debug("checkout cancel url: " + cart.getCartCheckoutCancelUrl());
			
			// set the return URL
			siteAlias.append("?checkout=true&type=paypal&paypal=get");
			cart.setCartCheckoutReturnUrl(siteAlias.toString());
			log.debug("checkout return url: " + cart.getCartCheckoutReturnUrl());

		}
		
		String encKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
		log.debug("encKey: " + encKey);

		// call PayPal and process based on "paypal" value.
		PayPalCheckoutManager ppm = null;
		
		try {
			ppm = new PayPalCheckoutManager(req, cart);
			ppm.setDbConn(dbConn);
			ppm.setAttributes(attributes);
			ppm.setCatalogSiteId(catalogSiteId);
			ppm.processTransaction();
			
		} catch (IOException e) {
			log.error("Error: PayPal checkout is unavailable, ", e);
			cart.addError("SYSTEM_ERROR", "PayPal checkout is temporarily unavailable. (System)");
		} catch (SQLException e) {
			log.error("Error: Merchant's PayPal credentials could not be retrieved, ", e);
			cart.addError("SYSTEM_ERROR", "Merchant authentication error. (Database)");
		} catch (EncryptionException e) {
			log.error("Error: Merchant's PayPal credentials could not be decrypted, ", e);
			cart.addError("SYSTEM_ERROR", "Merchant authentication error. (Encryption)");
		} catch (InvalidDataException e) {
			log.error("Error: Invalid PayPal checkout transaction type requested, ", e);
			cart.addError("SYSTEM_ERROR", "Invalid PayPal checkout request. (Transaction Type)");
		} catch (Exception e) {
			log.error("Error: An unknown error occurred, ", e);
			cart.addError("SYSTEM_ERROR", "An unknown error has occurred.");
		}

		// if errors occurred, return
		if (cart.hasErrors()) {
			log.debug("cart has errors:");
			for (String err : cart.getErrors().keySet()) {
				log.error("USA Shopping Cart error|msg: " + err + "|" + cart.getErrors().get(err));
			}
			return cart;
		}
		
		// save the cart
		try {
			saveCart(req, container, cart);
		} catch (ActionException e) {
			log.error("Error: Unable to save changes to cart, ", e);
			cart.addError("SYSTEM_ERROR", "Error: Unable to save changes to shopping cart.");
			return cart;
		}
		
		// if this was the last operation, do final checkout
		
		if (payPalAction.equalsIgnoreCase("do")) {
			log.debug("doing paypal final checkout...");
			try {
				payForOrder(req, cart);
			} catch (DocumentException de) {
				// means error occurred submitting to USA's webservice
				cart.addError("SYSTEM_ERROR", de.getMessage());
				return cart;
			}
			
			// if order complete, return a cart for display and flush the original cart.
			if (cart.isOrderCompleted()) {
				log.debug("order is complete...");
				ShoppingCartVO displayCart = new ShoppingCartVO();
				displayCart.setBillingInfo(cart.getBillingInfo());
				displayCart.setShippingInfo(cart.getShippingInfo());
				displayCart.setShippingOptions(cart.getShippingOptions());
				displayCart.setItems(cart.getItems());
				displayCart.setOrderComplete(cart.getOrderComplete());
				flushCart(container);
				return displayCart;
			}
		}
		
		return cart;

	}
	
	/**
	 * Formats the payment and set it on the cart.
	 * @param req
	 * @param cart
	 */
	private void formatPayment(SMTServletRequest req, ShoppingCartVO cart) {
		String encKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
		PaymentVO payment = new PaymentVO(encKey);
		payment.setExpirationMonth(req.getParameter("expMonth"));
		payment.setExpirationYear(req.getParameter("expYear"));
		payment.setPaymentNumber(req.getParameter("creditCardNumber"));
		payment.setPaymentCode(req.getParameter("securityNumber"));
		payment.setPaymentName(req.getParameter("nameOnCard"));
		cart.setPayment(payment);
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
		wsa.setDBConnection(dbConn); // for use by transaction logger
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, catalogSiteId);
		Element orderElem = wsa.placeOrder(req, cart, req.getRemoteAddr());
		OrderCompleteVO ocvo = parseOrderResponse(cart, orderElem);
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
	 * Retrieves user billing/shipping data from the user's session for a logged-in user.
	 * @param req
	 * @param cart
	 */
	private void retrieveCartUserData(SMTServletRequest req, ShoppingCartVO cart) {
		log.debug("processing retrieval of cart user data from session...");
		// See if there is data on the session to assign from login
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		if (user != null) {
			//log.debug("user session data is NOT null...");
			if (cart.getBillingInfo() == null) {
				//log.debug("user billing info is NOT null, setting billing/shipping info from user session data.");
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
		ShoppingCartItemVO item = null;
		//attempt to retrieve item from the cart.
		item = this.retrieveCartItemInfo(cart, productId, catalogSiteId);
		
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
	 * Manages user's billing or shipping information
	 * @param cart
	 * @param req
	 */
	protected void manageShippingInfo(ShoppingCartVO cart, SMTServletRequest req) {
		log.debug("manageShippingInfo...");
		String shippingType = StringUtil.checkVal(req.getParameter("shippingType"));
		log.debug("shippingType: " + shippingType);
		UserDataVO user = new UserDataVO(req);
		if ("billing".equalsIgnoreCase(shippingType)) {
			// retrieve profileId
			retrieveProfileId(req, user);
			
			// get billing comments
			String bComm = StringUtil.checkVal(req.getParameter("billingComments"));
			if (bComm.length() > 0) user.addAttribute(BILLING_COMMENTS, bComm);
			
			// set billing info on cart.
			cart.setBillingInfo(user);

			// Add the user to the session
			req.getSession().setAttribute(Constants.USER_DATA, user);
			
			if (Convert.formatBoolean(req.getParameter("useBilling"))) {
				cart.setShippingInfo(user);
				req.setParameter("type", "payment", true);
			} else { 
				req.setParameter("type", "Shipping", true);
			}
		} else {
			// Add the user shipping info to the session
			((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA)).setUserExtendedInfo(user);			
			// Update the shipping info
			cart.setShippingInfo(user);
			req.setParameter("type", "payment", true);
		}
		
		// If the user changed the shipping/billing info, have them return to the payment screen
		if (Convert.formatBoolean(req.getParameter("edit")))
			req.setParameter("type", "payment", true);
	}
	
	/**
	 * Retrieves the user profile ID based on the session data.  If that fails, attempts
	 * to retrieve user profile ID based on the user data passed in on the request.  If
	 * that too fails, we create a new profile based on the user data passed in on the
	 * request.  Any profile ID found/created is set on the 'user' object passed in to 
	 * the method.
	 * @param req
	 * @param user
	 */
	private void retrieveProfileId(SMTServletRequest req, UserDataVO user) {
		log.debug("retrieving profileId...");
		String profileId = null;
		ProfileManager pm = null;
		// first try to get profileId from session
		UserDataVO sessUser = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (sessUser != null) {
			profileId = sessUser.getProfileId();
		}
		if (StringUtil.checkVal(profileId).length() > 0) {
			user.setProfileId(profileId);
		} else {
			// profileId is not on sessioon, check for a profile based on user data passed in
			if (StringUtil.checkVal(user.getEmailAddress()).length() > 0) {
				pm = ProfileManagerFactory.getInstance(attributes);
				try {
					profileId = pm.checkProfile(user, dbConn);
					if (StringUtil.checkVal(profileId).length() == 0) {
						// no profile found, create it (profileId is set on 'user' object by profile manager).
						pm.updateProfile(user, dbConn);
					} else {
						// use the profileId found.
						user.setProfileId(profileId);
					}
				} catch (DatabaseException de) {
					log.error("Error checking/updating profile, ", de);
				}
			}
		}
		//log.debug("user profileId: " + profileId);
		// finally, if this was an 'edit' billing operation, try to update the profile
		if (StringUtil.checkVal(req.getParameter("type")).equalsIgnoreCase("edit")) {
			if (pm == null) pm = ProfileManagerFactory.getInstance(attributes);
			try {
				pm.updateProfilePartially(attributes, user, dbConn);
			} catch (DatabaseException de) {
				log.error("Error updating profile, ", de);
			}
		}
		
	}
	
	/**
	 * 
	 * @param cart
	 * @param isShippingMethodUpdate
	 * @throws DocumentException
	 */
	@SuppressWarnings("unchecked")
	public void manageShipping(SMTServletRequest req, ShoppingCartVO cart, 
			boolean isShippingMethodUpdate) throws DocumentException {
		log.debug("manageShipping...");
		//log.debug("isShippingMethodUpdate: " + isShippingMethodUpdate);
		// if simply updating method selection, return.
		if (isShippingMethodUpdate) return;

		// check to see if we are initializing shipping options
		if (Convert.formatBoolean(req.getParameter("initializeShipping"))) {
			if (cart.getShippingInfo() == null) {
				// no user shipping info, is a PayPal checkout.
				UserDataVO shipInfo = new UserDataVO();
				shipInfo.setZipCode(req.getParameter("shippingZipCode"));
				cart.setShippingInfo(shipInfo);
			}
		}
		
		Map<String, ShippingInfoVO> shipping = null;
		// get current shipping methodId
		String currShipMethodId = null;
		if (cart.getShipping() != null) {
			currShipMethodId = cart.getShipping().getShippingMethodId();
		}		
		
		boolean useShippingDiscount = false;
		double discShippingCost = -1.0;
		if (cart.isDiscounted()) {
			USADiscountVO uDisc = (USADiscountVO) cart.getCartDiscount().get(0);
			if (uDisc.getEnumDiscountType().equals(DiscountType.SHIPPING)) {
				if (cart.getSubTotal() >= uDisc.getOrderMinimum()) {
					discShippingCost = uDisc.getDiscountValue();
					useShippingDiscount = true;
				}
			}
		}
				
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, catalogSiteId);
		String zipCode = (cart.getShippingInfo() != null ? cart.getShippingInfo().getZipCode() : null);
		// get shipping info from web service
		Element shipInfo = wsa.retrieveShippingInfo(zipCode, cart.getItems());
		if (! this.checkElementError(cart, shipInfo)) {
			List<Element> sc = shipInfo.selectNodes("Method");
			shipping = new LinkedHashMap<>();
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
	 * Calls the webservice to authenticate a user login if appropriate.
	 * @param req
	 */
	public void checkAuth(SMTServletRequest req) {
		log.debug("checking auth...");
		if (! Convert.formatBoolean(req.getParameter("login"))) {
			return;
		} else if (Convert.formatBoolean(req.getParameter("bypass"))) {
			return;
		}
		
		String loginName = req.getParameter("emailAddress");
		String password = StringUtil.checkVal(req.getParameter("password"));
		
		UserDataVO user;
		Element userElem = null;
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
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
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, catalogSiteId + "_" + productId);
			ResultSet rs = ps.executeQuery();
			vo = new ShoppingCartItemVO();
			if (rs.next()) {
				ProductVO product = new ProductVO(rs);
				// use custom product number which is product ID without the catalog site ID prefix
				product.setProductId(product.getCustProductNo());
				//vo.setProductId(rs.getString("product_id"));
				vo.setProductId(product.getProductId());
				vo.setBasePrice(rs.getDouble("msrp_cost_no"));
				vo.setDescription(rs.getString("desc_txt"));
				vo.setProductName(rs.getString("product_nm"));
				//vo.setProductCategory(getCategory(rs.getString("product_id")));
				vo.setProduct(product);
			}
		} catch (SQLException sqle) {
			log.error("Unable to retrieve product", sqle);
		} finally {
			try {
				if (ps != null) ps.close();
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
	private ShoppingCartItemVO retrieveCartItemInfo(ShoppingCartVO cart, String productId, String siteId){
		// if productId does not contain an underscore, return null.  Did not come from cart, is new.
		if (productId.startsWith(siteId + "_")) {
			int pos = siteId.length() + 1;
			if (productId.substring(pos).indexOf("_") == -1) return null;
		} else if (productId.indexOf("_") == -1) {
			return null;
		}
		
		Set<String> keys = cart.getItems().keySet();
		for (String k : keys) {
			if(k.startsWith(productId)) {
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
		
		// 1: Retrieve product attributes for this product
		StringBuilder sb = new StringBuilder();
		sb.append("select a.*, b.attribute_nm 	from PRODUCT_ATTRIBUTE_XR a ");
		sb.append("inner join PRODUCT_ATTRIBUTE b on a.attribute_id = b.ATTRIBUTE_ID ");
		sb.append("where product_id = ? order by a.attrib2_txt, a.order_no ");
		log.debug("addProductAttributes SQL: " + sb.toString());
		
		Map<String, ProductAttributeVO> attribs = new LinkedHashMap<>();
		ProductVO product = cartItem.getProduct();
		String pIDAdv = product.getProductId();
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, catalogSiteId + "_" + pIDAdv);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ProductAttributeVO pavo = new ProductAttributeVO(rs);
				attribs.put(pavo.getProductAttributeId(), pavo);
			}
		} catch (SQLException sqle) {
			log.error("Unable to add product attributes: " + sqle);
		} finally { try { ps.close(); } catch (Exception e) {log.error("Error closing prepared statement, ", e);}}
		
		if (attribs.isEmpty()) return;
		
		/* 2: Iterate the request param keys to find product attribute selections that
		 * were made for this product. This populates a List of ProductAttributeVOs
		 * representing only the attributes chosen. */
		List<ProductAttributeVO> prodAttribs = new ArrayList<>();
		String prodAttribIdKey = null;
		Map<String, String[]> paramsMap = req.getParameterMap();
		for(String paramKey : paramsMap.keySet()) {
			if (paramKey.startsWith("attribute_")) {
				// custom key is the value of the 'key' on the paramaterMap
				if (paramsMap.get(paramKey) == null || 
						paramsMap.get(paramKey).length == 0) continue;
				
				// loop the attributes and add.
				for (String attribIdKey : paramsMap.get(paramKey)) {
					if (attribIdKey != null) prodAttribs.add(attribs.get(attribIdKey));
				}
				
			} else if (paramKey.startsWith("custom_")) {
				// custom key is the suffix of the parameterMap 'key'
				prodAttribIdKey = StringUtil.replace(paramKey, "custom_", "");
				if (prodAttribIdKey != null) prodAttribs.add(attribs.get(prodAttribIdKey));
			}
			
			prodAttribIdKey = null;

		}
		
		/* 3: Sort the product attribute selections and put them into the proper sequence.
		 * This is critical as it ensures that the product attribute portion of an order
		 * request is formatted in the correct order. 
		 */
		if (prodAttribs.size() > 1) Collections.sort(prodAttribs, new ProductAttributeComparator());
		
		// 4:  Loop the sorted product attribute VOs, add their values to item, modify the product id
		String pAttrId = null;
		String[] vals = null;

		StringEncoder se = new StringEncoder();

		// instantiate a LinkedHashMap to hold custom attribute mappings so we can
		// add them to the product VO last.
		Map<String, ProductAttributeVO> custom = new LinkedHashMap<>();

		for (ProductAttributeVO pavo : prodAttribs) {
			//update the cartItem's attribute cost.
			cartItem.setAttributePrice(cartItem.getAttributePrice() + pavo.getMsrpCostNo());
			pAttrId = pavo.getProductAttributeId();
			//if we have a custom field add its text to the ProdAttrVO's attributes Map so we can display it later.
			if (pavo.getAttributeId().contains("CUSTOM")) {
				pAttrId = "custom_" + pAttrId; // this line MUST be BEFORE the vals retrieval.
				vals= paramsMap.get(pAttrId);
				pavo.setAttribute("formVal", vals[0]);
				if(StringUtil.checkVal(vals[0]).length() > 0) pIDAdv += "_" + StringUtil.removeWhiteSpace(vals[0]);
				custom.put(pAttrId, pavo);
				pAttrId = null;
				continue;
			} else {
				pIDAdv += "_" + pAttrId;
				pAttrId = "attribute_" + pAttrId; // this line MUST be AFTER the pIDAdv update.
			}
			//add the attribute to the ProductVO 
			product.addProdAttribute(pAttrId, pavo);
			pAttrId = null;
		}

		/* set the advanced productID to the cartItem so we can tell differences
		 * between two of the same item with different attributes.  We decode any
		 * HTML-entities and then replace them so that the advanced productID
		 * only contains alphanumeric, underscore, or dash characters.  Otherwise
		 * the cart views do not work properly.
		 */
		pIDAdv = formatAdvancedProductId(se,pIDAdv);

		/* 5: Now add the custom attributes to the product's product attribute map.
		 * We do it this way to ensure that the custom attributes are the last in the
		 * sequence. */
		if (custom.size() > 0) {
			for (String pAId : custom.keySet()) {
				product.addProdAttribute(pAId, custom.get(pAId));
			}
		}
		
		/* 6: Set the advanced productID on the cartItem so we can differenciate
		 * between two of the same item with different attributes.*/
		cartItem.setProductId(pIDAdv);
		log.debug("advanced productId: " + pIDAdv);
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
	 * Flushes the container and returns a new cart.
	 * @param container
	 * @return
	 */
	private ShoppingCartVO flushCart(Storage container) {
		log.debug("flushCart...");
		try {
			container.flush();
			return container.load();
		} catch (ActionException ae) {
			log.error("Error flushing cart, returning new empty cart, ", ae);
			return new ShoppingCartVO();
		}
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
	
	/**
	 * Formats the advanced product ID by decoding HTML entities into their
	 * character equivalents and then removing anything that isn't alphanumeric,
	 * an underscore, or a dash.
	 * @param se
	 * @param val
	 * @return
	 */
	private String formatAdvancedProductId(StringEncoder se, String val) {
		if (val == null) return val;
		val = se.decodeValue(val);
		return StringUtil.formatFileName(val, false);
	}
}
