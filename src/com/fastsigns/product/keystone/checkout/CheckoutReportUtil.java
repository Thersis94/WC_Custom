package com.fastsigns.product.keystone.checkout;

import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.ModifierVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO.OptionVO;
import com.fastsigns.product.keystone.vo.ProductDetailVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: CheckoutReportUtil.java<p/>
 * <b>Description: Creates an email that summerizes a users order based on the
 *  shopping cart given to the function, then send the email out to the customer</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 16, 2013
 ****************************************************************************/

public class CheckoutReportUtil {

	private MessageSender sndr = null;
	protected static Logger log = null;
	
	/**
	 * In order to make the message sneder work we need acceess to the database
	 * connection and the attribute map from the action calling this class
	 * @param attributes
	 * @param dbConn
	 */
	public CheckoutReportUtil(Map<String, Object> attributes, Connection dbConn) {
		sndr = new MessageSender(attributes, dbConn);
		log = Logger.getLogger(this.getClass());
	}
	
	/**
	 * Creates an email based on the shopping cart passed to this function
	 * and sets it as from the given sender.
	 * @param cart
	 * @param sender
	 */
	public void sendSummary(ShoppingCartVO cart, String franId, String designatorNm) {
		try {
			EmailMessageVO msg = new EmailMessageVO();
			msg.addRecipient(cart.getBillingInfo().getEmailAddress());
			msg.setFrom(franId + "@fastsigns.com");
			//msg.addRecipient("billy.siliconmtn.com");
			msg.setSubject("FASTSIGNS Online Order Confirmation");
			msg.setHtmlBody(getHtmlBody(cart, franId, designatorNm));
			msg.setTextBody(getTextBody(cart, franId, designatorNm));
			msg.setReplyTo(franId + "@fastsigns.com");
			sndr.sendMessage(msg);
		} catch (InvalidDataException e) {
			log.error("Unable to send order summary", e);
		}
	}
	
	/**
	 * Creates the, much simpler, text body of the email.
	 * @param cart
	 * @return
	 */
	private String getTextBody(ShoppingCartVO cart, String franId, String designatorNm) {
		StringBuilder body = new StringBuilder();
		body.append(getTextCustomerInfo(cart.getShippingInfo()));
		//body.append(getTextPaymentInfo(cart.getPayment()));
		//body.append(getTextBillingInfo(cart.getBillingInfo()));
		body.append(getTextItemizedCart(cart));
		body.append(getTextShippingInfo(cart, franId));
		body.append("Thank you for placing your order with FASTSIGNS").append(designatorNm != null ? designatorNm : "").append(".\n");
		body.append("This is an automated message, for questions please contact ").append(franId).append("@fastsigns.com.\n");
		return body.toString();
	}

	/**
	 * Creates a string detailing information about 
	 * the user for the text body of the email
	 * @param user
	 * @return
	 */
	private String getTextCustomerInfo(UserDataVO user) {
		StringBuilder customer = new StringBuilder();
		customer.append("Shipping Information\n");
		customer.append("First Name: ").append(user.getFirstName()).append("\n");
		customer.append("Last Name: ").append(user.getLastName()).append("\n");
		customer.append("Company: ").append(StringUtil.checkVal(user.getAttributes().get("companyNm"))).append("\n");
		customer.append("Shipping Address: ").append(user.getAddress()).append("\n");
		customer.append("Suite/Apt #: ").append(user.getAddress2()).append("\n");
		customer.append("City: ").append(user.getCity()).append("\n");
		customer.append("State: ").append(user.getState()).append("\n");
		customer.append("Zip Code: ").append(user.getZipCode()).append("\n");
		customer.append("Contact Phone: ").append(user.getMainPhone()).append("\n");
		return customer.toString();
	}
	
	/**
	 * Creates an itemized String detailing information about the products the
	 * user purchased.
	 * @param cart
	 * @return
	 */
	private String getTextItemizedCart(ShoppingCartVO cart) {
		StringBuilder sb = new StringBuilder();
		sb.append("Thank you for your order\n");
		sb.append("Order Summary\n");
		for(ShoppingCartItemVO vo : cart.getItems().values()){
			KeystoneProductVO p = (KeystoneProductVO) vo.getProduct();
			sb.append("Name: ").append(vo.getProductName()).append("\nSelect a Size: ");
			sb.append(p.getSizes().get(0).getDimensions());
			if(p.getModifiers() != null) {
				for(ModifierVO m : p.getModifiers().values()){
					sb.append("\n\t").append(m.getModifier_name()).append(": ");
					int i = 0;
					if(m.getAttributes() != null) {
						for(AttributeVO a : m.getAttributes().values()) {
							if(i > 0) sb.append(" , ");
							sb.append(a.getAttribute_name()).append(" - ");
							if(a.getOptions() != null) {
								for(OptionVO o : a.getOptions().values()){
									sb.append(o.getOption_name());
								}
							}
							i++;
						}
					}
				}
			}
			sb.append("\nPrice: ").append(p.getMsrpCostNo() - p.getDiscount()/vo.getQuantity()).append("\nQuantity: ").append(vo.getQuantity());
			sb.append("\nTotal: ").append((p.getMsrpCostNo() * vo.getQuantity()) - p.getDiscount()).append("\n\n");
		}
		sb.append("Subtitle: ").append(String.format("%.2f", cart.getSubTotal())).append("\n");
		sb.append("Tax: ").append(String.format("%.2f", cart.getTaxAmount())).append("\n");
		sb.append("Shipping: ").append(String.format("%.2f", cart.getShipping().getShippingCost())).append("\n");
		sb.append("Order Total: ").append(String.format("%.2f", cart.getCartTotal()));
		return sb.toString();
	}

	/**
	 * Creates a string detailing information 
	 * about the users payment information
	 * @param payment
	 * @return
	 */
//	private String getTextPaymentInfo(PaymentVO payment) {
//		StringBuilder paymentInfo = new StringBuilder();
//		paymentInfo.append("Payment Information\n");
//		paymentInfo.append("Card Holder Name: ").append(payment.getPaymentName()).append("\n");
//		paymentInfo.append("Card Number: xxxx-xxxx-xxxx-").append(payment.getPaymentNumberSuffix()).append("\n");
//		paymentInfo.append("Expiration Date: ").append(payment.getExpirationMonth()).append("\n");
//		paymentInfo.append("/").append(payment.getExpirationYear()).append("\n");
//		paymentInfo.append("CVV Number: ").append(payment.getPaymentCode()).append("\n");
//		return paymentInfo.toString();
//	}

	/**
	 * Creates a string detailing information about
	 * the billing address provided by the user
	 * @param billingInfo
	 * @return
	 */
//	private String getTextBillingInfo(UserDataVO billingInfo) {
//		StringBuilder billing = new StringBuilder();
//		billing.append("Billing Information\n");
//		billing.append("First Name: ").append(billingInfo.getFirstName()).append("\n");
//		billing.append("Last Name: ").append(billingInfo.getLastName()).append("\n");
//		billing.append("Company: ").append(billingInfo.getAttributes().get("compnayNm")).append("\n");
//		billing.append("Billing Address: ").append(billingInfo.getAddress()).append("\n");
//		billing.append("Billing Address 2: ").append(billingInfo.getAddress2()).append("\n");
//		billing.append("City: ").append(billingInfo.getCity()).append("\n");
//		billing.append("State: ").append(billingInfo.getState()).append("\n");
//		billing.append("Zip Code: ").append(billingInfo.getZipCode()).append("\n");
//		billing.append("Billing Phone: ").append(billingInfo.getMainPhone()).append("\n");
//		return billing.toString();
//	}

	/**
	 * Creates a string detailing information
	 * about the costs of the user's order
	 * @param cart
	 * @return
	 */
	private String getTextShippingInfo(ShoppingCartVO cart, String franId) {
		StringBuilder shipping = new StringBuilder();
		Boolean freeShipping = true;
		shipping.append("Shipping Information\n");
		shipping.append("Sub Total: ").append(String.format("%.2f", cart.getShipping().getShippingCost())).append("\n");
		shipping.append("Shipping Option: ");
		
		// Check if we actually have a shipping option
		// or if the customer will pick the order up in person
		for (String key :cart.getShippingOptions().keySet()) {
			if(cart.getShipping().getShippingMethodId().equals(key))
				freeShipping = false;
		}
		if (freeShipping)
			shipping.append("Pickup At Store: 0.00").append("\n");
		else {
			shipping.append(cart.getShipping().getShippingMethodName()).append(": ");
			shipping.append(String.format("%.2f", cart.getShipping().getShippingCost())).append("\n");
		}
		double tax = cart.getTaxAmount() / cart.getSubTotal() * cart.getShipping().getShippingCost();
		shipping.append("Tax: ").append(String.format("%.2f", tax)).append("\n");
		shipping.append("Total: ");
		shipping.append(String.format("%.2f", cart.getSubTotal() + tax)).append("\n");
		return shipping.toString();
	}

	/**
	 * Creates the more detailed html body of the email
	 * @param cart
	 * @return
	 */
	private String getHtmlBody(ShoppingCartVO cart, String franId, String designatorNm) {
		StringBuilder body = new StringBuilder();
		body.append(getHtmlItemizedCart(cart));
		body.append("<div>");
		body.append(getHtmlCustomerInfo(cart.getShippingInfo()));
		//body.append(getHtmlPaymentInfo(cart.getPayment()));
		body.append("</div><div class='right' style='width: 45%;'>");
		//body.append(getHtmlBillingInfo(cart.getBillingInfo()));
		body.append(getHtmlShippingInfo(cart));
		body.append("</div>");
		body.append("<h1>Thank you for placing your order with <span  style='color:#1F497D'>").append(designatorNm != null ? designatorNm : "FASTSIGNS").append("</span>.</h1>");
		body.append("<h2>This is an automated message, <span style='color:#1F497D'>for questions please contact</span> ").append(franId).append("@fastsigns.com.</h2>");
		return body.toString();	
	}

	/**
	 * Creates the html table of user information
	 * @param user
	 * @return
	 */
	private String getHtmlCustomerInfo(UserDataVO user) {
		StringBuilder customer = new StringBuilder();
		customer.append("<table class='table table-bordered table-striped' id='shippingDetails'><h3 style='font-size:11.0pt;font-family:'Calibri','sans-serif';color:#1F497D'>Shipping Information</h3>");
		customer.append("<tbody><tr><td>First Name:</td><td style='color:#1F497D'>").append(user.getFirstName()).append("</td></tr>");
		customer.append("<tr><td>Last Name:</td><td style='color:#1F497D'>").append(user.getLastName()).append("</td></tr>");
		customer.append("<tr><td>Company:</td><td style='color:#1F497D'>").append(StringUtil.checkVal(user.getAttributes().get("companyNm"))).append("</td></tr>");
		customer.append("<tr><td>Billing Address:</td><td style='color:#1F497D'>").append(user.getAddress()).append("</td></tr>");
		customer.append("<tr><td>Billing Address 2:</td><td style='color:#1F497D'>").append(user.getAddress2()).append("</td></tr>");
		customer.append("<tr><td>City:</td><td style='color:#1F497D'>").append(user.getCity()).append("</td></tr>");
		customer.append("<tr><td>State:</td><td style='color:#1F497D'>").append(user.getState()).append("</td></tr>");
		customer.append("<tr><td>Zip Code:</td><td style='color:#1F497D'>").append(user.getZipCode()).append("</td></tr>");
		customer.append("<tr><td>Contact Phone:</td><td style='color:#1F497D'>").append(user.getMainPhone()).append("</td></tr>");
		customer.append("</tbody></table>");
		return customer.toString();
	}
	
	/**
	 * Creates the HTML table of the itemized cart.
	 * @param cart
	 * @return
	 */
	private String getHtmlItemizedCart(ShoppingCartVO cart) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2 style='color:#1F497D'>Thank you for your order</h2>");
		sb.append("<h3><p style='color:#1F497D'>Order Summary</p></h3>");
		sb.append("<table class='table table-bordered table-striped' id='cartDetails' style='width: 800px;'>");
		sb.append("<tr><th style='background:#B9BABE;padding:.75pt .75pt .75pt .75pt'>Name</th><th style='background:#B9BABE;padding:.75pt .75pt .75pt .75pt'>Price</th><th style='background:#B9BABE;padding:.75pt .75pt .75pt .75pt'>Quantity</th><th style='background:#B9BABE;padding:.75pt .75pt .75pt .75pt'>Total</th></tr>");
		for(ShoppingCartItemVO vo : cart.getItems().values()){
			ProductDetailVO p = (ProductDetailVO) vo.getProduct();
			sb.append("<tr><td style='background:#EBECEE;padding:.1in 4.8pt .1in 4.8pt'>").append(vo.getProductName()).append("<p style='margin: 0in;margin-bottom: .0001pt;font-size: 12.0pt;font-family: \"Times New Roman\",\"serif\";'><br/>Select a Size: ");
			sb.append(p.getDimensions());
			if(p.getModifiers() != null) {
				for(ModifierVO m : p.getModifiers().values()){
					sb.append("<br/>").append(m.getModifier_name()).append(": ");
					int i = 0;
					if(m.getAttributes() != null) {
						for(AttributeVO a : m.getAttributes().values()) {
							if(i > 0) sb.append(" , ");
							sb.append(a.getAttribute_name()).append(" - ");
							if(a.getOptions() != null) {
								for(OptionVO o : a.getOptions().values()){
									sb.append(o.getOption_name());
								}
							}
							i++;
						}
					}
				}
			}
			sb.append("</p></td><td style='background:#EBECEE;padding:.1in 4.8pt .1in 4.8pt'>").append(String.format("%.2f", p.getMsrpCostNo())).append("</td><td style='background:#EBECEE;padding:.1in 4.8pt .1in 4.8pt'>").append(vo.getQuantity());
			sb.append("</td><td style='background:#EBECEE;padding:.1in 4.8pt .1in 4.8pt'>").append(String.format("%.2f", (p.getMsrpCostNo() * vo.getQuantity()) - p.getDiscount())).append("</td></tr>");
		}
		sb.append("<tr><td class='hide'></td><td colspan='2' align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>Subtitle:</strong></td><td align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>").append(String.format("%.2f", cart.getSubTotal())).append("</strong></td></tr>");
		sb.append("<tr><td class='hide'></td><td colspan='2' align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>Tax:</strong></td><td align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>").append(String.format("%.2f", cart.getTaxAmount())).append("</strong></td></tr>");
		sb.append("<tr><td class='hide'></td><td colspan='2' align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>Shipping:</strong></td><td align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>").append(String.format("%.2f", cart.getShipping().getShippingCost())).append("</strong></td></tr>");
		sb.append("<tr><td class='hide'></td><td colspan='2' align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>Order Total:</strong></td><td align='right' style='background:#DDE2E6;padding:.75pt .75pt .75pt .75pt; text-align: right;'><strong>").append(String.format("%.2f", cart.getCartTotal())).append("</strong></td></tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Creates the html table of payment information
	 * @param payment
	 * @return
	 */
//	private String getHtmlPaymentInfo(PaymentVO payment) {
//		StringBuilder paymentInfo = new StringBuilder();
//		paymentInfo.append("<h3>Payment Information</h3>");
//		paymentInfo.append("<table class='table table-bordered table-striped' id='cardDetails'><tbody>");
//		paymentInfo.append("<tr><td>Card Holder Name</td><td>").append(payment.getPaymentName()).append("</td></tr>");
//		paymentInfo.append("<tr><td>Card Number</td><td>xxxx-xxxx-xxxx-").append(payment.getPaymentNumberSuffix()).append("</td></tr>");
//		paymentInfo.append("<tr><td>Expiration Date</td><td>").append(payment.getExpirationMonth());
//		paymentInfo.append("&nbsp;/&nbsp;").append(payment.getExpirationYear()).append("</td></tr>");
//		paymentInfo.append("<tr><td>CVV Number</td><td>").append(payment.getPaymentCode()).append("</td></tr>");
//		paymentInfo.append("<tbody></table>");
//		return paymentInfo.toString();
//	}
	
	/**
	 * Creates the html table of billing information
	 * @param billingInfo
	 * @return
	 */
//	private String getHtmlBillingInfo(UserDataVO billingInfo) {
//		StringBuilder billing = new StringBuilder();
//		billing.append("<h3>Billing Information</h3><table class='table table-bordered table-striped' id='billingDetails'><tbody>");
//		billing.append("<tr><td>First Name</td><td>").append(billingInfo.getFirstName()).append("</td></tr>");
//		billing.append("<tr><td>Last Name</td><td>").append(billingInfo.getLastName()).append("</td></tr>");
//		billing.append("<tr><td>Company</td><td>").append(billingInfo.getAttributes().get("compnayNm")).append("</td></tr>");
//		billing.append("<tr><td>Billing Address</td><td>").append(billingInfo.getAddress()).append("</td></tr>");
//		billing.append("<tr><td>Billing Address 2</td><td >").append(billingInfo.getAddress2()).append("</td></tr>");
//		billing.append("<tr><td>City</td><td>").append(billingInfo.getCity()).append("</td></tr>");
//		billing.append("<tr><td>State</td><td>").append(billingInfo.getState()).append("</td></tr>");
//		billing.append("<tr><td>Zip Code</td><td>").append(billingInfo.getZipCode()).append("</td></tr>");
//		billing.append("<tr><td>Billing Phone</td><td>").append(billingInfo.getMainPhone()).append("</td></tr>");
//		billing.append("</tbody></table>");
//		return billing.toString();
//	}
	
	/**
	 * Creates the html table of shipping information
	 * @param cart
	 * @return
	 */
	private String getHtmlShippingInfo(ShoppingCartVO cart) {
		StringBuilder shipping = new StringBuilder();
		Boolean freeShipping = true;
		shipping.append("<h3>Shipping Information</h3><table class='table table-bordered table-striped' id='cardDetails'><tbody>");
		shipping.append("<tr><td>Sub Total</td><td><strong id='subTotal'>").append(String.format("%.2f", cart.getShipping().getShippingCost())).append("</strong></td></tr>");
		shipping.append("<tr><td>Shipping Option</td><td>");
		
		// Check if we actually have a shipping option
		// or if the customer will pick the order up in person
		for (String key :cart.getShippingOptions().keySet()) {
			if(cart.getShipping().getShippingMethodId().equals(key))
				freeShipping = false;
		}
		if (freeShipping)
			shipping.append("<strong>Pickup At Store ($0.00)</strong>");
		else {
			shipping.append("<strong>").append(cart.getShipping().getShippingMethodName()).append(": ");
			shipping.append(String.format("%.2f", cart.getShipping().getShippingCost())).append("</strong>");
		}
		//double tax = cart.getTaxAmount() / cart.getSubTotal() * cart.getShipping().getShippingCost();

		shipping.append("</td></tr>");
		//shipping.append("<tr><td>Tax</td><td><strong id='taxTotal'>").append(String.format("%.2f", tax)).append("</strong></td></tr>");
		shipping.append("<tr><td>Total</td><td><strong id='costTotal'>");
		shipping.append(String.format("%.2f", cart.getShipping().getShippingCost()));
		shipping.append("</strong></td></tr><tbody></table>");
		return shipping.toString();
	}
	
}
