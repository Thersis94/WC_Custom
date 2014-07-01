package com.sheets.action;

// JDK 1.6.x
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

// JDK 1.6
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;

// Apache Commons
import org.apache.commons.codec.binary.Hex;

// Apache log4J 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: WebServiceManager.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Oct 18, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class WebServiceManager {
	private static final Logger log = Logger.getLogger("WebServiceManager");
	private String namespaceUri = "";
	public static final String CONNECTION_URL = "https://www.sheetsbrand.com/soap/mobile-ws/";
	public static final String API_KEY = "s778ss988786shdjode998d";
	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
	/**
	 * 
	 */
	public WebServiceManager() {
		
	}
	
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		log.debug("Starting Sheets web service manager");
		
		WebServiceManager wsm = new WebServiceManager();
		try {
			log.debug("Login: " + wsm.authenticate("jrcamire@yahoo.com", "cannondale"));
			//log.debug("taxes: " + wsm.calcTaxes("33430"));
		} catch (Exception e) {
			log.debug("Invalid Login: ", e);
		} 
	}
	
	/**
	 * 
	 * @param userName
	 * @param pwd
	 * @return
	 * @throws IOException
	 * @throws SOAPException
	 */
	public UserDataVO authenticate(String userName, String pwd) 
	throws IOException, SOAPException {
		String md5Pwd = pwd;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(md5Pwd.getBytes());
			md5Pwd = new String(Hex.encodeHex(thedigest));
		} catch (NoSuchAlgorithmException e) {}
		
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope ");
		xml.append("xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" ");
		xml.append("xmlns:ns1=\"urn:xmethods-delayed-quotes\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
		xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		xml.append("xmlns:enc=\"http://www.w3.org/2003/05/soap-encoding\"><env:Body>");
		xml.append("<ns1:login env:encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\">");
		xml.append("<api_key xsi:type=\"xsd:string\">").append(API_KEY).append("</api_key>");
		xml.append("<email_address xsi:type=\"xsd:string\">").append(userName).append("</email_address>");
		xml.append("<password xsi:type=\"xsd:string\">").append(md5Pwd).append("</password></ns1:login></env:Body></env:Envelope>");
		log.debug(xml.toString());
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		conn.addRequestHeader("User-Agent", USER_AGENT);
		conn.addRequestHeader("Content-type", "application/soap+xml; charset=utf-8");
		conn.addRequestHeader("SOAPAction", CONNECTION_URL + "login");
		byte[] b = conn.retrieveDataViaPost(CONNECTION_URL + "login", xml.toString());
		log.debug("hdrs: " + conn.getHeaderMap().keySet());
		
		log.debug("File Length:" + b.length + "|" + conn.getHeaderMap().get("Content-Length"));
		log.debug("Response Code: " + conn.getResponseCode());
		
		log.debug(new String(b));
		
		return this.parseUserData(b, pwd, userName);
	}
	
	/**
	 * 
	 * @param b
	 * @return
	 * @throws SOAPException
	 */
	protected UserDataVO parseUserData(byte[] b, String pwd, String userName) throws SOAPException {
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		MessageFactory msgFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		SOAPMessage message = msgFactory.createMessage();
		message.getSOAPPart().setContent(new StreamSource(bais));
		SOAPElement loginResp = (SOAPElement) message.getSOAPBody().getFirstChild();
		
		SOAPElement results = null;
		for (Iterator<?> iter = loginResp.getChildElements(); iter.hasNext(); ) {
			results = (SOAPElement)iter.next();
			if ("Results".equalsIgnoreCase(results.getNodeName())) break;
			
		}
		
		String profileId = new UUIDGenerator().getUUID();
		UserDataVO completeUser = null;
		UserDataVO user = new UserDataVO();
		user.setPassword(pwd);
		user.setProfileId(profileId);
		user.setAuthenticationId(profileId);
		user.setEmailAddress(userName);
		SOAPElement item = null;
		for (Iterator<?> iter = results.getChildElements(); iter.hasNext();) {
			item = (SOAPElement)iter.next();
			user = new UserDataVO();
			user.setPassword(pwd);
			user.setProfileId(profileId);
			user.setAuthenticationId(profileId);
			user.setAuthenticated(true);
			user.setEmailAddress(userName);

			for (Iterator<?> iter1 = item.getChildElements(); iter1.hasNext(); ) {
				SOAPElement field = (SOAPElement)iter1.next();
				
				if ("first_name".equalsIgnoreCase(field.getNodeName())) user.setFirstName(field.getValue());
				else if ("last_name".equalsIgnoreCase(field.getNodeName())) user.setLastName(field.getValue());
				else if ("address1".equalsIgnoreCase(field.getNodeName())) user.setAddress(field.getValue());
				else if ("address2".equalsIgnoreCase(field.getNodeName())) user.setAddress2(field.getValue());
				else if ("city".equalsIgnoreCase(field.getNodeName())) user.setCity(field.getValue());
				else if ("state".equalsIgnoreCase(field.getNodeName())) user.setState(field.getValue());
				else if ("zip".equalsIgnoreCase(field.getNodeName())) user.setZipCode(field.getValue());
				else if ("country".equalsIgnoreCase(field.getNodeName())) user.setCountryCode(field.getValue());
				else if ("email".equalsIgnoreCase(field.getNodeName())) user.setEmailAddress(field.getValue());
				else if ("phone".equalsIgnoreCase(field.getNodeName())) {
					PhoneVO p = new PhoneVO();
					p.setPhoneNumber(field.getValue());
					p.setPhoneType(PhoneVO.HOME_PHONE);
					user.addPhone(p);
				} else if ("default_address".equalsIgnoreCase(field.getNodeName())) {
					completeUser = user;
				}
			}
			
		}
		
		// If there is no default address, assign it
		if (completeUser == null) completeUser = user;
		return completeUser;
	}
	
	/**
	 * 
	 * @param zipCode
	 * @return
	 * @throws SOAPException 
	 * @throws Exception
	 */
	public double calcTaxes(String zipCode) throws IOException, SOAPException {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><env:Envelope ");
		xml.append("xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" ");
		xml.append("xmlns:ns1=\"urn:xmethods-delayed-quotes\" ");
		xml.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
		xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		xml.append("xmlns:enc=\"http://www.w3.org/2003/05/soap-encoding\">	");
		xml.append("<env:Body><ns1:taxes env:encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\"> ");
		xml.append("<api_key xsi:type=\"xsd:string\">").append(API_KEY);
		xml.append("</api_key><zipcode xsi:type=\"xsd:string\">").append(zipCode);
		xml.append("</zipcode></ns1:taxes></env:Body></env:Envelope>");
		
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		conn.addRequestHeader("User-Agent", USER_AGENT);
		conn.addRequestHeader("Content-type", "application/soap+xml; charset=utf-8");
		conn.addRequestHeader("SOAPAction", CONNECTION_URL + "taxes");
		byte[] b = conn.retrieveDataViaPost(CONNECTION_URL + "taxes", xml.toString());
		
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		MessageFactory msgFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		SOAPMessage message = msgFactory.createMessage();
		message.getSOAPPart().setContent(new StreamSource(bais));
		SOAPElement taxResp = (SOAPElement) message.getSOAPBody().getFirstChild();
		
		SOAPElement results = null;
		for (Iterator<?> iter = taxResp.getChildElements(); iter.hasNext(); ) {
			results = (SOAPElement)iter.next();
			if ("Results".equalsIgnoreCase(results.getNodeName())) break;
			
		}
		
		SOAPElement item = (SOAPElement) results.getFirstChild();
		double tax = 0.0;
		
		if (item != null) {
			for (Iterator<?> iter = item.getChildElements(); iter.hasNext(); ) {
				SOAPElement temp = (SOAPElement) iter.next();
				if ("tax".equalsIgnoreCase(temp.getNodeName())) {
					tax = Convert.formatDouble(temp.getTextContent());
				}
			}
		}
		
		return tax;
	}
	
	
	 /* Creates the SOAP message and modifies the HTTP header SOAPAction
	 * @param type
	 * @return
	 * @throws SOAPException
	 */
	protected SOAPMessage initMessage(String type) throws SOAPException {
		
		// Create the message and set the SoapAction HTTP Header
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage message = msgFactory.createMessage();
		
		message.getMimeHeaders().addHeader("SoapAction",namespaceUri + type);
		
		return message;
	}
	
	
	/**
	 * Connects to the SOAP Web Service and passes the message and returns the response
	 * @param message
	 * @param url
	 * @return
	 * @throws SOAPException
	 */
	protected SOAPMessage sendMessage(SOAPMessage message, String connectionUrl) 
	throws SOAPException {
		message.saveChanges();

		SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();
		SOAPConnection connection = factory.createConnection();
		SOAPMessage response = null;
		try {
			URL endpoint = new URL(connectionUrl);
			response = connection.call(message, endpoint);
		} catch (Exception e) {
			log.debug("Error Making Connection", e);
			throw new SOAPException("Unable to connect to server: " + connectionUrl, e);
		} finally {
			try {
				connection.close();
			} catch(Exception e) {}
		}
		
		return response;
	}
}
