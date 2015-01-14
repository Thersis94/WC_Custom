package com.universal.signals.action;

// Java 7
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>PayPalCheckoutManager.java <p/>
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
	private ProcessingServiceType serviceType = ProcessingServiceType.PAY_PAL;
	private TransactionType transactionType;
	private ShoppingCartVO cart;
	private Connection dbConn;
	private String encryptionKey;
	private String catalogSiteId;
	private Map<String,Object> attributes;
	
	/**
	 * @param req
	 * @param cart
	 * @throws InvalidDataException
	 */
	public PayPalCheckoutManager(SMTServletRequest req, ShoppingCartVO cart) 
			throws InvalidDataException {
		this.cart = cart;
		this.attributes = new HashMap<>();
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
		
		// init critical values for later use.
		encryptionKey = (String)attributes.get(Constants.ENCRYPT_KEY);

		// process the checkout request
		PaymentTransactionResponseVO pRes = processCheckoutRequest();
		
		// reset cart errors
		cart.flushErrors();
		
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
		checkCommonErrors(rMap);
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
		checkCommonErrors(rMap);
		checkSpecificErrors(rMap);
		if (cart.hasErrors()) return;
		
		// make sure billing info is null so views behave properly.
		if (cart.getBillingInfo() == null) {
			UserDataVO buyer = new UserDataVO();
			buyer.setEmailAddress(rMap.get("EMAIL"));
			buyer.setFirstName(rMap.get("FIRSTNAME"));
			buyer.setMiddleName(rMap.get("MIDDLENAME"));
			buyer.setLastName(rMap.get("LASTNAME"));
			buyer.setSuffixName(rMap.get("SUFFIX"));
			buyer.addPhone(new PhoneVO(rMap.get("PHONENUM")));
			buyer.setCountryCode(rMap.get("COUNTRYCODE"));
			cart.setBillingInfo(buyer);
		}
		cart.getBillingInfo().addAttribute("PAYER_ID", rMap.get("PAYERID"));
		cart.getBillingInfo().addAttribute("PAYER_STATUS", rMap.get("PAYERSTATUS"));
		cart.getBillingInfo().addAttribute("ADDRESS_STATUS", 
				StringUtil.checkVal(rMap.get("PAYMENTREQUEST_0_ADDRESSSTATUS")));
		
		// set 'ship to' info using the data returned from the 'get'
		UserDataVO newShipTo = new UserDataVO();
		newShipTo.setCountryCode(rMap.get("COUNTRYCODE"));
		newShipTo.setName(rMap.get("PAYMENTREQUEST_0_SHIPTONAME"));
		newShipTo.setEmailAddress(rMap.get("EMAIL"));
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
		checkCommonErrors(rMap);
		checkSpecificErrors(rMap);
		if (cart.hasErrors()) return;

		// add additional field vals needed to send to USA's webservice.
		UserDataVO buyer = cart.getBillingInfo();
		buyer.addAttribute("TOKEN", rMap.get("TOKEN"));
		buyer.addAttribute("CORRELATION_ID", rMap.get("CORRELATIONID"));
		buyer.addAttribute("TRANSACTION_ID", rMap.get("PAYMENTINFO_0_TRANSACTIONID"));
		buyer.addAttribute("PAYMENT_STATUS",  rMap.get("PAYMENTINFO_0_PAYMENTSTATUS"));
		buyer.addAttribute("PENDING_REASON",  rMap.get("PAYMENTINFO_0_PENDINGREASON"));		

	}
	
	/**
	 * Check for errors returned in the response and add to cart errors map.
	 * @param rMap
	 */
	private void checkCommonErrors(Map<String,String> rMap) {
		// if payment errors, capture and set on cart
		String errInfo = StringUtil.checkVal(rMap.get("ACK"),null);
		if (errInfo != null && ! errInfo.equalsIgnoreCase("success")) {
			// payment errors found.
			cart.addError("PAYPAL_ERROR_CODE", rMap.get("L_ERRORCODE0"));
			cart.addError("PAYPAL_ERROR_CODE_SEVERITY", rMap.get("L_SEVERITYCODE0"));
			cart.addError("PAYPAL_ERROR_TXT_SHORT", rMap.get("L_SHORTMESSAGE0"));
			cart.addError("PAYPAL_ERROR_TXT_LONG", rMap.get("L_LONGMESSAGE0"));
		}
	}
	
	/**
	 * Check for errors returned by the 'do express checkout' operation; add to cart
	 * errors map.
	 * @param rMap
	 */
	private void checkSpecificErrors(Map<String,String> rMap) {
		// if payment errors, capture and set on cart
		String errInfo = StringUtil.checkVal(rMap.get("PAYMENTINFO_0_ACK"),null);
		if (errInfo != null && ! errInfo.equalsIgnoreCase("success")) {
			// payment errors found.
			cart.addError("PAYPAL_ERROR_CODE_PAYMENT", rMap.get("PAYMENTINFO_0_ERRORCODE"));
			cart.addError("PAYPAL_ERROR_CODE_SEVERITY_PAYMENT", rMap.get("PAYMENTINFO_0_SEVERITYCODE"));
			cart.addError("PAYPAL_ERROR_TXT_SHORT_PAYMENT", rMap.get("PAYMENTINFO_0_SHORTMESSAGE"));
			cart.addError("PAYPAL_ERROR_TXT_LONG_PAYMENT", rMap.get("PAYMENTINFO_0_LONGMESSAGE"));
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
		log.debug("processCheckoutRequest...");
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
		log.debug("callSMTProxy...");
		// JSONify the request and build the request post data
		Gson g = new Gson();
		byte[] json = g.toJson(pReq).getBytes();
		String postStub = "type=json&xmlData=";
		
		// format/URL encode the xmlData value.
		String postData =  new String(json);
		log.debug("raw postData: " + postData);
		
		try {
			postData = URLEncoder.encode(postData,"utf-8");
		} catch (UnsupportedEncodingException uee) {
			log.error("Error URL encoding postData for proxy call, ", uee);
		}
		
		log.debug("URL-encoded postData: " + postData);
		
		// build proxy URL and call proxy
		StringBuilder smtProxyUrl = new StringBuilder((String)attributes.get(Constants.CFG_SMT_PROXY_URL));
		smtProxyUrl.append("/payment/process");
		SMTHttpConnectionManager mgr = new SMTHttpConnectionManager();
		byte[] bytes = mgr.retrieveDataViaPost(smtProxyUrl.toString(), postStub + postData);
		log.info("raw SMT proxy response: " + new String(bytes));
		
		// JSONify the response
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
		log.debug("build basic request...");
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
		log.debug("retrieveMerchantInfo...");
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

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
