package com.perfectstorm.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// PS Imports
import com.perfectstorm.action.admin.CustomerAction;
import com.perfectstorm.action.admin.MemberWidget;
import com.perfectstorm.action.admin.VenueWidget;
import com.perfectstorm.common.PSConstants;
import com.perfectstorm.common.PSConstants.PSRole;
import com.perfectstorm.common.PSLocales;
import com.perfectstorm.data.CustomerVO;
import com.perfectstorm.data.CustomerVO.CustomerType;
import com.perfectstorm.data.MemberVO;
import com.perfectstorm.data.VenueVO;

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
		keyMap.put("venues", new GenericVO("getVenueLookup", Boolean.TRUE));
		keyMap.put("locale", new GenericVO("getLocales", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getRoles", Boolean.FALSE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.FALSE));
		keyMap.put("customer", new GenericVO("getCustomers", Boolean.TRUE));
		keyMap.put("customerType", new GenericVO("getCustomerType", Boolean.FALSE));
		keyMap.put("member", new GenericVO("getMembers", Boolean.TRUE));
		keyMap.put("timezone", new GenericVO("getTimeZone", Boolean.TRUE));
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
	
	/**
	 * Gets the supported locales for the app
	 * @return
	 */
	public List<GenericVO> getLocales() {
		List<GenericVO> data = new ArrayList<>(8);

		for (PSLocales val : PSLocales.values()) {
			data.add(new GenericVO(val, val.getDesc()));
		}

		return data;
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
	 * Gets the supported Customer Types for the app
	 * @return
	 */
	public List<GenericVO> getCustomerType() {
		List<GenericVO> data = new ArrayList<>(8);

		for (CustomerType val : CustomerVO.CustomerType.values()) {
			data.add(new GenericVO(val, val.getCustomerName()));
		}

		return data;
	}
	
	/**
	 * Gets a list of customers.  If acReturen is an integer, use that to return 
	 * a small list for the autocomplete.  Otherwise, get them all
	 * @param req
	 * @return
	 */
	public List<GenericVO> getCustomers(ActionRequest req) {
		// Get the data from the action
		CustomerAction ca = new CustomerAction(getDBConnection(), getAttributes());
		BSTableControlVO bst = new BSTableControlVO(req);
		bst.setLimit(req.getIntegerParameter("acReturn", 1000));
		GridDataVO<CustomerVO> data = ca.getCustomers(bst);
		
		// Store the id and name to a generic vo
		List<GenericVO> customers = new ArrayList<>(data.getTotal());
		for (CustomerVO cust : data.getRowData()) {
			customers.add(new GenericVO(cust.getCustomerId(), cust.getCustomerName()));
		}
		
		return customers;
	}
	
	/**
	 * gets a list for Auto Complete for the member search
	 * @param req
	 * @return
	 */
	public List<GenericVO> getMembers(ActionRequest req) {
		// Get the member data
		MemberWidget mw = new MemberWidget(dbConn, attributes);
		String customerId = req.getParameter("customerId");
		BSTableControlVO bst = new BSTableControlVO(req, MemberVO.class);
		GridDataVO<MemberVO> members = mw.getMemberSimple(customerId, bst);
		
		// Convert to a name/value pair and return
		List<GenericVO> data = new ArrayList<>(10);
		for(MemberVO mem : members.getRowData()) {
			StringBuilder val = new StringBuilder(128);
			val.append(mem.getFirstName()).append(" ").append(mem.getLastName());
			val.append(" (").append(mem.getEmailAddress()).append(") ");
			data.add(new GenericVO(mem.getMemberId(), val.toString()));
		}
		
		return data;
	}
	
	
	/**
	 * Gets the list of timezone data
	 * @param req
	 * @return
	 */
	public List<GenericVO> getTimeZone(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		BSTableControlVO bst = new BSTableControlVO(req);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select timezone_cd as key, zone_nm as value from ");
		sql.append(getCustomSchema()).append("timezone where 1=1 ");
		
		if (bst.hasSearch()) {
			sql.append(" and lower(zone_nm) like ? ");
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		if (! StringUtil.isEmpty(req.getParameter("country"))) {
			sql.append(" and lower(country_cd) like ? ");
			vals.add(req.getParameter("country").toLowerCase());
		}
		
		sql.append("order by zone_nm");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSQLWithCount(sql.toString(), vals, new GenericVO(), bst).getRowData();
	}
	
}
