package com.universal.signals.action;

// Java 7
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

// Apache Log4j
import org.apache.log4j.Logger;

// Google Libs
import com.google.gson.Gson;

// SMTBaseLibs 2.0
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.payment.MerchantInfoVO;
import com.siliconmtn.commerce.payment.PaymentTransactionRequestVO;
import com.siliconmtn.commerce.payment.PaymentTransactionRequestVO.ProcessingServiceType;
import com.siliconmtn.commerce.payment.PaymentTransactionRequestVO.TransactionEnvironment;
import com.siliconmtn.commerce.payment.PaymentTransactionRequestVO.TransactionType;
import com.siliconmtn.commerce.payment.PaymentTransactionResponseVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title: </b>PayPalProxyManager.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Nov 24, 2014<p/>
 *<b>Changes: </b>
 * Nov 24, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class PayPalCheckoutManager {

	protected static final transient Logger log = Logger.getLogger(PayPalCheckoutManager.class);
	//String smtProxyUrl = "http://proxy.starcastle.siliconmtn.com:9000/websvc/payment/process";
	//final String smtProxyUrl = "http://proxy.starcastle.siliconmtn.com:9000/websvc/payment/process";
	final String smtProxyUrl = "http://10.0.80.5:9000/websvc/payment/process";
	private ProcessingServiceType serviceType = ProcessingServiceType.PAY_PAL;
	TransactionType transactionType;
	SMTServletRequest req;
	ShoppingCartVO cart;
	Connection dbConn;
	String encryptionKey;
	String catalogSiteId;
	
	/**
	 * @param req
	 * @param cart
	 * @throws InvalidDataException
	 */
	public PayPalCheckoutManager(SMTServletRequest req, ShoppingCartVO cart, 
			String encryptionKey) throws InvalidDataException {
		this.req = req;
		this.cart = cart;
		this.encryptionKey = encryptionKey;
		init(req.getParameter("paypal"), cart);
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws EncryptionException 
	 */
	protected void processTransaction() 	throws EncryptionException, 
		IOException, SQLException {
		log.debug("processTransaction...");
		
		// process the checkout request
		PaymentTransactionResponseVO pRes = processCheckoutRequest();
		
		// parse the response.
		switch (transactionType) {
			case EXPRESS_CHECKOUT_SET:
				parseSetResponse(pRes);
				break;
			case EXPRESS_CHECKOUT_GET:
				parseGetResponse(pRes);
				break;
			case EXPRESS_CHECKOUT_DO:
				parseDoResponse(pRes);
				break;
			default: break;
		}
	}
	
	/**
	 * 
	 * @param pRes
	 */
	@SuppressWarnings("unchecked")
	private void parseSetResponse(PaymentTransactionResponseVO pRes) {
		log.debug("parseSetResponse...");
		
		if (pRes.getAttributes() == null) return;
		Map<String, String> rMap = (Map<String, String>)pRes.getAttributes().get("responseMap");
		
		if (log.isDebugEnabled()) debugResponseMap(rMap,"set");
		
		// check for any errors.
		checkErrors(rMap);
		if (cart.hasErrors()) return;
		
		// set the transactional TOKEN value on cart as invoice number.
		cart.setInvoiceNo(pRes.getInvoiceNo());
		cart.setPurchaseOrderNo(pRes.getTransactionId()); // CORRELATIONID
		log.debug("token from parseSetResponse: " + cart.getInvoiceNo());
		// set the paypal checkout redirect url
		cart.setCartCheckoutUrl(pRes.getVerificationCode());
		log.debug("cartCheckoutUrl from parseSetResponse: " + cart.getCartCheckoutUrl());
	}
	
	/**
	 * Parses 'get' checkout action's response.  Updates cart info.
	 * @param pRes
	 */
	@SuppressWarnings("unchecked")
	private void parseGetResponse(PaymentTransactionResponseVO pRes) {
		log.debug("parseGetResponse...");
		
		if (pRes.getAttributes() == null) return;
		Map<String, String> rMap = (Map<String, String>)pRes.getAttributes().get("responseMap");
		
		if (log.isDebugEnabled()) debugResponseMap(rMap,"get");
		
		// check for any errors.
		checkErrors(rMap);
		if (cart.hasErrors()) return;
		
		// set buyer data using the billing info user object
		if (cart.getBillingInfo() == null) {
			UserDataVO buyer = new UserDataVO();
			buyer.setFirstName(rMap.get("FIRSTNAME"));
			buyer.setMiddleName(rMap.get("MIDDLENAME"));
			buyer.setLastName(rMap.get("LASTNAME"));
			buyer.setSuffixName(rMap.get("SUFFIX"));
			buyer.setEmailAddress(rMap.get("EMAIL"));
			cart.setBillingInfo(buyer);
		}
		
		// add certain data so we have it for future use
		cart.getBillingInfo().addAttribute("TOKEN", rMap.get("TOKEN"));
		cart.getBillingInfo().addAttribute("CORRELATION_ID", rMap.get("CORRELATIONID"));
		cart.getBillingInfo().addAttribute("PAYER_ID", rMap.get("PAYER_ID"));
		cart.getBillingInfo().setCountryCode(rMap.get("COUNTRYCODE"));
		
		// update ship to info using the shipping info user object
		// update the cart's 'ship To' info.
		if (cart.getShippingInfo() == null) {
			UserDataVO newShipTo = new UserDataVO();
			newShipTo.setName(rMap.get("PAYMENTREQUEST_0_SHIPTONAME"));
			newShipTo.setAddress(rMap.get("PAYMENTREQUEST_0_SHIPTOSTREET"));
			newShipTo.setAddress2(rMap.get("PAYMENTREQUEST_0_SHIPTOSTREET2"));
			newShipTo.setCity(rMap.get("PAYMENTREQUEST_0_SHIPTOCITY"));
			newShipTo.setState(rMap.get("PAYMENTREQUEST_0_SHIPTOSTATE"));
			newShipTo.setZipCode(rMap.get("PAYMENTREQUEST_0_SHIPTOZIP"));
			newShipTo.setCountryCode(rMap.get("PAYMENTREQUEST_0_SHIPTOCOUNTRYCODE"));
			if (StringUtil.checkVal(rMap.get("PAYMENTREQUEST_0_SHIPTOPHONENUM"),null) != null) {
				newShipTo.addPhone(new PhoneVO(rMap.get("PAYMENTREQUEST_0_SHIPTOPHONENUM")));
			}
			cart.setShippingInfo(newShipTo);
		}
		cart.getShippingInfo().addAttribute("ADDRESS_STATUS", 
				StringUtil.checkVal(rMap.get("PAYMENT_REQUEST_SHIPTOADDRESSSTATUS")));
		
	}
	
	/**
	 * Parses 'do' checkout action's response.  Updates cart info.
	 * @param pRes
	 */
	@SuppressWarnings("unchecked")
	private void parseDoResponse(PaymentTransactionResponseVO pRes) {
		log.debug("parseDoResponse...");
		
		if (pRes.getAttributes() == null) return;
		Map<String, String> rMap = (Map<String, String>)pRes.getAttributes().get("responseMap");
		
		if (log.isDebugEnabled()) debugResponseMap(rMap,"do");
		
		// check for any errors.
		checkErrors(rMap);
		if (cart.hasErrors()) return;
		
		// add additional field vals needed to send to USA's webservice.
		UserDataVO buyer = cart.getBillingInfo();
		buyer.addAttribute("TRANSACTION_ID", rMap.get("PAYMENTREQUEST_0_TRANSACTIONID"));
		buyer.addAttribute("PAYMENT_STATUS",  rMap.get("PAYMENTINFO_0_PAYMENTSTATUS"));
		buyer.addAttribute("PENDING_REASON",  rMap.get("PAYMENTINFO_0_PENDINGREASON"));
		
		// call final checkout on cart to process via Universal's webservice.
	}
	
	/**
	 * Check for errors returned in the response and add to cart errors map.
	 * @param rMap
	 */
	private void checkErrors(Map<String,String> rMap) {
		// if payment errors, capture and set on cart
		String errInfo = StringUtil.checkVal(rMap.get("PAYMENTINFO_0_ACK"),null);
		if (errInfo != null) {
			// payment errors found.
			cart.addError("PAYPAL_ERROR_CODE", rMap.get("PAYMENTINFO_0_ERRORCODE"));
			cart.addError("PAYPAL_ERROR_CODE_SEVERITY", rMap.get("PAYMENTINFO_0_SEVERITYCODE"));
			cart.addError("PAYPAL_ERROR_TXT_SHORT", rMap.get("PAYMENTINFO_0_SHORTMESSAGE"));
			cart.addError("PAYPAL_ERROR_TXT_LONG", rMap.get("PAYMENTINFO_0_LONGMESSAGE"));
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws EncryptionException 
	 */
	private PaymentTransactionResponseVO processCheckoutRequest() 
			throws IOException, SQLException, EncryptionException {
		log.debug("processCheckout...");
		PaymentTransactionRequestVO pReq = buildBasicRequest(cart);
		pReq.setTransactionType(transactionType);
		return callSMTProxy(pReq);
	}
	
	/**
	 * 
	 * @param pReq
	 * @return
	 * @throws IOException 
	 */
	private PaymentTransactionResponseVO callSMTProxy(PaymentTransactionRequestVO pReq) 
			throws IOException {
		
		Gson g = new Gson();
		byte[] json = g.toJson(pReq).getBytes();
		log.debug("raw payment request obj json: " + new String(json));
		
		String postData = "type=json&xmlData=" + new String(json);
		log.debug("postData: " + postData);
		SMTHttpConnectionManager mgr = new SMTHttpConnectionManager();
		byte[] bytes = mgr.retrieveDataViaPost(smtProxyUrl, postData);
		log.info("raw SMT proxy response: " + new String(bytes));
		PaymentTransactionResponseVO pRes = null;
		Gson gson = new Gson();
		pRes = gson.fromJson(new String(bytes), PaymentTransactionResponseVO.class);
		return pRes;

	}
	
	/**
	 * Builds the basic payment transaction request object used for all requests
	 * @param cart
	 * @return
	 * @throws SQLException 
	 * @throws EncryptionException 
	 */
	private PaymentTransactionRequestVO buildBasicRequest(ShoppingCartVO cart) 
			throws SQLException, EncryptionException {
		PaymentTransactionRequestVO pReq = new PaymentTransactionRequestVO();
		pReq.setProcessingServiceType(ProcessingServiceType.PAY_PAL);
		pReq.setMerchantInfo(retrieveMerchantInfo());
		pReq.setTransactionEnvironment(parseTransactionEnvironment(pReq.getMerchantInfo()));
		pReq.setPayment(buildPayment());
		pReq.setPaymentAmount(Double.toString(cart.getCartTotal()));
		pReq.setShoppingCart(cart);
		return pReq;
	}
	
	/**
	 * Retrieves and decrypts the merchant credentials from the custom db table.
	 * @return
	 * @throws SQLException 
	 * @throws EncryptionException 
	 */
	private MerchantInfoVO retrieveMerchantInfo() throws IllegalArgumentException,
		EncryptionException, SQLException {
		StringEncrypter se = new StringEncrypter(encryptionKey);
		StringBuilder sb = new StringBuilder();
		sb.append("select ENC_MERCHANT_USER_NM, ENC_MERCHANT_PASSWORD_TXT, ");
		sb.append("ENC_MERCHANT_SIGNATURE_TXT, ENVIRONMENT_TXT from MERCHANT ");
		sb.append("where SITE_ID = ? and SERVICE_PROVIDER_TYPE = ?");
		log.debug("MERCHANT sql: " + sb.toString() + "|" + catalogSiteId + "|" + serviceType.name());
		MerchantInfoVO m = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, catalogSiteId);
			ps.setString(2, serviceType.name());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				// retrieve from db and decrypt.
				m = new MerchantInfoVO();
				m.setLoginId(se.decrypt(rs.getString("ENC_MERCHANT_USER_NM")));
				m.setMerchantPassword(se.decrypt(rs.getString("ENC_MERCHANT_PASSWORD_TXT")));
				m.setTransactionKeyId(se.decrypt(rs.getString("ENC_MERCHANT_SIGNATURE_TXT")));
				m.setEnvironmentKey(rs.getString("ENVIRONMENT_TXT"));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving merchant credentials for PayPal for site, ", sqle);
			throw new SQLException(sqle.getMessage());
		}
		return m;
	}
	
	/**
	 * Parses the environment key set into a valid TransactionEnvironment enum.
	 * @param envKey
	 * @return
	 */
	private TransactionEnvironment parseTransactionEnvironment(MerchantInfoVO merchant) {
		String envKey = StringUtil.checkVal(merchant.getEnvironmentKey()).toLowerCase();
		if (envKey.startsWith("production")) {
			return TransactionEnvironment.PRODUCTION;
		} else {
			return TransactionEnvironment.SANDBOX;
		}
		
	}
		
	/**
	 * Initializes the action
	 * @param transaction
	 * @param cart
	 * @throws InvalidDataException
	 */
	private void init (String transaction, ShoppingCartVO cart) 
			throws InvalidDataException {
		
		if (cart == null) throw new InvalidDataException("Shopping cart is invalid.");
		
		if (transaction.equalsIgnoreCase("set")) {
			transactionType = TransactionType.EXPRESS_CHECKOUT_SET;
		} else if (transaction.equalsIgnoreCase("get")) {
			transactionType = TransactionType.EXPRESS_CHECKOUT_GET;
		} else if (transaction.equalsIgnoreCase("do")) {
			transactionType = TransactionType.EXPRESS_CHECKOUT_DO;
		} else {
			throw new InvalidDataException("Transaction type is invalid.");
		}
		
	}
	
	/**
	 * Helper method for debugging
	 * @param rMap
	 */
	private void debugResponseMap(Map<String,String> rMap, String method) {
		log.debug("dumping '" + method + "' response map: ");
		for (String key : rMap.keySet()) {
			log.debug(key + " | " + rMap.get(key));
		}
	}
	
	/**
	 * Builds a empty payment object required by the payment proxy
	 * @param cart
	 * @return
	 */
	private PaymentVO buildPayment() {
		PaymentVO payment = new PaymentVO(encryptionKey);
		return payment;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @param catalogSiteId the catalogSiteId to set
	 */
	public void setCatalogSiteId(String catalogSiteId) {
		this.catalogSiteId = catalogSiteId;
	}

}
