package com.ram.action.util;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

import com.ram.action.customer.CustomerAction;
import com.ram.action.data.RAMCustomerSearchVO;
import com.ram.action.or.vo.RAMCaseKitVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.SMTGeocoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs 3.2
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/********************************************************************
 * <b>Title: </b>LookupAction.java<br/>
 * <b>Description: </b>Utilit action to manage data lookups for selects and other items<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Jul 8, 2017
 * Last Updated: 
 *******************************************************************/
public class LookupAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public LookupAction() {
		super();
	}
	
	/**
	 * 
	 * @param actionInit
	 */
	public LookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		List<Object> data = null;
		
		switch(req.getParameter("type", "none")) {
			case "providers":
				data = getProviderLocations(req);
				break;
			case "oems":
				data = getOEMs(req);
				break;
			case "surgeons":
				data = getSurgeons(req);
				break;
			case "salesReps":
				data = getSalesReps(req);
				break;
			case "orRooms":
				data = getORRooms(req);
				break;
			case "kits":
				data = getKits(req);
				break;
			case "roles":
				data = getRoleList(req);
				break;
			case "kitCustomers":
				data = getKitCustomers(req);
				break;
			case "zipGeo":
				data = getZipGeocode(req);
				break;
			case "categories":
				data = getProductCategories();
				break;
			case "providerTypes":
				data = getProviderTypes();
				break;
			case "providerCustomers":
				data = getProviderCustomers(req);
				break;
			case "gtin":
				data = getCustomerGTINPrefix(req);
				break;
			case "regions":
				data = getRegions();
				break;
			default:
				log.debug("can't find list type");
		}
		this.putModuleData(data, data.size(), false);
	}
	
	/**
	 * get a zip code off the req object and returns location information
	 * @param req
	 * @return
	 */
	private List<Object> getZipGeocode(ActionRequest req) {
		SMTGeocoder gc = new SMTGeocoder();
		gc.setAttributes(attributes);
		gc.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
		gc.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, Boolean.FALSE);

		Location loc = new Location();
		loc.setZipCode(StringUtil.checkVal(req.getParameter("zipCode")));
		loc.setCountry("US");
		
		List<Object> locations = new ArrayList<>();
		
		for(GeocodeLocation locLoc : gc.geocodeLocation(loc)){
			locations.add(locLoc);
			log.debug("city " + locLoc.getCity());
		}
		
		return locations;
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<Object> getProviderCustomers(ActionRequest req) {
		RAMCustomerSearchVO csv = new RAMCustomerSearchVO(req);

		StringBuilder sql = new StringBuilder();
		sql.append("select customer_id as key, customer_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema());
		sql.append("ram_customer a ");
		sql.append(DBUtil.WHERE_CLAUSE).append("active_flg = 1 and customer_type_id = 'PROVIDER' ");
		sql.append(SecurityUtil.addCustomerFilter(req, "a"));
		sql.append("order by customer_nm ");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		List<Object> data = dbp.executeSelect(sql.toString(), CustomerAction.buildCustomerQueryParams(csv), new GenericVO());
		this.putModuleData(data);

		// Return the data
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * returns a list of regions for display on the site
	 * @param req
	 * @return
	 */
	private List<Object> getRegions() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder();
		sql.append("select region_id as key, region_nm as value  from ").append(schema).append("ram_region");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		List<Object> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		this.putModuleData(data);
		
		// Return the data
		return data;
	}

	/**
	 * returns a list of providers for display
	 * @param req
	 * @return
	 */
	private List<Object> getProviderTypes() {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder();
		sql.append("select customer_type_id as key, type_nm as value  from ").append(schema).append("ram_customer_type");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		List<Object> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		this.putModuleData(data);
		
		// Return the data
		return data;
	}

	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<Object> getKitCustomers(ActionRequest req) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		RAMCustomerSearchVO csv = new RAMCustomerSearchVO(req);
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder();
		sql.append("select customer_id as key, customer_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_customer a ");
		sql.append(CustomerAction.getCustomerLookupWhereClause(csv, schema, req, params));

		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		List<Object> data = dbp.executeSelect(sql.toString(), CustomerAction.buildCustomerQueryParams(csv), new GenericVO());
		this.putModuleData(data);

		// Return the data
		return dbp.executeSelect(sql.toString(), CustomerAction.buildCustomerQueryParams(csv), new GenericVO());
	}
	
	/**
	 * Gets a list of OEMS
	 * @param req
	 * @return
	 */
	public List<Object> getOEMs(ActionRequest req) {
		RAMCustomerSearchVO csv = new RAMCustomerSearchVO(req);

		StringBuilder sql = new StringBuilder();
		sql.append("select customer_id as key, customer_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_customer a where customer_type_id = 'OEM' ");
		sql.append(SecurityUtil.addOEMFilter(req, "a"));
		sql.append(" order by customer_nm asc ");
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));

		// Return the data
		return dbp.executeSelect(sql.toString(), CustomerAction.buildCustomerQueryParams(csv), new GenericVO());
	}

	/**
	 * Gets a list of or rooms for a given provider
	 * @param role
	 */
	public List<Object> getORRooms(ActionRequest req) {
		String selected = req.getParameter("selected");
		String caseType = req.getParameter("caseType");
		
		List<Object> params = new ArrayList<>();
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select or_room_id as key, or_name as value from ").append(schema);
		sql.append("ram_customer a ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("ram_customer_location b on a.customer_id = b.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema);
		sql.append("ram_or_room c on b.customer_location_id = c.customer_location_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		sql.append(SecurityUtil.addCustomerFilter(req, "a"));
		
		if ("display".equalsIgnoreCase(caseType)) sql.append("and c.or_room_id = ? ");
		else sql.append("and c.customer_location_id = cast(? as int) ");
		sql.append("order by or_name ");

		params.add(selected);
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), params, new GenericVO());
	}
	
	/**
	 * Gets a list of or rooms for a given provider
	 * @param role
	 * @param req 
	 */
	public List<Object> getKits(ActionRequest req) {
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select ck.case_kit_id, ck.case_id, p.product_id, p.product_nm, ck.processed_flg, lm.serial_no_txt from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_case_kit ck ");
		sql.append(DBUtil.INNER_JOIN).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_location_item_master lm on ck.location_item_master_id = lm.location_item_master_id ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_product p on lm.product_id = p.product_id ");
		sql.append("where case_id = ? and processed_flg = 0 ");
		
		log.debug(sql + "|" + req.getParameter("caseId"));

		String caseID = StringUtil.checkVal(req.getParameter("caseId"));
		List<Object> params = new ArrayList<>();
		params.add(caseID);
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), params, new RAMCaseKitVO());
	}
	
	/**
	 * Gets a list of providers for a given user
	 * @param role
	 */
	public List<Object> getProviderLocations(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_location_id as key, location_nm as value from ");
		sql.append(getCustomSchema()).append("ram_customer_location a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("ram_customer b on a.customer_id = b.customer_id ");
		sql.append("where a.active_flg = 1 and b.active_flg = 1 ");
		sql.append(SecurityUtil.addCustomerFilter(req, ""));
		sql.append("order by location_nm ");
		log.debug(sql);
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * Gets a list of sales reps for a given location
	 * @param role
	 */
	public List<Object> getSalesReps(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select profile_id as key, coalesce(first_nm, '') || ' ' || coalesce(last_nm, '') as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_user_role a ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role b on a.profile_role_id = b.profile_role_id ");
		sql.append("and lower(role_id) = '").append(SecurityUtil.getOEMSalesRepRoleId().toLowerCase()).append("' ");
		sql.append("where user_role_id in ( ");
		sql.append("select user_role_id from custom.ram_user_role_customer_xr ");
		sql.append(DBUtil.WHERE_1_CLAUSE).append(SecurityUtil.addCustomerFilter(req, ""));
		sql.append(") order by last_nm, first_nm ");
		log.debug(sql);
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * Gets a list of surgeons for a given customer
	 * @param role
	 */
	public List<Object> getSurgeons(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select a.surgeon_id as key, coalesce(first_nm, '') || ' ' || coalesce(last_nm, '') as value from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_surgeon a ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_surgeon_customer_xr b ");
		sql.append("on a.surgeon_id = b.surgeon_id where 1=1 ");
		sql.append(SecurityUtil.addCustomerFilter(req, ""));
		sql.append("order by last_nm, first_nm ");
		
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * Gets the role list for filtering searches by role. Only gets the roles
	 * @param req
	 * @return
	 */
	public List<Object> getRoleList(ActionRequest req) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select role_id as key, role_nm as value ");
		sql.append(DBUtil.FROM_CLAUSE).append("role where (organization_id is null ");
		sql.append("or organization_id = '").append(site.getOrganizationId()).append("') ");
		sql.append(SecurityUtil.getRoleFilter(req));
		sql.append(" order by role_nm ");
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * Gets the role list for filtering searches by role. Only gets the roles
	 * @param req
	 * @return
	 */
	public List<Object> getProductCategories() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select product_category_cd as key, category_desc as value ");
		sql.append(DBUtil.FROM_CLAUSE).append("ram_product_category ");
		sql.append(" order by category_desc ");
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * Gets the gtin prefix number for the given customer id
	 * @param req
	 * @return
	 */
	public List<Object> getCustomerGTINPrefix(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_id as key, gtin_number_txt as value ");
		sql.append(DBUtil.FROM_CLAUSE).append("ram_customer ");
		sql.append(DBUtil.WHERE_CLAUSE).append("customer_id = ? ");
		DBProcessor dbp = new DBProcessor(getDBConnection(), (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		
		List<Object> params = new ArrayList<>();
		params.add(Convert.formatInteger(req.getParameter("customerId")));
		return dbp.executeSelect(sql.toString(), params, new GenericVO());
	}
}
