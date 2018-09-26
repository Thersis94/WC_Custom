package com.wsla.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
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

		// @TODO Add language conversion
		if (keyMap.containsKey(listType)) {
			try {
				GenericVO vo = keyMap.get(listType);
				Boolean useRequest = Convert.formatBoolean(vo.getValue());
				Method method = this.getClass().getMethod(vo.getKey().toString());
				if (useRequest) putModuleData(method.invoke(this, req));
				else putModuleData(method.invoke(this));

			} catch (Exception e) {
				log.error("Unable to retrieve list: " + listType, e);
			}
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
	 * Load a list of OEMs from the ProviderAction
	 * @return
	 */
	public List<GenericVO> getOEMs() {
		return new ProviderAction(getAttributes(), getDBConnection()).getProviderOptions(ProviderType.OEM);
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
}