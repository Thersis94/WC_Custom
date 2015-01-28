package com.universal.commerce;

//Java 7
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

// Apache Dom4j
import org.dom4j.DocumentException;
import org.dom4j.Element;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.DiscountVO;
import com.siliconmtn.commerce.ShippingInfoVO;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Custom libs
import com.universal.commerce.USADiscountVO.DiscountStatus;
import com.universal.commerce.USADiscountVO.DiscountType;
import com.universal.commerce.USADiscountVO.ItemDiscountType;
import com.universal.util.WebServiceAction;

/****************************************************************************
 * <b>Title</b>: DiscountManager.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages business logic for USA discount processing.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Jan 23, 2013<p/>
 * Changes:
 * Jan 23, 2013: DBargerhuff; created class
 * Feb 13, 2013: DBargerhuff; added methods to class.
 * Jan 27, 2015: DBargerhuff; refactored item discount processing to fix bug in 
 * processing items that have options associated with them.
 ****************************************************************************/
public class DiscountManager implements Serializable {
	
	private static final long serialVersionUID = 1325281908774326923L;
	private Logger log = Logger.getLogger(getClass());
	private String catalogSiteId;
	private ShoppingCartVO cart;
	private USADiscountVO discount;
	private ActionInitVO actionInit;
	private Map<String,Object> attributes;
	private Map<String, String> errors;
	
	public DiscountManager() {
		errors = new HashMap<String, String>();
	}
	
	/**
	 * Retrieves and/or applies discount to shopping cart.  If a discount already exists
	 * on the cart, it is first removed and then the new discount is applied.
	 * @param cart
	 * @param promoCode
	 */
	public void updateCartDiscount(ShoppingCartVO cart, String promoCode) {
		log.debug("processing new discount...");
		String trimCode = StringUtil.checkVal(promoCode).trim().toUpperCase();
		log.debug("promoCode is: " + trimCode);
		if (trimCode.length() == 0) return;
		
		if (this.checkForDiscountRetrieval(cart, trimCode)) {
			USADiscountVO uDisc =  null;
			try {
				Element disc = this.retrieveFullCartDiscountElement(cart, trimCode);
				log.debug("discount XML retrieved: " + disc.asXML());
				uDisc = new USADiscountVO(disc);
				if (uDisc.getDiscountStatus().equals(DiscountStatus.ACTIVE)) {
					log.debug("discount type is: " + uDisc.getEnumDiscountType());
					// remove any existing discount and add newly retrieved discount
					this.removeCartDiscount(cart);
					cart.addDiscount(uDisc);
					cart.setPromotionCode(trimCode);
					if (uDisc.getEnumDiscountType().equals(DiscountType.ITEM)) {
						// if we added an item-level discount, we need to 
						// check the list of items on the cart against the discount VO
						// that we just retrieved.
						this.refreshCartItemDiscounts(cart);
					}
				} else {
					this.addError("discountCode", promoCode);
					this.addError("discountError", uDisc.getDiscountStatus().name());
				}
			} catch (DocumentException de) {
				log.error("Error retrieving full cart discount element from webservice, ", de);
				this.addError("discountCode", promoCode);
				this.addError("discountError", DiscountStatus.SYSTEM_ERROR.name());
			}
		}
	}
	
	/**
	 * Removes a cart's discount including any discount applied
	 * to items in the cart.
	 * @param cart
	 */
	public void removeCartDiscount(ShoppingCartVO cart) {
		if (cart.isDiscounted()) {
			log.debug("removing cart discount...");
			// first check to see if shopping cart items have discount also
			for (String ci : cart.getItems().keySet()) {
				ShoppingCartItemVO item = cart.getItems().get(ci);
				ProductVO prod = item.getProduct();
				if (prod != null && prod.isDiscounted()) {
					// remove/reset discount from items
					prod.setDiscounts(null);
				}
			}
			// remove/reset cart's discount
			cart.setCartDiscount(null);
			cart.setPromotionCode(null);
			cart.setPromotionDiscount(0.0);
		}
	}
	
	/**
	 * Processes 'free or flat-rate' shipping discount of the cart subtotal (excluding
	 * shipping and tax).
	 * @param updateShipping
	 */
	public void manageShippingDiscount(String updateShipping) {
		// if this is an 'updateShipping' operation, do not process the shipping discount.
		if (Convert.formatBoolean(updateShipping)) return;
		// see if user shipping info is set
		if (cart.getShippingInfo() != null) {
			if (cart.getSubTotal() >= discount.getOrderMinimum()) {
				ShippingInfoVO shipOption = new ShippingInfoVO();
				shipOption.setSelected(true);
				shipOption.setShippingCost(discount.getDiscountValue());
				shipOption.setShippingMethodId(USADiscountVO.DEFAULT_SHIPPING_METHOD);
				shipOption.setShippingMethodName(USADiscountVO.DEFAULT_SHIPPING_METHOD);
				//shipOption.setShippingMethodName(discount.getDescription());
				Map<String, ShippingInfoVO> shippingOptions = new HashMap<String, ShippingInfoVO>();
				shippingOptions.put(shipOption.getShippingMethodId(), shipOption);			
				cart.setShippingOptions(shippingOptions);
				cart.setShipping(shipOption.getShippingMethodId());
				//cart.setPromotionDiscount(discount.getDiscountValue());
				log.debug("shipping discount value is: " + discount.getDiscountValue());
			}
		}
	}
	
	/**
	 * Manages the discount for a single item when an item is added to the cart.
	 * @param cart
	 * @param item
	 */
	public void manageAddItemDiscount(ShoppingCartVO cart, ShoppingCartItemVO item) {
		log.debug("manageAddItemDiscount...");
		if (cart.isDiscounted()) {

			USADiscountVO cartDisc = (USADiscountVO) cart.getCartDiscount().get(0);
			if (cartDisc.getEnumDiscountType().equals(DiscountType.ITEM)) {

				if (cartDisc.getItemDiscountType().getIntegerType() > ItemDiscountType.DEFAULT.getIntegerType()) {
					// only process item-level discount if the item level discount type > 0.
					try {
						// get the discount for the item being added.
						USADiscountVO iDisc = this.retrieveCartItemDiscount(cart, item);
						String rawProdIdKey = item.getProduct().getCustProductNo();
						//log.debug("checking discount for item with productId key of: " + rawProdIdKey);
						if (iDisc.getProductDiscounts().containsKey(rawProdIdKey)) {

							if (iDisc.getItemDiscountType().equals(ItemDiscountType.CLEARANCE)) {
								double msrp = item.getProduct().getMsrpCostNo();
								double discVal = iDisc.getDiscountDollarValue();
								iDisc.setDiscountDollarValue(msrp - discVal);
							}
							//log.debug("adding discount to product with productId: " + item.getProductId());
							DiscountVO pDisc = iDisc.getProductDiscounts().get(rawProdIdKey);
							item.getProduct().addDiscount(pDisc);
							cartDisc.addProductDiscount(rawProdIdKey, pDisc);
						}
					} catch (DocumentException de) {
						log.error("Error managing cart item discount, ", de);
						this.addError("errorDiscountItem", de.getMessage());
					}
				}
			}
		}
	}
	
	/**
	 * Calculates cart discount based on the existing cart discount.
	 */
	public void recaculateCartDiscount() {
		if (cart == null) return;
		// reset the cart's subtotal to the non-discounted subtotal
		cart.setPromotionDiscount(0.0);
		if (cart.getSubTotal() == 0.0 || !cart.isDiscounted()) return;
		// recalculate cart based on the type of discount.
		discount = (USADiscountVO) cart.getCartDiscount().get(0);		
		//log.debug("processing discount of type: " + discount.getEnumDiscountType().name());
		switch (discount.getEnumDiscountType()) {
			case DOLLAR: // dollar off order total
				this.processDollarDiscount();
				break;
			case PERCENT: // % off order total
				this.processPercentDiscount();
				break;
			case ITEM: // discount off item prices
				this.processItemDiscount();
				break;
			case FREE: // free gift ... not implemented at this time
				break;
			default:
				break;
		}
		//log.debug("cart subtotal: " + cart.getSubTotal());
		//log.debug("promotion discount: " + cart.getPromotionDiscount());
	}

	/**
	 * Loops the cart items and applies the appropriate discount to each item.
	 * @param cart
	 */
	private void refreshCartItemDiscounts(ShoppingCartVO cart) {
		log.debug("refreshing cart item discounts");
		USADiscountVO uDisc = (USADiscountVO)cart.getCartDiscount().get(0);
		//log.debug("discount name is: " + uDisc.getDiscountName());
		Map<String, ShoppingCartItemVO> items = cart.getItems();
		//log.debug("number of cart items to check: " + items.size());
		Map<String, DiscountVO> prodDiscounts = uDisc.getProductDiscounts();
		//log.debug("prodDiscounts keys: " + prodDiscounts.keySet());
		
		/* 2015-01-26 DBargerhuff: IMPORTANT!!
		 * Product IDs returned by the USA webservice are NOT PREFIXED with
		 * a catalog ID.  Therefore, we must compare based on the product's
		 * custom product number field which is the raw, non-prefixed product ID.
		 * 
		 * Loop items, remove the discount from each item's product.  If the item's
		 * raw product ID (key) exists on the product discounts map, find the 'price'
		 * attribute and add the discount to the product.  If 'price' attribute does
		 * not exist, skip.
		 */
		for (String prodKey : items.keySet()) {
			ProductVO prod = items.get(prodKey).getProduct();
			//log.debug("examining raw product no: " + prod.getCustProductNo());
			// remove existing discount
			prod.setDiscounts(null);
			// check to see if new discount applies to this product
			if (prodDiscounts.containsKey(prod.getCustProductNo())) {
				//log.debug("adding product discount for product: " + prodKey);
				prod.addDiscount(prodDiscounts.get(prod.getCustProductNo()));
			}
		}
	}
	
	/**
	 * Checks to see if we need to retrieve discount data.
	 * @param cart
	 * @param promoCode
	 * @return
	 */
	private boolean checkForDiscountRetrieval(ShoppingCartVO cart, String promoCode) {
		boolean retrieveDiscount = false;
		if (promoCode.length() > 0) {
			if (cart.isDiscounted()) {
				// get current promo code
				String currPromoCode = StringUtil.checkVal(cart.getCartDiscount().get(0).getDiscountName());
				if (currPromoCode.length() > 0 && !currPromoCode.equalsIgnoreCase(promoCode)) {
					retrieveDiscount = true;
				}
			} else {
				retrieveDiscount = true;
			}
		}
		return retrieveDiscount;
	}
	
	/**
	 * Calls the webservice to retrieve the discount data element.
	 * @param cart
	 * @param promoCode
	 * @return
	 * @throws DocumentException
	 */
	private Element retrieveFullCartDiscountElement(ShoppingCartVO cart, String promoCode) 
			throws DocumentException {
		log.debug("retrieving discount detail from web service...");
		return this.retrieveDiscountElement(cart.getItems().values(), promoCode);
	}

	/**
	 * Retrieves the USADiscountVO for this item, if applicable, and sets the discount on the item.
	 * @param cart
	 * @param item
	 * @return
	 * @throws DocumentException
	 */
	private USADiscountVO retrieveCartItemDiscount(ShoppingCartVO cart, ShoppingCartItemVO item) 
			throws DocumentException {
		log.debug("retrieving cart item discount...");
		Element disc = this.retrieveCartItemDiscountElement(item, cart.getPromotionCode());
		return new USADiscountVO(disc);
	}
	
	/**
	 * Calls the webservice to retrieve the discount data element.
	 * @param item
	 * @param promoCode
	 * @return
	 * @throws DocumentException
	 */
	private Element retrieveCartItemDiscountElement(ShoppingCartItemVO item, String promoCode) 
			throws DocumentException {
		log.debug("retrieving single discount item element from web service...");
		List<ShoppingCartItemVO> items = new ArrayList<ShoppingCartItemVO>();
		items.add(item);
		return this.retrieveDiscountElement(items, promoCode);
	}
	
	/**
	 * Calls the webservice to retrieve the promo code details in an XML response.
	 * @param items
	 * @param promoCode
	 * @return
	 * @throws DocumentException
	 */
	private Element retrieveDiscountElement(Collection<ShoppingCartItemVO> items, String promoCode) 
			throws DocumentException {
		WebServiceAction wsa = new WebServiceAction(this.actionInit);
		wsa.setAttributes(attributes);
		wsa.setAttribute(WebServiceAction.CATALOG_SITE_ID, catalogSiteId);
		Element disc = wsa.retrievePromotionDiscount(items, promoCode);
		return disc;
	}
	
	/**
	 * Processes 'dollar off' discount of cart subtotal (excluding
	 * shipping and tax).  This method modifies the cart subtotal.
	 */
	private void processDollarDiscount() {
		if (cart.getSubTotal() >= discount.getOrderMinimum()) {
			//cart.setSubTotal(cart.getSubTotal() - discount.getDiscountValue());
			cart.setPromotionDiscount(discount.getDiscountValue());
			log.debug("discount dollar value is: " + discount.getDiscountValue());
		}
	}
	
	/**
	 * Processes 'percent off' discount of cart subtotal (excluding
	 * shipping and tax).  This method modifies the cart subtotal.
	 */
	private void processPercentDiscount() {
		if (cart.getSubTotal() >= discount.getOrderMinimum()) {
			//double subT = cart.getSubTotal();
			//cart.setSubTotal(subT - (subT * discount.getDiscountValue()));
			cart.setPromotionDiscount(cart.getSubTotal() * discount.getDiscountValue());
			log.debug("discount % value is: " + discount.getDiscountValue());
		}
	}
	
	/**
	 * Processes 'item level' discount of cart subtotal (excluding shipping and tax). This method
	 * modifies the cart subtotal.
	 */
	private void processItemDiscount() {
		// loop items
		log.debug("processing item discount...");
		if (cart.getItems() != null && cart.getItems().size() > 0) {
			double itemPrice = 0.0;
			double itemSubTotal = 0.0;
			
			for (String key : cart.getItems().keySet()) {
				ShoppingCartItemVO item = cart.getItems().get(key);
				itemPrice = item.getExtendedPrice() + (item.getQuantity() * item.getAttributePrice());
				
				//log.debug("---> item price before applying discount to this item: " + itemPrice);
				if (item.isDiscounted()) {
					//log.debug("-------> item is discounted...");
					USADiscountVO disc = (USADiscountVO) item.getProduct().getDiscounts().get(0);
					//log.debug("-------> disc type|value|dollar value: " + disc.getDiscountType() + "|" + disc.getDiscountValue() + "|" + disc.getDiscountDollarValue());
					itemPrice = ( item.getQuantity() * (disc.getDiscountDollarValue() + item.getAttributePrice()) );
				}
				
				//log.debug("---> item price after applying discount to item: " + itemPrice);
				itemSubTotal += itemPrice;
				//log.debug("itemSubTotal is now: " + itemSubTotal);
				
				// reset itemPrice
				itemPrice = 0.0;
			}
			
			
			//log.debug("cart|item subtotals after processing item discount: " + cart.getSubTotal() + "|" + itemSubTotal);
			BigDecimal bCartSub = BigDecimal.valueOf(cart.getSubTotal());
			BigDecimal bItemSub = BigDecimal.valueOf(itemSubTotal);
			BigDecimal pDisc = bCartSub.subtract(bItemSub);
			cart.setPromotionDiscount(pDisc.doubleValue());
		}
	}
	
	/**
	 * @return the catalogSiteId
	 */
	public String getCatalogSiteId() {
		return catalogSiteId;
	}

	/**
	 * @param catalogSiteId the catalogSiteId to set
	 */
	public void setCatalogSiteId(String catalogSiteId) {
		this.catalogSiteId = catalogSiteId;
	}

	/**
	 * @return the cart
	 */
	public ShoppingCartVO getCart() {
		return cart;
	}

	/**
	 * @param cart the cart to set
	 */
	public void setCart(ShoppingCartVO cart) {
		this.cart = cart;
	}

	public void addError(String key, String val) {
		if (StringUtil.checkVal(key).length() == 0) return;
		errors.put(key, val);
	}
	
	/**
	 * @return the errors
	 */
	public Map<String, String> getErrors() {
		return errors;
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(Map<String, String> errors) {
		this.errors = errors;
	}
	
	/**
	 * Helper method for determining if errors occurred
	 * during discount processing.
	 * @return
	 */
	public boolean hasErrors() {
		if (errors != null && ! errors.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the actionInit
	 */
	public ActionInitVO getActionInit() {
		return actionInit;
	}

	/**
	 * @param actionInit the actionInit to set
	 */
	public void setActionInit(ActionInitVO actionInit) {
		this.actionInit = actionInit;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	
}
