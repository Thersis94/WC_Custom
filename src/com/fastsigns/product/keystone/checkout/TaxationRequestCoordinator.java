package com.fastsigns.product.keystone.checkout;

import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.CycleDetectionStrategy;

import org.apache.log4j.Logger;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.setExcludes(new String[]{"data", "singleLineAddress", "matchCode"});
		jsonConfig.setIgnoreDefaultExcludes(false);
		jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);
		JSONObject jso = JSONObject.fromObject(taxInfo, jsonConfig);
		log.debug(jso);
		proxy.addPostData("xmlData", jso.toString());
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
		FastsignsSessVO fran = (FastsignsSessVO) attributes.get(KeystoneProxy.FRAN_SESS_VO);
		FranchiseVO franchise = (FranchiseVO) attributes.get("franchise");
		log.debug("franchise: " + StringUtil.getToString(franchise));
		log.debug("franchiseAttrs: " + franchise.getAttributes());
		
		TaxationRequestVO taxReq = new TaxationRequestVO();
		taxReq.setPurchaseOrderNumber(cart.getPurchaseOrderNo());  //comes off request
		taxReq.setReferenceCode(new UUIDGenerator().getUUID()); //needs to be a jobID or cartId --using a GUID for lack of something better -JM 1/24/14
		log.debug("invoiceNo=" + cart.getInvoiceNo());
//		taxReq.setCompanyCode(franchise.getFranchiseId()); //franchiseId
//		taxReq.setCustomerId(franchise.getFranchiseId()); //franchiseId
	taxReq.setCompanyCode("FSI0479");
	taxReq.setCustomerId("FSI0479");
		taxReq.setCustomerCode(cart.getBillingInfo().getProfileId());
		taxReq.setDetailLevel("Line"); //constant
		taxReq.setDocumentType("SalesOrder"); //constant
		taxReq.setCommitFlag(0); //constant for ecomm
//		taxReq.setLicenseId(StringUtil.checkVal(franchise.getAttributes().get("avalara_license_id"))); //franchise attrs: avalara_license_id
	taxReq.setLicenseId("FB589952A0E356A9");
//		taxReq.setAccountId(StringUtil.checkVal(franchise.getAttributes().get("avalara_tax_id"))); //franchise attrs: avalara_tax_id
	taxReq.setAccountId("1100131557");
		taxReq.setExemptionNumber(StringUtil.checkVal(fran.getProfile(franchise.getWebId()).getAttributes().get("taxExempt")));
		
		//leverage business rules to configure taxType, taxId, and keystoneEnvironment
		taxReq = configureTaxParameters(attributes, franchise, taxReq);
		
		
		taxReq.addTaxLocations(buildLocation(franchise.getLocation(), "src"));
		taxReq.addTaxLocations(buildLocation(cart.getShippingInfo().getLocation(), "destn"));
		taxReq = this.addLineItems(taxReq, cart);
		return taxReq;
	}
	
	
	
	/**
	 * reusable builder of TaxLocationVO from the passed LocationVO
	 * @param l
	 * @param locationId
	 * @return
	 */
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
	 * leverages business rules to configure the tax parameters.  -JM 01-24-14
	 * @param attributes
	 * @param franchise
	 * @param taxReq
	 * @return
	 */
	protected static TaxationRequestVO configureTaxParameters(Map<String, Object> attributes, 
			FranchiseVO franchise, TaxationRequestVO taxReq) {
		//use WC config value to determine which Environment to set, PRODUCTION=PRODUCTION, always.
		//STAGING = SANDBOX when using AVALARA, STAGING=MIGRATION where using FASTSIGNS_CUSTOM taxProvider
		//one of [PRODUCTION, SANDBOX when in staging AND AVALARA, MIGRATION when in staging and custom]
		String instanceNm = StringUtil.checkVal(attributes.get("keystoneEnvironment"));
		
		//determine the tax service we'll use; this comes from Keystone
		//try-catch here because "ecomm_tax_service" is a GUID if != AVALARA.
		TaxationServiceType taxType = null;
		try {
			taxType = TaxationServiceType.valueOf(franchise.getAttributes().get("ecomm_tax_service").toString());
		} catch (Exception e) {
			taxType = TaxationServiceType.FASTSIGNS_CUSTOM;
		}
		taxReq.setProviderType(taxType);
		
		//set the taxId and Environment according to the taxType
		if (TaxationServiceType.AVALARA.equals(taxType)) {
			//When Avalara: providerType="AVALARA", customerTaxId = "AVALARA"
			taxReq.setCustomerTaxId(TaxationServiceType.AVALARA.toString());
//			taxReq.setEnvironment("STAGING".equalsIgnoreCase(instanceNm) ? "SANDBOX" : "PRODUCTION");
		taxReq.setEnvironment("PRODUCTION");
		} else {
			//When Custom: providerType="FASTSIGNS_CUSTOM", customerTaxId = "SOME Guid"
			taxReq.setCustomerTaxId((String) franchise.getAttributes().get("default_tax_service"));
			taxReq.setEnvironment("STAGING".equalsIgnoreCase(instanceNm) ? "MIGRATION" : "PRODUCTION");
		}
		
		return taxReq;
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
