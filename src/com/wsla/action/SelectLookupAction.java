package com.wsla.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.admin.ProductCategoryAction;
//WSLA Libs
import static com.wsla.action.admin.ProductCategoryAction.PROD_CAT_ID;
import com.wsla.action.admin.ProductMasterAction;
import com.wsla.action.admin.ProductSetAction;
import com.wsla.action.admin.ProviderAction;
import com.wsla.action.admin.WarrantyAction;
import com.wsla.data.product.ProductVO;
import com.wsla.data.product.WarrantyType;
import com.wsla.data.provider.ProviderType;
import com.wsla.data.ticket.StatusCode;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVOs, which will automatically be
 * available in a select picker
 * as listType
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 11, 2018
 * @updates:
 ****************************************************************************/

public class SelectLookupAction extends SBActionAdapter {

	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "selectType";

	private static final String PROVIDER_TYPE = "providerType";
	private static final String PROVIDER_ID = "providerId";

	private static Map<String, GenericVO> keyMap = new HashMap<>(16);

	/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	static {
		keyMap.put("statusCode", new GenericVO("getStatusCodes", Boolean.FALSE));
		keyMap.put("oem", new GenericVO("getOems", Boolean.TRUE));
		keyMap.put("attributeGroupCode", new GenericVO("getAttributeGroups", Boolean.FALSE));
		keyMap.put(PROVIDER_TYPE, new GenericVO("getProviderTypes", Boolean.FALSE));
		keyMap.put("provider", new GenericVO("getProviders", Boolean.TRUE));
		keyMap.put("oemParts", new GenericVO("getProviderParts", Boolean.TRUE));
		keyMap.put("activeFlag", new GenericVO("getYesNoLookup", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getOrgRoles", Boolean.TRUE));
		keyMap.put("locale", new GenericVO("getLocales", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.FALSE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.FALSE));
		keyMap.put("defect", new GenericVO("getDefects", Boolean.TRUE));
		keyMap.put("product", new GenericVO("getProducts", Boolean.TRUE));
		keyMap.put("warranty", new GenericVO("getWarrantyList", Boolean.TRUE));
		keyMap.put("warrantyType", new GenericVO("getWarrantyTypeList", Boolean.FALSE));
		keyMap.put("category", new GenericVO("getProductCategories", Boolean.TRUE));
		keyMap.put("categoryGroup", new GenericVO("getCategoryGroups", Boolean.FALSE));
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
	 * @TODO Add language conversion
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String listType = req.getStringParameter(SELECT_KEY);
		GenericVO vo = keyMap.get(listType);
		if (vo == null)
			throw new ActionException("List type Not Found in KeyMap");

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
	 * Compile a list of Provider Type from the database, ordered by type_cd
	 * @return
	 */
	public List<GenericVO> getProviderTypes() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select provider_type_id as key, type_cd as value from ");
		sql.append(getCustomSchema()).append("wsla_provider_type order by type_cd");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}

	/**
	 * selects the existing attribute groups
	 * @return
	 */
	public List<GenericVO> getAttributeGroups(){
		StringBuilder sql = new StringBuilder(128);
		sql.append("select attribute_group_cd as key, group_nm as value from ");
		sql.append(getCustomSchema()).append("wsla_attribute_group order by group_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new GenericVO());

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
	 * Returns the list of roles for the WSLA org
	 * @param req
	 * @return
	 */
	public List<GenericVO> getOrgRoles(ActionRequest req) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

		StringBuilder sql = new StringBuilder(64);
		sql.append("select role_id as key, role_nm as value from role ");
		sql.append("where organization_id = ? or role_id = '100' order by role_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(site.getOrganizationId()), new GenericVO());
	}

	/**
	 * Load a list of Providers from the ProviderAction.  Must pass in the 
	 * ProviderType to determine which type to retrieve.  If providerType is not passed, 
	 * default to OEM
	 * @return
	 */
	public List<GenericVO> getProviders(ActionRequest req) {
		ProviderType pt = EnumUtil.safeValueOf(ProviderType.class, req.getParameter(PROVIDER_TYPE), ProviderType.OEM);
		return new ProviderAction(getAttributes(), getDBConnection()).getProviderOptions(pt);
	}


	/**
	 * return a list of OEMs - a specific type of providers.  Distinguished from 'getProvider' to avoid coupling in View logic.
	 * @param req
	 * @return
	 */
	public List<GenericVO> getOems(ActionRequest req) {
		if (req.hasParameter(PROD_CAT_ID)) {
			return new ProviderAction(getAttributes(), getDBConnection()).getOEMsByProductCategory(req.getParameter(PROD_CAT_ID));
		} else {
			req.setParameter(PROVIDER_TYPE, ProviderType.OEM.toString());
			return getProviders(req);
		}
	}


	/**
	 * load a list of PARTS tied to the given provider (likely an OEM)
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProviderParts(ActionRequest req) {
		return new ProductSetAction(getAttributes(), getDBConnection()).listPartsForProvider(req.getParameter(PROVIDER_ID));
	}


	/**
	 * Returns a list of status codes and their descriptions
	 * @return
	 */
	public List<GenericVO> getStatusCodes() {
		List<GenericVO> data = new ArrayList<>(64);

		// Iterate the enum
		for (StatusCode code : StatusCode.values()) {
			data.add(new GenericVO(code.name(), code.codeName));
		}

		// Sort the enum keys
		Collections.sort(data);

		return data;
	}

	/**
	 * Gets the supported locales for the app
	 * @return
	 */
	public List<GenericVO> getLocales() {
		List<GenericVO> data = new ArrayList<>(8);
		data.add(new GenericVO("en_US", "US English"));
		data.add(new GenericVO("es_MX", "MX Spanish"));

		return data;
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
	 * Gets the list of defects adds the 
	 * @return
	 */
	public List<GenericVO> getDefects(ActionRequest req) {
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder(64);
		sql.append("select defect_cd as key, defect_nm as value from ");
		sql.append(getCustomSchema()).append("wsla_defect where active_flg = 1 ");
		sql.append("and (provider_id is null ");
		if (req.hasParameter(PROVIDER_ID)) {
			sql.append("or provider_id = ? ");
			params.add(req.getParameter(PROVIDER_ID));
		}

		sql.append(") order by value");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), params, new GenericVO());
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
	 * Returns a list of user products
	 * @return
	 */
	public List<GenericVO> getProducts(ActionRequest req) {
		ProductMasterAction ai =  new ProductMasterAction(getAttributes(), getDBConnection());
		BSTableControlVO bst = new BSTableControlVO(req, ProductVO.class);
		bst.setLimit(1000);
		bst.setOffset(0);
		String providerId = req.getParameter(PROVIDER_ID);
		int setFlag = req.getIntegerParameter("setFlag");
		GridDataVO<ProductVO> products = ai.getProducts(null, providerId, setFlag, bst);

		List<GenericVO> data = new ArrayList<>(products.getTotal());
		for (ProductVO product : products.getRowData()) {
			data.add(new GenericVO(product.getProductId(), product.getProductName()	));
		}

		return data;
	}


	/**
	 * Returns a list of warranties in the DB
	 * @return
	 */
	public List<GenericVO> getWarrantyList(ActionRequest req) {
		String providerId = req.getParameter(PROVIDER_ID);

		//possibly use productId to find providerId
		if (req.hasParameter("productId")) {
			String sql = StringUtil.join("select provider_id as key from ", getCustomSchema(), 
					"wsla_product_master where product_id=?");
			log.debug(sql);
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			List<GenericVO> data = db.executeSelect(sql, Arrays.asList(req.getParameter("productId")), new GenericVO());
			if (data != null && !data.isEmpty())
				providerId = StringUtil.checkVal(data.get(0).getKey());
		}

		return new WarrantyAction(getAttributes(), getDBConnection()).listWarranties(providerId);
	}


	/**
	 * Returns a list of WarrantyType values - from the Enum
	 * @return
	 */
	public List<GenericVO> getWarrantyTypeList() {
		List<GenericVO> data = new ArrayList<>();
		for (WarrantyType type : WarrantyType.values())
			data.add(new GenericVO(type.name(), type.typeName));
		return data;
	}


	/**
	 * Default state is to receive the high level (parent) parent categories.
	 * Passing in the productCategoryId will get all of the children for the provided category
	 * passing in allLevels=true will get the entire list of categories
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProductCategories(ActionRequest req) {
		return new ProductCategoryAction(getAttributes(), getDBConnection()).getCategoryList(req);
	}


	/**
	 * Return a distinct list of Group_CD values from the product_group table
	 * @param req
	 * @return
	 */
	public List<GenericVO> getCategoryGroups() {
		return new ProductCategoryAction(getAttributes(), getDBConnection()).getGroupList();
	}
}