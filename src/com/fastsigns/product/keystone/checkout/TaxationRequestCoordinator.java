package com.fastsigns.product.keystone.checkout;

import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.StringUtil;
import com.smt.http.AbstractWebServiceServlet.TaxationServiceType;
import com.smt.taxation.LineItemVO;
import com.smt.taxation.TaxLocationVO;
import com.smt.taxation.TaxationRequestVO;
import com.smt.taxation.TaxationResponseVO;
import com.smt.taxation.TaxationDataParser;

/****************************************************************************
 * <b>Title</b>: ShippingRequestCoordinator.java<p/>
 * <b>Description: This Object's sole purpose is to build a taxation call to SMTProxy</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2013
 ****************************************************************************/
public class TaxationRequestCoordinator {
	
	protected static Logger log;
	private Map<String, Object> attributes;
	private CheckoutProxy proxy = null;

	public TaxationRequestCoordinator(Map<String, Object> attributes) {
		log = Logger.getLogger(CheckoutUtil.class);
		this.attributes = attributes;
		proxy = new CheckoutProxy((String) attributes.get("keystoneTaxCoordinator"));
	}
	
	public ShoppingCartVO retrieveTaxOptions(ShoppingCartVO cart) {
		try {
		TaxationRequestVO taxInfo = buildTaxRequest(cart);
		proxy.addPostData("type", "json");
		proxy.addPostData("xmlData", JSONObject.fromObject(taxInfo).toString());
		byte[] data = proxy.getData();
		log.debug(new String(data));
		
		//parse the response
		TaxationDataParser tdp = new TaxationDataParser();
		TaxationResponseVO taxes = tdp.parseResponseData(data, "json");
		cart.setTaxAmount(taxes.getTotalTax());
		log.debug("Total Taxes" + taxes.getTotalTax());
		} catch(Exception e) {
			log.error("TAXATION HAS FAILED", e);
		}
		return cart;
	}
	
	private TaxationRequestVO buildTaxRequest(ShoppingCartVO cart) {
		FranchiseVO franchise = (FranchiseVO) attributes.get("franchise");
		log.debug("franchise=" + franchise);
			FastsignsSessVO fran = (FastsignsSessVO) attributes.get(KeystoneProxy.FRAN_SESS_VO);
			TaxationRequestVO taxReq = new TaxationRequestVO();
			taxReq.setPurchaseOrderNumber("Cust PO Num");
			taxReq.setReferenceCode("Keystone XML Test Invoice");
			taxReq.setCompanyCode("smt");
			taxReq.setCustomerId("fs_loc_1"); //franchiseId
			taxReq.setCustomerCode(cart.getBillingInfo().getProfileId());
			taxReq.setDetailLevel("Line");
			taxReq.setDocumentType("SalesOrder");
			taxReq.setCommitFlag(0);
			taxReq.setLicenseId("F6B84F7ECD531A2F");
			taxReq.setEnvironment("SANDBOX");
			taxReq.setAccountId("1100090458");
			taxReq.setExemptionNumber(StringUtil.checkVal(fran.getProfile(franchise.getWebId()).getAttributes().get("taxExempt")));
		//determine the tax service we'll use; this comes from Keystone
			TaxationServiceType taxType = TaxationServiceType.valueOf(franchise.getAttributes().get("ecomm_tax_service").toString());
			taxReq.setProviderType(taxType); //ecomm_tax_service
			String taxIdKey = (TaxationServiceType.AVALARA.equals(taxType)) ? "avalara_tax_id" : "default_tax_service";
			taxReq.setCustomerTaxId((String) franchise.getAttributes().get(taxIdKey));  //avalara_tax_id -or- default_tax_service
			taxReq.addTaxLocations(buildLocation(franchise.getLocation(), "src"));
			taxReq.addTaxLocations(buildLocation(cart.getShippingInfo().getLocation(), "destn"));
			taxReq = this.addLineItems(taxReq, cart);
		log.debug("WINNING");
		return taxReq;
	}
	
	
	private TaxLocationVO buildLocation(Location l, String locationId) {
		TaxLocationVO loc = new TaxLocationVO();
		loc.setLocationId(locationId);
		loc.setAddress(l.getAddress());
		loc.setCity(l.getCity());
		loc.setState(l.getState());
		loc.setZipCode(l.getZipCode());
		return loc;
	}
	
	
	/**
	 * iterates the cart items and add them as LineItemVOs to the taxation request
	 * @param cart
	 * @return
	 */
	private TaxationRequestVO addLineItems(TaxationRequestVO taxReq, ShoppingCartVO cart) {
		
		for (ShoppingCartItemVO item : cart.getItems().values()) {
			KeystoneProductVO prod = (KeystoneProductVO) item.getProduct();
			//log.debug("item=" + item);
			LineItemVO li = new LineItemVO();
			li.setAmount(item.getBasePrice()*item.getQuantity());
			li.setDestinationLocationId("destn"); //destination is where the item is being sent 
			if(item.getProductId().equals("shipping"))
				li.setItemCode("FR");
			else
				li.setItemCode("Product");
			li.setLineItemId(item.getProductId());
			li.setOriginLocationId("src"); //src is where the item is being purchased from
			li.setQuantity(item.getQuantity());
			li.setUnitPrice(item.getBasePrice());
			//li.setTaxAmount(item.getBasePrice());
			li.setUsageType("USAGE"); //arbitrary
			li.setTaxCode(evalTaxCode(prod.getTax_code_id()));
			
			//log.debug("lineItem=" + li);
			taxReq.addLineItem(li);
		}

		return taxReq;
		
	}
	
	/**
	 * helper method for the Constants set in Keystone
	 * @param id
	 * @return
	 */
	private String evalTaxCode(int id) {
		switch (id) {
			case 1: return "P0000000";
			case 2: return "P0000000";
			case 3: return "P0000000";
			case 4: return "P0000000";
			case 6: return "FR";
			case 5: 
			default:return "P0000000";
		}
	}
	
}
