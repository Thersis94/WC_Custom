package com.perfectstorm.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perfectstorm.action.admin.VenueWidget;
import com.perfectstorm.data.VenueVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.GridDataVO;
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
		keyMap.put("venues", new GenericVO("getVenueLookup", Boolean.TRUE));
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
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getVenueLookup(ActionRequest req) {
		
		VenueWidget vw = new VenueWidget(dbConn, attributes);
		GridDataVO<VenueVO> data = vw.getVenues(null, new BSTableControlVO(req, VenueVO.class));
		List<GenericVO> genData = new ArrayList<>();
		for (VenueVO venue : data.getRowData()) {
			String desc = venue.getVenueName() + ", " + venue.getFormattedLocation();
			genData.add(new GenericVO(venue.getVenueId(), desc));
		}		
		
		return genData;
	}
}
