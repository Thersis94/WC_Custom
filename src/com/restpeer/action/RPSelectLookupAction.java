package com.restpeer.action;

// JDK 1.8.x
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.restpeer.action.admin.MemberWidget;
import com.restpeer.action.admin.UserWidget;
import com.restpeer.common.RPConstants;
import com.restpeer.common.RPConstants.AttributeGroupCode;
import com.restpeer.common.RPConstants.RPRole;
import com.restpeer.data.RPUserVO;
import com.restpeer.data.MemberVO;
import com.restpeer.data.MemberVO.MemberType;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.commerce.AjaxControllerFacadeAction;
import com.smt.sitebuilder.action.commerce.EcommAdminVO;
// WC3
import com.smt.sitebuilder.action.commerce.SelectLookupAction;
import com.smt.sitebuilder.action.commerce.product.EcommProductAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
		keyMap.put("members", new GenericVO("getMembers", Boolean.TRUE));
		keyMap.put("memberType", new GenericVO("getMemberTypes", Boolean.FALSE));
		keyMap.put("memberLocations", new GenericVO("getMemberLocations", Boolean.TRUE));
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
	 * Creates the list of attribute types (list, single, etc ...)
	 * @return
	 */
	public List<GenericVO> getMembers(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(16);
		
		String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
		String userId = "";// Add this when the login module is added
		
		MemberWidget mw = new MemberWidget(getDBConnection(), getAttributes());
		List<MemberVO> members = mw.getMembers(userId, roleId, req.getParameter("memberType"));
		
		for (MemberVO mvo : members) {
			data.add(new GenericVO(mvo.getMemberId(), mvo.getName()));
		}
		
		return data;
	}
	
	/**
	 * Creates the list of attribute types (list, single, etc ...)
	 * @return
	 */
	public List<GenericVO> getMemberTypes() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (MemberType mt : MemberType.values()) {
			data.add(new GenericVO(mt, mt.getMemberName()));
		}
		
		return data;
	}
	
	/**
	 * Returns a list of users
	 * @param req
	 * @return
	 */
	public List<GenericVO> getUsers(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(10);
		BSTableControlVO bst = new BSTableControlVO(req, RPUserVO.class);
		UserWidget uw = new UserWidget(getDBConnection(), getAttributes());
		GridDataVO<RPUserVO> users = uw.getUsers(req.getParameter("memberLocationId"), bst);
		
		for (RPUserVO user : users.getRowData()) {
			String name = user.getFirstName() + " " + user.getLastName();
			data.add(new GenericVO(user.getUserId(), name));
		}
		
		return data;
	}
	
	/**
	 * Auto-complete lookup form member locations
	 * @param req
	 * @return
	 */
	public List<GenericVO> getMemberLocations(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		String memberType = StringUtil.checkVal(req.getParameter("memberType"));
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select member_location_id as key, member_nm || ' - ' || location_nm as value ");
		sql.append("from custom.rp_member a  ");
		sql.append("inner join custom.rp_member_location b "); 
		sql.append("on a.member_id = b.member_id where 1=1 ");
		if(! memberType.isEmpty()) {
			sql.append("and member_type_cd = ? ");
			vals.add(memberType);
		}
		
		BSTableControlVO bst = new BSTableControlVO(req);
		if (bst.hasSearch()) {
			sql.append("and (lower(member_nm) like ? or lower(location_nm) like ?) ");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append("order by member_nm, location_nm limit 10");
		log.info(sql.length() + "|" + sql + "|" + vals);		
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new GenericVO());
	}
}
