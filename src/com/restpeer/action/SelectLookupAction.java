package com.restpeer.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// PS Imports
import com.perfectstorm.common.PSConstants;
import com.perfectstorm.common.PSConstants.PSRole;
import com.restpeer.action.admin.CategoryWidget;
import com.restpeer.common.RPConstants.DataType;
import com.restpeer.data.AttributeVO.UnitMeasure;
import com.restpeer.data.CategoryVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.Convert;
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

		for (PSRole val : PSConstants.PSRole.values()) {
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
}
