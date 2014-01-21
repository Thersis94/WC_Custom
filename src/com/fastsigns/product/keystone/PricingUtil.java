package com.fastsigns.product.keystone;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.ModifierVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.commerce.DiscountVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PricingAction.java<p/>
 * <b>Description: Calls Keystone for pricing using the passed-in ShoppingCartVO.
 * The SON sent to Keystone must contain the modifiers/attributes, sizing, quantity, etc.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 3, 2013
 ****************************************************************************/
public class PricingUtil {
	
	protected static Logger log;
	private Map<String, Object> attributes;

	public PricingUtil(Map<String, Object> attributes) {
		log = Logger.getLogger(PricingUtil.class);
		this.attributes = attributes;
	}
	
	public ShoppingCartVO loadPricing(SMTServletRequest req, ShoppingCartVO cart) {
		
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		String franId = sessVo.getFranchise(webId).getFranchiseId();

		KeystoneProxy proxy = new KeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("pricingManager");
		proxy.setAction("cost_and_price");
		proxy.addPostData("applyDiscounts", "true");
		proxy.setFranchiseId(franId);
		//Jason request this be added on our end, hard coded values that determine pricing coming back for ecom.
		proxy.addPostData("usages", "2,3");
		proxy.addPostData("products", formatJSONReq(cart));
		proxy.setAccountId(sessVo.getProfile(webId).getAccountId());
		
		try {
			//tell the proxy to go get our data
			byte[] byteData = proxy.getData();
		
			//transform the response into something meaningful to WC
			formatData(byteData, cart);
			
		} catch (InvalidDataException ide) {
			log.error("error loading pricing", ide);
		}
		
		return cart;
	}
	
	
	/**
	 * formats Keystone's response to our JSON inquiry into something meaningful to WC
	 * @param byteData
	 * @return
	 */
	private ShoppingCartVO formatData(byte[] byteData, ShoppingCartVO cart) throws InvalidDataException {
		
		try {
			JSONObject results = JSONObject.fromObject(new String(byteData)).getJSONObject("results");
			
			//loop through the cart and update each item's price
			for (ShoppingCartItemVO item : cart.getItems().values()) {
				JSONObject priceObj = results.getJSONObject(item.getProductId());
				if (priceObj == null) continue;
				
				KeystoneProductVO prod = (KeystoneProductVO) item.getProduct();
				prod.setDiscount(Convert.formatDouble(priceObj.getString("discount")));
				prod.setMsrpCostNo(Convert.formatDouble(priceObj.getString("price")));
				item.setBasePrice(Convert.formatDouble(priceObj.getString("unit_combined_subtotal")));
				item.setQuantity(priceObj.getInt("quantity"));
				JSONArray discounts = priceObj.getJSONArray("discounts");
				
				//Ensure we flush existing discounts out before adding new ones.
				prod.setDiscounts(new ArrayList<DiscountVO>());
				for (int i = 0; i < discounts.size(); i++) {
					JSONObject disc = discounts.getJSONObject(i);
					DiscountVO d = new DiscountVO();
					d.setDiscountValue(disc.getDouble("value"));
					d.setDiscountActive(Convert.formatBoolean(disc.getInt("active")));
					d.setDiscountId(disc.getString("discount_id"));
					d.setDiscountName(disc.getString("name"));
					d.setDiscountDollarValue(disc.getDouble("dollar_value"));
					d.setDiscountType(disc.getString("discount_type_id"));
					prod.addDiscount(d);
				}
				

				if (priceObj.containsKey("modifiers")) {
					log.debug("parsing modifiers");
					try {
						JSONArray modifiers = priceObj.getJSONArray("modifiers");
						
						for (int i = 0; i < modifiers.size(); i++) {
							JSONObject m = JSONObject.fromObject(modifiers.get(i));
							ModifierVO mod = prod.getModifiers().get(m.get("job_line_item_modifier_id"));
							mod.setPrice(m.getDouble("price"));
							mod.setDiscount(m.getDouble("discount"));
							mod.setQuantity(m.getInt("quantity"));
							mod.setUnit_cost(m.getDouble("unit_cost"));
						}
					} catch (Exception e) {
						log.error("could not parse modifier", e);
					}
				}
				
				log.debug("Discounts = " + prod.getDiscount());	
				cart.add(item);
			}

		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return cart;
	}
	
	
	/**
	 * structures a JSON object to submit to Keystone (via the Proxy)
	 * @param req
	 * @return
	 */
	private String formatJSONReq(ShoppingCartVO cart) {
		JSONObject products = new JSONObject();
		
		//loop the passed products
		for (ShoppingCartItemVO item : cart.getItems().values()) {
			KeystoneProductVO prod = (KeystoneProductVO) item.getProduct();
			if (prod == null) continue;  //we can't price a product that doesn't exist!

			//build the product Object
			JSONObject productN = new JSONObject();
			productN.accumulate("phantom", "true");
			productN.accumulate("product_id", prod.getProductId());
			
			//loop the modifiers for this product
			if (prod.getModifiers() != null) {
				JSONObject modifiers = new JSONObject();
				//Integer y = Integer.valueOf(0);
				for (ModifierVO mod : prod.getModifiers().values()) {
					JSONObject modN = new JSONObject();
					//String modifierId = "modifier" + y++;
					String modifierId = mod.getModifier_id();
					
					//loop the attributes of this modifier
					if (mod.getAttributes() != null) {
						JSONObject attributes = new JSONObject();
						
						Integer z = Integer.valueOf(0);
						for (AttributeVO attr : mod.getAttributes().values()) {
							JSONObject attrN = new JSONObject();
							attrN.accumulate("modifiers_attributes_id", attr.getModifiers_attribute_id());
							attrN.accumulate("value", attr.getValue());
							attributes.accumulate((z++).toString(), attrN);
						}
						
						modN.accumulate("attributes", attributes);
					}
					
					modN.accumulate("job_line_item_modifier_id", modifierId);
					modN.accumulate("modifier_id", mod.getModifier_id());
					modifiers.accumulate(modifierId, modN);
				}
				productN.accumulate("modifiers", modifiers);
			}
						
			if (prod.getSizes() != null && prod.getSizes().size() > 0) {
				SizeVO size = prod.getSizes().get(0);
				productN.accumulate("height", size.getHeight());
				productN.accumulate("height_unit_id", StringUtil.checkVal(size.getHeight_unit_id(), "1"));
				productN.accumulate("width", size.getWidth());
				productN.accumulate("width_unit_id", StringUtil.checkVal(size.getWidth_unit_id(), "1"));
			}
			productN.accumulate("quantity", item.getQuantity());
			
			//add this product to the container
			products.accumulate(item.getProductId(), productN);
		}
		
		log.debug("pricingRequest JSON: " + products.toString());
		return products.toString();
	}
}
