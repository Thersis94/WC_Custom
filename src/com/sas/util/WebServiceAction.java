package com.sas.util;

// JDK 1.6.x
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.OrderCompleteVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.AuthenticationException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

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
 ****************************************************************************/
public class WebServiceAction extends SBActionAdapter {
	
	/**
	 * URL of the Web Service APIs
	 */
	public static final String SAS_BASE_URL = "sasBaseUrl";
	
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
		attributes.put("sasBaseUrl", "https://www.stacksandstacks.com/shop/api/");

	}

	public static void main(String[] args) throws Exception {
		org.apache.log4j.BasicConfigurator.configure();
		WebServiceAction wsa = new WebServiceAction(null);
		wsa.attributes.put("sasBaseUrl", "https://www.stacksandstacks.com/shop/api/");
		Map<String, Integer> prods = new HashMap<String, Integer>();
		prods.put("80325", 3);
		wsa.retrieveShippingInfo("80401", prods);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		
	}
	
	/**
	 * 
	 * @param shippingInfo
	 * @param prods
	 * @return
	 */
	public double retrieveTaxInfo(UserDataVO shippingInfo, Collection<ShoppingCartItemVO> prods, String pc) 
	throws IOException, DocumentException {
		if (shippingInfo == null || prods == null || prods.size() == 0) return 0.0;
		
		String url = getAttribute(SAS_BASE_URL) + "tax-request.html";
		
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<TaxRequest>");
		s.append("<Zip>").append(shippingInfo.getZipCode()).append("</Zip>");
		s.append("<City>").append(shippingInfo.getCity()).append("</City>");
		s.append("<ProductIDs>");
		
		for (Iterator<ShoppingCartItemVO> iter = prods.iterator(); iter.hasNext(); ) {
			ShoppingCartItemVO item = iter.next();
			s.append("<ProductID qty=\"").append(item.getQuantity()).append("\">");
			s.append(item.getProductId()).append("</ProductID>");
		}	
			
		s.append("</ProductIDs>");
		s.append("<PromotionCode>").append(pc).append("</PromotionCode>");
		s.append("</TaxRequest>");
		
		// Make the HTTP call
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveDataViaPost(url, s.toString());
		
		// Parse the returned data
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Element sr = doc.getRootElement().element("Tax");
		double tax = Convert.formatDouble(sr.getTextTrim());
		
		return tax;
	}
	
	/**
	 * 
	 * @param shippingInfo
	 * @param prods
	 * @return
	 */
	public double retrievePromotionDiscount(Collection<ShoppingCartItemVO> prods, String pc) 
	throws IOException, DocumentException {
		if (prods == null || prods.size() == 0) return 0.0;
		
		String url = getAttribute(SAS_BASE_URL) + "promotion-code-request.html";
		
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<PromotionCodeRequest>");
		s.append("<Code>").append(pc).append("</Code>");
		s.append("<ProductIDs>");
		
		for (Iterator<ShoppingCartItemVO> iter = prods.iterator(); iter.hasNext(); ) {
			ShoppingCartItemVO item = iter.next();
			s.append("<ProductID qty=\"").append(item.getQuantity()).append("\">");
			s.append(item.getProductId()).append("</ProductID>");
		}	
			
		s.append("</ProductIDs>");
		s.append("</PromotionCodeRequest>");
		
		// Make the HTTP call
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveDataViaPost(url, s.toString());
		
		// Parse the returned data
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Element sr = doc.getRootElement().element("Discount");
		
		double discount = Convert.formatDouble(sr.getTextTrim());
		log.debug("Promo Code: " + pc + "|" + discount);
		return discount;
	}
	

	/**
	 * 
	 * @param shippingInfo
	 * @param prods
	 * @return
	 */
	public UserDataVO authenticateMember(String email, String pwd) 
	throws IOException, DocumentException, AuthenticationException {
		String url = getAttribute(SAS_BASE_URL) + "member-request.html";
		
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<MemberRequest>");
		s.append("<Email>").append(email).append("</Email>");
		s.append("<Password>").append(pwd).append("</Password>");
		s.append("</MemberRequest>");
		
		// Make the HTTP call
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveDataViaPost(url, s.toString());
		return this.parseUserData(data);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 * @throws DocumentException
	 */
	@SuppressWarnings("unchecked")
	public UserDataVO parseUserData(byte[] data) 
	throws DocumentException, AuthenticationException {
		// Parse the returned data
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Map<String, UserDataVO> locs = new HashMap<String, UserDataVO>();
		
		// Parse out the main info and member id
		Element root = doc.getRootElement();
		String memId = StringUtil.checkVal(root.element("MemberID").getTextTrim());
		
		// Make sure the member id is present.  If not, throw and exception
		log.debug("Mem ID Length: " + memId.length());
		if (memId.length() == 0) throw new AuthenticationException("Not authorized");
		
		// PArdse out the addresses
		Element addrContainer = root.element("Addresses");
		List<Element> addresses = addrContainer.elements("Address");
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
			user.setProfileId(memId);
			user.setAuthenticationId(memId);
			
			// Set the billing info to the main user data and the extended info
			// as the shipping info (if it exists)
			String type = StringUtil.checkVal(address.attributeValue("type"));
			if (BILLING_USER_TYPE.equalsIgnoreCase(type)) {
				completeUser = user;
				if (completeUser.getUserExtendedInfo() == null)
					completeUser.setUserExtendedInfo(user);
					
			} else if (SHIPPING_USER_TYPE.equalsIgnoreCase(type)) {
				if (user.getLastName().length() > 0)
					completeUser.setUserExtendedInfo(user);
			}
			
			// If the shipping info is empty, assign the billing info to the map
			if (user.getLastName().length() == 0 && SHIPPING_USER_TYPE.equalsIgnoreCase(type)) {
				locs.put(type, locs.get(BILLING_USER_TYPE));
			} else {
				locs.put(type, user);
			}
		}
		
		return completeUser;
	}
	
	/**
	 * 
	 * @param zip
	 * @param prods
	 * @return
	 * @throws IOException
	 * @throws DocumentException
	 */
	@SuppressWarnings("unchecked")
	public Map<String, ShippingInfoVO> retrieveShippingInfo(String zip, Map<String, Integer> prods) 
	throws IOException, DocumentException {
		// Build the URL
		String url = getAttribute(SAS_BASE_URL) + "shipping-request.html";
		
		// Build the XML Request
		StringBuilder s = new StringBuilder();
		s.append("xml=").append(BASE_XML_HEADER).append("<ShippingRequest>");
		s.append("<Zip>").append(zip).append("</Zip>").append("<ProductIDs>");
		
		// Add the products
		for (Iterator<String> iter = prods.keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			s.append("<ProductID qty=\"").append(prods.get(key)).append("\">");
			s.append(key).append("</ProductID>");
		}
		
		s.append("</ProductIDs></ShippingRequest>");
		log.debug("XML Req: " + s);
		
		// Make the HTTP call
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveDataViaPost(url, s.toString());
		
		// Parse the returned data
		Map<String, ShippingInfoVO> shipping = new LinkedHashMap<String, ShippingInfoVO>();
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Element sr = doc.getRootElement().element("ShippingCost");
		List<Element> sc = sr.selectNodes("Method");
		for (int i=0; i < sc.size(); i++) {
			ShippingInfoVO vo = new ShippingInfoVO();
			Element ele = sc.get(i);
			String type = ele.attributeValue("type");
			vo.setShippingMethodId(type);
			vo.setShippingMethodName(type);
			vo.setShippingCost(Convert.formatDouble(ele.getTextTrim()));
			log.debug("Shipping: " + vo.getShippingCost() + "|" + vo.getShippingMethodId());
			shipping.put(ele.attributeValue("type"),vo);
		}
		
		return shipping;
	}
	
	/**
	 * 
	 * @param cart
	 * @return
	 * @throws IOException
	 * @throws DocumentException
	 */
	public OrderCompleteVO placeOrder(ShoppingCartVO cart, String ipAddr)	
	throws IOException, DocumentException {
		// Build the URL
		String url = getAttribute(SAS_BASE_URL) + "order-request.html";
		
		// Get the request XML
		String requestXml = this.createOrderRequest(cart, ipAddr);
		log.debug("*****************\nRequest : " + requestXml);
		// Make the HTTP call
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveDataViaPost(url, requestXml);
		log.debug("XML: " + new String(data));
		return this.parseOrderResponse(data);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 * @throws DocumentException
	 */
	private OrderCompleteVO parseOrderResponse(byte[] data) throws DocumentException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		SAXReader reader = new SAXReader();
		Document doc = reader.read(bais);
		Element root = doc.getRootElement();
		
		OrderCompleteVO order = new OrderCompleteVO();
		order.setStatus(Convert.formatInteger(root.element("Status").getTextTrim()));
		order.setShipping(Convert.formatDouble(root.element("ShippingTotal").getTextTrim()));
		order.setTax(Convert.formatDouble(root.element("TaxTotal").getTextTrim()));
		order.setSubTotal(Convert.formatDouble(root.element("ProductTotal").getTextTrim()));
		order.setDiscount(Convert.formatDouble(root.element("DiscountTotal").getTextTrim()));
		order.setGrandTotal(Convert.formatDouble(root.element("GrandTotal").getTextTrim()));
		order.setOrderNumber(StringUtil.checkVal(root.element("OrderNumber").getTextTrim()));
		order.setMsg(root.element("MSG").getTextTrim());
		
		return order;
	}
	
	/**
	 * Builds the XML request from a ShoppingCartVO
	 * @param cart
	 * @param ipAddr
	 * @return
	 */
	private String createOrderRequest(ShoppingCartVO cart, String ipAddr) {
		// Build the XML Request
		String expMonth = cart.getPayment().getExpirationMonth();
		if (expMonth.length() == 1) expMonth = "0" + expMonth;
		int expYear = Convert.formatInteger(cart.getPayment().getExpirationYear()) - 2000;
		
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
		s.append("</Address>");
		s.append("</Addresses>");
		s.append("<ShippingMethod>").append(cart.getShipping().getShippingMethodId()).append("</ShippingMethod>");
		s.append("<PromotionCode>").append(StringUtil.checkVal(cart.getPromotionCode())).append("</PromotionCode>");
		s.append("<MemberID>").append(StringUtil.checkVal(cart.getBillingInfo().getProfileId())).append("</MemberID>");
		s.append("<CustomerIP>").append(ipAddr).append("</CustomerIP>");
		s.append("<CreditCard>");
		s.append("<Name>").append(cart.getPayment().getPaymentName()).append("</Name>");
		s.append("<Number>").append(cart.getPayment().getPaymentNumber()).append("</Number>");
		s.append("<ExpMonth>").append(expMonth).append("</ExpMonth>");
		s.append("<ExpYear>").append(expYear).append("</ExpYear>");
		s.append("<CSC>").append(cart.getPayment().getPaymentCode()).append("</CSC>");
		s.append("</CreditCard>");
		s.append("<ProductIDs>");
		
		for(Iterator<String> iter = cart.getProductCountById().keySet().iterator(); iter.hasNext(); ) {
			String key = iter.next();
			Integer qty = cart.getProductCountById().get(key);
			s.append("<ProductID qty=\"").append(qty).append("\">").append(key).append("</ProductID>");
		}
		
		s.append("</ProductIDs>");
		s.append("</OrderRequest>");
		log.debug("order Request: " + s);
		
		return s.toString();
	}
}
