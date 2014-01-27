package com.fastsigns.product.keystone.checkout;

import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import net.sf.json.JSONObject;

import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.MyProfileAction;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.security.FastsignsSessVO;
import com.fastsigns.security.FsKeystoneLoginModule;
import com.fastsigns.security.KeystoneProfileManager;
import com.fastsigns.security.KeystoneUserDataVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;

/****************************************************************************
 * <b>Title</b>: CheckoutUtil.java<p/>
 * <b>Description: Handles the complexities of calling the SMTProxy, as well as
 * submitting an order to Keystone.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2013
 ****************************************************************************/
public class CheckoutUtil {
	
	protected static Logger log;
	private Map<String, Object> attributes;

	public CheckoutUtil(Map<String, Object> attributes) {
		log = Logger.getLogger(CheckoutUtil.class);
		this.attributes = attributes;
	}
	
	
	/**
	 * this method is called 'build' because it gets called from the build method of
	 * ShoppingCartAction.  The two handlers here are "process checkout form"
	 * and "process order"  (when the confirmation page is submitted).
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException
	 */
	public ShoppingCartVO build(SMTServletRequest req, ShoppingCartVO cart) 
			throws ActionException {
		String step = req.getParameter("step");
		log.debug("step=" + step);
		
		if ("createAccount".equalsIgnoreCase(step)) {
			processCreateAccount(req, cart);
		} else if ("checkout".equalsIgnoreCase(step)) {
			cart = processCheckoutScreen(req, cart);
		} else if ("confirm".equalsIgnoreCase(step)) {
			cart = submitOrder(req, cart);
		} else if ("shippingtax".equalsIgnoreCase(step)) {
			req.setParameter("shippingTax", StringUtil.checkVal(getShippingTax(req, cart)));
		}
		
		return cart;
	}
	
	
	/**
	 * this method is called 'retrieve' because it gets called from the retrieve method of
	 * ShoppingCartAction.  The two handlers here are "load confirm page"
	 * and "load complete page"  (a receipt once the order is submitted).
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException
	 */
	public ShoppingCartVO retrieve(SMTServletRequest req, ShoppingCartVO cart) 
	throws ActionException {
		String step = req.getParameter("step");
		log.debug("step=" + step);
		
		if ("confirm".equalsIgnoreCase(step)) {
			cart = loadConfirmScreen(req, cart);
			
		} else if ("complete".equalsIgnoreCase(step)) {
			cart = loadCompleteScreen(req, cart);
		}
		
		return cart;
	}
	
	
	
	/**
	 * this method processes the 'checkout' screen.
	 * We need to capture the shipping, billing, payment, account, etc. info 
	 * incoming from the checkout screen and save it into the cart object.
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException any error that may occur, gracefully wrapped
	 */
	private ShoppingCartVO processCheckoutScreen(SMTServletRequest req, ShoppingCartVO cart)
	throws ActionException {
		log.info("saving checkoutScreen");
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		//save the billing info to the cart
		UserDataVO billing = new UserDataVO(req);
		billing.setProfileId(user.getProfileId());
		billing.addAttribute("companyNm", req.getParameter("company"));
		billing.addAttribute("addressId", req.getParameter("address_id"));
		cart.setBillingInfo(billing);
		
		//save the shipping info to the cart
		UserDataVO shipping = new UserDataVO();
		shipping.setProfileId(user.getProfileId());
		if (Convert.formatBoolean(req.getParameter("sameShippingBilling"))) {
			cart.setUseBillingForShipping(true);
			shipping.setData(req);
			shipping.addAttribute("companyNm", req.getParameter("company"));
			shipping.addAttribute("addressId", req.getParameter("address_id"));

		} else {
			cart.setUseBillingForShipping(false);
			shipping.setFirstName(req.getParameter("s_firstName"));
			shipping.setLastName(req.getParameter("s_lastName"));
			shipping.setAddress(req.getParameter("s_address"));
			shipping.setAddress2(req.getParameter("s_address2"));
			shipping.setCity(req.getParameter("s_city"));
			shipping.setState(req.getParameter("s_state"));
			shipping.setZipCode(req.getParameter("s_zipCode"));
			shipping.setMainPhone(req.getParameter("s_mainPhone"));
			shipping.addAttribute("companyNm", req.getParameter("s_company"));
			shipping.addAttribute("addressId", req.getParameter("s_address_id"));
		}
		boolean inStore = Convert.formatBoolean(req.getParameter("inStorePickup"));
		req.getSession().setAttribute("inStorePickup", (inStore) ? "true" : "");
			
		
		shipping.setEmailAddress(sessVo.getProfile(webId).getEmailAddress());
		shipping.setProfileId(user.getProfileId());
		cart.setShippingInfo(shipping);
		
		attributes.put("nextStep", "confirm");
		return cart;
	}
	
	
	/**
	 * this method handles the generation of the 'confirm' screen.
	 * We need to call the proxy to load taxation, and update the cart accordingly.
	 * We also need to call the proxy and load shipping options.
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException any error that may occur, gracefully wrapped
	 */
	private ShoppingCartVO loadConfirmScreen(SMTServletRequest req, ShoppingCartVO cart)
	throws ActionException {
		log.debug("loading confirm screen");
		
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		if (sessVo.getProfile(webId).getUserId() == null) {
			//not logged in, or no account to retrieve
			throw new ActionException(ErrorCodes.ERR_NOT_AUTHORIZED);
		}
		if (sessVo.getFranchise(webId) == null || sessVo.getFranchise(webId).getFranchiseId() == null) {
			try {
				sessVo = this.loadFranchiseVO(sessVo, webId);
			} catch (Exception e) {
				throw new ActionException("could not load franchise info", e);
			}
		}
		log.debug("loaded Session Data, retrieving shipping options.");
		
		attributes.put("franchise", sessVo.getFranchise(webId));
		attributes.put(KeystoneProxy.FRAN_SESS_VO, sessVo);
		
		//create a ShippingCost call to the SMTProxy.
		ShippingRequestCoordinator src = new ShippingRequestCoordinator(attributes);
		try {
			cart = src.retrieveShippingOptions(cart);
			log.debug("retrieved shipping options.");
			if(cart.getShippingOptions().size() == 0){
				req.setParameter("nextStep", "");
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				StringBuilder url = new StringBuilder(page.getFullPath());
				url.append("?msg=" + "Error Retrieving Shipping Information, please see cart.");
				req.setAttribute(Constants.REDIRECT_REQUEST, true);
				req.setAttribute(Constants.REDIRECT_URL, url.toString());
			}
		} catch (Exception e) {
			log.error("could not load shipping", e);
			throw new ActionException(e.getMessage());
		}
		log.debug("Retrieving Tax Information");
		//create a Tax call to the SMTProxy
		TaxationRequestCoordinator tax = new TaxationRequestCoordinator(attributes);
		try {
			ShoppingCartItemVO ship = new ShoppingCartItemVO();
			ship.setProductId("shipping");
			ship.setBasePrice(Convert.formatDouble(req.getParameter("shippingCost")));
			ship.setQuantity(1);
			KeystoneProductVO shipVo = new KeystoneProductVO();
			shipVo.setTax_code_id(6);
			shipVo.setMsrpCostNo(ship.getBasePrice());
			ship.setProduct(shipVo);
			cart.setSubTotal(cart.getSubTotal() + ship.getBasePrice());
			cart.add(ship);
			cart = tax.retrieveTaxOptions(cart);
			cart.remove("shipping");
		} catch (Exception e) {
			log.error(e);
			throw new ActionException(e.getMessage());
		}
		log.debug("Retrieved Tax Information, returning cart.");
		return cart;
	}
	
	private double getShippingTax(SMTServletRequest req, ShoppingCartVO cart) throws ActionException {
		if(cart.getItems().size() > 0){
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		if (sessVo.getProfile(webId).getUserId() == null) {
			//not logged in, or no account to retrieve
			throw new ActionException(ErrorCodes.ERR_NOT_AUTHORIZED);
		}
		if (sessVo.getFranchise(webId) == null) {
			try {
				sessVo = this.loadFranchiseVO(sessVo, webId);
			} catch (Exception e) {
				log.error(e);
				throw new ActionException("could not load franchise info", e);
			}
		}
			
		attributes.put(KeystoneProxy.FRAN_SESS_VO, sessVo);
		attributes.put("franchise", sessVo.getFranchise(webId));
		//create a Tax call to the SMTProxy
		TaxationRequestCoordinator tax = new TaxationRequestCoordinator(attributes);
		log.debug("Shipping Cost: " + Convert.formatDouble(req.getParameter("shippingCost")));
		try {
			ShoppingCartItemVO ship = new ShoppingCartItemVO();
			ship.setProductId("shipping");
			ship.setBasePrice(Convert.formatDouble(req.getParameter("shippingCost")));
			ship.setQuantity(1);
			KeystoneProductVO shipVo = new KeystoneProductVO();
			shipVo.setTax_code_id(6);
			shipVo.setMsrpCostNo(ship.getBasePrice());
			ship.setProduct(shipVo);
			cart.setSubTotal(cart.getSubTotal() + ship.getBasePrice());
			cart.add(ship);
			cart = tax.retrieveTaxOptions(cart);
			cart.remove("shipping");
		} catch (Exception e) {
			log.error("Could not retrieve shipping tax: ", e);
			throw new ActionException(e.getMessage());
		}
		}
		return cart.getTaxAmount();
	}
	
	/**
	 * this method submits the entire order directly to Keystone.  We need to take
	 * the cart object and serialize it into JSON according to the Keystone API.
	 * Use KeystoneProxy to submit the JSON object to Keystone, then evaluate the response.
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException any error that may occur, gracefully wrapped
	 */
	private ShoppingCartVO submitOrder(SMTServletRequest req, ShoppingCartVO cart)
	throws ActionException {
		log.info("submitting order to Keystone");
		
		//save the credit card info
		PaymentVO payment = new PaymentVO((String)attributes.get(Constants.ENCRYPT_KEY));
		payment.setPaymentType(StringUtil.checkVal(req.getParameter("cardType")));
		payment.setPaymentName(StringUtil.checkVal(req.getParameter("cardName")));
		payment.setPaymentNumber(StringUtil.checkVal(req.getParameter("cardNumber")));
		payment.setExpirationMonth(StringUtil.checkVal(req.getParameter("cardMonth")));
		payment.setExpirationYear(StringUtil.checkVal(req.getParameter("cardYear")));
		payment.setPaymentCode(StringUtil.checkVal(req.getParameter("cvvNumber")));
		payment.setPaymentType(StringUtil.checkVal(req.getParameter("cardType")));
		cart.setPurchaseOrderNo(StringUtil.checkVal(req.getParameter("purchaseOrderNo")));
		cart.setPayment(payment);
		
		//Save the selected Shipping Method.
		cart.setShipping(req.getParameter("shippingMethod"));
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		attributes.put(KeystoneProxy.FRAN_SESS_VO, sessVo);
		attributes.put("jobId", req.getSession().getAttribute("jobId"));
		attributes.put("webId", req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID));
		
		//if payment fails we'll get back a jobId...
		//we must pass that back to Keystone on the retry attempt!
		OrderSubmissionCoordinator coord = new OrderSubmissionCoordinator(attributes);
		cart = coord.submitOrder(cart);
		if(!Convert.formatBoolean(cart.getErrors().get("success"))){
			req.getSession().setAttribute("jobId", cart.getErrors().get("jobId"));
			//String message = cart.getErrors().get("message");
			//if(message.equalsIgnoreCase("Payment failed.")){
				req.setParameter("step", "checkout");
				attributes.put("nextStep", "checkout");
				throw new ActionException(cart.getErrors().get("message"));
			//}
		} else {
			cart.getErrors().put("Complete", "true");
		}
		attributes.put("nextStep", "complete");
		return cart;
	}
	
	/**
	 * this method handles the generation of the 'receipt' screen.
	 * The View here is purely cosmetic (order summary).
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException any error that may occur, gracefully wrapped
	 */
	private ShoppingCartVO loadCompleteScreen(SMTServletRequest req, ShoppingCartVO cart)
	throws ActionException {
		log.info("loading complete screen");
		
		/*
		 *  we no longer need the jobId, need to flush so future jobs will
		 *  submit correctly.
		 */
		req.getSession().removeAttribute("jobId");
		HttpServletResponse resp = (HttpServletResponse) attributes.get(GlobalConfig.HTTP_RESPONSE);

		//clear the reference cookie
    	Cookie c = new Cookie(Storage.CART_OBJ, getObjectId(req));
    	c.setMaxAge(0);
    	c.setPath("/");
    	resp.addCookie(c);
		
		//this should go straight to View and print what we have on the session
		//once the data is printed, we'll purge the cart from the session
		return cart;
	}
	
	private String getObjectId(SMTServletRequest req){
		//try to get objectId off the session before parsing cookies
		String objectId = StringUtil.checkVal(req.getSession().getAttribute(Storage.CART_OBJ));
		
		//if length=0, check for objectId stored in a cookie
		if (objectId.length() == 0) { 
			Cookie[] cookies = req.getCookies();
			if (cookies == null) cookies = new Cookie[0];
			
    		for (int x=0; x < cookies.length; x++) {
    			try {
    				if (StringUtil.checkVal(cookies[x].getName()).equals(Storage.CART_OBJ)) {
	    				objectId = cookies[x].getValue();
	    				log.debug("found cookie " + Storage.CART_OBJ + "=" + objectId);
	    				break;
	    			}
    			} catch (NullPointerException npe) {
    				log.error("cookies[" + x + "]=" + objectId, npe);
    			}
    		}
		} 
		/*
		 * If we get the string value, remove it from the session.
		 */
		else {
			req.getSession().setAttribute(Storage.CART_OBJ, "");
		}
		return objectId;
	}
	
	/**
	 * this method is likely rarely/never called, but is a necessary stop-gap incase the
	 * user does not have this information on their session.
	 * 
	 * update - Made this public so we could call it from the KeystoneFacadeAction.
	 * 			BL 12/6/13
	 * @param sessVo
	 */
	public FastsignsSessVO loadFranchiseVO(FastsignsSessVO sessVo, String webId) throws InvalidDataException {
		log.info("loading franchise account for webId=" + webId);
		KeystoneProfileManager pm = new KeystoneProfileManager();
		KeystoneProxy proxy = KeystoneProxy.newInstance(attributes);
		proxy.setModule("franchises");
		proxy.setAction("getFranchiseByWebNumber");
		proxy.addPostData("webNumber", webId);
		proxy.setParserType(KeystoneDataParser.DataParserType.DoNothing);
		
		try {
			byte[] data = (byte[]) proxy.getData().getActionData();
		
			JSONObject franObj = JSONObject.fromObject(new String(data));
			sessVo.addFranchise(pm.loadFranchiseFromJSON(franObj));
			
		} catch (Exception e) {
			log.error("could not load franchise data", e);
		}
		pm = null;
		
		return sessVo;
	}
	
	/**
	 * allows the user to create a new account at checkout.
	 * Leverages the same underlying code as MyProfileAction, via KeystoneProfileManager
	 * @param req
	 * @throws ActionException
	 */
	protected void processCreateAccount(SMTServletRequest req, ShoppingCartVO cart) throws ActionException {
		MyProfileAction mpa = new MyProfileAction();
		KeystoneProfileManager pm = new KeystoneProfileManager(attributes);
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		if (sessVo == null) sessVo = new FastsignsSessVO();
		KeystoneUserDataVO user = new KeystoneUserDataVO();
		user.setData(req);
		user.setUserId(mpa.ensureId(null)); //attaching invalid GUID ensures a new account will get created at Keystone

		//if the user is logged in we need to pass their user_login_id.
		//This scenario creates a Franchise account and attaches it to the login_account.
		if (sessVo.getProfiles().size() > 0) {
			for (String profileId : sessVo.getProfiles().keySet()) {
				user.setAuthenticationId(sessVo.getProfile(profileId).getAuthenticationId());
				break;
			}
		}
		
		//we need to tie a FranchiseId to this user.  We can do so by grabbing this value from the first product in the cart.
		//orders can only be placed to one franchise at a time, so we know all products will have the same franchise_id value (set).
		for (ShoppingCartItemVO vo: cart.getItems().values()) {
			KeystoneProductVO prod = (KeystoneProductVO) vo.getProduct();
			user.addAttribute("franchise_id", prod.getFranchise_id());
			log.debug("tied user to franchise_id: " + prod.getFranchise_id());
			break;
		}
		
		user = mpa.loadPhoneNumbers(user, req);
		user = mpa.loadAltEmails(user, req);
		log.debug("created userVO: " + user);
		try {
			user = pm.submitProfileToKeystone(user, req);
			//req.getSession().setAttribute(Constants.USER_DATA, user);
			
			//log this user in, now that they have a valid account,
			//this is done by encrypting their profileId and mimicking a "remember me"
			//login, because it's easier to do a 'headless' login and one using email & password, 
			//which is form-based.
			try {
				//encrypt the profileId (necessary for WC compatability!)
				String encProfileId = user.getProfileId();
				try {
					StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
					encProfileId = se.encrypt(encProfileId);
					encProfileId = URLEncoder.encode(encProfileId, "UTF-8");
				} catch (Exception e) {
					log.warn("could not encrypt profileId", e);
				}
				log.debug("encProfileId=" + encProfileId + " decProfileId=" + user.getProfileId());
				//req.setParameter("type", "ecomm");
				//SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
				attributes.put(AbstractRoleModule.HTTP_REQUEST, req);
				//SecurityController sc = new SecurityController(site.getLoginModule(), site.getRoleModule(), attributes);
				//sc.loadUserFromCookie(encProfileId, (String)attributes.get(Constants.ENCRYPT_KEY), (Connection)attributes.get(GlobalConfig.KEY_DB_CONN), req, site.getSiteId());
				FsKeystoneLoginModule klm = new FsKeystoneLoginModule(attributes);
				user = (KeystoneUserDataVO) klm.retrieveUserData(encProfileId);
				req.getSession().setAttribute(Constants.USER_DATA, user);
				
			} catch (Exception e) {
				log.error("unable to log-in user transparently", e);
				//just ignore these, there's nothing we can do (now) and don't want 
				//to confuse the user by throwing an exception
			}

			//now we need to add the users 'new' profile to our eComm session object, for Checkout to leverage.
			user.setWebId((String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID));
			sessVo.addProfile(user);
			req.getSession().setAttribute(KeystoneProxy.FRAN_SESS_VO, sessVo);
			log.debug("matched user to franchise=" + sessVo.getFranchise(user.getWebId()));
			
		} catch (Exception e) {
			throw new ActionException(e.getMessage(), e); //the error message will contain something 'friendly' sent from Keystone
		}
		
		attributes.put("nextStep", "checkout");
		return;
	}
}
