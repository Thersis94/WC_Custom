package com.gotimefitness.action;

import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> ShoppingCartAction.java<br/>
 * <b>Description:</b> Manages the shopping cart interactions for the website.  
 * Note:  Does NOT manage the checkout/purchase process. 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Nov 27, 2017
 ****************************************************************************/
public class ShoppingCartAction extends SimpleActionAdapter {

	private Storage cartStorage;

	public ShoppingCartAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ShoppingCartAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ShoppingCartVO cart = loadCart(req);
		putModuleData(cart);
	}


	/**
	 * creates a purchasable product using the given parameters and adds it to the user's shopping cart.
	 * @param productId
	 * @param name
	 * @param desc
	 * @param qnty
	 * @param basePrice
	 * @throws ActionException 
	 */
	protected void addToCart(ActionRequest req, String productId, String name, String desc, 
			int qnty, double basePrice) throws ActionException {
		ShoppingCartItemVO item = new ShoppingCartItemVO();
		item.setProductId(productId);
		item.setBasePrice(basePrice);
		item.setProductName(name);
		item.setQuantity(qnty);
		item.setDescription(desc);

		ShoppingCartVO cart = loadCart(req);
		cart.add(item);
		saveCart(cart);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ShoppingCartVO cart = loadCart(req);

		if ("quantity".equals(req.getParameter("action"))) {
			updateQuantity(req, cart);
			saveCart(cart);
		}

		redirectUser(req);
	}


	/**
	 * redirec the user after a build operation.  This could be expanded to include a confirmation message.
	 * @param req
	 */
	private void redirectUser(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		sendRedirect(page.getFullPath(), null, req);
	}


	/**
	 * iterates the request and updates the item quantities in the cart.
	 * @param req
	 * @param cart
	 */
	private void updateQuantity(ActionRequest req, ShoppingCartVO cart) {
		for (Map.Entry<String, String[]> entry: req.getParameterMap().entrySet()) {
			//ignore everything but legitimate quantities
			if (!entry.getKey().startsWith("qnty_") || entry.getValue() == null || entry.getValue().length == 0)  continue;

			String prodId = entry.getKey().substring(5);
			Integer qnty = Convert.formatInteger(entry.getValue()[0]);
			if (qnty == null || qnty < 1) {
				cart.remove(prodId);
			} else {
				cart.updateQuantity(prodId, qnty);
			}
			log.debug("updated " + prodId + " to qnty=" + qnty);
		}
	}


	/**
	 * Loads & returns the shopping cart after loading the Storage mechanism. 
	 * @return
	 * @throws ActionException 
	 */
	protected ShoppingCartVO loadCart(ActionRequest req) throws ActionException {
		if (cartStorage == null) {
			try {
				setAttribute(GlobalConfig.HTTP_REQUEST, req);
				cartStorage = StorageFactory.getDefaultInstance(getAttributes());
			} catch (ApplicationException ae) {
				log.error("could not init cart storage", ae);
				throw new ActionException(ae);
			}
		}
		return cartStorage.load();
	}


	/**
	 * simple abstraction to save the cart using the Storage
	 * @param cart
	 * @throws ActionException 
	 */
	protected void saveCart(ShoppingCartVO cart) throws ActionException {
		if (cartStorage != null) {
			cartStorage.save(cart);
		} else {
			throw new ActionException("Cart wasn't loaded properly.  Call loadCart(req) first.");
		}
	}
}