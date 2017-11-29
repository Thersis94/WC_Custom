package com.gotimefitness.action;

import com.mindbody.MindBodySaleApi;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.sales.MindBodyCheckoutShoppingCartConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> EcommerceAction.java<br/>
 * <b>Description:</b> Manages the checkout/purchase process.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Nov 28, 2017
 ****************************************************************************/
public class EcommerceAction extends SimpleActionAdapter {

	ShoppingCartAction sca;

	public EcommerceAction() {
		super();
		sca = new ShoppingCartAction();
	}

	/**
	 * @param arg0
	 */
	public EcommerceAction(ActionInitVO arg0) {
		super(arg0);
		sca = new ShoppingCartAction();
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


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ShoppingCartVO cart = loadCart(req);
		String msg = "Your order was submitted successfully.  Thank you.";

		try {
			populateCart(req, cart);
			sendOrderToMB(req, cart);

			cart.flush();
			saveCart(cart);
			redirectUser(req, msg);
		} catch (Exception e) {
			log.error("could not submit order", e);
			//failures get forwarded back to the form for resubmission - load the cart
			retrieve(req);
			req.setAttribute("msg", e.getMessage());
		}
	}


	/**
	 * takes the data off the request and injects it into the cart (order) for processing.
	 * @param req
	 * @param cart
	 */
	private void populateCart(ActionRequest req, ShoppingCartVO cart) {
		UserDataVO bUser = new UserDataVO(req);
		bUser.setName(req.getParameter("combinedName"));
		cart.setBillingInfo(bUser);
		
		PaymentVO pmt = new PaymentVO((String)getAttribute(Constants.ENCRYPT_KEY));
		pmt.setPaymentNumber(req.getParameter("ccNumber"));
		pmt.setPaymentCode(req.getParameter("cvvNumber"));
		pmt.setExpirationMonth(req.getParameter("ccExpMo"));
		pmt.setExpirationYear(req.getParameter("ccExpYr"));
		cart.setPayment(pmt);
	}

	
	/**
	 * send the order to MindBody using their SOAP API.  This is a multi-step transaction.
	 * @param req
	 * @param cart
	 * @throws ActionException 
	 */
	private void sendOrderToMB(ActionRequest req, ShoppingCartVO cart) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		MindBodyCredentialVO srcCreds = MindBodyUtil.buildSourceCredentials(site.getSiteConfig());
		MindBodyCredentialVO staffCreds = MindBodyUtil.buildStaffCredentials(site.getSiteConfig());

		UserDataVO vo = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyCheckoutShoppingCartConfig config = new MindBodyCheckoutShoppingCartConfig(srcCreds, staffCreds);
		config.setCart(cart);
		config.setClientId(vo.getProfileId());

		MindBodySaleApi api = new MindBodySaleApi();
		MindBodyResponseVO resp = api.getDocument(config);
		if (resp == null || resp.getErrorCode() != 200)
			throw new ActionException(resp != null ? resp.getMessage() : (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE));
	}


	/**
	 * redirec the user after a build operation.  This could be expanded to include a confirmation message.
	 * @param req
	 */
	private void redirectUser(ActionRequest req, String msg) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		sendRedirect(page.getFullPath(), msg, req);
	}


	/**
	 * Loads & returns the shopping cart after loading the Storage mechanism. 
	 * @return
	 * @throws ActionException 
	 */
	protected ShoppingCartVO loadCart(ActionRequest req) throws ActionException {
		sca.setAttributes(getAttributes());
		return sca.loadCart(req);
	}


	/**
	 * simple abstraction to save the cart using the Storage
	 * @param cart
	 * @throws ActionException 
	 */
	protected void saveCart(ShoppingCartVO cart) throws ActionException {
		sca.saveCart(cart);
	}
}