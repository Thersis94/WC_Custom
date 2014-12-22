package com.universal.signals.action;

// Java 7
import java.util.Comparator;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;

/****************************************************************************
 * <b>Title</b>: ProductAttributeComparator.java<p/>
 * <b>Description: </b> Compares product attribute VOs.  Standard 'attribute' VOs are sorted based on 
 * whether or not they are a custom attribute.  If not a custom attribute, they are sorted based on 
 * the attribute '2' text value and on display order (order_no).
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since July 31, 2012
 * Changes:
 * Jul 31, 2012: DBargerhuff: Created class.
 * Dec 15, 2014: DBargerhuff: Refactored comparison logic to resolve issues with
 * how attributes were being ordered when displayed and when we built the XML 
 * data for an order request.
 ****************************************************************************/

public class ProductAttributeComparator implements Comparator<ProductAttributeVO> {
	
	public int compare(ProductAttributeVO a1, ProductAttributeVO a2) {
		// first compare by attribute name
		int idVal = a1.getAttributeName().compareTo(a2.getAttributeName());
		if (idVal == 0) { // same name, dig deeper
			if (a1.getAttributeName().equalsIgnoreCase("custom")) {
				// custom attributes, sort by value_txt
				return a1.getValueText().compareTo(a2.getValueText());
			} else { 
				// standard attributes, sort by attrib2 text value
				int att2Val = a1.getAttribute2().compareTo(a2.getAttribute2());
				if (att2Val == 0) { // same attribute sequence, so compare display order number
					return a1.getDisplayOrderNo().compareTo(a2.getDisplayOrderNo());
				} else {
					return att2Val;
				}
			}
		} else {
			// not same type, favor standard over custom
			if (a1.getAttributeId().contains("CUSTOM")) {
				return -1;
			} else if (a2.getAttributeId().contains("CUSTOM")) {
				return 1;
			} else { // both are not same and not custom, compare attribute sequence
				int att2Val = a1.getAttribute2().compareTo(a2.getAttribute2());
				if (att2Val == 0) { // same attribute sequence, so compare display order number
					return a1.getDisplayOrderNo().compareTo(a2.getDisplayOrderNo());
				} else {
					return att2Val;
				}
			}
		}
	}
}
