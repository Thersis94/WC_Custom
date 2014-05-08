package com.universal.commerce;

// JDK 6
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache DOM4j
import org.apache.log4j.Logger;
import org.dom4j.Element;

// SMTBaseLibs 2.0
import com.siliconmtn.commerce.DiscountVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.XMLUtil;

/****************************************************************************
 * <b>Title</b>: USADiscountVO.java <p/>
 * <b>Project</b>: WebCrescendo Custom<p/>
 * <b>Description: </b> VO that holds discount data specific to USA(Signals) promo codes. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 15, 2013<p/>
 * Changes:
 * Apr 15, 2013:	DBargerhuff: created class
 ****************************************************************************/
public class USADiscountVO extends DiscountVO {

	private static final long serialVersionUID = -4658441889750752033L;
	private transient Logger log = Logger.getLogger(USADiscountVO.class);
	public static final String DEFAULT_SHIPPING_METHOD = "Standard";
	private DiscountStatus discountStatus = DiscountStatus.UNKNOWN;
	private DiscountType discountType = DiscountType.UNKNOWN;
	private ItemDiscountType itemDiscountType = ItemDiscountType.UNKNOWN;
	private String description;
	private Integer discountTypeItemValue = new Integer(-1);
	private boolean excludeBackOrders = false;
	private boolean exludeSaleItems = false;
	private double orderMinimum = 0.0; // minimum threshold at which the discount is active
	private String pegasusCode;
	private Date dateStart; // date upon which the discount became valid
	private Date dateEnd; // date upon which the discount becomes invalid (expires).
	private Map<String, DiscountVO> productDiscounts; // Map key is product ID
	
	/**
	 * Enum representing valid discount status values.
	 */
	public enum DiscountStatus {ACTIVE, INACTIVE, EXPIRED, NOTFOUND, SYSTEM_ERROR, UNKNOWN}
	
	/**
	 * Enum representing valid discount type values.
	 */
	public enum DiscountType {
		DOLLAR(1), PERCENT(2), SHIPPING(3), ITEM(4), FREE(5), UNKNOWN(-1);
		private int type = 0;
		
		DiscountType(int type) {
			this.type = type;
		}
		
		public Integer getIntegerType() {
			return this.type;
		}
		
		public static DiscountType getDiscountType(Integer integerType) {
			switch(integerType.intValue()) {
			case 1:	return DOLLAR;
			case 2:	return PERCENT;
			case 3:	return SHIPPING;
			case 4:	return ITEM;
			case 5:	return FREE;
				default:	return UNKNOWN;
			}
		}
	}
	
	/**
	 * Enum representing valid <b>item</b> discount type values.
	 */
	public enum ItemDiscountType {
		DEFAULT(0), ALL(1), CLEARANCE(2), CATEGORY(3), OFFERING(4), UNKNOWN(-1);
		private Integer type = -1;
		
		ItemDiscountType(Integer type) {
			this.type = type;
		}
		
		public Integer getIntegerType() {
			return this.type;
		}
		
		public static ItemDiscountType getItemDiscountType(Integer integerType) {
			switch(integerType.intValue()) {
			case 0:	return DEFAULT;
			case 1:	return ALL;
			case 2:	return CLEARANCE;
			case 3:	return CATEGORY;
			case 4:	return OFFERING;
				default:	return UNKNOWN;
			}
		}
	}
	
	/**
	 * 
	 */
	public USADiscountVO() {}
	
	/**
	 * 
	 * @param ele
	 */
	public USADiscountVO(Element ele) {
		this.parseDiscount(ele);
	}
	
	/**
	 * Parses the discount response XML element into the appropriate bean fields.
	 * @param ele
	 */
	protected void parseDiscount(Element ele) {
		if (ele == null) return;
		setDiscountName(XMLUtil.checkVal(ele.element("Code")));
		parseDiscountStatus(XMLUtil.checkVal(ele.element("Status"),true));
		setDescription(XMLUtil.checkVal(ele.element("StatusDetail")));
		setDiscountValue(Convert.formatDouble(XMLUtil.checkVal(ele.element("Discount"),true)));
		
		Integer dType = Convert.formatInteger(XMLUtil.checkVal(ele.element("DiscountType"),true));
		setEnumDiscountType(DiscountType.getDiscountType(dType));
		Integer idType = Convert.formatInteger(XMLUtil.checkVal(ele.element("DiscountTypeItemLevel"),true));
		setItemDiscountType(ItemDiscountType.getItemDiscountType(idType));
		
		setDiscountTypeItemValue(Convert.formatInteger(XMLUtil.checkVal(ele.element("DiscountTypeItemValue"),true)));
		setOrderMinimum(Convert.formatDouble(XMLUtil.checkVal(ele.element("OrderMinimum"),true)));
		setDateStart(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, XMLUtil.checkVal(ele.element("StartDate"),true)));
		setDateEnd(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN, XMLUtil.checkVal(ele.element("EndDate"),true)));
		// perform additional parsing if this is an ITEM discount
		if (this.discountType.equals(DiscountType.ITEM)) {
			// item discount type is > 0 we parse individual item discount
			if (itemDiscountType.getIntegerType() > ItemDiscountType.DEFAULT.getIntegerType()) {
				this.parseItemDiscount(ele);
			}
		}
		// set the inherited discount type field for use in JSTL-view
		setDiscountType(discountType.name());
	}
	
	/** Parses product discounts if this in an item-type discount 
	 * @param ele
	 */
	@SuppressWarnings("unchecked")
	protected void parseItemDiscount(Element ele) {
		if (ele == null) return;
		log.debug("parsing product discount element...");
		productDiscounts = new HashMap<String, DiscountVO>();
    	Element nm = ele.element("Code");
    	String discountName = nm.getTextTrim();
		Element prods = ele.element("Products");
		List<Element> ids = (List<Element>) prods.elements("ProductID");
		String productId = null;
		for (Element e : ids) {
			productId = e.attributeValue("id");
			Element price = e.element("ProdAttr");
			double itemAmountAfterDiscount = Convert.formatDouble(price.attributeValue("price"));
			if (itemAmountAfterDiscount > 0) {
				USADiscountVO uDisc = new USADiscountVO();
				uDisc.setDiscountName(discountName);
				uDisc.setDiscountId(productId);
				uDisc.setItemDiscountType(itemDiscountType);
				uDisc.setDiscountValue(getDiscountValue()); // used for item level '1'
				uDisc.setDiscountDollarValue(itemAmountAfterDiscount); // used for item level '2'
				productDiscounts.put(productId, uDisc);
			}
		}
	}
	
	/**
	 * Parses a String into a DiscountStatus.  If the String value
	 * does not match an existing DiscountStatus name, then
	 * DiscountStatus.UNKNOWN is set as the DiscountStatus.
	 * @param status
	 */
	protected void parseDiscountStatus(String status) {
		String cVal = StringUtil.checkVal(status);
		boolean found = false;
		for (DiscountStatus ds : DiscountStatus.values()) {
			if (cVal.equalsIgnoreCase(ds.name())) {
				setDiscountStatus(ds);
				found = true;
				break;
			}
		}
		if (! found) setDiscountStatus(DiscountStatus.UNKNOWN);
	}
	
	/**
	 * Parses the String value of type into a valid Integer value. 
	 * @param type
	 * @return
	 */
	protected Integer parseDiscountType(String type) {
		Integer typeVal = Convert.formatInteger(type, -1, true);
		if (typeVal > -1 && typeVal < 6) {
			return typeVal;
		} else {
			return -1;
		}
	}

	/**
	 * @return the discountStatus
	 */
	public DiscountStatus getDiscountStatus() {
		return discountStatus;
	}

	/**
	 * @param discountStatus the discountStatus to set
	 */
	public void setDiscountStatus(DiscountStatus discountStatus) {
		this.discountStatus = discountStatus;
		if (discountStatus.equals(DiscountStatus.ACTIVE)) {
			setDiscountActive(true);
		}
	}

	/**
	 * @return the discountType
	 */
	public DiscountType getEnumDiscountType() {
		return discountType;
	}

	/**
	 * @param discountType the discountType to set
	 */
	public void setEnumDiscountType(DiscountType discountType) {
		this.discountType = discountType;
	}

	/**
	 * @return the itemDiscountType
	 */
	public ItemDiscountType getItemDiscountType() {
		return itemDiscountType;
	}

	/**
	 * @param itemDiscountType the itemDiscountType to set
	 */
	public void setItemDiscountType(ItemDiscountType itemDiscountType) {
		this.itemDiscountType = itemDiscountType;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the discountTypeItemValue
	 */
	public Integer getDiscountTypeItemValue() {
		return discountTypeItemValue;
	}

	/**
	 * @param discountTypeItemValue the discountTypeItemValue to set
	 */
	public void setDiscountTypeItemValue(Integer discountTypeItemValue) {
		this.discountTypeItemValue = discountTypeItemValue;
	}

	/**
	 * @return the excludeBackOrders
	 */
	public boolean isExcludeBackOrders() {
		return excludeBackOrders;
	}

	/**
	 * @param excludeBackOrders the excludeBackOrders to set
	 */
	public void setExcludeBackOrders(boolean excludeBackOrders) {
		this.excludeBackOrders = excludeBackOrders;
	}

	/**
	 * @return the exludeSaleItems
	 */
	public boolean isExludeSaleItems() {
		return exludeSaleItems;
	}

	/**
	 * @param exludeSaleItems the exludeSaleItems to set
	 */
	public void setExludeSaleItems(boolean exludeSaleItems) {
		this.exludeSaleItems = exludeSaleItems;
	}

	/**
	 * @return the orderMinimum
	 */
	public double getOrderMinimum() {
		return orderMinimum;
	}

	/**
	 * @param orderMinimum the orderMinimum to set
	 */
	public void setOrderMinimum(double orderMinimum) {
		this.orderMinimum = orderMinimum;
	}

	/**
	 * @return the pegasusCode
	 */
	public String getPegasusCode() {
		return pegasusCode;
	}

	/**
	 * @param pegasusCode the pegasusCode to set
	 */
	public void setPegasusCode(String pegasusCode) {
		this.pegasusCode = pegasusCode;
	}

	/**
	 * @return the dateStart
	 */
	public Date getDateStart() {
		return dateStart;
	}

	/**
	 * @param dateStart the dateStart to set
	 */
	public void setDateStart(Date dateStart) {
		this.dateStart = dateStart;
	}

	/**
	 * @return the dateEnd
	 */
	public Date getDateEnd() {
		return dateEnd;
	}

	/**
	 * @param dateEnd the dateEnd to set
	 */
	public void setDateEnd(Date dateEnd) {
		this.dateEnd = dateEnd;
	}

	/**
	 * @return the productDiscounts
	 */
	public Map<String, DiscountVO> getProductDiscounts() {
		return productDiscounts;
	}

	/**
	 * @param productDiscounts the productDiscounts to set
	 */
	public void setProductDiscounts(Map<String,DiscountVO> productDiscounts) {
		this.productDiscounts = productDiscounts;
	}

	public void addProductDiscount(String key, DiscountVO productDiscount) {
		if (productDiscounts == null) productDiscounts = new HashMap<String, DiscountVO>();
		productDiscounts.put(key, productDiscount);
	}
}
