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
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.action.admin.ProviderAction;
import com.wsla.data.provider.ProviderType;
// WSLA Libs
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

	private static Map<String, GenericVO> keyMap = new HashMap<>(16);

	/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	static {
		keyMap.put("statusCode", new GenericVO("getStatusCodes", Boolean.FALSE));
		keyMap.put("providerType", new GenericVO("getProviderTypes", Boolean.FALSE));
		keyMap.put("oem", new GenericVO("getOEMs", Boolean.FALSE));
		keyMap.put("attributeGroupCode", new GenericVO("getAttributeGroups", Boolean.FALSE));
		keyMap.put("provider", new GenericVO("getProviders", Boolean.TRUE));
		keyMap.put("activeFlag", new GenericVO("getYesNoLookup", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getOrgRoles", Boolean.TRUE));
		keyMap.put("locale", new GenericVO("getLocales", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.FALSE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.FALSE));
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
		log.debug("look up ret called");
		String listType = req.getStringParameter(SELECT_KEY);
		
		// @TODO Add language conversion
		if (keyMap.containsKey(listType)) {
			try {
				GenericVO vo = keyMap.get(listType);
				
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
		}else {
			throw new ActionException("List type Not Found in KeyMap");
		}
		
	}

	/**
	 * 
	 * @return
	 */
	public List<GenericVO> getProviderTypes() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select provider_type_id as key, type_cd as value from ");
		sql.append(getCustomSchema()).append("wsla_provider_type order by type_cd");

		DBProcessor db = new DBProcessor(getDBConnection()); 
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * selects the existing attribute groups
	 * @return
	 */
	public List<GenericVO> getAttributeGroups(){
		log.debug("getting groups ");
		StringBuilder sql = new StringBuilder(128);
		sql.append("select attribute_group_cd as key, group_nm as value from ");
		sql.append(getCustomSchema()).append("wsla_attribute_group order by group_nm");
	
		DBProcessor db = new DBProcessor(getDBConnection());
		List<GenericVO> data = db.executeSelect(sql.toString(), null, new GenericVO());
				log.debug("size " + data.size());
		return data;
		
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
		ProviderType pt = ProviderType.valueOf(req.getStringParameter("providerType", ProviderType.OEM.toString()));
		return new ProviderAction(getAttributes(), getDBConnection()).getProviderOptions(pt);
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
}