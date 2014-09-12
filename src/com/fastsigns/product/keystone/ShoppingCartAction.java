package com.fastsigns.product.keystone;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.checkout.CheckoutReportUtil;
import com.fastsigns.product.keystone.checkout.CheckoutUtil;
import com.fastsigns.product.keystone.vo.ModifierVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO.OptionVO;
import com.fastsigns.product.keystone.vo.ProductDetailVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.commerce.cart.storage.StorageFactory;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
//import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
//import com.sun.org.apache.xml.internal.security.utils.Base64;

/****************************************************************************
 * <b>Title</b>: ShoppingCartAction.java<p/>
 * <b>Description: Handles adding, updating, and removing items from the shopping cart.
 * Also proxies calls for pricing inquries, since an items cost is impacted by what else
 * may live in their order.  While pricing inquires leverage the cart stored in the user's
 * session, pricing calls (alone) do not affect the cart.  (The changes made to the cart
 * are not stored and therefore do not persist.)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 8, 2012
 ****************************************************************************/
public class ShoppingCartAction extends SimpleActionAdapter {

	public ShoppingCartAction() {
	}
	
	public ShoppingCartAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/**
	 * internal method for updating/removing items from the shopping cart
	 */
	protected ShoppingCartVO updateCart(SMTServletRequest req, ShoppingCartVO cart) 
	throws ActionException {
		log.info("updating shopping cart");
		
		for (String key : req.getParameterMap().keySet()) {
			if (key.startsWith("qnty_")) {
				String prodId = key.substring(key.indexOf("qnty_")+5);
				log.debug("found prodId " + prodId);
				int val = Convert.formatInteger(req.getParameter(key), 0);
				
				if (val == 0 || req.hasParameter("delProd_" + prodId)) {
					//remove this product from the cart
					log.debug("removing product " + prodId);
					cart.remove(prodId);
				} else {
					//update the quantity for this product
					log.debug("updating " + prodId + " to qnty=" + val);
					cart.updateQuantity(prodId, val);
				}
			}
		}
		
		//We're changing the state of the cart so flush the jobId attached to it.
		flushCartJobData(cart, req);
		return cart;
	}
	
	/**
	 * We need a way to ensure that a job thats been submitted does not get resubmitted
	 * with different products or shipping.  We flush the jobId to ensure we place a new
	 * order with the updated data when this occurs.  We also flush the shipping options
	 * because when the cart changes you get different types of shipping.
	 * @param cart
	 * @param req
	 */
	private void flushCartJobData(ShoppingCartVO cart, SMTServletRequest req) {
		cart.setShippingOptions(new HashMap<String, ShippingInfoVO>());
		req.getSession().setAttribute("jobId", "");
		cart.setErrors(new HashMap<String, String>());		
	}

	/**
	 * adds an item to the cart.  Notice we're not saving the cart at this step, 
	 * just adding an item to it.
	 * @param req
	 * @param cart
	 * @return
	 * @throws ActionException
	 */
	protected ShoppingCartVO addToCart(SMTServletRequest req, ShoppingCartVO cart) 
	throws ActionException, InvalidDataException {
		log.info("adding product to cart");
		
		//get the ProductVO directly from Keystone
		ProductDetailVO prod = loadProduct(req);
		
		//preserve the webId and franchiseAliasPath.
		//These are necessary evils so this action can send users back to the Franchise's website.  :(
		prod.setWebId((String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID));
		prod.setFranchiseAliasId((String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ALIAS_PATH));
		prod.setUsageId(req.getParameter("usageId"));
		prod.setCatalogId(req.getParameter("catalog"));
		
		//if we're really adding this to the cart, change the productId that identifies
		//this item in the cart (uniquely).
		String oldProductId = prod.getProductId();
		String newProductId = prod.getProductId();
		if (!req.hasParameter("pricing")) {
			if(StringUtil.checkVal(req.getParameter("itemId")).length() > 0)
				newProductId = req.getParameter("itemId");
			else
				newProductId = new UUIDGenerator().getUUID();
			
			//iterate the cart; we cannot save products from different franchises!
			for (ShoppingCartItemVO vo : cart.getItems().values()) {
				ProductDetailVO prodTmp  = (ProductDetailVO) vo.getProduct();
				log.debug("adding=" + prod.getFranchise_id() + " existing=" + prodTmp.getFranchise_id());
				if (!prod.getFranchise_id().equals("0") && !prod.getFranchise_id().equals(prodTmp.getFranchise_id())) {
					throw new InvalidDataException("You cannot add products from multiple franchises to your cart, please empty your cart before adding this product.");
				}
			}

			//We're changing the state of the cart so flush the jobId attached to it.
			flushCartJobData(cart, req);
		}
		prod.addProdAttribute("oldProductId", oldProductId);
		//transpose the ProductVO into a generic type
		ShoppingCartItemVO vo = new ShoppingCartItemVO(prod);
		vo.setProductId(newProductId);
		vo.setProductName(prod.getDisplay_name());
		vo.setProductCategory(prod.getDimensions());
		vo.setDescription(prod.getWeb_description());
		vo.setQuantity(Convert.formatInteger(req.getParameter("quantity"), 1));
		vo.setBasePrice(prod.getMsrpCostNo());
		cart.add(vo);
		
		return cart;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 * 
	 * The build method here handles modifying the contents of the cart, or adding
	 * new items to it.  It finishes with a write transaction to the cart Container
	 * followed by browser redirection the shopping cart page. (a clean call to this action's retrieve method)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.info("managing shopping cart");
		Storage container = loadCartStorage(req);
		ShoppingCartVO cart = getCartData(container, req);
		String msg = "";
		String nextStep = null;
				
		//"step" defines a phase of the checkout process; 
		//lump them together and handle in a separate object.
		if (req.hasParameter("step")) {
			//these are "checkout" screens, not cart-mgmt related
			CheckoutUtil checkout = new CheckoutUtil(attributes);
			try {
				cart = checkout.build(req, cart);
				nextStep = (String) getAttribute("nextStep");
				/*
				 * If we have successfully placed and order, try to update the object_stor row with the job_id
				 * we received from keystone to make lookup easier.  Also add profileId so we know who
				 * these are tied to.
				 */
				//TODO should be in a sub-method, not here!
				if (nextStep != null && StringUtil.checkVal(cart.getErrors().get("jobId")).length() > 0) {
					String objectId = StringUtil.checkVal(req.getSession().getAttribute(Storage.CART_OBJ));
					String jobId = cart.getErrors().get("jobId");
					StringBuilder sb = new StringBuilder();
					sb.append("update object_stor set object_id=?, update_dt=?, profile_id=? where object_id=? ");

					// Send a message summary of the order to the user who placed it If payment succeeds. 
					if (nextStep.equals("complete") && Convert.formatBoolean(cart.getErrors().get("success"))) {
						String designatorNm = null;
						String webId = CenterPageAction.getFranchiseId(req);
						try { //don't risk the emailing failing over this NPE...
								FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
								designatorNm = sessVo.getFranchise(webId).getLocationName();
						} catch (Exception e) { }
						
						CheckoutReportUtil util = new CheckoutReportUtil(attributes, dbConn);
						util.sendSummary(cart, webId, designatorNm);
					}
					PreparedStatement ps = null;
					try {
						ps = dbConn.prepareStatement(sb.toString());
						ps.setString(1, jobId);
						ps.setTimestamp(2, Convert.getCurrentTimestamp());
						ps.setString(3, cart.getBillingInfo().getProfileId());
						ps.setString(4, objectId);
						ps.execute();
						log.debug("update obj_id=" + objectId + " to job_id=" + jobId);
						container.flush();
					} catch (Exception e) {
						log.error("Could not update shopping cart storage.", e);
					} finally {
						try { ps.close(); } catch(Exception e) { }
					}
				}
				
			} catch (Exception ae) {
				msg = ae.getMessage();
				//log everything but duplicate account issues
				if (!"user name already exists".equalsIgnoreCase(msg))
					log.error("error during checkout-build", ae);

				nextStep = req.getParameter("step");
			}
			
		} else if (req.hasParameter("update")) {
			//update cart quantities and deletions
			cart = updateCart(req, cart);
			
		} else {
			//add a product to the cart
			
			//if this is a pricing call, we want to work with a COPY of the cart, not the actual one
			if (req.hasParameter("pricing")) {
				//ensure we don't have the temp shipping vo.
				cart.remove("shipping");
				log.info("cloning shopping cart");
				//clone the cart, we only care about the attributes pertinant to pricing
				ShoppingCartVO cart2 = new ShoppingCartVO();
				for (ShoppingCartItemVO vo : cart.getItems().values()) {
					ShoppingCartItemVO newVo;
					try {
						newVo = vo.clone();
					} catch (CloneNotSupportedException e) {
						log.error(e);
						newVo = new ShoppingCartItemVO();
					}
					newVo.setProduct(vo.getProduct());
					cart2.add(newVo);
				}
				cart = cart2;
			}
			
			try {
				cart = addToCart(req, cart);
			} catch (InvalidDataException ide) {
				log.error(ide);
				msg = ide.getMessage();
			}
		}

		//do not recalculate pricing on empty carts or during the checkout process
		if (cart.getSize() > 0 && !req.hasParameter("step"))
			cart = this.recalcPricing(req, cart);
		
		
		//if this is NOT a call for pricing, write the cart to persistent storage
		//pricing calls will not do this; they're merely inquisitive
		if (!req.hasParameter("pricing")) {
			//save the cart to the Storage container
			try {
				container.save(cart);
			} catch (Exception ae) {
				log.error("could not save cart", ae);
			}
		
			//redirect the browser back to the page
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder(page.getFullPath());
			url.append("?msg=" + msg);
			if (req.hasParameter("catalog")) url.append("&catalog=").append(req.getParameter("catalog"));
			if (req.hasParameter("category")) url.append("&category=").append(req.getParameter("category"));
			//nextStep gets set by the CheckoutUtil
			if (nextStep != null) url.append("&step=").append(nextStep);
			if(req.hasParameter("startOver")) {	
				container.flush();
				//need to flush the jobId in the event they've submitted but are dropping the cart.
				req.removeAttribute("jobId");
				String storeAlias = StringUtil.checkVal(req.getSession().getAttribute(FastsignsSessVO.ECOM_ALIAS_PATH));
				if (storeAlias.length() == 0) storeAlias = CenterPageAction.getFranchiseId(req);
				
				req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
				req.setAttribute(Constants.REDIRECT_URL, "/" + storeAlias + "/store");
			} else {
				req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
				req.setAttribute(Constants.REDIRECT_URL, url.toString());
			}
		} else {
			//set the cart into a retrievable location for the invoking class to use.
			//at this point no changes to the cart have been made that affect the order.  (We did not write any changes to the Container)
			if ("shippingtax".equals(req.getParameter("step"))) {
					//save the cart to the Storage container of we've added/changed shipping method
					//this affects tax amounts, so we need to caputure it server-side where the end user can't manipulate it. (server-side)
					try {
						container.save(cart);
					} catch (Exception ae) {
						log.error("could not save cart", ae);
					}
			}
			validateCart(req, cart);
			super.putModuleData(cart);
		}
	}

	/**
	 * Determine if the cart item passes validation.  For now we are only checking
	 * weight restrictions, however this can be used to do any other checks
	 * required for Validation.
	 * @param cart
	 * @return
	 */
	private void validateCart(SMTServletRequest req, ShoppingCartVO cart) {
		//Map<String, String> errors = new HashMap<String, String>();
		/*
		for(ShoppingCartItemVO vo : cart.getItems().values()){
			KeystoneProductVO prod = (KeystoneProductVO)vo.getProduct();
			double weight = prod.getWeight() * prod.getSizes().get(0).getSquareInches() * vo.getQuantity();
			log.debug(weight);
			//TODO fix shipping caps
			//if (weight > 150)
			//	errors.put(vo.getProductId(), "Package exceeds weight limits, please lower quantity and try again.");
		}
		*/
		//if(errors.size() > 0)
		//	req.setAttribute("errors", errors);
	}

	private ShoppingCartVO recalcPricing(SMTServletRequest req, ShoppingCartVO cart) {
		//call PricingAction and recalculate costs
		PricingUtil util = new PricingUtil(attributes);
		cart = util.loadPricing(req, cart);
		util = null;
		
		return cart;
	}
	
	/**
	 * We need to ensure that when we reload a cart for reorder that we clear out
	 * the errors map for it.  If we don't then we'll have problems with future
	 * orders not saving to the correct job.
	 */
	private ShoppingCartVO getCartData(Storage s, SMTServletRequest req) {
		boolean clearErrors = req.hasParameter("job_id");
		ShoppingCartVO cart = null;
		try {
			cart = s.load();
			if(clearErrors) {
				flushCartJobData(cart, req);
			}
		} catch (Exception e) {
			log.error(e);
		}
		return cart;
		
		
	}

	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.info("loading cart object for display");
		Storage container = loadCartStorage(req);
		ShoppingCartVO cart = getCartData(container, req);
		String step = req.getParameter("step");
		boolean stepExists = req.hasParameter("step");
		
		if (stepExists && !"checkout".equals(step)) {
			//these are "checkout" screens, not cart-mgmt related
			CheckoutUtil checkout = new CheckoutUtil(attributes);
			try {
				cart = checkout.retrieve(req, cart);
			} catch (Exception ae) {
				log.error("error during checkout-Retrieve", ae);
			}
		}
		validateCart(req, cart);
		super.putModuleData(cart);
		container.save(cart);
		
		//If we successfully got to the review screen, flush the session container.
		if (stepExists && "complete".equalsIgnoreCase(step))
			container.flush();
		
		return;
	}
	
	
	/**
	 * calls the shopping cart API to get the cart's Storage mechanism
	 * @param req
	 * @return
	 */
	protected Storage loadCartStorage(SMTServletRequest req) throws ActionException {
		//build a Map of attributes to provide to our AbstractCartController
		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put(GlobalConfig.HTTP_RESPONSE, attributes.get(GlobalConfig.HTTP_RESPONSE));
		attrs.put(GlobalConfig.HTTP_REQUEST, req);
		attrs.put(GlobalConfig.KEY_DB_CONN, dbConn);

		//TODO this code does not belong here.
		if (req.hasParameter("job_id")) {
			try {
				cloneCart(req, attrs, req.getParameter("job_id"));
			} catch (Exception e) {
				log.error("unable to clone shopping cart storage.", e);
				String redir = "/" + CenterPageAction.getFranchiseId(req) + "/store?display=orders";
				super.sendRedirect(redir, "Cart could not be loaded.  No order information was found.", req);
			}
		}
        
		// Load the cart from our Storage medium.
        Storage container = null;
		try {
			container = StorageFactory.getInstance(StorageFactory.PERSISTENT_STORAGE, attrs);
		} catch (Exception e) {
			throw new ActionException(e);
		}
        
        return container;
	}
	

	//TODO the task of cloning is the responsabiltiy of the Storage API; add clone() to the interface if necessary
	private void cloneCart(SMTServletRequest req, Map<String, Object> attrs, String jobId) throws SQLException {
		//clone row in Object_stor table that matches job_id and replace current cart with it.
		StringBuilder sql = new StringBuilder();
		sql.append("insert into OBJECT_STOR (OBJECT_ID, OBJECT, CREATE_DT) select ");
		sql.append("? , OBJECT, getDate() from OBJECT_STOR where OBJECT_ID = ?");
		String guid = new UUIDGenerator().getUUID();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, guid);
			ps.setString(2, jobId);
			
			if (ps.executeUpdate() == 0) 
				throw new SQLException("Order does not exist.");
			
			req.getSession().setAttribute(Storage.CART_OBJ, guid);
			super.sendRedirect("/cart", "Cart successfully reloaded", req);
			
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		
	}
	
	/**
	 * call the ProductAction to get the complete ProductVO, so we can add it to the
	 * cart. We do not entrust certain values on the request because a user could
	 * mutate them.
	 */
	protected ProductDetailVO loadProduct(SMTServletRequest req) throws ActionException {
		ProductDetailVO prod = null;
		
		SMTActionInterface ai = new ProductDetailAction(actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		prod = (ProductDetailVO) mod.getActionData();
		ProductDetailVO prod2 = null;
		try {
			prod2 = prod.clone();
		} catch (CloneNotSupportedException e) {
			log.debug("Clone of Product Failed", e);
		}
		if (prod2 == null) throw new ActionException("Product Not Found");
		req.setValidateInput(Boolean.FALSE);

		//add the modifiers and customizations to the product (object) before we commit it to the cart
		this.customizeProduct(prod2, req);
		
		//If this is a DSOL call, make sure to assign the custom data attached.
		if(Convert.formatBoolean(req.getParameter("isDsol")) && req.hasParameter("highResData")){
				Map<String, Object> attr = prod2.getProdAttributes();
				KeystoneProductVO vo = (KeystoneProductVO) req.getSession().getAttribute("DSOLVO");
				
				//Move attributes from dsol session variable to the cart product variable.
				prod2.setDescription(vo.getDescription());
				prod2.setWeb_description(vo.getDescription());
				prod2.setDisplay_name(vo.getProductName());
				prod2.setCatalogId(vo.getCatalogId());
				prod2.setImageThumbUrl(vo.getImageThumbUrl());
				prod2.setImageUrl(vo.getImageUrl());
				prod2.setProdAttributes(vo.getProdAttributes());
				if(req.hasParameter("materialName"))
					attr.put("materialName", req.getParameter("materialName"));
		}
		req.setValidateInput(Boolean.TRUE);

		log.debug("loadedProduct=" + prod2);
		return prod2;
	}
	
	
	/**
	 * Parse through the modifiers/dimensions passed in on the html form and insert them into the
	 * productVO we're about to save in the cart.
	 * @param prod
	 * @param req
	 * @return
	 */
	private ProductDetailVO customizeProduct(ProductDetailVO prod, SMTServletRequest req) {
		//flush the modifiers already on the product, we only want to preserve what is being purchased.
		Map<String, ModifierVO> mods = prod.getModifiers();
		prod.setModifiers(null);
		
		Enumeration<?> params = req.getParameterNames();
		while (params.hasMoreElements()) {
			String param = (String) params.nextElement();
			
			//skip anything that's not a modifier, and modifiers not chosen by the user
			if (!param.startsWith("modifier~") || !req.hasParameter(param)) continue;
			
			try {
				String[] tokens = param.split("~");
				String modifierId = tokens[1];
				String modifierNm = req.getParameter("modifierName~"+modifierId);
				String attributeId = (tokens.length > 2) ? tokens[2] : null;
				String attributeNm = (attributeId != null) ? req.getParameter("modifierName~"+modifierId+"~"+attributeId) : null;
				if(attributeNm == null && attributeId != null && tokens.length > 3)
					attributeNm = req.getParameter("modifierName~"+modifierId+"~"+attributeId+"~"+tokens[3]);
				log.debug("found modifier " + modifierId + " with attrib=" + attributeId);
				log.debug("found modifierNm " + modifierNm + " with attribNm=" + attributeNm);
				
				ModifierVO mod = null;
				if(prod.getModifiers() != null)
					mod = prod.getModifiers().get(modifierId);
				
				if(mod == null) mod = new ModifierVO();
					
				mod.setModifier_id(modifierId);
				if(mod.getModifier_name() == null)
					mod.setModifier_name(modifierNm);
				
				if (attributeId != null) {
					AttributeVO attr = null;
					if(mod.getAttributes() != null)
						attr = mod.getAttributes().get(attributeId);
					if(attr == null) {
						attr = mod.new AttributeVO();
						attr.setAttribute_type(mods.get(modifierId).getAttributes().get(attributeId).getAttribute_type());
						attr.setAttribute_name(mods.get(modifierId).getAttributes().get(attributeId).getAttribute_name());
					}
					attr.setModifiers_attribute_id(attributeId);
					String value = req.getParameter(param);
					String[] values = value.split("~");
					attr.setValue(values[0]);
					if(values.length > 1){//TODO exception on product details page
						OptionVO opt = attr.new OptionVO();
						opt.setOption_name(values[1]);
						opt.setModifiers_attributes_options_id(values[2]);
						opt.setOption_value(values[0]);
						attr.addOption(opt);
					}
					else
						attr.setAttribute_name(attributeNm);
					mod.addAttribute(attr);
				}
				log.debug("adding modifier " + mod);
				prod.addModifier(mod);
				
			} catch (NullPointerException npe) {
				log.error("NPE parsing modifiers", npe);
			}
			
		}
		
		log.debug("Product Name: " + prod.getProductName());
			if(prod.getDisplay_name() == null){
				KeystoneProductVO dsol = (KeystoneProductVO) req.getSession().getAttribute("DSOLVO");
				if(dsol != null){
					prod.setDisplay_name(dsol.getProductName());
					prod.setWeb_description(dsol.getDescription());
				}
			}
		//set the product dimensions
		List<SizeVO> sizes = new ArrayList<SizeVO>();
		SizeVO size = new SizeVO();
		size.setHeight(Convert.formatInteger(req.getParameter("height")));
		size.setWidth(Convert.formatInteger(req.getParameter("width")));
		size.setHeight_unit_id(req.getParameter("height_unit_id"));
		size.setWidth_unit_id(req.getParameter("width_unit_id"));
		size.setProducts_sizes_id(req.getParameter("products_sizes_id"));
		size.setEcommerce_size_id(req.getParameter("ecommerce_size_id"));
		sizes.add(size);
		prod.setSizes(sizes);
		prod.setDimensions(size.getHeight() + " x " + size.getWidth());
		prod.setWeight(Convert.formatDouble(req.getParameter("weight")));

		return prod;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

}
