package com.fastsigns.product.keystone;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import com.fastsigns.product.keystone.vo.ImageVO;
import com.fastsigns.product.keystone.vo.ModifierVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO.OptionVO;
import com.fastsigns.product.keystone.vo.ProductDetailVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ProductAction.java<p/>
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
		if(req.hasParameter("pricing") && !req.hasParameter("pricingComplete")){
			req.setParameter("pricingComplete", "true");
			ShoppingCartAction sca = new ShoppingCartAction(actionInit);
			sca.setAttributes(attributes);
			sca.setDBConnection(dbConn);
			sca.build(req);
		}
		else {
			//TODO turn on caching
			KeystoneProxy proxy = new CachingKeystoneProxy(attributes, 1440);
			//KeystoneProxy proxy = new KeystoneProxy(attributes);
			proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
			proxy.setModule("products");
			proxy.setAction("getProductDetails");
			proxy.addPostData("productId", req.getParameter("product"));
			
			if (req.hasParameter("material")) {
				proxy.addPostData("material", "true");
			} else {
				proxy.addPostData("usageId", req.getParameter("usageId")); //categoryId
			}
			
			try {
				//tell the proxy to go get our data
				byte[] byteData = proxy.getData();
				
				//transform the response into something meaningful to WC
				mod.setActionData(formatData(byteData));
				
			} catch (InvalidDataException e) {
				log.debug(e);
				mod.setError(e);
				mod.setErrorMessage("Unable to load Product Details");
			}
			if(req.hasParameter("itemId")){
				ShoppingCartAction sca = new ShoppingCartAction(this.actionInit);
				sca.setDBConnection(dbConn);
				sca.setAttributes(attributes);
				Storage container = sca.loadCartStorage(req);
				ShoppingCartVO cart = container.load();		
				ShoppingCartItemVO vo = cart.getItems().get(req.getParameter("itemId"));
				if(vo != null)
					req.setAttribute("cartItem", vo);
			}
				
			
			//set APIKey for the browser to use to call for pricing.
			req.setAttribute("keystoneApiKey", proxy.buildApiKey());
			setAttribute(Constants.MODULE_DATA, mod);
		}
	}
	
	
	private ProductDetailVO formatData(byte[] byteData) throws InvalidDataException {
		//define a Map of object types for each of the different Object variables in the bean
		Map<String, Class<?>> dMap = new HashMap<String, Class<?>>();
		dMap.put("sizes", SizeVO.class);
		dMap.put("images", ImageVO.class);
		ProductDetailVO vo = null;
		
		try {
			//pass the definition Map and base bean Class to the static toBean generator
			JSONObject jsonObj = JSONObject.fromObject(new String(byteData));
			vo = (ProductDetailVO) JSONObject.toBean(jsonObj, ProductDetailVO.class, dMap);
	
			//now we need to iterate the modifiers and sublevels
			JSONObject modsObj = jsonObj.getJSONObject("modifiers");
			Set<?> modifiers = modsObj.keySet();
			for (Object modifier : modifiers) {
				JSONObject modObj = JSONObject.fromObject(modsObj.get(modifier));
				ModifierVO modVo = new ModifierVO();
				modVo.setDescription(modObj.getString("description"));
				modVo.setModifier_id(modObj.getString("modifier_id"));
				modVo.setModifier_name(modObj.getString("modifier_name"));
				
				JSONObject attrsObj = modObj.getJSONObject("attributes");
				Set<?> attributes = attrsObj.keySet();
				for (Object attribute : attributes) {
					JSONObject attrObj = JSONObject.fromObject(attrsObj.get(attribute));
					AttributeVO attrVo = modVo.new AttributeVO();
					attrVo.setAttribute_name(attrObj.getString("attribute_name"));
					attrVo.setAttribute_type(attrObj.getString("attribute_type"));
					attrVo.setModifiers_attribute_id(attrObj.getString("modifiers_attributes_id"));
					attrVo.setAttribute_required(attrObj.getInt("attribute_required"));
					
					JSONObject optionsObj = attrObj.getJSONObject("options");
					Set<?> options = optionsObj.keySet();
					for (Object option : options) {
						JSONObject optObj = JSONObject.fromObject(optionsObj.get(option));
						OptionVO optVo = attrVo.new OptionVO();
						optVo.setModifiers_attributes_options_id(optObj.getString("modifiers_attributes_options_id"));
						optVo.setOption_name(optObj.getString("option_name"));
						optVo.setOption_value(optObj.getString("option_value"));
						attrVo.addOption(optVo);
					}
					modVo.addAttribute(attrVo);
				}
				vo.addModifier(modVo);
			}
		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return vo;
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
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		//ShoppingCartAction facades all cart-related activities.
		//ShoppingCartAction will call Keystone for pricing, on our behalf (via PricingUtil).
		//the user's ShoppingCartVO will be put into ModuleVO for us to use when we get to the View (here)
		if(req.hasParameter("attributes"))
			retrieve(req);
		else{
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
