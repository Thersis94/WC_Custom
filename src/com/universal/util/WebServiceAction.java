package com.universal.util;

// Java 7
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


// DOM4j
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;


// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.XMLUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: WebServiceAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 30, 2011<p/>
 * <b>Changes: </b>
 * <b>Aug 24, 2012; David Bargerhuff: Refactored action to implement consistent XML request structure
 * and converted direct web service methods to return an object of type org.dom4j.Element.
 * Aug 29, 2014: DBargerhuff: Added support for sending billing comments with order.
 ****************************************************************************/
public class WebServiceAction extends SBActionAdapter {
	/**
	 * URL of the Web Service APIs
	 */
	public static final String USA_BASE_URL = "usaBaseUrl";
	public static final String CATALOG_SITE_ID = "catalogSiteId";
	
	/**
	 * Header value for the XML request
	 */
	public static final String BASE_XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";
	
	/**
	 * Get's the shipping rates for the provided products
	 */
	public static final int SHIPPING_SERVICE = 1;
	
	/**
	 * Code returned from the user object for the billing address type
	 */
	public static final String BILLING_USER_TYPE = "billing";
	
	/**
	 * Code returned from the user object for the shipping address type
	 */
	public static final String SHIPPING_USER_TYPE = "shipping";

	/**
	 * @param actionInit
	 */
	public WebServiceAction(ActionInitVO actionInit) {
		super(actionInit);
		attributes.put(USA_BASE_URL, "www.signals.com");
	}

	public static void main(String[] args) throws Exception {
		org.apache.log4j.BasicConfigurator.configure();
		//WebServiceAction wsa = new WebServiceAction(null);
		Map<String, Integer> prods = new HashMap<>();
		prods.put("80325", 3);
		//wsa.retrieveShippingInfo("80401", prods);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {}
	
	/**
	 * 
	 * @param shippingInfo
	 * @param prods
	 * @param promoCode
	 * @return
	 * @throws DocumentException
	 */
	public Element retrieveTaxInfo(UserDataVO shippingInfo, Collection<ShoppingCartItemVO> prods, String promoCode) 
	throws DocumentException {
		if (shippingInfo == null || prods == null || prods.size() == 0) return new DefaultElement("Tax");
		String url = this.retrieveServiceURL(StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)), "tax", false);
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<TaxRequest>");
		s.append("<State>").append(shippingInfo.getState()).append("</State>");
		s.append("<Zip>").append(shippingInfo.getZipCode()).append("</Zip>");
		this.addProductXMLByCollection(s, prods);		
		s.append("<PromotionCode>").append(promoCode).append("</PromotionCode>");
		s.append("</TaxRequest>");
		log.debug("Calling tax service: " + s);
		return this.callWebService(url, s, "Tax");
	}
	
	/**
	 * 
	 * @param prods
	 * @param pc
	 * @return
	 * @throws DocumentException
	 */
	public Element retrievePromotionDiscount(Collection<ShoppingCartItemVO> prods, String pc) 
	throws DocumentException {
		if (prods == null || prods.size() == 0) return new DefaultElement("PromotionCodeResponse");
		String url = this.retrieveServiceURL(StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)), "promocode", true);
		//String url = this.retrieveServiceURL(StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)), "promocode", false);
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<PromotionCodeRequest>");
		s.append("<Code>").append(pc).append("</Code>");
		this.addProductXMLByCollection(s, prods);
		s.append("</PromotionCodeRequest>");
		// Response element is 'PromotionCodeResponse' but is the root element of the reponse
		// hence we use 'root' as the element name requested from the webservice.
		return this.callWebService(url, s, "root");
	}
	
	/**
	 * 
	 * @param email
	 * @param pwd
	 * @param catalogSiteId
	 * @return
	 * @throws DocumentException
	 * @throws AuthenticationException
	 */
	public Element authenticateMember(String email, String pwd, String catalogSiteId) 
	throws DocumentException, AuthenticationException {
		String url = this.retrieveServiceURL(StringUtil.checkVal(catalogSiteId), "login", true);
		//String url = this.retrieveServiceURL(StringUtil.checkVal(catalogSiteId), "login", false);
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<MemberRequest>");
		s.append("<Email>").append(email).append("</Email>");
		s.append("<Password>").append(pwd).append("</Password>");
		s.append("</MemberRequest>");
		log.debug("sending login request");
		return this.callWebService(url, s, "root");
	}

	/**
	 * 
	 * @param zip
	 * @param prods
	 * @return
	 * @throws DocumentException
	 */
	public Element retrieveShippingInfo(String zip, Map<String, ShoppingCartItemVO> prods)
			throws DocumentException {
		// Build the URL
		String url = this.retrieveServiceURL(StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)), "shipping", false);
		log.debug("shipping info retrieval URL: " + url);
		// Build the XML Request
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<ShippingRequest>");
		s.append("<Zip>").append(zip).append("</Zip>");
		// add product XML
		this.addProductXMLByMap(s, prods);
		s.append("</ShippingRequest>");
		log.debug("shipping retrieval XML Req: " + s);
		return this.callWebService(url, s, "ShippingCost");
	}
	
	/**
	 * Returns the XML Element containing the response to a request
	 * for product availability.
	 * @param product
	 * @return
	 * @throws DocumentException
	 */
	public Element checkProductAvailability(ProductVO product) 
			throws DocumentException {
		String url = this.retrieveServiceURL(StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)), "stock", false);
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<StockRequest>");
		s.append("<Products>");
		this.addProductXML(s, product, 1, false);
		s.append("</Products>");
		s.append("</StockRequest>");
		log.debug("availability request XML: " + s);
		return this.callWebService(url, s, "Products");
	}
	
	/**
	 * 
	 * @param req
	 * @param cart
	 * @param ipAddr
	 * @return
	 * @throws DocumentException
	 */
	public Element placeOrder(SMTServletRequest req, ShoppingCartVO cart, String ipAddr) 
			throws DocumentException {
		// Build the URL
		String url = this.retrieveServiceURL(StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)), "checkout", true);
		// Get the request XML
		StringBuilder s = null;
		try {
			s = this.createOrderRequest(req, cart, ipAddr);
		} catch (EncryptionException ee) {
			Element errElem = new DefaultElement("Error");
			errElem.addElement("ErrorCode").setText("ERROR_ENCRYPTION");
			errElem.addElement("ErrorMessage").setText("Unable to process the order at this time.");
			return errElem;
		} catch (IllegalArgumentException iae) {
			Element errElem = new DefaultElement("Error");
			errElem.addElement("ErrorCode").setText("ERROR_ILLEGAL_ARGUMENT");
			errElem.addElement("ErrorMessage").setText("Unable to process the order at this time.");
			return errElem;
		}

		// log the order request
		logOrderTransaction(req, cart, s, true);
		
		// place the order.
		Element orderResponse = this.callWebService(url, s, "root");
		//Element orderResponse = this.createDebugResponseElement(cart);
		
		// log the order response
		logOrderTransaction(req, cart, new StringBuilder(orderResponse.asXML()), false);
		
		return orderResponse;

	}
	
	/**
	 * Helper method for parsing a user data XML element into a UserDataVO.
	 * @param root
	 * @return
	 * @throws DocumentException
	 * @throws AuthenticationException
	 */
	@SuppressWarnings("unchecked")
	public UserDataVO parseUserData(Element root) 
			throws DocumentException, AuthenticationException {
		//Map<String, UserDataVO> locs = new HashMap<String, UserDataVO>();
		// Parse out the main info and member id
		String memId = XMLUtil.checkVal(root.element("MemberID"));
		// Make sure the member id is present.  If not, throw and exception
		log.debug("Mem ID Length: " + memId.length());
		if (memId.length() == 0) throw new AuthenticationException("Not authorized");
		// Parse out the addresses
		Element addrContainer = root.element("Addresses");
		List<Element> addresses = (List<Element>)addrContainer.elements("Address");
		UserDataVO completeUser = new UserDataVO();
		// Loop the addresses provided
		for (int i=0; i < addresses.size(); i++) {
			Element address = addresses.get(i);
			UserDataVO user = new UserDataVO();
			user.setEmailAddress(address.element("Email").getTextTrim());
			user.setFirstName(address.element("FirstName").getTextTrim());
			user.setLastName(StringUtil.checkVal(address.element("LastName").getTextTrim()));
			user.setAddress(address.element("Street1").getTextTrim());
			user.setAddress2(address.element("Street2").getTextTrim());
			user.setCity(address.element("City").getTextTrim());
			user.setState(address.element("State").getTextTrim());
			user.setZipCode(address.element("Zip").getTextTrim());
			user.setMainPhone(address.element("DayPhone").getTextTrim());
			user.addPhone(new PhoneVO(address.element("EveningPhone").getTextTrim(), PhoneVO.EVENING_PHONE, "US"));
			user.setProfileId(memId);
			user.setAuthenticationId(memId);
			user.setAuthenticated(true);
			// Set billing info as main user data and extended info as shipping info (if it exists)
			String type = StringUtil.checkVal(address.attributeValue("type"));
			if (WebServiceAction.BILLING_USER_TYPE.equalsIgnoreCase(type)) {
				completeUser = user;
				if (completeUser.getUserExtendedInfo() == null) {
					/* 2015-01-14 DBargerhuff Refactoring this to place a clone 
					 * (different instance) of the user object on the completeUser 
					 * object's userExtendedInfo field.  Otherwise, downstream 
					 * JSONifying of this object will fail due to a stack overflow error.
					 */
					UserDataVO extUser = new UserDataVO();
					extUser.setEmailAddress(address.element("Email").getTextTrim());
					extUser.setFirstName(address.element("FirstName").getTextTrim());
					extUser.setLastName(StringUtil.checkVal(address.element("LastName").getTextTrim()));
					extUser.setAddress(address.element("Street1").getTextTrim());
					extUser.setAddress2(address.element("Street2").getTextTrim());
					extUser.setCity(address.element("City").getTextTrim());
					extUser.setState(address.element("State").getTextTrim());
					extUser.setZipCode(address.element("Zip").getTextTrim());
					extUser.setMainPhone(address.element("DayPhone").getTextTrim());
					extUser.addPhone(new PhoneVO(address.element("EveningPhone").getTextTrim(), PhoneVO.EVENING_PHONE, "US"));
					extUser.setProfileId(memId);
					completeUser.setUserExtendedInfo(extUser);
				}
					
			} else if (WebServiceAction.SHIPPING_USER_TYPE.equalsIgnoreCase(type)) {
				if (user.getLastName().length() > 0)
					completeUser.setUserExtendedInfo(user);
			}

		}
		return completeUser;
	}
	
	/**
	 * Builds the XML request from a ShoppingCartVO
	 * @param cart
	 * @param ipAddr
	 * @return
	 * @throws IllegalArgumentException 
	 * @throws EncryptionException 
	 */
	private StringBuilder createOrderRequest(SMTServletRequest req, 
			ShoppingCartVO cart, String ipAddr) throws EncryptionException, 
			IllegalArgumentException {
		// Build the XML Request
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<OrderRequest>");
		s.append("<Addresses>");
		s.append("<Address type=\"billing\">");
		s.append("<Email>").append(cart.getBillingInfo().getEmailAddress()).append("</Email>");
		s.append("<FirstName>").append(cart.getBillingInfo().getFirstName()).append("</FirstName>");
		s.append("<LastName>").append(cart.getBillingInfo().getLastName()).append("</LastName>");
		s.append("<Street1>").append(cart.getBillingInfo().getAddress()).append("</Street1>");
		s.append("<Street2>").append(cart.getBillingInfo().getAddress2()).append("</Street2>");
		s.append("<City>").append(cart.getBillingInfo().getCity()).append("</City>");
		s.append("<State>").append(cart.getBillingInfo().getState()).append("</State>");
		s.append("<Zip>").append(cart.getBillingInfo().getZipCode()).append("</Zip>");
		s.append("<DayPhone>").append(cart.getBillingInfo().getMainPhone()).append("</DayPhone>");
		String eveningPhone = null;
		for(PhoneVO p : cart.getBillingInfo().getPhoneNumbers())
			if(StringUtil.checkVal(p.getPhoneType()).equals(PhoneVO.EVENING_PHONE))
				eveningPhone = p.getPhoneNumber();		
		if (StringUtil.checkVal(eveningPhone).length() > 0) {
			s.append("<EveningPhone>").append(eveningPhone).append("</EveningPhone>");
		}
		/* Mantis #9173 DBargerhuff TODO Waiting for USA to provide specific XML 
		 * tag structure to use.*
		 *
		if (cart.getBillingInfo().getAttributes() != null) {
			Object o = cart.getBillingInfo().getAttributes().get(ShoppingCartAction.BILLING_COMMENTS);
			if (o != null) {
				s.append("<Comments>");
				s.append((String)o);
				s.append("</Comments>");				
			}
		}
		*/
		s.append("</Address>");
		s.append("<Address type=\"shipping\">");
		s.append("<Email>").append(cart.getShippingInfo().getEmailAddress()).append("</Email>");
		s.append("<FirstName>").append(cart.getShippingInfo().getFirstName()).append("</FirstName>");
		s.append("<LastName>").append(cart.getShippingInfo().getLastName()).append("</LastName>");
		s.append("<Street1>").append(cart.getShippingInfo().getAddress()).append("</Street1>");
		s.append("<Street2>").append(cart.getShippingInfo().getAddress2()).append("</Street2>");
		s.append("<City>").append(cart.getShippingInfo().getCity()).append("</City>");
		s.append("<State>").append(cart.getShippingInfo().getState()).append("</State>");
		s.append("<Zip>").append(cart.getShippingInfo().getZipCode()).append("</Zip>");
		s.append("<DayPhone>").append(cart.getShippingInfo().getMainPhone()).append("</DayPhone>");
		if (StringUtil.checkVal(eveningPhone).length() > 0) {
			s.append("<EveningPhone>").append(eveningPhone).append("</EveningPhone>");
		}
		s.append("</Address>");
		s.append("</Addresses>");
		s.append("<Shipping type=\"").append(cart.getShipping().getShippingMethodId()).append("\">");
		s.append(cart.getShipping().getShippingCost()).append("</Shipping>");
		s.append("<PromotionCode>").append(StringUtil.checkVal(cart.getPromotionCode())).append("</PromotionCode>");
		s.append("<OrderSubtotal>").append(cart.getSubTotal()).append("</OrderSubtotal>");
		s.append("<OrderTax>").append(cart.getTaxAmount()).append("</OrderTax>");
		s.append("<OrderDiscount>").append(StringUtil.checkVal(cart.getPromotionDiscount())).append("</OrderDiscount>");
		s.append("<MemberID>").append(StringUtil.checkVal(cart.getBillingInfo().getProfileId())).append("</MemberID>");
		s.append("<CustomerIP>").append(ipAddr).append("</CustomerIP>");
		
		// credit card info
		if (cart.getPayment() != null) {
			String expMonth = cart.getPayment().getExpirationMonth();
			if (expMonth.length() == 1) expMonth = "0" + expMonth;
			int expYear = Convert.formatInteger(cart.getPayment().getExpirationYear()) - 2000;
			s.append("<CreditCard>");
			s.append("<Name>").append(cart.getPayment().getPaymentName()).append("</Name>");
			s.append("<Number>").append(cart.getPayment().getPaymentNumber()).append("</Number>");
			s.append("<ExpMonth>").append(expMonth).append("</ExpMonth>");
			s.append("<ExpYear>").append(expYear).append("</ExpYear>");
			s.append("<CSC>").append(cart.getPayment().getPaymentCode()).append("</CSC>");
			s.append("</CreditCard>");
		}
		
		s.append("<Products>");
		for(Iterator<String> iter = cart.getProductCountById().keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			this.addProductXML(s, cart.getItems().get(key).getProduct(), cart.getProductCountById().get(key), true);
		}
		s.append("</Products>");
		// if this is a PayPal order, add tags here
		if (req.hasParameter("paypal") && 
				StringUtil.checkVal(req.getParameter("paypal")).equalsIgnoreCase("do")) {
			UserDataVO buyer = cart.getBillingInfo();
			s.append("<PayerID>");
			s.append(buyer.getAttributes().get("PAYER_ID"));
			s.append("</PayerID>");
			s.append("<TransactionID>");
			s.append(buyer.getAttributes().get("TRANSACTION_ID"));
			s.append("</TransactionID>");
			s.append("<Token>");
			s.append(buyer.getAttributes().get("TOKEN"));
			s.append("</Token>");
			s.append("<AddressStatus>");
			s.append(buyer.getAttributes().get("ADDRESS_STATUS"));
			s.append("</AddressStatus>");
			s.append("<PayerStatus>");
			s.append(buyer.getAttributes().get("PAYER_STATUS"));
			s.append("</PayerStatus>"); //(Y or N)
			s.append("<CorrelationID>");
			s.append(buyer.getAttributes().get("CORRELATION_ID"));
			s.append("</CorrelationID>");
			s.append("<PendingReason>");
			s.append(buyer.getAttributes().get("PENDING_REASON"));
			s.append("</PendingReason>");
		}
		s.append("</OrderRequest>");
		log.debug("*****************\nOrder Request : " + s + "\n");
		return s;
	}
	
	/**
	 * Retrieves the URL to use for retrieving catalog/product-related data.  To use an 'https' prefixed
	 * url, pass a boolean value of 'true' for useSSL.
	 * @param siteId
	 * @param suffix
	 * @param useSSL
	 * @return
	 */
	private String retrieveServiceURL(String siteId, String suffix, boolean useSSL) {
		StringBuffer prefix = new StringBuffer();
		if (useSSL) prefix.append("https://"); else prefix.append("http://");
		if (siteId.equalsIgnoreCase("USA_1")) {
			prefix.append(StringUtil.checkVal(getAttribute(USA_BASE_URL)));
		} else if (siteId.equalsIgnoreCase("USA_2")) {
			prefix.append("www.whatonearthcatalog.com");
		} else if (siteId.equalsIgnoreCase("USA_3")) {
			prefix.append("www.thewirelesscatalog.com");
		} else if (siteId.equalsIgnoreCase("USA_4")) {
			prefix.append("www.supportplus.com");
		} else if (siteId.equalsIgnoreCase("USA_5")) {
			prefix.append("www.basbleu.com");
		} else if (siteId.equalsIgnoreCase("USA_6")) {
			prefix.append("www.shopfloriana.com");
		} else if (siteId.equalsIgnoreCase("USA_7")) {
			prefix.append("www.catalogclassics.com");
		} else {
			prefix.append(StringUtil.checkVal(getAttribute(USA_BASE_URL)));
		}
		prefix.append("/cgi-bin/ws/").append(suffix);
		log.debug("using serviceURL: " + prefix.toString());
		return prefix.toString();
	}
	
	/**
	 * Removes the catalog site ID prefix and any underscore_suffix from a product ID and returns
	 * the base or raw product ID.
	 * @param catalogSiteIdPrefix
	 * @param prodId
	 * @return
	 */
	private String retrieveBaseProductId(String catalogSiteIdPrefix, String prodId) {
		String baseId = prodId;
		// remove the catalogSiteId
		if (prodId.indexOf(catalogSiteIdPrefix) > -1) {
			baseId = prodId.substring(prodId.indexOf(catalogSiteIdPrefix) + catalogSiteIdPrefix.length());
		}
		// now remove anything else that is not the product ID.
		if (baseId.indexOf("_") > -1) {
			baseId = baseId.substring(0, baseId.indexOf("_"));
		}
		return baseId;
	}
	
	/**
	 * Adds product XML tags to the XML request by parsing a Map of ShoppingCartItemVOs.
	 * @param s
	 * @param items
	 */
	private void addProductXMLByMap(StringBuilder s, Map<String, ShoppingCartItemVO> items) {
		s.append("<Products>");
		for (Iterator<String> iter = items.keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			this.addProductXML(s, items.get(key).getProduct(), items.get(key).getQuantity(), true);
		}
		s.append("</Products>");
	}
	
	/**
	 * Adds product XML tags to the XML request by parsing a Collection of ShoppingCartItemVOs.
	 * @param s
	 * @param prods
	 */
	private void addProductXMLByCollection(StringBuilder s, Collection<ShoppingCartItemVO> prods) {
		s.append("<Products>");		
		for (Iterator<ShoppingCartItemVO> iter = prods.iterator(); iter.hasNext(); ) {
			ShoppingCartItemVO item = iter.next();
			this.addProductXML(s, item.getProduct(), item.getQuantity(), true);
		}	
		s.append("</Products>");
	}
	
	/**
	 * Helper method for adding product XML tags to XML request.  Product attributes are included in
	 * the XML if includeAttributes is set to true.
	 * @param s
	 * @param product
	 * @param qty
	 * @param includeAttributes
	 */
	private void addProductXML(StringBuilder s, ProductVO product, Integer qty, boolean includeAttributes) {
		String catalogSiteIdPrefix = StringUtil.checkVal(getAttribute(CATALOG_SITE_ID)) + "_";
		String id = this.retrieveBaseProductId(catalogSiteIdPrefix, product.getProductId());
		s.append("<ProductID qty=\"").append(qty).append("\" id=\"").append(id).append("\">");
		if (includeAttributes) this.addProductAttributesXML(s, product);
		s.append("</ProductID>");
	}
	
	/**
	 * Adds product attribute XML tags to XML request.
	 * @param s
	 * @param product
	 */
	private void addProductAttributesXML(StringBuilder s, ProductVO product) {
		log.debug("adding product attributes to shipping retrieval request");
		if (product.getProdAttributes() != null && product.getProdAttributes().size() > 0) {
			String val = null;
			for (String key : product.getProdAttributes().keySet()) {
				ProductAttributeVO pAtt = (ProductAttributeVO) product.getProdAttributes().get(key);
				s.append("<ProdAttr code=\"");
				s.append(pAtt.getValueText()).append("\">");
				if(pAtt.getAttributeId().equals("USA_CUSTOM")) {
					val = StringUtil.checkVal(pAtt.getAttributes().get("formVal"));
					s.append(StringEncoder.urlEncode(val));
				}
				s.append("</ProdAttr>");
			}
		}
	}
	
	
	/**
	 * Calls the web service using the connection manager and xmlRequest value.  Utilizes a
	 * helper method to retrieves the requested XML element.
	 * @param url
	 * @param xmlRequest
	 * @param elem
	 * @return
	 * @throws DocumentException
	 */
	private Element callWebService (String url, StringBuilder xmlRequest, String elem) 
		throws DocumentException {
		// Make the HTTP call the web service
		log.debug("url: " + url);
		log.debug("xmlRequest: " + xmlRequest);
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = null;
		try {
			data = conn.retrieveDataViaPost(url, xmlRequest.toString());
		} catch (Exception ioe) {
			StringBuilder msg = new StringBuilder("Error retrieving '").append(elem);
			msg.append("' data from web service for url ").append(url).append(" - ");
			msg.append(ioe.getMessage());
			log.error(msg);
			return this.createErrorElement("SYSTEM_ERROR", "A system error occurred.");
		}
		// just in case the connection manager failed but 'ate' the
		// exception instead of throwing it to us.
		if (data == null) {
			log.debug("Unknown exception; error retrieving data from web service...");
			return this.createErrorElement("SYSTEM_ERROR", "A system error occurred.");
		}
		log.debug("xml response data: " + new String(data));
		return this.retrieveElement(data, elem);
	}
	
	/**
	 * Helper method for parsing an XML service response byte array.  Returns the requested XML element.
	 * @param data
	 * @param elemName
	 * @return
	 * @throws DocumentException
	 */
	private Element retrieveElement(byte[] data, String elemName) 
			throws DocumentException {
		log.debug("retrieving element '" + elemName + "'");
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		try {
			Document doc = new SAXReader().read(bais);
			if (doc.getRootElement().getName().equalsIgnoreCase("error") || 
					elemName.equalsIgnoreCase("root")) {
				return doc.getRootElement();
			} else {
				return doc.getRootElement().element(elemName);
			}
		} catch (DocumentException de) {
			throw de;
		} catch (Exception e) {
			log.error("elemName: " + elemName);
			log.error("error reading element", e);
		}
		return null;
	}
	
	/**
	 * Creates an Element named 'Error' using the code and message values
	 * passed in.
	 * @param code
	 * @param message
	 * @return
	 */
	private Element createErrorElement(String code, String message) {
		Element err = new DefaultElement("Error");
		err.addElement("ErrorCode").setText("SystemError");
		err.addElement("ErrorMessage").setText("SystemError");
		return err;
	}
	
	/**
	 * DEBUG - creates a dummy order response using the values in the cart
	 * @param cart
	 * @return
	 */
	@SuppressWarnings("unused")
	private Element createDebugResponseElement(ShoppingCartVO cart) {
		Element ele = new DefaultElement("OrderResponse");
		
		Element subEle = new DefaultElement("GrandTotal");
		subEle.addText(safeDouble(cart.getCartTotal()));
		ele.add(subEle);
		log.debug("cart total: " + cart.getCartTotal());
		
		subEle = new DefaultElement("ProductTotal");
		subEle.addText(safeDouble(cart.getSubTotal()));
		ele.add(subEle);
		log.debug("cart sub total: " + cart.getSubTotal());
		
		subEle = new DefaultElement("TaxTotal");
		subEle.addText(safeDouble(cart.getTaxAmount()));
		ele.add(subEle);
		
		subEle = new DefaultElement("ShippingTotal");
		subEle.addText(safeDouble(cart.getShipping().getShippingCost()));
		ele.add(subEle);
		log.debug("cart shipping cost: " + cart.getShipping().getShippingCost());
		
		
		subEle = new DefaultElement("DiscountTotal");
			if (cart.getCartDiscount() != null && ! cart.getCartDiscount().isEmpty()) {
				subEle.addText(safeDouble(cart.getCartDiscount().get(0).getDiscountDollarValue()));
			} else {
				subEle.addText("0.00");
			}
		ele.add(subEle);
		
		subEle = new DefaultElement("OrderNumber");
		subEle.addText("DEBUG: " + Calendar.getInstance().getTimeInMillis());
		ele.add(subEle);
		
		subEle = new DefaultElement("TransactionID");
		subEle.addText("DEBUG: " + cart.getInvoiceNo());
		ele.add(subEle);
		
		subEle = new DefaultElement("MSG");
		subEle.addText("DEBUG: Test order generated from cart.");
		ele.add(subEle);
		
		log.debug("debug response element: " + ele.asXML());
		return ele;
	}
	
	/**
	 * for DEBUG
	 * @param val
	 * @return
	 */
	private String safeDouble(double val) {
		try {
			return new Double(val).toString();
		} catch (NumberFormatException nfe) {
			return "0.00";
		}
	}
	
	/**
	 * Calls the custom transaction logger to log the order request.
	 * @param req
	 * @param cart
	 * @param orderRequest
	 * @throws SQLException 
	 */
	private void logOrderTransaction(SMTServletRequest req, ShoppingCartVO cart, 
			StringBuilder orderRequest, boolean isRequest) {
		log.debug("logging order request...");
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String encryptKey = (String)getAttribute(Constants.ENCRYPT_KEY);
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		USATransactionLogger uLog = new USATransactionLogger();
		uLog.setDbConn(dbConn);
		uLog.setSchema(schema);
		uLog.setEncryptionKey(encryptKey);
		uLog.setSiteId(site.getSiteId());
		try {
			uLog.logTransaction(cart, orderRequest, isRequest, req.hasParameter("paypal"));
		} catch (Exception e) {
			log.error("Error: Unable to log transaction, ", e);
		}
	}
	
}
