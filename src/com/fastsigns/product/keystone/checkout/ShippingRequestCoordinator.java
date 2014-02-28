package com.fastsigns.product.keystone.checkout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.Convert;
import com.smt.shipping.http.PackageVO;
import com.smt.shipping.http.ShippingAccountVO;
import com.smt.shipping.http.ShippingLocation;
import com.smt.shipping.http.ShippingRequestVO;

/****************************************************************************
 * <b>Title</b>: ShippingRequestCoordinator.java<p/>
 * <b>Description: This Object's sole purpose is to build a </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2013
 ****************************************************************************/
public class ShippingRequestCoordinator {
	
	protected static Logger log;
	private Map<String, Object> attributes;
	private CheckoutProxy proxy = null;

	public ShippingRequestCoordinator(Map<String, Object> attributes) {
		log = Logger.getLogger(CheckoutUtil.class);
		this.attributes = attributes;
		proxy = new CheckoutProxy((String) attributes.get("keystoneShippingCoordinator"));
	}
	
	public ShoppingCartVO retrieveShippingOptions(ShoppingCartVO cart) {
		FranchiseVO franchise = (FranchiseVO) attributes.get(KeystoneProxy.FRANCHISE);
		log.debug("franchise=" + franchise);
		
		/*
		 * Moved this code out of the try catch so that in event of an error retrieving 
		 * shipping options, we can still return instore pickup and the user can still 
		 * checkout.
		 */
		Map<String, ShippingInfoVO> opts = new LinkedHashMap<String, ShippingInfoVO>();
		ShippingInfoVO newVo = new ShippingInfoVO();
		newVo.setShippingMethodId("instore");
		newVo.setShippingMethodName("In-store Pickup");
		newVo.setShippingTime("0");
		newVo.setShippingCost(0);
		opts.put(newVo.getShippingMethodId(), newVo);
		
		try {
			ShippingRequestVO shippingInfo = buildShippingRequest(cart);
			String strShipReq = JSONObject.fromObject(shippingInfo).toString();
			log.debug("shipping request as String: " + strShipReq);
			proxy.addPostData("shippingRequest", strShipReq);
			byte[] data = proxy.getData();
			//DecimalFormat twoDForm = new DecimalFormat("#.##");
			
			//parse the response
			JSONObject raw = JSONObject.fromObject(new String(data));
			
			if (!raw.containsKey("isSuccess")) {				
				for (ShippingAccountVO acct : shippingInfo.getAccounts()) {
					// parse the rates response 
					JSONObject rates = raw.getJSONObject("ratesResponse");
					
					if (! rates.isNullObject() && ! rates.isEmpty()) {
						JSONArray jsonArr = rates.getJSONArray(acct.getShippingAccountType());
						for (int x=0; x < jsonArr.size(); x++) {
							JSONObject option = jsonArr.getJSONObject(x);
		
							//build a WC-coherent ShippingInfoVO
							newVo = new ShippingInfoVO();
							newVo.setShippingMethodId(option.optString("shippingMethodId"));
							newVo.setShippingMethodName(option.optString("shippingMethodName"));
							newVo.setShippingTime(option.optString("shippingTime"));
							
							//TODO Ensure this is the proper rateKey to use for FEDEX.  Multiple are sent back, this one was common to all.
							String rateKey = com.smt.shipping.http.ShippingInfoVO.COST_KEY_NEGOTIATED;
							Double rate = Convert.formatDouble(option.getJSONObject("shippingCosts").getDouble(rateKey));
							
							//get shipping markup from the Franchise data in keystone
							Integer markup = Convert.formatInteger((String)franchise.getAttributes().get("shipping_markup"), 0);
							log.debug("rate for " + newVo.getShippingMethodName() + "=" + rate );
							if (markup > 0) rate = rate * (1 + (markup * .01)); //bump costs by whatever is defined in Keystone
							log.debug("marked-up rate=" + rate + " markup=" + markup);
							newVo.setShippingCost(rate);
							
							opts.put(newVo.getShippingMethodId(), newVo);
						}
					}
				}
			}
		} catch(Exception e){
			log.error("Error Retrieving Shipping", e);
		}
		cart.setShippingOptions(opts);

		return cart;
	}
	
	private ShippingRequestVO buildShippingRequest(ShoppingCartVO cart) {
		FranchiseVO franchise = (FranchiseVO) attributes.get(KeystoneProxy.FRANCHISE);
		log.debug("franchise=" + franchise);
		log.debug("Building Shipping Source");
		ShippingLocation source = buildShippingLocation(franchise.getLocation());
		log.debug("Shipping source Built, building shipping Destination");
		ShippingLocation destn = buildShippingLocation(cart.getShippingInfo().getLocation());
		log.debug("Shipping destination built, adding accounts.");
		//this could be changed to a List<ShippingAccountVO> to support multiple couriers
		List<ShippingAccountVO> accounts = buildShippingAccounts(franchise);
		PackageVO pkg = loadPackage(cart);
		ShippingRequestVO sVo = new ShippingRequestVO();
		sVo.setShipper(source);
		sVo.setRecipient(destn);
		sVo.setAccounts(accounts);
		sVo.addPackage(pkg);
		
		return sVo;
	}
	
	/**
	 * iterates the cart items and gets the larged item's dimensions.  These will determine package size.
	 * Weight is itemWeght*Quantity (total combined)
	 * @param cart
	 * @return
	 */
	private PackageVO loadPackage(ShoppingCartVO cart) {
		PackageVO pkg = new PackageVO();
		int maxLength = 0;
		int maxWidth = 0;
		double weight = 0;
		
		for (ShoppingCartItemVO item : cart.getItems().values()) {
			KeystoneProductVO prod = (KeystoneProductVO) item.getProduct();
			if (prod.getSizes() == null || prod.getSizes().size() == 0) continue;
			
			
			/*
			 * TODO:  We need to have Fastsigns come back to us to add smarter algorithms here
			 * for size calculation.  Example Rolling, folding, etc.  Per Camire.
			 * We need to perform some better size calculation here.  
			 * Width by height is not the optimal measurement as the shipping carriers assume
			 * length to be the longest side in their calculations.  Here we are transposing
			 * the larger of the values to a length and the smaller to the width.  From there
			 * we save the largest of the orders lengths and widths as the package dimensions.
			 */
			SizeVO size = prod.getSizes().get(0);
			int l = Math.max(size.getHeight(), size.getWidth());
			int w = Math.min(size.getHeight(), size.getWidth());
			
			if (l > maxLength) maxLength = l;
			if (w > maxWidth) maxWidth = w;
			
			//increment the weight of the package accordingly
			weight += prod.getWeight()*size.getSquareInches()*item.getQuantity();
			log.debug("weight = " + weight);
			size = null;
		}
		
		//some providers will bomb if weight=0
		if (weight == 0) weight = 1;
		//todo fix shipping caps, see also ShoppingCartAction
		if (weight > 150) weight = 150;
		
		/*
		 * We're capping the length on the Package per Camires direction.  
		 * Most larger signs are vinyl and will be rolled or something and will take up less space
		 */
		pkg.setLength(Math.min(maxLength, 96));
		pkg.setWidth(Math.min(maxWidth, 48));
		pkg.setWeight(weight);
		return pkg;
		
	}
	
	/**
	 * transposes a Location into a ShippingLocation
	 * @param l
	 * @return
	 */
	private ShippingLocation buildShippingLocation(Location l) {
		ShippingLocation loc = new ShippingLocation();
		loc.setAddress(l.getAddress());
		loc.setCity(l.getCity());
		loc.setState(l.getState());
		loc.setZipCode(l.getZipCode());
		loc.setCountry(l.getCountry());
		return loc;
	}
	
	/**
	 * builds a ShippingAccountVO using the raw JSONObject the user is carrying
	 * on their session.  The passed object is the "franchise" object coming from Keystone
	 * @param accountJson
	 * @return
	 */
	private List<ShippingAccountVO> buildShippingAccounts(FranchiseVO fran) {
		List<ShippingAccountVO> data = new ArrayList<ShippingAccountVO>();
		ShippingAccountVO acct = new ShippingAccountVO();
		
		Map<String,String> accounts = new HashMap<String, String>();
		String shipService = (String) fran.getAttributes().get("ecomm_shipping_service");
		acct.setShippingAccountType(shipService);
		if(shipService.equals("FEDEX")) {
			accounts.put("FRANCHISE_ACCOUNT_KEY", (String) fran.getAttributes().get("fedex_account"));
			acct.setMeterNumber((String) fran.getAttributes().get("fedex_meter_number"));
		} else if(shipService.equals("UPS")) {
			accounts.put("FRANCHISE_ACCOUNT_KEY", (String) fran.getAttributes().get("ups_account"));
			acct.setAccountServiceKey((String) fran.getAttributes().get("ups_account_service_key"));
			acct.setAccountLoginId((String) fran.getAttributes().get("ups_account_login_id"));
			acct.setAccountPassword((String) fran.getAttributes().get("ups_account_login_password"));
		}
		//accounts.put(ShippingAccountVO.FRANCHISE_ACCOUNT_KEY, (String)fran.getAttributes().get("ups_account"));
		//TODO THIS NEEDS UPDATED ONCE KEYSTONE GETS FIXED
		//accounts.put(ShippingAccountVO.FRANCHISE_ACCOUNT_KEY, "3C9AB736B863D3C8");
		
		//TODO USE THIS INSTEAD
		//acct.setShippingAccountType((String)fran.getAttributes().get("ecomm_shipping_service"));
		
		acct.setAccountName(fran.getLocationName());
		//TODO THIS NEEDS UPDATED, RIGHT NOW UPS IS BROKEN!
		//	acct.setAccountLoginId("jcamire");
		//TODO THIS NEEDS UPDATED, RIGHT NOW UPS IS BROKEN!
		//	acct.setAccountPassword("c4nn0nda!e");
		acct.setAccountNumber(accounts);
		data.add(acct);
		
		return data;
	}
	
}
