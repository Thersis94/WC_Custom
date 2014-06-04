package com.fastsigns.product.keystone;


import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ProductDetailAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 9, 2012
 ****************************************************************************/
public class ProductDetailAction extends AbstractBaseAction {

	public ProductDetailAction() {
	}

	public ProductDetailAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
//		if (req.hasParameter("pricing") && !req.hasParameter("pricingComplete")) {
//			req.setParameter("pricingComplete", "true");
//			ShoppingCartAction sca = new ShoppingCartAction(actionInit);
//			sca.setAttributes(attributes);
//			sca.setDBConnection(dbConn);
//			sca.build(req);
			
//		} else {
			//Use Cached action and set necessary pieces for cache groups to be used. 
			attributes.put(Constants.SITE_DATA, req.getAttribute(Constants.SITE_DATA));
			attributes.put("wcFranchiseId", CenterPageAction.getFranchiseId(req));
			KeystoneProxy proxy = KeystoneProxy.newInstance(attributes);
			proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
			proxy.setModule("products");
			proxy.setAction("getProductDetails");
			proxy.addPostData("productId", req.getParameter("product"));
			proxy.setParserType(KeystoneDataParser.DataParserType.ProductDetail);
			
			if (req.hasParameter("material")) {
				proxy.addPostData("material", "true");
			} else {
				proxy.addPostData("usageId", req.getParameter("usageId")); //categoryId
			}
			
			try {
				//call the proxy
				mod.setActionData(proxy.getData().getActionData());
				
			} catch (Exception e) {
				log.error("could not load product using id=" + req.getParameter("product"), e);
				mod.setError(e);
				mod.setErrorMessage("Unable to load Product Details");
			}
			
			// I believe this code is for re-loading the DSOL page (for edits).  //comment added by speculation -JM 06.03.14
			if (req.hasParameter("itemId")) {
				log.debug("loading itemId=" + req.getParameter("itemId"));
				ShoppingCartAction sca = new ShoppingCartAction(this.actionInit);
				sca.setDBConnection(dbConn);
				sca.setAttributes(attributes);
				Storage container = sca.loadCartStorage(req);
				ShoppingCartVO cart = container.load();
				ShoppingCartItemVO vo = cart.getItems().get(req.getParameter("itemId"));
				if (vo != null)
					req.setAttribute("cartItem", vo);
			}
			
			//set APIKey for the browser to use to call for pricing.
			req.setAttribute("keystoneApiKey", proxy.buildApiKey());
			setAttribute(Constants.MODULE_DATA, mod);
//		}
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		//ShoppingCartAction facades all cart-related activities.
		//ShoppingCartAction will call Keystone for pricing, on our behalf (via PricingUtil).
		//the user's ShoppingCartVO will be put into ModuleVO for us to use when we get to the View (here)
		log.debug("hasAttributes=" + req.hasParameter("attributes"));
		if (req.hasParameter("attributes")) {
			retrieve(req);
		} else {
			ShoppingCartAction sca = new ShoppingCartAction(actionInit);
			sca.setAttributes(attributes);
			sca.setDBConnection(dbConn);
			sca.build(req);
		}
		
		//if this is NOT a pricing or product attributes call, that means it's an 'add to cart' request.
		//in that scenario we want to redirec the user to their shopping cart.
		//ShoppingCartAction already built the URL for us, we just need to prefix it with
		//the URL of the shopping cart page, instead of 'this' page.
		if (!req.hasParameter("pricing") && !req.hasParameter("attributes")) {
			//wrongUrl was set by the ShoppingCartAction, and points to 'this' page.
			//we want the user to go to the '/cart' page instead.
			String wrongUrl = req.getAttribute(Constants.REDIRECT_URL).toString();
			ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
			
			StringBuilder goodUrl = new StringBuilder();
			goodUrl.append(mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			goodUrl.append(wrongUrl.substring(wrongUrl.indexOf("?")));
			
			req.setAttribute(Constants.REDIRECT_URL, goodUrl.toString());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
	}

}
