package com.restpeer.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// PS Imports
import com.restpeer.action.admin.CategoryWidget;
import com.restpeer.action.admin.ProductWidget;
import com.restpeer.action.admin.UserWidget;
import com.restpeer.common.RPConstants;
import com.restpeer.common.RPConstants.DataType;
import com.restpeer.common.RPConstants.RPRole;
import com.restpeer.data.ProductVO.UnitMeasure;
import com.restpeer.data.RPUserVO;
import com.restpeer.data.AttributeVO.GroupCode;
import com.restpeer.data.CategoryVO;
import com.restpeer.data.MemberVO.MemberType;
import com.restpeer.data.ProductVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVOs, which will automatically be
 * available in a select picker
 * as listType
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
public class SelectLookupAction extends SBActionAdapter {

	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "selectType";

	/**
	 * Req value passed form a BS Table search
	 */
	public static final String REQ_SEARCH = "search";

		/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	private static Map<String, GenericVO> keyMap = new HashMap<>(16);
	static {
		keyMap.put("activeFlag", new GenericVO("getYesNoLookup", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getRoles", Boolean.FALSE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.FALSE));
		keyMap.put("category", new GenericVO("getCategories", Boolean.FALSE));
		keyMap.put("uom", new GenericVO("getUnitMeasures", Boolean.FALSE));
		keyMap.put("attrType", new GenericVO("getAttributeTypes", Boolean.FALSE));
		keyMap.put("attrGroup", new GenericVO("getAttributeGroups", Boolean.FALSE));
		keyMap.put("memberType", new GenericVO("getMemberTypes", Boolean.FALSE));
		keyMap.put("users", new GenericVO("getUsers", Boolean.TRUE));
		keyMap.put("memberLocations", new GenericVO("getMemberLocations", Boolean.TRUE));
		keyMap.put("locationProducts", new GenericVO("getLocationProducts", Boolean.TRUE));
	}

	/**
	 * 
	 */
	public SelectLookupAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SelectLookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String listType = req.getStringParameter(SELECT_KEY);
		GenericVO vo = keyMap.get(listType);
		
		// If the key is not found, throw a json error
		if (vo == null) {
			putModuleData(null, 0, false, "List Type Not Found in KeyMap", true);
			return;
		}

		try {
			if (Convert.formatBoolean(vo.getValue())) {
				Method method = this.getClass().getMethod(vo.getKey().toString(), req.getClass());
				putModuleData(method.invoke(this, req));
			} else {
				Method method = this.getClass().getMethod(vo.getKey().toString());
				putModuleData(method.invoke(this));
			}

		} catch (Exception e) {
			log.error("Unable to retrieve list: " + listType, e);
		}
	}

	/**
	 * Load a yes no list
	 * @return
	 */
	public List<GenericVO> getYesNoLookup() {
		List<GenericVO> yesNo = new ArrayList<>();

		yesNo.add(new GenericVO("1","Yes"));
		yesNo.add(new GenericVO("0","No"));

		return yesNo;
	}

	/**
	 * Gets the supported locales for the app
	 * @return
	 */
	public List<GenericVO> getRoles() {
		List<GenericVO> data = new ArrayList<>(8);
		
		for (RPRole val : RPConstants.RPRole.values()) {
			data.add(new GenericVO(val.getRoleId(), val.getRoleName()));
		}

		return data;
	}
	
	/**
	 * Retruns a list of user prefixes
	 * @return
	 */
	public List<GenericVO> getPrefix() {
		List<GenericVO> selectList = new ArrayList<>(8);
		selectList.add(new GenericVO("Mr.", "Mr."));
		selectList.add(new GenericVO("Mrs.", "Mrs."));
		selectList.add(new GenericVO("Ms", "Ms."));
		selectList.add(new GenericVO("Miss", "Miss"));

		return selectList;
	}
	
	/**
	 * Gets the supported genders for the app
	 * @return
	 */
	public List<GenericVO> getGenders() {
		List<GenericVO> data = new ArrayList<>(8);
		data.add(new GenericVO("F", "Female"));
		data.add(new GenericVO("M", "Male"));

		return data;
	}
	
	/**
	 * Gets a list of attribute categories
	 * @return
	 */
	public List<GenericVO> getCategories() {
		List<GenericVO> data = new ArrayList<>(16);
		CategoryWidget cw = new CategoryWidget(dbConn, attributes);
		List<CategoryVO> cats = cw.getCategories(); 
		
		for (CategoryVO cat : cats) {
			data.add(new GenericVO(cat.getCategoryCode(), cat.getName()));
		}
		
		return data;
	}
	
	/**
	 * Creates the list of uom
	 * @return
	 */
	public List<GenericVO> getUnitMeasures() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (UnitMeasure uom : UnitMeasure.values()) {
			data.add(new GenericVO(uom, uom.getUomName()));
		}
		
		return data;
	}
	
	/**
	 * Creates the list of attribute types (list, single, etc ...)
	 * @return
	 */
	public List<GenericVO> getAttributeTypes() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (DataType dt : DataType.values()) {
			data.add(new GenericVO(dt, dt.getTypeName()));
		}
		
		return data;
	}
	
	/**
	 * Creates the list of attribute groups 
	 * @return
	 */
	public List<GenericVO> getAttributeGroups() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (GroupCode gc : GroupCode.values()) {
			data.add(new GenericVO(gc, gc.getCodeName()));
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
	
	/**
	 * Gets the list of products in a hierarchy.  If the product is a high level cat,
	 * the product code is null
	 * @param req
	 * @return
	 */
	public List<GenericVO> getLocationProducts(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(10);
		
		ProductWidget pw = new ProductWidget(getDBConnection(), getAttributes());
		List<ProductVO> products = pw.getProducts(null, true);
		for (ProductVO pvo : products) {
			NumberFormat formatter = NumberFormat.getCurrencyInstance();
			String name = pvo.getName() + " - " + formatter.format(pvo.getPrice());
			if (StringUtil.isEmpty(pvo.getParentCode())) data.add(new GenericVO(null, pvo.getName()));
			else data.add(new GenericVO(pvo.getProductCode(), name));
		}
		
		return data;
	}
}
