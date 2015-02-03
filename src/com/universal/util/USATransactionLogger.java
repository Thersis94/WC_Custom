package com.universal.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title: </b>USATransactionLogger.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jan 16, 2015<p/>
 *<b>Changes: </b>
 * Jan 16, 2015: David Bargerhuff: Created class.
 ****************************************************************************/
public class USATransactionLogger {

	private Logger log = Logger.getLogger(USATransactionLogger.class);
	private Connection dbConn;
	private String schema;
	private String encryptionKey;
	private String siteId;
	
	/**
	 * 
	 */
	public USATransactionLogger() {

	}
	
	/**
	 * @param cart
	 * @param logData
	 * @param isTransRequest
	 * @param useCardMask
	 * @throws SQLException 
	 */
	protected void logTransaction(ShoppingCartVO cart, StringBuilder logData,  
			boolean isTransRequest, boolean isPayPal) throws SQLException {
		
		// make sure we remove/mask payment (i.e. credit card) data.
		maskPaymentData(cart, logData, isTransRequest);
		
		String transactionId = parseTransactionId(cart);
		String profileId = null;
		if (cart.getBillingInfo() != null) profileId = cart.getBillingInfo().getProfileId();
		
		log.debug("logData size: " + logData.length());
		String encLogData = logData.toString();
		// encrypt logData
		StringEncrypter se = null;
		try {
			se = new StringEncrypter(encryptionKey);
			encLogData = se.encrypt(logData.toString());
		} catch (EncryptionException ee) {
			encLogData = logData.toString();
			log.error("Error encrypting order logging data, ", ee);
		}
		
		// insert SQL
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(schema);
		sql.append("USA_TRANSACTION_LOG (SITE_ID, TRANS_ID, TRANS_TYPE, ");
		sql.append("PROFILE_ID, DATA_TXT_TYPE, DATA_TXT_ENC, TRANS_DT) ");
		sql.append("values (?,?,?,?,?,?,?)");
		log.debug("order log SQL: " + sql.toString());

		PreparedStatement ps = null;
		int index = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(index++, siteId);
			ps.setString(index++, transactionId);
			ps.setString(index++,  isPayPal ? "PAYPAL" : "UNIVERSAL");
			ps.setString(index ++, profileId);
			ps.setString(index++, isTransRequest ? "REQUEST" : "RESPONSE");
			ps.setString(index++, encLogData);
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} finally {
			DBUtil.close(ps);
		}
		
	}
	
	/**
	 * If this is an order request, we mask credit card data.
	 * @param cart
	 * @param logData
	 * @param isTransRequest
	 */
	private void maskPaymentData(ShoppingCartVO cart, StringBuilder logData, boolean isTransRequest) {
		if (isTransRequest) {
			if (cart.getPayment() != null) {
				String maskedCardNum = StringUtil.checkVal(cart.getPayment().getPaymentNumber(), null);
				if (maskedCardNum != null) {
					// this is a standard Universal checkout, we mask CC info.
					StringBuilder mcNum = new StringBuilder(16);
					mcNum.append("************");
					if (maskedCardNum.length() > 4) {
						mcNum.append(maskedCardNum.substring(maskedCardNum.length() - 4));
					} else {
						mcNum.append(maskedCardNum);
					}
					int indexStart = logData.indexOf("<Cr");
					int indexEnd = logData.indexOf("</Cr") + 13;
					
					if (indexStart > -1) {
						log.debug("orig card data: " + logData.substring(indexStart, indexEnd));
						StringBuilder s = new StringBuilder();
						s.append("<CreditCard>");
						s.append("<Name>").append(cart.getPayment().getPaymentName()).append("</Name>");
						s.append("<Number>").append(mcNum).append("</Number>");
						s.append("<ExpMonth>").append("**").append("</ExpMonth>");
						s.append("<ExpYear>").append("**").append("</ExpYear>");
						s.append("<CSC>").append("***").append("</CSC>");
						s.append("</CreditCard>");
						log.debug("masked card data: " + s.toString());
						logData.replace(indexStart, indexEnd, s.toString());
					}
				}
			}
		}
	}
	
	/**
	 * Parses the transaction ID for this transaction using either the billing info
	 * profile ID or an attribute on the billing info attributes map.
	 * @param cart
	 * @return
	 */
	private String parseTransactionId(ShoppingCartVO cart) {
		UserDataVO uBill = cart.getBillingInfo();
		// first look for a value on the attributes Map
		if (uBill.getAttributes() != null) {
			return (String) uBill.getAttributes().get("TRANSACTION_ID");
		} else {
			/* If transaction ID not found, look for a profile ID which is 
			 * either a WC profile ID or Universal's member ID that we set on the 
			 * UserDataVO object as profile ID during a user login to Universal. */
			if (StringUtil.checkVal(uBill.getProfileId(), null) != null) {
				return uBill.getProfileId();
			} else {
				return null;
			}
		}
	}

	/**
	 * @param siteId the siteId to set
	 */
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @param schema the schema to set
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

	/**
	 * @param encryptionKey the encryptionKey to set
	 */
	public void setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
	}	

}
