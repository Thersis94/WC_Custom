package com.restpeer.action;

// JDK 1.8.x
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.restpeer.action.admin.UserAction;
import com.restpeer.common.RPConstants;
import com.restpeer.common.RPConstants.AttributeGroupCode;
import com.restpeer.common.RPConstants.RPRole;
import com.restpeer.data.RPUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.commerce.AjaxControllerFacadeAction;
import com.smt.sitebuilder.action.commerce.EcommAdminVO;
// WC3
import com.smt.sitebuilder.action.commerce.SelectLookupAction;
import com.smt.sitebuilder.action.commerce.product.EcommProductAction;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVOs, which will automatically be
 * available in a select picker as listType.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
public class RPSelectLookupAction extends SelectLookupAction {

	static {
		keyMap.put("users", new GenericVO("getUsers", Boolean.TRUE));
	}

	/**
	 * 
	 */
	public RPSelectLookupAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RPSelectLookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Loads the supported roles by this app
	 * @return
	 */
	@Override
	public List<GenericVO> getRoles() {
		List<GenericVO> data = new ArrayList<>(8);
		
		for (RPRole val : RPConstants.RPRole.values()) {
			data.add(new GenericVO(val.getRoleId(), val.getRoleName()));
		}

		return data;
	}
	
	/**
	 * Creates the list of attribute groups 
	 * @return
	 */
	@Override
	public List<GenericVO> getAttributeGroups() {
		List<GenericVO> data = super.getAttributeGroups();
		
		for (AttributeGroupCode gc : AttributeGroupCode.values()) {
			data.add(new GenericVO(gc, gc.getCodeName()));
		}
		
		Collections.sort(data, (a, b) -> ((String)a.getValue()).compareTo((String)b.getValue()));
		return data;
	}
	
	/**
	 * Gets the list of products in a hierarchy.  If the product is a high level cat,
	 * the product code is null
	 * @param req
	 * @return
	 */
	@Override
	public List<GenericVO> getLocationProducts(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(10);
		
		// If catalogId wasn't passed, then set it to limit the search
		if (StringUtil.isEmpty(req.getParameter("catalogId"))) {
			EcommAdminVO ecommData = (EcommAdminVO) req.getAttribute(AjaxControllerFacadeAction.ECOMM_ADMIN_DATA);
			req.setParameter("catalogId", ecommData.getProductCatalogId());
		}
		
		// Build the list from the results
		EcommProductAction pw = new EcommProductAction(getDBConnection(), getAttributes());
		List<ProductVO> products = pw.getProducts(req, true);
		for (ProductVO pvo : products) {
			if (StringUtil.isEmpty(pvo.getParentId())) {
				data.add(new GenericVO(null, pvo.getProductName()));
			} else {
				NumberFormat formatter = NumberFormat.getCurrencyInstance();
				String name = pvo.getProductName() + " - " + formatter.format(pvo.getMsrpCostNo());
				String pId = getScheduleFlag(pvo.getAttributes()) + "_" + pvo.getProductId();
				data.add(new GenericVO(pId, name));
			}
		}
		
		return data;
	}
	
	/**
	 * Returns whether the item is schedulable.
	 * 
	 * @param container
	 * @return
	 */
	private int getScheduleFlag(ProductAttributeContainer container) {
		int scheduleFlag = 0;
		if (container == null) return scheduleFlag;
		
		for (Node attribute : container.getRootAttributes()) {
			ProductAttributeVO attr = (ProductAttributeVO) attribute.getUserObject();
			if (RPConstants.HAS_SCHEDULE.equals(attr.getAttributeId())) {
				scheduleFlag = Convert.formatInteger(attr.getValueText());
				break;
			}
		}
		
		return scheduleFlag;
	}
	
	/**
	 * Returns a list of users
	 * @param req
	 * @return
	 */
	public List<GenericVO> getUsers(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(10);
		BSTableControlVO bst = new BSTableControlVO(req, RPUserVO.class);
		UserAction uw = new UserAction(getDBConnection(), getAttributes());
		GridDataVO<RPUserVO> users = uw.getUsers(req.getParameter("dealerLocationId"), bst);
		
		for (RPUserVO user : users.getRowData()) {
			String name = user.getFirstName() + " " + user.getLastName();
			data.add(new GenericVO(user.getProfileId(), name));
		}
		
		return data;
	}
}
