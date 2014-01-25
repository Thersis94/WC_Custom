package com.fastsigns.product.keystone.checkout;

import java.text.DecimalFormat;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.product.keystone.vo.ModifierVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO;
import com.fastsigns.product.keystone.vo.ModifierVO.AttributeVO.OptionVO;
import com.fastsigns.product.keystone.vo.SizeVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.commerce.DiscountVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.http.AbstractWebServiceServlet.TaxationServiceType;
import com.smt.sitebuilder.action.FileLoader;


/****************************************************************************
 * <b>Title</b>: OrderSubmissionCoordinator.java<p/>
 * <b>Description: This Object's sole purpose is to convert a ShoppingCartVO
 * into a JSONObject for submission to Keystone.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2013
 * @updated Feb 20, 2013 Billy Larsen - Refactored code, cleaned up formatting,
 * 										added Comments.
 ****************************************************************************/
public class OrderSubmissionCoordinator {
	
	protected static Logger log;
	private Map<String, Object> attributes;
	private KeystoneProxy proxy = null;

	public OrderSubmissionCoordinator(Map<String, Object> attributes) {
		log = Logger.getLogger(CheckoutUtil.class);
		this.attributes = attributes;
		proxy = new KeystoneProxy(attributes);
	}
	
	/**
	 * This method handles taking in a shopping cart and converting it to a 
	 * JSON Object for submission to Keystone.
	 * @param cart
	 * @return
	 * @throws ActionException
	 */
	public ShoppingCartVO submitOrder(ShoppingCartVO cart) throws ActionException {
		FastsignsSessVO sessVo = (FastsignsSessVO) attributes.get(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String) attributes.get("webId");
		attributes.put("franchise", sessVo.getFranchise(webId));
		
		//Build Proxy Call
		proxy.setModule("jobs");
		proxy.setUserId(sessVo.getProfile(webId).getUserId());
		proxy.addPostData("eComm", "true");
		proxy.setFranchiseId(sessVo.getFranchise(webId).getFranchiseId());

		/*
		 *  If we have a jobId, this is a payment resubmit, otherwise we are 
		 *  submitting for the first time.
		 */
		if (attributes.get("jobId") != null && ((String)attributes.get("jobId")).length() > 0) {
			proxy.setAction("eCommPayJob");
			proxy.addPostData("jobId", (String) attributes.get("jobId"));
		} else {
			proxy.setAction("createEJob");
			proxy.addPostData("jobInfo", buildJobInfo(cart).toString());
			proxy.addPostData("shippingDetails", buildShippingDetails(cart).toString());
			proxy.addPostData("shipping", cart.getShipping().getShippingMethodId().equals("instore") ? "false" : "true");
		}
		
		proxy.addPostData("paymentDetails", buildPaymentDetails(cart).toString());
		
		try {
			byte[] data = proxy.getData();

			/*
			 * Place JSON Response data on the error map of the cart
			 * to flow up the chain. 
			 */
			JSONObject raw = JSONObject.fromObject(new String(data));
			cart.addError("success", raw.optString("success"));
			cart.addError("jobId", raw.optString("jobId"));
			cart.addError("message", raw.optString("message"));
			
		} catch (InvalidDataException ide) {
			log.error("order submission failed", ide);
			cart.addError("success", "false");
			cart.addError("message", ide.getMessage());
		}		
		
		return cart;
	}
	
	
	/**
	 * This method builds information about the job for initial submits.
	 * @param cart
	 * @return
	 */
	private JSONObject buildJobInfo(ShoppingCartVO cart) {
		FranchiseVO franchise = (FranchiseVO) attributes.get("franchise");
		JSONObject order = new JSONObject();
		
		//determine the tax service we'll use; this comes from Keystone
		TaxationServiceType taxType = TaxationServiceType.valueOf((String) franchise.getAttributes().get("ecomm_tax_service"));
		order.accumulate("default_tax_service",taxType);
		String taxIdKey = (TaxationServiceType.AVALARA.equals(taxType)) ? "avalara_tax_id" : "default_tax_service";
		order.accumulate("customerTaxId", (String) franchise.getAttributes().get(taxIdKey));  //avalara_tax_id -or- default_tax_service
		order.accumulate("products", buildProducts(cart));
		return order;
	}
	
	
	/**
	 * This method iterates the cart items and compiles the complex object 
	 * structure of the products in the cart (including modifiers & attributes)
	 * @param cart
	 * @return
	 */
	private JSONArray buildProducts(ShoppingCartVO cart) {
		JSONArray prods = new JSONArray();
		StringEncoder senc = new StringEncoder();
		/*
		 * Loop over the items in the shopping cart and add them to the JSON
		 * Array for submission.
		 */
		for (ShoppingCartItemVO item : cart.getItems().values()) {
			KeystoneProductVO prod = (KeystoneProductVO) item.getProduct();
			log.debug("item=" + item);
			
			//calculate tax amount and rates for the order. 
			double taxRate = cart.getTaxAmount() / cart.getSubTotal();
			double tax = roundTwoDecimals(taxRate * ((prod.getMsrpCostNo() * item.getQuantity()) - prod.getDiscount()));

			// build the product object
			JSONObject p = new JSONObject();
			p.accumulate("product_id", item.getProduct().getProductId());
			p.accumulate("name", senc.decodeValue(item.getProductName()));
			p.accumulate("quantity", item.getQuantity());
			p.accumulate("price", roundTwoDecimals(prod.getMsrpCostNo()));
			p.accumulate("discount", roundTwoDecimals(prod.getDiscount()));
			p.accumulate("tax_rate", taxRate);

			// Add Size Data
			SizeVO s = prod.getSizes().get(0);
			p.accumulate("height", s.getWidth());
			p.accumulate("height_unit_id", s.getWidth_unit_id());
			p.accumulate("width", s.getHeight());
			p.accumulate("width_unit_id", s.getHeight_unit_id());
			p.accumulate("surfaceArea", s.getSquareInches());
			p.accumulate("weightCoefficient", prod.getWeight());
			p.accumulate("weight", prod.getWeight() * s.getSquareInches());

			/*
			 * Loop over all the modifiers and add them to the order.
			 */
			JSONArray modifiers = new JSONArray();
			if(prod.getModifiers() != null){
				for (ModifierVO mod : prod.getModifiers().values()) {
					
					//calculate the modifier tax
					double mTax = roundTwoDecimals((mod.getPrice() * mod.getQuantity() - mod.getDiscount()) * (cart.getTaxAmount() / cart.getSubTotal()));
					
					//remove the modifier tax from the order tax for keystone.
					//tax-=mTax;

					//Add the modifier					
					modifiers.add(getModifier(mod, mTax, taxRate));
				}
			}
			p.accumulate("modifiers", modifiers);
			
			//Loop over the order discounts and add them.
			if (prod.getDiscounts() != null)
				p.accumulate("discounts", getDiscounts(prod));
			
			/*
			 * Add tax information once all modifiers have been calculated and
			 * their individual taxes deducted from the total. 
			 */
			p.accumulate("tax", tax);
			
			/*
			 * If we have a dsolItem, move the data to the permanent fileSystem.
			 */
			if(prod.getProdAttributes().containsKey("highResPath") && attributes.get("keystoneDsolFilePath") != null){
				//Generate random folders
				String hrd = moveFile((String) prod.getProdAttributes().get("highResPath"), this.attributes);
				if(hrd != null && hrd.length() > 0)
				p.accumulate("highResImage", generateFileData(hrd, "image/jpeg", (Integer) prod.getProdAttributes().get("hrdDataSize")));
				
				String pdf = moveFile((String) prod.getProdAttributes().get("pdfPath"), this.attributes);
				if(pdf != null && pdf.length() > 0)
				p.accumulate("pdfData", generateFileData(pdf, "application/pdf", (Integer) prod.getProdAttributes().get("pdfSize")));
				
				String svg = moveFile((String) prod.getProdAttributes().get("svgData"), this.attributes);
				if(svg != null && svg.length() > 0)
				p.accumulate("svgData", generateFileData(svg, "image/svg+xml", (Integer) prod.getProdAttributes().get("svgSize")));
				
//				//String lowResPath = DSOLAction.writeBase64File((String) prod.getProdAttributes().get("thumbnailData"), "thumbnailData.png", attributes, ran1, ran2);
//				String svg = DSOLAction.writeBase64File((byte[]) prod.getProdAttributes().get("svgData"), UUID.randomUUID() + ".txt", attributes, ran1, ran2);
//				if(svg != null && svg.length() > 0)
//					p.accumulate("svgData", generateFileData(svg, "text/rtf", prod.getProdAttributes().get("svgData")));
//				//String jsonPath = DSOLAction.writeBase64File((String) prod.getProdAttributes().get("jsonData"), "jsonData.txt", attributes, ran1, ran2);
				log.debug("Done Writing files");		
			}
				
			//add the finished product to the cart.
			prods.add(p);
		}
		log.debug(prods.toString());
		return prods;
	}

	public String moveFile(String path, Map<String, Object> attributes){
		FileLoader fl  = null;
		attributes.put("fileManagerType", attributes.get("dsolFileManagerType"));
		log.debug("path=" + (String) attributes.get("keystoneDsolTemplateFilePath") + path);
		try {
			fl = new FileLoader(attributes);
			String source = (String) attributes.get("keystoneDsolTemplateFilePath") + path, dest = (String) attributes.get("keystoneDsolFilePath") + path;
			fl.copy(source, dest);
			fl.deleteFile((String) attributes.get("keystoneDsolTemplateFilePath") + path);
			return path;
		} catch (Exception e) {
			log.error("exception", e);
		}
		return "";
	}
	/**
	 * Helper method that takes a file and writes the JSONObject data for it.
	 * @param file
	 * @param type
	 * @return
	 */
	private Object generateFileData(String path, String type, int fileSize) {
		JSONObject obj = new JSONObject();
		obj.accumulate("filename", path.substring(path.lastIndexOf("/") + 1, path.length()));
		log.debug("name = " + path.substring(path.lastIndexOf("\\") + 1, path.length()));
		obj.accumulate("filepath", "/" + path.replace("\\", "/"));
		obj.accumulate("filesize", fileSize);
		obj.accumulate("filetype", type);
		return obj;	
	}

	/**
	 * This method returns a JSONObject of Modifier Information for a product.
	 * @param mod
	 * @param mTax
	 * @param taxRate
	 * @return
	 */
	private JSONObject getModifier(ModifierVO mod, double mTax, double taxRate) {
		JSONObject m = new JSONObject();
		JSONArray a = new JSONArray();
		m.accumulate("job_line_item_modifier_id", "ext-record-0");
		m.accumulate("modifier_id", mod.getModifier_id());
		m.accumulate("modifier_name", mod.getModifier_name());
		m.accumulate("tax", mTax);
		m.accumulate("tax_rate", taxRate);
		m.accumulate("price", roundTwoDecimals(mod.getPrice()));
		m.accumulate("discount", roundTwoDecimals(mod.getDiscount()));
		m.accumulate("unit_cost", roundTwoDecimals(mod.getUnit_cost()));
		
		/*
		 * Loop over the attributes and add them to the order.
		 */
		if(mod.getAttributes() != null){
			for (AttributeVO avo : mod.getAttributes().values()) {
				a.add(getAttribute(avo));
			}
		}
		m.accumulate("attributes", a);
		log.debug(m.toString());
		return m;
	}
	
	/**
	 * This method returns a JSONObject of Attribute Information for a modifier.
	 * @param avo
	 * @return
	 */
	private JSONObject getAttribute(AttributeVO avo) {
		JSONObject attr = new JSONObject();
		attr.accumulate("modifiers_attribute_id", avo.getModifiers_attribute_id());
		attr.accumulate("attribute_name", avo.getAttribute_name());
		attr.accumulate("type", avo.getAttribute_type());
		attr.accumulate("value", roundTwoDecimals(Convert.formatDouble(avo.getValue())));
		
		// add selected option to attribute
		if(avo.getOptions() != null){
			for (OptionVO ovo : avo.getOptions().values()) {
				attr.accumulate("option_id", ovo.getModifiers_attributes_options_id());
				attr.accumulate("option_name", ovo.getOption_name());
			}
		}		return attr;
	}

	/**
	 * This method returns the discounts for a product.
	 * @param prod
	 * @return
	 */
	private JSONArray getDiscounts(KeystoneProductVO prod){
		JSONArray discounts = new JSONArray();
		JSONObject discount = new JSONObject();
		for(DiscountVO d : prod.getDiscounts()){
			discount.accumulate("job_line_item_discount_id", null);
			discount.accumulate("discount_id", d.getDiscountId());
			discount.accumulate("discount_type_id", d.getDiscountType());
			discount.accumulate("name", d.getDiscountName());
			discount.accumulate("value", roundTwoDecimals(d.getDiscountValue()));
			discount.accumulate("dollar_value", roundTwoDecimals(d.getDiscountDollarValue()));
			discount.accumulate("active", Convert.formatInteger(d.isDiscountActive()));
			discounts.add(discount);
		}
		return discounts;
	}
	
	/**
	 * This method truncates run off decimals into the currency format 0.00
	 * @param d
	 * @return
	 */
	private double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
    return Double.valueOf(twoDForm.format(d));
}

	/**
	 * This method assembles the paymentDetails of the order
	 * @param cart
	 * @return
	 */
	private JSONObject buildPaymentDetails(ShoppingCartVO cart) {
		PaymentVO p = cart.getPayment();
		JSONObject pymt = new JSONObject();
		if(StringUtil.checkVal(cart.getPurchaseOrderNo()).length() > 0) {
			pymt.accumulate("payment_method", "po");
			pymt.accumulate("po_number", cart.getPurchaseOrderNo());
		} else {
			JSONObject ccInfo = new JSONObject();
			ccInfo.accumulate("amount", roundTwoDecimals(cart.getCartTotal()));
			ccInfo.accumulate("ccNum", p.getEncPaymentNumber());
			ccInfo.accumulate("ccExpMo", p.getExpirationMonth());
			ccInfo.accumulate("ccExpYear", p.getExpirationYear());
			ccInfo.accumulate("ccName", p.getPaymentName());
			ccInfo.accumulate("ccSec", p.getPaymentCode());
			ccInfo.accumulate("ccZip", cart.getBillingInfo().getZipCode());
			pymt.accumulate("payment_method", "cc");
			pymt.accumulate("payment_method_type_id", p.getPaymentType());
			pymt.accumulate("ccInfo", ccInfo);
		}
		return pymt;
	}
	
	/**
	 * This method builds the shipping details for an order.
	 * @param cart
	 * @return
	 */
	private JSONObject buildShippingDetails(ShoppingCartVO cart) {
		JSONObject ship = new JSONObject();
		ShippingInfoVO si = cart.getShipping();
		if(si != null){
			ship.accumulate("address", buildAddressDetails(cart));
			ship.accumulate("shippingTotal", roundTwoDecimals(si.getShippingCost()));
			ship.accumulate("carrier_name", "UPS");
			ship.accumulate("class_of_service", si.getShippingMethodName());
		}
		return ship;
	}
	
	/**
	 * This method builds the address pieces for the shipping details.
	 * @param cart
	 * @return
	 */
	private JSONObject buildAddressDetails(ShoppingCartVO cart) {
		JSONObject address = new JSONObject();
		UserDataVO sd = cart.getShippingInfo();
		address.accumulate("address_1", sd.getAddress());
		address.accumulate("address_2", sd.getAddress2());
		address.accumulate("city", sd.getCity());
		address.accumulate("state_id", sd.getState());
		address.accumulate("zipcode", sd.getZipCode());
		address.accumulate("country_id", sd.getCountryCode());
		address.accumulate("lat", sd.getLatitude());
		address.accumulate("lng", sd.getLongitude());
		address.accumulate("address_type", "Shipping");
		address.accumulate("address_id", StringUtil.checkVal(sd.getAttributes().get("addressId")));
		return address;
	}
	
}
