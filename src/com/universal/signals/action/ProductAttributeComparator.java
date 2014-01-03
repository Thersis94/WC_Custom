package com.universal.signals.action;

// JDK 1.6 Libs
import java.util.Comparator;

import com.siliconmtn.commerce.catalog.ProductAttributeVO;

/****************************************************************************
 * <b>Title</b>: CartItemProductAttributeComparator.java<p/>
 * <b>Description: </b> Compares product attribute VOs.  Standare 'attribute' VOs are sorted based on 
 * whether or not they are a custom attribute.  If not a custom attribute, they are sorted based on 
 * the attribute '2' text value and on display order (order_no).
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since July 31, 2012
 ****************************************************************************/

public class ProductAttributeComparator implements Comparator<ProductAttributeVO> {
	
	public int compare(ProductAttributeVO a1, ProductAttributeVO a2) {
		int idVal = a1.getAttributeId().compareTo(a2.getAttributeId());
		if (idVal == 0) {
			// same type, dig deeper
			if (a1.getAttributeId().contains("CUSTOM")) {
				// custom attribute, compare value_txt
				return a1.getValueText().compareTo(a2.getValueText());
			} else {
				// standard attribute, compare attribute2_txt values
				int att2Val = a1.getAttribute2().compareTo(a2.getAttribute2());
				if (att2Val == 0) {
					// attribute2_txt vals are same, compare display_order_no
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
			} else {
				return idVal;
			}
		}
	}
}
