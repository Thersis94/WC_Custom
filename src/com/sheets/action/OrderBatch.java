package com.sheets.action;

// JDK 1.6.x

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base libs
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.ftp.SFTPClient;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.PGPEncrypt;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs

/****************************************************************************
 * <b>Title</b>: OrderBatch.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Oct 26, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class OrderBatch {

	private Properties config = new Properties();
	private static final Logger log = Logger.getLogger("OrderBatch");
	private Connection conn = null;
	private String delim = ",";
	private String delimQuot = ",\"";
	private String quotDelim = "\",";
	private String quotDelimQuot = "\",\"";
	
	/**
	 * 
	 */
	public OrderBatch() throws Exception {
		// Initialize the logger
		PropertyConfigurator.configure("scripts/sheets_order_batch_log4j.properties");
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/sheets_order_batch.properties")));
		
		// Get the DB Connection
		conn = getConnection();
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)  {
		OrderBatch ob = null;
		log.warn("Starting Order Batch");
		try {
			ob = new OrderBatch();
			
			// Get the pending orders
			List<ShoppingCartVO> orders = ob.getOrders();
			log.debug("Number of orders: " + orders.size());
			if (orders.size() == 0) {
				log.warn("No Messages to process, exiting process");
				System.exit(1);
			}
			
			// Create the file data in CSV format
			ByteArrayOutputStream orderData = ob.addOrdersToFile(orders);
			log.debug(new String(orderData.toByteArray()));
			
			// Encrypt the file data
			PGPEncrypt enc = new PGPEncrypt();
			byte[] sheetsPGPKey = enc.loadFile(ob.config.getProperty("pgpKeyLoc"));
			byte[] encFile = enc.encrypt(sheetsPGPKey, orderData.toByteArray());
			
			// Send the file to the SFTP Server
			ob.moveFile(encFile);
		} catch (Exception e) {
			log.error("Unable to process order batch", e);
			
		} finally {
			if (ob != null) ob.closeDbconn();
		}
		
		log.warn("Completed Order Batch");
	}
	
	/**
	 * Transfers the encrypted file data to the sftp server
	 * @param data
	 * @throws IOException
	 */
	private void moveFile(byte[] data) throws IOException {
		// Create the fileName
		String fileName = "sheets_orders_" + Convert.formatDate(new Date(), Convert.DATE_TIME_NOSPACE_PATTERN) + ".csv.pgp";
		
    	// Connect to the SFTP Server
    	SFTPClient s = new SFTPClient();
    	int port = Convert.formatInteger(config.getProperty("sftpPort"));
        s.connect(config.getProperty("sftpHost"), port,config.getProperty("sftpUser"), config.getProperty("sftpPassword"));
       	log.debug("File Name: " + fileName);
       	// Transfer the data
       	s.writeData(data, fileName);

       	// Close the connection
	    s.disconnect();
    }
	
	/**
	 * 
	 * @param orderData
	 * @throws IOException
    private void writeFile(ByteArrayOutputStream orderData) 
	throws IOException {
		String fileName = config.getProperty("fileLocation");
		fileName += "sheets_orders_" + Convert.formatDate(new Date(), Convert.DATE_TIME_NOSPACE_PATTERN) + ".csv";
		FileOutputStream fos = new FileOutputStream(fileName);
		orderData.writeTo(fos);
		orderData.flush();
		orderData.close();
		fos.flush();
		fos.close();
	}
	 */
	
	/**
	 * 
	 * @param orders
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 * @throws EncryptionException
	 */
	private ByteArrayOutputStream addOrdersToFile(List<ShoppingCartVO> orders) 
	throws SQLException, IOException, EncryptionException {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(this.formatRowHeader().getBytes());
		
		for (int i=0; i < orders.size(); i++) {
			ShoppingCartVO order = orders.get(i);
			
			// Loop the products ordered as there needs to be one line in the batch
			// File for each entry
			Map<String, ShoppingCartItemVO> items = order.getItems();
			int j = 1;
			for (Iterator<String> iter = items.keySet().iterator(); iter.hasNext(); j++) {
				ShoppingCartItemVO item = items.get(iter.next());
				baos.write(this.formatRow(order, j, item.getProductId(), item.getQuantity()).getBytes());
			}
			
			// Update the record so the entry is marked as in progress
			this.updateOrderStatus(order.getOrderComplete().getOrderNumber());
		}
		
		return baos;
	}
	
	/**
	 * Creates the row data
	 * @param cart
	 * @param loc
	 * @param sku
	 * @param qty
	 * @return
	 * @throws EncryptionException 
	 */
	private String formatRow(ShoppingCartVO cart, int loc, String sku, int qty) 
	throws EncryptionException {
		StringEncrypter se = new StringEncrypter(config.getProperty("encryptKey"));
		StringBuilder s = new StringBuilder();
		
		// Manage the cc number and exp month
		String ccMonth = cart.getPayment().getExpirationMonth() + "";
		if (ccMonth.length() == 1) ccMonth = "0" + ccMonth;
		String ccNum = se.decrypt(cart.getPayment().getEncPaymentNumber());
		
		// MD5 encode the password
		String pwd = StringUtil.checkVal(cart.getBillingInfo().getPassword());
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(pwd.getBytes());
			pwd = new String(Hex.encodeHex(thedigest));
		} catch (NoSuchAlgorithmException e) {}
		
		Date orderDate = cart.getOrderComplete().getOrderDate();
		s.append(cart.getOrderComplete().getOrderNumber()).append(delim);
		s.append(Convert.formatDate(orderDate, Convert.DATE_TIME_DASH_PATTERN)).append(delimQuot);
		s.append(cart.getBillingInfo().getEmailAddress()).append(quotDelimQuot);
		s.append(pwd).append(quotDelimQuot);
		s.append(cart.getBillingInfo().getFirstName()).append(quotDelimQuot);
		s.append(cart.getBillingInfo().getLastName()).append(quotDelimQuot);
		s.append(cart.getShippingInfo().getFirstName()).append(quotDelimQuot);
		s.append(cart.getShippingInfo().getLastName()).append(quotDelimQuot);
		s.append(cart.getShippingInfo().getAddress()).append(quotDelimQuot);
		s.append(StringUtil.checkVal(cart.getShippingInfo().getAddress2())).append(quotDelimQuot);
		s.append(cart.getShippingInfo().getCity()).append(quotDelim);
		s.append(cart.getShippingInfo().getState()).append(delim);
		s.append(cart.getShippingInfo().getZipCode()).append(delim);
		s.append(cart.getShippingInfo().getMainPhone()).append(delim);
		s.append(cart.getShipping().getShippingMethodId()).append(delimQuot);
		s.append(cart.getBillingInfo().getAddress()).append(quotDelimQuot);
		s.append(StringUtil.checkVal(cart.getBillingInfo().getAddress2())).append(quotDelimQuot);
		s.append(cart.getBillingInfo().getCity()).append(quotDelim);
		s.append(cart.getBillingInfo().getState()).append(delim);
		s.append(cart.getBillingInfo().getZipCode()).append(delim);
		s.append(cart.getBillingInfo().getMainPhone()).append(delimQuot);
		s.append(cart.getPayment().getPaymentName()).append(quotDelim);
		s.append(cart.getPayment().getPaymentType()).append(delim);
		s.append(ccNum).append(delim);
		s.append(ccMonth).append(delim);
		s.append(cart.getPayment().getExpirationYear()).append(delim);
		s.append(cart.getPayment().getPaymentCode()).append(delim);
		s.append(cart.getSubTotal()).append(delim);
		s.append(0).append(delim);
		s.append("").append(delim);
		s.append(cart.getTaxAmount()).append(delim);
		s.append(cart.getShipping().getShippingCost()).append(delim);
		s.append(cart.getCartTotal()).append(delim);
		s.append(loc).append(delim);
		s.append(sku).append(delim);
		s.append(qty).append(delim);
		s.append(0).append(delim);
		s.append("").append("\n");
		
		return s.toString();
	}
	
	/**
	 * Creates the row header
	 * @param cart
	 * @return
	 */
	private String formatRowHeader() {
		StringBuilder s = new StringBuilder();
		s.append("Mobile Order ID").append(delim);
		s.append("Order Date").append(delim);
		s.append("Customer E-mail Address").append(delim);
		s.append("Customer MD5 Password").append(delim);
		s.append("Customer First Name").append(delim);
		s.append("Customer Last Name").append(delim);
		s.append("Shipping First Name").append(delim);
		s.append("Shipping Last Name").append(delim);
		s.append("Shipping Address 1").append(delim);
		s.append("Shipping Address 2").append(delim);
		s.append("Shipping City").append(delim);
		s.append("Shipping State").append(delim);
		s.append("Shipping Zip Code").append(delim);
		s.append("Shipping Phone Number").append(delim);
		s.append("Shipping Method").append(delim);
		s.append("Billing Address 1").append(delim);
		s.append("Billing Address 2").append(delim);
		s.append("Billing City").append(delim);
		s.append("Billing State").append(delim);
		s.append("Billing Zip Code").append(delim);
		s.append("Billing Phone Number").append(delim);
		s.append("Credit Card Name").append(delim);
		s.append("Credit Card Type").append(delim);
		s.append("Credit Card Number").append(delim);
		s.append("Credit Card Expiration Month").append(delim);
		s.append("Credit Card Expiration Year").append(delim);
		s.append("Credit Card CCV").append(delim);
		s.append("Order Sub-Total").append(delim);
		s.append("Order Level Discount Amount").append(delim);
		s.append("Order Level Discount Code").append(delim);
		s.append("Order Tax").append(delim);
		s.append("Order Shipping").append(delim);
		s.append("Order Total").append(delim);
		s.append("Item Index").append(delim);
		s.append("Item SKU").append(delim);
		s.append("Item Quantity").append(delim);
		s.append("Item Level Discount Amount").append(delim);
		s.append("Item Level Discount Code").append("\n");

		return s.toString();
	}
	
	/**
	 * 
	 * @param orderId
	 * @throws SQLException
	 */
	private void updateOrderStatus(String orderId) throws SQLException {
		String s = "update product_order set product_order_status_id = 10 ";
		s += "where order_id = ? ";
		PreparedStatement ps = conn.prepareStatement(s);
		ps.setString(1, orderId);
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * 
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private List<ShoppingCartVO> getOrders() throws SQLException, UnsupportedEncodingException {
		List<ShoppingCartVO> data = new ArrayList<ShoppingCartVO>();
		String s = "select * from product_order where organization_id = 'sheets' ";
		s += "and product_order_status_id = 1";
		
		PreparedStatement ps = conn.prepareStatement(s);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			String xmlCart = rs.getString("cart_data_bin");
			ByteArrayInputStream bais = new ByteArrayInputStream(xmlCart.getBytes("UTF-8"));
			XMLDecoder dec = new XMLDecoder(bais);
			ShoppingCartVO cart = (ShoppingCartVO) dec.readObject();
			dec.close();
			
			data.add(cart);
		}
		
		return data;
	}
	
	/**
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	private Connection getConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		return dbc.getConnection();
	}
	
	private void closeDbconn() {
		try {
			conn.close();
		} catch (Exception e) {}
	}

}
