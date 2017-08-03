package com.ram.action.util;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

import com.ram.action.or.vo.RAMCaseKitVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
// WC Libs 3.2
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
		String type = req.getParameter("type", "none");
		SMTSession ses = req.getSession();
		SBUserRole role = (SBUserRole)ses.getAttribute(Constants.ROLE_DATA);
		
		switch(type) {
			case "providers":
				getProviders(req);
				break;
			case "surgeons":
				getSurgeons(req);
				break;
			case "salesReps":
				getSalesReps(req);
				break;
			case "orRooms":
				getORRooms(role, req.getParameter("selected"), req.getParameter("caseType"), req);
				break;
			case "kits":
				getKits(req);
				break;
			default:
				log.debug("can't find list type");
			
		}
		
	}

	/**
	 * Gets a list of or rooms for a given provider
	 * @param role
	 */
	public void getORRooms(SBUserRole role, String selected, String caseType, ActionRequest req) {
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select or_room_id as key, or_name as value from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_customer a ");
		sql.append(DBUtil.INNER_JOIN).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_customer_location b on a.customer_id = b.customer_id ");
		sql.append(DBUtil.INNER_JOIN).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ram_or_room c on b.customer_location_id = c.customer_location_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		sql.append(SecurityUtil.addCustomerFilter(req, "a"));
		
		if ("display".equalsIgnoreCase(caseType)) sql.append("and c.or_room_id = ? ");
		else sql.append("and c.customer_location_id = cast(? as int) ");
		sql.append("order by or_name ");
		log.debug(sql + "|" + role.getAttribute(0) + "|" + selected);

		params.add(selected);
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		this.putModuleData(data);
	}
	
	/**
	 * Gets a list of or rooms for a given provider
	 * @param role
	 * @param req 
	 */
	public void getKits(ActionRequest req) {
		
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
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new RAMCaseKitVO());
		this.putModuleData(data);
	}
	
	/**
	 * Gets a list of providers for a given user
	 * @param role
	 */
	public void getProviders(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_location_id as key, location_nm as value from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_customer_location ");
		sql.append("where active_flg = 1 ");
		sql.append(SecurityUtil.addCustomerFilter(req, ""));
		log.debug(sql);
		
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), null, new GenericVO());
		this.putModuleData(data);
	}
	
	/**
	 * Gets a list of sales reps for a given location
	 * @param role
	 */
	public void getSalesReps(ActionRequest req) {
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
		
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), null, new GenericVO());
		this.putModuleData(data);
	}
	
	/**
	 * Gets a list of surgeons for a given customer
	 * @param role
	 */
	public void getSurgeons(ActionRequest req) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select a.surgeon_id as key, coalesce(first_nm, '') || ' ' || coalesce(last_nm, '') as value from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_surgeon a ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ram_surgeon_customer_xr b ");
		sql.append("on a.surgeon_id = b.surgeon_id where 1=1 ");
		sql.append(SecurityUtil.addCustomerFilter(req, ""));
		sql.append("order by last_nm, first_nm ");
		
		List<Object> params = new ArrayList<>();
		DBProcessor dbp = new DBProcessor(getDBConnection());
		List<?> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		
		this.putModuleData(data);
	}
}
