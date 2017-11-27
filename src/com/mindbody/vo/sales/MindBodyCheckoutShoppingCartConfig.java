package com.mindbody.vo.sales;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodySaleApi.SaleDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.payment.PaymentVO;

/****************************************************************************
 * <b>Title:</b> MindBodyCheckoutShoppingCartConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage  config for CheckoutShoppingCart Endpoint
 * TODO - Config needs built out as necessary to support use case.  Total
 * Endpoint compliance is probably not feasable or worth doing at this time.
 *
 * https://developers.mindbodyonline.com/Documentation/SaleService?version=v5.1#checkoutshoppingcart
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyCheckoutShoppingCartConfig extends MindBodySalesConfig {

	private String clientId;
	private boolean test;
	private List<ShoppingCartItemVO> cartItems;
	private List<PaymentVO> payments;
	private String cartId;
	private boolean inStore;
	private String promotionCode;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyCheckoutShoppingCartConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(SaleDocumentType.CHECKOUT_SHOPPING_CART, source, user);
		cartItems = new ArrayList<>();
		payments = new ArrayList<>();
	}

	@Override
	public boolean isValid() {
		boolean isValid = super.isValid() && clientId != null;
		isValid = isValid && !cartItems.isEmpty() && !payments.isEmpty();

		return isValid;
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @return the test
	 */
	public boolean isTest() {
		return test;
	}

	/**
	 * @return the cartItems
	 */
	public List<ShoppingCartItemVO> getCartItems() {
		return cartItems;
	}

	/**
	 * @return the payments
	 */
	public List<PaymentVO> getPayments() {
		return payments;
	}

	/**
	 * @return the cartId
	 */
	public String getCartId() {
		return cartId;
	}

	/**
	 * @return the inStore
	 */
	public boolean isInStore() {
		return inStore;
	}

	/**
	 * @return the promotionCode
	 */
	public String getPromotionCode() {
		return promotionCode;
	}

	/**
	 * @param clientId the clientId to set.
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @param test the test to set.
	 */
	public void setTest(boolean test) {
		this.test = test;
	}

	/**
	 * @param cartItems the cartItems to set.
	 */
	public void setCartItems(List<ShoppingCartItemVO> cartItems) {
		this.cartItems = cartItems;
	}

	public void addCartItem(ShoppingCartItemVO item) {
		cartItems.add(item);
	}

	/**
	 * @param payments the payments to set.
	 */
	public void setPayments(List<PaymentVO> payments) {
		this.payments = payments;
	}

	public void addPayment(PaymentVO p) {
		payments.add(p);
	}

	/**
	 * @param cartId the cartId to set.
	 */
	public void setCartId(String cartId) {
		this.cartId = cartId;
	}

	/**
	 * @param inStore the inStore to set.
	 */
	public void setInStore(boolean inStore) {
		this.inStore = inStore;
	}

	/**
	 * @param promotionCode the promotionCode to set.
	 */
	public void setPromotionCode(String promotionCode) {
		this.promotionCode = promotionCode;
	}
}