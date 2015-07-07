/**
 *
 */
package com.ram.action.provider;

import com.ram.action.data.RAMProductSearchVO;
import com.ram.action.products.ProductAction;
import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: VisionProductAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Specialized Product Lookup Action that queries vision
 * system product grid and ensures specialized data is on it.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 22, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class VisionProductAction extends ProductAction {

	/**
	 * 
	 */
	public VisionProductAction() {
		super();
	}

	public VisionProductAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * Generates the queries for retrieving and counting products based on a number of input.
	 *
	 * Fixed query and database to use a View on the Customer Table to avoid the circular reference
	 * that was causing problems with the Product Lookup query.  Now performance is much better as we
	 * don't have to perform any intersects on the data.
	 *
	 * Cleaned up the select query to perform the pagination in query.  Reduces
	 * data returned and eliminates app server side looping to records we actually
	 * care about.
	 * @param customerId
	 * @param term
	 * @param kitFilter
	 * @param providerId
	 * @param isCount
	 * @param limit
	 * @return
	 */
	@Override
	public String getProdList(RAMProductSearchVO svo) {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder(1700);

		if(svo.isCount()) {
			sb.append("select count(distinct a.product_id) from ").append(schema);
		} else {
			sb.append("select * from (select ROW_NUMBER() OVER (order by Product_nm) as RowNum, ");
			sb.append("a.PRODUCT_ID, a.CUSTOMER_ID, a.CUST_PRODUCT_ID, a.PRODUCT_NM, ");
			sb.append("a.DESC_TXT, a.SHORT_DESC, a.LOT_CODE_FLG, a.ACTIVE_FLG, ");
			sb.append("a.EXPIREE_REQ_FLG, a.GTIN_PRODUCT_ID, b.CUSTOMER_NM, a.KIT_FLG, ");
			sb.append("a.MANUAL_ENTRY_FLG, COUNT(c.KIT_LAYER_ID) as 'LAYOUT_DEPTH_NO' from ").append(schema);
		}

		//Build Initial Query
		sb.append("ram_product a ");
		sb.append("inner join ").append(schema).append("RAM_OEM_CUSTOMER b ");
		sb.append("on a.customer_id = b.customer_id ");
		sb.append("inner join ").append(schema).append("RAM_KIT_LAYER c ");
		sb.append("on a.PRODUCT_ID = c.PRODUCT_ID ");
		sb.append(this.getWhereClause(svo));

		if(!svo.isCount()) {
			//Add Group By Statement due to Count(KitLayerId) from above.
			sb.append("group by ");
			sb.append("a.PRODUCT_ID, a.CUSTOMER_ID, a.CUST_PRODUCT_ID, a.PRODUCT_NM, ");
			sb.append("a.DESC_TXT, a.SHORT_DESC, a.LOT_CODE_FLG, a.ACTIVE_FLG, ");
			sb.append("a.EXPIREE_REQ_FLG, a.GTIN_PRODUCT_ID, b.CUSTOMER_NM, a.KIT_FLG, ");
			sb.append("a.MANUAL_ENTRY_FLG ");
		}

		//Lastly if this is not a count call order the results.
		if(!svo.isCount())
			sb.append(") as paginatedResult where RowNum >= ? and RowNum < ? order by RowNum");

		log.debug(svo.getCustomerId() + "|" + svo.getProviderId() + "|" + sb.toString());
		return sb.toString();
	}
}
