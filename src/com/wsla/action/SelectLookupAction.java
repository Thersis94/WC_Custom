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
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.ticket.StatusCode;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVos, which will automatically be
 * available in a selectpicker
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
	public static final String SELECT_KEY = "listType";
	
	
	private Map<String, String> keyMap = new HashMap<>(16);
	
	/**
	 * 
	 */
	public SelectLookupAction() {
		super();
		assignKeys();
	}

	/**
	 * @param actionInit
	 */
	public SelectLookupAction(ActionInitVO actionInit) {
		super(actionInit);
		assignKeys();
	}
	
	/**
	 * Assigns the keys for the select type to method mapping
	 */
	private void assignKeys() {
		keyMap.put("statusCode", "getStatusCodes");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		String listType = req.getStringParameter(SELECT_KEY);
		
		if (keyMap.containsKey(listType)) {
			try {
				Method method = this.getClass().getMethod(keyMap.get(listType));
				this.putModuleData(method.invoke(method));
			} catch (Exception e) {
				log.error("Unable to retrieve list: " + listType, e);
			}
		}
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

