package com.depuy.sitebuilder.ecoupon;

// JDK 1.5.0
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs 2.0
import com.siliconmtn.util.Convert;

// Fremarker API
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

/****************************************************************************
 * <b>Title</b>: CipherKey.java<p/>
 * <b>Description: </b> Creates an encoded value based upon 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since May 8, 2007
 ****************************************************************************/
public class CipherKey {
	private String pinCode = null;
	private String message = null;
	private static final String DECODE_VALS = " abcdefghijklmnopqrstuvwxyz0123456789!$%()*+,-.@;<=>?[]^_{|}~";
	private static final String FILLER_DATA = "couponsincproduction";
	private static final String OFFER_CODE = "50090";
	private static final String CHECK_CODE = "BN";
	private static final String SHORT_KEY = "0qyr6givbo";
	private static final String LONG_KEY = "yreqKQ7OltVbJkE6ocNDUSdns45WRxZhLg3BMGvu1IwaX29z8CjPTHpYAmFif";
	private static final String BASE_URL = "http://bricks.coupons.com/enable.asp?eb=1";
	
	/**
	 * Freemarker code to use in the HTML Text
	 */
	public static final String ECOUPON_URL_KEY = "eCouponUrl";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CipherKey cp = new CipherKey();
		cp.setPinCode("c0a80347d3406b08a2dfa254b1ac972d");
		cp.setMessage("the url is: <a href='${eCouponUrl}'>Click Here for a Coupon</a>");
	}
	
	/**
	 * Generates the eCoupon URL for the user to receive their coupon
	 * @return
	 */
	public String getCouponUrl() {
		StringBuffer sb = new StringBuffer();
		if(pinCode != null && pinCode.length() > 0) {
			sb.append(BASE_URL).append("&o=").append(OFFER_CODE);
			sb.append("&c=").append(CHECK_CODE).append("&p=").append(pinCode);
			sb.append("&cpt=").append(getEncodedVal());
		}

		return sb.toString();
	}
	
	/**
	 * Returns the encrypted pin code, offercode, short CipherKey and long CipherKey also
	 * known as the CPT parameter.  Uses the defailt offer code, short key and long key.
	 * Also uses the pin code assigned to the getter
	 * @return encoded value.  Null if no pin code
	 */	
	public String getEncodedVal() {
		if (pinCode ==  null || pinCode.length() == 0) return null;
		else return getEncodedVal(pinCode, OFFER_CODE, SHORT_KEY, LONG_KEY);
	}
	
	/**
	 * Returns the encrypted pin code, offercode, short CipherKey and long CipherKey also
	 * known as the CPT parameter.  Uses the defailt offer code, short key and long key
	 * @param pinCode
	 * @return
	 */
	public String getEncodedVal(String code) {
		this.pinCode = code;
		return this.getEncodedVal(pinCode, OFFER_CODE, SHORT_KEY, LONG_KEY);
	}

	/**
	 * Returns the encrypted pin code, offercode, short CipherKey and long CipherKey also
	 * known as the CPT parameter.
	 * @param pinCode User's unique identifier, as assigned by the client.
	 * @param iOfferCode Offer code for the coupon, as assigned by Coupons, Inc..
	 * @param sShortKey Short CipherKey for the coupon, as assigned by Coupons, Inc..
	 * @param sLongKey Long CipherKey for the coupon, as assigned by Coupons, Inc..
	 * @return
	 */
	public String getEncodedVal(String sPinCode, String iOfferCode, String sShortKey, String sLongKey) {
		int oCode = 0;
		if (iOfferCode.length() == 5) 
			oCode = Convert.formatInteger(iOfferCode) % 1000;
		else
			oCode = Convert.formatInteger(iOfferCode);
		
		int[] encodeModulo = new int[256];
		int[] vob = new int[2];
		
		vob[0] = (int)(oCode / 100);
		vob[1] = oCode % 100;
		
		for (int i = 0; i < 256; i++) {
			encodeModulo[i] = 0;
		}
		
		for (int i = 0; i <= 60; i++) {
			encodeModulo[(int)DECODE_VALS.charAt(i)] = i;
		}
		
		// append offer code to key
		sPinCode = sPinCode.toLowerCase() + iOfferCode;
		if (sPinCode.length() < 20) {
			sPinCode += FILLER_DATA.substring(0, (20 - sPinCode.length()));
		}
		
		int s1, s2, s3;
		int q = 0;
		int j = sPinCode.length();
		int k = sShortKey.length();
		String sCPT = "";
		
		for (int i = 0; i < j; i++) {
			s1 = encodeModulo[(int)sPinCode.charAt(i)];
			s2 = 2 * (encodeModulo[(int)sShortKey.charAt(i % k)]);
			s3 = vob[(i + 1) % 2];
			q = (q + s1 + s2 + s3) % 61;
			sCPT += sLongKey.charAt(q);
		}
		
		return sCPT;
	}


	/**
	 * @return the pinCode
	 */
	public String getPinCode() {
		return pinCode;
	}


	/**
	 * @param pinCode the pinCode to set
	 */
	public void setPinCode(String pinCode) {
		this.pinCode = pinCode;
	}
	
	/**
	 * Parses the response text to the user and replaces the freemarker
	 * ecoupon url with the appropriate url tag
	 * @return
	 */
	public String getResponseText() {
		// Set the data params for the marker
		Map<String, String> data = new HashMap<String, String>();
		data.put(ECOUPON_URL_KEY, this.getCouponUrl());
		
		// Create the free marker config and templates
		Configuration cfg = new Configuration();
		Template temp = null;
		StringBuffer sb = new StringBuffer();
		try {
			// Get or create a template
			cfg.setObjectWrapper(new DefaultObjectWrapper());
			
			// Use the String loader and load the template
			StringTemplateLoader stl = new StringTemplateLoader();
			stl.putTemplate("HTML", message);
			cfg.setTemplateLoader(stl);
			temp = cfg.getTemplate("HTML");
			
			// Load the data to a stream
			StringWriter out = new StringWriter();
			temp.process(data, out);
			sb = out.getBuffer();
			out.flush();
			
		} catch(Exception e) {
			
		}
		
		return sb.toString();
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
